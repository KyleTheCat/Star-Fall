/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RankViewModel @Inject constructor(store: RankStore) : ViewModel() {
    val totalSeconds = store.totalSeconds.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val celebrate = MutableStateFlow<Rank?>(null)

    init {
        viewModelScope.launch {
            var lastTier: Int? = null
            store.totalSeconds.collect { seconds ->
                val rank = Rank.forSeconds(seconds)
                val previous = lastTier
                if (previous != null && rank.tier > previous) celebrate.value = rank
                lastTier = rank.tier
            }
        }
    }

    fun dismissCelebration() { celebrate.value = null }
}
