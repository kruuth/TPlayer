package com.grok.tplayer.data.scanner

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.grok.tplayer.data.db.TrackDao
import com.grok.tplayer.data.model.SourceFolder
import com.grok.tplayer.data.model.Track
import com.grok.tplayer.data.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val metadataExtractor: MetadataExtractor,
    private val userPreferences: UserPreferences
) {
    private val _scanProgress = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanProgress: StateFlow<ScanState> = _scanProgress

    sealed class ScanState {
        data object Idle : ScanState()
        data class PreparingFolders(val folders: List<String>) : ScanState()
        data class Scanning(val current: Int, val total: Int, val fileName: String) : ScanState()
        data class Finished(val count: Int) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    suspend fun listSubfolders(treeUri: Uri): List<String> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val rootName = root.name?.takeIf { it.isNotBlank() } ?: "Root"
        // Only immediate children names (UI shows short names; we store full relative path)
        root.listFiles()
            .filter { it.isDirectory }
            .mapNotNull { dir -> dir.name?.let { name -> "$rootName/$name" } }
            .sorted()
    }

    suspend fun scanTree(
        treeUri: Uri,
        displayName: String,
        isDefault: Boolean = false,
        skipFolders: Set<String> = emptySet()
    ) = withContext(Dispatchers.IO) {
        try {
            _scanProgress.value = ScanState.Scanning(0, 0, "Preparing…")
            metadataExtractor.clearSessionCache()

            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            if (skipFolders.isNotEmpty()) {
                val current = userPreferences.settings.first().excludedFolders
                userPreferences.setExcludedFolders(current + skipFolders)
            }

            // Only use explicit skips + saved exclusions; never treat root as excluded
            val excluded = (userPreferences.settings.first().excludedFolders + skipFolders)
                .map { normalizePath(it) }
                .filter { it.isNotBlank() }
                .toSet()

            trackDao.insertSource(
                SourceFolder(
                    uri = treeUri.toString(),
                    displayName = displayName,
                    isDefault = isDefault,
                    lastScanned = System.currentTimeMillis()
                )
            )

            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: run {
                    _scanProgress.value = ScanState.Error("Cannot open folder — try picking it again")
                    return@withContext
                }

            val rootLabel = root.name?.takeIf { it.isNotBlank() } ?: "Root"
            _scanProgress.value = ScanState.Scanning(0, 0, "Listing files in $rootLabel…")

            val mp3Files = mutableListOf<Pair<DocumentFile, String>>()
            // Prefer DocumentsContract (more reliable than DocumentFile.listFiles on some devices)
            collectMp3sViaContract(treeUri, rootLabel, mp3Files, excluded)

            if (mp3Files.isEmpty()) {
                // Fallback to DocumentFile recursion
                collectMp3sDocumentFile(root, rootLabel, mp3Files, excluded)
            }

            val total = mp3Files.size
            if (total == 0) {
                trackDao.clearAllTracks()
                _scanProgress.value = ScanState.Finished(0)
                return@withContext
            }

            _scanProgress.value = ScanState.Scanning(0, total, "Found $total MP3(s), reading tags…")
            val tracks = mutableListOf<Track>()

            mp3Files.forEachIndexed { index, (doc, folderPath) ->
                val fileName = doc.name ?: "unknown.mp3"
                _scanProgress.value = ScanState.Scanning(index + 1, total, fileName)
                metadataExtractor.extract(doc.uri, fileName, folderPath)?.let { tracks.add(it) }
            }

            trackDao.clearAllTracks()
            trackDao.insertTracks(tracks)
            _scanProgress.value = ScanState.Finished(tracks.size)
        } catch (e: Exception) {
            e.printStackTrace()
            _scanProgress.value = ScanState.Error(e.message ?: "Scan failed")
        }
    }

    suspend fun scanDefaultMusicFolder(skipFolders: Set<String> = emptySet()) =
        withContext(Dispatchers.IO) {
            try {
                _scanProgress.value = ScanState.Scanning(0, 0, "Querying MediaStore…")
                metadataExtractor.clearSessionCache()

                if (skipFolders.isNotEmpty()) {
                    val current = userPreferences.settings.first().excludedFolders
                    userPreferences.setExcludedFolders(current + skipFolders)
                }
                val excluded = (userPreferences.settings.first().excludedFolders + skipFolders)
                    .map { normalizePath(it) }
                    .filter { it.isNotBlank() }
                    .toSet()

                val tracks = mutableListOf<Track>()
                val projection = arrayOf(
                    android.provider.MediaStore.Audio.Media._ID,
                    android.provider.MediaStore.Audio.Media.DISPLAY_NAME,
                    android.provider.MediaStore.Audio.Media.RELATIVE_PATH
                )
                // Broad query: any music track; filter .mp3 by name if MIME is missing
                val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
                context.contentResolver.query(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val nameCol =
                        cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
                    val pathCol =
                        cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.RELATIVE_PATH)
                    val total = cursor.count
                    var index = 0
                    var considered = 0
                    while (cursor.moveToNext()) {
                        index++
                        val fileName = cursor.getString(nameCol) ?: continue
                        // Accept mp3 by extension (MIME varies by OEM)
                        if (!fileName.endsWith(".mp3", ignoreCase = true)) continue

                        val folderPath = cursor.getString(pathCol)?.trimEnd('/')?.trimEnd('\\')
                            ?: "Music"
                        if (isExcluded(folderPath, excluded)) continue

                        considered++
                        val id = cursor.getLong(idCol)
                        val uri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )
                        _scanProgress.value = ScanState.Scanning(index, total, fileName)
                        metadataExtractor.extract(uri, fileName, folderPath)?.let { tracks.add(it) }
                    }
                    if (considered == 0 && total > 0) {
                        // No .mp3 by name — try all IS_MUSIC entries
                        cursor.moveToPosition(-1)
                        index = 0
                        while (cursor.moveToNext()) {
                            index++
                            val fileName = cursor.getString(nameCol) ?: "track"
                            val folderPath = cursor.getString(pathCol)?.trimEnd('/') ?: "Music"
                            if (isExcluded(folderPath, excluded)) continue
                            val id = cursor.getLong(idCol)
                            val uri = android.content.ContentUris.withAppendedId(
                                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                            )
                            _scanProgress.value = ScanState.Scanning(index, total, fileName)
                            metadataExtractor.extract(uri, fileName, folderPath)?.let { tracks.add(it) }
                        }
                    }
                }

                trackDao.clearAllTracks()
                trackDao.insertTracks(tracks)
                trackDao.insertSource(
                    SourceFolder(
                        uri = "mediastore://music",
                        displayName = "Internal storage/Music",
                        isDefault = true,
                        lastScanned = System.currentTimeMillis()
                    )
                )
                _scanProgress.value = ScanState.Finished(tracks.size)
            } catch (e: Exception) {
                e.printStackTrace()
                _scanProgress.value = ScanState.Error(e.message ?: "MediaStore scan failed")
            }
        }

    /**
     * Walk tree using DocumentsContract (works better than DocumentFile on many devices).
     */
    private fun collectMp3sViaContract(
        treeUri: Uri,
        rootLabel: String,
        out: MutableList<Pair<DocumentFile, String>>,
        excluded: Set<String>
    ) {
        val treeDocId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Exception) {
            return
        }
        walkDocumentTree(treeUri, treeDocId, rootLabel, out, excluded)
    }

    private fun walkDocumentTree(
        treeUri: Uri,
        parentDocId: String,
        folderPath: String,
        out: MutableList<Pair<DocumentFile, String>>,
        excluded: Set<String>
    ) {
        if (isExcluded(folderPath, excluded)) return

        val childrenUri = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        } catch (_: Exception) {
            return
        }

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: continue
                    val mime = cursor.getString(mimeIdx) ?: ""

                    if (DocumentsContract.Document.MIME_TYPE_DIR == mime) {
                        val childPath = "$folderPath/$name"
                        if (!isExcluded(childPath, excluded)) {
                            walkDocumentTree(treeUri, docId, childPath, out, excluded)
                        }
                    } else if (
                        name.endsWith(".mp3", ignoreCase = true) ||
                        mime.contains("mpeg", ignoreCase = true) ||
                        mime == "audio/mp3" ||
                        mime == "audio/x-mp3"
                    ) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val doc = DocumentFile.fromSingleUri(context, fileUri)
                        if (doc != null && doc.exists()) {
                            out.add(doc to folderPath)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun collectMp3sDocumentFile(
        dir: DocumentFile,
        currentPath: String,
        out: MutableList<Pair<DocumentFile, String>>,
        excluded: Set<String>
    ) {
        if (isExcluded(currentPath, excluded)) return
        val children = try {
            dir.listFiles()
        } catch (_: Exception) {
            emptyArray()
        }
        for (file in children) {
            try {
                when {
                    file.isDirectory -> {
                        val childPath = "$currentPath/${file.name}"
                        if (!isExcluded(childPath, excluded)) {
                            collectMp3sDocumentFile(file, childPath, out, excluded)
                        }
                    }
                    file.isFile -> {
                        val name = file.name ?: continue
                        if (name.endsWith(".mp3", ignoreCase = true)) {
                            out.add(file to currentPath)
                        }
                    }
                }
            } catch (_: Exception) {
                // skip unreadable entries
            }
        }
    }

    /** True only if [path] is exactly an excluded folder or under one. */
    private fun isExcluded(path: String, excluded: Set<String>): Boolean {
        if (excluded.isEmpty()) return false
        val n = normalizePath(path)
        if (n.isEmpty()) return false
        return excluded.any { excl ->
            val e = normalizePath(excl)
            if (e.isEmpty()) return@any false
            // Exact match or child of excluded folder — not loose substring match
            n == e || n.startsWith("$e/")
        }
    }

    private fun normalizePath(path: String): String =
        path.trim().trim('/').replace('\\', '/').replace(Regex("/+"), "/")
}
