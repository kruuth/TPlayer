package com.iks.tplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iks.tplayer.data.model.AlbumArt
import com.iks.tplayer.data.model.SourceFolder
import com.iks.tplayer.data.model.Track

@Database(
    entities = [Track::class, AlbumArt::class, SourceFolder::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}
