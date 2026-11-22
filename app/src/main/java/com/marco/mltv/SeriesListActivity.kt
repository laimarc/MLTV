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
import androidx.appcompat.app.AlertDialog
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
    private var movingCat: String? = null

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
        catAdapter = GroupAdapter(
            cats, curCat,
            onClick = { c -> curCat = c; catAdapter.setCurrent(c); renderGrid() },
            onLongClick = { c -> showCatMenu(c) }
        )
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
                    defaultCat()?.let { curCat = it }
                    render()
                    rvGrid.requestFocus()
                }
            } catch (e: Exception) {
                mainHandler.post { status.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun buildCats() {
        val ordered = orderedRealSeriesCats()
        val hidden = Store.hiddenSeriesCats(this)
        val list = ArrayList<String>()
        list.add("Tout")
        list.addAll(ordered.filter { !hidden.contains(it) })
        cats = list
    }

    /** Catégorie « Nouveauté (FR) » si elle existe (priorité à celle contenant FR). */
    private fun defaultCat(): String? {
        val news = cats.filter { it != "Tout" && it.contains("nouveaut", ignoreCase = true) }
        if (news.isEmpty()) return null
        return news.firstOrNull { it.contains("fr", ignoreCase = true) } ?: news.first()
    }

    private fun orderedRealSeriesCats(): MutableList<String> {
        val present = LinkedHashSet<String>()
        for (s in all) present.add(s.group)
        val saved = Store.seriesCatOrder(this)
        val out = ArrayList<String>()
        for (g in saved) if (present.contains(g) && !out.contains(g)) out.add(g)
        for (g in present) if (!out.contains(g)) out.add(g)
        return out
    }

    private fun showCatMenu(c: String) {
        if (c == "Tout") return
        val hidden = Store.isSeriesCatHidden(this, c)
        val items = arrayOf("Déplacer (haut/bas)", if (hidden) "Afficher" else "Masquer")
        AlertDialog.Builder(this)
            .setTitle(c)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> startMove(c)
                    1 -> {
                        Store.toggleHiddenSeriesCat(this, c)
                        if (curCat == c) curCat = "Tout"
                        buildCats()
                        render()
                    }
                }
            }
            .show()
    }

    private fun startMove(c: String) {
        if (c == "Tout") return
        movingCat = c
        catAdapter.setMoving(c)
    }

    private fun endMove() {
        movingCat = null
        catAdapter.setMoving(null)
    }

    private fun moveStep(up: Boolean) {
        val c = movingCat ?: return
        val from = cats.indexOf(c)
        val to = if (up) from - 1 else from + 1
        if (from < 0 || to < 1 || to > cats.lastIndex) return
        val list = cats.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        cats = list
        catAdapter.moveItem(from, to)
        Store.setSeriesCatOrder(this, cats.filter { it != "Tout" })
    }

    private fun render() {
        if (!cats.contains(curCat)) curCat = "Tout"
        catAdapter.update(cats, curCat)
        renderGrid()
    }

    private fun renderGrid() {
        var list = if (curCat == "Tout") all.filter { !Store.isSeriesCatHidden(this, it.group) }
        else all.filter { it.group == curCat }
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
            if (movingCat != null) {
                when (kc) {
                    KeyEvent.KEYCODE_DPAD_UP -> moveStep(true)
                    KeyEvent.KEYCODE_DPAD_DOWN -> moveStep(false)
                    else -> endMove()
                }
                return true
            }
            if (drawer.visibility == View.VISIBLE) {
                if (kc == KeyEvent.KEYCODE_DPAD_RIGHT || kc == KeyEvent.KEYCODE_BACK) {
                    closeDrawer(); return true
                }
                if (kc == KeyEvent.KEYCODE_DPAD_LEFT) { finish(); return true }
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
