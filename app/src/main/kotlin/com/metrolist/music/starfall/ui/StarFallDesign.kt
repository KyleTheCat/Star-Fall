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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.metrolist.music.R
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
    extraLarge = RoundedCornerShape(24.dp),
)

/** Inter (24pt static cuts), matching the design mockups. */
val StarFallFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val base = Typography().let { t ->
    fun TextStyle.inter() = copy(fontFamily = StarFallFontFamily)
    t.copy(
        displayLarge = t.displayLarge.inter(),
        displayMedium = t.displayMedium.inter(),
        displaySmall = t.displaySmall.inter(),
        headlineLarge = t.headlineLarge.inter(),
        headlineMedium = t.headlineMedium.inter(),
        headlineSmall = t.headlineSmall.inter(),
        titleLarge = t.titleLarge.inter(),
        titleMedium = t.titleMedium.inter(),
        titleSmall = t.titleSmall.inter(),
        bodyLarge = t.bodyLarge.inter(),
        bodyMedium = t.bodyMedium.inter(),
        bodySmall = t.bodySmall.inter(),
        labelLarge = t.labelLarge.inter(),
        labelMedium = t.labelMedium.inter(),
        labelSmall = t.labelSmall.inter(),
    )
}

/** Tighter, medium-weight headings over the Material defaults; body text stays untouched. */
val StarFallTypography = base.copy(
    headlineMedium = base.headlineMedium.withWeight(FontWeight.Medium),
    headlineSmall = base.headlineSmall.withWeight(FontWeight.Medium),
    titleLarge = base.titleLarge.withWeight(FontWeight.Medium),
    titleMedium = base.titleMedium.withWeight(FontWeight.Medium),
    labelLarge = base.labelLarge.withWeight(FontWeight.Medium),
)

private fun TextStyle.withWeight(weight: FontWeight) = copy(fontWeight = weight, letterSpacing = 0.sp)

/** Accent seed for the default StarFall theme (soft pink, matches the design mockups). */
val StarFallPink = Color(0xFFF4B8D0)

/** true = the mockup Home (chips, Quick picks, Recently played); false = the full Metrolist feed. */
const val StarFallSimpleHome = true

/** Flip to true to let Android 12+ wallpaper colors replace the StarFall default again. */
const val StarFallUseSystemDynamicColor = false

/**
 * Near-black warm surfaces with a pink accent, sampled from docs/mockups.
 * Surfaces: #0D0C0E (background), #1C191D (cards), #242025 (raised). Accent #F4B8D0, rose #E8A2BE.
 */
fun ColorScheme.starFallNight() = copy(
    primary = Color(0xFFF4B8D0),
    onPrimary = Color(0xFF1A1417),
    primaryContainer = Color(0xFF2D2227),
    onPrimaryContainer = Color(0xFFF4B8D0),
    secondary = Color(0xFFE8A2BE),
    onSecondary = Color(0xFF3A1424),
    secondaryContainer = Color(0xFF2D2227),
    onSecondaryContainer = Color(0xFFF5F3F5),
    tertiary = Color(0xFFE8A2BE),
    onTertiary = Color(0xFF3A1424),
    background = Color(0xFF0D0C0E),
    onBackground = Color(0xFFF5F3F5),
    surface = Color(0xFF0D0C0E),
    onSurface = Color(0xFFF5F3F5),
    surfaceVariant = Color(0xFF242025),
    onSurfaceVariant = Color(0xFF9E969C),
    surfaceTint = Color(0xFFF4B8D0),
    outline = Color(0xFF6B6165),
    outlineVariant = Color(0xFF2E2629),
    surfaceDim = Color(0xFF0D0C0E),
    surfaceBright = Color(0xFF2D2227),
    surfaceContainerLowest = Color(0xFF0D0C0E),
    surfaceContainerLow = Color(0xFF151317),
    surfaceContainer = Color(0xFF1C191D),
    surfaceContainerHigh = Color(0xFF242025),
    surfaceContainerHighest = Color(0xFF2D2227),
)

/** Obsidian canvas and charcoal surfaces only; keeps the accent of a user-picked seed color. */
fun ColorScheme.starFallSurfaces() = copy(
    background = Color(0xFF0D0C0E),
    onBackground = Color(0xFFF5F3F5),
    surface = Color(0xFF0D0C0E),
    onSurface = Color(0xFFF5F3F5),
    surfaceVariant = Color(0xFF242025),
    onSurfaceVariant = Color(0xFF9E969C),
    outlineVariant = Color(0xFF2E2629),
    surfaceDim = Color(0xFF0D0C0E),
    surfaceContainerLowest = Color(0xFF0D0C0E),
    surfaceContainerLow = Color(0xFF151317),
    surfaceContainer = Color(0xFF1C191D),
    surfaceContainerHigh = Color(0xFF242025),
    surfaceContainerHighest = Color(0xFF2D2227),
)
