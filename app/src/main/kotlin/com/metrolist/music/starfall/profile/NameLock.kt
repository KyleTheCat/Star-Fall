/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.profile

import java.util.concurrent.TimeUnit

/** Result of asking to change the display name. */
sealed interface NameChangeResult {
    data class Changed(val name: String) : NameChangeResult
    data object Unchanged : NameChangeResult
    data class Locked(val remainingMs: Long) : NameChangeResult
    data object Invalid : NameChangeResult
}

/** Time left on the name lock, split for display. Minutes round up so a locked name never reads "0m". */
data class LockRemaining(val days: Long, val hours: Long, val minutes: Long)

/** The display name may change once every [LOCK_MS]; this is the single source of that rule on-device. */
object NameLock {
    const val MAX_NAME = 24
    val LOCK_MS: Long = TimeUnit.DAYS.toMillis(7)

    /** Zero when the name was never changed. A clock set back before the last change keeps the full lock. */
    fun remainingMs(lastChangeAt: Long, now: Long): Long {
        if (lastChangeAt <= 0L) return 0L
        return (lastChangeAt + LOCK_MS - now).coerceIn(0L, LOCK_MS)
    }

    fun split(remainingMs: Long): LockRemaining {
        val totalMinutes = (remainingMs + TimeUnit.MINUTES.toMillis(1) - 1) / TimeUnit.MINUTES.toMillis(1)
        return LockRemaining(totalMinutes / (24 * 60), totalMinutes / 60 % 24, totalMinutes % 60)
    }

    /** Trims and collapses whitespace; null when blank or too long. */
    fun normalize(raw: String): String? {
        val name = raw.trim().replace(Regex("\\s+"), " ")
        return name.takeIf { it.isNotEmpty() && it.length <= MAX_NAME }
    }

    fun evaluate(currentName: String?, lastChangeAt: Long, raw: String, now: Long): NameChangeResult {
        val name = normalize(raw) ?: return NameChangeResult.Invalid
        if (name == currentName) return NameChangeResult.Unchanged
        val remaining = remainingMs(lastChangeAt, now)
        return if (remaining > 0L) NameChangeResult.Locked(remaining) else NameChangeResult.Changed(name)
    }
}
