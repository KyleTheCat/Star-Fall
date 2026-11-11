package com.metrolist.music.starfall

import com.metrolist.music.starfall.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Test

class RankTest {
    @Test fun boundaries() {
        assertEquals(Rank.STARDUST, Rank.forHours(9.99))
        assertEquals(Rank.METEOR, Rank.forHours(10.0))
        assertEquals(Rank.BLACK_HOLE, Rank.forSeconds(1000L * 3600))
    }

    @Test fun progressIsClamped() {
        assertEquals(0.5f, Rank.STARDUST.progress(5.0), 0.001f)
        assertEquals(1f, Rank.BLACK_HOLE.progress(2000.0), 0f)
    }
}
