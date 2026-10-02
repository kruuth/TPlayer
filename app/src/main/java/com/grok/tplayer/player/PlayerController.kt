package com.grok.tplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.grok.tplayer.data.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    /** true while in rewind-seek mode (not playing audio backwards) */
    private val _isRewinding = MutableStateFlow(false)
    val isRewinding: StateFlow<Boolean> = _isRewinding.asStateFlow()

    private var currentQueue: List<Track> = emptyList()
    private var currentIndex: Int = 0

    // 1x is index 0; cycle goes 1 → 2 → 4 → 8 → 16 → 1
    private val speedSteps = listOf(1.0f, 2.0f, 4.0f, 8.0f, 16.0f)
    private var speedIndex = 0

    private val scope = CoroutineScope(Dispatchers.Main)
    private var rewindJob: Job? = null

    fun connect() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        stopRewindJob()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                playNext()
            }
            updatePosition()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePosition()
        }
    }

    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        currentQueue = tracks
        currentIndex = startIndex.coerceIn(0, tracks.lastIndex.coerceAtLeast(0))
        val items = tracks.map { trackToMediaItem(it) }
        controller?.setMediaItems(items, currentIndex, 0L)
        controller?.prepare()
        if (tracks.isNotEmpty()) {
            _currentTrack.value = tracks[currentIndex]
            _duration.value = tracks[currentIndex].durationMs
        }
    }

    fun play() {
        stopRewindJob()
        if (speedIndex != 0) {
            speedIndex = 0
            applySpeed()
        }
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun togglePlayPause() {
        if (_isRewinding.value) {
            stopRewindJob()
            speedIndex = 0
            applySpeed()
            controller?.play()
            return
        }
        if (_isPlaying.value) pause() else play()
    }

    fun stop() {
        stopRewindJob()
        controller?.stop()
        controller?.seekTo(0)
        _position.value = 0
        speedIndex = 0
        applySpeed()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _position.value = positionMs
    }

    fun playNext() {
        if (currentQueue.isEmpty()) return
        stopRewindJob()
        currentIndex = (currentIndex + 1) % currentQueue.size
        controller?.seekToNextMediaItem()
        _currentTrack.value = currentQueue.getOrNull(currentIndex)
        _duration.value = currentQueue.getOrNull(currentIndex)?.durationMs ?: 0
        speedIndex = 0
        applySpeed()
    }

    /**
     * Previous track, or if >3s into current track, jump to start of current track.
     */
    fun playPrevious() {
        if (currentQueue.isEmpty()) return
        stopRewindJob()
        updatePosition()
        if (_position.value > 3000L) {
            seekTo(0)
            return
        }
        currentIndex = if (currentIndex - 1 < 0) currentQueue.lastIndex else currentIndex - 1
        controller?.seekToPreviousMediaItem()
        _currentTrack.value = currentQueue.getOrNull(currentIndex)
        _duration.value = currentQueue.getOrNull(currentIndex)?.durationMs ?: 0
        speedIndex = 0
        applySpeed()
    }

    /**
     * Cycle FF: 1x → 2x → 4x → 8x → 16x → 1x (normal).
     */
    fun cycleFastForward() {
        stopRewindJob()
        speedIndex = (speedIndex + 1) % speedSteps.size
        applySpeed()
        if (speedIndex != 0 && !_isPlaying.value) {
            controller?.play()
        }
    }

    /**
     * Cycle rewind seek speed: off → 2x → 4x → 8x → 16x → off.
     * Seeks backward in steps (ExoPlayer cannot play audio in reverse).
     */
    fun cycleRewind() {
        if (!_isRewinding.value && speedIndex == 0) {
            // start rewind at 2x
            speedIndex = 1
            startRewindJob()
        } else if (_isRewinding.value) {
            speedIndex = (speedIndex + 1) % speedSteps.size
            if (speedIndex == 0) {
                stopRewindJob()
                applySpeed()
            } else {
                // restart job with new step size
                startRewindJob()
            }
        } else {
            // was in FF mode — switch to rewind
            speedIndex = 1
            controller?.pause()
            startRewindJob()
        }
        _playbackSpeed.value = if (_isRewinding.value) speedSteps[speedIndex] else 1.0f
    }

    private fun startRewindJob() {
        stopRewindJob()
        _isRewinding.value = true
        controller?.pause()
        // Keep playback params at 1x so we only seek
        controller?.playbackParameters = PlaybackParameters(1.0f)
        val step = speedSteps[speedIndex.coerceIn(0, speedSteps.lastIndex)]
        // seek back (step * 250ms) every 250ms ≈ step× realtime
        val seekAmount = (250L * step).toLong()
        rewindJob = scope.launch {
            while (isActive && _isRewinding.value) {
                updatePosition()
                val newPos = (_position.value - seekAmount).coerceAtLeast(0L)
                seekTo(newPos)
                if (newPos <= 0L) {
                    // at start — go to previous track end or stop
                    stopRewindJob()
                    speedIndex = 0
                    applySpeed()
                    break
                }
                delay(250L)
            }
        }
        _playbackSpeed.value = step
    }

    private fun stopRewindJob() {
        rewindJob?.cancel()
        rewindJob = null
        _isRewinding.value = false
    }

    fun resetSpeed() {
        stopRewindJob()
        speedIndex = 0
        applySpeed()
    }

    private fun applySpeed() {
        val speed = speedSteps[speedIndex]
        _playbackSpeed.value = speed
        controller?.playbackParameters = PlaybackParameters(speed)
    }

    fun updatePosition() {
        controller?.let {
            _position.value = it.currentPosition
            val d = it.duration
            if (d > 0) _duration.value = d
        }
    }


    /** Skip to first track of the next distinct folder in the queue. */
    fun nextFolder() {
        if (currentQueue.isEmpty()) return
        val currentFolder = currentQueue.getOrNull(currentIndex)?.folderPath
        var i = currentIndex + 1
        while (i < currentQueue.size) {
            if (currentQueue[i].folderPath != currentFolder) {
                currentIndex = i
                controller?.seekTo(i, 0L)
                _currentTrack.value = currentQueue[i]
                _duration.value = currentQueue[i].durationMs
                speedIndex = 0
                applySpeed()
                controller?.play()
                return
            }
            i++
        }
        // wrap: first folder different from current
        playNext()
    }

    fun previousFolder() {
        if (currentQueue.isEmpty()) return
        val currentFolder = currentQueue.getOrNull(currentIndex)?.folderPath
        var i = currentIndex - 1
        while (i >= 0) {
            if (currentQueue[i].folderPath != currentFolder) {
                // land on first track of that folder
                val folder = currentQueue[i].folderPath
                while (i > 0 && currentQueue[i - 1].folderPath == folder) i--
                currentIndex = i
                controller?.seekTo(i, 0L)
                _currentTrack.value = currentQueue[i]
                _duration.value = currentQueue[i].durationMs
                speedIndex = 0
                applySpeed()
                controller?.play()
                return
            }
            i--
        }
        playPrevious()
    }

    fun seekBySeconds(seconds: Int) {
        seekRelative(seconds * 1000L)
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = (_position.value + deltaMs).coerceIn(0, _duration.value.coerceAtLeast(0))
        seekTo(newPos)
    }

    private fun trackToMediaItem(track: Track): MediaItem {
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(track.displayTitle)
            .setArtist(track.displayArtist)
            .setAlbumTitle(track.displayAlbum)
        // Attach cached cover so notification / lock screen show artwork
        track.albumArtPath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) {
                metaBuilder.setArtworkUri(android.net.Uri.fromFile(file))
            }
        }
        return MediaItem.Builder()
            .setUri(track.uri)
            .setMediaId(track.id.toString())
            .setMediaMetadata(metaBuilder.build())
            .build()
    }
}
