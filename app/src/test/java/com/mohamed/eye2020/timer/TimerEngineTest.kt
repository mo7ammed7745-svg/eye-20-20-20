package com.mohamed.eye2020.timer

import com.mohamed.eye2020.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private class FakeClock(
        var realtime: Long = 10_000L,
        var wall: Long = 1_700_000_000_000L,
    ) : Clock {
        override fun realtimeMillis(): Long = realtime
        override fun wallTimeMillis(): Long = wall

        fun advance(millis: Long) {
            realtime += millis
            wall += millis
        }
    }

    private val clock = FakeClock()
    private var screenOn = true
    private var settings = AppSettings(
        workMinutes = 20,
        breakSeconds = 20,
        screenOffGraceMinutes = 2,
        autoStartNextWork = true,
    )
    private val engine = TimerEngine(
        clock = clock,
        settingsProvider = { settings },
        screenOnProvider = { screenOn },
    )

    @Test
    fun startUsesConfiguredDurationAndDoesNotDrift() {
        val events = engine.startOrResumeWork()
        assertEquals(listOf(TimerEvent.WorkStarted), events)

        val started = engine.state.value
        assertTrue(started.isActive)
        assertTrue(started.isRunning)
        assertEquals(20 * 60_000L, started.remainingAt(clock.realtime))

        clock.advance(60_000L)
        assertEquals(19 * 60_000L, engine.state.value.remainingAt(clock.realtime))
    }

    @Test
    fun workFinishesIntoBreakButDoesNotRecordYet() {
        engine.startOrResumeWork()
        clock.advance(20 * 60_000L)

        val events = engine.tick()
        assertTrue(events.any { it is TimerEvent.WorkCompleted })
        assertTrue(events.any { it is TimerEvent.BreakStarted })
        assertFalse(events.any { it is TimerEvent.BreakCompleted })

        val state = engine.state.value
        assertEquals(TimerPhase.BREAK, state.phase)
        assertTrue(state.isActive)
        assertTrue(state.isRunning)
        assertEquals(20_000L, state.remainingAt(clock.realtime))
    }

    @Test
    fun breakCompletionIsRecordedExactlyOnceAndAutoStartsWork() {
        engine.startOrResumeWork()
        clock.advance(20 * 60_000L)
        engine.tick()

        clock.advance(20_000L)
        val events = engine.tick()
        val completed = events.filterIsInstance<TimerEvent.BreakCompleted>().single()
        assertEquals(20, completed.durationSeconds)
        assertTrue(completed.completedAtMillis >= completed.sessionStartedAtMillis)

        val state = engine.state.value
        assertEquals(TimerPhase.WORK, state.phase)
        assertTrue(state.isActive)
        assertTrue(state.isRunning)
        assertEquals(20 * 60_000L, state.remainingAt(clock.realtime))

        assertTrue(engine.tick().isEmpty())
    }

    @Test
    fun incompleteBreakIsNeverReportedAsCompleted() {
        engine.startOrResumeWork()
        clock.advance(20 * 60_000L)
        engine.tick()

        clock.advance(10_000L)
        assertTrue(engine.tick().isEmpty())
        assertEquals(TimerPhase.BREAK, engine.state.value.phase)
    }

    @Test
    fun disablingAutoStartLeavesAReadyWorkTimer() {
        settings = settings.copy(autoStartNextWork = false)
        engine.startOrResumeWork()
        clock.advance(20 * 60_000L)
        engine.tick()
        clock.advance(20_000L)

        val events = engine.tick()
        assertTrue(events.any { it is TimerEvent.BreakCompleted })
        val state = engine.state.value
        assertEquals(TimerPhase.WORK, state.phase)
        assertFalse(state.isActive)
        assertFalse(state.isRunning)
        assertEquals(20 * 60_000L, state.remainingMillis)
    }

    @Test
    fun userPauseFreezesRemainingTime() {
        engine.startOrResumeWork()
        clock.advance(60_000L)
        engine.pauseWork()

        val paused = engine.state.value
        assertEquals(PauseReason.USER, paused.pauseReason)
        assertEquals(19 * 60_000L, paused.remainingMillis)

        clock.advance(120_000L)
        assertEquals(19 * 60_000L, engine.state.value.remainingAt(clock.realtime))

        engine.startOrResumeWork()
        assertTrue(engine.state.value.isRunning)
        clock.advance(60_000L)
        assertEquals(18 * 60_000L, engine.state.value.remainingAt(clock.realtime))
    }

    @Test
    fun screenOffPausesWorkAndScreenOnWithinGraceResumesIt() {
        engine.startOrResumeWork()
        clock.advance(5 * 60_000L)

        screenOn = false
        engine.onScreenOff()
        val paused = engine.state.value
        assertEquals(PauseReason.SCREEN_OFF, paused.pauseReason)
        assertEquals(15 * 60_000L, paused.remainingMillis)

        clock.advance(60_000L)
        screenOn = true
        val events = engine.onScreenOn()
        assertTrue(events.any { it is TimerEvent.WorkResumedAfterScreenOff })
        assertTrue(engine.state.value.isRunning)
        assertEquals(15 * 60_000L, engine.state.value.remainingAt(clock.realtime))

        clock.advance(60_000L)
        assertEquals(14 * 60_000L, engine.state.value.remainingAt(clock.realtime))
    }

    @Test
    fun screenOffExactlyAtGraceBoundaryResumes() {
        engine.startOrResumeWork()
        clock.advance(2 * 60_000L)
        screenOn = false
        engine.onScreenOff()

        clock.advance(2 * 60_000L)
        screenOn = true
        engine.onScreenOn()

        assertTrue(engine.state.value.isRunning)
        assertEquals(18 * 60_000L, engine.state.value.remainingAt(clock.realtime))
    }

    @Test
    fun screenOffBeyondGraceResetsToFullWorkDuration() {
        engine.startOrResumeWork()
        clock.advance(4 * 60_000L)
        screenOn = false
        engine.onScreenOff()

        clock.advance(3 * 60_000L)
        screenOn = true
        val events = engine.onScreenOn()

        assertTrue(events.any { it is TimerEvent.WorkResetAfterScreenOff })
        val state = engine.state.value
        assertFalse(state.isActive)
        assertFalse(state.isRunning)
        assertEquals(20 * 60_000L, state.remainingMillis)
        assertEquals(PauseReason.SCREEN_OFF_RESET, state.pauseReason)
    }

    @Test
    fun breakKeepsCountingWhileScreenIsOff() {
        engine.startOrResumeWork()
        clock.advance(20 * 60_000L)
        engine.tick()

        clock.advance(5_000L)
        screenOn = false
        engine.onScreenOff()
        assertEquals(TimerPhase.BREAK, engine.state.value.phase)
        assertTrue(engine.state.value.isRunning)

        clock.advance(10_000L)
        screenOn = true
        engine.onScreenOn()
        assertEquals(5_000L, engine.state.value.remainingAt(clock.realtime))

        clock.advance(5_000L)
        val events = engine.tick()
        assertTrue(events.any { it is TimerEvent.BreakCompleted })
    }

    @Test
    fun breakEndingWhileScreenOffArmsPausedWork() {
        engine.startOrResumeWork()
        clock.advance(20 * 60_000L)
        engine.tick()

        screenOn = false
        clock.advance(20_000L)
        val events = engine.tick()

        assertTrue(events.any { it is TimerEvent.BreakCompleted })
        val state = engine.state.value
        assertEquals(TimerPhase.WORK, state.phase)
        assertTrue(state.isActive)
        assertFalse(state.isRunning)
        assertEquals(PauseReason.SCREEN_OFF, state.pauseReason)
        assertEquals(20 * 60_000L, state.remainingMillis)

        clock.advance(60_000L)
        screenOn = true
        engine.onScreenOn()
        assertTrue(engine.state.value.isRunning)
        assertEquals(20 * 60_000L, engine.state.value.remainingAt(clock.realtime))
    }

    @Test
    fun changingWorkDurationRestartsRunningCountdownAtFullValue() {
        engine.startOrResumeWork()
        clock.advance(60_000L)

        settings = settings.copy(workMinutes = 30)
        engine.applySettings(settings)

        val running = engine.state.value
        assertTrue(running.isRunning)
        assertEquals(30 * 60_000L, running.totalMillis)
        assertEquals(30 * 60_000L, running.remainingAt(clock.realtime))

        clock.advance(60_000L)
        assertEquals(29 * 60_000L, engine.state.value.remainingAt(clock.realtime))
    }

    @Test
    fun changingWorkDurationResetsPausedTimerAndKeepsItPaused() {
        engine.startOrResumeWork()
        clock.advance(60_000L)
        engine.pauseWork()

        settings = settings.copy(workMinutes = 30)
        engine.applySettings(settings)

        val paused = engine.state.value
        assertFalse(paused.isRunning)
        assertTrue(paused.isActive)
        assertEquals(PauseReason.USER, paused.pauseReason)
        assertEquals(30 * 60_000L, paused.totalMillis)
        assertEquals(30 * 60_000L, paused.remainingMillis)

        clock.advance(5 * 60_000L)
        assertEquals(30 * 60_000L, engine.state.value.remainingAt(clock.realtime))

        engine.startOrResumeWork()
        clock.advance(60_000L)
        assertEquals(29 * 60_000L, engine.state.value.remainingAt(clock.realtime))
    }

    @Test
    fun changingWorkDurationDoesNotInterruptBreak() {
        engine.startOrResumeWork()
        clock.advance(20 * 60_000L)
        engine.tick()
        clock.advance(5_000L)

        settings = settings.copy(workMinutes = 30)
        engine.applySettings(settings)

        val duringBreak = engine.state.value
        assertEquals(TimerPhase.BREAK, duringBreak.phase)
        assertEquals(20_000L, duringBreak.totalMillis)
        assertEquals(15_000L, duringBreak.remainingAt(clock.realtime))

        clock.advance(15_000L)
        val events = engine.tick()
        assertTrue(events.any { it is TimerEvent.BreakCompleted })

        val nextWork = engine.state.value
        assertEquals(TimerPhase.WORK, nextWork.phase)
        assertEquals(30 * 60_000L, nextWork.totalMillis)
        assertEquals(30 * 60_000L, nextWork.remainingAt(clock.realtime))
    }

    @Test
    fun changingUnrelatedSettingsDoesNotResetRunningCountdown() {
        engine.startOrResumeWork()
        clock.advance(60_000L)

        settings = settings.copy(autoStartNextWork = false)
        engine.applySettings(settings)

        val running = engine.state.value
        assertEquals(20 * 60_000L, running.totalMillis)
        assertEquals(19 * 60_000L, running.remainingAt(clock.realtime))
    }

    @Test
    fun aLiveBreakCannotBeSkippedByStartingWork() {
        engine.startOrResumeWork()
        clock.advance(20 * 60_000L)
        engine.tick()

        assertTrue(engine.startOrResumeWork().isEmpty())
        assertEquals(TimerPhase.BREAK, engine.state.value.phase)
    }
}
