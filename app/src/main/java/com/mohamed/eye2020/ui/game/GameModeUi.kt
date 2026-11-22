package com.mohamed.eye2020.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mohamed.eye2020.R
import com.mohamed.eye2020.data.settings.AppSettings
import com.mohamed.eye2020.game.GameAppEntry
import com.mohamed.eye2020.game.GameModeController
import com.mohamed.eye2020.game.GameModePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared pieces of the game-mode interface, used from both the home screen card and
 * the settings section: the app picker, the usage-access grant state, the app icon and
 * the package-name to friendly-app-name lookup.
 */

@Composable
fun rememberGameModeController(): GameModeController {
    val context = LocalContext.current
    return remember(context) { GameModeController(context) }
}

/** Recenters the usage-access grant each time the user returns to the app. */
@Composable
fun rememberUsageAccessGranted(): Boolean {
    val controller = rememberGameModeController()
    var granted by remember { mutableStateOf(controller.isUsageAccessGranted()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = controller.isUsageAccessGranted()
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
    val controller = rememberGameModeController()
    return remember(packages, controller) {
        packages.associateWith { controller.labelFor(it) }
    }
}

/**
 * The real launcher icon of an app, loaded off the main thread and cached by the
 * controller. Null while loading or when the app exposes no icon.
 */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val controller = rememberGameModeController()
    return produceState<ImageBitmap?>(initialValue = null, packageName, controller) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                controller.iconFor(packageName)?.toBitmap(ICON_PIXELS, ICON_PIXELS)?.asImageBitmap()
            }.getOrNull()
        }
    }.value
}

/**
 * One app icon in a rounded tile, with a letter placeholder while the real icon loads
 * or when the app is no longer installed. This is what makes the picker look like the
 * phone's app drawer instead of a plain checklist.
 */
@Composable
fun GameAppIcon(
    entry: GameAppEntry,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val icon = rememberAppIcon(entry.packageName)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = entry.label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Full-screen app chooser shown over the screen it was opened from. Every installed
 * launcher app appears with its icon and name, a search box narrows the list, and
 * selection changes are written into a pending set, then saved atomically on Done.
 */
@Composable
fun GameAppPickerOverlay(
    selected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val controller = rememberGameModeController()
    var apps by remember { mutableStateOf<List<GameAppEntry>?>(null) }
    var pendingSelection by remember(selected) { mutableStateOf(selected.toSet()) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(controller) {
        apps = withContext(Dispatchers.IO) { controller.launchableApps() }
    }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.game_mode_add_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.game_mode_selected_count,
                            pendingSelection.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onDismiss() }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = { onConfirm(pendingSelection.toList()) }) {
                    Text(stringResource(R.string.game_mode_done))
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.game_mode_clear_search),
                            )
                        }
                    }
                } else {
                    null
                },
                placeholder = { Text(stringResource(R.string.game_mode_search_hint)) },
            )

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            val allApps = apps
            val visibleApps = remember(allApps, query) {
                allApps?.let { GameModePolicy.filterApps(it, query) }.orEmpty()
            }
            when {
                allApps == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                allApps.isEmpty() -> EmptyPickerMessage(
                    text = stringResource(R.string.game_mode_none),
                )

                visibleApps.isEmpty() -> EmptyPickerMessage(
                    text = stringResource(R.string.game_mode_no_results, query.trim()),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        val checked = app.packageName in pendingSelection
                        GameAppRow(
                            entry = app,
                            checked = checked,
                            onToggle = { entry ->
                                pendingSelection = when {
                                    entry.packageName in pendingSelection ->
                                        pendingSelection - entry.packageName

                                    pendingSelection.size < AppSettings.MAX_GAME_APPS ->
                                        pendingSelection + entry.packageName

                                    else -> pendingSelection
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPickerMessage(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameAppIcon(entry = entry)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

private const val ICON_PIXELS = 96
