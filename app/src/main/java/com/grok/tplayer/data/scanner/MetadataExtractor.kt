package com.grok.tplayer.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.grok.tplayer.data.model.Track
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Extracts metadata and caches embedded artwork on disk (persistent filesDir).
 * Same image bytes are stored once and reused across tracks (content-hash key).
 * Artist+album key is also used so tracks in the same album share one cover file.
 */
class MetadataExtractor(private val context: Context) {

    private val artDir: File by lazy {
        File(context.filesDir, "album_art").also { if (!it.exists()) it.mkdirs() }
    }

    // In-memory map for one scan session: contentHash → path
    private val hashToPath = mutableMapOf<String, String>()
    // albumKey (artist|album) → path
    private val albumKeyToPath = mutableMapOf<String, String>()

    fun extract(uri: Uri, fileName: String, folderPath: String): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                    ?.take(4)?.toIntOrNull()
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore("/")?.toIntOrNull()
            val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val artBytes = retriever.embeddedPicture

            val artPath = cacheArt(artBytes, artist, album, fileName)

            Track(
                uri = uri.toString(),
                fileName = fileName,
                title = title,
                artist = artist,
                album = album,
                year = year,
                trackNumber = trackNumber,
                composer = composer,
                comment = null,
                durationMs = durationMs,
                folderPath = folderPath,
                albumArtPath = artPath,
                hasMultipleArts = false
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

    /**
     * Cache artwork:
     * 1) Reuse by artist|album if we already have a cover for that album
     * 2) Reuse by content hash if same image already on disk
     * 3) Otherwise write once under filesDir/album_art/
     */
    private fun cacheArt(
        bytes: ByteArray?,
        artist: String?,
        album: String?,
        fileName: String
    ): String? {
        if (bytes == null || bytes.isEmpty()) return null

        val albumKey = albumKey(artist, album)
        albumKey?.let { key ->
            albumKeyToPath[key]?.let { existing ->
                if (File(existing).exists()) return existing
            }
            // Check disk for prior album file
            val albumFile = File(artDir, "album_${key.hashCode()}.jpg")
            if (albumFile.exists() && albumFile.length() > 0) {
                albumKeyToPath[key] = albumFile.absolutePath
                return albumFile.absolutePath
            }
        }

        val hash = sha1(bytes)
        hashToPath[hash]?.let { existing ->
            if (File(existing).exists()) {
                albumKey?.let { albumKeyToPath[it] = existing }
                return existing
            }
        }

        val hashFile = File(artDir, "hash_$hash.jpg")
        if (hashFile.exists() && hashFile.length() > 0) {
            hashToPath[hash] = hashFile.absolutePath
            albumKey?.let { albumKeyToPath[it] = hashFile.absolutePath }
            return hashFile.absolutePath
        }

        return try {
            // Prefer album-keyed filename when we have album metadata
            val outFile = if (albumKey != null) {
                File(artDir, "album_${albumKey.hashCode()}.jpg")
            } else {
                hashFile
            }
            if (!outFile.exists() || outFile.length() == 0L) {
                FileOutputStream(outFile).use { it.write(bytes) }
            }
            val path = outFile.absolutePath
            hashToPath[hash] = path
            albumKey?.let { albumKeyToPath[it] = path }
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun albumKey(artist: String?, album: String?): String? {
        val a = album?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val ar = artist?.trim()?.takeIf { it.isNotEmpty() } ?: "unknown"
        return "${ar.lowercase()}|$a.lowercase()"
    }

    private fun sha1(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun loadArtBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            BitmapFactory.decodeFile(path)
        } catch (_: Exception) {
            null
        }
    }

    /** Clear in-memory maps (call between full rescans if desired). */
    fun clearSessionCache() {
        hashToPath.clear()
        albumKeyToPath.clear()
    }
}
