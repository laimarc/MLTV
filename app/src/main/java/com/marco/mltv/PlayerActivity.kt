package com.marco.mltv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.Button
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
    private var hasTextTracks = false
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

        // Reconnexion automatique en cas de coupure réseau / flux
        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { reconnect() }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) retries = 0
            }
        })

        if (live) {
            playerView.useController = false
            playCurrentLive()
        } else {
            // Film / épisode : sous-titres (FR par défaut au 1er affichage) + reprise
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setPreferredTextLanguage("fr")
                .build()
            val btnSubs = findViewById<Button>(R.id.btn_subs)
            btnSubs.setOnClickListener { showSubtitleMenu() }
            // n'apparaît qu'avec les commandes du lecteur, et seulement s'il y a des sous-titres
            playerView.setControllerVisibilityListener(
                PlayerView.ControllerVisibilityListener { vis ->
                    btnSubs.visibility =
                        if (vis == View.VISIBLE && hasTextTracks) View.VISIBLE else View.GONE
                }
            )
            p.addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    hasTextTracks = tracks.groups.any { it.type == C.TRACK_TYPE_TEXT }
                    if (!hasTextTracks) btnSubs.visibility = View.GONE
                }
            })
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

    private fun showSubtitleMenu() {
        val p = player ?: return
        val groups = p.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        // libellés : Désactivés + chaque piste
        data class Opt(val label: String, val groupIdx: Int, val trackIdx: Int)
        val opts = ArrayList<Opt>()
        opts.add(Opt("Désactivés", -1, -1))
        groups.forEachIndexed { gi, g ->
            for (ti in 0 until g.length) {
                val f = g.getTrackFormat(ti)
                val lang = f.language?.let { langName(it) } ?: "Piste ${ti + 1}"
                val extra = f.label?.let { " – $it" } ?: ""
                opts.add(Opt(lang + extra, gi, ti))
            }
        }
        val textDisabled = p.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        val curSel = when {
            textDisabled -> 0
            else -> opts.indexOfFirst { it.groupIdx >= 0 && groups[it.groupIdx].isTrackSelected(it.trackIdx) }.coerceAtLeast(0)
        }
        AlertDialog.Builder(this)
            .setTitle("Sous-titres")
            .setSingleChoiceItems(opts.map { it.label }.toTypedArray(), curSel) { d, which ->
                val o = opts[which]
                val b = p.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setPreferredTextLanguage(null)
                if (o.groupIdx < 0) {
                    b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                } else {
                    b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    b.setOverrideForType(
                        TrackSelectionOverride(groups[o.groupIdx].mediaTrackGroup, o.trackIdx)
                    )
                }
                p.trackSelectionParameters = b.build()
                d.dismiss()
            }
            .show()
    }

    private fun langName(code: String): String = when (code.lowercase().take(2)) {
        "fr" -> "Français"
        "en" -> "Anglais"
        "ar" -> "Arabe"
        "es" -> "Espagnol"
        "de" -> "Allemand"
        "it" -> "Italien"
        "pt" -> "Portugais"
        "nl" -> "Néerlandais"
        else -> code.uppercase()
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
