package com.mohamed.eye2020.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mohamed.eye2020.BreakActivity
import com.mohamed.eye2020.EyeApplication
import com.mohamed.eye2020.timer.TimerManager
import com.mohamed.eye2020.timer.TimerPhase
import com.mohamed.eye2020.timer.TimerState
import com.mohamed.eye2020.util.SystemSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hosts the foreground notification and screen-state listener. The countdown itself lives
 * in TimerManager; this service keeps the process alive and visible while a session needs
 * it, and stops as soon as nothing is running or waiting for screen-on.
 */
class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var manager: TimerManager
    private lateinit var notifications: NotificationHelper
    private var receiverRegistered = false
    private var lastNotificationKey: String? = null
    // The observer must not stop the service before onStartCommand has handled the
    // command that created it (for example a notification Resume action).
    private var commandReceived = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> manager.onScreenOff()
                Intent.ACTION_SCREEN_ON -> manager.onScreenOn()
            }
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val container = (application as EyeApplication).container
        manager = container.timerManager
        notifications = container.notificationHelper
        notifications.createChannels()
        registerScreenReceiver()
        manager.reconcileScreenState()
        startInForeground()
        startObserving()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        commandReceived = true
        when (intent?.action) {
            ACTION_PAUSE -> manager.pauseWork()
            ACTION_RESUME -> manager.startOrResumeWork()
        }

        // A pause command, or a restored state that no longer needs a foreground
        // service, should remove the notification immediately.
        if (!manager.needsForegroundService()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startInForeground()
        updateNotification()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        manager.persistNow()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        notifications.cancelBreakAlert()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        manager.persistNow()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startObserving() {
        serviceScope.launch {
            var previousInBreak = false
            while (isActive) {
                val state = manager.state.value
                val inBreak = state.phase == TimerPhase.BREAK && state.isActive

                if (inBreak && !previousInBreak) {
                    if (!BreakActivity.isOpen) {
                        notifications.postBreakAlert(state)
                        openBreakScreen()
                    }
                } else if (!inBreak && previousInBreak) {
                    notifications.cancelBreakAlert()
                }
                previousInBreak = inBreak

                updateNotification(state)
                if (commandReceived && !manager.needsForegroundService()) {
                    notifications.cancelBreakAlert()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                delay(500L)
            }
        }
    }

    private fun startInForeground() {
        val state = manager.state.value
        val remaining = state.remainingAt(SystemClock.elapsedRealtime())
        val notification = notifications.buildTimerNotification(state, remaining)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID_TIMER,
            notification,
            type,
        )
        lastNotificationKey = notificationKey(state, remaining)
    }

    private fun updateNotification(state: TimerState = manager.state.value) {
        val remaining = state.remainingAt(SystemClock.elapsedRealtime())
        val key = notificationKey(state, remaining)
        if (key == lastNotificationKey) return
        lastNotificationKey = key
        runCatching { notifications.updateTimerNotification(state, remaining) }
    }

    private fun notificationKey(state: TimerState, remaining: Long): String =
        "${state.phase}:${state.isActive}:${state.isRunning}:${state.pauseReason}:" +
            "${(remaining + 999L) / 1_000L}"

    private fun openBreakScreen() {
        if (BreakActivity.isOpen) return
        if (!SystemSettings.canDrawOverlays(this)) {
            // Without this normal permission Android may reject the background start;
            // the full-screen-intent notification remains the fallback.
            Log.w(TAG, "Overlay permission missing; break screen may need a tap")
        }
        val intent = Intent(this, BreakActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        runCatching { startActivity(intent) }
            .onFailure { Log.w(TAG, "Unable to open break screen", it) }
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    companion object {
        private const val TAG = "TimerService"
        const val ACTION_PAUSE = "com.mohamed.eye2020.action.PAUSE"
        const val ACTION_RESUME = "com.mohamed.eye2020.action.RESUME"
    }
}
