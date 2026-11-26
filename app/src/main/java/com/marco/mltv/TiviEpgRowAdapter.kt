package com.marco.mltv

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Lignes de la grille EPG défilable (cellules de programmes placées dans le temps). */
class TiviEpgRowAdapter(
    private var items: List<Channel>,
    private val onCellFocus: (Channel, Int, Epg.Prog?) -> Unit,
    private val onPlay: (Channel) -> Unit,
    private val onReplay: (Channel, Epg.Prog) -> Unit
) : RecyclerView.Adapter<TiviEpgRowAdapter.VH>() {

    fun update(list: List<Channel>) { items = list; notifyDataSetChanged() }

    class VH(val row: LinearLayout) : RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                TiviGrid.widthPx(ctx, TiviGrid.totalMinutes().toLong()),
                TiviGrid.rowHeightPx(ctx)
            )
        }
        return VH(row)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.row.context
        val c = items[position]
        val d = ctx.resources.displayMetrics.density
        val accent = Theme.accent(ctx)
        val now = System.currentTimeMillis()
        holder.row.removeAllViews()

        val progs = Epg.list(c.epgId).filter { it.stop > TiviGrid.gridStart && it.start < TiviGrid.gridEnd }

        fun spacer(minutes: Long) {
            if (minutes <= 0) return
            val v = View(ctx)
            v.layoutParams = LinearLayout.LayoutParams(TiviGrid.widthPx(ctx, minutes), LinearLayout.LayoutParams.MATCH_PARENT)
            holder.row.addView(v)
        }

        fun cell(p: Epg.Prog) {
            val start = maxOf(p.start, TiviGrid.gridStart)
            val end = minOf(p.stop, TiviGrid.gridEnd)
            val w = TiviGrid.widthPx(ctx, (end - start) / 60000L)
            if (w <= 0) return
            val isNow = p.start <= now && now < p.stop
            val isPast = p.stop <= now
            val cv = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    marginEnd = (2 * d).toInt(); topMargin = (2 * d).toInt(); bottomMargin = (2 * d).toInt()
                }
                gravity = Gravity.CENTER_VERTICAL
                setPadding((8 * d).toInt(), 0, (6 * d).toInt(), 0)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                textSize = 12f
                text = p.title
                isFocusable = true
                isClickable = true
                tag = p
                when {
                    isNow -> { setBackgroundColor(withAlpha(accent, 0x55)); setTextColor(accent) }
                    isPast -> { setBackgroundColor(0x14FFFFFF); setTextColor(0xFF93A1B3.toInt()) }
                    else -> { setBackgroundColor(0x26FFFFFF); setTextColor(0xFFD6DEE8.toInt()) }
                }
                setOnFocusChangeListener { _, f -> if (f) onCellFocus(c, position, p) }
                setOnClickListener {
                    when {
                        p.start <= now && now < p.stop -> onPlay(c)
                        p.stop <= now && c.archive -> onReplay(c, p)
                        else -> onPlay(c)
                    }
                }
            }
            holder.row.addView(cv)
        }

        if (progs.isEmpty()) {
            val v = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    TiviGrid.widthPx(ctx, TiviGrid.totalMinutes().toLong()),
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER_VERTICAL
                setPadding((8 * d).toInt(), 0, 0, 0)
                setTextColor(0xFF6B788A.toInt()); textSize = 12f; text = "Pas d'info"
                isFocusable = true; isClickable = true
                setOnFocusChangeListener { _, f -> if (f) onCellFocus(c, position, null) }
                setOnClickListener { onPlay(c) }
            }
            holder.row.addView(v)
            return
        }

        var cursor = TiviGrid.gridStart
        for (p in progs) {
            val s = maxOf(p.start, TiviGrid.gridStart)
            if (s > cursor) spacer((s - cursor) / 60000L)
            cell(p)
            cursor = minOf(p.stop, TiviGrid.gridEnd)
        }
        if (cursor < TiviGrid.gridEnd) spacer((TiviGrid.gridEnd - cursor) / 60000L)
    }

    private fun withAlpha(color: Int, a: Int): Int =
        Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
}
