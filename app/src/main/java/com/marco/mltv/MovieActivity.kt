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

/** Grille des films (VOD Xtream). */
class MovieActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)

        findViewById<TextView>(R.id.tv_header).text = "Films"
        status = findViewById(R.id.tv_status)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_grid)
        rv.layoutManager = GridLayoutManager(this, spanCount())

        val adapter = PosterAdapter<VodItem>(
            emptyList(),
            titleOf = { it.name },
            posterOf = { it.poster },
            onClick = { v ->
                startActivity(
                    Intent(this, PlayerActivity::class.java)
                        .putExtra("url", v.url)
                        .putExtra("title", v.name)
                )
            }
        )
        rv.adapter = adapter

        Thread {
            try {
                val list = XtreamClient.fetchVod(Store.host(this), Store.user(this), Store.pass(this))
                mainHandler.post {
                    adapter.update(list)
                    status.text = if (list.isEmpty()) "Aucun film trouvé" else "${list.size} films"
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
