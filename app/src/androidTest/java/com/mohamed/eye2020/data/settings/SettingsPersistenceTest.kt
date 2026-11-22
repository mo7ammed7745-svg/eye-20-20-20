package com.mohamed.eye2020.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    @Test
    fun settingsAreWrittenToDiskAndReadBackByANewRepository() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "settings-test-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
            val repository = SettingsRepository(dataStore)

            repository.update { current ->
                current.copy(
                    workMinutes = 30,
                    breakSeconds = 45,
                    screenOffGraceMinutes = 5,
                    autoStartNextWork = false,
                    vibrateOnBreakStart = false,
                    voiceAnnouncements = false,
                    announcementVolume = 0.35f,
                    keepScreenAwakeDuringBreak = false,
                    fullscreenBreak = false,
                    gameModeApps = listOf("com.example.game", "com.other.app"),
                )
            }

            val reloaded = SettingsRepository(dataStore).current()
            assertEquals(30, reloaded.workMinutes)
            assertEquals(45, reloaded.breakSeconds)
            assertEquals(5, reloaded.screenOffGraceMinutes)
            assertFalse(reloaded.autoStartNextWork)
            assertFalse(reloaded.vibrateOnBreakStart)
            assertFalse(reloaded.voiceAnnouncements)
            assertEquals(0.35f, reloaded.announcementVolume, 0.001f)
            assertFalse(reloaded.keepScreenAwakeDuringBreak)
            assertFalse(reloaded.fullscreenBreak)
            assertEquals(
                listOf("com.example.game", "com.other.app"),
                reloaded.gameModeApps,
            )

            assertTrue(file.exists())
            assertTrue(file.length() > 0L)
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    @Test
    fun repositoryRejectsInvalidDurationsAndKeepsDefaults() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "settings-test-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
            val repository = SettingsRepository(dataStore)

            repository.update {
                it.copy(workMinutes = 999, breakSeconds = 2, screenOffGraceMinutes = -4)
            }

            val reloaded = repository.current()
            assertEquals(AppSettings.DEFAULT_WORK_MINUTES, reloaded.workMinutes)
            assertEquals(AppSettings.DEFAULT_BREAK_SECONDS, reloaded.breakSeconds)
            assertEquals(AppSettings.DEFAULT_GRACE_MINUTES, reloaded.screenOffGraceMinutes)
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
