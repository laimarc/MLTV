package com.marco.mltv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load

/** Fiche d'un film : affiche, synopsis, bouton Lecture. */
class MovieDetailActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.apply(this)
        setContentView(R.layout.activity_movie_detail)

        val url = intent.getStringExtra("url") ?: ""
        val name = intent.getStringExtra("name") ?: "Film"
        val poster = intent.getStringExtra("poster") ?: ""
        val streamId = intent.getIntExtra("streamId", -1)

        findViewById<TextView>(R.id.tv_header).text = name
        findViewById<TextView>(R.id.tv_title).text = name
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val btnWatch = findViewById<Button>(R.id.btn_watch)
        fun refreshWatch() {
            btnWatch.text = if (streamId >= 0 && Store.isMovieWatch(this, streamId))
                "✓  Dans ma liste" else "＋  À regarder"
        }
        btnWatch.setOnClickListener {
            if (streamId >= 0) { Store.toggleMovieWatch(this, streamId); refreshWatch() }
        }
        refreshWatch()

        val ivPoster = findViewById<ImageView>(R.id.iv_poster)
        if (poster.isNotEmpty()) ivPoster.load(poster)

        val plot = findViewById<TextView>(R.id.tv_plot)
        val meta = findViewById<TextView>(R.id.tv_meta)
        plot.text = "Chargement du résumé…"

        val btnPlay = findViewById<Button>(R.id.btn_play)
        btnPlay.setOnClickListener {
            Store.addMovieHistory(this, streamId)
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .putExtra("url", url)
                    .putExtra("title", name)
            )
        }
        btnPlay.requestFocus()

        val btnTrailer = findViewById<Button>(R.id.btn_trailer)

        if (streamId >= 0) {
            Thread {
                try {
                    val info = XtreamClient.fetchVodInfo(
                        Store.host(this), Store.user(this), Store.pass(this), streamId
                    )
                    mainHandler.post {
                        plot.text = if (info.plot.isBlank()) "Pas de résumé disponible." else info.plot
                        meta.text = info.meta
                        if (info.trailer.isNotEmpty()) {
                            btnTrailer.visibility = View.VISIBLE
                            btnTrailer.setOnClickListener { openTrailer(info.trailer) }
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post { plot.text = "Résumé indisponible." }
                }
            }.start()
        } else {
            plot.text = ""
        }
    }

    private fun openTrailer(urlStr: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Impossible d'ouvrir la bande-annonce", android.widget.Toast.LENGTH_SHORT).show()
        }
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
