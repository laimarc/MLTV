package com.marco.mltv

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ManageRow(val key: String, val label: String, val hidden: Boolean)

class ManageAdapter(
    private var items: List<ManageRow>,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<ManageAdapter.VH>() {

    private var movingKey: String? = null

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val label: TextView = v.findViewById(R.id.tv_label)
        val badge: TextView = v.findViewById(R.id.tv_badge)
    }

    fun update(items: List<ManageRow>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun setMoving(key: String?) {
        val oldIdx = movingKey?.let { k -> items.indexOfFirst { it.key == k } } ?: -1
        movingKey = key
        val newIdx = key?.let { k -> items.indexOfFirst { it.key == k } } ?: -1
        if (oldIdx >= 0) notifyItemChanged(oldIdx)
        if (newIdx >= 0) notifyItemChanged(newIdx)
    }

    fun moveItem(from: Int, to: Int) {
        if (from < 0 || to < 0 || from >= items.size || to >= items.size) return
        val list = items.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        items = list
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.label.text = r.label
        val ctx = holder.itemView.context

        if (r.key == movingKey) {
            holder.itemView.setBackgroundResource(R.drawable.btn_accent)
            holder.label.setTextColor(ContextCompat.getColor(ctx, R.color.accent_text))
        } else {
            holder.itemView.setBackgroundResource(R.drawable.item_bg)
            holder.label.setTextColor(ContextCompat.getColor(ctx, R.color.txt))
        }

        if (r.hidden) {
            holder.label.paintFlags = holder.label.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.label.alpha = 0.5f
            holder.badge.visibility = View.VISIBLE
        } else {
            holder.label.paintFlags = holder.label.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.label.alpha = 1.0f
            holder.badge.visibility = View.GONE
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(holder.bindingAdapterPosition); true
        }
    }

    override fun getItemCount(): Int = items.size
}
