package com.mohamed.eye2020.timer

import android.os.SystemClock

interface Clock {
    fun realtimeMillis(): Long
    fun wallTimeMillis(): Long
}

/** elapsedRealtime includes deep sleep and is immune to wall-clock changes. */
object SystemClockSource : Clock {
    override fun realtimeMillis(): Long = SystemClock.elapsedRealtime()
    override fun wallTimeMillis(): Long = System.currentTimeMillis()
}
