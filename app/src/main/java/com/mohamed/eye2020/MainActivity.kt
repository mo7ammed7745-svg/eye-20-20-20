package com.mohamed.eye2020

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.mohamed.eye2020.ui.EyeApp
import com.mohamed.eye2020.ui.theme.EyeTheme

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
        setContent {
            EyeTheme {
                EyeApp(
                    factory = EyeViewModelFactory(application as EyeApplication),
                    timerManager = container.timerManager,
                    setupPreferences = container.setupPreferences,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    permissionRefreshKey = permissionRefreshKey,
                )
            }
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
