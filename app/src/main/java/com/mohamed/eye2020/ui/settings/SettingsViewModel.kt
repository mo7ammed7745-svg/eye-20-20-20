package com.mohamed.eye2020.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    /**
     * The persisted values are emitted first, so opening Settings does not flash the
     * factory defaults while DataStore finishes its first read.
     */
    val settings: StateFlow<AppSettings> = flow {
        emit(loadCurrent())
        emitAll(repository.settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun setWorkMinutes(value: Int) = update { it.copy(workMinutes = value) }

    fun setBreakSeconds(value: Int) = update { it.copy(breakSeconds = value) }

    fun setGraceMinutes(value: Int) = update { it.copy(screenOffGraceMinutes = value) }

    fun setAutoStart(enabled: Boolean) = update { it.copy(autoStartNextWork = enabled) }

    fun setAllowBreakSkipping(enabled: Boolean) =
        update { it.copy(allowBreakSkipping = enabled) }

    fun setVibrateOnStart(enabled: Boolean) =
        update { it.copy(vibrateOnBreakStart = enabled) }

    fun setVibrateOnEnd(enabled: Boolean) = update { it.copy(vibrateOnBreakEnd = enabled) }

    fun setVoiceAnnouncements(enabled: Boolean) =
        update { it.copy(voiceAnnouncements = enabled) }

    fun setAnnouncementVolume(volume: Float) =
        update { it.copy(announcementVolume = volume.coerceIn(0f, 1f)) }

    fun setKeepAwake(enabled: Boolean) =
        update { it.copy(keepScreenAwakeDuringBreak = enabled) }

    fun setFullscreenBreak(enabled: Boolean) = update { it.copy(fullscreenBreak = enabled) }

    /** Replaces the game-mode app list in one write (picker must feel atomic). */
    fun setGameModeApps(packages: List<String>) =
        update { it.copy(gameModeApps = packages) }

    fun addGameModeApp(packageName: String) = update { settings ->
        settings.copy(
            gameModeApps = settings.gameModeApps + packageName,
        )
    }

    fun removeGameModeApp(packageName: String) = update { settings ->
        settings.copy(
            gameModeApps = settings.gameModeApps.filterNot { it == packageName },
        )
    }

    private suspend fun loadCurrent(): AppSettings = runCatching { repository.current() }
        .getOrDefault(AppSettings())

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repository.update(transform)
        }
    }
}
