package com.mohamed.eye2020

import android.content.Context
import androidx.room.Room
import com.mohamed.eye2020.audio.AnnouncementPlayer
import com.mohamed.eye2020.data.BreakRepository
import com.mohamed.eye2020.data.db.EyeDatabase
import com.mohamed.eye2020.data.settings.SettingsRepository
import com.mohamed.eye2020.data.settings.SetupPreferences
import com.mohamed.eye2020.data.settings.eyeSettingsDataStore
import com.mohamed.eye2020.service.NotificationHelper
import com.mohamed.eye2020.timer.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Small manual dependency container. Keeps the app free of DI frameworks. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(appContext.eyeSettingsDataStore)

    val setupPreferences = SetupPreferences(appContext)

    private val database: EyeDatabase = Room.databaseBuilder(
        appContext,
        EyeDatabase::class.java,
        EyeDatabase.NAME,
    ).build()

    val breakRepository = BreakRepository(database.breakRecordDao())

    val notificationHelper = NotificationHelper(appContext).also { it.createChannels() }

    private val announcementPlayer = AnnouncementPlayer(appContext, applicationScope)

    val timerManager = TimerManager(
        context = appContext,
        settingsRepository = settingsRepository,
        breakRepository = breakRepository,
        announcementPlayer = announcementPlayer,
        appScope = applicationScope,
    )
}
