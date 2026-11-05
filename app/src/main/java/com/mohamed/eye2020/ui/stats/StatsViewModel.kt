package com.mohamed.eye2020.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.eye2020.data.BreakRepository
import com.mohamed.eye2020.domain.WeeklyStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(breakRepository: BreakRepository) : ViewModel() {

    val weeklyStats: StateFlow<WeeklyStats?> = breakRepository
        .observeWeeklyStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val allTimeCount: StateFlow<Int> = breakRepository
        .observeAllTimeCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
