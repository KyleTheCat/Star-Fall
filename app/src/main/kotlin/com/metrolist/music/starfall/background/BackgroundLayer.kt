/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.io.File

@Composable
fun BackgroundLayer(state: BackgroundState, customFile: File, modifier: Modifier = Modifier) {
    if (state.option.isWeb) {
        BackgroundWebView(state.option.id, modifier)
    } else {
        CustomBackground(customFile, state.customVersion, modifier)
    }
}

@Composable
private fun CustomBackground(file: File, version: Long, modifier: Modifier) {
    val bitmap = remember(version) { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
    if (bitmap == null) {
        BackgroundWebView(BackgroundOption.VOID.id, modifier)
    } else {
        Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    }
}

/** Draws the chosen backdrop behind [content]; skipped in light themes so text stays readable. */
@Composable
fun StarFallBackdrop(viewModel: BackgroundViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val state by viewModel.state.collectAsState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Box(Modifier.fillMaxSize()) {
        if (isDark) BackgroundLayer(state, viewModel.customFile, Modifier.fillMaxSize())
        content()
    }
}
