package com.mohamed.eye2020.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers the ceiling rounding and hour format that feed every visible countdown. */
class TimeFormatTest {

    @Test
    fun zeroShowsAsFullZeroClock() {
        assertEquals("00:00", TimeFormat.clock(0L))
    }

    @Test
    fun subSecondRoundsUpToOneSecond() {
        assertEquals("00:01", TimeFormat.clock(1L))
        assertEquals("00:01", TimeFormat.clock(999L))
        assertEquals("00:02", TimeFormat.clock(1_001L))
    }

    @Test
    fun negativeInputNeverShowsNegativeTime() {
        assertEquals("00:00", TimeFormat.clock(-5L))
    }

    @Test
    fun minutesAndSecondsAreSevenSegmentFormatted() {
        // Ceiling: 59.999 s counts as a full minute.
        assertEquals("01:00", TimeFormat.clock(59_999L))
        assertEquals("20:00", TimeFormat.clock(20 * 60_000L - 999L))
        assertEquals("01:05", TimeFormat.clock(65_000L))
    }

    @Test
    fun hoursUseColonSeparatedHours() {
        assertEquals("1:00:00", TimeFormat.clock(3_600_000L))
        assertEquals("1:01:01", TimeFormat.clock(3_661_000L))
        assertEquals("1:02:04", TimeFormat.clock(3_723_999L))
    }

    @Test
    fun singularAndPluralLabels() {
        assertEquals("1 minute", TimeFormat.minutesLabel(1))
        assertEquals("20 minutes", TimeFormat.minutesLabel(20))
        assertEquals("1 second", TimeFormat.secondsLabel(1))
        assertEquals("20 seconds", TimeFormat.secondsLabel(20))
    }
}
