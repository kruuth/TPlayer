package com.iks.tplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.iks.tplayer.data.model.Playlist
import com.iks.tplayer.data.model.PlaylistEntry
import com.iks.tplayer.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAllPlaylistsList(): List<Playlist>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): Playlist?

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM playlists")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insertEntry(entry: PlaylistEntry): Long

    @Query("DELETE FROM playlist_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun deleteEntriesForPlaylist(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    /**
     * Tracks in playlist order. Duplicate trackIds produce duplicate rows
     * (joined on entry id for uniqueness of placement).
     */
    @Query(
        """
        SELECT tracks.* FROM playlist_entries
        INNER JOIN tracks ON tracks.id = playlist_entries.trackId
        WHERE playlist_entries.playlistId = :playlistId
        ORDER BY playlist_entries.position ASC, playlist_entries.id ASC
        """
    )
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>>

    @Query(
        """
        SELECT tracks.* FROM playlist_entries
        INNER JOIN tracks ON tracks.id = playlist_entries.trackId
        WHERE playlist_entries.playlistId = :playlistId
        ORDER BY playlist_entries.position ASC, playlist_entries.id ASC
        """
    )
    suspend fun getTracksForPlaylistList(playlistId: Long): List<Track>

    @Query(
        """
        SELECT playlist_entries.id AS entryId, playlist_entries.position AS position,
               tracks.id AS trackId, tracks.uri AS uri, tracks.fileName AS fileName,
               tracks.title AS title, tracks.artist AS artist, tracks.album AS album,
               tracks.year AS year, tracks.trackNumber AS trackNumber,
               tracks.composer AS composer, tracks.comment AS comment,
               tracks.durationMs AS durationMs, tracks.folderPath AS folderPath,
               tracks.albumArtPath AS albumArtPath, tracks.hasMultipleArts AS hasMultipleArts
        FROM playlist_entries
        INNER JOIN tracks ON tracks.id = playlist_entries.trackId
        WHERE playlist_entries.playlistId = :playlistId
        ORDER BY playlist_entries.position ASC, playlist_entries.id ASC
        """
    )
    fun getEntriesWithTracks(playlistId: Long): Flow<List<PlaylistTrackRow>>

    @Query("SELECT COUNT(*) FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun entryCount(playlistId: Long): Int
}

/** Row for edit UI: entry id + track fields */
data class PlaylistTrackRow(
    val entryId: Long,
    val position: Int,
    val trackId: Long,
    val uri: String,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val year: Int?,
    val trackNumber: Int?,
    val composer: String?,
    val comment: String?,
    val durationMs: Long,
    val folderPath: String,
    val albumArtPath: String?,
    val hasMultipleArts: Boolean
) {
    val displayTitle: String
        get() = (title?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast(".")).trim()

    val displayArtist: String
        get() = (artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist").trim()

    fun toTrack(): Track = Track(
        id = trackId,
        uri = uri,
        fileName = fileName,
        title = title,
        artist = artist,
        album = album,
        year = year,
        trackNumber = trackNumber,
        composer = composer,
        comment = comment,
        durationMs = durationMs,
        folderPath = folderPath,
        albumArtPath = albumArtPath,
        hasMultipleArts = hasMultipleArts
    )
}
