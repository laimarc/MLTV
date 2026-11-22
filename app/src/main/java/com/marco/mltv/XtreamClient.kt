package com.marco.mltv

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** Client Xtream Codes : récupère les chaînes live via player_api.php. */
object XtreamClient {

    fun fetchLive(
        http: OkHttpClient,
        host: String,
        user: String,
        pass: String
    ): List<Channel> {
        val base = host.trimEnd('/')
        val u = enc(user)
        val p = enc(pass)
        val api = "$base/player_api.php?username=$u&password=$p"

        // Catégories : id -> nom
        val cats = JSONArray(get(http, "$api&action=get_live_categories"))
        val catMap = HashMap<String, String>()
        for (i in 0 until cats.length()) {
            val o = cats.optJSONObject(i) ?: continue
            catMap[o.optString("category_id")] = o.optString("category_name")
        }

        // Chaînes
        val streams = JSONArray(get(http, "$api&action=get_live_streams"))
        val out = ArrayList<Channel>()
        for (i in 0 until streams.length()) {
            val o: JSONObject = streams.optJSONObject(i) ?: continue
            val sid = o.optInt("stream_id", -1)
            if (sid < 0) continue
            val name = o.optString("name", "Sans nom")
            val logo = o.optString("stream_icon", "")
            val group = catMap[o.optString("category_id")] ?: "Autres"
            val epg = o.optString("epg_channel_id", "")
            val url = "$base/live/$u/$p/$sid.m3u8"
            out.add(Channel(if (name.isEmpty()) "Sans nom" else name, logo, group, url, epg))
        }
        if (out.isEmpty()) throw RuntimeException("Aucune chaîne reçue (identifiants ?)")
        return out
    }

    private fun get(http: OkHttpClient, url: String): String {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw RuntimeException("Réponse vide")
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
