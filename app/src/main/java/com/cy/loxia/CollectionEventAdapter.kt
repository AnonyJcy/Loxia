package com.cy.loxia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 收藏事件列表适配器
 */
class CollectionEventAdapter(
    private val onItemClick: (CollectionEvent) -> Unit = {}
) : ListAdapter<CollectionEvent, CollectionEventAdapter.ViewHolder>(EventDiffCallback()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEventIcon: TextView = itemView.findViewById(R.id.tvEventIcon)
        private val tvEventDressName: TextView = itemView.findViewById(R.id.tvEventDressName)
        private val tvEventType: TextView = itemView.findViewById(R.id.tvEventType)
        private val tvEventAmount: TextView = itemView.findViewById(R.id.tvEventAmount)
        private val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        private val tvEventRemaining: TextView = itemView.findViewById(R.id.tvEventRemaining)

        fun bind(event: CollectionEvent) {
            tvEventIcon.text = event.eventType.icon
            tvEventDressName.text = event.dressName
            tvEventType.text = event.eventType.label

            // 金额
            if (event.amount > 0) {
                tvEventAmount.text = String.format("¥%.0f", event.amount)
                tvEventAmount.visibility = View.VISIBLE
            } else {
                tvEventAmount.visibility = View.GONE
            }

            // 日期
            try {
                val date = LocalDate.parse(event.eventDate)
                tvEventDate.text = date.format(dateFormatter)
            } catch (e: Exception) {
                tvEventDate.text = event.eventDate
            }

            // 剩余天数
            val remaining = event.getRemainingText()
            tvEventRemaining.text = remaining

            // 根据紧急程度设置颜色
            val days = event.daysFromToday()
            when {
                days < 0 -> {
                    tvEventRemaining.setTextColor(itemView.context.getColor(R.color.nav_unselected))
                }
                days <= 3 -> {
                    tvEventRemaining.setTextColor(itemView.context.getColor(R.color.danger_red))
                }
                days <= 7 -> {
                    tvEventRemaining.setTextColor(itemView.context.getColor(R.color.warning_orange))
                }
                else -> {
                    tvEventRemaining.setTextColor(itemView.context.getColor(R.color.text_secondary))
                }
            }

            // 点击事件
            itemView.setOnClickListener { onItemClick(event) }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<CollectionEvent>() {
        override fun areItemsTheSame(oldItem: CollectionEvent, newItem: CollectionEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CollectionEvent, newItem: CollectionEvent): Boolean {
            return oldItem == newItem
        }
    }
}
