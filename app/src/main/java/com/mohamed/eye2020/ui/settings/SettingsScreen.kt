package com.mohamed.eye2020.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamed.eye2020.BuildConfig
import com.mohamed.eye2020.R
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.ui.permissions.rememberPermissionSnapshot
import com.mohamed.eye2020.util.SystemSettings
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<DurationField?>(null) }
    val permissions = rememberPermissionSnapshot()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_settings).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))
        SettingsSection(title = stringResource(R.string.section_timer)) {
            DurationRow(
                title = stringResource(R.string.work_duration),
                description = stringResource(R.string.work_duration_desc),
                value = stringResource(R.string.duration_minutes, settings.workMinutes),
                onClick = { dialog = DurationField.WORK },
            )
            RowDivider()
            DurationRow(
                title = stringResource(R.string.break_duration),
                description = stringResource(R.string.break_duration_desc),
                value = stringResource(R.string.duration_seconds, settings.breakSeconds),
                onClick = { dialog = DurationField.BREAK },
            )
            RowDivider()
            DurationRow(
                title = stringResource(R.string.grace_duration),
                description = stringResource(R.string.grace_duration_desc),
                value = stringResource(
                    R.string.duration_minutes,
                    settings.screenOffGraceMinutes,
                ),
                onClick = { dialog = DurationField.GRACE },
            )
            RowDivider()
            SwitchRow(
                title = stringResource(R.string.auto_start),
                description = stringResource(R.string.auto_start_desc),
                checked = settings.autoStartNextWork,
                onCheckedChange = viewModel::setAutoStart,
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = stringResource(R.string.section_break)) {
            SwitchRow(
                title = stringResource(R.string.vibrate_start),
                description = null,
                checked = settings.vibrateOnBreakStart,
                onCheckedChange = viewModel::setVibrateOnStart,
            )
            RowDivider()
            SwitchRow(
                title = stringResource(R.string.vibrate_end),
                description = null,
                checked = settings.vibrateOnBreakEnd,
                onCheckedChange = viewModel::setVibrateOnEnd,
            )
            RowDivider()
            SwitchRow(
                title = stringResource(R.string.voice_announcements),
                description = stringResource(R.string.voice_announcements_desc),
                checked = settings.voiceAnnouncements,
                onCheckedChange = viewModel::setVoiceAnnouncements,
            )
            RowDivider()
            AnnouncementVolumeRow(
                value = settings.announcementVolume,
                enabled = settings.voiceAnnouncements,
                onValueChangeFinished = viewModel::setAnnouncementVolume,
            )
            RowDivider()
            SwitchRow(
                title = stringResource(R.string.keep_awake),
                description = stringResource(R.string.keep_awake_desc),
                checked = settings.keepScreenAwakeDuringBreak,
                onCheckedChange = viewModel::setKeepAwake,
            )
            RowDivider()
            SwitchRow(
                title = stringResource(R.string.fullscreen_break),
                description = stringResource(R.string.fullscreen_break_desc),
                checked = settings.fullscreenBreak,
                onCheckedChange = viewModel::setFullscreenBreak,
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = stringResource(R.string.section_system)) {
            SystemRow(
                title = stringResource(R.string.notifications_setting),
                description = stringResource(R.string.notifications_setting_desc),
                status = if (permissions.notificationsGranted) {
                    stringResource(R.string.allowed)
                } else {
                    stringResource(R.string.not_allowed)
                },
                statusOk = permissions.notificationsGranted,
                actionLabel = stringResource(
                    if (permissions.notificationsGranted) {
                        R.string.manage
                    } else {
                        R.string.open_settings
                    },
                ),
                onAction = { SystemSettings.openNotificationSettings(context) },
            )
            RowDivider()
            SystemRow(
                title = stringResource(R.string.battery_setting),
                description = stringResource(R.string.battery_setting_desc),
                status = if (permissions.batteryOptimizationIgnored) {
                    stringResource(R.string.battery_unrestricted)
                } else {
                    stringResource(R.string.battery_optimized)
                },
                statusOk = permissions.batteryOptimizationIgnored,
                actionLabel = stringResource(
                    if (permissions.batteryOptimizationIgnored) {
                        R.string.manage
                    } else {
                        R.string.open_settings
                    },
                ),
                onAction = { SystemSettings.openBatteryOptimizationSettings(context) },
            )
            RowDivider()
            SystemRow(
                title = stringResource(R.string.overlay_setting),
                description = stringResource(R.string.overlay_setting_desc),
                status = if (permissions.overlayGranted) {
                    stringResource(R.string.allowed)
                } else {
                    stringResource(R.string.not_allowed)
                },
                statusOk = permissions.overlayGranted,
                actionLabel = stringResource(
                    if (permissions.overlayGranted) {
                        R.string.manage
                    } else {
                        R.string.open_settings
                    },
                ),
                onAction = { SystemSettings.openOverlaySettings(context) },
            )
            RowDivider()
            SystemRow(
                title = stringResource(R.string.fullscreen_alerts),
                description = stringResource(R.string.fullscreen_alerts_desc),
                status = if (!permissions.fullScreenIntentSupported) {
                    stringResource(R.string.not_required)
                } else if (permissions.fullScreenIntentGranted) {
                    stringResource(R.string.allowed)
                } else {
                    stringResource(R.string.not_allowed)
                },
                statusOk = !permissions.fullScreenIntentSupported ||
                    permissions.fullScreenIntentGranted,
                actionLabel = if (!permissions.fullScreenIntentSupported) {
                    null
                } else {
                    stringResource(
                        if (permissions.fullScreenIntentGranted) {
                            R.string.manage
                        } else {
                            R.string.open_settings
                        },
                    )
                },
                onAction = { SystemSettings.openFullScreenIntentSettings(context) },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.system_permission_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = stringResource(R.string.section_about)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.about_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.reliability_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.version_name, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    val activeDialog = dialog
    if (activeDialog != null) {
        when (activeDialog) {
            DurationField.WORK -> DurationPickerDialog(
                title = stringResource(R.string.dialog_work_title),
                unit = stringResource(R.string.unit_minutes),
                unitShort = stringResource(R.string.unit_minutes_short),
                presets = listOf(20, 30, 40),
                range = AppSettings.LIMIT_WORK,
                initialValue = settings.workMinutes,
                limitText = stringResource(R.string.limit_work),
                invalidText = stringResource(R.string.invalid_work),
                onDismiss = { dialog = null },
                onSave = {
                    viewModel.setWorkMinutes(it)
                    dialog = null
                },
            )

            DurationField.BREAK -> DurationPickerDialog(
                title = stringResource(R.string.dialog_break_title),
                unit = stringResource(R.string.unit_seconds),
                unitShort = stringResource(R.string.unit_seconds_short),
                presets = listOf(20, 30, 60),
                range = AppSettings.LIMIT_BREAK,
                initialValue = settings.breakSeconds,
                limitText = stringResource(R.string.limit_break),
                invalidText = stringResource(R.string.invalid_break),
                onDismiss = { dialog = null },
                onSave = {
                    viewModel.setBreakSeconds(it)
                    dialog = null
                },
            )

            DurationField.GRACE -> DurationPickerDialog(
                title = stringResource(R.string.dialog_grace_title),
                unit = stringResource(R.string.unit_minutes),
                unitShort = stringResource(R.string.unit_minutes_short),
                presets = listOf(1, 2, 5),
                range = AppSettings.LIMIT_GRACE,
                initialValue = settings.screenOffGraceMinutes,
                limitText = stringResource(R.string.limit_grace),
                invalidText = stringResource(R.string.invalid_grace),
                onDismiss = { dialog = null },
                onSave = {
                    viewModel.setGraceMinutes(it)
                    dialog = null
                },
            )
        }
    }
}

private enum class DurationField { WORK, BREAK, GRACE }

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun DurationRow(
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AnnouncementVolumeRow(
    value: Float,
    enabled: Boolean,
    onValueChangeFinished: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.announcement_volume),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(
                    R.string.volume_percent,
                    (sliderValue * 100f).roundToInt(),
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChangeFinished(sliderValue) },
            enabled = enabled,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SystemRow(
    title: String,
    description: String,
    status: String?,
    statusOk: Boolean,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(3.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (statusOk) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
            Spacer(Modifier.weight(1f))
            if (actionLabel != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun DurationPickerDialog(
    title: String,
    unit: String,
    unitShort: String,
    presets: List<Int>,
    range: IntRange,
    initialValue: Int,
    limitText: String,
    invalidText: String,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var input by remember(initialValue) { mutableStateOf(initialValue.toString()) }
    val parsed = input.toIntOrNull()
    val valid = parsed != null && parsed in range

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { candidate ->
                        if (candidate.length <= 3 && candidate.all { it.isDigit() }) {
                            input = candidate
                        }
                    },
                    singleLine = true,
                    isError = input.isNotEmpty() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(unitShort) },
                    supportingText = {
                        Text(
                            text = if (input.isNotEmpty() && !valid) invalidText else limitText,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.presets),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = input == preset.toString(),
                            onClick = { input = preset.toString() },
                            label = {
                                Text(
                                    text = preset.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = valid,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
