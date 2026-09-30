package com.grok.tplayer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["year"]),
        Index(value = ["folderPath"]),
        Index(value = ["uri"], unique = true)
    ]
)
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,                     // content:// or file URI string
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val year: Int?,
    val trackNumber: Int?,
    val composer: String?,
    val comment: String?,
    val durationMs: Long,
    val folderPath: String,              // relative or absolute folder for grouping
    val albumArtPath: String? = null,    // path to cached primary (front cover) image
    val hasMultipleArts: Boolean = false // flag if extra images exist
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast(".")

    val displayArtist: String
        get() = artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist"

    val displayAlbum: String
        get() = album?.takeIf { it.isNotBlank() } ?: "Unknown Album"
}
