package com.grok.tplayer.ui.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grok.tplayer.data.model.Track
import com.grok.tplayer.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackListViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private var loadJob: Job? = null

    fun load(mode: String, key: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val flow = when (mode) {
                "artist" -> repository.getTracksByArtist(key)
                "year" -> repository.getTracksByYear(key.toIntOrNull() ?: 0)
                "folder" -> repository.getTracksByFolder(key)
                else -> repository.getAllTracks()
            }
            flow.collect { _tracks.value = it }
        }
    }
}
