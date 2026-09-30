package com.grok.tplayer.ui.screens.source

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grok.tplayer.data.scanner.LibraryScanner
import com.grok.tplayer.ui.screens.source.SourceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceScreen(
    onLibraryReady: () -> Unit,
    viewModel: SourceViewModel = hiltViewModel()
) {
    val scanState by viewModel.scanProgress.collectAsState()
    val trackCount by viewModel.trackCount.collectAsState()

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.scanCustomFolder(it) }
    }

    LaunchedEffect(scanState) {
        if (scanState is LibraryScanner.ScanState.Finished && trackCount > 0) {
            onLibraryReady()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TPlayer – Choose Source", maxLines = 1, softWrap = false) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Select where your MP3 files live",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.scanDefaultMusic() },
                modifier = Modifier.fillMaxWidth(),
                enabled = scanState !is LibraryScanner.ScanState.Scanning
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Default /Internal storage/Music/")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { folderLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = scanState !is LibraryScanner.ScanState.Scanning
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose Custom Folder…")
            }

            Spacer(Modifier.height(32.dp))

            when (val state = scanState) {
                is LibraryScanner.ScanState.Scanning -> {
                    LinearProgressIndicator(
                        progress = {
                            if (state.total > 0) state.current.toFloat() / state.total else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Scanning ${state.current}/${state.total}: ${state.fileName}")
                }
                is LibraryScanner.ScanState.Finished -> {
                    Text("Found ${state.count} tracks")
                    if (state.count > 0) {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onLibraryReady) {
                            Text("Continue to Library")
                        }
                    }
                }
                is LibraryScanner.ScanState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
        }
    }
}
