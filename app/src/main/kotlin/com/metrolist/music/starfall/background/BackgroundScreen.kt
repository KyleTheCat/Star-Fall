/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundScreen(navController: NavController, vm: BackgroundViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val options = BackgroundOption.entries
    val pager = rememberPagerState { options.size }
    val haptics = LocalHapticFeedback.current

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TopAppBar(
            title = { Text(stringResource(R.string.starfall_backgrounds)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back))
                }
            },
        )
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.weight(1f),
        ) { page ->
            BackgroundCard(options[page], isActive = options[page] == state.option)
        }
        val current = options[pager.currentPage]
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.apply(current)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(stringResource(R.string.starfall_apply))
        }
    }
}

@Composable
private fun BackgroundCard(option: BackgroundOption, isActive: Boolean) {
    Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            BackgroundWebView(option.id, Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(stringResource(option.title), style = MaterialTheme.typography.titleLarge)
                val blurb = stringResource(option.blurb)
                Text(if (isActive) "$blurb · ${stringResource(R.string.starfall_active)}" else blurb, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
