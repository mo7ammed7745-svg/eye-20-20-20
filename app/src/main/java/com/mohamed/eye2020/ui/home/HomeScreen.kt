package com.mohamed.eye2020.ui.home

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamed.eye2020.R
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.timer.PauseReason
import com.mohamed.eye2020.timer.TimerPhase
import com.mohamed.eye2020.timer.TimerState
import com.mohamed.eye2020.ui.game.GameAppPickerOverlay
import com.mohamed.eye2020.ui.game.rememberGameAppLabels
import com.mohamed.eye2020.ui.game.rememberUsageAccessGranted
import com.mohamed.eye2020.ui.settings.SettingsViewModel
import com.mohamed.eye2020.ui.theme.Mint
import com.mohamed.eye2020.ui.theme.letterSpacingForLocale
import com.mohamed.eye2020.util.SystemSettings
import com.mohamed.eye2020.util.TimeFormat
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    settings: AppSettings,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.timerState.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showGameAppPicker by rememberSaveable { mutableStateOf(false) }
    val usageAccessGranted = rememberUsageAccessGranted()
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var overlayAllowed by remember {
        mutableStateOf(SystemSettings.canDrawOverlays(context))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        overlayAllowed = SystemSettings.canDrawOverlays(context)
    }

    val now = rememberRealtimeNow(state)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = letterSpacingForLocale(3.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!notificationsEnabled) {
                Spacer(Modifier.height(20.dp))
                PermissionCard(
                    title = stringResource(R.string.notifications_off_title),
                    body = stringResource(R.string.notifications_off_body),
                    action = stringResource(R.string.enable_notifications),
                    onAction = { SystemSettings.openNotificationSettings(context) },
                )
                }

            if (!overlayAllowed) {
                Spacer(Modifier.height(12.dp))
                PermissionCard(
                    title = stringResource(R.string.overlay_off_title),
                    body = stringResource(R.string.overlay_off_body),
                    action = stringResource(R.string.allow_overlay),
                    onAction = { SystemSettings.openOverlaySettings(context) },
                )
            }

            Spacer(Modifier.height(28.dp))
            TimerDial(
                state = state,
                now = now,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))
            TimerControls(
                state = state,
                onPrimaryAction = viewModel::primaryAction,
                onStopAction = viewModel::stopWork,
            )

            Spacer(Modifier.height(28.dp))
            GameModeCard(
                settings = settings,
                usageAccessGranted = usageAccessGranted,
                onAddApp = { showGameAppPicker = true },
                onRemoveApp = settingsViewModel::removeGameModeApp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))
            TodayCard(
                count = todayCount,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            HowItWorksCard(settings = settings)

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp),
                )
                Text(
                    text = stringResource(R.string.screen_off_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showGameAppPicker) {
            GameAppPickerOverlay(
                selected = settings.gameModeApps,
                onConfirm = { nextApps ->
                    settingsViewModel.setGameModeApps(nextApps)
                    showGameAppPicker = false
                },
                onDismiss = { showGameAppPicker = false },
            )
        }
    }
}

/**
 * Game mode lives on the home page so the picked games stay visible at a glance:
 * add and remove straight from here, with the usage-access grant only when missing.
 */
@Composable
private fun GameModeCard(
    settings: AppSettings,
    usageAccessGranted: Boolean,
    onAddApp: () -> Unit,
    onRemoveApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.game_mode_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.game_mode_apps_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            if (settings.gameModeApps.isEmpty()) {
                Text(
                    text = stringResource(R.string.game_mode_empty_home),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val labels = rememberGameAppLabels(settings.gameModeApps)
                settings.gameModeApps.forEachIndexed { index, packageName ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = labels[packageName] ?: packageName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        TextButton(onClick = { onRemoveApp(packageName) }) {
                            Text(stringResource(R.string.game_mode_remove))
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onAddApp) {
                Text(stringResource(R.string.game_mode_add_game))
            }
            if (settings.gameModeApps.size >= AppSettings.MAX_GAME_APPS) {
                Text(
                    text = stringResource(
                        R.string.game_mode_apps_limit,
                        AppSettings.MAX_GAME_APPS,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (!usageAccessGranted) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.game_mode_usage_needed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = { SystemSettings.openUsageAccessSettings(context) }) {
                    Text(stringResource(R.string.game_mode_usage_grant))
                }
            }
        }
    }
}

@Composable
private fun rememberRealtimeNow(state: TimerState): Long {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            // An inactive timer's remaining value is static; there is nothing to tick.
            val interval = when {
                state.isRunning -> 250L
                state.isActive -> 1_000L
                else -> return@LaunchedEffect
            }
            delay(interval)
        }
    }
    return now
}

