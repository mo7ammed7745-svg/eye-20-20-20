package com.mohamed.eye2020.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.mohamed.eye2020.data.settings.AppLanguage

/**
 * Forces the layout direction of everything below it from the language chosen inside the
 * app. The localized base context already reports the right direction; providing it here
 * as well makes the switch explicit and immediate, and it also covers overlays, dialogs
 * and the break activity that run in their own windows.
 */
@Composable
fun ProvideLanguageDirection(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (language.isArabic) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        },
        content = content,
    )
}
