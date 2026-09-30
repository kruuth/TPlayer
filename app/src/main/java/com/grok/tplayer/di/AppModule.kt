package com.grok.tplayer.di

import android.content.Context
import androidx.room.Room
import com.grok.tplayer.data.db.AppDatabase
import com.grok.tplayer.data.db.TrackDao
import com.grok.tplayer.data.scanner.MetadataExtractor
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTrackDao(db: AppDatabase): TrackDao = db.trackDao()

    @Provides
    @Singleton
    fun provideMetadataExtractor(@ApplicationContext context: Context): MetadataExtractor =
        MetadataExtractor(context)
}
