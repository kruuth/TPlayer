package com.grok.tplayer.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.grok.tplayer.MainActivity
import com.grok.tplayer.data.preferences.UserPreferences
import com.grok.tplayer.data.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Phone notification + Android Auto library/playback.
 * Does NOT inject PlayerController (avoids circular Hilt / deadlocks).
 * Phone UI controls the same ExoPlayer through MediaController.
 */
@UnstableApi
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var userPreferences: UserPreferences

    private var librarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var holdJob: android.os.Handler? = null

    private var lastNextDown = 0L
    private var lastPrevDown = 0L
    private var nextTapCount = 0
    private var prevTapCount = 0
    private var nextHolding = false
    private var prevHolding = false

    // Queue for folder skip / next-previous on steering
    private var queue: List<com.grok.tplayer.data.model.Track> = emptyList()
    private var queueIndex = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        holdJob = android.os.Handler(mainLooper)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        librarySession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(sessionActivity)
            .setId("tplayer_session")
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        librarySession

    override fun onDestroy() {
        holdJob?.removeCallbacksAndMessages(null)
        librarySession?.run {
            player.release()
            release()
            librarySession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = librarySession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 ||
            p.playbackState == Player.STATE_ENDED
        ) stopSelf()
    }

    private fun beepIfEnabled() {
        scope.launch {
            val enabled = try {
                userPreferences.settings.first().steeringBeepEnabled
            } catch (_: Exception) { true }
            if (!enabled) return@launch
            try {
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                holdJob?.postDelayed({ tg.release() }, 150)
            } catch (_: Exception) { }
        }
    }

    private fun seekByMs(delta: Long) {
        val pos = player.currentPosition + delta
        player.seekTo(pos.coerceAtLeast(0L))
    }

    private fun playNextInQueue() {
        if (queue.isEmpty()) {
            player.seekToNextMediaItem()
            return
        }
        queueIndex = (queueIndex + 1).coerceAtMost(queue.lastIndex)
        if (queueIndex < player.mediaItemCount) {
            player.seekTo(queueIndex, 0L)
            player.play()
        } else {
            player.seekToNextMediaItem()
        }
    }

    private fun playPrevInQueue() {
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }
        if (queue.isEmpty()) {
            player.seekToPreviousMediaItem()
            return
        }
        queueIndex = (queueIndex - 1).coerceAtLeast(0)
        player.seekTo(queueIndex, 0L)
        player.play()
    }

    private fun nextFolderInQueue() {
        if (queue.isEmpty()) {
            playNextInQueue(); return
        }
        val cur = queue.getOrNull(queueIndex)?.folderPath
        var i = queueIndex + 1
        while (i < queue.size) {
            if (queue[i].folderPath != cur) {
                queueIndex = i
                player.seekTo(i, 0L)
                player.play()
                return
            }
            i++
        }
        playNextInQueue()
    }

    private fun prevFolderInQueue() {
        if (queue.isEmpty()) {
            playPrevInQueue(); return
        }
        val cur = queue.getOrNull(queueIndex)?.folderPath
        var i = queueIndex - 1
        while (i >= 0) {
            if (queue[i].folderPath != cur) {
                val folder = queue[i].folderPath
                while (i > 0 && queue[i - 1].folderPath == folder) i--
                queueIndex = i
                player.seekTo(i, 0L)
                player.play()
                return
            }
            i--
        }
        playPrevInQueue()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val result = super.onConnect(session, controller)
            val cmds = result.availableSessionCommands.buildUpon()
                .add(SessionCommand(CMD_JUMP_BACK, Bundle.EMPTY))
                .add(SessionCommand(CMD_JUMP_FORWARD, Bundle.EMPTY))
                .add(SessionCommand(CMD_NEXT_FOLDER, Bundle.EMPTY))
                .add(SessionCommand(CMD_PREV_FOLDER, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(cmds, result.availablePlayerCommands)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_JUMP_BACK -> seekByMs(-10_000)
                CMD_JUMP_FORWARD -> seekByMs(10_000)
                CMD_NEXT_FOLDER -> nextFolderInQueue()
                CMD_PREV_FOLDER -> prevFolderInQueue()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            val event = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            } ?: return false

            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                        beepIfEnabled()
                        if (player.isPlaying) player.pause() else player.play()
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    handleNextMediaButton(event); return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    handlePrevMediaButton(event); return true
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        /**
         * Critical for Android Auto: resolve media IDs and return playable items.
         * Without this, AA hangs on "Getting your selection".
         */
        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            scope.launch {
                try {
                    val resolved = withContext(Dispatchers.IO) {
                        resolveMediaItems(mediaItems)
                    }
                    if (resolved.isNotEmpty()) {
                        queue = withContext(Dispatchers.IO) {
                            resolved.mapNotNull { item ->
                                val tid = item.mediaId.removePrefix("track:").toLongOrNull()
                                tid?.let { musicRepository.getTrackById(it) }
                            }
                        }
                        queueIndex = startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
                    }
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            resolved,
                            startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0)),
                            startPositionMs
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L)
                    )
                }
            }
            return future
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            scope.launch {
                try {
                    val resolved = withContext(Dispatchers.IO) { resolveMediaItems(mediaItems) }
                    future.set(resolved)
                } catch (e: Exception) {
                    future.set(emptyList())
                }
            }
            return future
        }

        private suspend fun resolveMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
            val out = mutableListOf<MediaItem>()
            for (item in mediaItems) {
                val id = item.mediaId
                when {
                    id.startsWith("track:") -> {
                        val trackId = id.removePrefix("track:").toLongOrNull() ?: continue
                        val track = musicRepository.getTrackById(trackId) ?: continue
                        out.add(trackToPlayableItem(track))
                    }
                    id == ID_NOW_PLAYING -> {
                        if (player.mediaItemCount > 0) {
                            for (i in 0 until player.mediaItemCount) {
                                out.add(player.getMediaItemAt(i))
                            }
                        } else if (queue.isNotEmpty()) {
                            queue.forEach { out.add(trackToPlayableItem(it)) }
                        } else {
                            // Fallback: all tracks so AA can still play something
                            musicRepository.getAllTracks().first().forEach {
                                out.add(trackToPlayableItem(it))
                            }
                        }
                    }
                    id.startsWith("$ID_ARTISTS/") -> {
                        val artist = id.removePrefix("$ID_ARTISTS/")
                        musicRepository.getTracksByArtist(artist).first()
                            .forEach { out.add(trackToPlayableItem(it)) }
                    }
                    id.startsWith("$ID_FOLDERS/") -> {
                        val folder = id.removePrefix("$ID_FOLDERS/")
                        musicRepository.getTracksByFolder(folder).first()
                            .forEach { out.add(trackToPlayableItem(it)) }
                    }
                    id == ID_ALL -> {
                        musicRepository.getAllTracks().first()
                            .forEach { out.add(trackToPlayableItem(it)) }
                    }
                    item.localConfiguration?.uri != null -> out.add(item)
                }
            }
            return out
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val extras = Bundle().apply {
                putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
                putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
            }
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("TPlayer")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setExtras(extras)
                        .build()
                ).build()
            val outParams = MediaLibraryService.LibraryParams.Builder()
                .setExtras(extras)
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, outParams))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            scope.launch {
                try {
                    val items = withContext(Dispatchers.IO) { buildChildren(parentId) }
                    future.set(LibraryResult.ofItemList(items, params))
                } catch (e: Exception) {
                    e.printStackTrace()
                    future.set(LibraryResult.ofError(SessionResult.RESULT_ERROR_IO))
                }
            }
            return future
        }

        private suspend fun buildChildren(parentId: String): List<MediaItem> {
            return when {
                parentId == ROOT_ID -> {
                    // Always start AA browse with library sections + Now Playing
                    listOf(
                        folderItem(ID_ARTISTS, "Artists"),
                        folderItem(ID_FOLDERS, "Folders"),
                        folderItem(ID_ALL, "All Tracks"),
                        MediaItem.Builder()
                            .setMediaId(ID_NOW_PLAYING)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle("Now Playing")
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                    .build()
                            ).build()
                    )
                }
                parentId == ID_ARTISTS ->
                    musicRepository.getAllArtists().first().map { folderItem("$ID_ARTISTS/$it", it) }
                parentId.startsWith("$ID_ARTISTS/") -> {
                    val artist = parentId.removePrefix("$ID_ARTISTS/")
                    musicRepository.getTracksByArtist(artist).first().map { trackBrowseItem(it) }
                }
                parentId == ID_FOLDERS ->
                    musicRepository.getAllFolders().first().map {
                        folderItem("$ID_FOLDERS/$it", it.substringAfterLast('/').ifBlank { it })
                    }
                parentId.startsWith("$ID_FOLDERS/") -> {
                    val folder = parentId.removePrefix("$ID_FOLDERS/")
                    musicRepository.getTracksByFolder(folder).first().map { trackBrowseItem(it) }
                }
                parentId == ID_ALL ->
                    musicRepository.getAllTracks().first().map { trackBrowseItem(it) }
                else -> emptyList()
            }
        }

        private fun folderItem(id: String, title: String) = MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            ).build()

        private fun trackBrowseItem(track: com.grok.tplayer.data.model.Track): MediaItem {
            val meta = MediaMetadata.Builder()
                .setTitle(track.displayTitle)
                .setArtist(track.displayArtist)
                .setAlbumTitle(track.displayAlbum)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            track.albumArtPath?.let { path ->
                val f = java.io.File(path)
                if (f.exists()) meta.setArtworkUri(android.net.Uri.fromFile(f))
            }
            return MediaItem.Builder()
                .setMediaId("track:${track.id}")
                .setUri(track.uri)
                .setMediaMetadata(meta.build())
                .build()
        }

        private fun trackToPlayableItem(track: com.grok.tplayer.data.model.Track): MediaItem {
            val meta = MediaMetadata.Builder()
                .setTitle(track.displayTitle)
                .setArtist(track.displayArtist)
                .setAlbumTitle(track.displayAlbum)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            track.albumArtPath?.let { path ->
                val f = java.io.File(path)
                if (f.exists()) meta.setArtworkUri(android.net.Uri.fromFile(f))
            }
            return MediaItem.Builder()
                .setMediaId("track:${track.id}")
                .setUri(android.net.Uri.parse(track.uri))
                .setMediaMetadata(meta.build())
                .build()
        }
    }

    // Separate runnables so canceling hold does not cancel double-tap detection
    private val nextHoldRunnable = object : Runnable {
        override fun run() {
            nextHolding = true
            nextTapCount = 0
            beepIfEnabled()
            seekByMs(10_000)
            holdJob?.postDelayed(this, 2000L) // keep seeking every 2s while held
        }
    }
    private val prevHoldRunnable = object : Runnable {
        override fun run() {
            prevHolding = true
            prevTapCount = 0
            beepIfEnabled()
            seekByMs(-10_000)
            holdJob?.postDelayed(this, 2000L)
        }
    }
    private var nextTapRunnable: Runnable? = null
    private var prevTapRunnable: Runnable? = null

    /**
     * Steering / headset Next:
     *  - short single tap → next track
     *  - double tap → next folder
     *  - hold (≥500ms) → +10s, then +10s every 2s while held
     */
    private fun handleNextMediaButton(event: KeyEvent) {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    lastNextDown = SystemClock.elapsedRealtime()
                    nextHolding = false
                    holdJob?.removeCallbacks(nextHoldRunnable)
                    // Start hold timer after 500ms (not 2s) so short taps never seek
                    holdJob?.postDelayed(nextHoldRunnable, 500L)
                }
            }
            KeyEvent.ACTION_UP -> {
                holdJob?.removeCallbacks(nextHoldRunnable)
                if (nextHolding) {
                    // Was a long-press seek session
                    nextHolding = false
                    nextTapCount = 0
                    nextTapRunnable?.let { holdJob?.removeCallbacks(it) }
                    return
                }
                val heldMs = SystemClock.elapsedRealtime() - lastNextDown
                if (heldMs >= 500L) return // treated as hold, already canceled

                nextTapCount++
                nextTapRunnable?.let { holdJob?.removeCallbacks(it) }
                val taps = nextTapCount
                val r = Runnable {
                    when {
                        taps >= 2 -> {
                            beepIfEnabled()
                            nextFolderInQueue()
                        }
                        else -> {
                            beepIfEnabled()
                            playNextInQueue()
                        }
                    }
                    nextTapCount = 0
                }
                nextTapRunnable = r
                holdJob?.postDelayed(r, 320L)
            }
        }
    }

    private fun handlePrevMediaButton(event: KeyEvent) {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    lastPrevDown = SystemClock.elapsedRealtime()
                    prevHolding = false
                    holdJob?.removeCallbacks(prevHoldRunnable)
                    holdJob?.postDelayed(prevHoldRunnable, 500L)
                }
            }
            KeyEvent.ACTION_UP -> {
                holdJob?.removeCallbacks(prevHoldRunnable)
                if (prevHolding) {
                    prevHolding = false
                    prevTapCount = 0
                    prevTapRunnable?.let { holdJob?.removeCallbacks(it) }
                    return
                }
                val heldMs = SystemClock.elapsedRealtime() - lastPrevDown
                if (heldMs >= 500L) return

                prevTapCount++
                prevTapRunnable?.let { holdJob?.removeCallbacks(it) }
                val taps = prevTapCount
                val r = Runnable {
                    when {
                        taps >= 2 -> {
                            beepIfEnabled()
                            prevFolderInQueue()
                        }
                        else -> {
                            beepIfEnabled()
                            playPrevInQueue()
                        }
                    }
                    prevTapCount = 0
                }
                prevTapRunnable = r
                holdJob?.postDelayed(r, 320L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "tplayer_playback"
        const val NOTIFICATION_ID = 1001
        const val ROOT_ID = "root"
        const val ID_ARTISTS = "artists"
        const val ID_FOLDERS = "folders"
        const val ID_ALL = "all"
        const val ID_NOW_PLAYING = "now_playing"
        const val CMD_JUMP_BACK = "tplayer.jump_back"
        const val CMD_JUMP_FORWARD = "tplayer.jump_forward"
        const val CMD_NEXT_FOLDER = "tplayer.next_folder"
        const val CMD_PREV_FOLDER = "tplayer.prev_folder"
    }
}
