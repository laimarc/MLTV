package com.marco.mltv

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

/** Une ligne de la grille EPG style TiViMate : infos chaîne à gauche + frise de programmes. */
class TiviRowAdapter(
    private var items: List<Channel>,
    private var winStart: Long,
    private val spanMin: Int,
    private val onFocus: (Channel) -> Unit,
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<TiviRowAdapter.VH>() {

    fun update(list: List<Channel>, start: Long) {
        items = list
        winStart = start
        notifyDataSetChanged()
    }

    class VH(val root: LinearLayout, val num: TextView, val logo: ImageView,
             val name: TextView, val rep: TextView, val lane: LinearLayout) :
        RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val d = ctx.resources.displayMetrics.density
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, (62 * d).toInt()
            )
            isFocusable = true
            isClickable = true
            setBackgroundResource(R.drawable.item_bg)
        }
        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams((300 * d).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
            setPadding((10 * d).toInt(), 0, (6 * d).toInt(), 0)
        }
        val num = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((42 * d).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            setTextColor(0xFF9FB0C8.toInt()); textSize = 13f
        }
        val logo = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((40 * d).toInt(), (32 * d).toInt()).apply {
                marginEnd = (8 * d).toInt()
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val name = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(0xFFE6EDF3.toInt()); textSize = 13f; maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val rep = TextView(ctx).apply {
            text = "⟲"; setTextColor(0xFF8B98A9.toInt()); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        info.addView(num); info.addView(logo); info.addView(name); info.addView(rep)
        val lane = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(info); root.addView(lane)
        return VH(root, num, logo, name, rep, lane)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.num.text = if (c.num > 0) c.num.toString() else ""
        holder.name.text = c.name
        holder.rep.visibility = if (c.archive) View.VISIBLE else View.GONE
        if (c.logo.isNotEmpty()) holder.logo.load(c.logo) else holder.logo.setImageDrawable(null)

        holder.root.setOnFocusChangeListener { _, f -> if (f) onFocus(c) }
        holder.root.setOnClickListener { onClick(c) }

        buildLane(holder.lane, c)
    }

    private fun buildLane(lane: LinearLayout, c: Channel) {
        lane.removeAllViews()
        val ctx = lane.context
        val d = ctx.resources.displayMetrics.density
        val accent = Theme.accent(ctx)
        val winEnd = winStart + spanMin * 60000L
        val now = System.currentTimeMillis()
        val progs = Epg.list(c.epgId).filter { it.stop > winStart && it.start < winEnd }

        fun addBlock(weightMin: Float, title: String, current: Boolean, filler: Boolean) {
            if (weightMin <= 0f) return
            val b = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weightMin).apply {
                    marginEnd = (1 * d).toInt()
                }
                gravity = Gravity.CENTER_VERTICAL
                setPadding((8 * d).toInt(), 0, (6 * d).toInt(), 0)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                textSize = 12f
                text = title
                if (filler) {
                    setBackgroundColor(0x11FFFFFF)
                    setTextColor(0xFF6B788A.toInt())
                } else if (current) {
                    setBackgroundColor(withAlpha(accent, 0x44))
                    setTextColor(accent)
                } else {
                    setBackgroundColor(0x22FFFFFF)
                    setTextColor(0xFFCFD8E3.toInt())
                }
            }
            lane.addView(b)
        }

        if (progs.isEmpty()) {
            addBlock(spanMin.toFloat(), "Pas d'info", current = false, filler = true)
            return
        }
        var cursor = winStart
        for (p in progs) {
            val segStart = maxOf(p.start, winStart)
            if (segStart > cursor) addBlock((segStart - cursor) / 60000f, "", false, true)
            val segEnd = minOf(p.stop, winEnd)
            val isNow = p.start <= now && now < p.stop
            addBlock((segEnd - segStart) / 60000f, p.title, isNow, false)
            cursor = segEnd
        }
        if (cursor < winEnd) addBlock((winEnd - cursor) / 60000f, "", false, true)
    }

    private fun withAlpha(color: Int, a: Int): Int =
        Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
}
