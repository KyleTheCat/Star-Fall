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
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
val StarFallPink = Color(0xFFF4A3BE)

/** true = the mockup Home (chips, Quick picks, Recently played); false = the full Metrolist feed. */
const val StarFallSimpleHome = true

/** Flip to true to let Android 12+ wallpaper colors replace the StarFall default again. */
const val StarFallUseSystemDynamicColor = false

/**
 * Near-black warm surfaces with a pink accent, sampled from docs/mockups.
 * Surfaces: #0E0B0D (background), #1A1517 (cards, chips), #251E21 (raised, selected).
 */
fun ColorScheme.starFallNight() = copy(
    primary = Color(0xFFF4A3BE),
    onPrimary = Color(0xFF3A1424),
    primaryContainer = Color(0xFF251E21),
    onPrimaryContainer = Color(0xFFF4A3BE),
    secondary = Color(0xFFE2B8C6),
    onSecondary = Color(0xFF3A1424),
    secondaryContainer = Color(0xFF251E21),
    onSecondaryContainer = Color(0xFFF2E9EC),
    tertiary = Color(0xFFF4A3BE),
    onTertiary = Color(0xFF3A1424),
    background = Color(0xFF0E0B0D),
    onBackground = Color(0xFFF2E9EC),
    surface = Color(0xFF0E0B0D),
    onSurface = Color(0xFFF2E9EC),
    surfaceVariant = Color(0xFF251E21),
    onSurfaceVariant = Color(0xFFA99CA1),
    surfaceTint = Color(0xFFF4A3BE),
    outline = Color(0xFF6B6165),
    outlineVariant = Color(0xFF2E2629),
    surfaceDim = Color(0xFF0E0B0D),
    surfaceBright = Color(0xFF332A2E),
    surfaceContainerLowest = Color(0xFF0A0809),
    surfaceContainerLow = Color(0xFF141012),
    surfaceContainer = Color(0xFF1A1517),
    surfaceContainerHigh = Color(0xFF251E21),
    surfaceContainerHighest = Color(0xFF2E2629),
)
