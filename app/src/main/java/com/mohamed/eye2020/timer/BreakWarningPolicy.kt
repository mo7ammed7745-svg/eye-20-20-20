package com.mohamed.eye2020.timer

/**
 * Decides when to warn that a break is about to start. Kept free of Android so the
 * "warn once, N seconds before the break" rule can be unit-tested.
 *
 * A warning is due while work is actively counting down and the remaining time is
 * inside the warning window. [warningLeadMillis] is the user's chosen number of seconds
 * (0 when the feature is switched off). [alreadyWarned] makes sure a session warns only
 * once, even though the timer ticks several times per second.
 */
object BreakWarningPolicy {

    fun shouldWarn(
        phase: TimerPhase,
        isActive: Boolean,
        isRunning: Boolean,
        remainingMillis: Long,
        warningLeadMillis: Long,
        alreadyWarned: Boolean,
    ): Boolean {
        if (warningLeadMillis <= 0L) return false
        if (phase != TimerPhase.WORK) return false
        if (!isActive || !isRunning) return false
        if (alreadyWarned) return false
        return remainingMillis in 1L..warningLeadMillis
    }
}
