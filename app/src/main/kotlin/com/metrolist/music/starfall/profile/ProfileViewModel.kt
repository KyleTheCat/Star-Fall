/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.starfall.online.StarFallOnline
import com.metrolist.music.starfall.rank.RankStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Edits the signed-in user's own profile and keeps the published copy in step with the privacy switch. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val store: ProfileStore,
    private val rank: RankStore,
    private val online: StarFallOnline,
) : ViewModel() {
    val settings: StateFlow<ProfileSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileSettings())
    val totalSeconds: StateFlow<Long> = rank.totalSeconds.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val identity: StateFlow<ProfileIdentity?> =
        online.identity.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val onlineAvailable: Boolean = online.available

    fun setBio(bio: String) = edit { store.setBio(bio) }
    fun setGenres(genres: List<String>) = edit { store.setGenres(genres) }
    fun setPlaylists(items: List<ShowcaseItem>) = edit { store.setPlaylists(items) }

    /** Off removes the published profile entirely; on republishes the current one. */
    fun setVisible(visible: Boolean) {
        viewModelScope.launch {
            store.setVisible(visible)
            sync()
        }
    }

    private fun edit(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            sync()
        }
    }

    private suspend fun sync() {
        val s = store.settings.first()
        runCatching {
            if (s.visible) {
                online.publishProfile(s, rank.totalSeconds.first())
            } else {
                online.unpublishProfile()
            }
        }
    }
}

/** Opens another listener's profile by uid (route argument). */
@HiltViewModel
class VisitedProfileViewModel @Inject constructor(
    private val online: StarFallOnline,
    handle: SavedStateHandle,
) : ViewModel() {
    private val uid: String = handle["uid"] ?: ""
    val lookup = MutableStateFlow<ProfileLookup>(ProfileLookup.Loading)

    init { load() }

    fun load() {
        viewModelScope.launch {
            lookup.value = ProfileLookup.Loading
            lookup.value = runCatching { online.loadProfile(uid) }
                .getOrElse { ProfileLookup.Unavailable }
        }
    }
}
