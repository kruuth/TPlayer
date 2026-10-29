package com.iks.tplayer.ui.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.iks.tplayer.data.model.Track
import com.iks.tplayer.player.PlayerController

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    val playlists by viewModel.playlists.collectAsState()
    val scope = rememberCoroutineScope()

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
                        Icon(Icons.Default.History, "Last played")
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
            itemsIndexed(sortedTracks, key = { _, track -> track.id }) { index, track ->
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
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            scope.launch {
                                if (mode == "playlist") {
                                    val id = key.toLongOrNull()
                                    if (id != null) {
                                        val queue = viewModel.queueForPlaylistPlay(id)
                                        val start = queue.indexOfFirst { it.id == track.id }.let {
                                            if (it >= 0) it else 0
                                        }
                                        playerController.setQueue(queue, start)
                                    } else {
                                        playerController.setQueue(sortedTracks, index)
                                    }
                                } else {
                                    playerController.setQueue(sortedTracks, index)
                                }
                                playerController.play()
                                onTrackSelected()
                            }
                        },
                        onLongClick = {
                            addToPlaylistTrack = track
                        }
                    )
                )
                HorizontalDivider()
            }
        }
    }

    addToPlaylistTrack?.let { track ->
        var newPlaylistName by remember(track.id) { mutableStateOf("") }
        var showCreateFields by remember(track.id) {
            mutableStateOf(playlists.isEmpty())
        }
        AlertDialog(
            onDismissRequest = { addToPlaylistTrack = null },
            title = { Text("Add to playlist") },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text(
                            "No playlists yet. Create one below.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                    } else if (!showCreateFields) {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(playlists) { pl ->
                                ListItem(
                                    headlineContent = {
                                        Text(pl.name, maxLines = 1, softWrap = false)
                                    },
                                    modifier = Modifier.clickable {
                                        viewModel.addToPlaylist(pl.id, track.id)
                                        addToPlaylistTrack = null
                                    }
                                )
                            }
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text("Create new playlist…", maxLines = 1, softWrap = false)
                                    },
                                    modifier = Modifier.clickable { showCreateFields = true }
                                )
                            }
                        }
                    }
                    if (showCreateFields || playlists.isEmpty()) {
                        if (playlists.isNotEmpty()) {
                            TextButton(onClick = { showCreateFields = false }) {
                                Text("Back to playlists")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            singleLine = true,
                            label = { Text("Playlist name") },
                            placeholder = { Text("My playlist") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (showCreateFields || playlists.isEmpty()) {
                    TextButton(
                        onClick = {
                            val name = newPlaylistName.trim().ifBlank { "Playlist" }
                            viewModel.createPlaylistAndAdd(name, track.id) {
                                addToPlaylistTrack = null
                            }
                        }
                    ) { Text("Create & add") }
                } else {
                    TextButton(onClick = { addToPlaylistTrack = null }) { Text("Close") }
                }
            },
            dismissButton = {
                TextButton(onClick = { addToPlaylistTrack = null }) { Text("Cancel") }
            }
        )
    }
}

enum class SortMode { ALPHA, TRACK_NUM, YEAR }
