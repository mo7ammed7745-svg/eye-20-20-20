package com.mohamed.eye2020.domain

import com.mohamed.eye2020.data.db.BreakRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class DayStat(
    val date: LocalDate,
    val label: String,
    val count: Int,
)

data class WeeklyStats(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val days: List<DayStat>,
    val todayCount: Int,
    val weeklyTotal: Int,
) {
    val startMillis: Long
        get() = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val endExclusiveMillis: Long
        get() = weekEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/**
 * Pure date math so the weekly report can be tested without Android or a database.
 * Weeks run Monday through Sunday.
 */
object StatsCalculator {

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun calculate(
        records: List<BreakRecord>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): WeeklyStats {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val weekStart = weekStart(today)
        val weekEnd = weekStart.plusDays(6)
        val startMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = weekEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val counts = records
            .asSequence()
            .filter { it.completedAtMillis in startMillis until endMillis }
            .groupingBy {
                Instant.ofEpochMilli(it.completedAtMillis).atZone(zone).toLocalDate()
            }
            .eachCount()

        val days = (0L..6L).map { offset ->
            val date = weekStart.plusDays(offset)
            DayStat(
                date = date,
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                count = counts[date] ?: 0,
            )
        }

        return WeeklyStats(
            weekStart = weekStart,
            weekEnd = weekEnd,
            days = days,
            todayCount = counts[today] ?: 0,
            weeklyTotal = days.sumOf { it.count },
        )
    }
}
