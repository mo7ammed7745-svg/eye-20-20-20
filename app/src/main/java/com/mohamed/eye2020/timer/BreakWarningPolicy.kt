package com.mohamed.eye2020.timer

/**
 * Decides when to warn that a break is about to start. Kept free of Android so the
 * "warn once, ten seconds before the break" rule can be unit-tested.
 *
 * A warning is due while work is actively counting down and the remaining time is
 * inside the ten-second window. [alreadyWarned] makes sure a session warns only once,
 * even though the timer ticks several times per second.
 */
object BreakWarningPolicy {

    const val WARNING_LEAD_MILLIS = 10_000L

    fun shouldWarn(
        phase: TimerPhase,
        isActive: Boolean,
        isRunning: Boolean,
        remainingMillis: Long,
        alreadyWarned: Boolean,
    ): Boolean {
        if (phase != TimerPhase.WORK) return false
        if (!isActive || !isRunning) return false
        if (alreadyWarned) return false
        return remainingMillis in 1L..WARNING_LEAD_MILLIS
    }
}
