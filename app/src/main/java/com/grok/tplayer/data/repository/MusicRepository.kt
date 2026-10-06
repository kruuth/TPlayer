package com.grok.tplayer.data.repository

import android.net.Uri
import com.grok.tplayer.data.db.TrackDao
import com.grok.tplayer.data.model.AlbumArt
import com.grok.tplayer.data.model.SourceFolder
import com.grok.tplayer.data.model.Track
import com.grok.tplayer.data.scanner.LibraryScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val scanner: LibraryScanner
) {
    val scanProgress: StateFlow<LibraryScanner.ScanState> = scanner.scanProgress

    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()
    fun getAllArtists(): Flow<List<String>> = trackDao.getAllArtists()
    fun getTracksByArtist(artist: String): Flow<List<Track>> = trackDao.getTracksByArtist(artist)
    fun getAlbumsByArtist(artist: String): Flow<List<String>> = trackDao.getAlbumsByArtist(artist)
    fun getTracksByAlbum(artist: String, album: String): Flow<List<Track>> =
        trackDao.getTracksByAlbum(artist, album)
    fun getAllYears(): Flow<List<Int>> = trackDao.getAllYears()
    fun getTracksByYear(year: Int): Flow<List<Track>> = trackDao.getTracksByYear(year)
    fun getAllFolders(): Flow<List<String>> = trackDao.getAllFolders()
    fun getTracksByFolder(folder: String): Flow<List<Track>> = trackDao.getTracksByFolder(folder)
    fun getSources(): Flow<List<SourceFolder>> = trackDao.getSources()
    fun searchTracks(q: String): Flow<List<Track>> = trackDao.searchTracks(q)
    suspend fun getAllTracksList(): List<Track> = trackDao.getAllTracksList()

    suspend fun getTrackById(id: Long): Track? = trackDao.getTrackById(id)
    suspend fun getArtsForTrack(trackId: Long): List<AlbumArt> = trackDao.getArtsForTrack(trackId)
    suspend fun getTrackCount(): Int = trackDao.getTrackCount()

    suspend fun listSubfolders(uri: Uri): List<String> = scanner.listSubfolders(uri)

    suspend fun scanDefault(skipFolders: Set<String> = emptySet()) =
        scanner.scanDefaultMusicFolder(skipFolders)

    suspend fun scanTree(
        uri: Uri,
        name: String,
        isDefault: Boolean = false,
        skipFolders: Set<String> = emptySet()
    ) = scanner.scanTree(uri, name, isDefault, skipFolders)
}
