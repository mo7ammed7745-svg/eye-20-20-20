package com.mohamed.eye2020.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun setWorkMinutes(value: Int) = update { it.copy(workMinutes = value) }

    fun setBreakSeconds(value: Int) = update { it.copy(breakSeconds = value) }

    fun setGraceMinutes(value: Int) = update { it.copy(screenOffGraceMinutes = value) }

    fun setAutoStart(enabled: Boolean) = update { it.copy(autoStartNextWork = enabled) }

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

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repository.update(transform)
        }
    }
}
