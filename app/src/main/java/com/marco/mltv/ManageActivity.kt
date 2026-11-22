package com.marco.mltv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

/** Mode « Gérer » : réordonner (▲▼) et masquer (👁) chaînes et bouquets. */
class ManageActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var rvCats: RecyclerView
    private lateinit var manageAdapter: ManageAdapter
    private lateinit var catAdapter: GroupAdapter
    private lateinit var btnChannels: Button
    private lateinit var btnGroups: Button

    private var allCh: List<Channel> = emptyList()
    private var byUrl: Map<String, Channel> = emptyMap()
    private var globalOrder: MutableList<String> = ArrayList()
    private var groupsOrdered: MutableList<String> = ArrayList()
    private var mode = "channels"
    private var mCat = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)

        status = findViewById(R.id.tv_status)
        rvCats = findViewById(R.id.rv_mcats)
        btnChannels = findViewById(R.id.btn_mode_channels)
        btnGroups = findViewById(R.id.btn_mode_groups)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_manage)
        rv.layoutManager = LinearLayoutManager(this)
        manageAdapter = ManageAdapter(
            emptyList(),
            onUp = { pos -> move(pos, true) },
            onDown = { pos -> move(pos, false) },
            onHide = { pos -> hide(pos) }
        )
        rv.adapter = manageAdapter

        rvCats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        catAdapter = GroupAdapter(emptyList(), "", onClick = { c ->
            mCat = c
            renderChannels()
        })
        rvCats.adapter = catAdapter

        btnChannels.setOnClickListener {
            mode = "channels"; updateModeButtons(); renderChannels()
        }
        btnGroups.setOnClickListener {
            mode = "groups"; updateModeButtons(); renderGroups()
        }

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
        mode = "channels"
        updateModeButtons()
        renderChannels()
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

    private fun channelView(): List<Channel> =
        globalOrder.mapNotNull { byUrl[it] }.filter { it.group == mCat }

    private fun renderChannels() {
        rvCats.visibility = View.VISIBLE
        catAdapter.update(groupsOrdered, mCat)
        val view = channelView()
        val rows = view.map { ManageRow(it.url, it.name, Store.isChannelHidden(this, it.url)) }
        manageAdapter.update(rows)
        status.text = "${rows.size} chaînes · $mCat"
    }

    private fun renderGroups() {
        rvCats.visibility = View.GONE
        val rows = groupsOrdered.map { ManageRow(it, it, Store.isGroupHidden(this, it)) }
        manageAdapter.update(rows)
        status.text = "${rows.size} bouquets"
    }

    private fun move(pos: Int, up: Boolean) {
        if (pos < 0) return
        if (mode == "channels") {
            val view = channelView()
            val target = if (up) pos - 1 else pos + 1
            if (pos > view.lastIndex || target < 0 || target > view.lastIndex) return
            val ia = globalOrder.indexOf(view[pos].url)
            val ib = globalOrder.indexOf(view[target].url)
            if (ia >= 0 && ib >= 0) {
                Collections.swap(globalOrder, ia, ib)
                Store.setChannelOrder(this, globalOrder)
                renderChannels()
            }
        } else {
            val target = if (up) pos - 1 else pos + 1
            if (pos > groupsOrdered.lastIndex || target < 0 || target > groupsOrdered.lastIndex) return
            Collections.swap(groupsOrdered, pos, target)
            Store.setGroupOrder(this, groupsOrdered)
            renderGroups()
        }
    }

    private fun hide(pos: Int) {
        if (pos < 0) return
        if (mode == "channels") {
            val view = channelView()
            if (pos > view.lastIndex) return
            Store.toggleHiddenChannel(this, view[pos].url)
            renderChannels()
        } else {
            if (pos > groupsOrdered.lastIndex) return
            Store.toggleHiddenGroup(this, groupsOrdered[pos])
            renderGroups()
        }
    }

    private fun updateModeButtons() {
        val active = mode == "channels"
        btnChannels.setBackgroundResource(if (active) R.drawable.btn_accent else R.drawable.btn_secondary)
        btnChannels.setTextColor(ContextCompat.getColor(this, if (active) R.color.accent_text else R.color.txt))
        btnGroups.setBackgroundResource(if (!active) R.drawable.btn_accent else R.drawable.btn_secondary)
        btnGroups.setTextColor(ContextCompat.getColor(this, if (!active) R.color.accent_text else R.color.txt))
    }
}
