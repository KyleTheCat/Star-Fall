/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

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
        viewModelScope, SharingStarted.Eagerly, BackgroundState(BackgroundOption.NEBULA),
    )
    init { viewModelScope.launch { store.removeLegacyCustomImage() } }


    fun apply(option: BackgroundOption) { viewModelScope.launch { store.apply(option) } }
}
