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
    onOpenSearch: () -> Unit = {},
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsState()
    val years by viewModel.years.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val excluded by viewModel.excludedFolders.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Artists", "Albums", "Years", "Folders", "All")

    // Normalized folder list (exclude excluded paths)
    val filteredFolders = remember(folders, excluded) {
        folders.filter { f ->
            excluded.none { excl ->
                f == excl || f.startsWith("$excl/") || f.startsWith("$excl\\")
            }
        }
    }

    // Common library root shared by all paths (e.g. "Music" or "storage/Music")
    // So the Folders tab opens *inside* the root and never lists the root itself.
    val libraryRoot = remember(filteredFolders) {
        commonPathPrefix(filteredFolders)
    }

    // Current location relative to library; empty string means at library root
    var relativePath by remember { mutableStateOf("") }

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
                    IconButton(onClick = onOpenPlayer) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Now Playing")
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
                            if (index != 3) relativePath = ""
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

/**
 * Longest common directory prefix of all paths.
 * e.g. ["Music/A", "Music/B"] → "Music"
 *      ["Music"] → "Music" (then children of Music are shown, not Music itself as the only entry)
 */
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
