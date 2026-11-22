package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
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
        setContentView(R.layout.activity_series_detail)

        val seriesId = intent.getIntExtra("seriesId", -1)
        val name = intent.getStringExtra("name") ?: "Série"
        findViewById<TextView>(R.id.tv_header).text = name
        status = findViewById(R.id.tv_status)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val rvSeasons = findViewById<RecyclerView>(R.id.rv_seasons)
        val rvEpisodes = findViewById<RecyclerView>(R.id.rv_episodes)
        rvSeasons.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvEpisodes.layoutManager = LinearLayoutManager(this)

        seasonAdapter = GroupAdapter(emptyList(), "") { s ->
            currentSeason = s
            renderEpisodes()
            seasonAdapter.update(seasons.map { it.name }, currentSeason)
        }
        rvSeasons.adapter = seasonAdapter

        episodeAdapter = EpisodeAdapter(emptyList()) { ep ->
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .putExtra("url", ep.url)
                    .putExtra("title", ep.title)
            )
        }
        rvEpisodes.adapter = episodeAdapter

        Thread {
            try {
                val data = XtreamClient.fetchSeriesInfo(
                    Store.host(this), Store.user(this), Store.pass(this), seriesId
                )
                mainHandler.post {
                    seasons = data
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
}
