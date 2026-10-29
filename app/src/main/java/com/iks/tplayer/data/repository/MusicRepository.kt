package com.iks.tplayer.data.repository

import android.net.Uri
import com.iks.tplayer.data.db.PlaylistDao
import com.iks.tplayer.data.db.PlaylistTrackRow
import com.iks.tplayer.data.db.TrackDao
import com.iks.tplayer.data.model.AlbumArt
import com.iks.tplayer.data.model.Playlist
import com.iks.tplayer.data.model.PlaylistEntry
import com.iks.tplayer.data.model.SourceFolder
import com.iks.tplayer.data.model.Track
import com.iks.tplayer.data.scanner.LibraryScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
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
    fun getCloudSources(): Flow<List<SourceFolder>> = trackDao.getCloudSources()
    suspend fun getCloudSourcesList() = trackDao.getCloudSourcesList()
    suspend fun removeSource(uri: String) = trackDao.deleteSource(uri)
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
        skipFolders: Set<String> = emptySet(),
        isCloud: Boolean = false
    ) = scanner.scanTree(uri, name, isDefault, skipFolders, isCloud)

    suspend fun scanTreeIfChanged(uri: Uri, name: String, isDefault: Boolean = false) =
        scanner.scanTreeIfChanged(uri, name, isDefault)

    suspend fun rescanAllSources() = scanner.rescanAllSources()

    // —— Playlists ——
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> =
        playlistDao.getTracksForPlaylist(playlistId)
    fun getPlaylistEntries(playlistId: Long): Flow<List<PlaylistTrackRow>> =
        playlistDao.getEntriesWithTracks(playlistId)

    suspend fun createPlaylist(name: String): Long {
        val order = playlistDao.maxSortOrder() + 1
        return playlistDao.insertPlaylist(Playlist(name = name.trim().ifBlank { "Playlist" }, sortOrder = order))
    }

    suspend fun renamePlaylist(id: Long, newName: String) {
        val p = playlistDao.getPlaylist(id) ?: return
        playlistDao.updatePlaylist(p.copy(name = newName.trim().ifBlank { p.name }))
    }

    suspend fun deletePlaylist(id: Long) {
        playlistDao.deleteEntriesForPlaylist(id)
        playlistDao.deletePlaylist(id)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        val pos = playlistDao.maxPosition(playlistId) + 1
        playlistDao.insertEntry(PlaylistEntry(playlistId = playlistId, trackId = trackId, position = pos))
    }

    suspend fun removePlaylistEntry(entryId: Long) {
        playlistDao.deleteEntry(entryId)
    }

    /**
     * Queue for playback starting at [startPlaylistId]: that playlist's tracks,
     * then each following playlist in sort order, until none remain.
     */
    suspend fun buildPlaylistChainQueue(startPlaylistId: Long): List<Track> {
        val all = playlistDao.getAllPlaylistsList()
        val startIdx = all.indexOfFirst { it.id == startPlaylistId }.let { if (it >= 0) it else 0 }
        val out = mutableListOf<Track>()
        for (i in startIdx until all.size) {
            out += playlistDao.getTracksForPlaylistList(all[i].id)
        }
        return out
    }

    suspend fun getPlaylist(id: Long) = playlistDao.getPlaylist(id)
}
