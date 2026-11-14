package com.mohamed.eye2020.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mohamed.eye2020.R
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.game.GameAppEntry
import com.mohamed.eye2020.game.GameModeController

/**
 * Shared pieces of the game-mode interface, used from both the home screen card and
 * the settings section: the app picker, the usage-access grant state, and the
 * package-name to friendly-app-name lookup.
 */

/** Recenters the usage-access grant each time the user returns to the app. */
@Composable
fun rememberUsageAccessGranted(): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(GameModeController(context).isUsageAccessGranted()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = GameModeController(context).isUsageAccessGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

/** package name -> installed app label ("Candy Crush"), falling back to the raw name. */
@Composable
fun rememberGameAppLabels(packages: List<String>): Map<String, String> {
    val context = LocalContext.current
    return remember(packages) {
        val controller = GameModeController(context)
        packages.associateWith { controller.labelFor(it) }
    }
}

/**
 * Full-screen app chooser shown over the screen it was opened from. Selection changes
 * are written into a pending set, then saved atomically on Done.
 */
@Composable
fun GameAppPickerOverlay(
    selected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { GameModeController(context) }
    var apps by remember { mutableStateOf(controller.launchableApps()) }
    var pendingSelection by remember(selected) { mutableStateOf(selected.toSet()) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.game_mode_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onDismiss() }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = { onConfirm(pendingSelection.toList()) }) {
                    Text(stringResource(R.string.game_mode_done))
                }
            }
            HorizontalDivider()
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.game_mode_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName }) { app ->
                    val checked = app.packageName in pendingSelection
                    GameAppRow(
                        entry = app,
                        checked = checked,
                        onToggle = { entry ->
                            pendingSelection = if (entry.packageName in pendingSelection) {
                                pendingSelection - entry.packageName
                            } else if (pendingSelection.size < AppSettings.MAX_GAME_APPS) {
                                pendingSelection + entry.packageName
                            } else {
                                pendingSelection
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GameAppRow(
    entry: GameAppEntry,
    checked: Boolean,
    onToggle: (GameAppEntry) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) { onToggle(entry) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(16.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}
