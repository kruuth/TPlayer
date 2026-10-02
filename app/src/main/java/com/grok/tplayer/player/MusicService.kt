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
import com.grok.tplayer.MainActivity
import com.grok.tplayer.data.preferences.UserPreferences
import com.grok.tplayer.data.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var playerController: PlayerController

    private var librarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var holdJob: android.os.Handler? = null

    private var lastNextDown = 0L
    private var lastPrevDown = 0L
    private var nextTapCount = 0
    private var prevTapCount = 0
    private var nextHolding = false
    private var prevHolding = false

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
                CMD_JUMP_BACK -> playerController.seekBySeconds(-10)
                CMD_JUMP_FORWARD -> playerController.seekBySeconds(10)
                CMD_NEXT_FOLDER -> playerController.nextFolder()
                CMD_PREV_FOLDER -> playerController.previousFolder()
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
                        playerController.togglePlayPause()
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    handleNextMediaButton(event)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    handlePrevMediaButton(event)
                    return true
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("TPlayer")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                ).build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return try {
                val items = runBlocking { buildChildren(parentId) }
                Futures.immediateFuture(LibraryResult.ofItemList(items, params))
            } catch (_: Exception) {
                Futures.immediateFuture(LibraryResult.ofError(SessionResult.RESULT_ERROR_UNKNOWN))
            }
        }

        private suspend fun buildChildren(parentId: String): List<MediaItem> {
            return when {
                parentId == ROOT_ID -> listOf(
                    folderItem(ID_ARTISTS, "Artists"),
                    folderItem(ID_FOLDERS, "Folders"),
                    folderItem(ID_ALL, "All Tracks")
                )
                parentId == ID_ARTISTS ->
                    musicRepository.getAllArtists().first().map { folderItem("$ID_ARTISTS/$it", it) }
                parentId.startsWith("$ID_ARTISTS/") -> {
                    val artist = parentId.removePrefix("$ID_ARTISTS/")
                    musicRepository.getTracksByArtist(artist).first().map { trackItem(it) }
                }
                parentId == ID_FOLDERS ->
                    musicRepository.getAllFolders().first().map {
                        folderItem("$ID_FOLDERS/$it", it.substringAfterLast('/'))
                    }
                parentId.startsWith("$ID_FOLDERS/") -> {
                    val folder = parentId.removePrefix("$ID_FOLDERS/")
                    musicRepository.getTracksByFolder(folder).first().map { trackItem(it) }
                }
                parentId == ID_ALL ->
                    musicRepository.getAllTracks().first().map { trackItem(it) }
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

        private fun trackItem(track: com.grok.tplayer.data.model.Track): MediaItem {
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
    }

    private fun handleNextMediaButton(event: KeyEvent) {
        val now = SystemClock.elapsedRealtime()
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            lastNextDown = now
            nextHolding = false
            holdJob?.postDelayed({
                nextHolding = true
                beepIfEnabled()
                playerController.seekBySeconds(10)
                val r = object : Runnable {
                    override fun run() {
                        if (nextHolding) {
                            playerController.seekBySeconds(10)
                            holdJob?.postDelayed(this, 2000L)
                        }
                    }
                }
                holdJob?.postDelayed(r, 2000L)
            }, 2000L)
        } else if (event.action == KeyEvent.ACTION_UP) {
            holdJob?.removeCallbacksAndMessages(null)
            if (nextHolding) {
                nextHolding = false
                nextTapCount = 0
                return
            }
            if (now - lastNextDown < 2000L) {
                nextTapCount++
                holdJob?.postDelayed({
                    if (nextTapCount >= 2) {
                        beepIfEnabled(); playerController.nextFolder()
                    } else if (nextTapCount == 1) {
                        beepIfEnabled(); playerController.playNext()
                    }
                    nextTapCount = 0
                }, 350L)
            }
        }
    }

    private fun handlePrevMediaButton(event: KeyEvent) {
        val now = SystemClock.elapsedRealtime()
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            lastPrevDown = now
            prevHolding = false
            holdJob?.postDelayed({
                prevHolding = true
                beepIfEnabled()
                playerController.seekBySeconds(-10)
                val r = object : Runnable {
                    override fun run() {
                        if (prevHolding) {
                            playerController.seekBySeconds(-10)
                            holdJob?.postDelayed(this, 2000L)
                        }
                    }
                }
                holdJob?.postDelayed(r, 2000L)
            }, 2000L)
        } else if (event.action == KeyEvent.ACTION_UP) {
            holdJob?.removeCallbacksAndMessages(null)
            if (prevHolding) {
                prevHolding = false
                prevTapCount = 0
                return
            }
            if (now - lastPrevDown < 2000L) {
                prevTapCount++
                holdJob?.postDelayed({
                    if (prevTapCount >= 2) {
                        beepIfEnabled(); playerController.previousFolder()
                    } else if (prevTapCount == 1) {
                        beepIfEnabled(); playerController.playPrevious()
                    }
                    prevTapCount = 0
                }, 350L)
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
        const val CMD_JUMP_BACK = "tplayer.jump_back"
        const val CMD_JUMP_FORWARD = "tplayer.jump_forward"
        const val CMD_NEXT_FOLDER = "tplayer.next_folder"
        const val CMD_PREV_FOLDER = "tplayer.prev_folder"
    }
}
