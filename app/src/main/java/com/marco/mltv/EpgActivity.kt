package com.marco.mltv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Guide complet d'une chaîne : liste des programmes, celui en cours surligné. */
class EpgActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var adapter: EpgProgAdapter
    private lateinit var rv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epg)

        val epgId = intent.getStringExtra("epgId") ?: ""
        val name = intent.getStringExtra("name") ?: "Guide"
        findViewById<TextView>(R.id.tv_header).text = "Guide · $name"
        status = findViewById(R.id.tv_status)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        rv = findViewById(R.id.rv_epg)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = EpgProgAdapter(emptyList())
        rv.adapter = adapter

        if (epgId.isEmpty()) {
            status.text = "Cette chaîne n'a pas d'identifiant EPG."
            return
        }

        if (Epg.loaded) {
            show(epgId)
        } else {
            status.text = "Chargement du guide…"
            Thread {
                try {
                    Epg.ensureLoaded(Store.host(this), Store.user(this), Store.pass(this))
                    mainHandler.post { show(epgId) }
                } catch (e: Exception) {
                    mainHandler.post { status.text = "EPG indisponible : ${e.message}" }
                }
            }.start()
        }
    }

    private fun show(epgId: String) {
        val list = Epg.list(epgId)
        if (list.isEmpty()) {
            status.text = "Aucun programme pour cette chaîne."
            return
        }
        adapter.update(list)
        status.text = "${list.size} programmes"
        // positionne sur le programme en cours
        val now = System.currentTimeMillis()
        val idx = list.indexOfFirst { it.start <= now && now < it.stop }
        if (idx >= 0) (rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(idx, 0)
    }
}
