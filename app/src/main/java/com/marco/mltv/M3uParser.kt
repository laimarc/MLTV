package com.marco.mltv

/** Transforme le texte d'une playlist M3U en liste de chaînes. */
object M3uParser {

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        var name = ""
        var logo = ""
        var group = ""
        var epg = ""
        var pending = false

        for (raw in text.split("\n")) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF")) {
                val info = line.substringAfter(":", line)
                val comma = info.lastIndexOf(',')
                val attrs = if (comma >= 0) info.substring(0, comma) else info
                name = if (comma >= 0) info.substring(comma + 1).trim() else "Sans nom"
                if (name.isEmpty()) name = "Sans nom"
                logo = attr(attrs, "tvg-logo")
                group = attr(attrs, "group-title").ifEmpty { "Autres" }
                epg = attr(attrs, "tvg-id")
                pending = true
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                if (pending) {
                    out.add(Channel(name, logo, group, line, epg))
                    pending = false
                }
            }
        }
        return out
    }

    private fun attr(src: String, key: String): String {
        val m = Regex("$key=\"([^\"]*)\"").find(src) ?: return ""
        return m.groupValues[1]
    }
}
