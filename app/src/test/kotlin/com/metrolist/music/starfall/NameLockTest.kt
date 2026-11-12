/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall

import com.metrolist.music.starfall.profile.NameChangeResult
import com.metrolist.music.starfall.profile.NameLock
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NameLockTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private val now = 1_000 * day

    @Test fun neverChangedIsNotLocked() {
        assertEquals(0L, NameLock.remainingMs(0L, now))
        assertEquals(NameChangeResult.Changed("Nova"), NameLock.evaluate(null, 0L, "Nova", now))
    }

    @Test fun lockedForSevenDays() {
        val changedAt = now - 3 * day - TimeUnit.HOURS.toMillis(20)
        val result = NameLock.evaluate("Old", changedAt, "New", now)
        assertTrue(result is NameChangeResult.Locked)
        assertEquals(3 * day + TimeUnit.HOURS.toMillis(4), (result as NameChangeResult.Locked).remainingMs)
    }

    @Test fun unlocksExactlyAtSevenDays() {
        assertEquals(NameChangeResult.Locked(1L), NameLock.evaluate("Old", now - NameLock.LOCK_MS + 1, "New", now))
        assertEquals(NameChangeResult.Changed("New"), NameLock.evaluate("Old", now - NameLock.LOCK_MS, "New", now))
    }

    @Test fun clockSetBackKeepsFullLock() {
        assertEquals(NameLock.LOCK_MS, NameLock.remainingMs(now + 30 * day, now))
    }

    @Test fun sameNameIsUnchangedEvenWhenLocked() {
        assertEquals(NameChangeResult.Unchanged, NameLock.evaluate("Nova", now - day, "  Nova ", now))
    }

    @Test fun validatesNames() {
        assertEquals(NameChangeResult.Invalid, NameLock.evaluate(null, 0L, "   ", now))
        assertEquals(NameChangeResult.Invalid, NameLock.evaluate(null, 0L, "x".repeat(NameLock.MAX_NAME + 1), now))
        assertEquals("Star Fall", NameLock.normalize("  Star   Fall "))
        assertNull(NameLock.normalize(""))
    }

    @Test fun splitsRemainingTimeAndRoundsMinutesUp() {
        val parts = NameLock.split(3 * day + TimeUnit.HOURS.toMillis(4) + 1)
        assertEquals(3L, parts.days)
        assertEquals(4L, parts.hours)
        assertEquals(1L, parts.minutes)
    }
}
