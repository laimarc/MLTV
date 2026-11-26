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

/** Interface alternative façon TiViMate : vidéo plein écran + panneaux translucides. */
@UnstableApi
class TivimateActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    private lateinit var rvCats: RecyclerView
    private lateinit var rvGrid: RecyclerView
    private lateinit var rulerCells: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvGridTitle: TextView
    private lateinit var tvClock: TextView

    private lateinit var catAdapter: GroupAdapter
    private lateinit var rowAdapter: TiviRowAdapter

    private var allChannels: List<Channel> = emptyList()
    private var groups: List<String> = listOf("Tout")
    private var curGroup: String = "★ Favoris"
    private var search: String = ""
    private var playingUrl: String = ""

    private val spanMin = 180
    private var winStart = 0L

    private val clockFmt = SimpleDateFormat("EEE d MMM HH:mm", Locale.FRANCE)
    private val hm = SimpleDateFormat("HH:mm", Locale.FRANCE)
    private val clockTick = object : Runnable {
        override fun run() {
            tvClock.text = clockFmt.format(Date()).replaceFirstChar { it.uppercase() }
            mainHandler.postDelayed(this, 30000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.apply(this)
        setContentView(R.layout.activity_tivimate)

        playerView = findViewById(R.id.player_view)
        rvCats = findViewById(R.id.rv_cats)
        rvGrid = findViewById(R.id.rv_grid)
        rulerCells = findViewById(R.id.ruler_cells)
        etSearch = findViewById(R.id.et_search)
        tvGridTitle = findViewById(R.id.tv_grid_title)
        tvClock = findViewById(R.id.tv_clock)

        applyTransparency()
        mainHandler.post(clockTick)

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

        rvCats.layoutManager = LinearLayoutManager(this)
        catAdapter = GroupAdapter(groups, curGroup, onClick = { g ->
            curGroup = g
            renderGrid()
            rvGrid.requestFocus()
        })
        rvCats.adapter = catAdapter

        rvGrid.layoutManager = LinearLayoutManager(this)
        winStart = floorToHalfHour(System.currentTimeMillis())
        rowAdapter = TiviRowAdapter(emptyList(), winStart, spanMin,
            onFocus = { c -> tvGridTitle.text = (if (c.num > 0) "${c.num}  " else "") + c.name },
            onClick = { c -> openFullscreen(c) })
        rvGrid.adapter = rowAdapter
        buildRuler()

        setupNav()
        setupSearch()
        loadChannels()
    }

    private fun applyTransparency() {
        val a = Store.panelAlpha(this).coerceIn(30, 100) * 255 / 100
        val bg = Color.argb(a, 14, 22, 38)
        findViewById<View>(R.id.nav_panel).setBackgroundColor(bg)
        findViewById<View>(R.id.cat_panel).setBackgroundColor(bg)
        findViewById<View>(R.id.grid_panel).setBackgroundColor(bg)
    }

    private fun setupNav() {
        findViewById<TextView>(R.id.nav_tv).setOnClickListener { rvGrid.requestFocus() }
        findViewById<TextView>(R.id.nav_films).setOnClickListener {
            startActivity(Intent(this, MovieActivity::class.java))
        }
        findViewById<TextView>(R.id.nav_series).setOnClickListener {
            startActivity(Intent(this, SeriesListActivity::class.java))
        }
        findViewById<TextView>(R.id.nav_list).setOnClickListener {
            startActivity(Intent(this, MovieActivity::class.java))
        }
        findViewById<TextView>(R.id.nav_settings).setOnClickListener {
            startActivity(Intent(this, SourceActivity::class.java).putExtra("edit", true))
        }
        findViewById<TextView>(R.id.nav_search).setOnClickListener {
            etSearch.visibility = View.VISIBLE
            etSearch.requestFocus()
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                search = s?.toString() ?: ""
                renderGrid()
            }
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
                    rvGrid.requestFocus()
                    loadEpg()
                }
            } catch (e: Exception) {
                mainHandler.post { tvGridTitle.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun loadEpg() {
        if (Store.type(this) != "xtream" || Epg.loaded) {
            if (Epg.loaded) buildRuler().also { renderGrid() }
            return
        }
        Thread {
            try {
                Epg.ensureLoaded(Store.host(this), Store.user(this), Store.pass(this))
                mainHandler.post { buildRuler(); renderGrid() }
            } catch (e: Exception) {
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
        return if (search.isBlank()) byGroup
        else byGroup.filter { it.name.contains(search, ignoreCase = true) }
    }

    private fun renderGrid() {
        if (!groups.contains(curGroup)) curGroup = "Tout"
        catAdapter.update(groups, curGroup)
        rowAdapter.update(currentFiltered(), winStart)
    }

    private fun buildRuler() {
        rulerCells.removeAllViews()
        val cells = spanMin / 30
        for (i in 0 until cells) {
            val t = winStart + i * 30 * 60000L
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.START
                setPadding((8 * resources.displayMetrics.density).toInt(), 0, 0, 0)
                setTextColor(0xFF8B98A9.toInt())
                textSize = 12f
                text = hm.format(Date(t))
            }
            rulerCells.addView(tv)
        }
    }

    private fun floorToHalfHour(ms: Long): Long {
        val half = 30 * 60000L
        return (ms / half) * half
    }

    private fun playBackground(c: Channel) {
        val p = player ?: return
        playingUrl = c.url
        p.setMediaItem(MediaItem.fromUri(c.url))
        p.prepare()
        p.playWhenReady = true
    }

    private fun openFullscreen(c: Channel) {
        val list = currentFiltered()
        val idx = list.indexOfFirst { it.url == c.url }
        LiveSession.channels = list
        LiveSession.index = if (idx >= 0) idx else 0
        playBackground(c)
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra("url", c.url)
                .putExtra("title", c.name)
                .putExtra("live", true)
                .putExtra("epgId", c.epgId)
        )
    }

    override fun onResume() {
        super.onResume()
        // synchronise avec la dernière chaîne zappée en plein écran
        val ls = LiveSession.channels
        val li = LiveSession.index
        if (ls.isNotEmpty() && li in ls.indices && ls[li].url != playingUrl) {
            playBackground(ls[li])
        }
        if (playingUrl.isNotEmpty()) player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(clockTick)
        player?.release()
        player = null
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            val cur = currentFocus
            if (cur !is EditText && rvGrid.hasFocus()) {
                rvCats.requestFocus()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
