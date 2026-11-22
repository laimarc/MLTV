package com.marco.mltv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Lecture plein écran. En direct : pas de barre de défilement + overlay info chaîne. */
@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val url = intent.getStringExtra("url") ?: ""
        val name = intent.getStringExtra("title") ?: ""
        val live = intent.getBooleanExtra("live", false)
        val epgId = intent.getStringExtra("epgId") ?: ""

        val playerView = findViewById<PlayerView>(R.id.player_view)

        val p = ExoPlayer.Builder(this).build()
        player = p
        playerView.player = p

        if (live) {
            // Pas de barre de défilement pour le direct
            playerView.findViewById<View?>(androidx.media3.ui.R.id.exo_progress)?.visibility = View.GONE
            playerView.findViewById<View?>(androidx.media3.ui.R.id.exo_position)?.visibility = View.GONE
            playerView.findViewById<View?>(androidx.media3.ui.R.id.exo_duration)?.visibility = View.GONE

            // Overlay info chaîne
            val overlay = findViewById<View>(R.id.info_overlay)
            InfoOverlay.populate(
                name, epgId,
                findViewById(R.id.tv_info_channel),
                findViewById(R.id.tv_info_now),
                findViewById(R.id.tv_info_next),
                findViewById<ProgressBar>(R.id.pb_info)
            )
            overlay.visibility = View.VISIBLE
            mainHandler.postDelayed({ overlay.visibility = View.GONE }, 6000)
        }

        if (url.isNotEmpty()) {
            p.setMediaItem(MediaItem.fromUri(url))
            p.prepare()
            p.playWhenReady = true
        }
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
