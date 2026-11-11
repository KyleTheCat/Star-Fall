/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.starfall.online.StarFallAccountSection
import com.metrolist.music.starfall.ui.StarFallCard

/** Your rank, the ladder, and account / leaderboard controls. Ranks live here, never on the public profile. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFallRankScreen(navController: NavController, vm: RankViewModel = hiltViewModel()) {
    val total by vm.totalSeconds.collectAsState()
    val celebrate by vm.celebrate.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.starfall_rank_title)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back))
                }
            },
        )
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StarFallCard(Modifier.fillMaxWidth()) {
                RankRing(total, Modifier.fillMaxWidth().padding(24.dp))
            }
            StarFallCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    StarFallAccountSection(navController)
                }
            }
            StarFallCard(Modifier.fillMaxWidth()) {
                RankLadder(Rank.forSeconds(total).tier, Modifier.padding(20.dp))
            }
        }
    }
    celebrate?.let { RankUpCelebration(it, onDone = vm::dismissCelebration) }
}
