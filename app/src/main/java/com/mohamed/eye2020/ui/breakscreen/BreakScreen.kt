package com.mohamed.eye2020.ui.breakscreen

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamed.eye2020.R
import com.mohamed.eye2020.timer.TimerState
import com.mohamed.eye2020.util.TimeFormat
import kotlinx.coroutines.delay

private val BreakWhite = Color(0xFFF2F5F4)
private val BreakMuted = Color(0xFF7D8A86)
private val BreakLine = Color(0xFF1C2422)
private val BreakProgress = Color(0xFFB9C4C0)

@Composable
fun BreakScreen(
    state: TimerState,
    modifier: Modifier = Modifier,
    /** Opt-in via Settings; null keeps the break strictly until it finishes. */
    onSkip: (() -> Unit)? = null,
) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(200L)
        }
    }

    val remaining = state.remainingAt(now)
    val time = TimeFormat.clock(remaining)
    val progress = state.progressAt(now)
    val description = stringResource(R.string.break_time_remaining, time)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.break_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 5.sp,
                color = BreakMuted,
            )
            Spacer(Modifier.height(36.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(292.dp)
                    .clearAndSetSemantics { contentDescription = description },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = BreakLine,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    if (progress > 0f) {
                        drawArc(
                            color = BreakProgress,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Text(
                    text = time,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (time.length > 5) 58.sp else 92.sp,
                        lineHeight = if (time.length > 5) 66.sp else 100.sp,
                    ),
                    color = BreakWhite,
                    fontWeight = FontWeight.Light,
                )
            }

            Spacer(Modifier.height(34.dp))
            Text(
                text = stringResource(R.string.break_instruction),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = BreakWhite,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.break_subinstruction),
                style = MaterialTheme.typography.bodyMedium,
                color = BreakMuted,
                textAlign = TextAlign.Center,
            )
            if (onSkip != null) {
                Spacer(Modifier.height(28.dp))
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.skip_break),
                        style = MaterialTheme.typography.labelLarge,
                        color = BreakMuted,
                    )
                }
            }
        }
    }
}
