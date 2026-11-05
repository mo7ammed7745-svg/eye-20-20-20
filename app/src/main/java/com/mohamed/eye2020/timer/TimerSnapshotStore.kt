package com.mohamed.eye2020.timer

import android.content.Context

data class TimerSnapshot(
    val state: TimerState,
    val savedAtRealtimeMillis: Long,
    val savedAtWallMillis: Long,
)

/**
 * Small, fast-lived snapshot used only to recover the live timer after process death.
 * User preferences live in DataStore; completed breaks live in Room. This is neither.
 */
class TimerSnapshotStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun save(state: TimerState, realtimeMillis: Long, wallMillis: Long) {
        preferences.edit()
            .putInt(KEY_VERSION, VERSION)
            .putString(KEY_PHASE, state.phase.name)
            .putBoolean(KEY_ACTIVE, state.isActive)
            .putBoolean(KEY_RUNNING, state.isRunning)
            .putLong(KEY_REMAINING, state.remainingMillis)
            .putLong(KEY_TOTAL, state.totalMillis)
            .putLong(KEY_ANCHOR_REALTIME, state.anchorRealtimeMillis)
            .putLong(KEY_SESSION_STARTED, state.sessionStartedAtMillis)
            .putString(KEY_PAUSE_REASON, state.pauseReason?.name ?: "")
            .putLong(KEY_SCREEN_OFF_REALTIME, state.screenOffAtRealtimeMillis ?: -1L)
            .putLong(KEY_SCREEN_OFF_WALL, state.screenOffAtWallMillis ?: -1L)
            .putLong(KEY_SAVED_REALTIME, realtimeMillis)
            .putLong(KEY_SAVED_WALL, wallMillis)
            // This snapshot is the crash-recovery record. Commit synchronously so a
            // process kill cannot discard the write before it reaches disk.
            .commit()
    }

    fun load(): TimerSnapshot? {
        if (preferences.getInt(KEY_VERSION, -1) != VERSION) return null
        val phase = runCatching {
            TimerPhase.valueOf(preferences.getString(KEY_PHASE, "") ?: "")
        }.getOrNull() ?: return null
        val remaining = preferences.getLong(KEY_REMAINING, -1L)
        val total = preferences.getLong(KEY_TOTAL, -1L)
        val anchor = preferences.getLong(KEY_ANCHOR_REALTIME, -1L)
        if (remaining < 0L || total <= 0L || anchor < 0L) return null

        val pauseReason = runCatching {
            PauseReason.valueOf(preferences.getString(KEY_PAUSE_REASON, "") ?: "")
        }.getOrNull()
        val screenOffRealtime = preferences.getLong(KEY_SCREEN_OFF_REALTIME, -1L)
            .takeIf { it >= 0L }
        val screenOffWall = preferences.getLong(KEY_SCREEN_OFF_WALL, -1L)
            .takeIf { it >= 0L }

        val state = TimerState(
            phase = phase,
            isActive = preferences.getBoolean(KEY_ACTIVE, false),
            isRunning = preferences.getBoolean(KEY_RUNNING, false),
            remainingMillis = remaining,
            totalMillis = total,
            anchorRealtimeMillis = anchor,
            sessionStartedAtMillis = preferences.getLong(KEY_SESSION_STARTED, 0L),
            pauseReason = pauseReason,
            screenOffAtRealtimeMillis = screenOffRealtime,
            screenOffAtWallMillis = screenOffWall,
        )
        return TimerSnapshot(
            state = state,
            savedAtRealtimeMillis = preferences.getLong(KEY_SAVED_REALTIME, -1L),
            savedAtWallMillis = preferences.getLong(KEY_SAVED_WALL, -1L),
        )
    }

    private companion object {
        const val PREFS_NAME = "eye_timer_snapshot"
        const val VERSION = 1
        const val KEY_VERSION = "version"
        const val KEY_PHASE = "phase"
        const val KEY_ACTIVE = "active"
        const val KEY_RUNNING = "running"
        const val KEY_REMAINING = "remaining"
        const val KEY_TOTAL = "total"
        const val KEY_ANCHOR_REALTIME = "anchor_realtime"
        const val KEY_SESSION_STARTED = "session_started"
        const val KEY_PAUSE_REASON = "pause_reason"
        const val KEY_SCREEN_OFF_REALTIME = "screen_off_realtime"
        const val KEY_SCREEN_OFF_WALL = "screen_off_wall"
        const val KEY_SAVED_REALTIME = "saved_realtime"
        const val KEY_SAVED_WALL = "saved_wall"
    }
}
