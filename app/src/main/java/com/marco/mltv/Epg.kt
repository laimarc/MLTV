package com.marco.mltv

import android.util.Xml
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Guide des programmes (EPG) à partir du XMLTV Xtream (xmltv.php). */
object Epg {

    data class Prog(val start: Long, val stop: Long, val title: String, val desc: String)

    @Volatile
    var loaded = false
        private set

    private var map: Map<String, List<Prog>> = emptyMap()

    /** Charge le XMLTV si ce n'est pas déjà fait. À appeler hors du thread principal. */
    @Synchronized
    fun ensureLoaded(host: String, user: String, pass: String) {
        if (loaded) return
        val base = host.trimEnd('/')
        val url = "$base/xmltv.php?username=${enc(user)}&password=${enc(pass)}"
        val req = Request.Builder().url(url).build()
        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            val stream = resp.body?.byteStream() ?: throw RuntimeException("EPG vide")
            map = parse(stream)
        }
        loaded = true
    }

    fun nowTitle(channelId: String): String? = nowProg(channelId)?.title

    fun nowProg(channelId: String): Prog? {
        val t = System.currentTimeMillis()
        return map[channelId]?.firstOrNull { it.start <= t && t < it.stop }
    }

    fun nextProg(channelId: String): Prog? {
        val t = System.currentTimeMillis()
        return map[channelId]?.firstOrNull { it.start > t }
    }

    fun list(channelId: String): List<Prog> = map[channelId] ?: emptyList()

    // ---------- parsing ----------
    private fun parse(stream: java.io.InputStream): Map<String, MutableList<Prog>> {
        val now = System.currentTimeMillis()
        val minKeep = now - 3L * 3600_000L            // -3 h
        val maxKeep = now + 3L * 24 * 3600_000L       // +3 jours
        val out = HashMap<String, MutableList<Prog>>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        var event = parser.eventType
        var chan = ""
        var start = 0L
        var stop = 0L
        var title = ""
        var desc = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        chan = parser.getAttributeValue(null, "channel") ?: ""
                        start = parseTime(parser.getAttributeValue(null, "start") ?: "")
                        stop = parseTime(parser.getAttributeValue(null, "stop") ?: "")
                        title = ""
                        desc = ""
                    }
                    "title" -> title = safeText(parser)
                    "desc" -> desc = safeText(parser)
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme") {
                    if (chan.isNotEmpty() && stop >= minKeep && start <= maxKeep) {
                        out.getOrPut(chan) { ArrayList() }.add(Prog(start, stop, title, desc))
                    }
                }
            }
            event = parser.next()
        }
        for (list in out.values) list.sortBy { it.start }
        return out
    }

    private fun safeText(parser: XmlPullParser): String {
        return try {
            if (parser.next() == XmlPullParser.TEXT) parser.text ?: "" else ""
        } catch (e: Exception) {
            ""
        }
    }

    private val fmtTz = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val fmtUtc = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseTime(s: String): Long {
        val t = s.trim()
        if (t.isEmpty()) return 0L
        return try {
            fmtTz.parse(t)?.time ?: 0L
        } catch (e: Exception) {
            try {
                if (t.length >= 14) fmtUtc.parse(t.substring(0, 14))?.time ?: 0L else 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
