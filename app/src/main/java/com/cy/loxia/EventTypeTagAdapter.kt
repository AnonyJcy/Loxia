package com.cy.loxia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * 事件类型标签适配器
 */
class EventTypeTagAdapter(
    private val onItemClick: (EventType) -> Unit = {}
) : ListAdapter<EventTypeTagAdapter.EventTypeData, EventTypeTagAdapter.ViewHolder>(EventTypeDiffCallback()) {

    data class EventTypeData(
        val type: EventType,
        val count: Int
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_type_tag, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        holder.bind(data)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEventTypeName: TextView = itemView.findViewById(R.id.tvEventTypeName)
        private val tvEventTypeCount: TextView = itemView.findViewById(R.id.tvEventTypeCount)

        fun bind(data: EventTypeData) {
            tvEventTypeName.text = "${data.type.icon} ${data.type.label}"
            tvEventTypeCount.text = data.count.toString()

            itemView.setOnClickListener { onItemClick(data.type) }
        }
    }

    private class EventTypeDiffCallback : DiffUtil.ItemCallback<EventTypeData>() {
        override fun areItemsTheSame(oldItem: EventTypeData, newItem: EventTypeData): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: EventTypeData, newItem: EventTypeData): Boolean {
            return oldItem == newItem
        }
    }
}
