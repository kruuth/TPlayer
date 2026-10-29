package com.iks.tplayer.data.scanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.iks.tplayer.data.db.TrackDao
import com.iks.tplayer.data.model.SourceFolder
import com.iks.tplayer.data.model.Track
import com.iks.tplayer.data.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** Only one scan at a time — prevents overlapping / double scans. */
    private val scanMutex = Mutex()

    private val scanNotifyId = 2002
    private val scanChannelId = "tplayer_scan"

    private fun ensureScanChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(scanChannelId) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                scanChannelId,
                "Library scan",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
    }

    private fun notifyScan(current: Int, total: Int, fileName: String) {
        ensureScanChannel()
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val text = if (total > 0) "$current / $total  $fileName" else fileName
        val notif = NotificationCompat.Builder(context, scanChannelId)
            .setContentTitle("Scanning library")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(total.coerceAtLeast(0), current.coerceAtLeast(0), total <= 0)
            .setSilent(true)
            .build()
        mgr.notify(scanNotifyId, notif)
    }

    private fun cancelScanNotification() {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.cancel(scanNotifyId)
    }


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
        skipFolders: Set<String> = emptySet(),
        isCloud: Boolean = false,
        /** When true, do not mark Finished (used while rescanning several sources). */
        reportFinished: Boolean = true,
        /**
         * When true (default for rescan), only re-read tags in folders whose
         * size/content signature changed. First scan or force full ignores this.
         */
        incremental: Boolean = true
    ) = withContext(Dispatchers.IO) {
        if (!scanMutex.tryLock()) {
            // Another scan is already running
            return@withContext
        }
        try {
            _scanProgress.value = ScanState.Scanning(0, 0, "Preparing…")
            notifyScan(0, 0, "Preparing…")
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
                    lastScanned = System.currentTimeMillis(),
                    isCloud = isCloud
                )
            )
            // DataStore backup so root survives rare DB problems
            userPreferences.addSavedSourceUri(treeUri.toString())

            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: run {
                    _scanProgress.value = ScanState.Error("Cannot open folder — try picking it again")
                    return@withContext
                }

            // Prefix cloud paths so the Cloud browse tab can filter them
            val rootLabel = if (isCloud) {
                "Cloud/${displayName.ifBlank { root.name ?: "Drive" }}"
            } else {
                root.name?.takeIf { it.isNotBlank() } ?: "Root"
            }
            _scanProgress.value = ScanState.Scanning(0, 0, "Listing files in $rootLabel…")

            val mp3Files = mutableListOf<Pair<DocumentFile, String>>()
            // Prefer DocumentsContract (more reliable than DocumentFile.listFiles on some devices)
            collectMp3sViaContract(treeUri, rootLabel, mp3Files, excluded)

            if (mp3Files.isEmpty()) {
                // Fallback to DocumentFile recursion
                collectMp3sDocumentFile(root, rootLabel, mp3Files, excluded)
            }

            // Build per-folder content signatures (count + sizes + names) — cheap vs tag reads
            val byFolder = mutableMapOf<String, MutableList<DocumentFile>>()
            for ((doc, path) in mp3Files) {
                byFolder.getOrPut(path) { mutableListOf() }.add(doc)
            }
            val newSigs: Map<String, String> = byFolder.mapValues { (_, docs) ->
                folderContentSignature(docs)
            }
            val oldSigsAll = userPreferences.parseFolderSignatures(
                userPreferences.settings.first().folderSignatures
            )
            // Only compare signatures under this root
            val oldSigs = oldSigsAll.filterKeys { key ->
                key == rootLabel || key.startsWith("$rootLabel/")
            }

            val rootFp = fingerprintFromDocs(mp3Files.map { it.first })
            val lastFp = userPreferences.settings.first().libraryFingerprint
            val hasLibrary = trackDao.getTrackCount() > 0

            // Whole-tree quick out: nothing changed at all
            if (incremental && hasLibrary && rootFp == lastFp && oldSigs.isNotEmpty() &&
                oldSigs.keys == newSigs.keys && oldSigs.all { (k, v) -> newSigs[k] == v }
            ) {
                _scanProgress.value = ScanState.Scanning(0, 0, "No changes in $rootLabel")
                if (reportFinished) {
                    _scanProgress.value = ScanState.Finished(trackDao.getTrackCount())
                    cancelScanNotification()
                }
                return@withContext
            }

            val changedFolders: Set<String>
            val removedFolders: Set<String>
            if (incremental && hasLibrary && oldSigs.isNotEmpty()) {
                changedFolders = newSigs.filter { (path, sig) -> oldSigs[path] != sig }.keys
                removedFolders = oldSigs.keys.filter { it !in newSigs }.toSet()
            } else {
                // Full scan (first time or forced)
                changedFolders = newSigs.keys
                removedFolders = emptySet()
            }

            // Files that need tag extraction
            val toExtract = if (changedFolders.size == newSigs.size && removedFolders.isEmpty() &&
                !(incremental && hasLibrary && oldSigs.isNotEmpty())
            ) {
                mp3Files // full
            } else {
                mp3Files.filter { it.second in changedFolders }
            }

            val total = toExtract.size
            if (mp3Files.isEmpty()) {
                // Tree empty — clear tracks under this root
                trackDao.deleteTracksUnderFolder(rootLabel)
                val merged = oldSigsAll.filterKeys { key ->
                    !(key == rootLabel || key.startsWith("$rootLabel/"))
                }
                userPreferences.setFolderSignatures(userPreferences.encodeFolderSignatures(merged))
                userPreferences.setLibraryFingerprint(rootFp)
                if (reportFinished) {
                    _scanProgress.value = ScanState.Finished(trackDao.getTrackCount())
                    cancelScanNotification()
                }
                return@withContext
            }

            if (total == 0 && removedFolders.isEmpty()) {
                // Signatures updated only (shouldn't happen often)
                val merged = oldSigsAll.filterKeys { key ->
                    !(key == rootLabel || key.startsWith("$rootLabel/"))
                } + newSigs
                userPreferences.setFolderSignatures(userPreferences.encodeFolderSignatures(merged))
                userPreferences.setLibraryFingerprint(rootFp)
                if (reportFinished) {
                    _scanProgress.value = ScanState.Finished(trackDao.getTrackCount())
                    cancelScanNotification()
                }
                return@withContext
            }

            val skipMsg = if (incremental && changedFolders.size < newSigs.size) {
                " (${newSigs.size - changedFolders.size} folders unchanged, skipped)"
            } else ""
            _scanProgress.value = ScanState.Scanning(
                0, total,
                "Updating ${changedFolders.size} folder(s), $total file(s)$skipMsg"
            )
            notifyScan(0, total, "Reading tags…")

            val tracks = mutableListOf<Track>()
            toExtract.forEachIndexed { index, (doc, folderPath) ->
                val fileName = doc.name ?: "unknown.mp3"
                _scanProgress.value = ScanState.Scanning(index + 1, total, fileName)
                notifyScan(index + 1, total, fileName)
                metadataExtractor.extract(doc.uri, fileName, folderPath)?.let { tracks.add(it) }
            }

            // Remove tracks only from changed/removed folders (keep unchanged folders as-is)
            if (!incremental || oldSigs.isEmpty() || !hasLibrary) {
                trackDao.deleteTracksUnderFolder(rootLabel)
            } else {
                // Exact folder only — do not wipe child folders that were unchanged
                for (folder in changedFolders) {
                    trackDao.deleteTracksInFolderExact(folder)
                }
                for (folder in removedFolders) {
                    trackDao.deleteTracksInFolderExact(folder)
                }
            }
            if (tracks.isNotEmpty()) {
                trackDao.insertTracks(tracks)
            }

            // Persist signatures for this root merged with other roots
            val merged = oldSigsAll.filterKeys { key ->
                !(key == rootLabel || key.startsWith("$rootLabel/"))
            } + newSigs
            userPreferences.setFolderSignatures(userPreferences.encodeFolderSignatures(merged))
            userPreferences.setLibraryFingerprint(rootFp)
            if (reportFinished) {
                _scanProgress.value = ScanState.Finished(trackDao.getTrackCount())
                cancelScanNotification()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _scanProgress.value = ScanState.Error(e.message ?: "Scan failed")
                cancelScanNotification()
        } finally {
            if (scanMutex.isLocked) scanMutex.unlock()
        }
    }

    /**
     * Quick check: list MP3s under [treeUri] and compare fingerprint to last scan.
     * Returns true if a full rescan was started (or needed).
     */
    suspend fun scanTreeIfChanged(
        treeUri: Uri,
        displayName: String,
        isDefault: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val excluded = userPreferences.settings.first().excludedFolders
                .map { normalizePath(it) }.filter { it.isNotBlank() }.toSet()
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            val rootLabel = root.name?.takeIf { it.isNotBlank() } ?: "Root"
            val mp3Files = mutableListOf<Pair<DocumentFile, String>>()
            collectMp3sViaContract(treeUri, rootLabel, mp3Files, excluded)
            if (mp3Files.isEmpty()) {
                collectMp3sDocumentFile(root, rootLabel, mp3Files, excluded)
            }
            val fp = fingerprintFromDocs(mp3Files.map { it.first })
            val last = userPreferences.settings.first().libraryFingerprint
            if (fp == last && trackDao.getTrackCount() > 0) {
                return@withContext false // no change
            }
            // Changed — incremental folder rescan
            scanTree(treeUri, displayName, isDefault, emptySet(), incremental = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Force-rescan every saved source folder (Settings) as one continuous job. */
    suspend fun rescanAllSources() = withContext(Dispatchers.IO) {
        val sources = trackDao.getSourcesList()
        if (sources.isEmpty()) {
            scanDefaultMusicFolder()
            return@withContext
        }
        _scanProgress.value = ScanState.Scanning(0, 0, "Rescanning ${sources.size} source(s)…")
        sources.forEachIndexed { index, src ->
            val label = "Source ${index + 1}/${sources.size}: ${src.displayName}"
            _scanProgress.value = ScanState.Scanning(index, sources.size, label)
            try {
                if (src.uri.startsWith("mediastore:")) {
                    // Inner scan must not release overall UI as Finished mid-loop
                    scanDefaultMusicFolder(reportFinished = false)
                } else {
                    val uri = Uri.parse(src.uri)
                    scanTree(
                        uri,
                        src.displayName,
                        src.isDefault,
                        emptySet(),
                        isCloud = src.isCloud,
                        reportFinished = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _scanProgress.value = ScanState.Finished(trackDao.getTrackCount())
                cancelScanNotification()
    }

    /** Signature for one folder: file count + total size + hash of name/size/mtime. */
    private fun folderContentSignature(docs: List<DocumentFile>): String {
        val parts = docs.mapNotNull { d ->
            val name = d.name ?: return@mapNotNull null
            val len = try { d.length() } catch (_: Exception) { 0L }
            val mod = try { d.lastModified() } catch (_: Exception) { 0L }
            "$name:$len:$mod"
        }.sorted()
        var totalBytes = 0L
        for (d in docs) {
            totalBytes += try { d.length() } catch (_: Exception) { 0L }
        }
        return "${parts.size}:$totalBytes:" + parts.joinToString("|").hashCode()
    }
    private fun fingerprintFromDocs(docs: List<DocumentFile>): String {
        // Stable signature of library contents without reading tags
        val parts = docs.mapNotNull { d ->
            val name = d.name ?: return@mapNotNull null
            val len = try { d.length() } catch (_: Exception) { 0L }
            val mod = try { d.lastModified() } catch (_: Exception) { 0L }
            "$name|$len|$mod"
        }.sorted()
        return parts.size.toString() + ":" + parts.joinToString(";").hashCode().toString()
    }

    suspend fun scanDefaultMusicFolder(
        skipFolders: Set<String> = emptySet(),
        reportFinished: Boolean = true
    ) =
        withContext(Dispatchers.IO) {
            if (!scanMutex.tryLock()) return@withContext
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
                        notifyScan(index, total, fileName)
                        metadataExtractor.extract(uri, fileName, folderPath)?.let { tracks.add(it) }
                    }
                    // Single pass only — already filtered to .mp3 above.
                    // (Previously a second full pass over IS_MUSIC made the UI look like a double scan.)
                }

                // Merge MediaStore results (REPLACE by unique uri)
                trackDao.insertTracks(tracks)
                trackDao.insertSource(
                    SourceFolder(
                        uri = "mediastore://music",
                        displayName = "Internal storage/Music",
                        isDefault = true,
                        lastScanned = System.currentTimeMillis()
                    )
                )
                val fp = "mediastore:" + tracks.size + ":" + tracks.map { it.uri }.sorted().hashCode()
                userPreferences.setLibraryFingerprint(fp)
                if (reportFinished) {
                    _scanProgress.value = ScanState.Finished(trackDao.getTrackCount())
                cancelScanNotification()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _scanProgress.value = ScanState.Error(e.message ?: "MediaStore scan failed")
                cancelScanNotification()
            } finally {
                if (scanMutex.isLocked) scanMutex.unlock()
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
