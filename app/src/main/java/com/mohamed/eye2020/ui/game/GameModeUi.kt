package com.mohamed.eye2020.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
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
    return produceState<ImageBitmap?>(
        initialValue = iconBitmapCache[packageName],
        packageName,
        controller,
    ) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            iconBitmapCache[packageName] ?: runCatching {
                controller.iconFor(packageName)?.toBitmap(ICON_PIXELS, ICON_PIXELS)?.asImageBitmap()
            }.getOrNull()?.also { bitmap ->
                // Small bounded cache: scrolling back up, or reopening the picker, reuses
                // the decoded icon instead of asking the package manager again.
                if (iconBitmapCache.size >= ICON_CACHE_LIMIT) iconBitmapCache.clear()
                iconBitmapCache[packageName] = bitmap
            }
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
 * Full-screen app chooser. The apps already picked sit in their own card at the top, so
 * the selection can be reviewed and trimmed at a glance; below it every installed
 * launcher app is listed with only its icon and name. Selection changes are kept in a
 * pending set and saved atomically on Done.
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
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
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
                Spacer(Modifier.width(4.dp))
                Button(onClick = { onConfirm(pendingSelection.toList()) }) {
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

            val allApps = apps
            val visibleApps = remember(allApps, query) {
                allApps?.let { GameModePolicy.filterApps(it, query) }.orEmpty()
            }
            val selectionFull = pendingSelection.size >= AppSettings.MAX_GAME_APPS
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

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                ) {
                    if (pendingSelection.isNotEmpty()) {
                        item(key = "selected") {
                            SelectedAppsSection(
                                packages = pendingSelection.toList(),
                                onRemove = { packageName ->
                                    pendingSelection = pendingSelection - packageName
                                },
                            )
                        }
                    }
                    if (selectionFull) {
                        item(key = "limit") {
                            Text(
                                text = stringResource(
                                    R.string.game_mode_apps_limit,
                                    AppSettings.MAX_GAME_APPS,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(
                                    horizontal = 24.dp,
                                    vertical = 8.dp,
                                ),
                            )
                        }
                    }
                    if (visibleApps.isEmpty()) {
                        item(key = "no_results") {
                            EmptyPickerMessage(
                                text = stringResource(
                                    R.string.game_mode_no_results,
                                    query.trim(),
                                ),
                            )
                        }
                    } else {
                        items(visibleApps, key = { it.packageName }) { app ->
                            val checked = app.packageName in pendingSelection
                            GameAppRow(
                                entry = app,
                                checked = checked,
                                enabled = checked || !selectionFull,
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
}

/**
 * The picked apps, on a single horizontally scrolling row of chips. Each chip carries
 * the app icon, its name and its own remove button.
 */
@Composable
private fun SelectedAppsSection(
    packages: List<String>,
    onRemove: (String) -> Unit,
) {
    val labels = rememberGameAppLabels(packages)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.game_mode_selected_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.game_mode_selected_count, packages.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (packages.size > 1) {
                        TextButton(onClick = { packages.forEach(onRemove) }) {
                            Text(stringResource(R.string.game_mode_clear_all))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(packages, key = { it }) { packageName ->
                        SelectedAppChip(
                            entry = GameAppEntry(
                                packageName = packageName,
                                label = labels[packageName] ?: packageName,
                            ),
                            onRemove = { onRemove(packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedAppChip(
    entry: GameAppEntry,
    onRemove: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameAppIcon(entry = entry, size = 28)
            Spacer(Modifier.width(8.dp))
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp),
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(
                        R.string.game_mode_remove_app,
                        entry.label,
                    ),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

/**
 * One installed app: icon and name only, with a check mark that fills in when the app is
 * picked. The package name is deliberately not shown; search still matches it.
 */
@Composable
private fun GameAppRow(
    entry: GameAppEntry,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (GameAppEntry) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (checked) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    Color.Transparent
                },
            )
            .clickable(enabled = enabled, role = Role.Checkbox) { onToggle(entry) }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameAppIcon(entry = entry)
        Spacer(Modifier.width(14.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        SelectionMark(checked = checked)
    }
}

/** Round selection indicator: an empty outline until the app is picked, then a tick. */
@Composable
private fun SelectionMark(checked: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) accent else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (checked) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private const val ICON_PIXELS = 96

/** Decoded launcher icons, shared by the home card, the settings list and the picker. */
private val iconBitmapCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

private const val ICON_CACHE_LIMIT = 120
