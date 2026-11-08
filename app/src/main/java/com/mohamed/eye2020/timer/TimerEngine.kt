package com.mohamed.eye2020.timer

import com.mohamed.eye2020.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A platform-free state machine. Android code supplies the clock, the screen state and
 * the current settings; tests supply fakes. Every transition is synchronised so the
 * service, notification and UI can never create competing timers.
 */
class TimerEngine(
    private val clock: Clock,
    private val settingsProvider: () -> AppSettings,
    private val screenOnProvider: () -> Boolean = { true },
    initialState: TimerState? = null,
) {
    private var appliedWorkMillis: Long = settingsProvider().workMillis

    private val _state = MutableStateFlow(
        initialState ?: TimerState.idle(settingsProvider().workMillis),
    )
    val state: StateFlow<TimerState> = _state.asStateFlow()

    /**
     * A saved work duration immediately restarts the work countdown at the new full
     * value. A paused timer stays paused and keeps the full new value until resumed.
     * A live break is never interrupted; the new duration is used by the next work session.
     */
    @Synchronized
    fun applySettings(settings: AppSettings) {
        val workMillis = settings.workMillis
        val workDurationChanged = workMillis != appliedWorkMillis
        appliedWorkMillis = workMillis

        val current = _state.value
        if (current.phase != TimerPhase.WORK) return

        if (!workDurationChanged) {
            val isReadyWork = !current.isActive || current.pauseReason == PauseReason.SCREEN_OFF_RESET
            if (isReadyWork) {
                _state.value = current.copy(
                    remainingMillis = workMillis,
                    totalMillis = workMillis,
                )
            }
            return
        }

        _state.value = current.copy(
            remainingMillis = workMillis,
            totalMillis = workMillis,
            anchorRealtimeMillis = clock.realtimeMillis(),
            sessionStartedAtMillis = if (current.isActive) clock.wallTimeMillis() else 0L,
        )
    }

    /**
     * Starts a fresh work session, or resumes a paused user/screen-off session.
     * A live break cannot be skipped from here.
     */
    @Synchronized
    fun startOrResumeWork(): List<TimerEvent> {
        val now = clock.realtimeMillis()
        val wall = clock.wallTimeMillis()
        val current = _state.value

        if (current.phase == TimerPhase.BREAK && current.isActive) return emptyList()
        if (current.phase == TimerPhase.WORK && current.isActive && current.isRunning) {
            return emptyList()
        }

        val canResume = current.phase == TimerPhase.WORK &&
            current.isActive &&
            !current.isRunning &&
            (current.pauseReason == PauseReason.USER || current.pauseReason == PauseReason.SCREEN_OFF)

        if (canResume) {
            _state.value = current.copy(
                isRunning = true,
                anchorRealtimeMillis = now,
                pauseReason = null,
                screenOffAtRealtimeMillis = null,
                screenOffAtWallMillis = null,
            )
            val events = mutableListOf<TimerEvent>(TimerEvent.WorkResumed)
            if (!screenOnProvider()) {
                events += pauseForScreenOff(now, wall)
            }
            return events
        }

        val duration = settingsProvider().workMillis
        _state.value = TimerState(
            phase = TimerPhase.WORK,
            isActive = true,
            isRunning = true,
            remainingMillis = duration,
            totalMillis = duration,
            anchorRealtimeMillis = now,
            sessionStartedAtMillis = wall,
        )
        val events = mutableListOf<TimerEvent>(TimerEvent.WorkStarted)
        if (!screenOnProvider()) {
            events += pauseForScreenOff(now, wall)
        }
        return events
    }

    @Synchronized
    fun pauseWork(): List<TimerEvent> {
        val now = clock.realtimeMillis()
        val current = _state.value
        if (current.phase != TimerPhase.WORK || !current.isActive || !current.isRunning) {
            return emptyList()
        }
        val remaining = current.remainingAt(now)
        _state.value = current.copy(
            isRunning = false,
            remainingMillis = remaining,
            anchorRealtimeMillis = now,
            pauseReason = PauseReason.USER,
            screenOffAtRealtimeMillis = null,
            screenOffAtWallMillis = null,
        )
        return listOf(TimerEvent.WorkPaused)
    }

    /** Screen-off only pauses work. A break deliberately keeps counting. */
    @Synchronized
    fun onScreenOff(): List<TimerEvent> {
        val current = _state.value
        if (current.phase != TimerPhase.WORK || !current.isActive || !current.isRunning) {
            return emptyList()
        }
        return listOf(
            pauseForScreenOff(
                nowRealtime = clock.realtimeMillis(),
                nowWall = clock.wallTimeMillis(),
            ),
        )
    }

    /**
     * Screen-on after a screen-off pause. Within the grace period the remaining countdown
     * resumes; past the grace period a fresh work session starts from zero automatically,
     * so the user never has to open the app and press Start again. A stale
     * [PauseReason.SCREEN_OFF_RESET] arm (restored from a killed process) also restarts
     * fresh here.
     */
    @Synchronized
    fun onScreenOn(): List<TimerEvent> {
        val now = clock.realtimeMillis()
        val wall = clock.wallTimeMillis()
        val current = _state.value

        if (current.phase == TimerPhase.WORK &&
            !current.isActive &&
            current.pauseReason == PauseReason.SCREEN_OFF_RESET
        ) {
            return startFreshWorkAfterScreenOff(now, wall, screenOffDurationMillis = 0L)
        }

        if (current.phase != TimerPhase.WORK ||
            !current.isActive ||
            current.isRunning ||
            current.pauseReason != PauseReason.SCREEN_OFF
        ) {
            return emptyList()
        }

        val screenOffAt = current.screenOffAtRealtimeMillis
        val offDuration = screenOffAt?.let { (now - it).coerceAtLeast(0L) }
        val settings = settingsProvider()

        if (offDuration != null && offDuration <= settings.graceMillis) {
            _state.value = current.copy(
                isRunning = true,
                anchorRealtimeMillis = now,
                pauseReason = null,
                screenOffAtRealtimeMillis = null,
                screenOffAtWallMillis = null,
            )
            return listOf(TimerEvent.WorkResumedAfterScreenOff(current.remainingMillis))
        }

        return startFreshWorkAfterScreenOff(now, wall, offDuration ?: 0L)
    }

    private fun startFreshWorkAfterScreenOff(
        now: Long,
        wall: Long,
        screenOffDurationMillis: Long,
    ): List<TimerEvent> {
        val fullDuration = settingsProvider().workMillis
        _state.value = TimerState(
            phase = TimerPhase.WORK,
            isActive = true,
            isRunning = true,
            remainingMillis = fullDuration,
            totalMillis = fullDuration,
            anchorRealtimeMillis = now,
            sessionStartedAtMillis = wall,
        )
        return listOf(
            TimerEvent.WorkResetAfterScreenOff(
                fullDurationMillis = fullDuration,
                screenOffDurationMillis = screenOffDurationMillis,
            ),
            TimerEvent.WorkStartedAfterBreak,
        )
    }

    /**
     * Ends the current work session (running or paused) and returns to a ready idle
     * timer. A live break cannot be stopped this way; use [skipBreak].
     */
    @Synchronized
    fun reset(): List<TimerEvent> {
        val current = _state.value
        if (current.phase != TimerPhase.WORK || !current.isActive) return emptyList()
        _state.value = TimerState.idle(settingsProvider().workMillis)
        return listOf(TimerEvent.WorkStopped)
    }

    /**
     * Leaves an active break before it completes. The break is intentionally not
     * recorded, and the next work session is armed according to [AppSettings.autoStartNextWork]:
     * running right away with the screen on, or paused-with-grace when the screen is off.
     */
    @Synchronized
    fun skipBreak(): List<TimerEvent> {
        val now = clock.realtimeMillis()
        val wall = clock.wallTimeMillis()
        val current = _state.value
        if (current.phase != TimerPhase.BREAK || !current.isActive) return emptyList()

        val settings = settingsProvider()
        val workDuration = settings.workMillis
        val events = mutableListOf<TimerEvent>(TimerEvent.BreakSkipped)

        if (!settings.autoStartNextWork) {
            _state.value = TimerState.idle(workDuration)
            return events
        }

        if (!screenOnProvider()) {
            _state.value = TimerState(
                phase = TimerPhase.WORK,
                isActive = true,
                isRunning = false,
                remainingMillis = workDuration,
                totalMillis = workDuration,
                anchorRealtimeMillis = now,
                sessionStartedAtMillis = wall,
                pauseReason = PauseReason.SCREEN_OFF,
                screenOffAtRealtimeMillis = now,
                screenOffAtWallMillis = wall,
            )
            events += TimerEvent.WorkStartedAfterBreak
            events += TimerEvent.WorkPausedForScreenOff(workDuration)
            return events
        }

        _state.value = TimerState(
            phase = TimerPhase.WORK,
            isActive = true,
            isRunning = true,
            remainingMillis = workDuration,
            totalMillis = workDuration,
            anchorRealtimeMillis = now,
            sessionStartedAtMillis = wall,
        )
        events += TimerEvent.WorkStartedAfterBreak
        return events
    }

    /**
     * Called frequently by the application-scoped ticker. It only changes state when a
     * phase actually reaches zero, so there is no per-second write churn.
     */
    @Synchronized
    fun tick(): List<TimerEvent> {
        val now = clock.realtimeMillis()
        val wall = clock.wallTimeMillis()
        val current = _state.value
        if (!current.isRunning) return emptyList()
        if (current.remainingAt(now) > 0L) return emptyList()

        val completionRealtime = current.anchorRealtimeMillis + current.remainingMillis
        val overshoot = (now - completionRealtime).coerceAtLeast(0L)
        val completionWall = wall - overshoot

        return if (current.phase == TimerPhase.WORK) {
            startBreakAfterWork(now, wall, completionRealtime, completionWall, overshoot)
        } else {
            finishBreak(now, wall, completionRealtime, completionWall, overshoot)
        }
    }

    private fun startBreakAfterWork(
        now: Long,
        wall: Long,
        completionRealtime: Long,
        completionWall: Long,
        overshoot: Long,
    ): List<TimerEvent> {
        val breakDuration = settingsProvider().breakMillis
        val startsAt = if (overshoot <= LATE_TICK_TOLERANCE_MS) completionRealtime else now
        val startsAtWall = if (overshoot <= LATE_TICK_TOLERANCE_MS) completionWall else wall

        _state.value = TimerState(
            phase = TimerPhase.BREAK,
            isActive = true,
            isRunning = true,
            remainingMillis = breakDuration,
            totalMillis = breakDuration,
            anchorRealtimeMillis = startsAt,
            sessionStartedAtMillis = startsAtWall,
        )
        return listOf(
            TimerEvent.WorkCompleted(completionWall),
            TimerEvent.BreakStarted(startsAtWall, breakDuration),
        )
    }

    private fun finishBreak(
        now: Long,
        wall: Long,
        completionRealtime: Long,
        completionWall: Long,
        overshoot: Long,
    ): List<TimerEvent> {
        val current = _state.value
        val settings = settingsProvider()
        val events = mutableListOf<TimerEvent>(
            TimerEvent.BreakCompleted(
                sessionStartedAtMillis = current.sessionStartedAtMillis,
                completedAtMillis = completionWall,
                durationSeconds = (current.totalMillis / 1_000L).toInt(),
            ),
        )

        val workDuration = settings.workMillis
        if (!settings.autoStartNextWork) {
            _state.value = TimerState.idle(workDuration)
            return events
        }

        if (!screenOnProvider()) {
            // The break ended while the screen was off. Arm the work session but do not
            // count any of its time; grace is measured from the actual break end.
            _state.value = TimerState(
                phase = TimerPhase.WORK,
                isActive = true,
                isRunning = false,
                remainingMillis = workDuration,
                totalMillis = workDuration,
                anchorRealtimeMillis = now,
                sessionStartedAtMillis = completionWall,
                pauseReason = PauseReason.SCREEN_OFF,
                screenOffAtRealtimeMillis = completionRealtime,
                screenOffAtWallMillis = completionWall,
            )
            events += TimerEvent.WorkStartedAfterBreak
            events += TimerEvent.WorkPausedForScreenOff(workDuration)
            return events
        }

        val late = overshoot > LATE_TICK_TOLERANCE_MS
        _state.value = TimerState(
            phase = TimerPhase.WORK,
            isActive = true,
            isRunning = true,
            remainingMillis = workDuration,
            totalMillis = workDuration,
            anchorRealtimeMillis = if (late) now else completionRealtime,
            sessionStartedAtMillis = if (late) wall else completionWall,
        )
        events += TimerEvent.WorkStartedAfterBreak
        return events
    }

    private fun pauseForScreenOff(nowRealtime: Long, nowWall: Long): TimerEvent {
        val current = _state.value
        val remaining = current.remainingAt(nowRealtime)
        _state.value = current.copy(
            isRunning = false,
            remainingMillis = remaining,
            anchorRealtimeMillis = nowRealtime,
            pauseReason = PauseReason.SCREEN_OFF,
            screenOffAtRealtimeMillis = nowRealtime,
            screenOffAtWallMillis = nowWall,
        )
        return TimerEvent.WorkPausedForScreenOff(remaining)
    }

    private companion object {
        const val LATE_TICK_TOLERANCE_MS = 5_000L
    }
}
