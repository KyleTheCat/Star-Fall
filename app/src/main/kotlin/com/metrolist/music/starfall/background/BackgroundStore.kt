/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.starfallPrefs by preferencesDataStore("starfall_prefs")
private val KEY_BG = stringPreferencesKey("background")

data class BackgroundState(val option: BackgroundOption)

@Singleton
class BackgroundStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    val state: Flow<BackgroundState> = ctx.starfallPrefs.data.map {
        // Unknown ids, including the removed "custom" image option, fall back to the default.
        BackgroundState(BackgroundOption.fromId(it[KEY_BG]))
    }

    suspend fun apply(option: BackgroundOption) {
        ctx.starfallPrefs.edit { it[KEY_BG] = option.id }
    }

    /** Drops the imported image and its preferences left behind by earlier versions. */
    suspend fun removeLegacyCustomImage() = withContext(Dispatchers.IO) {
        File(ctx.filesDir, LEGACY_IMAGE).delete()
        ctx.starfallPrefs.edit {
            if (it[KEY_BG] == "custom") it[KEY_BG] = BackgroundOption.NEBULA.id
            it.remove(stringPreferencesKey("custom_bg_version"))
        }
    }

    private companion object {
        const val LEGACY_IMAGE = "starfall_bg_custom.jpg"
    }
}
