package com.example.eventpal_organization.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.model.EventInfoMainItemModel

class AdaptEvenInfoMainRecycler(
    private val itemList: List<EventInfoMainItemModel>,
    private val onItemClick: (String) -> Unit  // Click listener passing document ID
) : RecyclerView.Adapter<AdaptEvenInfoMainRecycler.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentTextView: TextView = itemView.findViewById(R.id.listItemName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item1, parent, false)
        return ItemViewHolder(view)
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemList[position]
        holder.contentTextView.text = item.id
        holder.itemView.setOnClickListener {
            onItemClick(item.id)  // Handle item click and pass the document ID
        }
    }

    override fun getItemCount(): Int = itemList.size
}
