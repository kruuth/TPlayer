package com.iks.tplayer.ui.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iks.tplayer.data.model.Playlist
import com.iks.tplayer.data.model.Track
import com.iks.tplayer.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackListViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var loadJob: Job? = null
    private var loadedMode: String? = null
    private var loadedKey: String? = null

    fun load(mode: String, key: String) {
        // Already collecting this exact list — avoid cancel/restart flicker
        if (mode == loadedMode && key == loadedKey && loadJob?.isActive == true) return

        loadJob?.cancel()
        loadedMode = mode
        loadedKey = key
        loadJob = viewModelScope.launch {
            val flow = when (mode) {
                "artist" -> repository.getTracksByArtist(key)
                "year" -> repository.getTracksByYear(key.toIntOrNull() ?: 0)
                "folder" -> repository.getTracksByFolder(key)
                "playlist" -> repository.getTracksForPlaylist(key.toLongOrNull() ?: -1L)
                else -> repository.getAllTracks()
            }
            // Skip emissions that are identical by track ids + order
            flow
                .distinctUntilChanged { a, b ->
                    a.size == b.size && a.indices.all { i -> a[i].id == b[i].id && a[i].uri == b[i].uri }
                }
                .collect { list ->
                    _tracks.value = list
                }
        }
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    /** Create a playlist and immediately add [trackId] to it. */
    fun createPlaylistAndAdd(name: String, trackId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addTrackToPlaylist(id, trackId)
            onDone()
        }
    }

    /** Queue = this playlist + following playlists until none remain. */
    suspend fun queueForPlaylistPlay(playlistId: Long): List<Track> =
        repository.buildPlaylistChainQueue(playlistId)
}
