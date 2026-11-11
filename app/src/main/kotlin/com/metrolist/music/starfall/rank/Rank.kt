/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

enum class Rank(val tier: Int, val minHours: Double, val label: String, val icon: String) {
    STARDUST(1, 0.0, "Stardust", "🌌"),
    METEOR(2, 10.0, "Meteor", "☄️"),
    COMET(3, 25.0, "Comet", "🌠"),
    ASTEROID(4, 50.0, "Asteroid", "🪨"),
    MOON(5, 90.0, "Moon", "🌙"),
    STAR(6, 150.0, "Star", "⭐"),
    PLANET(7, 230.0, "Planet", "🪐"),
    NOVA(8, 330.0, "Nova", "💥"),
    RED_GIANT(9, 460.0, "Red Giant", "🔴"),
    SUPERNOVA(10, 620.0, "Supernova", "🌟"),
    NEUTRON_STAR(11, 800.0, "Neutron Star", "⚛️"),
    BLACK_HOLE(12, 950.0, "Black Hole", "⚫");

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
