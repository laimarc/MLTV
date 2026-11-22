package com.marco.mltv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
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

    // Reprise de lecture (VOD)
    private var vodUrl: String = ""
    private val saveTick = object : Runnable {
        override fun run() {
            saveProgress()
            mainHandler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        live = intent.getBooleanExtra("live", false)
        val playerView = findViewById<PlayerView>(R.id.player_view)
        overlay = findViewById(R.id.info_overlay)

        val p = PlayerFactory.create(this)
        player = p
        playerView.player = p

        if (live) {
            playerView.useController = false
            playCurrentLive()
        } else {
            // Film / épisode : sous-titres (FR par défaut) + reprise
            playerView.setShowSubtitleButton(true)
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setPreferredTextLanguage("fr")
                .build()
            vodUrl = intent.getStringExtra("url") ?: ""
            if (vodUrl.isNotEmpty()) {
                p.setMediaItem(MediaItem.fromUri(vodUrl))
                p.prepare()
                val resume = Store.resumePos(this, vodUrl)
                if (resume > 10000) askResume(resume) else p.playWhenReady = true
                mainHandler.postDelayed(saveTick, 10000)
            }
        }
    }

    private fun askResume(posMs: Long) {
        val p = player ?: return
        val m = (posMs / 60000).toInt()
        val s = ((posMs / 1000) % 60).toInt()
        val t = String.format("%d:%02d", m, s)
        AlertDialog.Builder(this)
            .setTitle("Reprendre la lecture ?")
            .setMessage("Tu t'étais arrêté à $t.")
            .setPositiveButton("Reprendre à $t") { _, _ ->
                p.seekTo(posMs)
                p.playWhenReady = true
            }
            .setNegativeButton("Depuis le début") { _, _ ->
                p.seekTo(0)
                p.playWhenReady = true
            }
            .setCancelable(false)
            .show()
    }

    private fun saveProgress() {
        if (live || vodUrl.isEmpty()) return
        val p = player ?: return
        val pos = p.currentPosition
        val dur = p.duration
        if (dur > 0 && pos >= dur - 15000) {
            // terminé (ou presque) : on oublie la reprise
            Store.clearResumePos(this, vodUrl)
        } else if (pos > 10000) {
            Store.setResumePos(this, vodUrl, pos)
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
        if (live && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> { finish(); return true }
                KeyEvent.KEYCODE_DPAD_UP -> if (LiveSession.channels.isNotEmpty()) { zap(1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> if (LiveSession.channels.isNotEmpty()) { zap(-1); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        saveProgress()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(saveTick)
        saveProgress()
        player?.release()
        player = null
    }
}
