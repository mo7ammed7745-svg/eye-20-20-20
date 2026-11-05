package com.mohamed.eye2020

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mohamed.eye2020.ui.home.HomeViewModel
import com.mohamed.eye2020.ui.settings.SettingsViewModel
import com.mohamed.eye2020.ui.stats.StatsViewModel

class EyeViewModelFactory(private val app: EyeApplication) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = app.container
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                container.timerManager,
                container.breakRepository,
            ) as T

            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(
                container.breakRepository,
            ) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                container.settingsRepository,
            ) as T

            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
