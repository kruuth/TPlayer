package com.grok.tplayer.data.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tplayer_settings")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val buttonColor: Long = 0xFF6200EE,      // ARGB
    val backgroundColor: Long? = null,       // null = use theme default
    val fontColor: Long? = null,
    val excludedFolders: Set<String> = emptySet(),
    val steeringBeepEnabled: Boolean = true,
    val lastTrackId: Long = -1L,
    val lastPositionMs: Long = 0L
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val BUTTON = longPreferencesKey("button_color")
        val BACKGROUND = longPreferencesKey("background_color")
        val FONT = longPreferencesKey("font_color")
        val EXCLUDED = stringSetPreferencesKey("excluded_folders")
        val HAS_BG = intPreferencesKey("has_bg")
        val HAS_FONT = intPreferencesKey("has_font")
        val STEERING_BEEP = booleanPreferencesKey("steering_beep")
        val LAST_TRACK_ID = longPreferencesKey("last_track_id")
        val LAST_POSITION = longPreferencesKey("last_position_ms")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = when (prefs[Keys.THEME]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            buttonColor = prefs[Keys.BUTTON] ?: 0xFF6200EE,
            backgroundColor = if (prefs[Keys.HAS_BG] == 1) prefs[Keys.BACKGROUND] else null,
            fontColor = if (prefs[Keys.HAS_FONT] == 1) prefs[Keys.FONT] else null,
            excludedFolders = prefs[Keys.EXCLUDED] ?: emptySet(),
            steeringBeepEnabled = prefs[Keys.STEERING_BEEP] ?: true,
            lastTrackId = prefs[Keys.LAST_TRACK_ID] ?: -1L,
            lastPositionMs = prefs[Keys.LAST_POSITION] ?: 0L
        )
    }

    suspend fun setLastPlayback(trackId: Long, positionMs: Long) {
        context.dataStore.edit {
            it[Keys.LAST_TRACK_ID] = trackId
            it[Keys.LAST_POSITION] = positionMs
        }
    }

    suspend fun setSteeringBeepEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STEERING_BEEP] = enabled }
    }


    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setButtonColor(argb: Long) {
        context.dataStore.edit { it[Keys.BUTTON] = argb }
    }

    suspend fun setBackgroundColor(argb: Long?) {
        context.dataStore.edit {
            if (argb == null) {
                it[Keys.HAS_BG] = 0
                it.remove(Keys.BACKGROUND)
            } else {
                it[Keys.HAS_BG] = 1
                it[Keys.BACKGROUND] = argb
            }
        }
    }

    suspend fun setFontColor(argb: Long?) {
        context.dataStore.edit {
            if (argb == null) {
                it[Keys.HAS_FONT] = 0
                it.remove(Keys.FONT)
            } else {
                it[Keys.HAS_FONT] = 1
                it[Keys.FONT] = argb
            }
        }
    }

    suspend fun setExcludedFolders(folders: Set<String>) {
        context.dataStore.edit { it[Keys.EXCLUDED] = folders }
    }

    suspend fun addExcludedFolder(path: String) {
        context.dataStore.edit {
            val current = it[Keys.EXCLUDED] ?: emptySet()
            it[Keys.EXCLUDED] = current + path
        }
    }

    suspend fun removeExcludedFolder(path: String) {
        context.dataStore.edit {
            val current = it[Keys.EXCLUDED] ?: emptySet()
            it[Keys.EXCLUDED] = current - path
        }
    }
}

fun Long.toComposeColor(): Color = Color(this)
