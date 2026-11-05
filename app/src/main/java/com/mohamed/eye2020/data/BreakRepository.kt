package com.mohamed.eye2020.data

import com.mohamed.eye2020.data.db.BreakRecord
import com.mohamed.eye2020.data.db.BreakRecordDao
import com.mohamed.eye2020.domain.StatsCalculator
import com.mohamed.eye2020.domain.WeeklyStats
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BreakRepository(private val dao: BreakRecordDao) {

    fun observeWeeklyStats(nowMillis: Long = System.currentTimeMillis()): Flow<WeeklyStats> {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val weekStart = StatsCalculator.weekStart(today)
        val startMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeSince(startMillis).map { records ->
            StatsCalculator.calculate(records, nowMillis, zone)
        }
    }

    fun observeAllTimeCount(): Flow<Int> = dao.observeTotalCount()

    /**
     * Records a break only after its countdown reached zero. The unique session-start
     * index makes this idempotent if the service is recreated at the wrong moment.
     */
    suspend fun recordCompletedBreak(
        sessionStartedAtMillis: Long,
        completedAtMillis: Long,
        durationSeconds: Int,
    ): Boolean {
        if (completedAtMillis < sessionStartedAtMillis || durationSeconds <= 0) return false
        val rowId = dao.insert(
            BreakRecord(
                sessionStartedAtMillis = sessionStartedAtMillis,
                completedAtMillis = completedAtMillis,
                durationSeconds = durationSeconds,
            ),
        )
        return rowId != -1L
    }
}
