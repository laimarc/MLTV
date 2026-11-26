package com.marco.mltv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

/** Interface façon TiViMate : vidéo plein écran + volets figés + grille EPG défilable. */
@UnstableApi
class TivimateActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    private lateinit var navPanel: LinearLayout
    private lateinit var catPanel: LinearLayout
    private lateinit var rvCats: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var rvEpg: RecyclerView
    private lateinit var headerScroll: SyncHScroll
    private lateinit var bodyScroll: SyncHScroll
    private lateinit var headerCells: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvGridTitle: TextView

    private lateinit var catAdapter: GroupAdapter
    private lateinit var chAdapter: TiviChannelColAdapter
    private lateinit var epgAdapter: TiviEpgRowAdapter

    private var allChannels: List<Channel> = emptyList()
    private var groups: List<String> = listOf("Tout")
    private var curGroup: String = "★ Favoris"
    private var search: String = ""
    private var playingUrl: String = ""
    private var focusedPos: Int = 0

    private var vGuard = false
    private var hGuard = false

    private val hm = SimpleDateFormat("HH:mm", Locale.FRANCE)
    private val dayFmt = SimpleDateFormat("EEE", Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.apply(this)
        setContentView(R.layout.activity_tivimate)
        TiviGrid.initWindow(pastDays = 3, futureDays = 2)

        playerView = findViewById(R.id.player_view)
        navPanel = findViewById(R.id.nav_panel)
        catPanel = findViewById(R.id.cat_panel)
        rvCats = findViewById(R.id.rv_cats)
        rvChannels = findViewById(R.id.rv_channels)
        rvEpg = findViewById(R.id.rv_epg)
        headerScroll = findViewById(R.id.header_scroll)
        bodyScroll = findViewById(R.id.body_scroll)
        headerCells = findViewById(R.id.header_cells)
        etSearch = findViewById(R.id.et_search)
        tvGridTitle = findViewById(R.id.tv_grid_title)

        applyTransparency()
        setupPlayer()
        setupAdapters()
        setupSync()
        buildHeader()
        setupNav()
        setupSearch()
        loadChannels()
    }

    private fun applyTransparency() {
        val a = Store.panelAlpha(this).coerceIn(30, 100) * 255 / 100
        val bg = Color.argb(a, 14, 22, 38)
        navPanel.setBackgroundColor(bg)
        catPanel.setBackgroundColor(bg)
        findViewById<View>(R.id.grid_panel).setBackgroundColor(bg)
    }

    private fun setupPlayer() {
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
    }

    private fun setupAdapters() {
        rvCats.layoutManager = LinearLayoutManager(this)
        catAdapter = GroupAdapter(groups, curGroup, onClick = { g ->
            curGroup = g
            renderGrid()
            rvChannels.requestFocus()
        })
        rvCats.adapter = catAdapter

        rvChannels.layoutManager = LinearLayoutManager(this)
        chAdapter = TiviChannelColAdapter(emptyList(),
            onFocus = { c, pos ->
                focusedPos = pos
                tvGridTitle.text = (if (c.num > 0) "${c.num}  " else "") + c.name
            },
            onClick = { c -> openFullscreen(c) })
        rvChannels.adapter = chAdapter

        rvEpg.layoutManager = LinearLayoutManager(this)
        epgAdapter = TiviEpgRowAdapter(emptyList(),
            onCellFocus = { c, pos, p ->
                focusedPos = pos
                val t = if (p != null) "  ·  ${hm.format(Date(p.start))}–${hm.format(Date(p.stop))}" else ""
                tvGridTitle.text = (if (c.num > 0) "${c.num}  " else "") + c.name + t
            },
            onPlay = { c -> openFullscreen(c) },
            onReplay = { c, p -> replay(c, p) })
        rvEpg.adapter = epgAdapter
    }

    private fun setupSync() {
        // vertical : colonne chaînes <-> grille
        rvChannels.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (vGuard) return; vGuard = true; rvEpg.scrollBy(0, dy); vGuard = false
            }
        })
        rvEpg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (vGuard) return; vGuard = true; rvChannels.scrollBy(0, dy); vGuard = false
            }
        })
        // horizontal : grille -> frise
        bodyScroll.onScrollX = { x ->
            if (!hGuard) { hGuard = true; headerScroll.scrollTo(x, 0); hGuard = false }
        }
    }

    private fun buildHeader() {
        headerCells.removeAllViews()
        val slots = TiviGrid.totalMinutes() / TiviGrid.SLOT_MIN
        val w = TiviGrid.widthPx(this, TiviGrid.SLOT_MIN.toLong())
        for (i in 0 until slots) {
            val t = TiviGrid.gridStart + i.toLong() * TiviGrid.SLOT_MIN * 60000L
            val d = Date(t)
            val midnight = SimpleDateFormat("HH:mm", Locale.FRANCE).format(d) == "00:00"
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.START
                setPadding((8 * resources.displayMetrics.density).toInt(), 0, 0, 0)
                setTextColor(if (midnight) 0xFFB9C6D6.toInt() else 0xFF8B98A9.toInt())
                textSize = 12f
                text = if (midnight) "${dayFmt.format(d).replaceFirstChar { it.uppercase() }} ${hm.format(d)}" else hm.format(d)
            }
            headerCells.addView(tv)
        }
    }

    private fun setupNav() {
        findViewById<TextView>(R.id.nav_tv).setOnClickListener { hideNav(); rvChannels.requestFocus() }
        findViewById<TextView>(R.id.nav_films).setOnClickListener { startActivity(Intent(this, MovieActivity::class.java)) }
        findViewById<TextView>(R.id.nav_series).setOnClickListener { startActivity(Intent(this, SeriesListActivity::class.java)) }
        findViewById<TextView>(R.id.nav_list).setOnClickListener { startActivity(Intent(this, MovieActivity::class.java)) }
        findViewById<TextView>(R.id.nav_settings).setOnClickListener { startActivity(Intent(this, SourceActivity::class.java).putExtra("edit", true)) }
        findViewById<TextView>(R.id.nav_search).setOnClickListener {
            hideNav(); etSearch.visibility = View.VISIBLE; etSearch.requestFocus()
        }
    }

    private fun showNav() {
        navPanel.visibility = View.VISIBLE
        navPanel.post { findViewById<TextView>(R.id.nav_tv).requestFocus() }
    }

    private fun hideNav() {
        navPanel.visibility = View.GONE
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { search = s?.toString() ?: ""; renderGrid() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun loadChannels() {
        tvGridTitle.text = "Chargement…"
        Thread {
            try {
                val list = ChannelRepository.load(this)
                mainHandler.post {
                    allChannels = list
                    buildGroups()
                    catAdapter.update(groups, curGroup)
                    renderGrid()
                    val first = currentFiltered().firstOrNull()
                    if (first != null) playBackground(first)
                    rvChannels.requestFocus()
                    scrollToNow()
                    loadEpg()
                }
            } catch (e: Exception) {
                mainHandler.post { tvGridTitle.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun loadEpg() {
        if (Store.type(this) != "xtream" || Epg.loaded) {
            if (Epg.loaded) { renderGrid(); scrollToNow() }
            return
        }
        Thread {
            try {
                Epg.ensureLoaded(Store.host(this), Store.user(this), Store.pass(this))
                mainHandler.post { renderGrid(); scrollToNow() }
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun scrollToNow() {
        mainHandler.post {
            val x = (TiviGrid.offsetPx(this, System.currentTimeMillis()) -
                TiviGrid.widthPx(this, 20)).coerceAtLeast(0)
            bodyScroll.scrollTo(x, 0)
            headerScroll.scrollTo(x, 0)
        }
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
        list.add("Tout"); list.add("★ Favoris")
        list.addAll(ordered.filter { !hidden.contains(it) })
        groups = list
    }

    private fun currentFiltered(): List<Channel> {
        val favs = Store.favorites(this)
        val hiddenCh = Store.hiddenChannels(this)
        val hiddenGrp = Store.hiddenGroups(this)
        val order = Store.channelOrder(this)
        var base = allChannels.filter { !hiddenCh.contains(it.url) && !hiddenGrp.contains(it.group) }
        if (order.isNotEmpty()) {
            val idx = HashMap<String, Int>()
            for ((i, u) in order.withIndex()) idx[u] = i
            base = base.sortedBy { idx[it.url] ?: Int.MAX_VALUE }
        }
        val byGroup = when (curGroup) {
            "Tout" -> base
            "★ Favoris" -> base.filter { favs.contains(it.url) }
            else -> base.filter { it.group == curGroup }
        }
        return if (search.isBlank()) byGroup else byGroup.filter { it.name.contains(search, ignoreCase = true) }
    }

    private fun renderGrid() {
        if (!groups.contains(curGroup)) curGroup = "Tout"
        catAdapter.update(groups, curGroup)
        val list = currentFiltered()
        chAdapter.update(list)
        epgAdapter.update(list)
    }

    private fun playBackground(c: Channel) {
        val p = player ?: return
        playingUrl = c.url
        p.setMediaItem(MediaItem.fromUri(c.url)); p.prepare(); p.playWhenReady = true
    }

    private fun openFullscreen(c: Channel) {
        val list = currentFiltered()
        val idx = list.indexOfFirst { it.url == c.url }
        LiveSession.channels = list
        LiveSession.index = if (idx >= 0) idx else 0
        playBackground(c)
        startActivity(Intent(this, PlayerActivity::class.java)
            .putExtra("url", c.url).putExtra("title", c.name)
            .putExtra("live", true).putExtra("epgId", c.epgId))
    }

    private fun replay(c: Channel, p: Epg.Prog) {
        if (c.streamId < 0) return
        val dur = ((p.stop - p.start) / 60000L).toInt().coerceAtLeast(1)
        val url = XtreamClient.catchupUrl(Store.host(this), Store.user(this), Store.pass(this), c.streamId, p.start, dur)
        startActivity(Intent(this, PlayerActivity::class.java)
            .putExtra("url", url).putExtra("title", "⟲ " + p.title).putExtra("catchup", true))
    }

    /** Depuis la colonne chaînes, entre dans la grille sur la cellule « maintenant ». */
    private fun enterEpgAtNow(pos: Int) {
        scrollToNow()
        rvEpg.post {
            val vh = rvEpg.findViewHolderForAdapterPosition(pos) as? TiviEpgRowAdapter.VH ?: return@post
            val now = System.currentTimeMillis()
            val row = vh.row
            for (i in 0 until row.childCount) {
                val child = row.getChildAt(i)
                val prog = child.tag as? Epg.Prog
                if (prog != null && prog.start <= now && now < prog.stop) { child.requestFocus(); return@post }
            }
            // sinon première cellule focusable
            for (i in 0 until row.childCount) {
                val child = row.getChildAt(i)
                if (child.isFocusable) { child.requestFocus(); return@post }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val ls = LiveSession.channels
        val li = LiveSession.index
        if (ls.isNotEmpty() && li in ls.indices && ls[li].url != playingUrl) playBackground(ls[li])
        if (playingUrl.isNotEmpty()) player?.playWhenReady = true
    }

    override fun onStop() { super.onStop(); player?.playWhenReady = false }

    override fun onDestroy() {
        super.onDestroy()
        player?.release(); player = null
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val kc = event.keyCode
            // Menu visible : droite le referme
            if (navPanel.visibility == View.VISIBLE) {
                if (kc == KeyEvent.KEYCODE_DPAD_RIGHT && navPanel.hasFocus()) {
                    hideNav(); rvCats.requestFocus(); return true
                }
                if (kc == KeyEvent.KEYCODE_BACK) { hideNav(); rvCats.requestFocus(); return true }
                return super.dispatchKeyEvent(event)
            }
            when (kc) {
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (rvChannels.hasFocus()) { enterEpgAtNow(focusedPos); return true }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (currentFocus is EditText) return super.dispatchKeyEvent(event)
                    if (rvChannels.hasFocus()) { rvCats.requestFocus(); return true }
                    if (rvCats.hasFocus()) { showNav(); return true }
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (rvEpg.hasFocus()) { focusChannel(focusedPos); return true }
                    if (etSearch.visibility == View.VISIBLE) { etSearch.visibility = View.GONE; rvChannels.requestFocus(); return true }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun focusChannel(pos: Int) {
        rvChannels.post {
            (rvChannels.findViewHolderForAdapterPosition(pos) as? TiviChannelColAdapter.VH)?.root?.requestFocus()
                ?: rvChannels.requestFocus()
        }
    }
}
