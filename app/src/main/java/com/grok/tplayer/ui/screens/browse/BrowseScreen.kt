package com.grok.tplayer.ui.screens.browse

import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onOpenList: (mode: String, key: String) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsState()
    val years by viewModel.years.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val excluded by viewModel.excludedFolders.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Artists", "Albums", "Years", "Folders", "All")

    // Hierarchical folder navigation state
    var folderPath by remember { mutableStateOf("") } // "" = root listing of top-level segments

    val visibleFolders = remember(folders, excluded, folderPath) {
        val filtered = folders.filter { f ->
            excluded.none { excl -> f == excl || f.startsWith("$excl/") || f.startsWith("$excl\\") }
        }
        if (folderPath.isEmpty()) {
            // top-level unique first path segments
            filtered.map { path ->
                path.trim('/').substringBefore('/')
            }.distinct().sorted()
        } else {
            val prefix = folderPath.trimEnd('/') + "/"
            filtered
                .filter { it == folderPath || it.startsWith(prefix) }
                .map { path ->
                    val rest = path.removePrefix(folderPath).trimStart('/')
                    if (rest.isEmpty()) null
                    else rest.substringBefore('/')
                }
                .filterNotNull()
                .distinct()
                .sorted()
        }
    }

    // Full paths under current folder that are exact matches (contain tracks at this level)
    val canPlayCurrentFolder = remember(folders, folderPath) {
        folderPath.isNotEmpty() && folders.any { it == folderPath || it.startsWith(folderPath.trimEnd('/') + "/") }
    }

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
                    IconButton(onClick = onOpenPlayer) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Now Playing")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                            if (index != 3) folderPath = ""
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
                    // Folder browser with Up
                    Column {
                        if (folderPath.isNotEmpty()) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "⬆ Up",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        folderPath,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                },
                                leadingContent = {
                                    Icon(Icons.Default.ArrowUpward, "Go up")
                                },
                                modifier = Modifier.clickable {
                                    folderPath = folderPath.trimEnd('/')
                                        .substringBeforeLast('/', missingDelimiterValue = "")
                                }
                            )
                            HorizontalDivider()
                            if (canPlayCurrentFolder) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            "▶ Play this folder",
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    leadingContent = {
                                        Icon(Icons.Default.PlayArrow, null)
                                    },
                                    modifier = Modifier.clickable {
                                        onOpenList("folder", folderPath)
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                        LazyColumn {
                            items(visibleFolders) { name ->
                                val nextPath = if (folderPath.isEmpty()) name
                                else folderPath.trimEnd('/') + "/" + name
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
                                        // If this is a leaf folder (no children), open tracks; else descend
                                        val hasChildren = folders.any {
                                            it.startsWith(nextPath.trimEnd('/') + "/") &&
                                                it != nextPath
                                        }
                                        if (hasChildren) {
                                            folderPath = nextPath
                                        } else {
                                            onOpenList("folder", nextPath)
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
                4 -> ListItem(
                    headlineContent = {
                        Text("Play all tracks (A–Z)", maxLines = 1, softWrap = false)
                    },
                    modifier = Modifier.clickable { onOpenList("all", "all") }
                )
            }
        }
    }
}
