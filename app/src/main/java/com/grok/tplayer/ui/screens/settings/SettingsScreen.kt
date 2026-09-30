package com.grok.tplayer.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grok.tplayer.data.preferences.ThemeMode
import com.grok.tplayer.data.preferences.toComposeColor
import com.grok.tplayer.data.scanner.LibraryScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val scanState by viewModel.scanProgress.collectAsState()

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.changeRootAndScan(it) }
    }

    val presetButtons = listOf(
        0xFF6200EEL to "Purple",
        0xFF1976D2L to "Blue",
        0xFF388E3CL to "Green",
        0xFFD32F2FL to "Red",
        0xFFF57C00L to "Orange",
        0xFF00897BL to "Teal"
    )
    val presetBg = listOf(
        null to "Default",
        0xFFFFFBFE to "White",
        0xFFF5F5F5 to "Light gray",
        0xFF121212 to "Near black",
        0xFF1A237E to "Navy",
        0xFF3E2723 to "Brown"
    )
    val presetFont = listOf(
        null to "Default",
        0xFF1C1B1FL to "Near black",
        0xFFE6E1E5L to "Near white",
        0xFFB0BEC5L to "Blue gray",
        0xFFFFF59DL to "Yellow"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Theme", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                        ThemeMode.SYSTEM -> "System"
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text("Button color", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presetButtons.forEach { (color, label) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = if (settings.buttonColor == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setButtonColor(color) },
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text("Background color", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presetBg.forEach { (color, _) ->
                        val bg = color?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .then(
                                    if (settings.backgroundColor == color) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { viewModel.setBackgroundColor(color) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text("Font color", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presetFont.forEach { (color, _) ->
                        val c = color?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .then(
                                    if (settings.fontColor == color) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { viewModel.setFontColor(color) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text("Library", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { folderLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Folder, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Change root folder & rescan", maxLines = 1, softWrap = false)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.rescanDefault() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rescan default Music folder", maxLines = 1, softWrap = false)
                }
                when (val s = scanState) {
                    is LibraryScanner.ScanState.Scanning -> {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (s.total > 0) s.current.toFloat() / s.total else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Scanning ${s.current}/${s.total}", maxLines = 1, softWrap = false)
                    }
                    is LibraryScanner.ScanState.Finished -> {
                        Text("Scan done: ${s.count} tracks", maxLines = 1, softWrap = false)
                    }
                    is LibraryScanner.ScanState.Error -> {
                        Text(s.message, color = MaterialTheme.colorScheme.error, maxLines = 2)
                    }
                    else -> {}
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Exclude folders",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    "Excluded folders are hidden from the Folders tab and ignored on future scans when possible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(folders) { folder ->
                val excluded = folder in settings.excludedFolders
                ListItem(
                    headlineContent = {
                        Text(folder, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    },
                    trailingContent = {
                        if (excluded) {
                            IconButton(onClick = { viewModel.includeFolder(folder) }) {
                                Icon(Icons.Default.Delete, "Remove exclusion")
                            }
                        } else {
                            TextButton(onClick = { viewModel.excludeFolder(folder) }) {
                                Text("Exclude", maxLines = 1, softWrap = false)
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (excluded)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            Color.Transparent
                    )
                )
                HorizontalDivider()
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
