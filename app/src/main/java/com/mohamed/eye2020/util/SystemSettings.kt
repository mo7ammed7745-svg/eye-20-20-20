package com.mohamed.eye2020.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.app.AppOpsManager
import android.provider.Settings

object SystemSettings {

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        start(context, intent) { openAppDetails(context) }
    }

    fun openAppDetails(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        start(context, intent) {}
    }

    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.parse("package:${context.packageName}"),
        )
        start(context, intent) { openAppDetails(context) }
    }

    /**
     * Opens the system battery-optimization list. Android has no public per-app page for
     * this setting; the user selects Eye 20-20-20 there and chooses "Unrestricted" (the
     * wording varies by OEM). No restricted battery-optimization permission is used.
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        start(context, intent) { openAppDetails(context) }
    }

    /**
     * "Display over other apps" is the normal Android permission that lets a background
     * app open the break screen over whatever the user is doing. It is not accessibility
     * automation and grants no control over other apps.
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        start(context, intent) { openAppDetails(context) }
    }

    fun canDrawOverlays(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /**
     * "Usage access" is a user-granted Android setting, not an accessibility service.
     * Game mode reads it to know which app is currently in the foreground so it can
     * stay quiet during a break; nothing else is read from the usage data.
     */
    fun openUsageAccessSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        start(context, intent) { openAppDetails(context) }
    }

    fun canSeeUsageApps(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun start(context: Context, intent: Intent, fallback: () -> Unit) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { fallback() }
    }
}
