package com.marco.mltv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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

        // ---- Diagnostic rattrapage ----
        findViewById<Button>(R.id.btn_diag).setOnClickListener { runCatchupDiag() }

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

    /** Récupère la playlist M3U et affiche la 1re ligne de chaîne contenant « catchup »,
     *  plus un exemple de ligne et d'URL de flux, pour connaître le format de rattrapage. */
    private fun runCatchupDiag() {
        val host = Store.host(this).trimEnd('/')
        val u = Store.user(this)
        val p = Store.pass(this)
        if (host.isEmpty() || u.isEmpty() || p.isEmpty()) {
            Toast.makeText(this, "Connecte-toi d'abord en Xtream.", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Recherche dans la playlist…", Toast.LENGTH_SHORT).show()
        Thread {
            val msg = try {
                val url = "$host/get.php?username=${URLEncoder.encode(u, "UTF-8")}" +
                    "&password=${URLEncoder.encode(p, "UTF-8")}&type=m3u_plus&output=ts"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "VLC/3.0.20 LibVLC/3.0.20")
                    connectTimeout = 15000
                    readTimeout = 25000
                }
                conn.inputStream.bufferedReader().use { br ->
                    var line: String?
                    var count = 0
                    var sampleExtinf: String? = null
                    var sampleUrl: String? = null
                    var catchLine: String? = null
                    while (br.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        if (l.startsWith("#EXTINF")) {
                            if (sampleExtinf == null) sampleExtinf = l
                            if (catchLine == null && l.contains("catchup", ignoreCase = true)) catchLine = l
                        } else if (l.startsWith("http") && sampleUrl == null) {
                            sampleUrl = l
                        }
                        if (catchLine != null && sampleUrl != null) break
                        if (++count > 400000) break
                    }
                    when {
                        catchLine != null -> "LIGNE CATCHUP TROUVÉE :\n\n$catchLine"
                        else -> "Aucun 'catchup-source' dans la playlist.\n\n" +
                            "Exemple de chaîne :\n${sampleExtinf ?: "(aucune)"}\n\n" +
                            "Exemple d'URL de flux :\n${sampleUrl ?: "(aucune)"}"
                    }
                }
            } catch (e: Exception) {
                "Erreur : ${e.message}"
            }
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Diagnostic rattrapage")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }.start()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            val cur = currentFocus
            if (cur !is EditText) {
                val next = cur?.focusSearch(View.FOCUS_LEFT)
                if (next == null || next === cur) {
                    // retour à la liste des chaînes (si une source est configurée)
                    if (Store.type(this).isNotEmpty()) goMain()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
