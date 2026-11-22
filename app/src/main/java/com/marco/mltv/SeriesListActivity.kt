package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Grille des séries (Xtream). */
class SeriesListActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)

        findViewById<TextView>(R.id.tv_header).text = "Séries"
        status = findViewById(R.id.tv_status)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_grid)
        rv.layoutManager = GridLayoutManager(this, spanCount())

        val adapter = PosterAdapter<SeriesItem>(
            emptyList(),
            titleOf = { it.name },
            posterOf = { it.poster },
            onClick = { s ->
                startActivity(
                    Intent(this, SeriesDetailActivity::class.java)
                        .putExtra("seriesId", s.seriesId)
                        .putExtra("name", s.name)
                )
            }
        )
        rv.adapter = adapter

        Thread {
            try {
                val list = XtreamClient.fetchSeries(Store.host(this), Store.user(this), Store.pass(this))
                mainHandler.post {
                    adapter.update(list)
                    status.text = if (list.isEmpty()) "Aucune série trouvée" else "${list.size} séries"
                    rv.requestFocus()
                }
            } catch (e: Exception) {
                mainHandler.post { status.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun spanCount(): Int {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        return (widthDp / 170).toInt().coerceIn(3, 8)
    }
}
