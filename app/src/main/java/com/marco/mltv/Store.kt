package com.marco.mltv

import android.content.Context

/** Sauvegarde locale de la source et des favoris (SharedPreferences). */
object Store {
    private const val PREFS = "mltv"

    private fun prefs(c: Context) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasConfig(c: Context): Boolean =
        prefs(c).getString("type", null) != null

    fun type(c: Context): String = prefs(c).getString("type", "") ?: ""

    fun saveM3u(c: Context, url: String) {
        prefs(c).edit()
            .putString("type", "m3u")
            .putString("m3u", url.trim())
            .apply()
    }

    fun saveXtream(c: Context, host: String, user: String, pass: String) {
        prefs(c).edit()
            .putString("type", "xtream")
            .putString("host", host.trim().trimEnd('/'))
            .putString("user", user.trim())
            .putString("pass", pass.trim())
            .apply()
    }

    fun m3uUrl(c: Context): String = prefs(c).getString("m3u", "") ?: ""
    fun host(c: Context): String = prefs(c).getString("host", "") ?: ""
    fun user(c: Context): String = prefs(c).getString("user", "") ?: ""
    fun pass(c: Context): String = prefs(c).getString("pass", "") ?: ""

    // Favoris : ensemble d'URL
    fun favorites(c: Context): MutableSet<String> =
        HashSet(prefs(c).getStringSet("favs", emptySet()) ?: emptySet())

    fun isFavorite(c: Context, url: String): Boolean =
        favorites(c).contains(url)

    fun toggleFavorite(c: Context, url: String) {
        val set = favorites(c)
        if (!set.remove(url)) set.add(url)
        prefs(c).edit().putStringSet("favs", set).apply()
    }

    // ---- Masquage ----
    fun hiddenChannels(c: Context): MutableSet<String> =
        HashSet(prefs(c).getStringSet("hidCh", emptySet()) ?: emptySet())

    fun isChannelHidden(c: Context, url: String): Boolean = hiddenChannels(c).contains(url)

    fun toggleHiddenChannel(c: Context, url: String) {
        val set = hiddenChannels(c)
        if (!set.remove(url)) set.add(url)
        prefs(c).edit().putStringSet("hidCh", set).apply()
    }

    fun hiddenGroups(c: Context): MutableSet<String> =
        HashSet(prefs(c).getStringSet("hidGrp", emptySet()) ?: emptySet())

    fun isGroupHidden(c: Context, name: String): Boolean = hiddenGroups(c).contains(name)

    fun toggleHiddenGroup(c: Context, name: String) {
        val set = hiddenGroups(c)
        if (!set.remove(name)) set.add(name)
        prefs(c).edit().putStringSet("hidGrp", set).apply()
    }

    // ---- Ordre personnalisé ----
    fun channelOrder(c: Context): List<String> = split(prefs(c).getString("chOrder", "") ?: "")
    fun setChannelOrder(c: Context, list: List<String>) {
        prefs(c).edit().putString("chOrder", list.joinToString("\n")).apply()
    }

    fun groupOrder(c: Context): List<String> = split(prefs(c).getString("grpOrder", "") ?: "")
    fun setGroupOrder(c: Context, list: List<String>) {
        prefs(c).edit().putString("grpOrder", list.joinToString("\n")).apply()
    }

    // ---- Catégories Films (VOD) ----
    fun vodCatOrder(c: Context): List<String> = split(prefs(c).getString("vodCatOrder", "") ?: "")
    fun setVodCatOrder(c: Context, list: List<String>) {
        prefs(c).edit().putString("vodCatOrder", list.joinToString("\n")).apply()
    }

    fun hiddenVodCats(c: Context): MutableSet<String> =
        HashSet(prefs(c).getStringSet("hidVodCat", emptySet()) ?: emptySet())

    fun isVodCatHidden(c: Context, name: String): Boolean = hiddenVodCats(c).contains(name)

    fun toggleHiddenVodCat(c: Context, name: String) {
        val set = hiddenVodCats(c)
        if (!set.remove(name)) set.add(name)
        prefs(c).edit().putStringSet("hidVodCat", set).apply()
    }

    // ---- Catégories Séries ----
    fun seriesCatOrder(c: Context): List<String> = split(prefs(c).getString("serCatOrder", "") ?: "")
    fun setSeriesCatOrder(c: Context, list: List<String>) {
        prefs(c).edit().putString("serCatOrder", list.joinToString("\n")).apply()
    }

    fun hiddenSeriesCats(c: Context): MutableSet<String> =
        HashSet(prefs(c).getStringSet("hidSerCat", emptySet()) ?: emptySet())

    fun isSeriesCatHidden(c: Context, name: String): Boolean = hiddenSeriesCats(c).contains(name)

    fun toggleHiddenSeriesCat(c: Context, name: String) {
        val set = hiddenSeriesCats(c)
        if (!set.remove(name)) set.add(name)
        prefs(c).edit().putStringSet("hidSerCat", set).apply()
    }

    // ---- Reprise de lecture (films / épisodes) : position en ms par URL ----
    fun resumePos(c: Context, url: String): Long =
        prefs(c).getLong("pos:" + url, 0L)

    fun setResumePos(c: Context, url: String, posMs: Long) {
        prefs(c).edit().putLong("pos:" + url, posMs).apply()
    }

    fun clearResumePos(c: Context, url: String) {
        prefs(c).edit().remove("pos:" + url).apply()
    }

    // ---- Fluidité (mémoire tampon) : "equilibre" | "confort" | "maxi" ----
    fun bufferMode(c: Context): String = prefs(c).getString("bufMode", "confort") ?: "confort"
    fun setBufferMode(c: Context, mode: String) {
        prefs(c).edit().putString("bufMode", mode).apply()
    }

    private fun split(s: String): List<String> =
        if (s.isEmpty()) emptyList() else s.split("\n")
}
