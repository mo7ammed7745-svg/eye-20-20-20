package com.mohamed.eye2020.data.settings

/**
 * All user-configurable behavior. Durations are stored in whole minutes/seconds so the
 * settings UI and persisted values stay simple and predictable.
 */
data class AppSettings(
    val workMinutes: Int = DEFAULT_WORK_MINUTES,
    val breakSeconds: Int = DEFAULT_BREAK_SECONDS,
    val screenOffGraceMinutes: Int = DEFAULT_GRACE_MINUTES,
    val autoStartNextWork: Boolean = true,
    val allowBreakSkipping: Boolean = false,
    val vibrateOnBreakStart: Boolean = true,
    val vibrateOnBreakEnd: Boolean = true,
    val voiceAnnouncements: Boolean = true,
    val announcementVolume: Float = DEFAULT_ANNOUNCEMENT_VOLUME,
    val keepScreenAwakeDuringBreak: Boolean = true,
    val fullscreenBreak: Boolean = true,
    /** Show a pop-up during the last seconds of focus, before the break starts. */
    val breakWarningEnabled: Boolean = true,
    /** How many seconds before the break that pop-up appears. */
    val breakWarningSeconds: Int = DEFAULT_BREAK_WARNING_SECONDS,
    /**
     * Third-party packages that get the quiet break: no full-screen break screen,
     * only a sound plus a small heads-up notification, while one of these apps is
     * in the foreground. An empty list means game mode is inactive.
     */
    val gameModeApps: List<String> = emptyList(),
) {
    val workMillis: Long get() = workMinutes * 60_000L
    val breakMillis: Long get() = breakSeconds * 1_000L
    val graceMillis: Long get() = screenOffGraceMinutes * 60_000L

    /** Zero when the warning is off, so callers can pass it straight to the policy. */
    val breakWarningMillis: Long
        get() = if (breakWarningEnabled) breakWarningSeconds * 1_000L else 0L

    /**
     * Never allow a corrupt or hand-edited preference to break the timer. Invalid values
     * fall back to the documented defaults; valid values are clamped to safe bounds.
     */
    fun validated(): AppSettings = copy(
        workMinutes = sanitize(workMinutes, LIMIT_WORK, DEFAULT_WORK_MINUTES),
        breakSeconds = sanitize(breakSeconds, LIMIT_BREAK, DEFAULT_BREAK_SECONDS),
        screenOffGraceMinutes = sanitize(
            screenOffGraceMinutes,
            LIMIT_GRACE,
            DEFAULT_GRACE_MINUTES,
        ),
        breakWarningSeconds = sanitize(
            breakWarningSeconds,
            LIMIT_BREAK_WARNING,
            DEFAULT_BREAK_WARNING_SECONDS,
        ),
        announcementVolume = if (announcementVolume.isNaN()) {
            DEFAULT_ANNOUNCEMENT_VOLUME
        } else {
            announcementVolume.coerceIn(0f, 1f)
        },
        gameModeApps = sanitizeGameApps(gameModeApps),
    )

    companion object {
        const val DEFAULT_WORK_MINUTES = 20
        const val DEFAULT_BREAK_SECONDS = 20
        const val DEFAULT_GRACE_MINUTES = 2
        const val DEFAULT_ANNOUNCEMENT_VOLUME = 0.7f
        const val DEFAULT_BREAK_WARNING_SECONDS = 10

        val LIMIT_WORK = 1..180
        val LIMIT_BREAK = 5..900
        val LIMIT_GRACE = 0..60
        val LIMIT_BREAK_WARNING = 5..60

        const val MAX_GAME_APPS = 50
        const val MAX_GAME_PACKAGE_LENGTH = 256

        private fun sanitize(value: Int, range: IntRange, default: Int): Int =
            if (value in range) value else default

        /**
         * Game-mode entries come from the package manager, but a hand-edited preference
         * file must not be able to stuff the list with junk: entry names are trimmed,
         * blanks and duplicates are dropped, and the list is capped and ordered so the
         * comparison against the foreground package stays deterministic.
         */
        private fun sanitizeGameApps(apps: List<String>): List<String> = apps.asSequence()
            .map { it.trim().take(MAX_GAME_PACKAGE_LENGTH) }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .take(MAX_GAME_APPS)
            .toList()
    }
}
