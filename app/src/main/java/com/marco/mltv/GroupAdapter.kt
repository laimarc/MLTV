package com.marco.mltv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(
    private var groups: List<String>,
    private var current: String,
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit = {}
) : RecyclerView.Adapter<GroupAdapter.VH>() {

    private var movingKey: String? = null

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tv_group)
    }

    fun update(groups: List<String>, current: String) {
        this.groups = groups
        this.current = current
        notifyDataSetChanged()
    }

    /** Met à jour la surbrillance « sélection » sans reconstruire la liste (garde le focus). */
    fun setCurrent(newCur: String) {
        val old = groups.indexOf(current)
        current = newCur
        val neu = groups.indexOf(newCur)
        if (old >= 0) notifyItemChanged(old)
        if (neu >= 0) notifyItemChanged(neu)
    }

    /** Marque l'élément en cours de déplacement (ou null). */
    fun setMoving(key: String?) {
        val oldIdx = movingKey?.let { groups.indexOf(it) } ?: -1
        movingKey = key
        val newIdx = key?.let { groups.indexOf(it) } ?: -1
        if (oldIdx >= 0) notifyItemChanged(oldIdx)
        if (newIdx >= 0) notifyItemChanged(newIdx)
    }

    /** Déplace un élément et anime (conserve le focus). */
    fun moveItem(from: Int, to: Int) {
        if (from < 0 || to < 0 || from >= groups.size || to >= groups.size) return
        val list = groups.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        groups = list
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = groups[position]
        holder.tv.text = g
        val ctx = holder.itemView.context
        if (g == movingKey) {
            holder.tv.setBackgroundResource(R.drawable.btn_accent)
            holder.tv.setTextColor(Theme.onAccent(ctx))
            holder.tv.isSelected = false
        } else {
            holder.tv.setBackgroundResource(R.drawable.item_bg)
            holder.tv.setTextColor(ContextCompat.getColor(ctx, R.color.txt))
            holder.tv.isSelected = g == current
        }
        holder.tv.setOnClickListener { onClick(g) }
        holder.tv.setOnLongClickListener { onLongClick(g); true }
    }

    override fun getItemCount(): Int = groups.size
}
