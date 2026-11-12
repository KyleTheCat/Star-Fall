/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.profilePrefs by preferencesDataStore("starfall_profile")
private val KEY_BIO = stringPreferencesKey("bio")
private val KEY_GENRES = stringPreferencesKey("genres")
private val KEY_PLAYLISTS = stringPreferencesKey("playlists")
private val KEY_VISIBLE = booleanPreferencesKey("visible")
private val KEY_NAME = stringPreferencesKey("custom_name")
private val KEY_NAME_CHANGED_AT = longPreferencesKey("last_name_change_at")

data class ProfileSettings(
    val bio: String = "",
    val genres: List<String> = emptyList(),
    val playlists: List<ShowcaseItem> = emptyList(),
    /** Master privacy switch. Off means nobody else can open this profile. */
    val visible: Boolean = true,
    /** Overrides the Google display name everywhere when set. */
    val customName: String? = null,
    /** Epoch millis of the last custom-name change; 0 when never changed. */
    val lastNameChangeAt: Long = 0L,
)

const val MAX_BIO = 200
const val MAX_GENRES = 5
const val MAX_PLAYLISTS = 6

@Singleton
class ProfileStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val playlistSerializer = ListSerializer(ShowcaseItem.serializer())

    val settings: Flow<ProfileSettings> = ctx.profilePrefs.data.map { p ->
        ProfileSettings(
            bio = p[KEY_BIO].orEmpty(),
            genres = p[KEY_GENRES].orEmpty().split('\n').filter { it.isNotBlank() },
            playlists = p[KEY_PLAYLISTS]
                ?.let { runCatching { json.decodeFromString(playlistSerializer, it) }.getOrNull() }
                .orEmpty(),
            visible = p[KEY_VISIBLE] ?: true,
            customName = p[KEY_NAME],
            lastNameChangeAt = p[KEY_NAME_CHANGED_AT] ?: 0L,
        )
    }

    suspend fun setBio(bio: String) = ctx.profilePrefs.edit { it[KEY_BIO] = bio.trim().take(MAX_BIO) }

    suspend fun setGenres(genres: List<String>) = ctx.profilePrefs.edit {
        it[KEY_GENRES] = genres.map(String::trim).filter(String::isNotEmpty).distinct().take(MAX_GENRES).joinToString("\n")
    }

    suspend fun setPlaylists(items: List<ShowcaseItem>) = ctx.profilePrefs.edit {
        it[KEY_PLAYLISTS] = json.encodeToString(playlistSerializer, items.take(MAX_PLAYLISTS))
    }

    suspend fun setVisible(visible: Boolean) = ctx.profilePrefs.edit { it[KEY_VISIBLE] = visible }

    /** Enforces the 7-day name lock for every build flavor; online builds also enforce it server-side. */
    suspend fun setDisplayName(raw: String, now: Long = System.currentTimeMillis()): NameChangeResult {
        var result: NameChangeResult = NameChangeResult.Invalid
        ctx.profilePrefs.edit {
            result = NameLock.evaluate(it[KEY_NAME], it[KEY_NAME_CHANGED_AT] ?: 0L, raw, now)
            (result as? NameChangeResult.Changed)?.let { changed ->
                it[KEY_NAME] = changed.name
                it[KEY_NAME_CHANGED_AT] = now
            }
        }
        return result
    }

    /** Adopts a name and lock time stored online (e.g. after reinstalling), never loosening a local lock. */
    suspend fun adoptRemoteName(name: String?, changedAt: Long) = ctx.profilePrefs.edit {
        if (name != null && changedAt > (it[KEY_NAME_CHANGED_AT] ?: 0L)) {
            it[KEY_NAME] = name
            it[KEY_NAME_CHANGED_AT] = changedAt
        }
    }
}
