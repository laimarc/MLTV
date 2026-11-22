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

/** Grille des films (VOD Xtream) avec catégories. */
class MovieActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var adapter: PosterAdapter<VodItem>
    private lateinit var catAdapter: GroupAdapter
    private lateinit var drawer: View
    private lateinit var rvCatDrawer: RecyclerView
    private lateinit var rvGrid: RecyclerView

    private var all: List<VodItem> = emptyList()
    private var cats: List<String> = listOf("Tout")
    private var curCat: String = "Tout"
    private var search: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)

        findViewById<TextView>(R.id.tv_header).text = "Films"
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
            onClick = { v ->
                startActivity(
                    Intent(this, MovieDetailActivity::class.java)
                        .putExtra("url", v.url)
                        .putExtra("title", v.name)
                        .putExtra("name", v.name)
                        .putExtra("poster", v.poster)
                        .putExtra("streamId", v.streamId)
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
                val list = XtreamClient.fetchVod(Store.host(this), Store.user(this), Store.pass(this))
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
        val ordered = orderedRealVodCats()
        val hidden = Store.hiddenVodCats(this)
        val list = ArrayList<String>()
        list.add("Tout")
        list.addAll(ordered.filter { !hidden.contains(it) })
        cats = list
    }

    private fun orderedRealVodCats(): MutableList<String> {
        val present = LinkedHashSet<String>()
        for (v in all) present.add(v.group)
        val saved = Store.vodCatOrder(this)
        val out = ArrayList<String>()
        for (g in saved) if (present.contains(g) && !out.contains(g)) out.add(g)
        for (g in present) if (!out.contains(g)) out.add(g)
        return out
    }

    private fun moveCat(c: String, up: Boolean) {
        val list = orderedRealVodCats()
        val i = list.indexOf(c)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j < 0 || j > list.lastIndex) return
        val t = list[i]; list[i] = list[j]; list[j] = t
        Store.setVodCatOrder(this, list)
        buildCats()
        render()
    }

    private fun showCatMenu(c: String) {
        if (c == "Tout") return
        val hidden = Store.isVodCatHidden(this, c)
        val items = arrayOf("▲ Monter", "▼ Descendre", if (hidden) "Afficher" else "Masquer")
        AlertDialog.Builder(this)
            .setTitle(c)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> moveCat(c, true)
                    1 -> moveCat(c, false)
                    2 -> {
                        Store.toggleHiddenVodCat(this, c)
                        if (curCat == c) curCat = "Tout"
                        buildCats()
                        render()
                    }
                }
            }
            .show()
    }

    private fun render() {
        if (!cats.contains(curCat)) curCat = "Tout"
        catAdapter.update(cats, curCat)
        renderGrid()
    }

    private fun renderGrid() {
        var list = if (curCat == "Tout") all.filter { !Store.isVodCatHidden(this, it.group) }
        else all.filter { it.group == curCat }
        if (search.isNotBlank()) list = list.filter { it.name.contains(search, ignoreCase = true) }
        adapter.update(list)
        status.text = if (list.isEmpty()) "Aucun film" else "${list.size} films"
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
