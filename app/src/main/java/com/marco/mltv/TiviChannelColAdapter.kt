package com.marco.mltv

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

/** Colonne figée des chaînes (numéro + logo + nom), style TiViMate compact. */
class TiviChannelColAdapter(
    private var items: List<Channel>,
    private val onFocus: (Channel, Int) -> Unit,
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<TiviChannelColAdapter.VH>() {

    fun update(list: List<Channel>) { items = list; notifyDataSetChanged() }

    class VH(val root: LinearLayout, val num: TextView, val logo: ImageView, val name: TextView) :
        RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val d = ctx.resources.displayMetrics.density
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, TiviGrid.rowHeightPx(ctx)
            )
            isFocusable = true
            isClickable = true
            setBackgroundResource(R.drawable.item_bg)
            setPadding((8 * d).toInt(), 0, (6 * d).toInt(), 0)
        }
        val num = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((34 * d).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            setTextColor(0xFF9FB0C8.toInt()); textSize = 11f
        }
        val logo = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((34 * d).toInt(), (26 * d).toInt()).apply {
                marginEnd = (6 * d).toInt()
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val name = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(0xFFE6EDF3.toInt()); textSize = 11f; maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        root.addView(num); root.addView(logo); root.addView(name)
        return VH(root, num, logo, name)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.num.text = if (c.num > 0) c.num.toString() else ""
        holder.name.text = c.name
        if (c.logo.isNotEmpty()) holder.logo.load(c.logo) else holder.logo.setImageDrawable(null)
        holder.root.setOnFocusChangeListener { _, f -> if (f) onFocus(c, position) }
        holder.root.setOnClickListener { onClick(c) }
    }
}
