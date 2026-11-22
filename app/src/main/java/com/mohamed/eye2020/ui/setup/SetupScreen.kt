package com.mohamed.eye2020.ui.setup

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamed.eye2020.R
import com.mohamed.eye2020.ui.theme.letterSpacingForLocale
import com.mohamed.eye2020.util.PermissionStatus
import com.mohamed.eye2020.util.SystemSettings

/**
 * One-time permission setup shown before the main tabs on first launch. Each setting is
 * drawn as a picture card with a big glyph, a step number and a live "ready" state, so the
 * screen explains itself visually instead of relying on a wall of text. Every item is
 * optional: the app remains usable if the user skips.
 */
@Composable
fun SetupScreen(
    status: PermissionStatus,
    onRequestNotificationPermission: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val showFullScreenIntent = status.fullScreenIntentSupported &&
        !status.fullScreenIntentGranted
    val checks = buildList {
        add(status.notificationsGranted)
        add(status.batteryOptimizationIgnored)
        add(status.overlayGranted)
        if (status.fullScreenIntentSupported) add(status.fullScreenIntentGranted)
    }
    val readyCount = checks.count { it }

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
            SetupHero()
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.setup_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = letterSpacingForLocale(3.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
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

            Spacer(Modifier.height(20.dp))
            SetupProgress(ready = readyCount, total = checks.size)

            Spacer(Modifier.height(20.dp))
            SetupPermissionCard(
                step = 1,
                glyph = SetupGlyph.BELL,
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
                step = 2,
                glyph = SetupGlyph.BATTERY,
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
                step = 3,
                glyph = SetupGlyph.OVERLAY,
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

            if (showFullScreenIntent) {
                Spacer(Modifier.height(14.dp))
                SetupPermissionCard(
                    step = 4,
                    glyph = SetupGlyph.ALERT,
                    title = stringResource(R.string.setup_fsi_title),
                    badge = stringResource(R.string.setup_optional),
                    body = stringResource(R.string.setup_fsi_body),
                    statusText = stringResource(R.string.setup_not_set),
                    statusOk = false,
                    statusIsError = false,
                ) {
                    Button(
                        onClick = { SystemSettings.openFullScreenIntentSettings(context) },
                    ) {
                        Text(stringResource(R.string.setup_allow_fsi))
                    }
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

/**
 * A calm, animated eye drawn with simple shapes: an almond outline, a pulsing iris and a
 * clock ring that hints at the 20-minute cycle. It gives the setup screen a face before
 * any text is read.
 */
@Composable
private fun SetupHero() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val ink = MaterialTheme.colorScheme.onSurface
    val irisHighlight = MaterialTheme.colorScheme.onPrimary
    val pulse by rememberInfiniteTransition(label = "eye-pulse").animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "iris-pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(168.dp)) {
            val radius = size.minDimension / 2f
            val center = center

            drawCircle(
                color = primary.copy(alpha = 0.08f),
                radius = radius,
                center = center,
            )
            drawArc(
                color = primary.copy(alpha = 0.35f),
                startAngle = -90f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 4.dp.toPx(), center.y - radius + 4.dp.toPx()),
                size = Size(
                    (radius - 4.dp.toPx()) * 2f,
                    (radius - 4.dp.toPx()) * 2f,
                ),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = secondary,
                startAngle = 230f,
                sweepAngle = 40f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 4.dp.toPx(), center.y - radius + 4.dp.toPx()),
                size = Size(
                    (radius - 4.dp.toPx()) * 2f,
                    (radius - 4.dp.toPx()) * 2f,
                ),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )

            val eyeWidth = radius * 0.62f
            val eyeHeight = radius * 0.40f
            val almond = Path().apply {
                moveTo(center.x - eyeWidth, center.y)
                quadraticTo(center.x, center.y - eyeHeight, center.x + eyeWidth, center.y)
                quadraticTo(center.x, center.y + eyeHeight, center.x - eyeWidth, center.y)
                close()
            }
            drawPath(
                path = almond,
                color = ink,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
                color = primary,
                radius = radius * 0.17f * pulse,
                center = center,
            )
            drawCircle(
                color = irisHighlight,
                radius = radius * 0.065f * pulse,
                center = center,
            )
        }
    }
}

/** Three (or four) segments that fill in as each permission is granted. */
@Composable
private fun SetupProgress(ready: Int, total: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.setup_progress, ready, total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (ready == total) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.setup_all_ready),
                    style = MaterialTheme.typography.labelMedium,
                    color = primary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (index < ready) primary else track),
                )
            }
        }
    }
}

private enum class SetupGlyph { BELL, BATTERY, OVERLAY, ALERT }

/**
 * A permission explained as a picture: a large glyph in a tinted tile, a step number, the
 * plain-language body, a live state line and the button that opens the Android setting.
 */
@Composable
private fun SetupPermissionCard(
    step: Int,
    glyph: SetupGlyph,
    title: String,
    badge: String,
    body: String,
    statusText: String,
    statusOk: Boolean,
    statusIsError: Boolean = true,
    actions: @Composable () -> Unit,
) {
    val accent = if (statusOk) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (statusOk) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (statusOk) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(28.dp),
                            )
                        } else {
                            PermissionGlyph(kind = glyph, tint = accent)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = step.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (statusOk) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.Info
                    },
                    contentDescription = null,
                    tint = when {
                        statusOk -> MaterialTheme.colorScheme.primary
                        statusIsError -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        statusOk -> MaterialTheme.colorScheme.primary
                        statusIsError -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}

@Composable
private fun PermissionGlyph(kind: SetupGlyph, tint: Color) {
    when (kind) {
        SetupGlyph.BELL -> Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )

        SetupGlyph.BATTERY -> Canvas(modifier = Modifier.size(30.dp)) {
            val stroke = 2.dp.toPx()
            val bodyWidth = size.width * 0.80f
            val bodyHeight = size.height * 0.46f
            val top = (size.height - bodyHeight) / 2f
            drawRoundRect(
                color = tint,
                topLeft = Offset(0f, top),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = stroke),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(bodyWidth + 1.dp.toPx(), size.height / 2f - 3.dp.toPx()),
                size = Size(3.dp.toPx(), 6.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(stroke * 1.8f, top + stroke * 1.8f),
                size = Size(
                    bodyWidth * 0.60f,
                    bodyHeight - stroke * 3.6f,
                ),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
        }

        SetupGlyph.OVERLAY -> Canvas(modifier = Modifier.size(30.dp)) {
            val stroke = 2.dp.toPx()
            drawRoundRect(
                color = tint.copy(alpha = 0.45f),
                topLeft = Offset(size.width * 0.02f, size.height * 0.06f),
                size = Size(size.width * 0.62f, size.height * 0.60f),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = stroke),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(size.width * 0.34f, size.height * 0.34f),
                size = Size(size.width * 0.62f, size.height * 0.60f),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = stroke),
            )
        }

        SetupGlyph.ALERT -> Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
    }
}
