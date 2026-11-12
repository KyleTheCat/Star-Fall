/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.starfall.rank.RankIcon
import com.metrolist.music.starfall.ui.SwipeActionRow
import com.metrolist.music.starfall.ui.pressScale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StarFallLeaderboardViewModel @Inject constructor(private val online: StarFallOnline) : ViewModel() {
    val board = MutableStateFlow<Board?>(null)
    val failed = MutableStateFlow(false)
    val myUid: String? get() = online.myUid

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { online.loadBoard() }
                .onSuccess { board.value = it; failed.value = false }
                .onFailure { failed.value = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFallLeaderboardScreen(navController: NavController, vm: StarFallLeaderboardViewModel = hiltViewModel()) {
    val board by vm.board.collectAsState()
    val failed by vm.failed.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.starfall_leaderboard)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back))
                }
            },
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.starfall_leaderboard_reset), style = MaterialTheme.typography.bodySmall)
            if (failed) Text(stringResource(R.string.starfall_sign_in_failed), color = MaterialTheme.colorScheme.error)
            val data = board ?: return@Column
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(data.top, key = { _, e -> e.uid }) { index, entry ->
                    SwipeActionRow(
                        endToStartIcon = painterResource(R.drawable.account),
                        onEndToStart = { navController.navigate("starfall/profile/${entry.uid}") },
                    ) {
                        EntryRow(index + 1, entry, entry.uid == vm.myUid) { navController.navigate("starfall/profile/${entry.uid}") }
                    }
                }
            }
            val me = data.me
            if (me != null && data.top.none { it.uid == me.uid }) EntryRow(data.myPosition ?: 0, me, highlight = true) { navController.navigate("starfall/profile/${me.uid}") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(position: Int, entry: BoardEntry, highlight: Boolean, onClick: () -> Unit) {
    val colors = if (highlight) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
    Card(colors = colors, onClick = onClick, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().pressScale()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("#$position")
            AsyncImage(model = entry.avatar, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
            Text(entry.name, Modifier.weight(1f), maxLines = 1)
            Text("%.1f h".format(entry.hours))
            RankIcon(entry.rank)
        }
    }
}
