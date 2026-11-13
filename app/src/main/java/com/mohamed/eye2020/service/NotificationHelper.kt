package com.mohamed.eye2020.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mohamed.eye2020.BreakActivity
import com.mohamed.eye2020.MainActivity
import com.mohamed.eye2020.R
import com.mohamed.eye2020.timer.PauseReason
import com.mohamed.eye2020.timer.TimerPhase
import com.mohamed.eye2020.timer.TimerState
import com.mohamed.eye2020.util.TimeFormat

class NotificationHelper(private val context: Context) {

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.notif_channel_timer),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notif_channel_timer_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val breakChannel = NotificationChannel(
            CHANNEL_BREAK,
            context.getString(R.string.notif_channel_break),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_break_desc)
            setShowBadge(true)
            setSound(
                null,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build(),
            )
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannels(listOf(timerChannel, breakChannel))
    }

    fun buildTimerNotification(
        state: TimerState,
        remainingMillis: Long,
    ): Notification {
        val isIdle = !state.isActive
        val isBreak = state.phase == TimerPhase.BREAK
        val isScreenOffPause = state.pauseReason == PauseReason.SCREEN_OFF
        val time = TimeFormat.clock(remainingMillis)

        val title = when {
            isIdle -> context.getString(R.string.app_name)
            isBreak -> context.getString(R.string.status_break)
            isScreenOffPause -> context.getString(R.string.notif_paused_screen_off)
            state.isRunning -> context.getString(R.string.status_working)
            else -> context.getString(R.string.status_paused)
        }
        val text = when {
            isIdle -> context.getString(R.string.notif_timer_ready)
            isBreak -> context.getString(R.string.notif_break, time)
            isScreenOffPause -> context.getString(R.string.notif_paused, time)
            state.isRunning -> context.getString(R.string.notif_working, time)
            else -> context.getString(R.string.notif_paused, time)
        }

        val progress = if (state.totalMillis > 0L) {
            (1_000L - (remainingMillis.coerceIn(0L, state.totalMillis) * 1_000L / state.totalMillis))
                .toInt()
        } else {
            0
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(
                if (isBreak) breakPendingIntent() else mainPendingIntent(),
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(1_000, progress.coerceIn(0, 1_000), false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (!isBreak && state.isActive) {
            if (state.pauseReason == PauseReason.USER) {
                builder.addAction(
                    R.drawable.ic_play,
                    context.getString(R.string.notif_action_resume),
                    commandPendingIntent(TimerService.ACTION_RESUME, REQUEST_RESUME),
                )
                builder.addAction(
                    R.drawable.ic_stop,
                    context.getString(R.string.notif_action_stop),
                    commandPendingIntent(TimerService.ACTION_STOP, REQUEST_STOP),
                )
            } else if (state.isRunning) {
                builder.addAction(
                    R.drawable.ic_pause,
                    context.getString(R.string.notif_action_pause),
                    commandPendingIntent(TimerService.ACTION_PAUSE, REQUEST_PAUSE),
                )
            }
        }

        return builder.build()
    }

    /**
     * Updates the ongoing notification only when Android lets this app post one. The
     * foreground-service notification itself is managed by startForeground(); this
     * path is the periodic countdown refresh.
     */
    fun updateTimerNotification(state: TimerState, remainingMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_TIMER,
            buildTimerNotification(state, remainingMillis),
        )
    }

    fun buildBreakAlertNotification(state: TimerState): Notification {
        val breakIntent = breakPendingIntent()
        val seconds = (state.totalMillis / 1_000L).toInt()
        val duration = context.resources.getQuantityString(
            R.plurals.label_seconds,
            seconds,
            seconds,
        )
        return NotificationCompat.Builder(context, CHANNEL_BREAK)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle(context.getString(R.string.notif_alert_title))
            .setContentText(context.getString(R.string.notif_alert_text, duration))
            .setContentIntent(breakIntent)
            .setFullScreenIntent(breakIntent, true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun postBreakAlert(state: TimerState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BREAK_ALERT, buildBreakAlertNotification(state))
        }
    }

    fun cancelBreakAlert() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BREAK_ALERT)
        }
    }

    /**
     * Game-mode popup: a plain heads-up banner without the full-screen intent, so the
     * running game is never covered by the break screen. Tapping it opens the app
     * instead of the break screen. The banner slides down from the top, is removed
     * again after a couple of seconds, and the rest of the countdown runs quietly.
     */
    fun buildQuietBreakNotification(state: TimerState): Notification {
        val seconds = (state.totalMillis / 1_000L).toInt()
        val duration = context.resources.getQuantityString(
            R.plurals.label_seconds,
            seconds,
            seconds,
        )
        return NotificationCompat.Builder(context, CHANNEL_BREAK)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle(context.getString(R.string.notif_quiet_title))
            .setContentText(context.getString(R.string.notif_quiet_text, duration))
            .setContentIntent(mainPendingIntent())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Posts the quiet game-mode banner and schedules its removal about two seconds
     * later, so it behaves like a brief slide-down popup instead of a lingering
     * notification while the game keeps running.
     */
    fun postQuietBreakAlert(state: TimerState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val posted = runCatching {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BREAK_ALERT, buildQuietBreakNotification(state))
        }.isSuccess
        if (posted) {
            // Drop any stale removal first so a fresh banner can never be cut short.
            quietAlertHandler.removeCallbacks(removeQuietAlert)
            quietAlertHandler.postDelayed(removeQuietAlert, QUIET_ALERT_VISIBLE_MILLIS)
        }
    }

    private val quietAlertHandler = Handler(Looper.getMainLooper())
    private val removeQuietAlert = Runnable {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BREAK_ALERT)
        }
    }

    private fun mainPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_MAIN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun breakPendingIntent(): PendingIntent {
        val intent = Intent(context, BreakActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_BREAK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun commandPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_TIMER = "active_timer"
        const val CHANNEL_BREAK = "break_alerts"
        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_BREAK_ALERT = 1002

        /** How long the quiet game-mode banner stays visible before it slides away. */
        const val QUIET_ALERT_VISIBLE_MILLIS = 2_000L

        private const val REQUEST_OPEN_MAIN = 10
        private const val REQUEST_OPEN_BREAK = 11
        private const val REQUEST_PAUSE = 12
        private const val REQUEST_RESUME = 13
        private const val REQUEST_STOP = 14
    }
}
