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
    val vibrateOnBreakStart: Boolean = true,
    val vibrateOnBreakEnd: Boolean = true,
    val voiceAnnouncements: Boolean = true,
    val announcementVolume: Float = DEFAULT_ANNOUNCEMENT_VOLUME,
    val keepScreenAwakeDuringBreak: Boolean = true,
    val fullscreenBreak: Boolean = true,
) {
    val workMillis: Long get() = workMinutes * 60_000L
    val breakMillis: Long get() = breakSeconds * 1_000L
    val graceMillis: Long get() = screenOffGraceMinutes * 60_000L

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
        announcementVolume = if (announcementVolume.isNaN()) {
            DEFAULT_ANNOUNCEMENT_VOLUME
        } else {
            announcementVolume.coerceIn(0f, 1f)
        },
    )

    companion object {
        const val DEFAULT_WORK_MINUTES = 20
        const val DEFAULT_BREAK_SECONDS = 20
        const val DEFAULT_GRACE_MINUTES = 2
        const val DEFAULT_ANNOUNCEMENT_VOLUME = 0.7f

        val LIMIT_WORK = 1..180
        val LIMIT_BREAK = 5..900
        val LIMIT_GRACE = 0..60

        private fun sanitize(value: Int, range: IntRange, default: Int): Int =
            if (value in range) value else default
    }
}
