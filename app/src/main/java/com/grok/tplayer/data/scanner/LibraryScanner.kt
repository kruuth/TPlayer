package com.grok.tplayer.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.grok.tplayer.data.db.TrackDao
import com.grok.tplayer.data.model.SourceFolder
import com.grok.tplayer.data.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val metadataExtractor: MetadataExtractor
) {
    private val _scanProgress = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanProgress: StateFlow<ScanState> = _scanProgress

    sealed class ScanState {
        data object Idle : ScanState()
        data class Scanning(val current: Int, val total: Int, val fileName: String) : ScanState()
        data class Finished(val count: Int) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    /**
     * Scan a SAF tree URI recursively for MP3 files.
     */
    suspend fun scanTree(treeUri: Uri, displayName: String, isDefault: Boolean = false) =
        withContext(Dispatchers.IO) {
            try {
                _scanProgress.value = ScanState.Scanning(0, 0, "Preparing…")

                // Persist permission
                try {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // may already be persisted
                }

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
                        _scanProgress.value = ScanState.Error("Cannot open folder")
                        return@withContext
                    }

                val mp3Files = mutableListOf<Pair<DocumentFile, String>>()
                collectMp3s(root, root.name ?: "Root", mp3Files)

                val total = mp3Files.size
                val tracks = mutableListOf<Track>()

                mp3Files.forEachIndexed { index, (doc, folderPath) ->
                    _scanProgress.value = ScanState.Scanning(index + 1, total, doc.name ?: "")
                    val uri = doc.uri
                    val fileName = doc.name ?: "unknown.mp3"
                    metadataExtractor.extract(uri, fileName, folderPath)?.let { tracks.add(it) }
                }

                // Replace library (simple strategy: clear + insert)
                trackDao.clearAllTracks()
                trackDao.insertTracks(tracks)

                _scanProgress.value = ScanState.Finished(tracks.size)
            } catch (e: Exception) {
                e.printStackTrace()
                _scanProgress.value = ScanState.Error(e.message ?: "Scan failed")
            }
        }

    /**
     * Scan the default public Music directory via MediaStore / direct path when possible.
     * Falls back to asking user for SAF if needed.
     */
    suspend fun scanDefaultMusicFolder() = withContext(Dispatchers.IO) {
        // For simplicity we still prefer SAF; caller can open Music folder via SAF
        // or we can query MediaStore.Audio.Media
        try {
            _scanProgress.value = ScanState.Scanning(0, 0, "Scanning MediaStore…")
            val tracks = mutableListOf<Track>()
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.DISPLAY_NAME,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.ALBUM,
                android.provider.MediaStore.Audio.Media.YEAR,
                android.provider.MediaStore.Audio.Media.TRACK,
                android.provider.MediaStore.Audio.Media.DURATION,
                android.provider.MediaStore.Audio.Media.DATA,
                android.provider.MediaStore.Audio.Media.RELATIVE_PATH
            )
            val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${android.provider.MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/mpeg%'"
            context.contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
                val yearCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.YEAR)
                val trackCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TRACK)
                val durCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                val pathCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.RELATIVE_PATH)

                val total = cursor.count
                var index = 0
                while (cursor.moveToNext()) {
                    index++
                    val id = cursor.getLong(idCol)
                    val uri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val fileName = cursor.getString(nameCol) ?: "unknown.mp3"
                    _scanProgress.value = ScanState.Scanning(index, total, fileName)

                    // Prefer full extraction for art + missing fields
                    val extracted = metadataExtractor.extract(uri, fileName, cursor.getString(pathCol) ?: "Music")
                    if (extracted != null) {
                        tracks.add(extracted)
                    } else {
                        tracks.add(
                            Track(
                                uri = uri.toString(),
                                fileName = fileName,
                                title = cursor.getString(titleCol),
                                artist = cursor.getString(artistCol),
                                album = cursor.getString(albumCol),
                                year = if (yearCol >= 0) cursor.getInt(yearCol).takeIf { it > 0 } else null,
                                trackNumber = if (trackCol >= 0) cursor.getInt(trackCol).takeIf { it > 0 } else null,
                                composer = null,
                                comment = null,
                                durationMs = cursor.getLong(durCol),
                                folderPath = cursor.getString(pathCol) ?: "Music"
                            )
                        )
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

    private fun collectMp3s(
        dir: DocumentFile,
        currentPath: String,
        out: MutableList<Pair<DocumentFile, String>>
    ) {
        dir.listFiles().forEach { file ->
            if (file.isDirectory) {
                collectMp3s(file, "$currentPath/${file.name}", out)
            } else if (file.isFile && file.name?.endsWith(".mp3", ignoreCase = true) == true) {
                out.add(file to currentPath)
            }
        }
    }
}
