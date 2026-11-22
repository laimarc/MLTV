package com.marco.mltv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class PosterAdapter<T>(
    private var items: List<T>,
    private val titleOf: (T) -> String,
    private val posterOf: (T) -> String,
    private val onClick: (T) -> Unit
) : RecyclerView.Adapter<PosterAdapter<T>.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val poster: ImageView = v.findViewById(R.id.iv_poster)
        val title: TextView = v.findViewById(R.id.tv_title)
    }

    fun update(items: List<T>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poster, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = titleOf(item)
        val url = posterOf(item)
        if (url.isNotEmpty()) {
            holder.poster.load(url)
        } else {
            holder.poster.setImageDrawable(null)
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
