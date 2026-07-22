package com.example.eventpal_organization.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R

class BossHashMapRecyclerAdapter(
    private val context: Context,
    private val hashMapList: List<Pair<String, Map<String, String>>>
) : RecyclerView.Adapter<BossHashMapRecyclerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hashmapTitle: TextView = view.findViewById(R.id.hashmapTitle)
        val keyValueContainer: LinearLayout = view.findViewById(R.id.keyValueContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.boss_single_info_recycler_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (hashMapKey, hashMapValues) = hashMapList[position]
        holder.hashmapTitle.text = hashMapKey
        holder.keyValueContainer.removeAllViews()

        for ((key, value) in hashMapValues) {
            val itemView = LayoutInflater.from(context)
                .inflate(R.layout.boss_single_key_value_item, holder.keyValueContainer, false)

            val keyValueText: TextView = itemView.findViewById(R.id.keyValueText)
            keyValueText.text = "$key: $value"

            holder.keyValueContainer.addView(itemView)
        }
    }

    override fun getItemCount(): Int = hashMapList.size
}
