package com.mohamed.eye2020.data.settings

import android.content.Context

/**
 * Remembers that the one-time permission setup screen has been completed. This is kept
 * separately from user preferences because it is a first-run marker, not a setting.
 */
class SetupPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun isCompleted(): Boolean = preferences.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        // commit() rather than apply(): the marker must survive an immediate process
        // death so the setup screen is never shown again after the user finishes it.
        preferences.edit().putBoolean(KEY_COMPLETED, true).commit()
    }

    private companion object {
        const val PREFS_NAME = "eye_setup"
        const val KEY_COMPLETED = "completed"
    }
}
