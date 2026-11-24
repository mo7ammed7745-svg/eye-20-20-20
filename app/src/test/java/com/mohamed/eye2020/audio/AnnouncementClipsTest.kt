package com.mohamed.eye2020.audio

import com.mohamed.eye2020.data.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AnnouncementClipsTest {

    @Test
    fun `english uses the english recordings`() {
        assertEquals("time_to_rest", AnnouncementClips.breakStart(AppLanguage.ENGLISH))
        assertEquals("time_to_work", AnnouncementClips.workStart(AppLanguage.ENGLISH))
    }

    @Test
    fun `arabic uses the arabic recordings`() {
        assertEquals("time_to_rest_ar", AnnouncementClips.breakStart(AppLanguage.ARABIC))
        assertEquals("time_to_work_ar", AnnouncementClips.workStart(AppLanguage.ARABIC))
    }

    @Test
    fun `fallback names stay valid raw resource names`() {
        listOf(
            AnnouncementClips.BREAK_START,
            AnnouncementClips.BREAK_START_ARABIC,
            AnnouncementClips.WORK_START,
            AnnouncementClips.WORK_START_ARABIC,
        ).forEach { name ->
            assertEquals(name.lowercase(), name)
            assert(name.all { it.isLetterOrDigit() || it == '_' }) { "invalid raw name: $name" }
        }
    }
}
