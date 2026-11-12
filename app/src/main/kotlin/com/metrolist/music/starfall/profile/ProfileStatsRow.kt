/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import java.time.LocalDateTime

/** Hours, distinct songs and distinct artists from this device's listening history. */
@Composable
fun ProfileStatsRow(hours: Double, modifier: Modifier = Modifier) {
    val database = LocalDatabase.current
    val from = remember { LocalDateTime.of(2000, 1, 1, 0, 0) }
    val to = remember { LocalDateTime.now().plusDays(1) }
    val songs by remember(database) { database.getUniqueSongCountInRange(from, to) }.collectAsState(initial = 0)
    val artists by remember(database) { database.getUniqueArtistCountInRange(from, to) }.collectAsState(initial = 0)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Stat("%.1f".format(hours), stringResource(R.string.starfall_stat_hours))
        Stat(songs.toString(), stringResource(R.string.songs))
        Stat(artists.toString(), stringResource(R.string.artists))
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