@Composable
private fun TimerDial(
    state: TimerState,
    now: Long,
    modifier: Modifier = Modifier,
) {
    val remaining = state.remainingAt(now)
    val progress = state.progressAt(now)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = when {
        state.phase == TimerPhase.BREAK -> MaterialTheme.colorScheme.secondary
        state.pauseReason == PauseReason.SCREEN_OFF_RESET -> MaterialTheme.colorScheme.onSurfaceVariant
        state.isRunning -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    }
    val status = when {
        state.phase == TimerPhase.BREAK -> stringResource(R.string.status_break)
        state.isRunning -> stringResource(R.string.status_working)
        state.pauseReason == PauseReason.SCREEN_OFF -> stringResource(R.string.status_paused)
        state.isActive -> stringResource(R.string.status_paused)
        state.pauseReason == PauseReason.SCREEN_OFF_RESET -> stringResource(R.string.status_ready)
        else -> stringResource(R.string.status_ready)
    }
    val description = when {
        state.phase == TimerPhase.BREAK -> stringResource(R.string.status_break)
        state.isRunning -> stringResource(R.string.status_working_desc)
        state.pauseReason == PauseReason.SCREEN_OFF -> stringResource(R.string.status_screen_off_desc)
        state.isActive -> stringResource(R.string.status_paused_desc)
        else -> stringResource(R.string.status_ready_desc)
    }
    val timeText = TimeFormat.clock(remaining)
    val timerDescription = stringResource(R.string.cd_timer, timeText)
    val timeStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = if (timeText.length > 5) 50.sp else 76.sp,
        lineHeight = if (timeText.length > 5) 56.sp else 80.sp,
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(300.dp)
                .clearAndSetSemantics {
                    // One localized announcement instead of hardcoded English and
                    // re-announcing each visible text as a separate focus.
                    contentDescription = "$status. $timerDescription"
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeWidth = 13.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                drawArc(
                    color = track.copy(alpha = 0.55f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                if (progress > 0f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(236.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = status.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = letterSpacingForLocale(2.6.sp),
                        color = if (state.isRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = timeText,
                        style = timeStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.timer_of_total,
                            TimeFormat.clock(state.totalMillis),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimerControls(
    state: TimerState,
    onPrimaryAction: () -> Unit,
    onStopAction: () -> Unit,
) {
    when {
        state.phase == TimerPhase.BREAK -> {
            Text(
                text = stringResource(R.string.break_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.isRunning -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(stringResource(R.string.pause))
                }
                OutlinedButton(
                    onClick = onStopAction,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.stop),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        state.isActive -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.resume),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                OutlinedButton(
                    onClick = onStopAction,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.stop),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        else -> {
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.start_focus),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onAction) {
                Text(action)
            }
        }
    }
}

@Composable
private fun TodayCard(count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.today_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.today_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HowItWorksCard(settings: AppSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.how_it_works),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.how_it_works_body,
                    pluralStringResource(
                        R.plurals.label_minutes,
                        settings.workMinutes,
                        settings.workMinutes,
                    ),
                    pluralStringResource(
                        R.plurals.label_seconds,
                        settings.breakSeconds,
                        settings.breakSeconds,
                    ),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(Mint.copy(alpha = 0.14f), MaterialTheme.colorScheme.primary) {
                    Text(
                        text = stringResource(R.string.duration_minutes, settings.workMinutes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Pill(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.duration_seconds, settings.breakSeconds),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(
    background: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            content()
        }
    }
}
