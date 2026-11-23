package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Fiche série : saisons + épisodes. */
class SeriesDetailActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var episodeAdapter: EpisodeAdapter
    private lateinit var seasonAdapter: GroupAdapter

    private var seasons: List<Season> = emptyList()
    private var currentSeason: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.apply(this)
        setContentView(R.layout.activity_series_detail)

        val seriesId = intent.getIntExtra("seriesId", -1)
        val name = intent.getStringExtra("name") ?: "Série"
        findViewById<TextView>(R.id.tv_header).text = name
        status = findViewById(R.id.tv_status)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val btnWatch = findViewById<Button>(R.id.btn_watch)
        fun refreshWatch() {
            btnWatch.text = if (seriesId >= 0 && Store.isSeriesWatch(this, seriesId))
                "✓  Dans ma liste" else "＋  À regarder"
        }
        btnWatch.setOnClickListener {
            if (seriesId >= 0) { Store.toggleSeriesWatch(this, seriesId); refreshWatch() }
        }
        refreshWatch()

        val rvSeasons = findViewById<RecyclerView>(R.id.rv_seasons)
        val rvEpisodes = findViewById<RecyclerView>(R.id.rv_episodes)
        rvSeasons.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvEpisodes.layoutManager = LinearLayoutManager(this)

        seasonAdapter = GroupAdapter(emptyList(), "", onClick = { s ->
            currentSeason = s
            renderEpisodes()
            seasonAdapter.update(seasons.map { it.name }, currentSeason)
        })
        rvSeasons.adapter = seasonAdapter

        episodeAdapter = EpisodeAdapter(emptyList()) { ep ->
            Store.addSeriesHistory(this, seriesId)
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .putExtra("url", ep.url)
                    .putExtra("title", ep.title)
            )
        }
        rvEpisodes.adapter = episodeAdapter

        val tvMeta = findViewById<TextView>(R.id.tv_meta)
        val tvPlot = findViewById<TextView>(R.id.tv_plot)

        Thread {
            try {
                val info = XtreamClient.fetchSeriesInfo(
                    Store.host(this), Store.user(this), Store.pass(this), seriesId
                )
                mainHandler.post {
                    tvMeta.text = info.meta
                    tvPlot.text = info.plot
                    seasons = info.seasons
                    if (seasons.isEmpty()) {
                        status.text = "Aucun épisode disponible"
                    } else {
                        currentSeason = seasons[0].name
                        seasonAdapter.update(seasons.map { it.name }, currentSeason)
                        renderEpisodes()
                        rvEpisodes.requestFocus()
                    }
                }
            } catch (e: Exception) {
                mainHandler.post { status.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun renderEpisodes() {
        val season = seasons.firstOrNull { it.name == currentSeason }
        val eps = season?.episodes ?: emptyList()
        episodeAdapter.update(eps)
        status.text = "${eps.size} épisode" + if (eps.size > 1) "s" else ""
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            val cur = currentFocus
            if (cur !is EditText) {
                val next = cur?.focusSearch(View.FOCUS_LEFT)
                if (next == null || next === cur) { finish(); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
