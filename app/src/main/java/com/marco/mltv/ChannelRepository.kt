package com.marco.mltv

import android.content.Context

/** Charge les chaînes live selon la source enregistrée. Appeler hors du thread principal. */
object ChannelRepository {

    fun load(c: Context): List<Channel> {
        return when (Store.type(c)) {
            "m3u" -> {
                val url = Store.m3uUrl(c)
                if (url.isEmpty()) throw RuntimeException("Aucune playlist enregistrée")
                M3uParser.parse(Http.get(url))
            }
            "xtream" -> XtreamClient.fetchLive(Store.host(c), Store.user(c), Store.pass(c))
            else -> emptyList()
        }
    }
}
