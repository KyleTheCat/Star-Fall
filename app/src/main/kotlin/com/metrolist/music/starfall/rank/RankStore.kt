/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rankPrefs by preferencesDataStore("starfall_rank")
private val KEY_TOTAL = longPreferencesKey("total_seconds")

/** Cumulative active-listening seconds, kept on-device for every build flavor. */
@Singleton
class RankStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    val totalSeconds: Flow<Long> = ctx.rankPrefs.data.map { it[KEY_TOTAL] ?: 0L }

    suspend fun add(seconds: Long) {
        ctx.rankPrefs.edit { it[KEY_TOTAL] = (it[KEY_TOTAL] ?: 0L) + seconds }
    }

    suspend fun raiseTo(total: Long) {
        ctx.rankPrefs.edit { if (total > (it[KEY_TOTAL] ?: 0L)) it[KEY_TOTAL] = total }
    }
}
