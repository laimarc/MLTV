package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var rvGroups: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var status: TextView

    private lateinit var groupAdapter: GroupAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private var allChannels: List<Channel> = emptyList()
    private var groups: List<String> = listOf("Tout")
    private var curGroup: String = "Tout"

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        rvGroups = findViewById(R.id.rv_groups)
        rvChannels = findViewById(R.id.rv_channels)
        status = findViewById(R.id.tv_status)

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

        rvGroups.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvChannels.layoutManager = LinearLayoutManager(this)

        groupAdapter = GroupAdapter(groups, curGroup) { g ->
            curGroup = g
            render()
        }
        rvGroups.adapter = groupAdapter

        channelAdapter = ChannelAdapter(
            emptyList(),
            favUrls = { Store.favorites(this) },
            onClick = { c -> play(c) },
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
                }
            } catch (e: Exception) {
                mainHandler.post {
                    status.text = "Erreur : ${e.message}\nAppuie sur « Sources » pour vérifier."
                }
            }
        }.start()
    }

    private fun buildGroups() {
        val distinct = LinkedHashSet<String>()
        for (c in allChannels) distinct.add(c.group)
        val list = ArrayList<String>()
        list.add("Tout")
        list.add("★ Favoris")
        list.addAll(distinct)
        groups = list
    }

    private fun currentFiltered(): List<Channel> {
        val favs = Store.favorites(this)
        return when (curGroup) {
            "Tout" -> allChannels
            "★ Favoris" -> allChannels.filter { favs.contains(it.url) }
            else -> allChannels.filter { it.group == curGroup }
        }
    }

    private fun render() {
        groupAdapter.update(groups, curGroup)
        val filtered = currentFiltered()
        channelAdapter.update(filtered)
        status.text = "${filtered.size} chaîne" + if (filtered.size > 1) "s" else ""
    }

    private fun play(c: Channel) {
        val p = player ?: return
        p.setMediaItem(MediaItem.fromUri(c.url))
        p.prepare()
        p.playWhenReady = true
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
