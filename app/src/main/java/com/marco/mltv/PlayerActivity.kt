package com.marco.mltv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
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
        Theme.apply(this)
        setContentView(R.layout.activity_player)

        live = intent.getBooleanExtra("live", false)
        val playerView = findViewById<PlayerView>(R.id.player_view)
        overlay = findViewById(R.id.info_overlay)

        val p = PlayerFactory.create(this)
        player = p
        playerView.player = p

        // Reconnexion automatique en cas de coupure réseau / flux
        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (!live) {
                    Toast.makeText(this@PlayerActivity, "Lecture impossible", Toast.LENGTH_SHORT).show()
                }
                reconnect()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) retries = 0
            }
        })

        if (live) {
            playerView.useController = false
            playCurrentLive()
        } else {
            // Film / épisode : bouton sous-titres natif (dans la barre, à côté de la roue),
            // sous-titres forcés affichés d'office + reprise
            playerView.setShowSubtitleButton(true)
            p.addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) { applyForcedDefault(tracks) }
            })
            vodUrl = intent.getStringExtra("url") ?: ""
            if (vodUrl.isNotEmpty()) {
                if (intent.getBooleanExtra("catchup", false)) {
                    // Rattrapage : flux TS brut via la couche réseau tolérante
                    val factory = TolerantHttpDataSource.Factory("VLC/3.0.20 LibVLC/3.0.20")
                    val source = ProgressiveMediaSource.Factory(factory)
                        .createMediaSource(MediaItem.fromUri(vodUrl))
                    p.setMediaSource(source)
                    p.prepare()
                    p.playWhenReady = true
                } else {
                    p.setMediaItem(MediaItem.fromUri(vodUrl))
                    p.prepare()
                    val resume = Store.resumePos(this, vodUrl)
                    if (resume > 10000) askResume(resume) else p.playWhenReady = true
                    mainHandler.postDelayed(saveTick, 10000)
                }
            }
        }
    }

    /** Au 1er chargement des pistes : affiche la piste forcée (dialogues étrangers)
     *  comme au cinéma ; sinon sous-titres coupés. L'utilisateur garde la main ensuite. */
    private var autoSubDone = false
    private fun applyForcedDefault(tracks: Tracks) {
        if (autoSubDone) return
        val p = player ?: return
        val groups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        if (groups.isEmpty()) return
        autoSubDone = true

        var grp: Tracks.Group? = null
        var idx = -1
        outer@ for (g in groups) {
            for (i in 0 until g.length) {
                val f = g.getTrackFormat(i)
                val forced = (f.selectionFlags and C.SELECTION_FLAG_FORCED) != 0
                if (forced) {
                    val isFr = f.language?.lowercase()?.take(2) == "fr"
                    if (isFr) { grp = g; idx = i; break@outer }
                    if (grp == null) { grp = g; idx = i }
                }
            }
        }

        val b = p.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setPreferredTextLanguage(null)
        if (grp != null) {
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            b.setOverrideForType(TrackSelectionOverride(grp.mediaTrackGroup, idx))
        } else {
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true) // pas de forcé : coupé par défaut
        }
        p.trackSelectionParameters = b.build()
    }

    private fun askResume(posMs: Long) {
        val p = player ?: return
        val m = (posMs / 60000).toInt()
        val s = ((posMs / 1000) % 60).toInt()
        val t = String.format("%d:%02d", m, s)
        AlertDialog.Builder(this, R.style.MLTVDialog)
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
            playUrl(url, name, epgId, "")
            return
        }
        val idx = LiveSession.index.coerceIn(0, list.lastIndex)
        LiveSession.index = idx
        val c = list[idx]
        playUrl(c.url, c.name, c.epgId, c.logo)
    }

    private fun playUrl(url: String, name: String, epgId: String, logo: String) {
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
            findViewById<ProgressBar>(R.id.pb_info),
            logo,
            findViewById(R.id.iv_info_logo)
        )
        overlay.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideOverlay)
        mainHandler.postDelayed(hideOverlay, 6000)
    }

    private var retries = 0
    private val reconnectRun = Runnable {
        val p = player ?: return@Runnable
        if (live) p.seekToDefaultPosition()   // revient au bord du direct
        p.prepare()                            // la VOD garde sa position
        p.playWhenReady = true
    }

    private fun reconnect() {
        retries++
        val delay = if (retries <= 3) 1500L else 4000L
        mainHandler.removeCallbacks(reconnectRun)
        mainHandler.postDelayed(reconnectRun, delay)
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
        mainHandler.removeCallbacks(reconnectRun)
        saveProgress()
        player?.release()
        player = null
    }
}
