/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.background

import androidx.annotation.StringRes
import com.metrolist.music.R

enum class BackgroundOption(val id: String, @StringRes val title: Int, @StringRes val blurb: Int) {
    NEBULA("nebula", R.string.starfall_bg_nebula, R.string.starfall_bg_nebula_desc),
    STARFIELD("starfield", R.string.starfall_bg_starfield, R.string.starfall_bg_starfield_desc),
    AURORA("aurora", R.string.starfall_bg_aurora, R.string.starfall_bg_aurora_desc),
    FLARE("flare", R.string.starfall_bg_flare, R.string.starfall_bg_flare_desc),
    ECLIPSE("eclipse", R.string.starfall_bg_eclipse, R.string.starfall_bg_eclipse_desc),
    GALAXY("galaxy", R.string.starfall_bg_galaxy, R.string.starfall_bg_galaxy_desc),
    VOID("void", R.string.starfall_bg_void, R.string.starfall_bg_void_desc),
    COMET("comet", R.string.starfall_bg_comet, R.string.starfall_bg_comet_desc),
    CUSTOM("custom", R.string.starfall_bg_custom, R.string.starfall_bg_custom_desc);

    val isWeb: Boolean get() = this != CUSTOM

    companion object {
        fun fromId(id: String?): BackgroundOption = entries.firstOrNull { it.id == id } ?: NEBULA
    }
}
