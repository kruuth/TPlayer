package com.grok.tplayer.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.grok.tplayer.data.model.Track
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts ID3-like metadata and embedded artwork using MediaMetadataRetriever.
 * Note: MediaMetadataRetriever returns only ONE embedded picture.
 * For full multi-image support a native TagLib binding is recommended later.
 */
class MetadataExtractor(private val context: Context) {

    data class ExtractedMeta(
        val title: String?,
        val artist: String?,
        val album: String?,
        val year: Int?,
        val trackNumber: Int?,
        val composer: String?,
        val comment: String?,
        val durationMs: Long,
        val albumArtBytes: ByteArray?
    )

    fun extract(uri: Uri, fileName: String, folderPath: String): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val meta = ExtractedMeta(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                        ?.take(4)?.toIntOrNull(),
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    ?.substringBefore("/")?.toIntOrNull(),
                composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
                comment = null, // MediaMetadataRetriever has no reliable COMMENT key
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                albumArtBytes = retriever.embeddedPicture
            )

            val artPath = meta.albumArtBytes?.let { bytes ->
                cacheArt(bytes, fileName)
            }

            Track(
                uri = uri.toString(),
                fileName = fileName,
                title = meta.title,
                artist = meta.artist,
                album = meta.album,
                year = meta.year,
                trackNumber = meta.trackNumber,
                composer = meta.composer,
                comment = meta.comment,
                durationMs = meta.durationMs,
                folderPath = folderPath,
                albumArtPath = artPath,
                hasMultipleArts = false // single image only with this extractor
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun cacheArt(bytes: ByteArray, fileName: String): String? {
        return try {
            val dir = File(context.cacheDir, "album_art")
            if (!dir.exists()) dir.mkdirs()
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val file = File(dir, "${safeName.hashCode()}_cover.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadArtBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            BitmapFactory.decodeFile(path)
        } catch (_: Exception) {
            null
        }
    }
}
