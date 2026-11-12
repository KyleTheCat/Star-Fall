/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.online

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R

/** Nothing to sign in to here; the profile screen already says the profile stays on this device. */
@Composable
fun StarFallAccountSection(navController: NavController) = Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarFallLeaderboardScreen(navController: NavController) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.starfall_leaderboard)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back))
                }
            },
        )
        Text(stringResource(R.string.starfall_online_unavailable), Modifier.padding(16.dp))
    }
}
