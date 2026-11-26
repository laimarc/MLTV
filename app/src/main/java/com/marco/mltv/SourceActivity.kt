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
        Theme.apply(this)

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
                    btn.setTextColor(Theme.onAccent(this))
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

        // ---- Onglets Source / Fluidité / Thème ----
        val secSource = findViewById<View>(R.id.sec_source)
        val secBuffer = findViewById<View>(R.id.sec_buffer)
        val secTheme = findViewById<View>(R.id.sec_theme)
        val tabSource = findViewById<Button>(R.id.btn_tab_source)
        val tabBuffer = findViewById<Button>(R.id.btn_tab_buffer)
        val tabTheme = findViewById<Button>(R.id.btn_tab_theme)
        val tabs = listOf(tabSource to secSource, tabBuffer to secBuffer, tabTheme to secTheme)
        fun showTab(active: View) {
            for ((tab, sec) in tabs) {
                val on = tab === active
                sec.visibility = if (on) View.VISIBLE else View.GONE
                tab.setBackgroundResource(if (on) R.drawable.btn_accent else R.drawable.btn_secondary)
                tab.setTextColor(if (on) Theme.onAccent(this) else ContextCompat.getColor(this, R.color.txt))
            }
        }
        tabSource.setOnClickListener { showTab(tabSource) }
        tabBuffer.setOnClickListener { showTab(tabBuffer) }
        tabTheme.setOnClickListener { showTab(tabTheme) }
        if (openThemeTab) { openThemeTab = false; showTab(tabTheme) } else showTab(tabSource)

        // ---- Pastilles de couleur (thème) ----
        buildThemeSwatches(findViewById(R.id.theme_container))

        // ---- Style d'interface + transparence ----
        wireDisplayOptions()

        // ---- Version ----
        findViewById<TextView>(R.id.tv_version).text = "MLTV  v${BuildConfig.VERSION_NAME}"
    }

    private fun buildThemeSwatches(container: android.widget.LinearLayout) {
        container.removeAllViews()
        val cur = Store.theme(this)
        val dp = resources.displayMetrics.density
        for (opt in Theme.options) {
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                isFocusable = true
                isClickable = true
                setBackgroundResource(R.drawable.item_bg)
                setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * dp).toInt() }
            }
            val dot = View(this).apply {
                val d = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(opt.color)
                }
                background = d
                layoutParams = android.widget.LinearLayout.LayoutParams((26 * dp).toInt(), (26 * dp).toInt())
            }
            val label = TextView(this).apply {
                text = opt.label + if (opt.key == cur) "   ✓" else ""
                setTextColor(ContextCompat.getColor(this@SourceActivity, R.color.txt))
                textSize = 16f
                val lp = android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.leftMargin = (14 * dp).toInt()
                layoutParams = lp
            }
            row.addView(dot)
            row.addView(label)
            row.setOnClickListener {
                if (Store.theme(this) != opt.key) {
                    Store.setTheme(this, opt.key)
                    openThemeTab = true
                    recreate()
                }
            }
            container.addView(row)
        }
    }

    private fun wireDisplayOptions() {
        val sMltv = findViewById<Button>(R.id.btn_style_mltv)
        val sTivi = findViewById<Button>(R.id.btn_style_tivi)
        fun refreshStyle() {
            val tivi = Store.uiStyle(this) == "tivimate"
            sMltv.setBackgroundResource(if (!tivi) R.drawable.btn_accent else R.drawable.btn_secondary)
            sMltv.setTextColor(if (!tivi) Theme.onAccent(this) else ContextCompat.getColor(this, R.color.txt))
            sTivi.setBackgroundResource(if (tivi) R.drawable.btn_accent else R.drawable.btn_secondary)
            sTivi.setTextColor(if (tivi) Theme.onAccent(this) else ContextCompat.getColor(this, R.color.txt))
        }
        sMltv.setOnClickListener { Store.setUiStyle(this, "mltv"); refreshStyle() }
        sTivi.setOnClickListener { Store.setUiStyle(this, "tivimate"); refreshStyle() }
        refreshStyle()

        val levels = listOf(
            R.id.btn_tr_60 to 60, R.id.btn_tr_78 to 78,
            R.id.btn_tr_92 to 92, R.id.btn_tr_100 to 100
        )
        fun refreshTr() {
            val cur = Store.panelAlpha(this)
            for ((id, v) in levels) {
                val b = findViewById<Button>(id)
                val on = cur == v
                b.setBackgroundResource(if (on) R.drawable.btn_accent else R.drawable.btn_secondary)
                b.setTextColor(if (on) Theme.onAccent(this) else ContextCompat.getColor(this, R.color.txt))
            }
        }
        for ((id, v) in levels) {
            findViewById<Button>(id).setOnClickListener { Store.setPanelAlpha(this, v); refreshTr() }
        }
        refreshTr()
    }

    companion object { private var openThemeTab = false }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
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
