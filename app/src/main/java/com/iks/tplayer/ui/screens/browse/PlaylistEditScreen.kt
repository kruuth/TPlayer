package com.iks.tplayer.ui.screens.browse

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iks.tplayer.data.db.PlaylistTrackRow
import com.iks.tplayer.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistEditViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    private val _entries = MutableStateFlow<List<PlaylistTrackRow>>(emptyList())
    val entries: StateFlow<List<PlaylistTrackRow>> = _entries.asStateFlow()

    private val _title = MutableStateFlow("Playlist")
    val title: StateFlow<String> = _title.asStateFlow()

    private var collectJob: Job? = null

    fun load(id: Long) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            _title.value = repository.getPlaylist(id)?.name ?: "Playlist"
            repository.getPlaylistEntries(id).collect { _entries.value = it }
        }
    }

    fun removeEntry(entryId: Long) {
        viewModelScope.launch { repository.removePlaylistEntry(entryId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistEditScreen(
    playlistId: Long,
    onBack: () -> Unit,
    viewModel: PlaylistEditViewModel = hiltViewModel()
) {
    LaunchedEffect(playlistId) { viewModel.load(playlistId) }
    val entries by viewModel.entries.collectAsState()
    val title by viewModel.title.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit: $title",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (entries.isEmpty()) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                "No songs yet. Long-press any track in the library → Add to playlist."
                            )
                        }
                    )
                }
            }
            items(entries, key = { it.entryId }) { row ->
                ListItem(
                    headlineContent = {
                        Text(
                            row.displayTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    supportingContent = {
                        Text(
                            row.displayArtist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeEntry(row.entryId) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove from playlist")
                        }
                    }
                )
            }
        }
    }
}
