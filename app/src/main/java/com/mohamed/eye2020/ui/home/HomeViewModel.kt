package com.mohamed.eye2020.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.eye2020.data.BreakRepository
import com.mohamed.eye2020.timer.TimerManager
import com.mohamed.eye2020.timer.TimerPhase
import com.mohamed.eye2020.timer.TimerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val timerManager: TimerManager,
    breakRepository: BreakRepository,
) : ViewModel() {

    val timerState: StateFlow<TimerState> = timerManager.state

    val todayCount: StateFlow<Int> = breakRepository
        .observeWeeklyStats()
        .map { it.todayCount }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    fun primaryAction() {
        val state = timerManager.state.value
        if (state.phase == TimerPhase.WORK &&
            state.isActive &&
            state.isRunning
        ) {
            timerManager.pauseWork()
        } else {
            timerManager.startOrResumeWork()
        }
    }

    fun stopWork() {
        timerManager.stopWork()
    }

    fun ensureService() {
        timerManager.ensureServiceRunning()
    }
}
