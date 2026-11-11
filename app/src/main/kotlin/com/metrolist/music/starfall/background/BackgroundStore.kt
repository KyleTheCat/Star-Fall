/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
private val KEY_VERSION = longPreferencesKey("custom_bg_version")

data class BackgroundState(val option: BackgroundOption, val customVersion: Long)

@Singleton
class BackgroundStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    val customFile: File get() = File(ctx.filesDir, "starfall_bg_custom.jpg")

    val state: Flow<BackgroundState> = ctx.starfallPrefs.data.map {
        BackgroundState(BackgroundOption.fromId(it[KEY_BG]), it[KEY_VERSION] ?: 0L)
    }

    suspend fun apply(option: BackgroundOption) {
        ctx.starfallPrefs.edit { it[KEY_BG] = option.id }
    }

    suspend fun importCustom(uri: Uri) = withContext(Dispatchers.IO) {
        if (!BackgroundImporter.import(ctx, uri, customFile)) return@withContext
        ctx.starfallPrefs.edit { it[KEY_BG] = BackgroundOption.CUSTOM.id; it[KEY_VERSION] = System.currentTimeMillis() }
    }
}
