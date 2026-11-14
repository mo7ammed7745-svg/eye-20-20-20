package com.mohamed.eye2020.game

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.mohamed.eye2020.data.settings.AppSettings

/**
 * The pure decision part of game mode, kept free of Android so it is unit-testable:
 * game mode repels the full break screen only when the user listed at least one app,
 * and the app currently in the foreground is one of them.
 */
object GameModePolicy {

    fun shouldUseQuietBreak(
        gameModeApps: List<String>,
        foregroundPackage: String?,
    ): Boolean {
        if (gameModeApps.isEmpty()) return false
        if (foregroundPackage == null) return false
        return foregroundPackage in gameModeApps
    }
}

/**
 * The Android half of the decision. Android only reveals the foreground app to holders
 * of the special "Usage access" permission, so every call checks that grant first and
 * the caller must treat a missing permission as "game mode inactive".
 */
class GameModeController(private val context: Context) {

    fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * The package that most recently resumed an activity within the last minute.
     * Runs on the tick thread at most once per break start, so the linear walk over the
     * event list is cheap enough.
     */
    fun currentForegroundPackage(): String? {
        if (!isUsageAccessGranted()) return null
        val stats = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val events = stats.queryEvents(now - LOOKBACK_MILLIS, now) ?: return null
        var latest: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            if (events.getNextEvent(event) &&
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                latest = event.packageName
            }
        }
        // The timer's own activity counts as a normal rest screen; never go quiet there.
        if (latest == context.packageName) return null
        return latest
    }

    /** The launchable apps the game-mode picker can offer, sorted by label. */
    fun launchableApps(): List<GameAppEntry> {
        val launcherIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            context.packageManager.queryIntentActivities(launcherIntent, 0)
        }.getOrDefault(emptyList())
        return resolved.asSequence()
            .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
            .mapNotNull { entry ->
                val label = runCatching { entry.loadLabel(context.packageManager) }
                    .getOrNull()?.toString() ?: return@mapNotNull null
                GameAppEntry(
                    packageName = entry.activityInfo.packageName,
                    label = label,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun shouldUseQuietBreak(settings: AppSettings): Boolean =
        GameModePolicy.shouldUseQuietBreak(
            gameModeApps = settings.gameModeApps,
            foregroundPackage = currentForegroundPackage(),
        )

    /**
     * Friendly display name of an installed app ("Candy Crush" instead of
     * com.king.candycrushsaga). Falls back to the package name if the app was
     * uninstalled since it was added, so the list never shows blanks.
     */
    fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
}

/** One row of the game-mode app picker. */
data class GameAppEntry(
    val packageName: String,
    val label: String,
)

private const val LOOKBACK_MILLIS = 60_000L
