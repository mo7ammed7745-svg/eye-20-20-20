package com.mohamed.eye2020.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mohamed.eye2020.util.PermissionStatus

/**
 * Re-reads Android permission state whenever the screen resumes, which covers returning
 * from a system settings page. [refreshKey] additionally covers runtime permission
 * dialogs whose result may arrive without a full lifecycle resume.
 */
@Composable
fun rememberPermissionSnapshot(refreshKey: Any? = null): PermissionStatus {
    val context = LocalContext.current
    var status by remember(context) { mutableStateOf(PermissionStatus.read(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = PermissionStatus.read(context)
    }
    LaunchedEffect(refreshKey) {
        status = PermissionStatus.read(context)
    }

    return status
}
