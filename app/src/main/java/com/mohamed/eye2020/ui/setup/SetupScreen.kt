package com.mohamed.eye2020.ui.setup

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamed.eye2020.R
import com.mohamed.eye2020.ui.theme.letterSpacingForLocale
import com.mohamed.eye2020.util.PermissionStatus
import com.mohamed.eye2020.util.SystemSettings

/**
 * One-time permission setup shown before the main tabs on first launch. Every item is
 * optional: the app remains usable if the user skips, but the explanations make clear
 * which settings improve background reliability and which only improve the break screen.
 */
@Composable
fun SetupScreen(
    status: PermissionStatus,
    onRequestNotificationPermission: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = onFinished,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.setup_finish),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.setup_skip))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = letterSpacingForLocale(3.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            SetupPermissionCard(
                title = stringResource(R.string.setup_notifications_title),
                badge = stringResource(R.string.setup_recommended),
                body = stringResource(R.string.setup_notifications_body),
                statusText = if (status.notificationsGranted) {
                    stringResource(R.string.allowed)
                } else {
                    stringResource(R.string.not_allowed)
                },
                statusOk = status.notificationsGranted,
            ) {
                if (!status.notificationsGranted &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    Button(onClick = onRequestNotificationPermission) {
                        Text(stringResource(R.string.setup_allow_notifications))
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = { SystemSettings.openNotificationSettings(context) },
                    ) {
                        Text(stringResource(R.string.open_settings))
                    }
                } else {
                    Button(
                        onClick = { SystemSettings.openNotificationSettings(context) },
                    ) {
                        Text(
                            stringResource(
                                if (status.notificationsGranted) {
                                    R.string.manage
                                } else {
                                    R.string.open_notification_settings
                                },
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SetupPermissionCard(
                title = stringResource(R.string.setup_battery_title),
                badge = stringResource(R.string.setup_recommended),
                body = stringResource(R.string.setup_battery_body),
                statusText = if (status.batteryOptimizationIgnored) {
                    stringResource(R.string.battery_unrestricted)
                } else {
                    stringResource(R.string.battery_optimized)
                },
                statusOk = status.batteryOptimizationIgnored,
            ) {
                Button(onClick = { SystemSettings.openBatteryOptimizationSettings(context) }) {
                    Text(
                        stringResource(
                            if (status.batteryOptimizationIgnored) {
                                R.string.manage
                            } else {
                                R.string.open_battery_settings
                            },
                        ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            SetupPermissionCard(
                title = stringResource(R.string.setup_overlay_title),
                badge = stringResource(R.string.setup_optional),
                body = stringResource(R.string.setup_overlay_body),
                statusText = if (status.overlayGranted) {
                    stringResource(R.string.allowed)
                } else {
                    stringResource(R.string.setup_not_set)
                },
                statusOk = status.overlayGranted,
                statusIsError = false,
            ) {
                Button(onClick = { SystemSettings.openOverlaySettings(context) }) {
                    Text(
                        stringResource(
                            if (status.overlayGranted) {
                                R.string.manage
                            } else {
                                R.string.setup_allow_overlay
                            },
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.setup_later_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reliability_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SetupPermissionCard(
    title: String,
    badge: String,
    body: String,
    statusText: String,
    statusOk: Boolean,
    statusIsError: Boolean = true,
    actions: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    statusOk -> MaterialTheme.colorScheme.primary
                    statusIsError -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }
    }
}
