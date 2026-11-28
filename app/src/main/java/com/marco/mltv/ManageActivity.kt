package com.marco.mltv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

/** Gérer les chaînes : choisir un bouquet, appui long sur une chaîne → Déplacer / Masquer. */
class ManageActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var manageAdapter: ManageAdapter
    private lateinit var catAdapter: GroupAdapter

    private var allCh: List<Channel> = emptyList()
    private var byUrl: Map<String, Channel> = emptyMap()
    private var globalOrder: MutableList<String> = ArrayList()
    private var groupsOrdered: MutableList<String> = ArrayList()
    private var mCat = ""
    private var viewRows: List<ManageRow> = emptyList()
    private var movingKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.apply(this)
        setContentView(R.layout.activity_manage)

        status = findViewById(R.id.tv_status)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_manage)
        rv.layoutManager = LinearLayoutManager(this)
        manageAdapter = ManageAdapter(emptyList()) { idx -> showRowMenu(idx) }
        rv.adapter = manageAdapter

        val rvCats = findViewById<RecyclerView>(R.id.rv_mcats)
        rvCats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        catAdapter = GroupAdapter(emptyList(), "", onClick = { c ->
            mCat = c
            catAdapter.setCurrent(c)
            renderList()
        })
        rvCats.adapter = catAdapter

        status.text = "Chargement…"
        Thread {
            try {
                val list = ChannelRepository.load(this)
                mainHandler.post { setup(list) }
            } catch (e: Exception) {
                mainHandler.post { status.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun setup(list: List<Channel>) {
        allCh = list
        byUrl = list.associateBy { it.url }
        globalOrder = buildGlobalOrder(list)
        groupsOrdered = buildGroupOrder(list)
        mCat = groupsOrdered.firstOrNull() ?: ""
        catAdapter.update(groupsOrdered, mCat)
        renderList()
    }

    private fun buildGlobalOrder(list: List<Channel>): MutableList<String> {
        val present = LinkedHashSet(list.map { it.url })
        val saved = Store.channelOrder(this)
        val out = ArrayList<String>()
        for (u in saved) if (present.contains(u) && !out.contains(u)) out.add(u)
        for (c in list) if (!out.contains(c.url)) out.add(c.url)
        return out
    }

    private fun buildGroupOrder(list: List<Channel>): MutableList<String> {
        val present = LinkedHashSet<String>()
        for (c in list) present.add(c.group)
        val saved = Store.groupOrder(this)
        val out = ArrayList<String>()
        for (g in saved) if (present.contains(g) && !out.contains(g)) out.add(g)
        for (g in present) if (!out.contains(g)) out.add(g)
        return out
    }

    private fun renderList() {
        viewRows = globalOrder.mapNotNull { byUrl[it] }
            .filter { it.group == mCat }
            .map { ManageRow(it.url, it.name, Store.isChannelHidden(this, it.url)) }
        manageAdapter.update(viewRows)
        status.text = "${viewRows.size} chaînes · $mCat"
    }

    private fun showRowMenu(idx: Int) {
        if (idx < 0 || idx > viewRows.lastIndex) return
        val row = viewRows[idx]
        val items = arrayOf("Déplacer (haut/bas)", if (row.hidden) "Afficher" else "Masquer")
        AlertDialog.Builder(this, R.style.MLTVDialog)
            .setTitle(row.label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> startMove(row.key)
                    1 -> {
                        Store.toggleHiddenChannel(this, row.key)
                        renderList()
                    }
                }
            }
            .show()
    }

    private fun startMove(key: String) {
        movingKey = key
        manageAdapter.setMoving(key)
    }

    private fun endMove() {
        movingKey = null
        manageAdapter.setMoving(null)
    }

    private fun moveStep(up: Boolean) {
        val key = movingKey ?: return
        val from = viewRows.indexOfFirst { it.key == key }
        val to = if (up) from - 1 else from + 1
        if (from < 0 || to < 0 || to > viewRows.lastIndex) return
        val ia = globalOrder.indexOf(viewRows[from].key)
        val ib = globalOrder.indexOf(viewRows[to].key)
        if (ia < 0 || ib < 0) return
        Collections.swap(globalOrder, ia, ib)
        Store.setChannelOrder(this, globalOrder)
        val list = viewRows.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        viewRows = list
        manageAdapter.moveItem(from, to)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && movingKey != null) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> moveStep(true)
                KeyEvent.KEYCODE_DPAD_DOWN -> moveStep(false)
                else -> endMove()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
