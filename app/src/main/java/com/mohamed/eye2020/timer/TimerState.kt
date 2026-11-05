package com.mohamed.eye2020.timer

enum class TimerPhase { WORK, BREAK }

enum class PauseReason { USER, SCREEN_OFF, SCREEN_OFF_RESET }

/**
 * The single source of truth for the countdown. While running, [remainingMillis] is the
 * value at [anchorRealtimeMillis]; every observer derives the current value from the
 * monotonic clock. This avoids drift from repeated one-second decrements.
 */
data class TimerState(
    val phase: TimerPhase = TimerPhase.WORK,
    val isActive: Boolean = false,
    val isRunning: Boolean = false,
    val remainingMillis: Long = 20 * 60_000L,
    val totalMillis: Long = 20 * 60_000L,
    val anchorRealtimeMillis: Long = 0L,
    val sessionStartedAtMillis: Long = 0L,
    val pauseReason: PauseReason? = null,
    val screenOffAtRealtimeMillis: Long? = null,
    val screenOffAtWallMillis: Long? = null,
) {
    fun remainingAt(nowRealtimeMillis: Long): Long {
        if (!isRunning) return remainingMillis.coerceAtLeast(0L)
        val elapsed = (nowRealtimeMillis - anchorRealtimeMillis).coerceAtLeast(0L)
        return (remainingMillis - elapsed).coerceAtLeast(0L)
    }

    fun progressAt(nowRealtimeMillis: Long): Float {
        if (totalMillis <= 0L) return 0f
        val elapsed = (totalMillis - remainingAt(nowRealtimeMillis)).coerceIn(0L, totalMillis)
        return elapsed.toFloat() / totalMillis.toFloat()
    }

    companion object {
        fun idle(workMillis: Long): TimerState = TimerState(
            phase = TimerPhase.WORK,
            isActive = false,
            isRunning = false,
            remainingMillis = workMillis,
            totalMillis = workMillis,
        )
    }
}

sealed interface TimerEvent {
    data object WorkStarted : TimerEvent
    data object WorkResumed : TimerEvent
    data object WorkPaused : TimerEvent
    data class WorkCompleted(val completedAtMillis: Long) : TimerEvent
    data class BreakStarted(
        val sessionStartedAtMillis: Long,
        val durationMillis: Long,
    ) : TimerEvent

    data class BreakCompleted(
        val sessionStartedAtMillis: Long,
        val completedAtMillis: Long,
        val durationSeconds: Int,
    ) : TimerEvent

    data class WorkPausedForScreenOff(val remainingMillis: Long) : TimerEvent
    data class WorkResumedAfterScreenOff(val remainingMillis: Long) : TimerEvent
    data class WorkResetAfterScreenOff(
        val fullDurationMillis: Long,
        val screenOffDurationMillis: Long,
    ) : TimerEvent

    data object WorkStartedAfterBreak : TimerEvent
}
