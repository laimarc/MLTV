package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Grille des films (VOD Xtream) avec catégories. */
class MovieActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var adapter: PosterAdapter<VodItem>
    private lateinit var catAdapter: GroupAdapter

    private var all: List<VodItem> = emptyList()
    private var cats: List<String> = listOf("Tout")
    private var curCat: String = "Tout"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)

        findViewById<TextView>(R.id.tv_header).text = "Films"
        status = findViewById(R.id.tv_status)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_grid)
        rv.layoutManager = GridLayoutManager(this, spanCount())

        adapter = PosterAdapter(
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

        val rvCats = findViewById<RecyclerView>(R.id.rv_cats)
        rvCats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        catAdapter = GroupAdapter(cats, curCat) { c ->
            curCat = c
            render()
        }
        rvCats.adapter = catAdapter

        Thread {
            try {
                val list = XtreamClient.fetchVod(Store.host(this), Store.user(this), Store.pass(this))
                mainHandler.post {
                    all = list
                    buildCats()
                    render()
                    rv.requestFocus()
                }
            } catch (e: Exception) {
                mainHandler.post { status.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun buildCats() {
        val distinct = LinkedHashSet<String>()
        for (v in all) distinct.add(v.group)
        val list = ArrayList<String>()
        list.add("Tout")
        list.addAll(distinct)
        cats = list
    }

    private fun render() {
        catAdapter.update(cats, curCat)
        val filtered = if (curCat == "Tout") all else all.filter { it.group == curCat }
        adapter.update(filtered)
        status.text = if (filtered.isEmpty()) "Aucun film" else "${filtered.size} films"
    }

    private fun spanCount(): Int {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        return (widthDp / 170).toInt().coerceIn(3, 8)
    }
}
