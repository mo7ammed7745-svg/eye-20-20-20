package com.mohamed.eye2020.domain

import com.mohamed.eye2020.data.db.BreakRecord
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCalculatorTest {

    private val zone = ZoneId.of("UTC")
    private val now = LocalDate.of(2026, 11, 4) // Wednesday
        .atTime(12, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    private fun record(date: LocalDate, hour: Int = 12, id: Long = 0): BreakRecord {
        val completed = date.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()
        return BreakRecord(
            id = id,
            sessionStartedAtMillis = completed - 20_000L,
            completedAtMillis = completed,
            durationSeconds = 20,
        )
    }

    @Test
    fun weekRunsMondayThroughSundayAndCountsOnlyThatWeek() {
        val monday = LocalDate.of(2026, 11, 2)
        val records = listOf(
            record(monday, id = 1),
            record(monday, hour = 9, id = 2),
            record(monday.plusDays(1), id = 3),
            record(monday.plusDays(2), id = 4),
            record(monday.plusDays(2), hour = 14, id = 5),
            record(monday.plusDays(2), hour = 18, id = 6),
            record(monday.plusDays(6), id = 7),
            record(monday.minusDays(1), id = 8),
            record(monday.minusDays(3), id = 9),
            record(monday.plusDays(7), id = 10),
        )

        val stats = StatsCalculator.calculate(records, now, zone)

        assertEquals(monday, stats.weekStart)
        assertEquals(monday.plusDays(6), stats.weekEnd)
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), stats.days.map { it.label })
        assertEquals(listOf(2, 1, 3, 0, 0, 0, 1), stats.days.map { it.count })
        assertEquals(3, stats.todayCount)
        assertEquals(7, stats.weeklyTotal)
    }

    @Test
    fun emptyWeekIsAllZeroes() {
        val stats = StatsCalculator.calculate(emptyList(), now, zone)

        assertEquals(7, stats.days.size)
        assertEquals(0, stats.todayCount)
        assertEquals(0, stats.weeklyTotal)
        assertEquals(0, stats.days.sumOf { it.count })
    }

    @Test
    fun weekStartOnSundayIsThePreviousMonday() {
        assertEquals(
            LocalDate.of(2026, 11, 2),
            StatsCalculator.weekStart(LocalDate.of(2026, 11, 8)),
        )
    }

    @Test
    fun countsUseTheSuppliedTimeZoneForDayBoundaries() {
        val riyadh = ZoneId.of("Asia/Riyadh")
        val monday = LocalDate.of(2026, 11, 2)
        val completed = monday.atTime(0, 30).atZone(riyadh).toInstant().toEpochMilli()
        val record = BreakRecord(
            sessionStartedAtMillis = completed - 20_000L,
            completedAtMillis = completed,
            durationSeconds = 20,
        )
        val mondayNoon = monday.atTime(12, 0).atZone(riyadh).toInstant().toEpochMilli()

        val stats = StatsCalculator.calculate(listOf(record), mondayNoon, riyadh)

        assertEquals(1, stats.days.first { it.date == monday }.count)
        assertEquals(1, stats.todayCount)
    }

    @Test
    fun weekStartMillisMatchesTheWindowUsedByCalculate() {
        // The repository relies on weekStartMillis to pick which records to load;
        // if it drifts from calculate's own window, entries vanish from the chart.
        val startMillis = StatsCalculator.weekStartMillis(now, zone)

        val stats = StatsCalculator.calculate(
            records = listOf(
                record(LocalDate.of(2026, 11, 2), hour = 0, id = 1),
                record(LocalDate.of(2026, 11, 8), hour = 23, id = 2),
                record(LocalDate.of(2026, 11, 1), id = 3),
                record(LocalDate.of(2026, 11, 9), id = 4),
            ),
            nowMillis = now,
            zone = zone,
        )

        val counted = stats.days.sumOf { it.count }
        assertEquals(2, counted)
        assertEquals(
            LocalDate.of(2026, 11, 2).atStartOfDay(zone).toInstant().toEpochMilli(),
            startMillis,
        )
    }
}
