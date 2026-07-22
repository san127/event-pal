package com.example.eventpal_organization.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R

class HashMapRecyclerAdapter(
    private val context: Context,
    private val hashMapList: List<Pair<String, Map<String, String>>>,
    private val listener: OnHashMapActionListener
) : RecyclerView.Adapter<HashMapRecyclerAdapter.ViewHolder>() {

    interface OnHashMapActionListener {
        fun onEditKeyValue(hashMapKey: String, key: String, value: String)
        fun onAddKeyValue(hashMapKey: String)
        fun onDeleteKeyValue(hashMapKey: String, key: String)
        fun onDeleteHashMap(hashMapKey: String)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hashmapTitle: TextView = view.findViewById(R.id.hashmapTitle)
        val keyValueContainer: LinearLayout = view.findViewById(R.id.keyValueContainer)
        val btnAdd: AppCompatButton = view.findViewById(R.id.btnAddKeyValue)  // ✅ FIXED
        val btnDeleteHashMap: AppCompatButton = view.findViewById(R.id.btnDeleteHashMap)  // ✅ FIXED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_info_recyc_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (hashMapKey, hashMapValues) = hashMapList[position]
        holder.hashmapTitle.text = hashMapKey
        holder.keyValueContainer.removeAllViews()

        for ((key, value) in hashMapValues) {
            val itemView = LayoutInflater.from(context).inflate(R.layout.single_key_value_item, holder.keyValueContainer, false)
            val keyValueText: TextView = itemView.findViewById(R.id.keyValueText)
            val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

            keyValueText.text = "$key: $value"

            btnEdit.setOnClickListener { listener.onEditKeyValue(hashMapKey, key, value) }
            btnDelete.setOnClickListener { listener.onDeleteKeyValue(hashMapKey, key) }

            holder.keyValueContainer.addView(itemView)
        }

        holder.btnAdd.setOnClickListener { listener.onAddKeyValue(hashMapKey) }
        holder.btnDeleteHashMap.setOnClickListener { listener.onDeleteHashMap(hashMapKey) }
    }

    override fun getItemCount(): Int = hashMapList.size
}
