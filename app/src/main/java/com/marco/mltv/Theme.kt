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
            Opt("bleu", "Bleu", 0xFF3B82F6.toInt(), R.style.Accent_Bleu),
            Opt("vert", "Vert", 0xFF22B85C.toInt(), R.style.Accent_Vert),
            Opt("rouge", "Rouge", 0xFFE4484B.toInt(), R.style.Accent_Rouge),
            Opt("violet", "Violet", 0xFF8B5CF6.toInt(), R.style.Accent_Violet),
            Opt("cyan", "Cyan", 0xFF22B8CF.toInt(), R.style.Accent_Cyan),
            Opt("rose", "Rose", 0xFFEC4899.toInt(), R.style.Accent_Rose),
            Opt("jaune", "Jaune", 0xFFF5C518.toInt(), R.style.Accent_Jaune)
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
