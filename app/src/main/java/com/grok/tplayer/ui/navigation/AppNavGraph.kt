package com.grok.tplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.grok.tplayer.player.PlayerController
import com.grok.tplayer.ui.screens.browse.BrowseScreen
import com.grok.tplayer.ui.screens.browse.TrackListScreen
import com.grok.tplayer.ui.screens.player.NowPlayingScreen
import com.grok.tplayer.ui.screens.settings.SettingsScreen
import com.grok.tplayer.ui.screens.source.SourceScreen

object Routes {
    const val SOURCE = "source"
    const val BROWSE = "browse"
    const val TRACK_LIST = "track_list/{mode}/{key}"
    const val NOW_PLAYING = "now_playing"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    playerController: PlayerController
) {
    NavHost(navController = navController, startDestination = Routes.SOURCE) {
        composable(Routes.SOURCE) {
            SourceScreen(
                onLibraryReady = {
                    navController.navigate(Routes.BROWSE) {
                        popUpTo(Routes.SOURCE) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.BROWSE) {
            BrowseScreen(
                onOpenList = { mode, key ->
                    // URL-encode key so paths with / work
                    val encoded = android.net.Uri.encode(key)
                    navController.navigate("track_list/$mode/$encoded")
                },
                onOpenPlayer = { navController.navigate(Routes.NOW_PLAYING) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
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
                onOpenPlayer = { navController.navigate(Routes.NOW_PLAYING) }
            )
        }
        composable(Routes.NOW_PLAYING) {
            NowPlayingScreen(
                playerController = playerController,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
