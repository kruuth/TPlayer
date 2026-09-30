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

@HiltViewModel
class SourceViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    val scanProgress: StateFlow<LibraryScanner.ScanState> = repository.scanProgress

    private val _trackCount = MutableStateFlow(0)
    val trackCount: StateFlow<Int> = _trackCount.asStateFlow()

    init {
        viewModelScope.launch {
            _trackCount.value = repository.getTrackCount()
        }
    }

    fun scanDefaultMusic() {
        viewModelScope.launch {
            repository.scanDefault()
            _trackCount.value = repository.getTrackCount()
        }
    }

    fun scanCustomFolder(uri: Uri) {
        viewModelScope.launch {
            val name = uri.lastPathSegment ?: "Custom Folder"
            repository.scanTree(uri, name)
            _trackCount.value = repository.getTrackCount()
        }
    }
}
