/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Spacing scale shared by every StarFall screen. */
object StarFallSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** One corner-radius scale for cards, sheets, dialogs and chips. */
val StarFallShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()

/** Tighter, medium-weight headings over the Material defaults; body text stays untouched. */
val StarFallTypography = base.copy(
    headlineMedium = base.headlineMedium.withWeight(FontWeight.Medium),
    headlineSmall = base.headlineSmall.withWeight(FontWeight.Medium),
    titleLarge = base.titleLarge.withWeight(FontWeight.Medium),
    titleMedium = base.titleMedium.withWeight(FontWeight.Medium),
    labelLarge = base.labelLarge.withWeight(FontWeight.Medium),
)

private fun TextStyle.withWeight(weight: FontWeight) = copy(fontWeight = weight, letterSpacing = 0.sp)

/** Accent seed for the default StarFall theme. */
val StarFallMint = Color(0xFF8EE6B5)

/** Flip to true to let Android 12+ wallpaper colors replace the StarFall default again. */
const val StarFallUseSystemDynamicColor = false

/** Deep indigo night-sky surfaces; accent colors still come from the mint seed. */
fun ColorScheme.starFallNight() = copy(
    background = Color(0xFF0C0C1C),
    surface = Color(0xFF0C0C1C),
    surfaceDim = Color(0xFF0A0A18),
    surfaceBright = Color(0xFF2A2A4A),
    surfaceContainerLowest = Color(0xFF0A0A18),
    surfaceContainerLow = Color(0xFF12122A),
    surfaceContainer = Color(0xFF17172E),
    surfaceContainerHigh = Color(0xFF1E1E3A),
    surfaceContainerHighest = Color(0xFF262646),
)
