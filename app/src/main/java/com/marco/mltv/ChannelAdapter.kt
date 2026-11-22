package com.marco.mltv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ChannelAdapter(
    private var items: List<Channel>,
    private val favUrls: () -> Set<String>,
    private val nowTitle: (String) -> String?,
    private val onClick: (Channel) -> Unit,
    private val onLongClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val badge: TextView = v.findViewById(R.id.tv_badge)
        val logo: ImageView = v.findViewById(R.id.iv_logo)
        val name: TextView = v.findViewById(R.id.tv_name)
        val groupName: TextView = v.findViewById(R.id.tv_group_name)
        val star: TextView = v.findViewById(R.id.tv_star)
    }

    fun update(items: List<Channel>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.badge.text = badgeOf(c.name)
        if (c.logo.isNotEmpty()) {
            holder.logo.visibility = View.VISIBLE
            holder.logo.load(c.logo)
        } else {
            holder.logo.visibility = View.GONE
            holder.logo.setImageDrawable(null)
        }
        holder.name.text = c.name
        val now = if (c.epgId.isNotEmpty()) nowTitle(c.epgId) else null
        holder.groupName.text = now ?: c.group
        holder.star.visibility = if (favUrls().contains(c.url)) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(c) }
        holder.itemView.setOnLongClickListener {
            onLongClick(c)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    private fun badgeOf(name: String): String {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return "?"
        val parts = cleaned.split(" ", "-", "_").filter { it.isNotEmpty() }
        val letters = if (parts.size >= 2) {
            "" + parts[0][0] + parts[1][0]
        } else {
            cleaned.take(2)
        }
        return letters.uppercase()
    }
}
