package com.grok.tplayer.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grok.tplayer.data.preferences.ThemeMode
import com.grok.tplayer.data.preferences.UserPreferences
import com.grok.tplayer.data.repository.MusicRepository
import com.grok.tplayer.data.scanner.LibraryScanner
import com.grok.tplayer.ui.screens.source.PendingScan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _pendingScan = MutableStateFlow<PendingScan?>(null)
    val pendingScan: StateFlow<PendingScan?> = _pendingScan.asStateFlow()

    private val _selectedSkips = MutableStateFlow<Set<String>>(emptySet())
    val selectedSkips: StateFlow<Set<String>> = _selectedSkips.asStateFlow()

    private val _listingFolders = MutableStateFlow(false)
    val listingFolders: StateFlow<Boolean> = _listingFolders.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

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

    fun setSteeringBeep(enabled: Boolean) {
        viewModelScope.launch { preferences.setSteeringBeepEnabled(enabled) }
    }

    fun includeFolder(path: String) {
        viewModelScope.launch { preferences.removeExcludedFolder(path) }
    }

    fun prepareCustomRoot(uri: Uri) {
        viewModelScope.launch {
            _listingFolders.value = true
            _statusMessage.value = "Listing folders…"
            try {
                val subs = repository.listSubfolders(uri)
                _pendingScan.value = PendingScan(
                    uri = uri,
                    isDefault = false,
                    displayName = uri.lastPathSegment ?: "Custom Folder",
                    subfolders = subs
                )
                _selectedSkips.value = emptySet()
                _statusMessage.value = null
            } catch (e: Exception) {
                _statusMessage.value = "Could not open folder: ${e.message}"
            } finally {
                _listingFolders.value = false
            }
        }
    }

    fun prepareDefaultRescan() {
        _pendingScan.value = PendingScan(
            uri = null,
            isDefault = true,
            displayName = "Internal storage/Music",
            subfolders = emptyList()
        )
        _selectedSkips.value = emptySet()
    }

    fun toggleSkip(folder: String) {
        _selectedSkips.value = _selectedSkips.value.toMutableSet().also { set ->
            if (!set.add(folder)) set.remove(folder)
        }
    }

    fun selectAllSkips() {
        _selectedSkips.value = _pendingScan.value?.subfolders?.toSet() ?: emptySet()
    }

    fun clearSkips() {
        _selectedSkips.value = emptySet()
    }

    fun cancelPending() {
        _pendingScan.value = null
        _selectedSkips.value = emptySet()
    }

    fun startScanWithSkips() {
        val pending = _pendingScan.value ?: return
        val skips = _selectedSkips.value
        _pendingScan.value = null
        viewModelScope.launch {
            try {
                _statusMessage.value = "Scanning…"
                if (pending.isDefault) {
                    repository.scanDefault(skips)
                } else if (pending.uri != null) {
                    repository.scanTree(
                        pending.uri,
                        pending.displayName,
                        isDefault = false,
                        skipFolders = skips
                    )
                }
            } catch (e: Exception) {
                _statusMessage.value = "Scan failed: ${e.message}"
            }
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
