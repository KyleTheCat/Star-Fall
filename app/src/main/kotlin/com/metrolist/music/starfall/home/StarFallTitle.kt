/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/** Four-point star used in the Home header. */
@Composable
fun StarFallStar(color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        val i = r * 0.2f
        val path = Path().apply {
            moveTo(c.x, c.y - r)
            quadraticTo(c.x + i, c.y - i, c.x + r, c.y)
            quadraticTo(c.x + i, c.y + i, c.x, c.y + r)
            quadraticTo(c.x - i, c.y + i, c.x - r, c.y)
            quadraticTo(c.x - i, c.y - i, c.x, c.y - r)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun StarFallTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StarFallStar()
        Spacer(Modifier.width(16.dp))
        Text("StarFall", style = MaterialTheme.typography.headlineMedium)
    }
}
