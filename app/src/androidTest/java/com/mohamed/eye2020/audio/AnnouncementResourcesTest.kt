package com.mohamed.eye2020.audio

import android.media.MediaPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mohamed.eye2020.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the recordings themselves: both languages must decode and report a sensible
 * duration, so a corrupt or accidentally dropped MP3 fails the build instead of silently
 * leaving a break without an announcement.
 */
@RunWith(AndroidJUnit4::class)
class AnnouncementResourcesTest {

    @Test
    fun everyAnnouncementDecodes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clips = listOf(
            "english break" to R.raw.time_to_rest,
            "english work" to R.raw.time_to_work,
            "arabic break" to R.raw.time_to_rest_ar,
            "arabic work" to R.raw.time_to_work_ar,
        )
        for ((label, resId) in clips) {
            val player = MediaPlayer.create(context, resId)
            assertNotNull("$label clip did not decode", player)
            try {
                assertTrue("$label clip is empty", player!!.duration > 500)
            } finally {
                player.release()
            }
        }
    }
}
