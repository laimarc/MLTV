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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@UnstableApi
class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var rvCatDrawer: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var status: TextView
    private lateinit var drawer: View
    private lateinit var tvCurCat: TextView

    private lateinit var groupAdapter: GroupAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private var allChannels: List<Channel> = emptyList()
    private var groups: List<String> = listOf("Tout")
    private var curGroup: String = "Tout"
    private var curEpgId: String = ""
    private var curName: String = ""
    private var curStreamId: Int = -1
    private var curArchive: Boolean = false
    private var curUrl: String = ""
    private var search: String = ""

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        rvCatDrawer = findViewById(R.id.rv_catdrawer)
        rvChannels = findViewById(R.id.rv_channels)
        status = findViewById(R.id.tv_status)
        drawer = findViewById(R.id.drawer)
        tvCurCat = findViewById(R.id.tv_curcat)

        findViewById<Button>(R.id.btn_cats).setOnClickListener { openDrawer() }

        findViewById<Button>(R.id.btn_sources).setOnClickListener {
            val i = Intent(this, SourceActivity::class.java)
            i.putExtra("edit", true)
            startActivity(i)
            finish()
        }

        findViewById<Button>(R.id.btn_films).setOnClickListener {
            if (Store.type(this) == "xtream") {
                startActivity(Intent(this, MovieActivity::class.java))
            } else {
                Toast.makeText(this, "Disponible uniquement en Xtream", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_series).setOnClickListener {
            if (Store.type(this) == "xtream") {
                startActivity(Intent(this, SeriesListActivity::class.java))
            } else {
                Toast.makeText(this, "Disponible uniquement en Xtream", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_guide).setOnClickListener {
            if (curEpgId.isEmpty()) {
                Toast.makeText(this, "Choisis d'abord une chaîne", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(
                    Intent(this, EpgActivity::class.java)
                        .putExtra("epgId", curEpgId)
                        .putExtra("name", curName)
                        .putExtra("streamId", curStreamId)
                        .putExtra("archive", curArchive)
                )
            }
        }

        findViewById<Button>(R.id.btn_manage).setOnClickListener {
            startActivity(Intent(this, ManageActivity::class.java))
        }

        findViewById<EditText>(R.id.et_search).addTextChangedListener { e ->
            search = e?.toString() ?: ""
            render()
        }

        rvCatDrawer.layoutManager = LinearLayoutManager(this)
        rvChannels.layoutManager = LinearLayoutManager(this)

        groupAdapter = GroupAdapter(groups, curGroup) { g ->
            curGroup = g
            closeDrawer()
            render()
        }
        rvCatDrawer.adapter = groupAdapter

        channelAdapter = ChannelAdapter(
            emptyList(),
            favUrls = { Store.favorites(this) },
            nowTitle = { id -> Epg.nowTitle(id) },
            onClick = { c ->
                if (c.url == curUrl) {
                    // 2e appui sur la chaîne déjà lancée → plein écran
                    startActivity(
                        Intent(this, PlayerActivity::class.java)
                            .putExtra("url", c.url)
                            .putExtra("title", c.name)
                    )
                } else {
                    // 1er appui → lecture dans la vue liste
                    play(c)
                }
            },
            onLongClick = { c ->
                Store.toggleFavorite(this, c.url)
                channelAdapter.update(currentFiltered())
                Toast.makeText(
                    this,
                    if (Store.isFavorite(this, c.url)) "Ajouté aux favoris" else "Retiré des favoris",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        rvChannels.adapter = channelAdapter

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Bouton plein écran dans les contrôles du lecteur live
        playerView.setFullscreenButtonClickListener {
            if (curUrl.isNotEmpty()) {
                startActivity(
                    Intent(this, PlayerActivity::class.java)
                        .putExtra("url", curUrl)
                        .putExtra("title", curName)
                )
            }
        }

        loadChannels()
    }

    private fun loadChannels() {
        status.text = "Chargement…"
        Thread {
            try {
                val list = ChannelRepository.load(this)
                mainHandler.post {
                    allChannels = list
                    buildGroups()
                    render()
                    rvChannels.requestFocus()
                    loadEpg()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    status.text = "Erreur : ${e.message}\nAppuie sur « Sources » pour vérifier."
                }
            }
        }.start()
    }

    private fun loadEpg() {
        if (Store.type(this) != "xtream" || Epg.loaded) {
            if (Epg.loaded) channelAdapter.update(currentFiltered())
            return
        }
        Thread {
            try {
                Epg.ensureLoaded(Store.host(this), Store.user(this), Store.pass(this))
                mainHandler.post { channelAdapter.update(currentFiltered()) }
            } catch (e: Exception) {
                // EPG indisponible : on garde les groupes, pas de blocage
            }
        }.start()
    }

    private fun buildGroups() {
        val present = LinkedHashSet<String>()
        for (c in allChannels) present.add(c.group)
        val hidden = Store.hiddenGroups(this)
        val saved = Store.groupOrder(this)
        val ordered = ArrayList<String>()
        for (g in saved) if (present.contains(g) && !ordered.contains(g)) ordered.add(g)
        for (g in present) if (!ordered.contains(g)) ordered.add(g)
        val list = ArrayList<String>()
        list.add("Tout")
        list.add("★ Favoris")
        list.addAll(ordered.filter { !hidden.contains(it) })
        groups = list
    }

    private fun currentFiltered(): List<Channel> {
        val favs = Store.favorites(this)
        val hiddenCh = Store.hiddenChannels(this)
        val hiddenGrp = Store.hiddenGroups(this)
        val base = applyOrder(
            allChannels.filter { !hiddenCh.contains(it.url) && !hiddenGrp.contains(it.group) }
        )
        val byGroup = when (curGroup) {
            "Tout" -> base
            "★ Favoris" -> base.filter { favs.contains(it.url) }
            else -> base.filter { it.group == curGroup }
        }
        return if (search.isBlank()) byGroup
        else byGroup.filter { it.name.contains(search, ignoreCase = true) }
    }

    private fun applyOrder(list: List<Channel>): List<Channel> {
        val order = Store.channelOrder(this)
        if (order.isEmpty()) return list
        val idx = HashMap<String, Int>()
        for ((i, u) in order.withIndex()) idx[u] = i
        return list.sortedBy { idx[it.url] ?: Int.MAX_VALUE }
    }

    private fun render() {
        if (!groups.contains(curGroup)) curGroup = "Tout"
        groupAdapter.update(groups, curGroup)
        tvCurCat.text = curGroup
        val filtered = currentFiltered()
        channelAdapter.update(filtered)
        status.text = "${filtered.size} chaîne" + if (filtered.size > 1) "s" else ""
    }

    private fun openDrawer() {
        drawer.visibility = View.VISIBLE
        rvCatDrawer.requestFocus()
    }

    private fun closeDrawer() {
        drawer.visibility = View.GONE
        rvChannels.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val kc = event.keyCode
            if (drawer.visibility == View.VISIBLE) {
                if (kc == KeyEvent.KEYCODE_DPAD_RIGHT || kc == KeyEvent.KEYCODE_BACK) {
                    closeDrawer()
                    return true
                }
            } else if (kc == KeyEvent.KEYCODE_DPAD_LEFT) {
                val cur = currentFocus
                if (cur !is EditText) {
                    val next = cur?.focusSearch(View.FOCUS_LEFT)
                    if (next == null || next === cur) {
                        openDrawer()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun play(c: Channel) {
        val p = player ?: return
        curEpgId = c.epgId
        curName = c.name
        curStreamId = c.streamId
        curArchive = c.archive
        curUrl = c.url
        p.setMediaItem(MediaItem.fromUri(c.url))
        p.prepare()
        p.playWhenReady = true
    }

    override fun onResume() {
        super.onResume()
        if (allChannels.isNotEmpty()) {
            buildGroups()
            render()
        }
        // reprend le direct au retour du plein écran
        if (curUrl.isNotEmpty()) player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
