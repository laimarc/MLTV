package com.marco.mltv

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity

/** Gestion du thème de couleur (accent) choisi par l'utilisateur. */
object Theme {

    data class Opt(val key: String, val label: String, val color: Int, val style: Int)

    val options: List<Opt> by lazy {
        listOf(
            Opt("orange", "Orange", 0xFFFF7A18.toInt(), R.style.Accent_Orange),
            Opt("indigo", "Indigo", 0xFF6366F1.toInt(), R.style.Accent_Indigo),
            Opt("emeraude", "Émeraude", 0xFF10B981.toInt(), R.style.Accent_Emeraude),
            Opt("cyan", "Cyan", 0xFF06B6D4.toInt(), R.style.Accent_Cyan),
            Opt("rose", "Rose", 0xFFF43F5E.toInt(), R.style.Accent_Rose),
            Opt("violet", "Violet", 0xFFA855F7.toInt(), R.style.Accent_Violet),
            Opt("ambre", "Ambre", 0xFFF59E0B.toInt(), R.style.Accent_Ambre),
            Opt("sarcelle", "Sarcelle", 0xFF14B8A6.toInt(), R.style.Accent_Sarcelle)
        )
    }

    private fun current(ctx: Context): Opt =
        options.firstOrNull { it.key == Store.theme(ctx) } ?: options[0]

    /** À appeler au tout début de onCreate de chaque écran, avant setContentView. */
    fun apply(a: AppCompatActivity) {
        a.theme.applyStyle(current(a).style, true)
    }

    fun accent(ctx: Context): Int = resolve(ctx, R.attr.accentColor)
    fun onAccent(ctx: Context): Int = resolve(ctx, R.attr.accentText)

    private fun resolve(ctx: Context, attr: Int): Int {
        val tv = TypedValue()
        ctx.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }
}
