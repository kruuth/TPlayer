package com.grok.tplayer.ui.screens.settings

import android.content.Intent
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
    val pending by viewModel.pendingScan.collectAsState()
    val skips by viewModel.selectedSkips.collectAsState()
    val listing by viewModel.listingFolders.collectAsState()
    val status by viewModel.statusMessage.collectAsState()

    // OpenDocumentTree with persistable read permission flags
    val folderLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: android.content.Context, input: Uri?): Intent {
                return super.createIntent(context, input).apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    )
                }
            }
        }
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.prepareCustomRoot(uri)
        }
    }

    val presetButtons = listOf(
        0xFF6200EEL, 0xFF1976D2L, 0xFF388E3CL, 0xFFD32F2FL, 0xFFF57C00L, 0xFF00897BL
    )
    val presetBg = listOf(
        null, 0xFFFFFBFE, 0xFFF5F5F5, 0xFF121212, 0xFF1A237E, 0xFF3E2723
    )
    val presetFont = listOf(
        null, 0xFF1C1B1FL, 0xFFE6E1E5L, 0xFFB0BEC5L, 0xFFFFF59DL
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
                    presetButtons.forEach { color ->
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
                                .clickable { viewModel.setButtonColor(color) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text("Background color", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presetBg.forEach { color ->
                        val bg = color?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .then(
                                    if (settings.backgroundColor == color)
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
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
                    presetFont.forEach { color ->
                        val c = color?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .then(
                                    if (settings.fontColor == color)
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !listing && scanState !is LibraryScanner.ScanState.Scanning
                ) {
                    Icon(Icons.Default.Folder, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Change root folder & rescan", maxLines = 1, softWrap = false)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.prepareDefaultRescan() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !listing && scanState !is LibraryScanner.ScanState.Scanning
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rescan default Music folder", maxLines = 1, softWrap = false)
                }

                if (listing) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Listing folders…", maxLines = 1, softWrap = false)
                    }
                }

                status?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, maxLines = 2, color = MaterialTheme.colorScheme.primary)
                }

                when (val s = scanState) {
                    is LibraryScanner.ScanState.Scanning -> {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (s.total > 0) s.current.toFloat() / s.total else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Scanning ${s.current}/${s.total}: ${s.fileName}",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is LibraryScanner.ScanState.Finished -> {
                        Text("Scan done: ${s.count} tracks", maxLines = 1, softWrap = false)
                    }
                    is LibraryScanner.ScanState.Error -> {
                        Text(s.message, color = MaterialTheme.colorScheme.error, maxLines = 3)
                    }
                    else -> {}
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text("Android Auto / steering", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Beep on steering controls",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.75f)
                    )
                    Switch(
                        checked = settings.steeringBeepEnabled,
                        onCheckedChange = { viewModel.setSteeringBeep(it) }
                    )
                }
                Text(
                    "Plays a short beep when play/pause, next, or previous is used from the car.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text("Exclude folders", style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                Text(
                    "Hidden from Folders tab and skipped on future scans.",
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
                        else Color.Transparent
                    )
                )
                HorizontalDivider()
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Pre-scan skip dialog (same as Source screen)
    if (pending != null) {
        val p = pending!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelPending() },
            title = { Text("Skip folders before scan?", maxLines = 2) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Source: ${p.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    if (p.subfolders.isEmpty()) {
                        Text(
                            "No subfolders listed. Scan will include all MP3s (you can exclude later below).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text("Check folders to skip:", style = MaterialTheme.typography.bodyMedium)
                        Row {
                            TextButton(onClick = { viewModel.selectAllSkips() }) {
                                Text("Select all", maxLines = 1, softWrap = false)
                            }
                            TextButton(onClick = { viewModel.clearSkips() }) {
                                Text("Clear", maxLines = 1, softWrap = false)
                            }
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(p.subfolders) { folder ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = folder in skips,
                                        onCheckedChange = { viewModel.toggleSkip(folder) }
                                    )
                                    Text(
                                        folder.substringAfterLast('/'),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.startScanWithSkips() }) {
                    Text(
                        if (skips.isEmpty()) "Scan all" else "Scan (${skips.size} skipped)",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelPending() }) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
            }
        )
    }
}
