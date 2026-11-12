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
import kotlinx.coroutines.flow.combine
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

    /** Custom name when set, otherwise the Google name; null when signed out and unnamed. */
    val displayName: StateFlow<String?> = combine(settings, identity) { s, id -> s.customName ?: id?.name }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // After a reinstall the name and its lock live online; adopt them so the lock cannot be skipped.
        viewModelScope.launch {
            identity.collect { id ->
                if (id == null) return@collect
                runCatching { online.fetchNameState() }.getOrNull()?.let { store.adoptRemoteName(it.name, it.changedAtMs) }
            }
        }
    }

    /** Changes the display name at most once per 7 days; the online build also asks Firestore, which enforces it too. */
    suspend fun changeName(raw: String): NameChangeResult {
        val now = System.currentTimeMillis()
        val current = settings.value
        val check = NameLock.evaluate(displayName.value, current.lastNameChangeAt, raw, now)
        if (check !is NameChangeResult.Changed) return check
        if (identity.value != null) {
            val saved = online.saveCustomName(check.name)
            saved.exceptionOrNull()?.let { error ->
                if (!online.isDenied(error)) throw error
                runCatching { online.fetchNameState() }.getOrNull()?.let { store.adoptRemoteName(it.name, it.changedAtMs) }
                return NameChangeResult.Locked(NameLock.remainingMs(store.settings.first().lastNameChangeAt, now))
            }
        }
        val result = store.setDisplayName(check.name, now)
        if (result is NameChangeResult.Changed) sync()
        return result
    }

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
