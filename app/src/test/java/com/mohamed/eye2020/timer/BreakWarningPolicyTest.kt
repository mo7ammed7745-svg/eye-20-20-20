package com.mohamed.eye2020.timer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakWarningPolicyTest {

    private fun shouldWarn(
        phase: TimerPhase = TimerPhase.WORK,
        isActive: Boolean = true,
        isRunning: Boolean = true,
        remainingMillis: Long,
        warningLeadMillis: Long = 10_000L,
        alreadyWarned: Boolean = false,
    ): Boolean = BreakWarningPolicy.shouldWarn(
        phase = phase,
        isActive = isActive,
        isRunning = isRunning,
        remainingMillis = remainingMillis,
        warningLeadMillis = warningLeadMillis,
        alreadyWarned = alreadyWarned,
    )

    @Test
    fun warnsInsideTheLastTenSeconds() {
        assertTrue(shouldWarn(remainingMillis = 10_000L))
        assertTrue(shouldWarn(remainingMillis = 6_500L))
        assertTrue(shouldWarn(remainingMillis = 1L))
    }

    @Test
    fun doesNotWarnBeforeTheWindow() {
        assertFalse(shouldWarn(remainingMillis = 10_001L))
        assertFalse(shouldWarn(remainingMillis = 60_000L))
    }

    @Test
    fun followsTheChosenCountdownLength() {
        assertTrue(shouldWarn(remainingMillis = 20_000L, warningLeadMillis = 20_000L))
        assertFalse(shouldWarn(remainingMillis = 21_000L, warningLeadMillis = 20_000L))
        assertTrue(shouldWarn(remainingMillis = 5_000L, warningLeadMillis = 5_000L))
        assertFalse(shouldWarn(remainingMillis = 6_000L, warningLeadMillis = 5_000L))
    }

    @Test
    fun switchedOffNeverWarns() {
        assertFalse(shouldWarn(remainingMillis = 5_000L, warningLeadMillis = 0L))
        assertFalse(shouldWarn(remainingMillis = 1L, warningLeadMillis = 0L))
    }

    @Test
    fun doesNotWarnTwiceForTheSameSession() {
        assertFalse(shouldWarn(remainingMillis = 9_000L, alreadyWarned = true))
    }

    @Test
    fun doesNotWarnOutsideRunningWork() {
        assertFalse(shouldWarn(phase = TimerPhase.BREAK, remainingMillis = 5_000L))
        assertFalse(shouldWarn(isActive = false, remainingMillis = 5_000L))
        assertFalse(shouldWarn(isRunning = false, remainingMillis = 5_000L))
    }

    @Test
    fun doesNotWarnAtZero() {
        assertFalse(shouldWarn(remainingMillis = 0L))
    }
}
