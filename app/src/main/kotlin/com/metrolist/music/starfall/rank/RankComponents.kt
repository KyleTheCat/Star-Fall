/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun RankRing(totalSeconds: Long, modifier: Modifier = Modifier) {
    val hours = totalSeconds / 3600.0
    val rank = Rank.forHours(hours)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { rank.progress(hours) }, modifier = Modifier.size(160.dp), strokeWidth = 8.dp)
            Text(rank.icon, style = MaterialTheme.typography.displayMedium)
        }
        Text("${rank.label} · ${"%.1f".format(hours.coerceAtMost(1000.0))} h", style = MaterialTheme.typography.titleMedium)
        rank.next?.let { Text("Next: ${it.label} at ${it.minHours.toInt()} h", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun RankLadder(currentTier: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rank.entries.forEach { rank ->
            val reached = rank.tier <= currentTier
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(rank.icon, modifier = Modifier.scale(if (rank.tier == currentTier) 1.3f else 1f))
                Text(
                    "${rank.label} · ${rank.minHours.toInt()} h",
                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun RankUpCelebration(rank: Rank, onDone: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(rank) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scale.animateTo(1.4f, spring(Spring.DampingRatioMediumBouncy))
        scale.animateTo(1f)
        delay(2_000)
        onDone()
    }
    AlertDialog(
        onDismissRequest = onDone,
        confirmButton = { TextButton(onClick = onDone) { Text("Nice") } },
        title = { Text("Rank up!") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(rank.icon, style = MaterialTheme.typography.displayLarge, modifier = Modifier.scale(scale.value).padding(16.dp))
                Text("You are now a ${rank.label}")
            }
        },
    )
}
