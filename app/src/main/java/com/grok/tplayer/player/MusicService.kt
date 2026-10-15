package com.grok.tplayer.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import kotlinx.coroutines.delay
import com.grok.tplayer.R
import androidx.media3.session.CommandButton
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.ContextCompat
import android.graphics.Typeface
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Bitmap
import android.os.SystemClock
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
import androidx.media3.session.SessionError
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

    // Queue for folder skip (custom AA buttons only; steering uses system defaults)
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
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = player.currentMediaItemIndex
                if (idx >= 0) queueIndex = idx
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    scope.launch {
                        val advance = try {
                            userPreferences.settings.first().autoAdvanceTracks
                        } catch (_: Exception) { true }
                        if (advance) {
                            // Prefer next media item; fall back to our queue helper
                            if (player.hasNextMediaItem()) {
                                player.seekToNextMediaItem()
                                player.play()
                            } else if (queue.size > 1) {
                                playNextInQueue()
                            }
                        }
                    }
                }
            }
        })
        scope.launch { applyAutoAdvancePolicy() }

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

        applyTransportLayout()

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

    private fun applyAutoAdvancePolicy() {
        scope.launch {
            val advance = try {
                userPreferences.settings.first().autoAdvanceTracks
            } catch (_: Exception) { true }
            // When false, stop after each track; when true, continue through the queue
            player.pauseAtEndOfMediaItems = !advance
            if (advance && player.repeatMode == Player.REPEAT_MODE_ONE) {
                player.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    private fun seekByMs(delta: Long) {
        val pos = player.currentPosition + delta
        player.seekTo(pos.coerceAtLeast(0L))
    }

    private fun playNextInQueue() {
        val count = player.mediaItemCount
        if (count <= 0) return
        // Keep queueIndex aligned with the player
        val idx = player.currentMediaItemIndex.coerceAtLeast(0)
        queueIndex = idx

        // Prefer jumping into the next folder when this was the last track of the current one
        if (queue.isNotEmpty() && queueIndex in queue.indices) {
            val curFolder = queue[queueIndex].folderPath
            val isLastInFolder = queueIndex == queue.lastIndex ||
                queue[queueIndex + 1].folderPath != curFolder
            if (isLastInFolder) {
                // nextFolderInQueue seeks + plays; if no next folder, fall through to wrap
                if (nextFolderInQueue()) return
            }
        }

        val next = (idx + 1) % count
        queueIndex = next
        player.seekTo(next, 0L)
        player.play()
    }

    /**
     * @param restartIfMidTrack classic media behavior (UI previous).
     * Steering uses false so a single tap always goes to the previous track.
     */
    private fun playPrevInQueue(restartIfMidTrack: Boolean = true) {
        if (restartIfMidTrack && player.currentPosition > 3_000L) {
            player.seekTo(0)
            return
        }
        val count = player.mediaItemCount
        if (count <= 0) return
        val idx = player.currentMediaItemIndex
        val prev = if (idx <= 0) count - 1 else idx - 1
        queueIndex = prev
        player.seekTo(prev, 0L)
        player.play()
    }

    private fun nextFolderInQueue(): Boolean {
        if (queue.isEmpty()) {
            beepIfEnabled()
            return false
        }
        // Align with player in case indices drifted
        val playerIdx = player.currentMediaItemIndex
        if (playerIdx in queue.indices) queueIndex = playerIdx
        val cur = queue.getOrNull(queueIndex)?.folderPath
        var i = queueIndex + 1
        while (i < queue.size) {
            if (queue[i].folderPath != cur) {
                queueIndex = i
                player.seekTo(i, 0L)
                player.play()
                return true
            }
            i++
        }
        // Already on last folder
        beepIfEnabled()
        return false
    }

    private fun prevFolderInQueue(): Boolean {
        if (queue.isEmpty()) {
            beepIfEnabled()
            return false
        }
        val playerIdx = player.currentMediaItemIndex
        if (playerIdx in queue.indices) queueIndex = playerIdx
        val cur = queue.getOrNull(queueIndex)?.folderPath
        var i = queueIndex - 1
        while (i >= 0) {
            if (queue[i].folderPath != cur) {
                val folder = queue[i].folderPath
                while (i > 0 && queue[i - 1].folderPath == folder) i--
                queueIndex = i
                player.seekTo(i, 0L)
                player.play()
                return true
            }
            i--
        }
        beepIfEnabled()
        return false
    }

    private fun toggleRepeatMode() {
        val next = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = next
        showSteeringFeedback(
            when (next) {
                Player.REPEAT_MODE_ALL -> "REPEAT ALL"
                Player.REPEAT_MODE_ONE -> "REPEAT ONE"
                else -> "REPEAT OFF"
            }
        )
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
                .add(SessionCommand(CMD_NEXT_TRACK, Bundle.EMPTY))
                .add(SessionCommand(CMD_PREV_TRACK, Bundle.EMPTY))
                .add(SessionCommand(CMD_TOGGLE_REPEAT, Bundle.EMPTY))
                .build()
            // Explicitly advertise track skip so AA shows Previous / Next on the playback UI
            val playerCmds = result.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_BACK)
                .add(Player.COMMAND_SEEK_FORWARD)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_GET_TIMELINE)
                .build()
            applyTransportLayout()
            return MediaSession.ConnectionResult.accept(cmds, playerCmds)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_JUMP_BACK -> {
                    seekByMs(-10_000)
                    showSteeringFeedback("−10s")
                }
                CMD_JUMP_FORWARD -> {
                    seekByMs(10_000)
                    showSteeringFeedback("+10s")
                }
                CMD_NEXT_TRACK -> {
                    playNextInQueue()
                    showSteeringFeedback("NEXT TRACK")
                }
                CMD_PREV_TRACK -> {
                    playPrevInQueue(restartIfMidTrack = false)
                    showSteeringFeedback("PREV TRACK")
                }
                CMD_NEXT_FOLDER -> {
                    if (nextFolderInQueue()) showSteeringFeedback("NEXT FOLDER")
                    else showSteeringFeedback("LAST FOLDER")
                }
                CMD_PREV_FOLDER -> {
                    if (prevFolderInQueue()) showSteeringFeedback("PREV FOLDER")
                    else showSteeringFeedback("FIRST FOLDER")
                }
                CMD_TOGGLE_REPEAT -> toggleRepeatMode()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        /**
         * Steering wheel Next / Previous:
         *   1 tap  → seek ±10 seconds
         *   2 taps → next / previous track
         *   3 taps → next / previous folder
         * Play/pause and other keys use the system default.
         */
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
            } ?: return super.onMediaButtonEvent(session, controllerInfo, intent)

            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> {
                    handleMultiTapMediaButton(event, forward = true)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> {
                    handleMultiTapMediaButton(event, forward = false)
                    return true
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
                    val (resolved, start) = withContext(Dispatchers.IO) {
                        resolveAndExpandQueue(mediaItems, startIndex)
                    }
                    if (resolved.isNotEmpty()) {
                        queue = withContext(Dispatchers.IO) {
                            resolved.mapNotNull { item ->
                                val tid = item.mediaId.removePrefix("track:").toLongOrNull()
                                tid?.let { musicRepository.getTrackById(it) }
                            }
                        }
                        queueIndex = start.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
                    }
                    applyAutoAdvancePolicy()
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            resolved,
                            start.coerceIn(0, (resolved.size - 1).coerceAtLeast(0)),
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


        /**
         * Build a library-wide queue sorted by folder, then title.
         * Single-track (or single-folder) play requests expand to this full queue so that
         * advancing past the last song in e.g. "1990" continues into the next folder.
         */
        private suspend fun libraryQueueSorted(): List<com.grok.tplayer.data.model.Track> {
            return musicRepository.getAllTracks().first()
                .sortedWith(
                    compareBy(
                        { it.folderPath.lowercase() },
                        { it.displayTitle.lowercase() }
                    )
                )
        }

        private suspend fun resolveAndExpandQueue(
            mediaItems: List<MediaItem>,
            requestedStart: Int
        ): Pair<List<MediaItem>, Int> {
            // Prefer a full cross-folder queue whenever we can identify a start track
            if (mediaItems.size == 1) {
                val id = mediaItems[0].mediaId
                when {
                    id.startsWith("track:") -> {
                        val trackId = id.removePrefix("track:").toLongOrNull()
                        val track = trackId?.let { musicRepository.getTrackById(it) }
                        if (track != null) {
                            val all = libraryQueueSorted()
                            if (all.isNotEmpty()) {
                                val items = all.map { trackToPlayableItem(it) }
                                val idx = all.indexOfFirst { it.id == track.id }.let {
                                    if (it >= 0) it else 0
                                }
                                return items to idx
                            }
                        }
                    }
                    id.startsWith("$ID_FOLDERS/") -> {
                        val folder = id.removePrefix("$ID_FOLDERS/")
                        val all = libraryQueueSorted()
                        if (all.isNotEmpty()) {
                            val items = all.map { trackToPlayableItem(it) }
                            val idx = all.indexOfFirst { it.folderPath == folder }.let {
                                if (it >= 0) it else 0
                            }
                            return items to idx
                        }
                    }
                    id.startsWith("$ID_ARTISTS/") -> {
                        val artist = id.removePrefix("$ID_ARTISTS/")
                        // Artist play: keep artist order, but after last song of last album
                        // fall through isn't cross-folder; still load artist list sorted by album/title
                        val list = musicRepository.getTracksByArtist(artist).first()
                            .sortedWith(
                                compareBy(
                                    { it.folderPath.lowercase() },
                                    { it.displayTitle.lowercase() }
                                )
                            )
                        if (list.isNotEmpty()) {
                            return list.map { trackToPlayableItem(it) } to 0
                        }
                    }
                }
            }
            // Multi-item play from phone UI / setMediaItems: if they all share one folder,
            // still expand to full library so end-of-folder advances to the next folder.
            val resolved = resolveMediaItems(mediaItems)
            if (resolved.size > 1) {
                val trackIds = resolved.mapNotNull {
                    it.mediaId.removePrefix("track:").toLongOrNull()
                }
                val tracks = trackIds.mapNotNull { musicRepository.getTrackById(it) }
                val folders = tracks.map { it.folderPath }.toSet()
                if (folders.size == 1 && tracks.isNotEmpty()) {
                    val startTrackId = tracks.getOrNull(
                        requestedStart.coerceIn(0, tracks.lastIndex)
                    )?.id
                    val all = libraryQueueSorted()
                    if (all.isNotEmpty()) {
                        val items = all.map { trackToPlayableItem(it) }
                        val idx = all.indexOfFirst { it.id == startTrackId }.let {
                            if (it >= 0) it else 0
                        }
                        return items to idx
                    }
                }
            }
            val start = requestedStart.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
            return resolved to start
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
                    id == ID_RESUME_LAST -> {
                        val settings = userPreferences.settings.first()
                        val all = musicRepository.getAllTracks().first()
                        if (all.isNotEmpty()) {
                            val idx = all.indexOfFirst { it.id == settings.lastTrackId }.let {
                                if (it >= 0) it else 0
                            }
                            // Full queue starting at last track so next/prev work
                            all.forEach { out.add(trackToPlayableItem(it)) }
                            // Caller uses startIndex 0 by default; we reorder so last is first
                            if (idx > 0) {
                                val rotated = all.drop(idx) + all.take(idx)
                                out.clear()
                                rotated.forEach { out.add(trackToPlayableItem(it)) }
                            }
                        }
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
                    future.set(LibraryResult.ofError(SessionError.ERROR_IO))
                }
            }
            return future
        }

        private suspend fun buildChildren(parentId: String): List<MediaItem> {
            return when {
                parentId == ROOT_ID -> {
                    // Selection screen first; Play last track is a one-tap resume
                    listOf(
                        MediaItem.Builder()
                            .setMediaId(ID_RESUME_LAST)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle("Play last track")
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                    .build()
                            ).build(),
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
                    .setTitle(title.trim())
                    .setDisplayTitle(title.trim())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setExtras(Bundle().apply {
                        putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
                    })
                    .build()
            ).build()

        private fun trackBrowseItem(track: com.grok.tplayer.data.model.Track): MediaItem {
            // Flat, cleaned title — AA indents when titles have leading spaces/numbers path noise
            val title = cleanBrowseTitle(track.displayTitle)
            val artist = track.displayArtist.trim()
            // Single primary line only (title). Secondary text as subtitle string avoids hierarchy indent.
            // No artwork on browse list — missing art causes AA list indentation
            val meta = MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title)
                .setSubtitle(artist)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply {
                    putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
                    putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
                })
            return MediaItem.Builder()
                .setMediaId("track:${track.id}")
                .setUri(android.net.Uri.parse(track.uri))
                .setMediaMetadata(meta.build())
                .build()
        }

        private fun cleanBrowseTitle(raw: String): String {
            var s = raw.trim().trimStart(' ', '	', ' ')
            // Strip common leading track-number prefixes that look "indented"
            s = s.replace(Regex("""^0*(\d{1,3})\s*[-.)]\s*"""), "")
            s = s.replace(Regex("""^0*(\d{1,3})\s+"""), "")
            return s.trim()
        }

        private fun trackToPlayableItem(track: com.grok.tplayer.data.model.Track): MediaItem {
            val meta = MediaMetadata.Builder()
                .setTitle(track.displayTitle)
                .setArtist(track.displayArtist)
                .setAlbumTitle(track.displayAlbum)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            var hasArt = false
            track.albumArtPath?.let { path ->
                val f = java.io.File(path)
                if (f.exists()) {
                    meta.setArtworkUri(android.net.Uri.fromFile(f))
                    hasArt = true
                }
            }
            if (!hasArt) {
                // Large app icon as fallback on AA / notification playback screen
                meta.setArtworkUri(appIconUri())
            }
            return MediaItem.Builder()
                .setMediaId("track:${track.id}")
                .setUri(android.net.Uri.parse(track.uri))
                .setMediaMetadata(meta.build())
                .build()
        }
    }

    private fun appIconUri(): android.net.Uri =
        android.net.Uri.parse("android.resource://${packageName}/${R.mipmap.ic_launcher}")

    /**
     * Android Auto / notification transport layout.
     *
     * Primary row (slots):  Prev folder | Prev track | Play/Pause | Next track | Next folder
     * Secondary / overflow: −10s | +10s | Repeat
     *
     * Cars still decide exact placement; slots + order give the strongest hint.
     */
    private fun applyTransportLayout() {
        val buttons = buildCustomLayout()
        librarySession?.setCustomLayout(buttons)
        // Media3 1.5+: stronger hint for AA button placement
        try {
            librarySession?.setMediaButtonPreferences(buttons)
        } catch (_: Exception) {
        }
    }

    private fun buildCustomLayout(): ImmutableList<CommandButton> {
        // Primary slots = previous / next *track* (what users expect on the main row).
        // Folder / ±10s / repeat go to secondary or overflow so they do not replace track skip.
        // Session commands (not player commands) so AA on-screen arrows always hit our handlers
        val prevTrack = CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setDisplayName("Previous")
            .setSessionCommand(SessionCommand(CMD_PREV_TRACK, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_BACK)
            .build()
        val nextTrack = CommandButton.Builder(CommandButton.ICON_NEXT)
            .setDisplayName("Next")
            .setSessionCommand(SessionCommand(CMD_NEXT_TRACK, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_FORWARD)
            .build()
        val prevFolder = CommandButton.Builder(CommandButton.ICON_REWIND)
            .setDisplayName("Prev folder")
            .setSessionCommand(SessionCommand(CMD_PREV_FOLDER, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_BACK_SECONDARY, CommandButton.SLOT_OVERFLOW)
            .build()
        val nextFolder = CommandButton.Builder(CommandButton.ICON_FAST_FORWARD)
            .setDisplayName("Next folder")
            .setSessionCommand(SessionCommand(CMD_NEXT_FOLDER, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_FORWARD_SECONDARY, CommandButton.SLOT_OVERFLOW)
            .build()
        val jumpBack = CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setDisplayName("-10 sec")
            .setSessionCommand(SessionCommand(CMD_JUMP_BACK, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
        val jumpFwd = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10)
            .setDisplayName("+10 sec")
            .setSessionCommand(SessionCommand(CMD_JUMP_FORWARD, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
        val repeat = CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
            .setDisplayName("Repeat")
            .setSessionCommand(SessionCommand(CMD_TOGGLE_REPEAT, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
        return ImmutableList.of(
            prevTrack, nextTrack,
            prevFolder, nextFolder,
            jumpBack, jumpFwd, repeat
        )
    }

    private var feedbackRestoreJob: Runnable? = null
    private var savedArtworkUri: android.net.Uri? = null

    /** Briefly replace album art with a label showing which steering action was used. */
    private fun showSteeringFeedback(label: String) {
        scope.launch {
            val enabled = try {
                userPreferences.settings.first().showSteeringFeedback
            } catch (_: Exception) { false }
            if (!enabled) return@launch
            try {
                val idx = player.currentMediaItemIndex
                if (idx < 0) return@launch
                val item = player.getMediaItemAt(idx)
                if (savedArtworkUri == null) {
                    savedArtworkUri = item.mediaMetadata.artworkUri
                }
                val file = java.io.File(cacheDir, "steering_feedback.png")
                val bmp = makeFeedbackBitmap(label)
                java.io.FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                val feedbackUri = android.net.Uri.fromFile(file)
                val newMeta = item.mediaMetadata.buildUpon()
                    .setArtworkUri(feedbackUri)
                    .build()
                player.replaceMediaItem(
                    idx,
                    item.buildUpon().setMediaMetadata(newMeta).build()
                )
                feedbackRestoreJob?.let { holdJob?.removeCallbacks(it) }
                val restore = Runnable {
                    try {
                        val i2 = player.currentMediaItemIndex
                        if (i2 < 0) return@Runnable
                        val cur = player.getMediaItemAt(i2)
                        val restored = cur.mediaMetadata.buildUpon()
                            .setArtworkUri(savedArtworkUri ?: appIconUri())
                            .build()
                        player.replaceMediaItem(
                            i2,
                            cur.buildUpon().setMediaMetadata(restored).build()
                        )
                    } catch (_: Exception) {
                    } finally {
                        savedArtworkUri = null
                    }
                }
                feedbackRestoreJob = restore
                holdJob?.postDelayed(restore, 1500L)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun makeFeedbackBitmap(label: String): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFF1A1A1A.toInt())
        // App icon watermark
        try {
            val d = ContextCompat.getDrawable(this, R.mipmap.ic_launcher)
            if (d != null) {
                val icon = d.toBitmap(size / 2, size / 2)
                canvas.drawBitmap(icon, (size - icon.width) / 2f, size * 0.18f, null)
            }
        } catch (_: Exception) {
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        canvas.drawText(label, size / 2f, size * 0.78f, paint)
        return bmp
    }


    // Multi-tap steering: 1 = ±10s, 2 = track, 3 = folder
    private var nextTapCount = 0
    private var prevTapCount = 0
    private var nextTapRunnable: Runnable? = null
    private var prevTapRunnable: Runnable? = null
    private val multiTapWindowMs = 800L

    private fun handleMultiTapMediaButton(event: KeyEvent, forward: Boolean) {
        // Count ACTION_DOWN only (ignore long-press repeats)
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return

        if (forward) {
            nextTapCount++
            nextTapRunnable?.let { holdJob?.removeCallbacks(it) }
            if (nextTapCount >= 3) {
                nextTapCount = 0
                beepIfEnabled()
                if (nextFolderInQueue()) showSteeringFeedback("NEXT FOLDER")
                else showSteeringFeedback("LAST FOLDER")
                return
            }
            val r = Runnable {
                when (nextTapCount) {
                    1 -> {
                        beepIfEnabled()
                        seekByMs(10_000)
                        showSteeringFeedback("+10s")
                    }
                    2 -> {
                        beepIfEnabled()
                        playNextInQueue()
                        showSteeringFeedback("NEXT TRACK")
                    }
                }
                nextTapCount = 0
            }
            nextTapRunnable = r
            holdJob?.postDelayed(r, multiTapWindowMs)
        } else {
            prevTapCount++
            prevTapRunnable?.let { holdJob?.removeCallbacks(it) }
            if (prevTapCount >= 3) {
                prevTapCount = 0
                beepIfEnabled()
                if (prevFolderInQueue()) showSteeringFeedback("PREV FOLDER")
                else showSteeringFeedback("FIRST FOLDER")
                return
            }
            val r = Runnable {
                when (prevTapCount) {
                    1 -> {
                        beepIfEnabled()
                        seekByMs(-10_000)
                        showSteeringFeedback("−10s")
                    }
                    2 -> {
                        beepIfEnabled()
                        playPrevInQueue(restartIfMidTrack = false)
                        showSteeringFeedback("PREV TRACK")
                    }
                }
                prevTapCount = 0
            }
            prevTapRunnable = r
            holdJob?.postDelayed(r, multiTapWindowMs)
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
        const val ID_RESUME_LAST = "resume_last"
        const val CMD_JUMP_BACK = "tplayer.jump_back"
        const val CMD_JUMP_FORWARD = "tplayer.jump_forward"
        const val CMD_NEXT_FOLDER = "tplayer.next_folder"
        const val CMD_PREV_FOLDER = "tplayer.prev_folder"
        const val CMD_NEXT_TRACK = "tplayer.next_track"
        const val CMD_PREV_TRACK = "tplayer.prev_track"
        const val CMD_TOGGLE_REPEAT = "tplayer.toggle_repeat"
    }
}

