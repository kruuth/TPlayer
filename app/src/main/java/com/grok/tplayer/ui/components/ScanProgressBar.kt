package com.grok.tplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grok.tplayer.data.scanner.LibraryScanner
import kotlinx.coroutines.flow.StateFlow

/**
 * Global status strip shown while a library scan is running.
 */
@Composable
fun ScanProgressBar(
    scanProgress: StateFlow<LibraryScanner.ScanState>,
    modifier: Modifier = Modifier
) {
    val state by scanProgress.collectAsState()
    when (val s = state) {
        is LibraryScanner.ScanState.Scanning -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Scanning library… ${s.current}/${s.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        if (s.total > 0) s.current.toFloat() / s.total else 0f
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (s.fileName.isNotBlank()) {
                    Text(
                        text = s.fileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        }
        else -> {}
    }
}
