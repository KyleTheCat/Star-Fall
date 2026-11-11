/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BackgroundViewModel @Inject constructor(private val store: BackgroundStore) : ViewModel() {
    val state = store.state.stateIn(
        viewModelScope, SharingStarted.Eagerly, BackgroundState(BackgroundOption.NEBULA, 0L),
    )
    val customFile get() = store.customFile

    fun apply(option: BackgroundOption) { viewModelScope.launch { store.apply(option) } }
    fun importCustom(uri: Uri) { viewModelScope.launch { store.importCustom(uri) } }
}
