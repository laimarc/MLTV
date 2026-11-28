package com.marco.mltv

/** Film (VOD Xtream). */
data class VodItem(
    val name: String,
    val poster: String,
    val group: String,
    val url: String,
    val streamId: Int = -1
)

/** Série (Xtream) — on garde l'id pour charger saisons/épisodes. */
data class SeriesItem(
    val name: String,
    val poster: String,
    val group: String,
    val seriesId: Int
)

/** Un épisode jouable. */
data class Episode(
    val title: String,
    val url: String
)

/** Une saison et ses épisodes. */
data class Season(
    val name: String,
    val episodes: List<Episode>
)

/** Fiche d'un film (synopsis + infos). */
data class VodInfo(
    val plot: String,
    val meta: String,
    val trailer: String = ""
)

/** Fiche d'une série (synopsis + infos + saisons). */
data class SeriesInfo(
    val plot: String,
    val meta: String,
    val seasons: List<Season>,
    val trailer: String = ""
)
