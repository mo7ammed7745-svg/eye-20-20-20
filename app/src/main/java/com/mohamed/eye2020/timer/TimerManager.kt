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
import com.mohamed.eye2020.service.TimerService
import com.mohamed.eye2020.util.Haptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    init {
        appScope.launch {
            settingsRepository.settings.collect { settings ->
                latestSettings = settings
                engine.applySettings(settings)
            }
        }

        // Recover a countdown that reached zero while the process was gone, then persist
        // the corrected state immediately.
        appScope.launch {
            if (powerManager?.isInteractive != false) {
                dispatchEvents(engine.onScreenOn())
            }
            dispatchEvents(engine.tick())
            persistNow()
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
        return current.isRunning ||
            (current.phase == TimerPhase.WORK &&
                current.isActive &&
                current.pauseReason == PauseReason.SCREEN_OFF)
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
                    if (latestSettings.vibrateOnBreakStart) {
                        Haptics.breakStarted(appContext)
                    }
                    if (latestSettings.voiceAnnouncements) {
                        announcementPlayer.playBreakStart(latestSettings.announcementVolume)
                    }
                }

                is TimerEvent.BreakCompleted -> {
                    if (latestSettings.vibrateOnBreakEnd) {
                        Haptics.breakEnded(appContext)
                    }
                    if (latestSettings.voiceAnnouncements) {
                        announcementPlayer.playWorkStart(latestSettings.announcementVolume)
                    }
                    appScope.launch {
                        breakRepository.recordCompletedBreak(
                            sessionStartedAtMillis = event.sessionStartedAtMillis,
                            completedAtMillis = event.completedAtMillis,
                            durationSeconds = event.durationSeconds,
                        )
                    }
                }

                else -> Unit
            }
            _events.tryEmit(event)
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
