package com.mohamed.eye2020.util

import android.content.Context
import android.content.res.Configuration
import com.mohamed.eye2020.data.settings.AppLanguage
import com.mohamed.eye2020.data.settings.LanguagePreferences
import java.util.Locale

/**
 * Applies the language chosen inside the app. Both activities wrap their base context
 * before anything is drawn, so text, RTL layout and the Arabic typography all come from
 * the same configuration, on every Android version this app supports.
 */
object AppLocale {

    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(LanguagePreferences.storedLanguage(base).tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }

    /** Makes Java date/number formatting follow the app language instead of the phone's. */
    fun applyDefault(context: Context) {
        Locale.setDefault(Locale.forLanguageTag(LanguagePreferences.storedLanguage(context).tag))
    }
}
