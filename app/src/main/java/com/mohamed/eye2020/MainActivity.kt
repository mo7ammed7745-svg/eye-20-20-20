package com.mohamed.eye2020

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.mohamed.eye2020.data.settings.AppLanguage
import com.mohamed.eye2020.ui.EyeApp
import com.mohamed.eye2020.ui.ProvideLanguageDirection
import com.mohamed.eye2020.ui.theme.EyeTheme
import com.mohamed.eye2020.util.AppLocale
import com.mohamed.eye2020.util.AppVisibility

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as EyeApplication).container

    private var permissionRefreshKey by mutableIntStateOf(0)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Forces the setup screen and Settings section to re-read the permission even
        // when the system dialog does not produce a separate ON_RESUME event.
        permissionRefreshKey++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyLayoutDirection()
        setContent {
            val language by container.languagePreferences.language.collectAsState()
            EyeTheme {
                ProvideLanguageDirection(language) {
                    EyeApp(
                        factory = EyeViewModelFactory(application as EyeApplication),
                        timerManager = container.timerManager,
                        setupPreferences = container.setupPreferences,
                        languagePreferences = container.languagePreferences,
                        onLanguageChange = ::changeLanguage,
                        onRequestNotificationPermission = ::requestNotificationPermission,
                        permissionRefreshKey = permissionRefreshKey,
                    )
                }
            }
        }
    }

    /**
     * The chosen language is applied to the base context before the first frame, so the
     * app never renders one frame in the wrong language or direction.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    private fun changeLanguage(language: AppLanguage) {
        container.languagePreferences.set(language)
        // Recreates this activity so the new locale is applied in attachBaseContext
        // before the first frame, with no half-translated flash.
        recreate()
    }

    /**
     * Mirrors the whole window, not just Compose content, so system-drawn parts such as
     * the status bar layout and IME hints follow Arabic too.
     */
    private fun applyLayoutDirection() {
        window.decorView.layoutDirection =
            if (container.languagePreferences.language.value.isArabic) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
    }

    override fun onResume() {
        super.onResume()
        container.timerManager.reconcileScreenState()
        container.timerManager.ensureServiceRunning()
        if (container.timerManager.isInBreak() && !BreakActivity.isOpen) {
            startActivity(
                Intent(this, BreakActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.onActivityStarted()
    }

    override fun onStop() {
        AppVisibility.onActivityStopped()
        super.onStop()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
