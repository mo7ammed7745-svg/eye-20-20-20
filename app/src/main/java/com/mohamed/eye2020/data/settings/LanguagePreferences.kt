package com.mohamed.eye2020.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app language lives in SharedPreferences instead of DataStore on purpose: it has to
 * be readable synchronously in Activity.attachBaseContext, before the first frame, so the
 * screen never flashes the wrong language.
 */
class LanguagePreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val state = MutableStateFlow(read(preferences))

    val language: StateFlow<AppLanguage> = state.asStateFlow()

    /** False until the user has answered the first-launch language question. */
    val hasChosen: Boolean get() = preferences.contains(KEY_LANGUAGE)

    fun set(language: AppLanguage) {
        // commit() rather than apply(): the activity is recreated right after this call
        // and must read the new value back synchronously.
        preferences.edit().putString(KEY_LANGUAGE, language.tag).commit()
        state.value = language
    }

    companion object {
        const val PREFS_NAME = "eye_language"
        private const val KEY_LANGUAGE = "language"

        /** Reads the stored language without creating a long-lived instance. */
        fun storedLanguage(context: Context): AppLanguage = read(
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        )

        private fun read(preferences: SharedPreferences): AppLanguage =
            AppLanguage.fromTag(preferences.getString(KEY_LANGUAGE, null))
                ?: AppLanguage.deviceDefault()
    }
}
