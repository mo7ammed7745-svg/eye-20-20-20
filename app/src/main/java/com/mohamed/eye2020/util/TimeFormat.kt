package com.mohamed.eye2020.util

import java.util.Locale

object TimeFormat {

    /** 20:00, 1:05, or 1:02:03. Always tabular and English. */
    fun clock(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L) + 999L) / 1000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun minutesLabel(minutes: Int): String = if (minutes == 1) "1 minute" else "$minutes minutes"

    fun secondsLabel(seconds: Int): String = if (seconds == 1) "1 second" else "$seconds seconds"
}
