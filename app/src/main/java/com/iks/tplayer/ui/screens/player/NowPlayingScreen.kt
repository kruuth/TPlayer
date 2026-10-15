package com.iks.tplayer.ui.screens.player

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.iks.tplayer.data.preferences.AppSettings
import com.iks.tplayer.data.preferences.UserPreferences
import com.iks.tplayer.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.math.atan2
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    prefs: UserPreferences
) : ViewModel() {
    val settings = prefs.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerController: PlayerController,
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val appSettings by viewModel.settings.collectAsState()
    val track by playerController.currentTrack.collectAsState()
    val isPlaying by playerController.isPlaying.collectAsState()
    val position by playerController.position.collectAsState()
    val duration by playerController.duration.collectAsState()
    val speed by playerController.playbackSpeed.collectAsState()
    val isRewinding by playerController.isRewinding.collectAsState()

    val discRotation = remember { Animatable(0f) }
    var gestureAngle by remember { mutableFloatStateOf(0f) }
    var gestureActive by remember { mutableStateOf(false) }
    var gestureEndToken by remember { mutableIntStateOf(0) }
    var showVuMeter by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val artModel: Any? = remember(track?.albumArtPath, appSettings.defaultArtUri) {
        val embedded = track?.albumArtPath?.let { File(it) }?.takeIf { it.exists() }
        when {
            embedded != null -> embedded
            !appSettings.defaultArtUri.isNullOrBlank() -> Uri.parse(appSettings.defaultArtUri)
            else -> null
        }
    }

    LaunchedEffect(track?.id) {
        discRotation.snapTo(0f)
        gestureAngle = 0f
        gestureActive = false
    }

    LaunchedEffect(isPlaying, isRewinding) {
        while (isPlaying || isRewinding) {
            playerController.updatePosition()
            delay(200)
        }
    }

    LaunchedEffect(isPlaying, isRewinding, speed, gestureActive, gestureEndToken, track?.id) {
        if (!isPlaying || isRewinding || speed != 1.0f || gestureActive) return@LaunchedEffect
        if (kotlin.math.abs(discRotation.value) < 0.5f) return@LaunchedEffect
        delay(5_000)
        if (isPlaying && !isRewinding && speed == 1.0f && !gestureActive) {
            discRotation.animateTo(0f, animationSpec = tween(durationMillis = 900))
        }
    }

    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val discSize = if (isLandscape) {
        (config.screenHeightDp.dp - 96.dp).coerceIn(160.dp, 340.dp)
    } else {
        (config.screenWidthDp.dp - 16.dp).coerceAtMost(420.dp)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NowPlayingBackground(artModel, appSettings.backgroundColor)

        if (showVuMeter && appSettings.vuMeterOnTapEnabled) {
            VuMeterScreen(
                title = track?.displayTitle ?: "No track",
                artist = track?.displayArtist ?: "",
                isPlaying = isPlaying && !isRewinding,
                onDismiss = { showVuMeter = false }
            )
            return@Box
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
            val mod = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 4.dp)

            if (isLandscape) {
                Row(
                    modifier = mod,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumDisc(
                            discSize = discSize,
                            artModel = artModel,
                            discRotation = discRotation.value,
                            gestureAngle = gestureAngle,
                            gestureActive = gestureActive,
                            onGestureAngle = { gestureAngle += it },
                            onGestureActive = { active -> gestureActive = active; gestureAngle = 0f },
                            onGestureEnd = { gestureEndToken++ },
                            onRotate = { d -> scope.launch { discRotation.snapTo(discRotation.value + d) } },
                            onSeekRelative = { playerController.seekRelative(it) },
                            onCenterTap = { playerController.togglePlayPause() },
                            vuMeterEnabled = appSettings.vuMeterOnTapEnabled,
                            onOpenVuMeter = { showVuMeter = true }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TrackTitleBlock(track?.displayTitle, track?.displayArtist, track?.displayAlbum)
                        Spacer(Modifier.height(8.dp))
                        PlaybackControlsBlock(
                            playerController = playerController,
                            isPlaying = isPlaying,
                            isRewinding = isRewinding,
                            speed = speed,
                            position = position,
                            duration = duration
                        )
                    }
                }
            } else {
                Column(
                    modifier = mod,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TrackTitleBlock(track?.displayTitle, track?.displayArtist, track?.displayAlbum)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumDisc(
                            discSize = discSize,
                            artModel = artModel,
                            discRotation = discRotation.value,
                            gestureAngle = gestureAngle,
                            gestureActive = gestureActive,
                            onGestureAngle = { gestureAngle += it },
                            onGestureActive = { active -> gestureActive = active; gestureAngle = 0f },
                            onGestureEnd = { gestureEndToken++ },
                            onRotate = { d -> scope.launch { discRotation.snapTo(discRotation.value + d) } },
                            onSeekRelative = { playerController.seekRelative(it) },
                            onCenterTap = { playerController.togglePlayPause() },
                            vuMeterEnabled = appSettings.vuMeterOnTapEnabled,
                            onOpenVuMeter = { showVuMeter = true }
                        )
                    }
                    PlaybackControlsBlock(
                        playerController = playerController,
                        isPlaying = isPlaying,
                        isRewinding = isRewinding,
                        speed = speed,
                        position = position,
                        duration = duration
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingBackground(artModel: Any?, backgroundColorArgb: Long?) {
    if (artModel != null) {
        AsyncImage(
            model = artModel,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 2.4f
                    scaleY = 2.4f
                },
            contentScale = ContentScale.Crop
        )
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
        val customBg = backgroundColorArgb?.let { Color(it) }
        Box(
            Modifier
                .fillMaxSize()
                .background(customBg ?: Color(0xFF121212))
        )
        if (customBg != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun TrackTitleBlock(title: String?, artist: String?, album: String?) {
    Text(
        text = title ?: "No track",
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
        text = "${artist ?: ""} — ${album ?: ""}",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.85f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AlbumDisc(
    discSize: Dp,
    artModel: Any?,
    discRotation: Float,
    gestureAngle: Float,
    gestureActive: Boolean,
    onGestureAngle: (Float) -> Unit,
    onGestureActive: (Boolean) -> Unit,
    onGestureEnd: () -> Unit,
    onRotate: (Float) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onCenterTap: () -> Unit,
    vuMeterEnabled: Boolean = false,
    onOpenVuMeter: () -> Unit = {}
) {
    Box(
        modifier = Modifier.size(discSize),
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

        val inner = discSize - 28.dp
        Box(
            modifier = Modifier
                .size(inner)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .rotate(discRotation)
                .pointerInput(Unit) {
                    var lastAngle = 0.0
                    var center = Offset.Zero
                    detectDragGestures(
                        onDragStart = { offset ->
                            center = Offset(size.width / 2f, size.height / 2f)
                            lastAngle = angleFromCenter(offset, center)
                            onGestureActive(true)
                        },
                        onDragEnd = {
                            onGestureActive(false)
                            onGestureEnd()
                        },
                        onDragCancel = {
                            onGestureActive(false)
                            onGestureEnd()
                        },
                        onDrag = { change, _ ->
                            val currentAngle = angleFromCenter(change.position, center)
                            var delta = currentAngle - lastAngle
                            if (delta > 180) delta -= 360
                            if (delta < -180) delta += 360
                            val d = delta.toFloat()
                            onGestureAngle(d)  // parent accumulates
                            onRotate(d)
                            val seekDelta = (delta * 50).toLong()
                            if (kotlin.math.abs(seekDelta) > 12) {
                                onSeekRelative(seekDelta)
                            }
                            lastAngle = currentAngle
                        }
                    )
                }
                .pointerInput(vuMeterEnabled) {
                    detectTapGestures { offset ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - c.x
                        val dy = offset.y - c.y
                        val r = kotlin.math.sqrt(dx * dx + dy * dy)
                        val maxR = minOf(size.width, size.height) / 2f
                        when {
                            vuMeterEnabled -> onOpenVuMeter()
                            r <= maxR / 3f -> onCenterTap()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (artModel != null) {
                AsyncImage(
                    model = artModel,
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
}

@Composable
private fun PlaybackControlsBlock(
    playerController: PlayerController,
    isPlaying: Boolean,
    isRewinding: Boolean,
    speed: Float,
    position: Long,
    duration: Long
) {
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
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f))
        )
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
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


@Composable
private fun VuMeterScreen(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onDismiss: () -> Unit
) {
    val barCount = 32
    val levels = remember { FloatArray(barCount) { 0.15f } }
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying) {
        while (true) {
            withFrameMillis {
                frame++
                val tSec = frame / 60f
                for (i in 0 until barCount) {
                    val base = if (isPlaying) {
                        val wave = (sin(tSec * 6.0 + i * 0.45) + 1.0) * 0.5
                        val pulse = (sin(tSec * 2.2 + i * 0.2) + 1.0) * 0.25
                        val jitter = Random.nextFloat() * 0.2f
                        (0.2 + wave * 0.55 + pulse + jitter).toFloat().coerceIn(0.08f, 1f)
                    } else {
                        (levels[i] * 0.92f).coerceAtLeast(0.06f)
                    }
                    levels[i] = levels[i] * 0.55f + base * 0.45f
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 48.dp)
        ) {
            val w = size.width
            val h = size.height * 0.78f
            val gap = 4.dp.toPx()
            val barW = ((w - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
            val top = size.height * 0.08f
            for (i in 0 until barCount) {
                val level = levels[i]
                val barH = h * level
                val x = i * (barW + gap)
                val y = top + (h - barH)
                val color = when {
                    level > 0.85f -> Color(0xFFFF5252)
                    level > 0.6f -> Color(0xFFFFD740)
                    else -> Color(0xFF69F0AE)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE),
                textAlign = TextAlign.Center
            )
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Tap to return",
                color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
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
