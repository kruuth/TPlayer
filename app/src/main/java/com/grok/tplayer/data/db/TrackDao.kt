package com.grok.tplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grok.tplayer.data.model.AlbumArt
import com.grok.tplayer.data.model.SourceFolder
import com.grok.tplayer.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    // Tracks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): Track?

    @Query("SELECT DISTINCT artist FROM tracks WHERE artist IS NOT NULL AND artist != '' ORDER BY artist COLLATE NOCASE ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getTracksByArtist(artist: String): Flow<List<Track>>

    @Query("SELECT DISTINCT album FROM tracks WHERE artist = :artist AND album IS NOT NULL ORDER BY album COLLATE NOCASE ASC")
    fun getAlbumsByArtist(artist: String): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE album = :album AND artist = :artist ORDER BY trackNumber ASC, title COLLATE NOCASE ASC")
    fun getTracksByAlbum(artist: String, album: String): Flow<List<Track>>

    @Query("SELECT DISTINCT year FROM tracks WHERE year IS NOT NULL ORDER BY year DESC")
    fun getAllYears(): Flow<List<Int>>

    @Query("SELECT * FROM tracks WHERE year = :year ORDER BY artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC, trackNumber ASC")
    fun getTracksByYear(year: Int): Flow<List<Track>>

    @Query("SELECT DISTINCT folderPath FROM tracks ORDER BY folderPath COLLATE NOCASE ASC")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE folderPath = :folder OR folderPath LIKE :folder || '/%' ORDER BY title COLLATE NOCASE ASC")
    fun getTracksByFolder(folder: String): Flow<List<Track>>

    @Query("DELETE FROM tracks")
    suspend fun clearAllTracks()

    /** Remove tracks whose folder path is under [prefix] (for re-scan of one tree). */
    @Query("DELETE FROM tracks WHERE folderPath = :prefix OR folderPath LIKE :prefix || '/%'")
    suspend fun deleteTracksUnderFolder(prefix: String)

    @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
    suspend fun getTrackByUri(uri: String): Track?

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int

    @Query("SELECT * FROM source_folders")
    suspend fun getSourcesList(): List<SourceFolder>

    // Album Arts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumArt(art: AlbumArt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumArts(arts: List<AlbumArt>)

    @Query("SELECT * FROM album_arts WHERE trackId = :trackId ORDER BY isPrimary DESC, pictureType ASC")
    suspend fun getArtsForTrack(trackId: Long): List<AlbumArt>

    @Query("DELETE FROM album_arts WHERE trackId = :trackId")
    suspend fun deleteArtsForTrack(trackId: Long)

    // Source folders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceFolder)

    @Query("SELECT * FROM source_folders")
    fun getSources(): Flow<List<SourceFolder>>

    @Query("SELECT * FROM source_folders WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultSource(): SourceFolder?

    @Query("DELETE FROM source_folders")
    suspend fun clearSources()

    @Transaction
    suspend fun replaceLibrary(tracks: List<Track>, arts: List<AlbumArt>) {
        clearAllTracks()
        insertTracks(tracks)
        // arts need track IDs; caller should handle after insert if needed
    }
}
