package com.grok.tplayer.ui.screens.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grok.tplayer.player.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerController: PlayerController,
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {
    val track by playerController.currentTrack.collectAsState()
    val isPlaying by playerController.isPlaying.collectAsState()
    val position by playerController.position.collectAsState()
    val duration by playerController.duration.collectAsState()
    val speed by playerController.playbackSpeed.collectAsState()
    val isRewinding by playerController.isRewinding.collectAsState()

    val discRotation = remember { Animatable(0f) }
    var gestureAngle by remember { mutableFloatStateOf(0f) }
    var gestureActive by remember { mutableStateOf(false) }
    // Bumps when a seek gesture ends so the 5s reset timer restarts
    var gestureEndToken by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val artFile = remember(track?.albumArtPath) {
        track?.albumArtPath?.let { File(it) }?.takeIf { it.exists() }
    }

    // Reset when track changes
    LaunchedEffect(track?.id) {
        discRotation.snapTo(0f)
        gestureAngle = 0f
        gestureActive = false
    }

    // Position polling
    LaunchedEffect(isPlaying, isRewinding) {
        while (isPlaying || isRewinding) {
            playerController.updatePosition()
            delay(200)
        }
    }

    // After normal playback for 5 seconds (no gesture, 1x, not rewinding), ease art back to 0°
    LaunchedEffect(isPlaying, isRewinding, speed, gestureActive, gestureEndToken, track?.id) {
        if (!isPlaying || isRewinding || speed != 1.0f || gestureActive) return@LaunchedEffect
        if (kotlin.math.abs(discRotation.value) < 0.5f) return@LaunchedEffect
        delay(5_000)
        // Still in normal play after 5s?
        if (isPlaying && !isRewinding && speed == 1.0f && !gestureActive) {
            discRotation.animateTo(0f, animationSpec = tween(durationMillis = 900))
        }
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    // Nearly edge-to-edge disc
    val discSize = (screenWidth - 16.dp).coerceAtMost(420.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background: solid theme color, or blown-up album art
        if (artFile != null) {
            AsyncImage(
                model = artFile,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 2.4f
                        scaleY = 2.4f
                    },
                contentScale = ContentScale.Crop
            )
            // Darken so controls stay readable
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        // Folder in the top middle → library browse
                        IconButton(onClick = onOpenLibrary) {
                            Icon(Icons.Default.Folder, "Library / folders")
                        }
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
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.MoreVert, "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track?.displayTitle ?: "No track",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${track?.displayArtist ?: ""} — ${track?.displayAlbum ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Large circular disc + ring
                Box(
                    modifier = Modifier
                        .size(discSize)
                        .weight(1f, fill = false),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 12.dp.toPx()
                        val pad = stroke / 2 + 1.dp.toPx()
                        val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                        val topLeft = Offset(pad, pad)

                        drawArc(
                            color = Color.White.copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        if (gestureActive && kotlin.math.abs(gestureAngle) > 1.5f) {
                            val ringColor =
                                if (gestureAngle > 0f) Color(0xFF69F0AE) else Color(0xFF40C4FF)
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = gestureAngle.coerceIn(-360f, 360f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Disc slightly smaller than outer ring
                    val inner = discSize - 28.dp
                    Box(
                        modifier = Modifier
                            .size(inner)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .rotate(discRotation.value)
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
                                        gestureEndToken++
                                    },
                                    onDragCancel = {
                                        gestureActive = false
                                        gestureAngle = 0f
                                        gestureEndToken++
                                    },
                                    onDrag = { change, _ ->
                                        val currentAngle = angleFromCenter(change.position, center)
                                        var delta = currentAngle - lastAngle
                                        if (delta > 180) delta -= 360
                                        if (delta < -180) delta += 360
                                        val d = delta.toFloat()
                                        gestureAngle += d
                                        scope.launch { discRotation.snapTo(discRotation.value + d) }
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
                                modifier = Modifier.size(96.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        // Spindle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .align(Alignment.Center)
                        )
                    }

                    if (gestureActive && kotlin.math.abs(gestureAngle) > 6f) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = if (gestureAngle > 0f) "▶ Forward" else "◀ Reverse",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (speed != 1.0f || isRewinding) {
                    Text(
                        text = if (isRewinding) "⏪ ${speed.toInt()}×" else "${speed.toInt()}×",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF69F0AE),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

                // Straight progress line (no Material Slider "squiggle")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 4.dp)
                        .pointerInput(duration) {
                            detectTapGestures { offset ->
                                if (duration > 0) {
                                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                                    playerController.seekTo((frac * duration).toLong())
                                }
                            }
                            detectDragGestures { change, _ ->
                                if (duration > 0) {
                                    val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                                    playerController.seekTo((frac * duration).toLong())
                                }
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Background track
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.28f))
                    )
                    // Filled progress
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                    // Thumb
                    Box(
                        Modifier
                            .padding(start = ((progress * 1000).toInt().coerceIn(0, 1000) / 1000f).let { /* placed via offset below */ 0.dp })
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = size.height / 2f
                        val x = progress * size.width
                        drawCircle(
                            color = Color.White,
                            radius = 7.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(position),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Larger on-screen transport controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { playerController.playPrevious() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            "Previous",
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { playerController.cycleRewind() },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            "Rewind",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                    FilledIconButton(
                        onClick = { playerController.togglePlayPause() },
                        modifier = Modifier.size(76.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            if (isPlaying && !isRewinding) Icons.Default.Pause
                            else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    IconButton(
                        onClick = { playerController.cycleFastForward() },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            "Fast Forward",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { playerController.playNext() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            "Next",
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                    }
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
