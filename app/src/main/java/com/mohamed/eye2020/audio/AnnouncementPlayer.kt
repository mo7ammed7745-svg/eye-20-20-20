package com.mohamed.eye2020.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Application-scoped player for the two optional voice recordings. Resources are looked up
 * by name so the app still builds and runs when the MP3 files have not been added.
 * Decoding happens on a background dispatcher; [MediaPlayer.create] reads and prepares a
 * file, which is far too slow for the main thread.
 */
class AnnouncementPlayer(
    context: Context,
    private val playScope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()

    @Volatile
    private var activePlayer: MediaPlayer? = null

    fun playBreakStart(volume: Float) = play(RAW_BREAK_START, volume)

    fun playWorkStart(volume: Float) = play(RAW_WORK_START, volume)

    private fun play(rawName: String, volume: Float) {
        val safeVolume = if (volume.isNaN()) 1f else volume.coerceIn(0f, 1f)
        playScope.launch {
            val prepared = withContext(Dispatchers.IO) {
                val resId = appContext.resources.getIdentifier(
                    rawName,
                    "raw",
                    appContext.packageName,
                )
                if (resId == 0) {
                    Log.w(TAG, "Missing res/raw/$rawName.mp3; announcement skipped")
                    return@withContext null
                }
                releaseActive()
                try {
                    MediaPlayer.create(appContext, resId)
                } catch (error: Exception) {
                    Log.w(TAG, "Unable to open res/raw/$rawName.mp3", error)
                    null
                }
            } ?: return@launch

            activate(prepared)
            runCatching {
                prepared.setVolume(safeVolume, safeVolume)
                prepared.setOnCompletionListener { finished ->
                    mainHandler.post { release(finished) }
                }
                prepared.setOnErrorListener { failed, what, extra ->
                    Log.w(TAG, "Announcement playback failed ($what/$extra)")
                    mainHandler.post { release(failed) }
                    true
                }
                prepared.start()
            }.onFailure { error ->
                Log.w(TAG, "Unable to play res/raw/$rawName.mp3", error)
                release(prepared)
            }
        }
    }

    private fun activate(player: MediaPlayer) = synchronized(lock) {
        activePlayer = player
    }

    private fun releaseActive() = synchronized(lock) {
        activePlayer?.let { release(it) }
        activePlayer = null
    }

    private fun release(player: MediaPlayer) = synchronized(lock) {
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
