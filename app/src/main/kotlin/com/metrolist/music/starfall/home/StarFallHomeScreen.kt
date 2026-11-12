/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.screens.HomeScreen
import com.metrolist.music.viewmodels.HomeViewModel
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.map

/**
 * The simplified Home from docs/mockups/1_home_avatar.png: mood chips, Quick picks tiles and a
 * Recently played list. When a chip is selected the full feed takes over so chip content still works.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFallHomeScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()
    if (selectedChip != null) {
        HomeScreen(snackbarHostState = snackbarHostState, viewModel = viewModel)
        return
    }

    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val listenTogether = LocalListenTogetherManager.current
    val isGuest = listenTogether?.let { it.isInRoom && !it.isHost } ?: false

    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val recent by remember(database) {
        database.recentEvents(40).map { events -> events.map { it.song }.distinctBy { it.id }.take(5) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) { viewModel.loadHomeData() }

    fun play(song: Song) {
        if (isGuest) return
        playerConnection.playQueue(
            YouTubeQueue.radio(song.toMediaMetadata()),
        )
    }

    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val layoutDir = androidx.compose.ui.platform.LocalLayoutDirection.current
    val horizontal = 20.dp

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = insets.calculateStartPadding(layoutDir),
                end = insets.calculateEndPadding(layoutDir),
                top = insets.calculateTopPadding(),
                bottom = insets.calculateBottomPadding() + 16.dp,
            ),
        ) {
            val chips = homePage?.chips.orEmpty()
            if (chips.isNotEmpty()) {
                item(key = "chips") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = horizontal),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        items(chips, key = { it.title }) { chip ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { viewModel.toggleChip(chip) },
                            ) {
                                Text(
                                    text = chip.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                )
                            }
                        }
                    }
                }
            }

            val picks = quickPicks.orEmpty().distinctBy { it.id }
            if (picks.isNotEmpty()) {
                item(key = "quick_picks_title") { SectionTitle("Quick picks", horizontal) }
                item(key = "quick_picks") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = horizontal),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(picks, key = { "qp_${it.id}" }) { song ->
                            Column(
                                modifier = Modifier
                                    .width(148.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { play(song) },
                            ) {
                                Cover(song, Modifier.fillMaxWidth().aspectRatio(1f), 20.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = song.artists.joinToString { it.name },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            if (recent.isNotEmpty()) {
                item(key = "recent_title") { SectionTitle("Recently played", horizontal, top = 28.dp) }
                items(recent, key = { "recent_${it.id}" }) { song ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { play(song) }
                            .padding(horizontal = horizontal, vertical = 8.dp),
                    ) {
                        Cover(song, Modifier.size(64.dp), 14.dp)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToString { it.name },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, horizontal: androidx.compose.ui.unit.Dp, top: androidx.compose.ui.unit.Dp = 16.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(start = horizontal, end = horizontal, top = top, bottom = 12.dp),
    )
}

@Composable
private fun Cover(song: Song, modifier: Modifier, radius: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .then(Modifier),
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.matchParentSize()) {}
        AsyncImage(
            model = ImageRequest.Builder(context).data(song.song.thumbnailUrl).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}
