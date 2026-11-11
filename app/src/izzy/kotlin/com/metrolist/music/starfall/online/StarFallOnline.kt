/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.online

import com.metrolist.music.starfall.profile.ProfileIdentity
import com.metrolist.music.starfall.profile.ProfileLookup
import com.metrolist.music.starfall.profile.ProfileSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Account, leaderboard and public profiles need Google Play services; this build keeps rank and profile on-device. */
@Singleton
class StarFallOnline @Inject constructor() {
    val available: Boolean = false
    val myUid: String? = null
    val identity: Flow<ProfileIdentity?> = flowOf(null)

    suspend fun addListening(seconds: Long) = Unit
    suspend fun publishProfile(settings: ProfileSettings, totalSeconds: Long) = Unit
    suspend fun unpublishProfile() = Unit
    suspend fun loadProfile(uid: String): ProfileLookup = ProfileLookup.Unavailable
}
