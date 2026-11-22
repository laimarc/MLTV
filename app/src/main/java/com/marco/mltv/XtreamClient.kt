package com.marco.mltv

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** Client Xtream Codes : live, films (VOD) et séries via player_api.php. */
object XtreamClient {

    private fun api(host: String, user: String, pass: String): String {
        val base = host.trimEnd('/')
        return "$base/player_api.php?username=${enc(user)}&password=${enc(pass)}"
    }

    // ---------- LIVE ----------
    fun fetchLive(host: String, user: String, pass: String): List<Channel> {
        val base = host.trimEnd('/')
        val u = enc(user); val p = enc(pass)
        val a = api(host, user, pass)

        val catMap = categoryMap("$a&action=get_live_categories")
        val streams = JSONArray(Http.get("$a&action=get_live_streams"))
        val out = ArrayList<Channel>()
        for (i in 0 until streams.length()) {
            val o = streams.optJSONObject(i) ?: continue
            val sid = o.optInt("stream_id", -1)
            if (sid < 0) continue
            val name = o.optString("name", "Sans nom").ifEmpty { "Sans nom" }
            val logo = o.optString("stream_icon", "")
            val group = catMap[o.optString("category_id")] ?: "Autres"
            val url = "$base/live/$u/$p/$sid.m3u8"
            val archive = o.optInt("tv_archive", 0) == 1
            out.add(Channel(name, logo, group, url, o.optString("epg_channel_id", ""), sid, archive))
        }
        if (out.isEmpty()) throw RuntimeException("Aucune chaîne reçue (identifiants ?)")
        return out
    }

    /** URL de catch-up (replay) pour un programme passé. Format courant « timeshift ». */
    fun catchupUrl(host: String, user: String, pass: String, streamId: Int, startMillis: Long, durationMin: Int): String {
        val base = host.trimEnd('/')
        val u = enc(user); val p = enc(pass)
        val start = catchupFmt.format(java.util.Date(startMillis))
        return "$base/timeshift/$u/$p/$durationMin/$start/$streamId.m3u8"
    }

    private val catchupFmt = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", java.util.Locale.US)

    // ---------- FILMS (VOD) ----------
    fun fetchVod(host: String, user: String, pass: String): List<VodItem> {
        val base = host.trimEnd('/')
        val u = enc(user); val p = enc(pass)
        val a = api(host, user, pass)

        val catMap = categoryMap("$a&action=get_vod_categories")
        val streams = JSONArray(Http.get("$a&action=get_vod_streams"))
        val out = ArrayList<VodItem>()
        for (i in 0 until streams.length()) {
            val o = streams.optJSONObject(i) ?: continue
            val sid = o.optInt("stream_id", -1)
            if (sid < 0) continue
            val name = o.optString("name", "Sans titre").ifEmpty { "Sans titre" }
            val poster = o.optString("stream_icon", o.optString("cover", ""))
            val group = catMap[o.optString("category_id")] ?: "Autres"
            val ext = o.optString("container_extension", "mp4").ifEmpty { "mp4" }
            val url = "$base/movie/$u/$p/$sid.$ext"
            out.add(VodItem(name, poster, group, url))
        }
        return out
    }

    // ---------- SÉRIES ----------
    fun fetchSeries(host: String, user: String, pass: String): List<SeriesItem> {
        val a = api(host, user, pass)
        val catMap = categoryMap("$a&action=get_series_categories")
        val arr = JSONArray(Http.get("$a&action=get_series"))
        val out = ArrayList<SeriesItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val sid = o.optInt("series_id", -1)
            if (sid < 0) continue
            val name = o.optString("name", "Sans titre").ifEmpty { "Sans titre" }
            val poster = o.optString("cover", "")
            val group = catMap[o.optString("category_id")] ?: "Autres"
            out.add(SeriesItem(name, poster, group, sid))
        }
        return out
    }

    fun fetchSeriesInfo(host: String, user: String, pass: String, seriesId: Int): List<Season> {
        val base = host.trimEnd('/')
        val u = enc(user); val p = enc(pass)
        val a = api(host, user, pass)
        val root = JSONObject(Http.get("$a&action=get_series_info&series_id=$seriesId"))
        val episodesObj = root.optJSONObject("episodes") ?: return emptyList()

        val seasons = ArrayList<Season>()
        val keys = episodesObj.keys().asSequence().toList()
            .sortedBy { it.toIntOrNull() ?: 0 }
        for (key in keys) {
            val epArr = episodesObj.optJSONArray(key) ?: continue
            val eps = ArrayList<Episode>()
            for (i in 0 until epArr.length()) {
                val e = epArr.optJSONObject(i) ?: continue
                val id = e.optString("id", "")
                if (id.isEmpty()) continue
                val ext = e.optString("container_extension", "mp4").ifEmpty { "mp4" }
                val title = e.optString("title", "Épisode ${i + 1}").ifEmpty { "Épisode ${i + 1}" }
                val url = "$base/series/$u/$p/$id.$ext"
                eps.add(Episode(title, url))
            }
            if (eps.isNotEmpty()) seasons.add(Season("Saison $key", eps))
        }
        return seasons
    }

    // ---------- util ----------
    private fun categoryMap(url: String): HashMap<String, String> {
        val cats = JSONArray(Http.get(url))
        val map = HashMap<String, String>()
        for (i in 0 until cats.length()) {
            val o = cats.optJSONObject(i) ?: continue
            map[o.optString("category_id")] = o.optString("category_name")
        }
        return map
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
