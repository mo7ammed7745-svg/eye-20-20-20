package com.mohamed.eye2020.timer

import com.mohamed.eye2020.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerRecoveryTest {

    private val settings = AppSettings(
        workMinutes = 20,
        breakSeconds = 20,
        screenOffGraceMinutes = 2,
    )
    private val savedWall = 1_700_000_000_000L
    private val savedAt = 10_000L

    private fun runningWork(
        remainingMillis: Long = 20 * 60_000L,
        anchorRealtimeMillis: Long = savedAt,
    ) = TimerState(
        phase = TimerPhase.WORK,
        isActive = true,
        isRunning = true,
        remainingMillis = remainingMillis,
        totalMillis = 20 * 60_000L,
        anchorRealtimeMillis = anchorRealtimeMillis,
        sessionStartedAtMillis = savedWall,
    )

    private fun snapshot(state: TimerState) = TimerSnapshot(
        state = state,
        savedAtRealtimeMillis = savedAt,
        savedAtWallMillis = savedWall,
    )

    @Test
    fun runningWorkRecoversExactRemainingAfterDeadPeriod() {
        val now = savedAt + 5 * 60_000L
        val restored = TimerRecovery.restore(
            snapshot = snapshot(runningWork()),
            settings = settings,
            nowRealtime = now,
            nowWall = savedWall + 5 * 60_000L,
            screenInteractive = true,
        )

        assertNotNull(restored)
        assertTrue(restored!!.isActive)
        assertTrue(restored.isRunning)
        assertEquals(TimerPhase.WORK, restored.phase)
        assertEquals(15 * 60_000L, restored.remainingAt(now))
    }

    @Test
    fun workThatFinishedWhileGoneRestoresRemainingBreak() {
        val now = savedAt + 15_000L
        val restored = TimerRecovery.restore(
            snapshot = snapshot(runningWork(remainingMillis = 10_000L)),
            settings = settings,
            nowRealtime = now,
            nowWall = savedWall + 15_000L,
            screenInteractive = true,
        )

        assertNotNull(restored)
        assertEquals(TimerPhase.BREAK, restored!!.phase)
        assertTrue(restored.isRunning)
        assertEquals(15_000L, restored.remainingAt(now))
        assertEquals(20_000L, restored.totalMillis)
        assertEquals(savedWall + 10_000L, restored.sessionStartedAtMillis)
    }

    @Test
    fun workAndBreakThatFinishedWhileGoneResetToIdle() {
        val now = savedAt + 45_000L
        val restored = TimerRecovery.restore(
            snapshot = snapshot(runningWork(remainingMillis = 10_000L)),
            settings = settings,
            nowRealtime = now,
            nowWall = savedWall + 45_000L,
            screenInteractive = true,
        )

        assertNotNull(restored)
        assertFalse(restored!!.isActive)
        assertFalse(restored.isRunning)
        assertEquals(settings.workMillis, restored.remainingMillis)
    }

    @Test
    fun wallClockMismatchIsTreatedAsAReboot() {
        val restored = TimerRecovery.restore(
            snapshot = snapshot(runningWork()),
            settings = settings,
            nowRealtime = savedAt + 5 * 60_000L,
            nowWall = savedWall + 60 * 60_000L,
            screenInteractive = true,
        )

        assertNotNull(restored)
        assertFalse(restored!!.isActive)
        assertEquals(settings.workMillis, restored.remainingMillis)
    }

    @Test
    fun screenOffRestorePausesAtTheLastSavedPoint() {
        val now = savedAt + 5 * 60_000L
        val restored = TimerRecovery.restore(
            snapshot = snapshot(runningWork()),
            settings = settings,
            nowRealtime = now,
            nowWall = savedWall + 5 * 60_000L,
            screenInteractive = false,
        )

        assertNotNull(restored)
        assertFalse(restored!!.isRunning)
        assertEquals(PauseReason.SCREEN_OFF, restored.pauseReason)
        assertEquals(20 * 60_000L, restored.remainingMillis)
        assertEquals(savedAt, restored.screenOffAtRealtimeMillis)
    }

    @Test
    fun breakStillInProgressIsRestored() {
        val breakState = TimerState(
            phase = TimerPhase.BREAK,
            isActive = true,
            isRunning = true,
            remainingMillis = 20_000L,
            totalMillis = 20_000L,
            anchorRealtimeMillis = savedAt,
            sessionStartedAtMillis = savedWall,
        )
        val now = savedAt + 8_000L
        val restored = TimerRecovery.restore(
            snapshot = snapshot(breakState),
            settings = settings,
            nowRealtime = now,
            nowWall = savedWall + 8_000L,
            screenInteractive = true,
        )

        assertNotNull(restored)
        assertEquals(TimerPhase.BREAK, restored!!.phase)
        assertTrue(restored.isRunning)
        assertEquals(12_000L, restored.remainingAt(now))
    }

    @Test
    fun breakThatFinishedWhileGoneEndsTheSession() {
        val breakState = TimerState(
            phase = TimerPhase.BREAK,
            isActive = true,
            isRunning = true,
            remainingMillis = 20_000L,
            totalMillis = 20_000L,
            anchorRealtimeMillis = savedAt,
            sessionStartedAtMillis = savedWall,
        )
        val now = savedAt + 25_000L
        val restored = TimerRecovery.restore(
            snapshot = snapshot(breakState),
            settings = settings,
            nowRealtime = now,
            nowWall = savedWall + 25_000L,
            screenInteractive = true,
        )

        assertNotNull(restored)
        assertFalse(restored!!.isActive)
        assertFalse(restored.isRunning)
        assertEquals(settings.workMillis, restored.remainingMillis)
    }

    @Test
    fun userPausedWorkIsStillThereAfterALongGap() {
        val paused = TimerState(
            phase = TimerPhase.WORK,
            isActive = true,
            isRunning = false,
            remainingMillis = 12 * 60_000L,
            totalMillis = 20 * 60_000L,
            anchorRealtimeMillis = savedAt,
            pauseReason = PauseReason.USER,
        )
        val restored = TimerRecovery.restore(
            snapshot = snapshot(paused),
            settings = settings,
            nowRealtime = savedAt + 24 * 60 * 60_000L,
            nowWall = savedWall + 24 * 60 * 60_000L,
            screenInteractive = true,
        )

        assertNotNull(restored)
        assertTrue(restored!!.isActive)
        assertFalse(restored.isRunning)
        assertEquals(PauseReason.USER, restored.pauseReason)
        assertEquals(12 * 60_000L, restored.remainingMillis)
    }

    @Test
    fun missingSnapshotLeavesTheTimerAtItsDefaultState() {
        val restored = TimerRecovery.restore(
            snapshot = null,
            settings = settings,
            nowRealtime = savedAt,
            nowWall = savedWall,
            screenInteractive = true,
        )

        assertEquals(null, restored)
    }
}
