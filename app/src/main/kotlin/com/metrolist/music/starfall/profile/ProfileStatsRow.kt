/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.starfall.ui.StarFallSpacing
import com.metrolist.music.starfall.ui.StarFallStatTile
import java.time.LocalDateTime

/** Hours, distinct songs and distinct artists from this device's listening history. */
@Composable
fun ProfileStatsRow(hours: Double, modifier: Modifier = Modifier) {
    val database = LocalDatabase.current
    val from = remember { LocalDateTime.of(2000, 1, 1, 0, 0) }
    val to = remember { LocalDateTime.now().plusDays(1) }
    val songs by remember(database) { database.getUniqueSongCountInRange(from, to) }.collectAsState(initial = 0)
    val artists by remember(database) { database.getUniqueArtistCountInRange(from, to) }.collectAsState(initial = 0)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.sm)) {
        StarFallStatTile("%.1f".format(hours), stringResource(R.string.starfall_stat_hours), Modifier.weight(1f))
        StarFallStatTile(songs.toString(), stringResource(R.string.songs), Modifier.weight(1f))
        StarFallStatTile(artists.toString(), stringResource(R.string.artists), Modifier.weight(1f))
    }
}
