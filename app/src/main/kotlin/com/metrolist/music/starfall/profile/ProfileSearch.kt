/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import com.metrolist.music.starfall.rank.Rank
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A search result. The rank is derived from published hours, so nothing extra is stored. */
data class ProfileHit(val uid: String, val name: String, val avatar: String?, val totalSeconds: Long) {
    val rank: Rank get() = Rank.forSeconds(totalSeconds)
}

/** One page of results; [cursor] is opaque to callers and handed back to fetch the next page. */
data class ProfilePage(val hits: List<ProfileHit>, val cursor: Any?, val endReached: Boolean)

object ProfileQuery {
    const val PAGE_SIZE = 20
    const val DEBOUNCE_MS = 350L

    /** Last code point Firestore orders after any real character, closing the prefix range. */
    private const val RANGE_END = '\uf8ff'

    fun normalize(raw: String): String = raw.trim().lowercase(Locale.ROOT).take(NameLock.MAX_NAME)
    fun upperBound(prefix: String): String = prefix + RANGE_END
}

/**
 * Prefix search with cursor pagination. Callers debounce typing; a new [refresh] discards the old cursor,
 * and [loadMore] is a no-op while a request runs or after the last page.
 */
class ProfileSearchPager(
    private val fetch: suspend (prefix: String, after: Any?, limit: Int) -> ProfilePage,
) {
    private val mutex = Mutex()
    private var prefix = ""
    private var cursor: Any? = null
    private var done = true
    private var items = emptyList<ProfileHit>()

    val results: List<ProfileHit> get() = items
    val endReached: Boolean get() = done

    suspend fun refresh(raw: String): List<ProfileHit> = mutex.withLock {
        prefix = ProfileQuery.normalize(raw)
        cursor = null
        items = emptyList()
        done = prefix.isEmpty()
        if (!done) append(fetch(prefix, null, ProfileQuery.PAGE_SIZE))
        items
    }

    suspend fun loadMore(): List<ProfileHit> = mutex.withLock {
        if (!done) append(fetch(prefix, cursor, ProfileQuery.PAGE_SIZE))
        items
    }

    private fun append(page: ProfilePage) {
        val known = items.mapTo(HashSet()) { it.uid }
        items = items + page.hits.filter { known.add(it.uid) }
        cursor = page.cursor
        done = page.endReached || page.hits.isEmpty()
    }
}
