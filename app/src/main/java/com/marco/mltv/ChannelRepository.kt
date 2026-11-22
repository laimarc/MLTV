package com.marco.mltv

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Charge les chaînes selon la source enregistrée. Appeler hors du thread principal. */
object ChannelRepository {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun load(c: Context): List<Channel> {
        return when (Store.type(c)) {
            "m3u" -> {
                val url = Store.m3uUrl(c)
                if (url.isEmpty()) throw RuntimeException("Aucune playlist enregistrée")
                M3uParser.parse(download(url))
            }
            "xtream" -> XtreamClient.fetchLive(
                http, Store.host(c), Store.user(c), Store.pass(c)
            )
            else -> emptyList()
        }
    }

    private fun download(url: String): String {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw RuntimeException("Réponse vide")
        }
    }
}
