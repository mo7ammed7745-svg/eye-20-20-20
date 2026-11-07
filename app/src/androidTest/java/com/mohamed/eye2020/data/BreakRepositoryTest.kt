package com.mohamed.eye2020.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohamed.eye2020.data.db.EyeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BreakRepositoryTest {

    @Test
    fun duplicateAndInvalidRecordsAreNotCounted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, EyeDatabase::class.java).build()
        val now = System.currentTimeMillis()
        val repository = BreakRepository(database.breakRecordDao(), wallClock = { now })

        try {
            assertTrue(repository.recordCompletedBreak(now - 25_000L, now - 5_000L, 20))
            assertFalse(repository.recordCompletedBreak(now - 25_000L, now - 5_000L, 20))
            assertFalse(repository.recordCompletedBreak(now, now - 1_000L, 20))

            assertEquals(1, repository.observeAllTimeCount().first())

            val weekly = repository.observeWeeklyStats().first()
            assertEquals(1, weekly.todayCount)
            assertEquals(1, weekly.weeklyTotal)
        } finally {
            database.close()
        }
    }

    @Test
    fun completedBreaksAppearInTheWeeklyAndAllTimeTotals() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, EyeDatabase::class.java).build()
        val now = System.currentTimeMillis()
        val repository = BreakRepository(database.breakRecordDao(), wallClock = { now })

        try {
            repository.recordCompletedBreak(now - 60_000L, now - 40_000L, 20)
            repository.recordCompletedBreak(now - 120_000L, now - 100_000L, 20)
            repository.recordCompletedBreak(now - 180_000L, now - 160_000L, 20)

            assertEquals(3, repository.observeAllTimeCount().first())
            assertEquals(3, repository.observeWeeklyStats().first().todayCount)
        } finally {
            database.close()
        }
    }
}
