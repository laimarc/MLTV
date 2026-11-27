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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UnstableApi
class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var rvCatDrawer: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var status: TextView
    private lateinit var drawer: View
    private lateinit var tvCurCat: TextView

    private lateinit var tvClock: TextView
    private val clockFmt = SimpleDateFormat("EEE d MMM 'à' HH:mm", Locale.FRANCE)
    private val clockTick = object : Runnable {
        override fun run() {
            tvClock.text = clockFmt.format(Date()).replaceFirstChar { it.uppercase() }
            mainHandler.postDelayed(this, 30000)
        }
    }
    private fun startClock() { mainHandler.post(clockTick) }

    private lateinit var rvProgs: RecyclerView
    private lateinit var tvChHeader: TextView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailTime: TextView
    private lateinit var tvDetailDesc: TextView
    private lateinit var pbDetail: ProgressBar
    private lateinit var progAdapter: EpgProgAdapter
    private val hm = SimpleDateFormat("HH:mm", Locale.FRANCE)
    private var curChannel: Channel? = null

    private lateinit var groupAdapter: GroupAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private var allChannels: List<Channel> = emptyList()
    private var groups: List<String> = listOf("Tout")
    private var curGroup: String = "★ Favoris"
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
        Theme.apply(this)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        rvCatDrawer = findViewById(R.id.rv_catdrawer)
        rvChannels = findViewById(R.id.rv_channels)
        status = findViewById(R.id.tv_status)
        drawer = findViewById(R.id.drawer)
        tvCurCat = findViewById(R.id.tv_curcat)
        tvClock = findViewById(R.id.tv_clock)
        startClock()
        rvProgs = findViewById(R.id.rv_progs)
        tvChHeader = findViewById(R.id.tv_ch_header)
        tvDetailTitle = findViewById(R.id.tv_detail_title)
        tvDetailTime = findViewById(R.id.tv_detail_time)
        tvDetailDesc = findViewById(R.id.tv_detail_desc)
        pbDetail = findViewById(R.id.pb_detail)

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

        findViewById<Button>(R.id.btn_manage).setOnClickListener {
            startActivity(Intent(this, ManageActivity::class.java))
        }

        findViewById<EditText>(R.id.et_search).addTextChangedListener { e ->
            search = e?.toString() ?: ""
            render()
        }

        rvCatDrawer.layoutManager = LinearLayoutManager(this)
        rvChannels.layoutManager = LinearLayoutManager(this)
        rvProgs.layoutManager = LinearLayoutManager(this)

        progAdapter = EpgProgAdapter(
            emptyList(),
            onReplay = { prog -> replay(prog) },
            onFocus = { prog -> showDetails(prog) }
        )
        rvProgs.adapter = progAdapter

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
                val oldIdx = currentFiltered().indexOfFirst { it.url == c.url }
                Store.toggleFavorite(this, c.url)
                channelAdapter.update(currentFiltered())
                Toast.makeText(
                    this,
                    if (Store.isFavorite(this, c.url)) "Ajouté aux favoris" else "Retiré des favoris",
                    Toast.LENGTH_SHORT
                ).show()
                // garder le cadre sur la chaîne choisie (et non en haut de liste)
                val list = currentFiltered()
                if (list.isNotEmpty()) {
                    val here = list.indexOfFirst { it.url == c.url }
                    val target = if (here >= 0) here else oldIdx.coerceIn(0, list.lastIndex)
                    (rvChannels.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(target, rvChannels.height / 3)
                    tryFocusChannel(target, 0)
                }
            }
        )
        rvChannels.adapter = channelAdapter

        player = PlayerFactory.create(this)
        playerView.player = player
        playerView.useController = false
        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                mainHandler.postDelayed({
                    player?.let { it.seekToDefaultPosition(); it.prepare(); it.playWhenReady = true }
                }, 1500)
            }
        })

        loadChannels()
        Updater.checkAsync(this)
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
                    // restaure la dernière chaîne regardée (survit à la recréation de l'activité),
                    // sinon la première de la liste
                    val saved = Store.lastChannel(this)
                    val savedCh = if (saved.isNotEmpty()) allChannels.firstOrNull { it.url == saved } else null
                    if (savedCh != null) {
                        // si la chaîne n'est pas dans le groupe courant, bascule sur « Tout »
                        if (currentFiltered().none { it.url == saved }) {
                            curGroup = "Tout"
                            render()
                        }
                        play(savedCh)
                        rvChannels.postDelayed({ focusCurrentChannel() }, 250)
                    } else {
                        currentFiltered().firstOrNull()?.let { play(it) }
                        rvChannels.requestFocus()
                    }
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
            if (Epg.loaded) {
                channelAdapter.update(currentFiltered())
                curChannel?.let { loadChannelEpg(it) }
            }
            return
        }
        Thread {
            try {
                Epg.ensureLoaded(Store.host(this), Store.user(this), Store.pass(this))
                mainHandler.post {
                    channelAdapter.update(currentFiltered())
                    curChannel?.let { loadChannelEpg(it) }
                }
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
        val target = curChannel?.group
        val idx = if (target != null) groups.indexOf(target) else -1
        if (idx < 0) {
            rvCatDrawer.requestFocus()
            return
        }
        val lm = rvCatDrawer.layoutManager as LinearLayoutManager
        lm.scrollToPositionWithOffset(idx, rvCatDrawer.height / 3)
        rvCatDrawer.post {
            rvCatDrawer.findViewHolderForAdapterPosition(idx)?.itemView?.findViewById<View>(R.id.tv_group)?.requestFocus()
                ?: rvCatDrawer.postDelayed({
                    rvCatDrawer.findViewHolderForAdapterPosition(idx)?.itemView?.findViewById<View>(R.id.tv_group)?.requestFocus()
                }, 120)
        }
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
        curChannel = c
        curEpgId = c.epgId
        curName = c.name
        curStreamId = c.streamId
        curArchive = c.archive
        curUrl = c.url
        Store.setLastChannel(this, c.url)
        p.setMediaItem(MediaItem.fromUri(c.url))
        p.prepare()
        p.playWhenReady = true
        loadChannelEpg(c)
    }

    private fun loadChannelEpg(c: Channel) {
        tvChHeader.text = (if (c.num > 0) "${c.num}  " else "") + c.name
        val progs = Epg.list(c.epgId)
        progAdapter.update(progs, c.archive)
        val now = System.currentTimeMillis()
        val idx = progs.indexOfFirst { it.start <= now && now < it.stop }
        when {
            idx >= 0 -> {
                (rvProgs.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(idx, 0)
                showDetails(progs[idx])
            }
            progs.isNotEmpty() -> showDetails(progs[0])
            else -> {
                tvDetailTitle.text = c.name
                tvDetailTime.text = ""
                tvDetailDesc.text = "Pas de guide pour cette chaîne."
                pbDetail.progress = 0
            }
        }
    }

    private fun showDetails(p: Epg.Prog) {
        tvDetailTitle.text = p.title
        val durMin = ((p.stop - p.start) / 60000L).toInt()
        tvDetailTime.text = "${hm.format(Date(p.start))} – ${hm.format(Date(p.stop))}  ·  $durMin min"
        val total = (p.stop - p.start).coerceAtLeast(1L)
        pbDetail.progress = (((System.currentTimeMillis() - p.start) * 100) / total).toInt().coerceIn(0, 100)
        tvDetailDesc.text = p.desc
    }

    private fun replay(prog: Epg.Prog) {
        val c = curChannel ?: return
        if (c.streamId < 0) return
        val dur = ((prog.stop - prog.start) / 60000L).toInt().coerceAtLeast(1)
        val url = XtreamClient.catchupUrl(Store.host(this), Store.user(this), Store.pass(this), c.streamId, prog.start, dur)
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra("url", url)
                .putExtra("title", "⟲ " + prog.title)
                .putExtra("catchup", true)
        )
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


    override fun onResume() {
        super.onResume()
        if (allChannels.isNotEmpty()) {
            buildGroups()
            render()
            // synchronise avec la dernière chaîne zappée en plein écran
            val ls = LiveSession.channels
            val li = LiveSession.index
            if (ls.isNotEmpty() && li in ls.indices && ls[li].url != curUrl) {
                play(ls[li])
            }
            // au retour (plein écran, Films, Séries…) : le cadre revient sur la chaîne en lecture.
            // Android pose son focus auto (chaîne 1) APRÈS onResume ; on réapplique donc
            // notre focus après un court délai (pour passer après lui) ET au focus fenêtre.
            if (curUrl.isNotEmpty()) {
                focusCurrentPending = true
                rvChannels.postDelayed({ runPendingFocus() }, 250)
            }
        }
        // reprend le direct au retour du plein écran
        if (curUrl.isNotEmpty()) player?.playWhenReady = true
    }

    private var focusCurrentPending = false

    /** Exécuté une seule fois par retour (le 1er des 2 déclencheurs consomme le drapeau). */
    private fun runPendingFocus() {
        if (!focusCurrentPending) return
        focusCurrentPending = false
        if (curUrl.isNotEmpty()) focusCurrentChannel()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && focusCurrentPending) {
            rvChannels.postDelayed({ runPendingFocus() }, 250)
        }
    }

    private var focusAttachListener: RecyclerView.OnChildAttachStateChangeListener? = null

    private fun focusCurrentChannel() {
        val target = curUrl
        val idx = currentFiltered().indexOfFirst { it.url == target }
        // DIAGNOSTIC temporaire (toast visible)
        Toast.makeText(
            this,
            "retour  cur=${if (target.isEmpty()) "VIDE" else target.takeLast(6)}  idx=$idx",
            Toast.LENGTH_LONG
        ).show()
        if (idx < 0) return
        val lm = rvChannels.layoutManager as LinearLayoutManager
        lm.scrollToPositionWithOffset(idx, rvChannels.height / 3)

        // retire un éventuel écouteur précédent
        focusAttachListener?.let { rvChannels.removeOnChildAttachStateChangeListener(it) }
        focusAttachListener = null

        // la vue est déjà présente → on pose le cadre tout de suite
        val vh = rvChannels.findViewHolderForAdapterPosition(idx)
        if (vh != null && vh.itemView.requestFocus()) return

        // sinon on attend que la vue de la chaîne soit réellement attachée
        val l = object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                if (rvChannels.getChildAdapterPosition(view) == idx) {
                    view.requestFocus()
                    rvChannels.removeOnChildAttachStateChangeListener(this)
                    if (focusAttachListener === this) focusAttachListener = null
                }
            }
            override fun onChildViewDetachedFromWindow(view: View) {}
        }
        focusAttachListener = l
        rvChannels.addOnChildAttachStateChangeListener(l)
        // filet de sécurité temporel
        tryFocusChannel(idx, 0)
    }

    private fun tryFocusChannel(idx: Int, attempt: Int) {
        val vh = rvChannels.findViewHolderForAdapterPosition(idx)
        if (vh != null && vh.itemView.requestFocus()) {
            focusAttachListener?.let { rvChannels.removeOnChildAttachStateChangeListener(it) }
            focusAttachListener = null
            return
        }
        if (attempt >= 15) return
        if (vh == null) {
            (rvChannels.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(idx, rvChannels.height / 3)
        }
        rvChannels.postDelayed({ tryFocusChannel(idx, attempt + 1) }, 70)
    }


    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(clockTick)
        player?.release()
        player = null
    }
}
