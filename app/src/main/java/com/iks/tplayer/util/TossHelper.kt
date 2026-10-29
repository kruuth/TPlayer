package com.iks.tplayer.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.widget.Toast
import androidx.core.content.FileProvider
import com.iks.tplayer.data.model.Track
import java.io.File

/**
 * "Toss" the currently playing track to another device.
 *
 * Full MP3 files cannot be transferred over NFC alone (payload size / speed limits).
 * This uses the system share sheet so the user can pick Nearby Share, Bluetooth,
 * messaging apps, etc. If NFC hardware is present, a short tip is shown to hold
 * phones together and use Nearby Share / the peer app.
 */
object TossHelper {

    fun tossTrack(context: Context, track: Track?) {
        if (track == null) {
            Toast.makeText(context, "Nothing playing to toss", Toast.LENGTH_SHORT).show()
            return
        }
        val shareUri = resolveShareableUri(context, track) ?: run {
            Toast.makeText(context, "Cannot access this file for sharing", Toast.LENGTH_SHORT).show()
            return
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, track.displayTitle)
            putExtra(
                Intent.EXTRA_TEXT,
                "${track.displayTitle} — ${track.displayArtist}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, track.displayTitle, shareUri)
        }

        val chooser = Intent.createChooser(send, "Toss “${track.displayTitle}”").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val nfc = NfcAdapter.getDefaultAdapter(context)
        val tip = when {
            nfc == null -> null
            !nfc.isEnabled -> "Turn on NFC, then choose Nearby Share to toss"
            else -> "Hold phones together, then choose Nearby Share"
        }
        if (tip != null) {
            Toast.makeText(context, tip, Toast.LENGTH_LONG).show()
        }

        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Toss failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveShareableUri(context: Context, track: Track): Uri? {
        val raw = track.uri.trim()
        if (raw.isEmpty()) return null
        val parsed = Uri.parse(raw)
        return when (parsed.scheme?.lowercase()) {
            "content" -> parsed
            "file" -> {
                val path = parsed.path ?: return null
                val file = File(path)
                if (!file.exists()) return null
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
            else -> {
                // Plain filesystem path
                val file = File(raw)
                if (!file.exists()) return null
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
        }
    }
}
