/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

data class ProfileSettings(
    val bio: String = "",
    val genres: List<String> = emptyList(),
    val playlists: List<ShowcaseItem> = emptyList(),
    /** Master privacy switch. Off means nobody else can open this profile. */
    val visible: Boolean = true,
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
}
