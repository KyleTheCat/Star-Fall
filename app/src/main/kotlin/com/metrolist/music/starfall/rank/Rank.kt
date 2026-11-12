/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import androidx.annotation.DrawableRes
import com.metrolist.music.R

enum class Rank(val tier: Int, val minHours: Double, val label: String, @DrawableRes val iconRes: Int) {
    STARDUST(1, 0.0, "Stardust", R.drawable.ic_rank_01_stardust),
    METEOR(2, 10.0, "Meteor", R.drawable.ic_rank_02_meteor),
    COMET(3, 25.0, "Comet", R.drawable.ic_rank_03_comet),
    ASTEROID(4, 50.0, "Asteroid", R.drawable.ic_rank_04_asteroid),
    MOON(5, 90.0, "Moon", R.drawable.ic_rank_05_moon),
    STAR(6, 150.0, "Star", R.drawable.ic_rank_06_star),
    PLANET(7, 230.0, "Planet", R.drawable.ic_rank_07_planet),
    NOVA(8, 330.0, "Nova", R.drawable.ic_rank_08_nova),
    RED_GIANT(9, 460.0, "Red Giant", R.drawable.ic_rank_09_red_giant),
    SUPERNOVA(10, 620.0, "Supernova", R.drawable.ic_rank_10_supernova),
    NEUTRON_STAR(11, 800.0, "Neutron Star", R.drawable.ic_rank_11_neutron_star),
    BLACK_HOLE(12, 950.0, "Black Hole", R.drawable.ic_rank_12_black_hole);

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    fun progress(hours: Double): Float {
        val upper = next ?: return 1f
        return ((hours - minHours) / (upper.minHours - minHours)).coerceIn(0.0, 1.0).toFloat()
    }

    companion object {
        fun forHours(hours: Double): Rank = entries.last { hours >= it.minHours }
        fun forSeconds(seconds: Long): Rank = forHours(seconds / 3600.0)
    }
}
