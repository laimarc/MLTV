package com.marco.mltv

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Client HTTP partagé. */
object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun get(url: String, noCache: Boolean = false): String {
        val b = Request.Builder().url(url)
        if (noCache) b.header("Cache-Control", "no-cache, no-store, max-age=0")
            .header("Pragma", "no-cache")
        client.newCall(b.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw RuntimeException("Réponse vide")
        }
    }

    fun download(url: String, dest: java.io.File) {
        val req = Request.Builder().url(url)
            .header("Cache-Control", "no-cache, no-store, max-age=0")
            .header("Pragma", "no-cache")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            val body = resp.body ?: throw RuntimeException("Réponse vide")
            body.byteStream().use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            }
        }
    }
}
