/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** Draws the chosen backdrop behind [content]; skipped in light themes so text stays readable. */
@Composable
fun StarFallBackdrop(viewModel: BackgroundViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val state by viewModel.state.collectAsState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Box(Modifier.fillMaxSize()) {
        if (isDark) BackgroundWebView(state.option.id, Modifier.fillMaxSize())
        content()
    }
}
