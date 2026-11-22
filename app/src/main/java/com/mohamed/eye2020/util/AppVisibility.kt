package com.mohamed.eye2020.util

/**
 * Tracks whether one of the app's screens is visible. The pre-break warning uses it to
 * avoid showing the same countdown twice: inside the app the slide-down banner is shown,
 * outside the app a heads-up notification is posted instead.
 */
object AppVisibility {

    @Volatile
    var isForeground: Boolean = false
        private set

    fun onActivityStarted() {
        isForeground = true
    }

    fun onActivityStopped() {
        isForeground = false
    }
}
