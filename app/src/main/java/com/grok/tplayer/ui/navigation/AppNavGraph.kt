package com.grok.tplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.grok.tplayer.data.repository.MusicRepository
import com.grok.tplayer.player.PlayerController
import com.grok.tplayer.ui.screens.browse.BrowseScreen
import com.grok.tplayer.ui.screens.browse.SearchScreen
import com.grok.tplayer.ui.screens.browse.TrackListScreen
import com.grok.tplayer.ui.screens.player.NowPlayingScreen
import com.grok.tplayer.ui.screens.settings.SettingsScreen
import com.grok.tplayer.ui.screens.source.SourceScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

object Routes {
    const val SOURCE = "source"
    const val BROWSE = "browse"
    const val TRACK_LIST = "track_list/{mode}/{key}"
    const val NOW_PLAYING = "now_playing"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
}

@HiltViewModel
class StartDestinationViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {
    var startRoute by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            val count = repository.getTrackCount()
            if (count > 0) {
                playerController.restoreLastPlayback()
                startRoute = Routes.NOW_PLAYING
            } else {
                startRoute = Routes.SOURCE
            }
        }
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    playerController: PlayerController,
    startVm: StartDestinationViewModel = hiltViewModel()
) {
    val start = startVm.startRoute ?: return

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.SOURCE) {
            SourceScreen(
                onLibraryReady = {
                    playerController.restoreLastPlayback()
                    navController.navigate(Routes.NOW_PLAYING) {
                        popUpTo(Routes.SOURCE) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.BROWSE) {
            BrowseScreen(
                onOpenList = { mode, key ->
                    val encoded = android.net.Uri.encode(key)
                    navController.navigate("track_list/$mode/$encoded")
                },
                onOpenPlayer = { navController.navigate(Routes.NOW_PLAYING) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) }
            )
        }
        composable(
            route = Routes.TRACK_LIST,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("key") { type = NavType.StringType }
            )
        ) { backStack ->
            val mode = backStack.arguments?.getString("mode") ?: "all"
            val key = android.net.Uri.decode(backStack.arguments?.getString("key") ?: "")
            TrackListScreen(
                mode = mode,
                key = key,
                playerController = playerController,
                onTrackSelected = { navController.navigate(Routes.NOW_PLAYING) },
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPlayer = { navController.navigate(Routes.NOW_PLAYING) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) }
            )
        }
        composable(Routes.NOW_PLAYING) {
            NowPlayingScreen(
                playerController = playerController,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.BROWSE)
                    }
                },
                onOpenLibrary = {
                    navController.navigate(Routes.BROWSE) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                playerController = playerController,
                onTrackSelected = {
                    navController.navigate(Routes.NOW_PLAYING) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
