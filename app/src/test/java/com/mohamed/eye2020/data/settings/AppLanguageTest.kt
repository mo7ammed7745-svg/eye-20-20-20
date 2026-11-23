package com.mohamed.eye2020.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `stored tags map back to their language`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag("ar"))
    }

    @Test
    fun `tags are matched case insensitively`() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag("AR"))
    }

    @Test
    fun `missing or unknown values fall back to no choice`() {
        assertNull(AppLanguage.fromTag(null))
        assertNull(AppLanguage.fromTag(""))
        assertNull(AppLanguage.fromTag("fr"))
    }

    @Test
    fun `only arabic is right to left`() {
        assertEquals(true, AppLanguage.ARABIC.isArabic)
        assertEquals(false, AppLanguage.ENGLISH.isArabic)
    }
}
