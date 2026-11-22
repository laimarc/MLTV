package com.marco.mltv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(
    private var groups: List<String>,
    private var current: String,
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit = {}
) : RecyclerView.Adapter<GroupAdapter.VH>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = groups[position]
        holder.tv.text = g
        holder.tv.isSelected = g == current
        holder.tv.setOnClickListener { onClick(g) }
        holder.tv.setOnLongClickListener { onLongClick(g); true }
    }

    override fun getItemCount(): Int = groups.size
}
