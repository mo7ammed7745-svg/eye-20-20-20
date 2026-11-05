package com.mohamed.eye2020.util

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

/**
 * A point-in-time view of the Android settings that affect background reliability.
 * Android does not notify the app when most of these change, so callers re-read this
 * snapshot when they return to the foreground.
 */
data class PermissionStatus(
    val notificationsGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val overlayGranted: Boolean,
    val fullScreenIntentSupported: Boolean,
    val fullScreenIntentGranted: Boolean,
) {
    companion object {
        fun read(context: Context): PermissionStatus {
            val powerManager = context.getSystemService(PowerManager::class.java)
            val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName)
                ?: true

            val fullScreenSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            val fullScreenGranted = if (fullScreenSupported) {
                context.getSystemService(NotificationManager::class.java)
                    ?.canUseFullScreenIntent() ?: false
            } else {
                true
            }

            return PermissionStatus(
                notificationsGranted = NotificationManagerCompat.from(context)
                    .areNotificationsEnabled(),
                batteryOptimizationIgnored = batteryIgnored,
                overlayGranted = SystemSettings.canDrawOverlays(context),
                fullScreenIntentSupported = fullScreenSupported,
                fullScreenIntentGranted = fullScreenGranted,
            )
        }
    }
}
