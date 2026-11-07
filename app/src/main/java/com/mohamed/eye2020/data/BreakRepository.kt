package com.mohamed.eye2020.data

import com.mohamed.eye2020.data.db.BreakRecord
import com.mohamed.eye2020.data.db.BreakRecordDao
import com.mohamed.eye2020.domain.StatsCalculator
import com.mohamed.eye2020.domain.WeeklyStats
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class BreakRepository(
    private val dao: BreakRecordDao,
    /** Injectable so tests can pin the wall clock to fixed record timestamps. */
    private val wallClock: () -> Long = System::currentTimeMillis,
) {

    /**
     * A weekly summary that stays current while the UI stays open. Break records drive
     * the immediate update; a day-change re-anchor keeps "today" and the Monday–Sunday
     * window from freezing on yesterday's week after midnight.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeWeeklyStats(): Flow<WeeklyStats> {
        val zone = ZoneId.systemDefault()
        return dayChangeTicker(zone)
            .flatMapLatest {
                dao.observeSince(StatsCalculator.weekStartMillis(wallClock(), zone))
                    .map { records -> StatsCalculator.calculate(records, wallClock(), zone) }
            }
            .distinctUntilChanged()
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

    /**
     * Emits once immediately, then again shortly after each midnight, so a stats screen
     * left open across midnight re-reads the just-changed week window.
     */
    private fun dayChangeTicker(zone: ZoneId): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            val now = Instant.ofEpochMilli(wallClock())
            val nextMidnight = now.atZone(zone)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
            val delayMillis = Duration.between(now, nextMidnight).toMillis() + 1_000L
            delay(delayMillis.coerceIn(1_000L, Duration.ofDays(2).toMillis()))
        }
    }
}
