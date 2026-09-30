package com.grok.tplayer.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grok.tplayer.data.preferences.ThemeMode
import com.grok.tplayer.data.preferences.UserPreferences
import com.grok.tplayer.data.repository.MusicRepository
import com.grok.tplayer.data.scanner.LibraryScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val repository: MusicRepository
) : ViewModel() {

    val settings = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.grok.tplayer.data.preferences.AppSettings())

    val allFolders = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanProgress: StateFlow<LibraryScanner.ScanState> = repository.scanProgress

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setButtonColor(argb: Long) {
        viewModelScope.launch { preferences.setButtonColor(argb) }
    }

    fun setBackgroundColor(argb: Long?) {
        viewModelScope.launch { preferences.setBackgroundColor(argb) }
    }

    fun setFontColor(argb: Long?) {
        viewModelScope.launch { preferences.setFontColor(argb) }
    }

    fun excludeFolder(path: String) {
        viewModelScope.launch { preferences.addExcludedFolder(path) }
    }

    fun includeFolder(path: String) {
        viewModelScope.launch { preferences.removeExcludedFolder(path) }
    }

    fun changeRootAndScan(uri: Uri) {
        viewModelScope.launch {
            val name = uri.lastPathSegment ?: "Custom"
            repository.scanTree(uri, name)
        }
    }

    fun rescanDefault() {
        viewModelScope.launch { repository.scanDefault() }
    }
}
