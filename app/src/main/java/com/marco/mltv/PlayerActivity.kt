package com.marco.mltv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Lecture plein écran. En direct : pas de barre de défilement, overlay info + zapping haut/bas. */
@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var live = false
    private lateinit var overlay: View
    private val hideOverlay = Runnable { overlay.visibility = View.GONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        live = intent.getBooleanExtra("live", false)
        val playerView = findViewById<PlayerView>(R.id.player_view)
        overlay = findViewById(R.id.info_overlay)

        val p = ExoPlayer.Builder(this).build()
        player = p
        playerView.player = p

        if (live) {
            playerView.useController = false
            playCurrentLive()
        } else {
            val url = intent.getStringExtra("url") ?: ""
            if (url.isNotEmpty()) {
                p.setMediaItem(MediaItem.fromUri(url))
                p.prepare()
                p.playWhenReady = true
            }
        }
    }

    private fun playCurrentLive() {
        val list = LiveSession.channels
        if (list.isEmpty()) {
            val url = intent.getStringExtra("url") ?: ""
            val name = intent.getStringExtra("title") ?: ""
            val epgId = intent.getStringExtra("epgId") ?: ""
            playUrl(url, name, epgId)
            return
        }
        val idx = LiveSession.index.coerceIn(0, list.lastIndex)
        LiveSession.index = idx
        val c = list[idx]
        playUrl(c.url, c.name, c.epgId)
    }

    private fun playUrl(url: String, name: String, epgId: String) {
        val p = player ?: return
        if (url.isNotEmpty()) {
            p.setMediaItem(MediaItem.fromUri(url))
            p.prepare()
            p.playWhenReady = true
        }
        InfoOverlay.populate(
            name, epgId,
            findViewById(R.id.tv_info_channel),
            findViewById(R.id.tv_info_now),
            findViewById(R.id.tv_info_next),
            findViewById<ProgressBar>(R.id.pb_info)
        )
        overlay.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideOverlay)
        mainHandler.postDelayed(hideOverlay, 6000)
    }

    private fun zap(delta: Int) {
        val n = LiveSession.channels.size
        if (n == 0) return
        LiveSession.index = ((LiveSession.index + delta) % n + n) % n
        playCurrentLive()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (live && event.action == KeyEvent.ACTION_DOWN && LiveSession.channels.isNotEmpty()) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> { zap(1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { zap(-1); return true }
            }
        }
        return super.dispatchKeyEvent(event)
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
