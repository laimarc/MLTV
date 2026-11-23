package com.marco.mltv

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import coil.load
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Remplit l'overlay « info chaîne » (logo + nom + programme en cours / à suivre). */
object InfoOverlay {

    private val hm = SimpleDateFormat("HH:mm", Locale.FRANCE)

    fun populate(
        channelName: String,
        epgId: String,
        chTv: TextView,
        nowTv: TextView,
        nextTv: TextView,
        pb: ProgressBar,
        logoUrl: String = "",
        logoIv: ImageView? = null
    ) {
        chTv.text = channelName
        if (logoIv != null) {
            if (logoUrl.isNotEmpty()) {
                logoIv.visibility = View.VISIBLE
                logoIv.load(logoUrl)
            } else {
                logoIv.visibility = View.GONE
            }
        }
        val now = if (epgId.isNotEmpty()) Epg.nowProg(epgId) else null
        val next = if (epgId.isNotEmpty()) Epg.nextProg(epgId) else null

        if (now != null) {
            nowTv.text = "● ${now.title}   ${hm.format(Date(now.start))} – ${hm.format(Date(now.stop))}"
            nowTv.visibility = View.VISIBLE
            val total = (now.stop - now.start).coerceAtLeast(1L)
            pb.progress = (((System.currentTimeMillis() - now.start) * 100) / total).toInt().coerceIn(0, 100)
            pb.visibility = View.VISIBLE
        } else {
            nowTv.visibility = View.GONE
            pb.visibility = View.GONE
        }

        if (next != null) {
            nextTv.text = "À suivre : ${next.title}  ${hm.format(Date(next.start))}"
            nextTv.visibility = View.VISIBLE
        } else {
            nextTv.visibility = View.GONE
        }
    }
}
