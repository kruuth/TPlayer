package com.iks.tplayer.ui.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowseScreen(
    onOpenList: (mode: String, key: String) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onEditPlaylist: (Long) -> Unit = {},
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsState()
    val years by viewModel.years.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val excluded by viewModel.excludedFolders.collectAsState()
    val cloudSources by viewModel.cloudSources.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val tabs = listOf("Artists", "Albums", "Years", "Folders", "Cloud", "Playlists", "All")
    val savedTab by viewModel.lastBrowseTab.collectAsState()
    // Restore last active tab (Folders if user was browsing folders, etc.)
    var selectedTab by remember { mutableIntStateOf(0) }
    var tabInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(savedTab) {
        if (!tabInitialized) {
            selectedTab = savedTab.coerceIn(0, tabs.lastIndex)
            tabInitialized = true
        }
    }

    // Normalized folder list (exclude excluded paths); local Folders tab hides Cloud/ paths
    val filteredFolders = remember(folders, excluded) {
        folders.filter { f ->
            !f.startsWith("Cloud/") &&
            excluded.none { excl ->
                f == excl || f.startsWith("$excl/") || f.startsWith("$excl\\")
            }
        }
    }
    val cloudFolderPaths = remember(folders) {
        folders.filter { it.startsWith("Cloud/") }
    }

    // Common library root shared by all paths (e.g. "Music" or "storage/Music")
    // So the Folders tab opens *inside* the root and never lists the root itself.
    val libraryRoot = remember(filteredFolders) {
        commonPathPrefix(filteredFolders)
    }

    // Current location relative to library; empty string means at library root
    var relativePath by remember { mutableStateOf("") }
    var cloudRelative by remember { mutableStateOf("") }

    // Absolute path for queries / display
    val currentPath = remember(libraryRoot, relativePath) {
        when {
            relativePath.isEmpty() -> libraryRoot
            libraryRoot.isEmpty() -> relativePath
            else -> libraryRoot.trimEnd('/') + "/" + relativePath.trimStart('/')
        }
    }

    val visibleChildNames = remember(filteredFolders, currentPath, libraryRoot) {
        childFolderNames(filteredFolders, currentPath)
    }

    val canPlayCurrentFolder = remember(filteredFolders, currentPath) {
        currentPath.isNotEmpty() && filteredFolders.any {
            it == currentPath || it.startsWith(currentPath.trimEnd('/') + "/")
        }
    }

    // Show Up only when deeper than library root
    val canGoUp = relativePath.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Library",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    // Jump back to last track / last folder or artist context on the player
                    IconButton(onClick = onOpenPlayer) {
                        Icon(Icons.Default.History, contentDescription = "Last played")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            viewModel.setLastBrowseTab(index)
                            if (index != 3) relativePath = ""
                            if (index != 4) cloudRelative = ""
                        },
                        text = {
                            Text(
                                title,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> LazyColumn {
                    items(artists) { artist ->
                        ListItem(
                            headlineContent = {
                                Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                            },
                            modifier = Modifier.clickable { onOpenList("artist", artist) }
                        )
                        HorizontalDivider()
                    }
                }
                1 -> ListItem(
                    headlineContent = {
                        Text("All Albums", maxLines = 1, softWrap = false)
                    },
                    modifier = Modifier.clickable { onOpenList("all", "albums") }
                )
                2 -> LazyColumn {
                    items(years) { year ->
                        ListItem(
                            headlineContent = {
                                Text(year.toString(), maxLines = 1, softWrap = false)
                            },
                            modifier = Modifier.clickable { onOpenList("year", year.toString()) }
                        )
                        HorizontalDivider()
                    }
                }
                3 -> {
                    Column {
                        if (canGoUp) {
                            ListItem(
                                headlineContent = {
                                    Text("⬆ Up", maxLines = 1, softWrap = false)
                                },
                                supportingContent = {
                                    Text(
                                        relativePath.ifEmpty { libraryRoot },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                },
                                leadingContent = {
                                    Icon(Icons.Default.ArrowUpward, "Go up")
                                },
                                modifier = Modifier.clickable {
                                    relativePath = relativePath.trimEnd('/')
                                        .substringBeforeLast('/', missingDelimiterValue = "")
                                }
                            )
                            HorizontalDivider()
                        }

                        // Always allow playing current level (including library root contents)
                        if (canPlayCurrentFolder) {
                            ListItem(
                                headlineContent = {
                                    Text("▶ Play this folder", maxLines = 1, softWrap = false)
                                },
                                supportingContent = if (relativePath.isNotEmpty()) {
                                    {
                                        Text(
                                            relativePath,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
                                        )
                                    }
                                } else null,
                                leadingContent = {
                                    Icon(Icons.Default.PlayArrow, null)
                                },
                                modifier = Modifier.clickable {
                                    onOpenList("folder", currentPath)
                                }
                            )
                            HorizontalDivider()
                        }

                        LazyColumn {
                            items(visibleChildNames) { name ->
                                val nextRelative = if (relativePath.isEmpty()) name
                                else relativePath.trimEnd('/') + "/" + name
                                val nextAbsolute = if (libraryRoot.isEmpty()) nextRelative
                                else libraryRoot.trimEnd('/') + "/" + nextRelative

                                ListItem(
                                    headlineContent = {
                                        Text(
                                            name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
                                        )
                                    },
                                    leadingContent = {
                                        Icon(Icons.Default.Folder, null)
                                    },
                                    trailingContent = {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                    },
                                    modifier = Modifier.clickable {
                                        val hasChildren = filteredFolders.any {
                                            it.startsWith(nextAbsolute.trimEnd('/') + "/") &&
                                                it != nextAbsolute
                                        }
                                        if (hasChildren) {
                                            relativePath = nextRelative
                                        } else {
                                            onOpenList("folder", nextAbsolute)
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
                4 -> {
                    // Cloud connections + folder tree under Cloud/<name>/...
                    val cloudRoot = "Cloud"
                    val cloudCurrent = if (cloudRelative.isEmpty()) cloudRoot
                    else "$cloudRoot/${cloudRelative.trim('/')}"
                    val cloudChildren = childFolderNames(cloudFolderPaths, cloudCurrent)
                    val cloudCanPlay = cloudRelative.isNotEmpty() && cloudFolderPaths.any {
                        it == cloudCurrent || it.startsWith(cloudCurrent.trimEnd('/') + "/")
                    }
                    LazyColumn {
                        if (cloudSources.isEmpty() && cloudFolderPaths.isEmpty()) {
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            "No cloud drives connected",
                                            maxLines = 2,
                                            softWrap = true
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            "Settings → Cloud drives → Connect cloud folder (pick Google Drive or another provider).",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 3
                                        )
                                    },
                                    leadingContent = {
                                        Icon(Icons.Default.Cloud, contentDescription = null)
                                    }
                                )
                            }
                        }
                        if (cloudRelative.isNotEmpty()) {
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text("Up", maxLines = 1, softWrap = false)
                                    },
                                    leadingContent = {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                                    },
                                    modifier = Modifier.clickable {
                                        cloudRelative = cloudRelative
                                            .trim('/')
                                            .substringBeforeLast('/', missingDelimiterValue = "")
                                    }
                                )
                            }
                        }
                        // Top level: show each connection name from sources if no relative path
                        if (cloudRelative.isEmpty()) {
                            items(cloudSources) { src ->
                                val childName = src.displayName
                                ListItem(
                                    headlineContent = {
                                        Text(childName, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                    },
                                    supportingContent = {
                                        Text("Cloud connection", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    },
                                    leadingContent = {
                                        Icon(Icons.Default.Cloud, contentDescription = null)
                                    },
                                    trailingContent = {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                    },
                                    modifier = Modifier.clickable {
                                        // Enter Cloud/<displayName>
                                        cloudRelative = childName
                                    }
                                )
                            }
                        }
                        items(cloudChildren) { name ->
                            val nextRel = if (cloudRelative.isEmpty()) name
                            else "${cloudRelative.trim('/')}/$name"
                            ListItem(
                                headlineContent = {
                                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                },
                                leadingContent = {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                },
                                trailingContent = {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                },
                                modifier = Modifier.clickable {
                                    cloudRelative = nextRel
                                }
                            )
                        }
                        if (cloudCanPlay) {
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text("Play this folder", maxLines = 1, softWrap = false)
                                    },
                                    leadingContent = {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    },
                                    modifier = Modifier.clickable {
                                        onOpenList("folder", cloudCurrent)
                                    }
                                )
                            }
                        }
                    }
                }
                5 -> PlaylistsTab(
                    playlists = playlists,
                    onOpenPlaylist = { id -> onOpenList("playlist", id.toString()) },
                    onEditPlaylist = { id -> onEditPlaylist(id) },
                    onCreate = { name -> viewModel.createPlaylist(name) },
                    onRename = { id, name -> viewModel.renamePlaylist(id, name) },
                    onDelete = { id -> viewModel.deletePlaylist(id) }
                )
                6 -> ListItem(
                    headlineContent = {
                        Text("Play all tracks (A–Z)", maxLines = 1, softWrap = false)
                    },
                    modifier = Modifier.clickable { onOpenList("all", "all") }
                )
            }
        }
    }
}

