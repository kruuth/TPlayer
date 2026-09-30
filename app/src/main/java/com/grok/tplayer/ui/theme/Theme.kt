package com.grok.tplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.grok.tplayer.data.preferences.AppSettings
import com.grok.tplayer.data.preferences.ThemeMode
import com.grok.tplayer.data.preferences.toComposeColor

data class TPlayerColors(
    val button: Color = Color(0xFF6200EE),
    val customBackground: Color? = null,
    val customFont: Color? = null
)

val LocalTPlayerColors = staticCompositionLocalOf { TPlayerColors() }

@Composable
fun TPlayerTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val buttonColor = settings.buttonColor.toComposeColor()

    val baseLight = lightColorScheme(
        primary = buttonColor,
        secondary = Color(0xFF03DAC6),
        background = settings.backgroundColor?.toComposeColor() ?: Color(0xFFFFFBFE),
        surface = settings.backgroundColor?.toComposeColor() ?: Color(0xFFFFFBFE),
        onBackground = settings.fontColor?.toComposeColor() ?: Color(0xFF1C1B1F),
        onSurface = settings.fontColor?.toComposeColor() ?: Color(0xFF1C1B1F)
    )

    val baseDark = darkColorScheme(
        primary = buttonColor,
        secondary = Color(0xFF03DAC6),
        background = settings.backgroundColor?.toComposeColor() ?: Color(0xFF121212),
        surface = settings.backgroundColor?.toComposeColor() ?: Color(0xFF1E1E1E),
        onBackground = settings.fontColor?.toComposeColor() ?: Color(0xFFE6E1E5),
        onSurface = settings.fontColor?.toComposeColor() ?: Color(0xFFE6E1E5)
    )

    CompositionLocalProvider(
        LocalTPlayerColors provides TPlayerColors(
            button = buttonColor,
            customBackground = settings.backgroundColor?.toComposeColor(),
            customFont = settings.fontColor?.toComposeColor()
        )
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) baseDark else baseLight,
            content = content
        )
    }
}
