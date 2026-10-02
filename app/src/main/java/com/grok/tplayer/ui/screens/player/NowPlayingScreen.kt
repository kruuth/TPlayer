package com.grok.tplayer.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grok.tplayer.player.PlayerController
import kotlinx.coroutines.delay
import java.io.File
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

    // Total rotation of the disc (degrees); updates while dragging
    var discRotation by remember { mutableFloatStateOf(0f) }
    var gestureAngle by remember { mutableFloatStateOf(0f) }
    var gestureActive by remember { mutableStateOf(false) }

    // Reset disc orientation when track changes
    LaunchedEffect(track?.id) {
        discRotation = 0f
        gestureAngle = 0f
        gestureActive = false
    }

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
                    Text("Now Playing", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
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
            Text(
                text = track?.displayTitle ?: "No track",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
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

            Spacer(Modifier.height(20.dp))

            // Circular artwork inside a direction ring; art rotates with finger
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer direction ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val pad = stroke / 2 + 2.dp.toPx()
                    val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                    val topLeft = Offset(pad, pad)

                    // Track ring (always visible)
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.35f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    if (gestureActive && kotlin.math.abs(gestureAngle) > 1.5f) {
                        val ringColor = if (gestureAngle > 0f) Color(0xFF4CAF50) else Color(0xFF2196F3)
                        val sweep = gestureAngle.coerceIn(-360f, 360f)
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }

                // Circular album art (fits inside the ring) — rotates with gesture
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .rotate(discRotation)
                        .pointerInput(Unit) {
                            var lastAngle = 0.0
                            var center = Offset.Zero
                            detectDragGestures(
                                onDragStart = { offset ->
                                    center = Offset(size.width / 2f, size.height / 2f)
                                    lastAngle = angleFromCenter(offset, center)
                                    gestureAngle = 0f
                                    gestureActive = true
                                },
                                onDragEnd = {
                                    gestureActive = false
                                    gestureAngle = 0f
                                },
                                onDragCancel = {
                                    gestureActive = false
                                    gestureAngle = 0f
                                },
                                onDrag = { change, _ ->
                                    val currentAngle = angleFromCenter(change.position, center)
                                    var delta = currentAngle - lastAngle
                                    if (delta > 180) delta -= 360
                                    if (delta < -180) delta += 360
                                    val d = delta.toFloat()
                                    gestureAngle += d
                                    discRotation += d
                                    val seekDelta = (delta * 50).toLong()
                                    if (kotlin.math.abs(seekDelta) > 12) {
                                        playerController.seekRelative(seekDelta)
                                    }
                                    lastAngle = currentAngle
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val artFile = track?.albumArtPath?.let { File(it) }?.takeIf { it.exists() }
                    if (artFile != null) {
                        AsyncImage(
                            model = artFile,
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = "No artwork",
                            modifier = Modifier.size(88.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Center spindle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .align(Alignment.Center)
                    )
                }

                if (gestureActive && kotlin.math.abs(gestureAngle) > 6f) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = if (gestureAngle > 0f) "▶ Forward" else "◀ Reverse",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
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
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
