package com.mohamed.eye2020.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamed.eye2020.EyeViewModelFactory
import com.mohamed.eye2020.R
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.data.settings.SetupPreferences
import com.mohamed.eye2020.timer.TimerEvent
import com.mohamed.eye2020.timer.TimerManager
import com.mohamed.eye2020.timer.TimerPhase
import com.mohamed.eye2020.ui.breakscreen.BreakScreen
import com.mohamed.eye2020.ui.home.HomeScreen
import com.mohamed.eye2020.ui.home.HomeViewModel
import com.mohamed.eye2020.ui.permissions.rememberPermissionSnapshot
import com.mohamed.eye2020.ui.settings.SettingsScreen
import com.mohamed.eye2020.ui.settings.SettingsViewModel
import com.mohamed.eye2020.ui.setup.SetupScreen
import com.mohamed.eye2020.ui.stats.StatsScreen
import com.mohamed.eye2020.ui.stats.StatsViewModel

private enum class Destination(
    val labelRes: Int,
    val icon: ImageVector,
) {
    TIMER(R.string.nav_timer, Icons.Filled.Home),
    STATS(R.string.nav_stats, Icons.Filled.DateRange),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings),
}

@Composable
fun EyeApp(
    factory: EyeViewModelFactory,
    timerManager: TimerManager,
    setupPreferences: SetupPreferences,
    onRequestNotificationPermission: () -> Unit,
    permissionRefreshKey: Int = 0,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val statsViewModel: StatsViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val timerState by timerManager.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val currentSettings = settings ?: AppSettings()
    val permissionStatus = rememberPermissionSnapshot(permissionRefreshKey)
    var setupCompleted by remember { mutableStateOf(setupPreferences.isCompleted()) }

    if (timerState.phase == TimerPhase.BREAK && timerState.isActive) {
        BackHandler(enabled = true) {
            // A break cannot be dismissed through the app.
        }
        BreakScreen(state = timerState)
        return
    }

    if (!setupCompleted) {
        SetupScreen(
            status = permissionStatus,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onFinished = {
                setupPreferences.markCompleted()
                setupCompleted = true
            },
        )
        return
    }

    var selected by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resetMessage = stringResource(R.string.reset_snackbar)

    LaunchedEffect(timerManager) {
        timerManager.events.collect { event ->
            if (event is TimerEvent.WorkResetAfterScreenOff) {
                snackbarHostState.showSnackbar(resetMessage)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                Destination.entries.forEachIndexed { index, destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (Destination.entries[selected]) {
            Destination.TIMER -> HomeScreen(
                viewModel = homeViewModel,
                settings = currentSettings,
                modifier = Modifier.padding(innerPadding),
            )

            Destination.STATS -> StatsScreen(
                viewModel = statsViewModel,
                modifier = Modifier.padding(innerPadding),
            )

            Destination.SETTINGS -> SettingsScreen(
                settings = currentSettings,
                viewModel = settingsViewModel,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
