package com.marco.mltv

import android.content.Context

/** Échelle de temps partagée par la grille EPG style TiViMate. */
object TiviGrid {
    const val DP_PER_MIN = 2f      // largeur d'une minute
    const val ROW_H_DP = 60        // hauteur d'une ligne
    const val SLOT_MIN = 30        // pas de la frise horaire

    var gridStart = 0L
    var gridEnd = 0L

    fun initWindow(pastDays: Int, futureDays: Int) {
        val now = System.currentTimeMillis()
        val hour = 3600_000L
        val base = (now / hour) * hour
        gridStart = base - pastDays * 24 * hour
        gridEnd = base + futureDays * 24 * hour
    }

    fun totalMinutes(): Int = ((gridEnd - gridStart) / 60000L).toInt()

    fun pxPerMin(c: Context): Float = DP_PER_MIN * c.resources.displayMetrics.density

    fun widthPx(c: Context, minutes: Long): Int = (minutes * pxPerMin(c)).toInt()

    fun rowHeightPx(c: Context): Int = (ROW_H_DP * c.resources.displayMetrics.density).toInt()

    /** Offset horizontal (px) d'un instant depuis le début de la grille. */
    fun offsetPx(c: Context, time: Long): Int =
        widthPx(c, ((time - gridStart) / 60000L))
}
