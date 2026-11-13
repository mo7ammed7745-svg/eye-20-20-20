package com.mohamed.eye2020.timer

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.mohamed.eye2020.audio.AnnouncementPlayer
import com.mohamed.eye2020.data.BreakRepository
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.data.settings.SettingsRepository
import com.mohamed.eye2020.game.GameModeController
import com.mohamed.eye2020.service.TimerService
import com.mohamed.eye2020.util.Haptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Application-scoped owner of the one and only timer. UI and service both read from it,
 * so navigation, activity recreation and notification actions can never spawn a second
 * countdown.
 */
class TimerManager(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val breakRepository: BreakRepository,
    private val announcementPlayer: AnnouncementPlayer,
    private val gameModeController: GameModeController,
    private val appScope: CoroutineScope,
    private val clock: Clock = SystemClockSource,
    private val snapshotStore: TimerSnapshotStore = TimerSnapshotStore(context),
) {
    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(PowerManager::class.java)

    @Volatile
    private var latestSettings: AppSettings = runBlocking {
        runCatching { settingsRepository.current() }.getOrDefault(AppSettings())
    }

    private val initialSnapshot: TimerSnapshot? = snapshotStore.load()
    private val initialState: TimerState? = TimerRecovery.restore(
        snapshot = initialSnapshot,
        settings = latestSettings,
        nowRealtime = clock.realtimeMillis(),
        nowWall = clock.wallTimeMillis(),
        screenInteractive = powerManager?.isInteractive ?: true,
    )
    private val engine = TimerEngine(
        clock = clock,
        settingsProvider = { latestSettings },
        screenOnProvider = { powerManager?.isInteractive ?: true },
        initialState = initialState,
    )

    val state: StateFlow<TimerState> = engine.state

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    private var lastPersistedAt = 0L

    @Volatile
    private var pendingRecordJob: Job? = null

    /**
     * Set when a break starts while a whitelisted game is in the foreground: the break
     * still runs and records, but the service suppresses the break screen and posts only
     * a small heads-up notification. Cleared on every non-quiet break so a leftover flag
     * can never leak into the next break.
     */
    @Volatile
    private var quietBreakActive = false

    /** Whether the currently running break may show only the quiet game-mode popup. */
    fun isQuietBreakActive(): Boolean = quietBreakActive

    init {
        appScope.launch {
            // applySettings is guarded upstream plus here: a preference-stream error
            // must not kill the coroutine that owns the only timer.
            settingsRepository.settings
                .catch { error -> Log.w(TAG, "Stopped reading settings", error) }
                .collect { settings ->
                    latestSettings = settings
                    engine.applySettings(settings)
                }
        }

        // Recover a countdown that reached zero while the process was gone, then persist
        // the corrected state immediately. ensureServiceRunning brings back the foreground
        // notification after Android or the maker recreated the process out of band —
        // without it a restored running timer would have no notification or actions.
        appScope.launch {
            if (powerManager?.isInteractive != false) {
                dispatchEvents(engine.onScreenOn())
            }
            dispatchEvents(engine.tick())
            persistNow()
            ensureServiceRunning()
        }

        appScope.launch {
            engine.state.drop(1).collect { persistNow() }
        }

        startTicker()
    }

    fun startOrResumeWork() {
        dispatchEvents(engine.startOrResumeWork())
        ensureServiceRunning()
    }

    fun pauseWork() {
        dispatchEvents(engine.pauseWork())
    }

    /** Ends the current work session and returns to the ready idle timer. */
    fun stopWork() {
        dispatchEvents(engine.reset())
    }

    /** Leaves an active break early without recording it. */
    fun skipBreak() {
        dispatchEvents(engine.skipBreak())
        ensureServiceRunning()
    }

    fun onScreenOff() {
        dispatchEvents(engine.onScreenOff())
    }

    fun onScreenOn() {
        dispatchEvents(engine.onScreenOn())
    }

    /**
     * Covers a missed screen broadcast (for example when the service starts after the
     * display was already turned back on).
     */
    fun reconcileScreenState() {
        val interactive = powerManager?.isInteractive ?: true
        if (interactive) {
            dispatchEvents(engine.onScreenOn())
        } else {
            dispatchEvents(engine.onScreenOff())
        }
    }

    fun isInBreak(): Boolean {
        val current = engine.state.value
        return current.phase == TimerPhase.BREAK && current.isActive
    }

    fun needsForegroundService(): Boolean {
        val current = engine.state.value
        if (!current.isActive) return false
        // A user-paused session keeps its foreground notification so Pause/Resume/Stop
        // stay reachable from the shade. A screen-off pause already does the same.
        return current.isRunning ||
            current.phase == TimerPhase.WORK && (
                current.pauseReason == PauseReason.USER ||
                    current.pauseReason == PauseReason.SCREEN_OFF
                )
    }

    fun ensureServiceRunning() {
        if (!needsForegroundService()) return
        try {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, TimerService::class.java),
            )
        } catch (error: Exception) {
            // Background-start restrictions can reject this in unusual states. The timer
            // still advances in-process and the next user action starts the service.
            Log.w(TAG, "Unable to start timer service", error)
        }
    }

    @Synchronized
    fun persistNow() {
        // A completed-break insert must reach Room before the post-break snapshot
        // replaces the break state; otherwise a crash in between silently loses it.
        pendingRecordJob?.takeIf { it.isActive }?.let { job ->
            runCatching {
                runBlocking { job.join() }
            }
        }
        pendingRecordJob = null
        val now = clock.realtimeMillis()
        snapshotStore.save(engine.state.value, now, clock.wallTimeMillis())
        lastPersistedAt = now
    }

    private fun startTicker() {
        appScope.launch(Dispatchers.Default) {
            while (isActive) {
                val running = engine.state.value.isRunning
                if (running) {
                    dispatchEvents(engine.tick())
                    persistIfDue()
                }
                delay(if (running) TICK_INTERVAL_MS else IDLE_INTERVAL_MS)
            }
        }
    }

    @Synchronized
    private fun persistIfDue() {
        val now = clock.realtimeMillis()
        if (now - lastPersistedAt >= PERSIST_INTERVAL_MS) {
            snapshotStore.save(engine.state.value, now, clock.wallTimeMillis())
            lastPersistedAt = now
        }
    }

    private fun dispatchEvents(events: List<TimerEvent>) {
        if (events.isEmpty()) return
        for (event in events) {
            when (event) {
                is TimerEvent.BreakStarted -> {
                    quietBreakActive = runCatching {
                        // Usage access is the only permission that can be missing here;
                        // any failure must fall back to the normal break screen.
                        gameModeController.shouldUseQuietBreak(latestSettings)
                    }.getOrDefault(false)
                    if (latestSettings.vibrateOnBreakStart) {
                        Haptics.breakStarted(appContext)
                    }
                    if (latestSettings.voiceAnnouncements) {
                        announcementPlayer.playBreakStart(latestSettings.announcementVolume)
                    }
                }

                is TimerEvent.BreakCompleted -> {
                    quietBreakActive = false
                    if (latestSettings.vibrateOnBreakEnd) {
                        Haptics.breakEnded(appContext)
                    }
                    if (latestSettings.voiceAnnouncements) {
                        announcementPlayer.playWorkStart(latestSettings.announcementVolume)
                    }
                    pendingRecordJob = appScope.launch {
                        runCatching {
                            breakRepository.recordCompletedBreak(
                                sessionStartedAtMillis = event.sessionStartedAtMillis,
                                completedAtMillis = event.completedAtMillis,
                                durationSeconds = event.durationSeconds,
                            )
                        }.onFailure { error ->
                            // A full disk or busy database must never crash the process
                            // that holds the only timer.
                            Log.w(TAG, "Unable to record completed break", error)
                        }
                    }
                }

                else -> Unit
            }
            _events.tryEmit(event)
            if (event is TimerEvent.BreakSkipped ||
                event is TimerEvent.WorkStopped ||
                event is TimerEvent.WorkResetAfterScreenOff
            ) {
                quietBreakActive = false
            }
        }
        persistNow()
    }

    private companion object {
        const val TAG = "TimerManager"
        const val TICK_INTERVAL_MS = 250L
        const val IDLE_INTERVAL_MS = 750L
        const val PERSIST_INTERVAL_MS = 5_000L
    }
}
