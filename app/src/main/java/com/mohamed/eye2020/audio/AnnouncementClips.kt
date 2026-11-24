package com.mohamed.eye2020.audio

import com.mohamed.eye2020.data.settings.AppLanguage

/**
 * Raw resource names of the two spoken announcements. English and Arabic each ship their
 * own recording; the English clip stays as the fallback if an Arabic one is ever missing.
 */
object AnnouncementClips {
    const val BREAK_START = "time_to_rest"
    const val BREAK_START_ARABIC = "time_to_rest_ar"
    const val WORK_START = "time_to_work"
    const val WORK_START_ARABIC = "time_to_work_ar"

    fun breakStart(language: AppLanguage): String =
        if (language.isArabic) BREAK_START_ARABIC else BREAK_START

    fun workStart(language: AppLanguage): String =
        if (language.isArabic) WORK_START_ARABIC else WORK_START
}
