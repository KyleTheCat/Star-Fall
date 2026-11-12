/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall

import com.metrolist.music.starfall.profile.ProfileHit
import com.metrolist.music.starfall.profile.ProfilePage
import com.metrolist.music.starfall.profile.ProfileQuery
import com.metrolist.music.starfall.profile.ProfileSearchPager
import com.metrolist.music.starfall.rank.Rank
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSearchTest {
    private fun hit(i: Int, name: String = "mira$i") = ProfileHit("uid$i", name, null, i * 3600L)

    /** Serves names in order, like a Firestore range query, and records each call. */
    private class FakeBackend(private val all: List<ProfileHit>) {
        val calls = mutableListOf<Triple<String, Any?, Int>>()
        suspend fun fetch(prefix: String, after: Any?, limit: Int): ProfilePage {
            calls += Triple(prefix, after, limit)
            val matches = all.filter { it.name.lowercase().startsWith(prefix) }
            val start = (after as? Int) ?: 0
            val page = matches.drop(start).take(limit)
            return ProfilePage(page, start + page.size, endReached = page.size < limit)
        }
    }

    @Test fun normalizesAndBoundsThePrefix() {
        assertEquals("mir", ProfileQuery.normalize("  MiR "))
        assertEquals("mir\uf8ff", ProfileQuery.upperBound("mir"))
        assertEquals(24, ProfileQuery.normalize("a".repeat(60)).length)
    }

    @Test fun blankQueryDoesNotHitTheBackend() = runBlocking {
        val backend = FakeBackend(listOf(hit(1)))
        val pager = ProfileSearchPager(backend::fetch)
        assertTrue(pager.refresh("   ").isEmpty())
        assertTrue(pager.endReached)
        assertTrue(backend.calls.isEmpty())
    }

    @Test fun paginatesWithTheReturnedCursor() = runBlocking {
        val backend = FakeBackend((1..45).map { hit(it) })
        val pager = ProfileSearchPager(backend::fetch)
        assertEquals(20, pager.refresh("Mira").size)
        assertFalse(pager.endReached)
        assertEquals(40, pager.loadMore().size)
        assertEquals(45, pager.loadMore().size)
        assertTrue(pager.endReached)
        // Nothing further is requested once the last page arrived.
        val calls = backend.calls.size
        pager.loadMore()
        assertEquals(calls, backend.calls.size)
        assertEquals(listOf<Any?>(null, 20, 40), backend.calls.map { it.second })
        assertEquals(ProfileQuery.PAGE_SIZE, backend.calls.first().third)
    }

    @Test fun newQueryResetsTheCursor() = runBlocking {
        val backend = FakeBackend((1..30).map { hit(it) } + hit(99, "zed"))
        val pager = ProfileSearchPager(backend::fetch)
        pager.refresh("mira")
        pager.loadMore()
        assertEquals(listOf("zed"), pager.refresh("Z").map { it.name })
        assertEquals(null, backend.calls.last().second)
    }

    @Test fun dropsDuplicatesAcrossPages() = runBlocking {
        val pages = ArrayDeque(listOf(
            ProfilePage(listOf(hit(1), hit(2)), 1, endReached = false),
            ProfilePage(listOf(hit(2), hit(3)), 2, endReached = true),
        ))
        val pager = ProfileSearchPager { _, _, _ -> pages.removeFirst() }
        pager.refresh("mira")
        assertEquals(listOf("uid1", "uid2", "uid3"), pager.loadMore().map { it.uid })
    }

    @Test fun hitsDeriveTheirRankFromHours() {
        assertEquals(Rank.STARDUST, hit(1).rank)
        assertEquals(Rank.METEOR, hit(10).rank)
    }
}
