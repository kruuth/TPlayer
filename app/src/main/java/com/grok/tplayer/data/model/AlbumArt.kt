package com.grok.tplayer.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "album_arts",
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class AlbumArt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val artPath: String,          // cached file path
    val pictureType: Int = 3,     // 3 = front cover (ID3 APIC)
    val isPrimary: Boolean = false
)
