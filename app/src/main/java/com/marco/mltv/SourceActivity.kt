package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SourceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Déjà configuré et lancement normal -> aller directement au lecteur
        val editing = intent.getBooleanExtra("edit", false)
        if (!editing && Store.hasConfig(this)) {
            goMain()
            return
        }

        setContentView(R.layout.activity_source)

        val etM3u = findViewById<EditText>(R.id.et_m3u)
        val etHost = findViewById<EditText>(R.id.et_host)
        val etUser = findViewById<EditText>(R.id.et_user)
        val etPass = findViewById<EditText>(R.id.et_pass)
        val err = findViewById<TextView>(R.id.tv_err)

        // Pré-remplissage
        if (Store.type(this) == "m3u") etM3u.setText(Store.m3uUrl(this))
        if (Store.type(this) == "xtream") {
            etHost.setText(Store.host(this))
            etUser.setText(Store.user(this))
            etPass.setText(Store.pass(this))
        }

        findViewById<Button>(R.id.btn_m3u).setOnClickListener {
            val url = etM3u.text.toString().trim()
            if (url.isEmpty()) {
                err.text = "Renseigne l'URL de la playlist."
            } else {
                Store.saveM3u(this, url)
                goMain()
            }
        }

        findViewById<Button>(R.id.btn_xtream).setOnClickListener {
            val host = etHost.text.toString().trim()
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()
            if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                err.text = "Renseigne serveur, utilisateur et mot de passe."
            } else {
                Store.saveXtream(this, host, user, pass)
                goMain()
            }
        }

        // ---- Fluidité (mémoire tampon) ----
        val bEq = findViewById<Button>(R.id.btn_buf_equilibre)
        val bCo = findViewById<Button>(R.id.btn_buf_confort)
        val bMx = findViewById<Button>(R.id.btn_buf_maxi)
        fun refreshBuf() {
            val m = Store.bufferMode(this)
            for ((btn, mode) in listOf(bEq to "equilibre", bCo to "confort", bMx to "maxi")) {
                if (mode == m) {
                    btn.setBackgroundResource(R.drawable.btn_accent)
                    btn.setTextColor(ContextCompat.getColor(this, R.color.accent_text))
                } else {
                    btn.setBackgroundResource(R.drawable.btn_secondary)
                    btn.setTextColor(ContextCompat.getColor(this, R.color.txt))
                }
            }
        }
        bEq.setOnClickListener { Store.setBufferMode(this, "equilibre"); refreshBuf() }
        bCo.setOnClickListener { Store.setBufferMode(this, "confort"); refreshBuf() }
        bMx.setOnClickListener { Store.setBufferMode(this, "maxi"); refreshBuf() }
        refreshBuf()

        // ---- Onglets Source / Fluidité ----
        val secSource = findViewById<View>(R.id.sec_source)
        val secBuffer = findViewById<View>(R.id.sec_buffer)
        val tabSource = findViewById<Button>(R.id.btn_tab_source)
        val tabBuffer = findViewById<Button>(R.id.btn_tab_buffer)
        fun showTab(source: Boolean) {
            secSource.visibility = if (source) View.VISIBLE else View.GONE
            secBuffer.visibility = if (source) View.GONE else View.VISIBLE
            tabSource.setBackgroundResource(if (source) R.drawable.btn_accent else R.drawable.btn_secondary)
            tabSource.setTextColor(ContextCompat.getColor(this, if (source) R.color.accent_text else R.color.txt))
            tabBuffer.setBackgroundResource(if (source) R.drawable.btn_secondary else R.drawable.btn_accent)
            tabBuffer.setTextColor(ContextCompat.getColor(this, if (source) R.color.txt else R.color.accent_text))
        }
        tabSource.setOnClickListener { showTab(true) }
        tabBuffer.setOnClickListener { showTab(false) }
        showTab(true)

        // ---- Version ----
        findViewById<TextView>(R.id.tv_version).text = "MLTV  v${BuildConfig.VERSION_NAME}"
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            val cur = currentFocus
            if (cur !is EditText) {
                val next = cur?.focusSearch(View.FOCUS_LEFT)
                if (next == null || next === cur) { finish(); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
