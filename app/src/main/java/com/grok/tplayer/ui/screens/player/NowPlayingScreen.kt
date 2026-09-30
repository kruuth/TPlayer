package com.grok.tplayer.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grok.tplayer.player.PlayerController
import kotlinx.coroutines.delay
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerController: PlayerController,
    onBack: () -> Unit
) {
    val track by playerController.currentTrack.collectAsState()
    val isPlaying by playerController.isPlaying.collectAsState()
    val position by playerController.position.collectAsState()
    val duration by playerController.duration.collectAsState()
    val speed by playerController.playbackSpeed.collectAsState()
    val isRewinding by playerController.isRewinding.collectAsState()

    LaunchedEffect(isPlaying, isRewinding) {
        while (isPlaying || isRewinding) {
            playerController.updatePosition()
            delay(200)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Now Playing",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to list")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title – single line, marquee if long
            Text(
                text = track?.displayTitle ?: "No track",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = "${track?.displayArtist ?: ""} — ${track?.displayAlbum ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Artwork + circular seek gesture
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        var lastAngle = 0.0
                        var center = Offset.Zero
                        detectDragGestures(
                            onDragStart = { offset ->
                                center = Offset(size.width / 2f, size.height / 2f)
                                lastAngle = angleFromCenter(offset, center)
                            },
                            onDrag = { change, _ ->
                                val currentAngle = angleFromCenter(change.position, center)
                                var delta = currentAngle - lastAngle
                                if (delta > 180) delta -= 360
                                if (delta < -180) delta += 360
                                // Screen Y grows downward: positive delta = clockwise visually
                                // Clockwise → seek forward; counterclockwise → seek backward
                                val seekDelta = (delta * 50).toLong()
                                if (kotlin.math.abs(seekDelta) > 15) {
                                    playerController.seekRelative(seekDelta)
                                }
                                lastAngle = currentAngle
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val artPath = track?.albumArtPath
                if (artPath != null) {
                    AsyncImage(
                        model = artPath,
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "No artwork",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (speed != 1.0f || isRewinding) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isRewinding) "⏪ ${speed.toInt()}×" else "${speed.toInt()}×",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(Modifier.height(12.dp))

            val progress = if (duration > 0) position.toFloat() / duration else 0f
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { fraction ->
                    playerController.seekTo((fraction * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(position), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerController.playPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { playerController.cycleRewind() }) {
                    Icon(Icons.Default.FastRewind, "Rewind", modifier = Modifier.size(32.dp))
                }
                FilledIconButton(
                    onClick = { playerController.togglePlayPause() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (isPlaying && !isRewinding) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { playerController.stop() }) {
                    Icon(Icons.Default.Stop, "Stop", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { playerController.cycleFastForward() }) {
                    Icon(Icons.Default.FastForward, "Fast Forward", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { playerController.playNext() }) {
                    Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

private fun angleFromCenter(pos: Offset, center: Offset): Double {
    val dx = pos.x - center.x
    val dy = pos.y - center.y
    return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
