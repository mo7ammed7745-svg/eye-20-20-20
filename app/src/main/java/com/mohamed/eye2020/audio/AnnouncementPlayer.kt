package com.mohamed.eye2020.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Application-scoped player for the two optional voice recordings. Resources are looked up
 * by name so the app still builds and runs when the MP3 files have not been added.
 */
class AnnouncementPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activePlayer: MediaPlayer? = null

    fun playBreakStart(volume: Float) = play(RAW_BREAK_START, volume)

    fun playWorkStart(volume: Float) = play(RAW_WORK_START, volume)

    private fun play(rawName: String, volume: Float) {
        val safeVolume = if (volume.isNaN()) 1f else volume.coerceIn(0f, 1f)
        mainHandler.post {
            val resId = appContext.resources.getIdentifier(
                rawName,
                "raw",
                appContext.packageName,
            )
            if (resId == 0) {
                Log.w(TAG, "Missing res/raw/$rawName.mp3; announcement skipped")
                return@post
            }

            releaseActive()
            val player = try {
                MediaPlayer.create(appContext, resId)
            } catch (error: Exception) {
                Log.w(TAG, "Unable to open res/raw/$rawName.mp3", error)
                null
            } ?: return@post

            activePlayer = player
            runCatching {
                player.setVolume(safeVolume, safeVolume)
                player.setOnCompletionListener { finished -> release(finished) }
                player.setOnErrorListener { failed, what, extra ->
                    Log.w(TAG, "Announcement playback failed ($what/$extra)")
                    release(failed)
                    true
                }
                player.start()
            }.onFailure { error ->
                Log.w(TAG, "Unable to play res/raw/$rawName.mp3", error)
                release(player)
            }
        }
    }

    private fun releaseActive() {
        activePlayer?.let { release(it) }
    }

    private fun release(player: MediaPlayer) {
        if (activePlayer === player) {
            activePlayer = null
        }
        runCatching { player.release() }
    }

    private companion object {
        const val TAG = "AnnouncementPlayer"
        const val RAW_BREAK_START = "time_to_rest"
        const val RAW_WORK_START = "time_to_work"
    }
}
