package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var infoOverlay: View
    private lateinit var infoChannel: TextView
    private lateinit var infoNow: TextView
    private lateinit var infoNext: TextView
    private lateinit var infoPb: ProgressBar
    private val hideInfo = Runnable { infoOverlay.visibility = View.GONE }

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
    private var movingCat: String? = null

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

        groupAdapter = GroupAdapter(
            groups, curGroup,
            onClick = { g ->
                curGroup = g
                groupAdapter.setCurrent(g)
                renderList()
            },
            onLongClick = { g -> showGroupMenu(g) }
        )
        rvCatDrawer.adapter = groupAdapter

        channelAdapter = ChannelAdapter(
            emptyList(),
            favUrls = { Store.favorites(this) },
            nowTitle = { id -> Epg.nowTitle(id) },
            onClick = { c ->
                if (c.url == curUrl) {
                    // 2e appui sur la chaîne déjà lancée → plein écran
                    openFullscreen()
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

        infoOverlay = findViewById(R.id.info_overlay)
        infoChannel = findViewById(R.id.tv_info_channel)
        infoNow = findViewById(R.id.tv_info_now)
        infoNext = findViewById(R.id.tv_info_next)
        infoPb = findViewById(R.id.pb_info)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        hideSeekBar(playerView)

        // Bouton plein écran dans les contrôles du lecteur live
        playerView.setFullscreenButtonClickListener {
            if (curUrl.isNotEmpty()) openFullscreen()
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
        renderList()
    }

    private fun renderList() {
        tvCurCat.text = curGroup
        val filtered = currentFiltered()
        channelAdapter.update(filtered)
        status.text = "${filtered.size} chaîne" + if (filtered.size > 1) "s" else ""
    }

    private fun showGroupMenu(g: String) {
        if (g == "Tout" || g == "★ Favoris") return
        val hidden = Store.isGroupHidden(this, g)
        val items = arrayOf("Déplacer (haut/bas)", if (hidden) "Afficher" else "Masquer")
        AlertDialog.Builder(this)
            .setTitle(g)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> startMove(g)
                    1 -> {
                        Store.toggleHiddenGroup(this, g)
                        if (curGroup == g) curGroup = "Tout"
                        buildGroups()
                        render()
                    }
                }
            }
            .show()
    }

    private fun startMove(g: String) {
        if (g == "Tout" || g == "★ Favoris") return
        movingCat = g
        groupAdapter.setMoving(g)
        Toast.makeText(this, "Haut/Bas pour déplacer, OK pour valider", Toast.LENGTH_SHORT).show()
    }

    private fun endMove() {
        movingCat = null
        groupAdapter.setMoving(null)
    }

    private fun moveStep(up: Boolean) {
        val g = movingCat ?: return
        val from = groups.indexOf(g)
        val to = if (up) from - 1 else from + 1
        // 0 et 1 = "Tout" et "★ Favoris", on ne déplace pas au-dessus
        if (from < 0 || to < 2 || to > groups.lastIndex) return
        val list = groups.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        groups = list
        groupAdapter.moveItem(from, to)
        Store.setGroupOrder(this, groups.filter { it != "Tout" && it != "★ Favoris" })
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
        showInfo(c.name, c.epgId)
    }

    private fun showInfo(name: String, epgId: String) {
        InfoOverlay.populate(name, epgId, infoChannel, infoNow, infoNext, infoPb)
        infoOverlay.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideInfo)
        mainHandler.postDelayed(hideInfo, 6000)
    }

    private fun openFullscreen() {
        if (curUrl.isEmpty()) return
        val list = currentFiltered()
        val idx = list.indexOfFirst { it.url == curUrl }
        LiveSession.channels = list
        LiveSession.index = if (idx >= 0) idx else 0
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra("url", curUrl)
                .putExtra("title", curName)
                .putExtra("live", true)
                .putExtra("epgId", curEpgId)
        )
    }

    private fun hideSeekBar(pv: PlayerView) {
        pv.findViewById<View?>(androidx.media3.ui.R.id.exo_progress)?.visibility = View.GONE
        pv.findViewById<View?>(androidx.media3.ui.R.id.exo_position)?.visibility = View.GONE
        pv.findViewById<View?>(androidx.media3.ui.R.id.exo_duration)?.visibility = View.GONE
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
