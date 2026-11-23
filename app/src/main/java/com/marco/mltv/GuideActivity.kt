package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Guide 3 colonnes : chaînes | grille horaire | aperçu + détails. */
@UnstableApi
class GuideActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hm = SimpleDateFormat("HH:mm", Locale.FRANCE)

    private var player: ExoPlayer? = null
    private lateinit var rvChannels: RecyclerView
    private lateinit var rvProgs: RecyclerView
    private lateinit var tvChHeader: TextView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailTime: TextView
    private lateinit var tvDetailDesc: TextView
    private lateinit var pbDetail: ProgressBar

    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var progAdapter: EpgProgAdapter

    private var channels: List<Channel> = emptyList()
    private var selectedChannel: Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.apply(this)
        setContentView(R.layout.activity_guide)

        rvChannels = findViewById(R.id.rv_channels)
        rvProgs = findViewById(R.id.rv_progs)
        tvChHeader = findViewById(R.id.tv_ch_header)
        tvDetailTitle = findViewById(R.id.tv_detail_title)
        tvDetailTime = findViewById(R.id.tv_detail_time)
        tvDetailDesc = findViewById(R.id.tv_detail_desc)
        pbDetail = findViewById(R.id.pb_detail)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val playerView = findViewById<PlayerView>(R.id.player_view)
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        rvChannels.layoutManager = LinearLayoutManager(this)
        rvProgs.layoutManager = LinearLayoutManager(this)

        channelAdapter = ChannelAdapter(
            emptyList(),
            favUrls = { Store.favorites(this) },
            nowTitle = { id -> Epg.nowTitle(id) },
            onClick = { c -> selectChannel(c) },
            onLongClick = { }
        )
        rvChannels.adapter = channelAdapter

        progAdapter = EpgProgAdapter(
            emptyList(),
            onReplay = { prog -> replay(prog) },
            onFocus = { prog -> showDetails(prog) }
        )
        rvProgs.adapter = progAdapter

        tvDetailTitle.text = "Chargement…"
        Thread {
            try {
                val list = ChannelRepository.load(this)
                try { Epg.ensureLoaded(Store.host(this), Store.user(this), Store.pass(this)) } catch (_: Exception) {}
                mainHandler.post {
                    channels = list
                    channelAdapter.update(list)
                    if (list.isNotEmpty()) selectChannel(list[0])
                    rvChannels.requestFocus()
                }
            } catch (e: Exception) {
                mainHandler.post { tvDetailTitle.text = "Erreur : ${e.message}" }
            }
        }.start()
    }

    private fun selectChannel(c: Channel) {
        selectedChannel = c
        player?.setMediaItem(MediaItem.fromUri(c.url))
        player?.prepare()
        player?.playWhenReady = true
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
        val c = selectedChannel ?: return
        if (c.streamId < 0) return
        val dur = ((prog.stop - prog.start) / 60000L).toInt().coerceAtLeast(1)
        val url = XtreamClient.catchupUrl(Store.host(this), Store.user(this), Store.pass(this), c.streamId, prog.start, dur)
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra("url", url)
                .putExtra("title", "⟲ " + prog.title)
        )
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
