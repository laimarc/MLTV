package com.marco.mltv

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ManageRow(val key: String, val label: String, val hidden: Boolean)

class ManageAdapter(
    private var items: List<ManageRow>,
    private val onUp: (Int) -> Unit,
    private val onDown: (Int) -> Unit,
    private val onHide: (Int) -> Unit
) : RecyclerView.Adapter<ManageAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val label: TextView = v.findViewById(R.id.tv_label)
        val up: Button = v.findViewById(R.id.btn_up)
        val down: Button = v.findViewById(R.id.btn_down)
        val hide: Button = v.findViewById(R.id.btn_hide)
    }

    fun update(items: List<ManageRow>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.label.text = r.label
        if (r.hidden) {
            holder.label.paintFlags = holder.label.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.label.alpha = 0.5f
            holder.hide.text = "Afficher"
        } else {
            holder.label.paintFlags = holder.label.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.label.alpha = 1.0f
            holder.hide.text = "Masquer"
        }
        holder.up.setOnClickListener { onUp(holder.bindingAdapterPosition) }
        holder.down.setOnClickListener { onDown(holder.bindingAdapterPosition) }
        holder.hide.setOnClickListener { onHide(holder.bindingAdapterPosition) }
    }

    override fun getItemCount(): Int = items.size
}
