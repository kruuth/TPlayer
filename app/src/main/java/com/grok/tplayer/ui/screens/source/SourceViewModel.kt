package com.grok.tplayer.ui.screens.source

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grok.tplayer.data.repository.MusicRepository
import com.grok.tplayer.data.scanner.LibraryScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingScan(
    val uri: Uri?,
    val isDefault: Boolean,
    val displayName: String,
    val subfolders: List<String>
)

@HiltViewModel
class SourceViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    val scanProgress: StateFlow<LibraryScanner.ScanState> = repository.scanProgress

    private val _trackCount = MutableStateFlow(0)
    val trackCount: StateFlow<Int> = _trackCount.asStateFlow()

    private val _pendingScan = MutableStateFlow<PendingScan?>(null)
    val pendingScan: StateFlow<PendingScan?> = _pendingScan.asStateFlow()

    private val _selectedSkips = MutableStateFlow<Set<String>>(emptySet())
    val selectedSkips: StateFlow<Set<String>> = _selectedSkips.asStateFlow()

    private val _listingFolders = MutableStateFlow(false)
    val listingFolders: StateFlow<Boolean> = _listingFolders.asStateFlow()

    init {
        viewModelScope.launch {
            _trackCount.value = repository.getTrackCount()
        }
    }

    fun prepareDefaultScan() {
        viewModelScope.launch {
            _pendingScan.value = PendingScan(
                uri = null,
                isDefault = true,
                displayName = "Internal storage/Music",
                subfolders = emptyList()
            )
            _selectedSkips.value = emptySet()
        }
    }

    fun prepareCustomScan(uri: Uri) {
        viewModelScope.launch {
            _listingFolders.value = true
            try {
                val subs = repository.listSubfolders(uri)
                _pendingScan.value = PendingScan(
                    uri = uri,
                    isDefault = false,
                    displayName = uri.lastPathSegment ?: "Custom Folder",
                    subfolders = subs
                )
                _selectedSkips.value = emptySet()
            } finally {
                _listingFolders.value = false
            }
        }
    }

    fun toggleSkip(folder: String) {
        _selectedSkips.value = _selectedSkips.value.toMutableSet().also { set ->
            if (!set.add(folder)) set.remove(folder)
        }
    }

    fun selectAllSkips() {
        val all = _pendingScan.value?.subfolders?.toSet() ?: emptySet()
        _selectedSkips.value = all
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
            if (pending.isDefault) {
                repository.scanDefault(skips)
            } else if (pending.uri != null) {
                repository.scanTree(pending.uri, pending.displayName, false, skips)
            }
            _trackCount.value = repository.getTrackCount()
        }
    }

    fun scanDefaultMusic() {
        prepareDefaultScan()
    }

    fun scanCustomFolder(uri: Uri) {
        prepareCustomScan(uri)
    }
}
