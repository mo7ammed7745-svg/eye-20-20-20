package com.mohamed.eye2020

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.timer.TimerPhase
import com.mohamed.eye2020.ui.breakscreen.BreakScreen
import com.mohamed.eye2020.ui.theme.EyeTheme
import kotlinx.coroutines.launch

/**
 * A deliberately strict, pure-black break screen. It disables the back gesture and has
 * no in-app action that can end a break. Home, system gestures, emergency calls and the
 * power button remain outside any normal app's control.
 */
class BreakActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as EyeApplication).container

    private var closing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        onBackPressedDispatcher.addCallback(this) {
            // Intentionally no-op. There is no way out through the app's own UI.
        }
        applyWindowOptions(AppSettings())

        if (!container.timerManager.isInBreak()) {
            finish()
            return
        }

        setContent {
            val state by container.timerManager.state.collectAsStateWithLifecycle()
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            EyeTheme(darkTheme = true) {
                BreakScreen(
                    state = state,
                    onSkip = if (settings.allowBreakSkipping) {
                        { container.timerManager.skipBreak() }
                    } else {
                        null
                    },
                )
            }
            androidx.compose.runtime.LaunchedEffect(
                settings.fullscreenBreak,
                settings.keepScreenAwakeDuringBreak,
            ) {
                applyWindowOptions(settings)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                container.timerManager.state.collect { state ->
                    if (state.phase != TimerPhase.BREAK || !state.isActive) {
                        openMainAndFinish()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isOpen = true
        // If Android restarted the process for this activity, keep the foreground
        // service and persistent notification alive for the rest of the break.
        container.timerManager.ensureServiceRunning()
        container.notificationHelper.cancelBreakAlert()
    }

    override fun onStop() {
        isOpen = false
        super.onStop()
    }

    override fun onDestroy() {
        isOpen = false
        super.onDestroy()
    }

    private fun openMainAndFinish() {
        if (closing) return
        closing = true
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
        )
        finish()
    }

    private fun applyWindowOptions(settings: AppSettings) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        if (settings.fullscreenBreak) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        if (settings.keepScreenAwakeDuringBreak) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    companion object {
        @Volatile
        var isOpen: Boolean = false
            private set
    }
}
