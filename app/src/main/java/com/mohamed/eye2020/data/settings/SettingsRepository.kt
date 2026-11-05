package com.mohamed.eye2020.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.eyeSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "eye_settings",
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences.toAppSettings() }
        .distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val next = transform(preferences.toAppSettings()).validated()
            preferences[Keys.WORK_MINUTES] = next.workMinutes
            preferences[Keys.BREAK_SECONDS] = next.breakSeconds
            preferences[Keys.GRACE_MINUTES] = next.screenOffGraceMinutes
            preferences[Keys.AUTO_START] = next.autoStartNextWork
            preferences[Keys.VIBRATE_START] = next.vibrateOnBreakStart
            preferences[Keys.VIBRATE_END] = next.vibrateOnBreakEnd
            preferences[Keys.VOICE_ANNOUNCEMENTS] = next.voiceAnnouncements
            preferences[Keys.ANNOUNCEMENT_VOLUME] = next.announcementVolume
            preferences[Keys.KEEP_AWAKE] = next.keepScreenAwakeDuringBreak
            preferences[Keys.FULLSCREEN_BREAK] = next.fullscreenBreak
        }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        workMinutes = this[Keys.WORK_MINUTES] ?: AppSettings.DEFAULT_WORK_MINUTES,
        breakSeconds = this[Keys.BREAK_SECONDS] ?: AppSettings.DEFAULT_BREAK_SECONDS,
        screenOffGraceMinutes = this[Keys.GRACE_MINUTES] ?: AppSettings.DEFAULT_GRACE_MINUTES,
        autoStartNextWork = this[Keys.AUTO_START] ?: true,
        vibrateOnBreakStart = this[Keys.VIBRATE_START] ?: true,
        vibrateOnBreakEnd = this[Keys.VIBRATE_END] ?: true,
        voiceAnnouncements = this[Keys.VOICE_ANNOUNCEMENTS] ?: true,
        announcementVolume = this[Keys.ANNOUNCEMENT_VOLUME]
            ?: AppSettings.DEFAULT_ANNOUNCEMENT_VOLUME,
        keepScreenAwakeDuringBreak = this[Keys.KEEP_AWAKE] ?: true,
        fullscreenBreak = this[Keys.FULLSCREEN_BREAK] ?: true,
    ).validated()

    private object Keys {
        val WORK_MINUTES = intPreferencesKey("work_minutes")
        val BREAK_SECONDS = intPreferencesKey("break_seconds")
        val GRACE_MINUTES = intPreferencesKey("screen_off_grace_minutes")
        val AUTO_START = booleanPreferencesKey("auto_start_next_work")
        val VIBRATE_START = booleanPreferencesKey("vibrate_on_break_start")
        val VIBRATE_END = booleanPreferencesKey("vibrate_on_break_end")
        val VOICE_ANNOUNCEMENTS = booleanPreferencesKey("voice_announcements")
        val ANNOUNCEMENT_VOLUME = floatPreferencesKey("announcement_volume")
        val KEEP_AWAKE = booleanPreferencesKey("keep_screen_awake_during_break")
        val FULLSCREEN_BREAK = booleanPreferencesKey("fullscreen_break")
    }
}
