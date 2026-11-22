package com.marco.mltv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EpgProgAdapter(
    private var items: List<Epg.Prog>,
    private var archive: Boolean = false,
    private val onReplay: (Epg.Prog) -> Unit = {},
    private val onFocus: (Epg.Prog) -> Unit = {}
) : RecyclerView.Adapter<EpgProgAdapter.VH>() {

    private val hm = SimpleDateFormat("HH:mm", Locale.FRANCE)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val time: TextView = v.findViewById(R.id.tv_time)
        val title: TextView = v.findViewById(R.id.tv_title)
    }

    fun update(items: List<Epg.Prog>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun update(items: List<Epg.Prog>, archive: Boolean) {
        this.archive = archive
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prog, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.time.text = hm.format(Date(p.start)) + " – " + hm.format(Date(p.stop))

        val now = System.currentTimeMillis()
        val isNow = p.start <= now && now < p.stop
        val isPast = p.stop <= now
        val replayable = archive && isPast

        holder.title.text = if (replayable) "⟲ ${p.title}" else p.title

        val accent = 0xFFFF7A18.toInt()
        val txt = 0xFFE6EDF3.toInt()
        val muted = 0xFF8B98A9.toInt()
        holder.title.setTextColor(if (isNow) accent else txt)
        holder.time.setTextColor(if (isNow) accent else muted)

        holder.itemView.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) onFocus(p) }

        if (replayable) {
            holder.itemView.setOnClickListener { onReplay(p) }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = items.size
}
