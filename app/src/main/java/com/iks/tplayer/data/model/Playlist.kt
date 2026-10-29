package com.iks.tplayer.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Display / playback order among playlists */
    val sortOrder: Int = 0
)

/**
 * One row per song placement in a playlist.
 * The same [trackId] may appear multiple times (user request).
 */
@Entity(
    tableName = "playlist_entries",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
        // No FK on trackId: tracks may be rescanned/replaced; keep entry for now
    ],
    indices = [
        Index("playlistId"),
        Index("trackId")
    ]
)
data class PlaylistEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val trackId: Long,
    val position: Int
)
