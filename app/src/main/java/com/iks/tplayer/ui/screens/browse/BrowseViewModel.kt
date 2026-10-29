package com.iks.tplayer.ui.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iks.tplayer.data.preferences.UserPreferences
import com.iks.tplayer.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val preferences: UserPreferences
) : ViewModel() {
    val artists = repository.getAllArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val years = repository.getAllYears()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cloudSources = repository.getCloudSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val excludedFolders = preferences.settings
        .map { it.excludedFolders }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val lastBrowseTab = preferences.settings
        .map { it.lastBrowseTab }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setLastBrowseTab(index: Int) {
        viewModelScope.launch { preferences.setLastBrowseTab(index) }
    }


    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            onCreated(id)
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { repository.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { repository.deletePlaylist(id) }
    }
}
