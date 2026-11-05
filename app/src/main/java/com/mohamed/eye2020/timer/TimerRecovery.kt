package com.mohamed.eye2020.timer

import com.mohamed.eye2020.data.settings.AppSettings
import kotlin.math.abs

/**
 * Pure recovery rules for a timer snapshot left behind by a killed process. Kept separate
 * from TimerManager so the clock and reboot edge cases can be tested without Android.
 */
internal object TimerRecovery {

    fun restore(
        snapshot: TimerSnapshot?,
        settings: AppSettings,
        nowRealtime: Long,
        nowWall: Long,
        screenInteractive: Boolean,
    ): TimerState? {
        if (snapshot == null) return null

        val savedAt = snapshot.savedAtRealtimeMillis
        val elapsed = nowRealtime - savedAt
        val wallElapsed = nowWall - snapshot.savedAtWallMillis

        // elapsedRealtime resets on reboot, and wall time should advance by roughly the
        // same amount while the process is gone. A mismatch means a reboot or a clock
        // change, so the old session must not be treated as continuous work.
        val rebooted = savedAt < 0L ||
            nowRealtime < savedAt ||
            snapshot.savedAtWallMillis <= 0L ||
            wallElapsed < 0L ||
            abs(wallElapsed - elapsed) > CLOCK_DISCREPANCY_TOLERANCE_MS
        val restored = snapshot.state

        if (!restored.isActive) {
            return TimerState.idle(settings.workMillis)
        }
        if (rebooted) {
            return TimerState.idle(settings.workMillis)
        }

        if (restored.isRunning && restored.phase == TimerPhase.WORK) {
            val remainingAtSave = restored.remainingAt(savedAt)

            if (!screenInteractive) {
                // The screen is off now, so some or all of the dead period was not
                // usable work time. Pause at the last saved point and let normal
                // screen-on grace decide whether to resume or reset.
                return restored.copy(
                    isRunning = false,
                    remainingMillis = remainingAtSave,
                    anchorRealtimeMillis = savedAt,
                    pauseReason = PauseReason.SCREEN_OFF,
                    screenOffAtRealtimeMillis = savedAt,
                    screenOffAtWallMillis = snapshot.savedAtWallMillis
                        .takeIf { it > 0L },
                )
            }

            if (elapsed < remainingAtSave) {
                // The process was gone for less than the remaining work time. Subtract
                // the dead period so the recovered countdown continues accurately.
                return restored.copy(
                    remainingMillis = remainingAtSave - elapsed,
                    anchorRealtimeMillis = nowRealtime,
                )
            }

            // Work reached zero while the process was gone. If the break would still
            // be in progress, restore it; otherwise the whole session is over.
            val breakElapsed = elapsed - remainingAtSave
            if (breakElapsed < settings.breakMillis) {
                return TimerState(
                    phase = TimerPhase.BREAK,
                    isActive = true,
                    isRunning = true,
                    remainingMillis = settings.breakMillis - breakElapsed,
                    totalMillis = settings.breakMillis,
                    anchorRealtimeMillis = nowRealtime,
                    sessionStartedAtMillis = nowWall - breakElapsed,
                )
            }
            return TimerState.idle(settings.workMillis)
        }

        if (restored.isRunning) {
            // A break was running when the process died. If it is still in progress,
            // keep its original anchor. If it already ended, the session is over; do
            // not replay a break and auto-start a fresh work session hours later.
            return if (restored.remainingAt(nowRealtime) > 0L) {
                restored
            } else {
                TimerState.idle(settings.workMillis)
            }
        }

        if (restored.phase == TimerPhase.WORK &&
            restored.pauseReason == PauseReason.SCREEN_OFF &&
            elapsed > SCREEN_OFF_PAUSE_STALE_MS
        ) {
            return TimerState.idle(settings.workMillis)
        }
        return restored
    }

    private const val SCREEN_OFF_PAUSE_STALE_MS = 120_000L
    private const val CLOCK_DISCREPANCY_TOLERANCE_MS = 120_000L
}
