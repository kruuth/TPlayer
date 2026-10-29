package com.iks.tplayer.di

import android.content.Context
import androidx.room.Room
import com.iks.tplayer.data.db.AppDatabase
import com.iks.tplayer.data.db.Migrations
import com.iks.tplayer.data.db.PlaylistDao
import com.iks.tplayer.data.db.TrackDao
import com.iks.tplayer.data.scanner.MetadataExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "tplayer.db")
            .addMigrations(*Migrations.ALL)
            // Only wipe if someone installs an older schema over a newer one
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideTrackDao(db: AppDatabase): TrackDao = db.trackDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    @Singleton
    fun provideMetadataExtractor(@ApplicationContext context: Context): MetadataExtractor =
        MetadataExtractor(context)
}
