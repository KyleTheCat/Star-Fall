/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import kotlinx.serialization.Serializable

/** A playlist (or similar item) a listener chooses to show off. */
@Serializable
data class ShowcaseItem(val id: String, val title: String, val thumbnail: String? = null)

/** Signed-in account identity; null when offline-only or not signed in. */
data class ProfileIdentity(val uid: String, val name: String, val avatar: String?)

/**
 * What other people can see. Has no rank field: viewers derive the rank from
 * [hours], so nothing extra is published.
 */
data class PublicProfile(
    val uid: String,
    val name: String,
    val avatar: String?,
    val totalSeconds: Long,
    val bio: String,
    val genres: List<String>,
    val playlists: List<ShowcaseItem>,
) {
    val hours: Double get() = totalSeconds / 3600.0
}

/** Result of opening somebody's profile. */
sealed interface ProfileLookup {
    data object Loading : ProfileLookup
    data object Private : ProfileLookup
    data object Unavailable : ProfileLookup
    data class Found(val profile: PublicProfile) : ProfileLookup
}
