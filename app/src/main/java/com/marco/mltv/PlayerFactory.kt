package com.marco.mltv

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

/** Crée un ExoPlayer avec une mémoire tampon selon le réglage de fluidité. */
@UnstableApi
object PlayerFactory {

    fun create(c: Context): ExoPlayer {
        val (minB, maxB, start, afterRebuf) = buffers(Store.bufferMode(c))
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(minB, maxB, start, afterRebuf)
            .build()
        return ExoPlayer.Builder(c)
            .setLoadControl(loadControl)
            .build()
    }

    /** (minBufferMs, maxBufferMs, bufferForPlaybackMs, afterRebufferMs) */
    private fun buffers(mode: String): IntArray = when (mode) {
        "equilibre" -> intArrayOf(15000, 30000, 2500, 5000)
        "maxi" -> intArrayOf(50000, 120000, 7500, 12000)
        else -> intArrayOf(30000, 60000, 5000, 8000) // confort (défaut)
    }
}
