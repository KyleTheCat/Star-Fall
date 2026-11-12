/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.starfall.ui.StarFallSpacing
import kotlinx.coroutines.delay

/** The tier's monochrome mark; tinted by the surrounding content color unless [tint] is given. */
@Composable
fun RankIcon(rank: Rank, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    Icon(
        painter = painterResource(rank.iconRes),
        contentDescription = stringResource(R.string.starfall_rank_icon_cd, rank.label),
        modifier = modifier.size(size),
        tint = if (tint == androidx.compose.ui.graphics.Color.Unspecified) MaterialTheme.colorScheme.primary else tint,
    )
}

/** Rank label, hours and progress to the next tier, with the full ladder one tap away. */
@Composable
fun RankProgress(totalSeconds: Long, modifier: Modifier = Modifier) {
    val hours = totalSeconds / 3600.0
    val rank = Rank.forHours(hours)
    var showLadder by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(StarFallSpacing.md)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(MaterialTheme.shapes.medium).clickable { showLadder = !showLadder },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.md),
        ) {
            RankIcon(rank, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(rank.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.starfall_hours_listened, "%.1f".format(hours.coerceAtMost(1000.0))),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LinearProgressIndicator(
            progress = { rank.progress(hours) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        val next = rank.next
        Text(
            if (next != null) stringResource(R.string.starfall_rank_next, next.label, next.minHours.toInt()) else stringResource(R.string.starfall_rank_max),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedVisibility(showLadder, enter = fadeIn(), exit = fadeOut()) {
            RankLadder(rank.tier)
        }
    }
}

@Composable
fun RankLadder(currentTier: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = StarFallSpacing.sm), verticalArrangement = Arrangement.spacedBy(StarFallSpacing.sm)) {
        Text(stringResource(R.string.starfall_rank_all), style = MaterialTheme.typography.labelLarge)
        Rank.entries.forEach { rank ->
            val reached = rank.tier <= currentTier
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StarFallSpacing.md)) {
                RankIcon(
                    rank,
                    tint = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    stringResource(R.string.starfall_rank_row, rank.label, rank.minHours.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (rank.tier == currentTier) FontWeight.Medium else FontWeight.Normal,
                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun RankUpCelebration(rank: Rank, onDone: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(rank) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(4_000)
        onDone()
    }
    AlertDialog(
        onDismissRequest = onDone,
        confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.starfall_ok)) } },
        title = { Text(stringResource(R.string.starfall_rank_up_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                RankIcon(rank, size = 64.dp, modifier = Modifier.padding(StarFallSpacing.lg))
                Text(stringResource(R.string.starfall_rank_up_body, rank.label))
            }
        },
    )
}