/**
 * Longest common directory prefix of all paths.
 * e.g. ["Music/A", "Music/B"] → "Music"
 *      ["Music"] → "Music" (then children of Music are shown, not Music itself as the only entry)
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistsTab(
    playlists: List<com.iks.tplayer.data.model.Playlist>,
    onOpenPlaylist: (Long) -> Unit,
    onEditPlaylist: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var menuPlaylistId by remember { mutableStateOf<Long?>(null) }
    var renameTarget by remember { mutableStateOf<com.iks.tplayer.data.model.Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<com.iks.tplayer.data.model.Playlist?>(null) }

    LazyColumn {
        item {
            ListItem(
                headlineContent = {
                    Text("New playlist", maxLines = 1, softWrap = false)
                },
                leadingContent = {
                    Icon(Icons.Default.Add, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    createName = ""
                    showCreate = true
                }
            )
        }
        items(playlists, key = { it.id }) { pl ->
            Box {
                ListItem(
                    headlineContent = {
                        Text(pl.name, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    },
                    leadingContent = {
                        Icon(Icons.Default.QueueMusic, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = { onOpenPlaylist(pl.id) },
                        onLongClick = { menuPlaylistId = pl.id }
                    )
                )
                DropdownMenu(
                    expanded = menuPlaylistId == pl.id,
                    onDismissRequest = { menuPlaylistId = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit songs") },
                        onClick = {
                            menuPlaylistId = null
                            onEditPlaylist(pl.id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuPlaylistId = null
                            renameTarget = pl
                            renameText = pl.name
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuPlaylistId = null
                            deleteTarget = pl
                        }
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreate(createName.ifBlank { "Playlist" })
                    showCreate = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancel") }
            }
        )
    }
    renameTarget?.let { pl ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename playlist") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(pl.id, renameText)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }
    deleteTarget?.let { pl ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete playlist?") },
            text = { Text("Delete “${pl.name}”? Songs are not removed from the library.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(pl.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

private fun commonPathPrefix(paths: List<String>): String {
    if (paths.isEmpty()) return ""
    val normalized = paths.map { it.trim('/').replace('\\', '/') }
    val first = normalized.first().split('/')
    var end = first.size
    for (p in normalized.drop(1)) {
        val parts = p.split('/')
        var i = 0
        while (i < end && i < parts.size && parts[i] == first[i]) i++
        end = i
        if (end == 0) return ""
    }
    // If every path is exactly the same single segment (e.g. all "Music"),
    // that segment *is* the library root — we still use it so we list its children.
    return first.take(end).joinToString("/")
}

/** Immediate child folder names under [parent] (not the parent itself). */
private fun childFolderNames(allPaths: List<String>, parent: String): List<String> {
    val p = parent.trim('/').replace('\\', '/')
    return allPaths
        .map { it.trim('/').replace('\\', '/') }
        .mapNotNull { path ->
            when {
                p.isEmpty() -> {
                    // No common root — show first segments, but if a path has more depth, only first
                    path.substringBefore('/').takeIf { it.isNotEmpty() }
                }
                path == p -> null // don't list the parent folder itself
                path.startsWith("$p/") -> {
                    path.removePrefix("$p/").substringBefore('/').takeIf { it.isNotEmpty() }
                }
                else -> null
            }
        }
        .distinct()
        .sorted()
}
