package com.grok.tplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.grok.tplayer.data.preferences.AppSettings
import com.grok.tplayer.data.preferences.UserPreferences
import com.grok.tplayer.data.repository.MusicRepository
import com.grok.tplayer.player.PlayerController
import com.grok.tplayer.ui.components.ScanProgressBar
import com.grok.tplayer.ui.navigation.AppNavGraph
import com.grok.tplayer.ui.theme.TPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerController: PlayerController

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var musicRepository: MusicRepository

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — playback still works */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        playerController.connect()

        setContent {
            val settings by userPreferences.settings.collectAsState(initial = AppSettings())
            TPlayerTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        ScanProgressBar(
                            scanProgress = musicRepository.scanProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // fillMaxSize takes remaining space without using weight()
                        Box(Modifier.fillMaxSize()) {
                            val navController = rememberNavController()
                            AppNavGraph(
                                navController = navController,
                                playerController = playerController
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            playerController.disconnect()
        }
    }
}
