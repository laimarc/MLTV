package com.marco.mltv

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/** Crée un ExoPlayer avec mémoire tampon réglable, un User-Agent type lecteur IPTV
 *  (certains serveurs bloquent le rattrapage sinon) et des délais réseau larges. */
@UnstableApi
object PlayerFactory {

    private const val UA = "VLC/3.0.20 LibVLC/3.0.20"

    fun create(c: Context): ExoPlayer {
        val (minB, maxB, start, afterRebuf) = buffers(Store.bufferMode(c))
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(minB, maxB, start, afterRebuf)
            .build()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(UA)
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)

        return ExoPlayer.Builder(c)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    /** (minBufferMs, maxBufferMs, bufferForPlaybackMs, afterRebufferMs) */
    private fun buffers(mode: String): IntArray = when (mode) {
        "equilibre" -> intArrayOf(15000, 30000, 2500, 5000)
        "maxi" -> intArrayOf(50000, 120000, 7500, 12000)
        else -> intArrayOf(30000, 60000, 5000, 8000) // confort (défaut)
    }
}
