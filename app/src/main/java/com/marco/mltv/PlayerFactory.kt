package com.marco.mltv

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Crée un ExoPlayer avec mémoire tampon réglable et pile réseau OkHttp
 *  (tolérante aux réponses non-standard des serveurs de rattrapage/replay). */
@UnstableApi
object PlayerFactory {

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun create(c: Context): ExoPlayer {
        val (minB, maxB, start, afterRebuf) = buffers(Store.bufferMode(c))
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(minB, maxB, start, afterRebuf)
            .build()

        val httpFactory = OkHttpDataSource.Factory(okHttp)
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
