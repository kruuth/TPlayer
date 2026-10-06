package com.grok.tplayer.ui.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.grok.tplayer.data.model.Track
import com.grok.tplayer.player.PlayerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    mode: String,
    key: String,
    playerController: PlayerController,
    onTrackSelected: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenPlayer: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    viewModel: TrackListViewModel = hiltViewModel()
) {
    LaunchedEffect(mode, key) {
        viewModel.load(mode, key)
    }

    val tracks by viewModel.tracks.collectAsState()
    var sortMode by remember { mutableStateOf(SortMode.ALPHA) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedTracks = remember(tracks, sortMode) {
        when (sortMode) {
            SortMode.ALPHA -> tracks.sortedBy { it.displayTitle.lowercase() }
            SortMode.TRACK_NUM -> tracks.sortedWith(
                compareBy<Track> { it.trackNumber ?: Int.MAX_VALUE }
                    .thenBy { it.displayTitle.lowercase() }
            )
            SortMode.YEAR -> tracks.sortedWith(
                compareByDescending<Track> { it.year ?: 0 }
                    .thenBy { it.displayTitle.lowercase() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (mode) {
                            "artist" -> key
                            "year" -> "Year $key"
                            "folder" -> key
                            else -> "All Tracks"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(onClick = onOpenPlayer) {
                        Icon(Icons.Default.PlayArrow, "Now Playing")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, "Sort")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.MoreVert, "Settings")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Alphabetical", maxLines = 1, softWrap = false) },
                            onClick = { sortMode = SortMode.ALPHA; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("By Track Number", maxLines = 1, softWrap = false) },
                            onClick = { sortMode = SortMode.TRACK_NUM; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("By Year", maxLines = 1, softWrap = false) },
                            onClick = { sortMode = SortMode.YEAR; showSortMenu = false }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            itemsIndexed(sortedTracks) { index, track ->
                ListItem(
                    headlineContent = {
                        Text(
                            track.displayTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    supportingContent = {
                        Text(
                            "${track.displayArtist} • ${track.displayAlbum}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.clickable {
                        playerController.setQueue(sortedTracks, index)
                        playerController.play()
                        onTrackSelected()
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

enum class SortMode { ALPHA, TRACK_NUM, YEAR }
