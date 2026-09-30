package com.grok.tplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "source_folders")
data class SourceFolder(
    @PrimaryKey val uri: String,          // tree URI string
    val displayName: String,
    val isDefault: Boolean = false,
    val lastScanned: Long = 0L
)
