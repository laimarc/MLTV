package com.marco.mltv

import android.content.Intent
import android.os.Bundle
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

        // ---- Version ----
        findViewById<TextView>(R.id.tv_version).text = "MLTV  v${BuildConfig.VERSION_NAME}"
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
