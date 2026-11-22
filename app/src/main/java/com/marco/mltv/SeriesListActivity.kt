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
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Grille des séries (Xtream) avec catégories. */
class SeriesListActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var adapter: PosterAdapter<SeriesItem>
    private lateinit var catAdapter: GroupAdapter
    private lateinit var drawer: View
    private lateinit var rvCatDrawer: RecyclerView
    private lateinit var rvGrid: RecyclerView

    private var all: List<SeriesItem> = emptyList()
    private var cats: List<String> = listOf("Tout")
    private var curCat: String = "Tout"
    private var search: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)

        findViewById<TextView>(R.id.tv_header).text = "Séries"
        status = findViewById(R.id.tv_status)
        drawer = findViewById(R.id.drawer)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_cats).setOnClickListener { openDrawer() }

        rvGrid = findViewById(R.id.rv_grid)
        rvGrid.layoutManager = GridLayoutManager(this, spanCount())

        adapter = PosterAdapter(
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
        rvGrid.adapter = adapter

        rvCatDrawer = findViewById(R.id.rv_catdrawer)
        rvCatDrawer.layoutManager = LinearLayoutManager(this)
        catAdapter = GroupAdapter(cats, curCat) { c ->
            curCat = c
            closeDrawer()
            render()
        }
        rvCatDrawer.adapter = catAdapter

        findViewById<EditText>(R.id.et_search).addTextChangedListener { e ->
            search = e?.toString() ?: ""
            render()
        }

        Thread {
            try {
                val list = XtreamClient.fetchSeries(Store.host(this), Store.user(this), Store.pass(this))
                mainHandler.post {
                    all = list
                    buildCats()
                    render()
                    rvGrid.requestFocus()
                }
            } catch (e: Exception) {
                mainHandler.post { status.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun buildCats() {
        val distinct = LinkedHashSet<String>()
        for (s in all) distinct.add(s.group)
        val list = ArrayList<String>()
        list.add("Tout")
        list.addAll(distinct)
        cats = list
    }

    private fun render() {
        catAdapter.update(cats, curCat)
        var list = if (curCat == "Tout") all else all.filter { it.group == curCat }
        if (search.isNotBlank()) list = list.filter { it.name.contains(search, ignoreCase = true) }
        adapter.update(list)
        status.text = if (list.isEmpty()) "Aucune série" else "${list.size} séries"
    }

    private fun spanCount(): Int {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        return (widthDp / 170).toInt().coerceIn(3, 8)
    }

    private fun openDrawer() {
        drawer.visibility = View.VISIBLE
        rvCatDrawer.requestFocus()
    }

    private fun closeDrawer() {
        drawer.visibility = View.GONE
        rvGrid.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val kc = event.keyCode
            if (drawer.visibility == View.VISIBLE) {
                if (kc == KeyEvent.KEYCODE_DPAD_RIGHT || kc == KeyEvent.KEYCODE_BACK) {
                    closeDrawer(); return true
                }
            } else if (kc == KeyEvent.KEYCODE_DPAD_LEFT) {
                val cur = currentFocus
                if (cur !is EditText) {
                    val next = cur?.focusSearch(View.FOCUS_LEFT)
                    if (next == null || next === cur) { openDrawer(); return true }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
