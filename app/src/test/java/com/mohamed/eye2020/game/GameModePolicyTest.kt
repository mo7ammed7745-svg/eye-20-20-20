package com.mohamed.eye2020.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModePolicyTest {

    private val apps = listOf(
        GameAppEntry("com.google.android.youtube", "YouTube"),
        GameAppEntry("com.king.candycrushsaga", "Candy Crush Saga"),
        GameAppEntry("com.mojang.minecraftpe", "Minecraft"),
    )

    @Test
    fun quietBreakOnlyForListedForegroundApp() {
        assertTrue(
            GameModePolicy.shouldUseQuietBreak(
                gameModeApps = listOf("com.mojang.minecraftpe"),
                foregroundPackage = "com.mojang.minecraftpe",
            ),
        )
        assertFalse(
            GameModePolicy.shouldUseQuietBreak(
                gameModeApps = listOf("com.mojang.minecraftpe"),
                foregroundPackage = "com.google.android.youtube",
            ),
        )
        assertFalse(
            GameModePolicy.shouldUseQuietBreak(
                gameModeApps = emptyList(),
                foregroundPackage = "com.mojang.minecraftpe",
            ),
        )
        assertFalse(
            GameModePolicy.shouldUseQuietBreak(
                gameModeApps = listOf("com.mojang.minecraftpe"),
                foregroundPackage = null,
            ),
        )
    }

    @Test
    fun emptyQueryKeepsEveryApp() {
        assertEquals(apps, GameModePolicy.filterApps(apps, ""))
        assertEquals(apps, GameModePolicy.filterApps(apps, "   "))
    }

    @Test
    fun searchMatchesLabelIgnoringCase() {
        assertEquals(
            listOf(apps[1]),
            GameModePolicy.filterApps(apps, "candy"),
        )
        assertEquals(
            listOf(apps[2]),
            GameModePolicy.filterApps(apps, "MINECRAFT"),
        )
    }

    @Test
    fun searchAlsoMatchesPackageName() {
        assertEquals(
            listOf(apps[0]),
            GameModePolicy.filterApps(apps, "youtube"),
        )
        assertEquals(
            listOf(apps[2]),
            GameModePolicy.filterApps(apps, "mojang"),
        )
    }

    @Test
    fun searchWithoutMatchReturnsEmptyList() {
        assertTrue(GameModePolicy.filterApps(apps, "no such app").isEmpty())
    }
}
