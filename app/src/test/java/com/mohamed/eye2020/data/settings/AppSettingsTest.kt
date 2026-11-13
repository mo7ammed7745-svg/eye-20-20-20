package com.mohamed.eye2020.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun defaultsMatchTheTwentyTwentyTwentyRule() {
        val settings = AppSettings()

        assertEquals(20, settings.workMinutes)
        assertEquals(20, settings.breakSeconds)
        assertEquals(2, settings.screenOffGraceMinutes)
        assertTrue(settings.autoStartNextWork)
        // Skipping a break is an explicit opt-in; the default must protect the break.
        assertTrue(!settings.allowBreakSkipping)
        assertTrue(settings.vibrateOnBreakStart)
        assertTrue(settings.vibrateOnBreakEnd)
        assertTrue(settings.voiceAnnouncements)
        assertEquals(0.7f, settings.announcementVolume, 0f)
        assertEquals(20 * 60_000L, settings.workMillis)
        assertEquals(20_000L, settings.breakMillis)
        assertEquals(120_000L, settings.graceMillis)
    }

    @Test
    fun invalidPersistedValuesFallBackToDefaults() {
        val settings = AppSettings(
            workMinutes = 0,
            breakSeconds = 1,
            screenOffGraceMinutes = 500,
        ).validated()

        assertEquals(20, settings.workMinutes)
        assertEquals(20, settings.breakSeconds)
        assertEquals(2, settings.screenOffGraceMinutes)
    }

    @Test
    fun announcementVolumeIsClampedAndNaNFallsBackToFullVolume() {
        assertEquals(
            0f,
            AppSettings(announcementVolume = -0.5f).validated().announcementVolume,
            0f,
        )
        assertEquals(
            1f,
            AppSettings(announcementVolume = 2f).validated().announcementVolume,
            0f,
        )
        assertEquals(
            AppSettings.DEFAULT_ANNOUNCEMENT_VOLUME,
            AppSettings(announcementVolume = Float.NaN).validated().announcementVolume,
            0f,
        )
    }

    @Test
    fun boundaryValuesAreAccepted() {
        val settings = AppSettings(
            workMinutes = 180,
            breakSeconds = 900,
            screenOffGraceMinutes = 0,
        ).validated()

        assertEquals(180, settings.workMinutes)
        assertEquals(900, settings.breakSeconds)
        assertEquals(0, settings.screenOffGraceMinutes)
    }

    @Test
    fun gameModeAppsAreTrimmedDedupedSortedAndCapped() {
        val settings = AppSettings(
            gameModeApps = listOf(
                "  com.example.game  ",
                "",
                "com.example.game",
                "com.other.app",
            ),
        ).validated()

        assertEquals(listOf("com.example.game", "com.other.app"), settings.gameModeApps)
    }

    @Test
    fun gameModeAppsRespectTheHardCap() {
        val many = List(60) { "com.example.app$it" }
        val settings = AppSettings(gameModeApps = many).validated()

        assertEquals(AppSettings.MAX_GAME_APPS, settings.gameModeApps.size)
    }

    @Test
    fun quietBreakOnlyWhenListedAppIsForeground() {
        val apps = listOf("com.example.game")
        val policy = com.mohamed.eye2020.game.GameModePolicy

        assertEquals(false, policy.shouldUseQuietBreak(apps, null))
        assertEquals(false, policy.shouldUseQuietBreak(emptyList(), "com.example.game"))
        assertEquals(false, policy.shouldUseQuietBreak(apps, "com.other.app"))
        assertEquals(true, policy.shouldUseQuietBreak(apps, "com.example.game"))
    }
}
