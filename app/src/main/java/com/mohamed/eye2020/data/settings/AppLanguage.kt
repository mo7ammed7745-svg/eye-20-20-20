package com.mohamed.eye2020.data.settings

import java.util.Locale

/**
 * The languages the app is translated into. The chosen value drives the whole app on its
 * own, so the phone's system language does not have to be changed for the app to switch.
 */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    ARABIC("ar"),
    ;

    val isArabic: Boolean get() = this == ARABIC

    companion object {
        /** The language stored under [tag], or null when nothing (valid) was stored yet. */
        fun fromTag(tag: String?): AppLanguage? =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }

        /** First-launch default: the phone's language when it is Arabic, English otherwise. */
        fun deviceDefault(): AppLanguage =
            if (Locale.getDefault().language == ARABIC.tag) ARABIC else ENGLISH
    }
}
