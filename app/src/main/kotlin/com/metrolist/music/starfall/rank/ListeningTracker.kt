/**
 * StarFall (C) 2026 KyleTheCat
 * Licensed under GPL-3.0
 */

package com.metrolist.music.starfall.rank

import com.metrolist.music.starfall.online.StarFallOnline
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Counts only seconds of active playback; saves locally and syncs online in batches. */
@Singleton
class ListeningTracker @Inject constructor(
    private val store: RankStore,
    private val online: StarFallOnline,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null
    private var pending = 0L

    fun setPlaying(playing: Boolean) = if (playing) startTicking() else stopTicking()

    private fun startTicking() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                delay(1_000)
                pending++
                if (pending >= FLUSH_EVERY_SECONDS) flush()
            }
        }
    }

    private fun stopTicking() {
        ticker?.cancel()
        ticker = null
        flush()
    }

    private fun flush() {
        val seconds = pending
        if (seconds <= 0) return
        pending = 0
        scope.launch {
            runCatching { store.add(seconds) }
            runCatching { online.addListening(seconds) }
        }
    }

    private companion object { const val FLUSH_EVERY_SECONDS = 30L }
}
