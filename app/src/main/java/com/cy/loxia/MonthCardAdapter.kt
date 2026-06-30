package com.cy.loxia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/**
 * 月份卡片适配器（横向滚动）
 */
class MonthCardAdapter(
    private val onItemClick: (Int) -> Unit = {}
) : ListAdapter<MonthCardAdapter.MonthCardData, MonthCardAdapter.ViewHolder>(MonthDiffCallback()) {

    data class MonthCardData(
        val month: Int,
        val eventCount: Int,
        val pendingAmount: Double,
        val isCurrentMonth: Boolean = false
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        holder.bind(data)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val tvMonth: TextView = itemView.findViewById(R.id.tvMonth)
        private val tvEventCount: TextView = itemView.findViewById(R.id.tvEventCount)
        private val tvPendingAmount: TextView = itemView.findViewById(R.id.tvPendingAmount)

        fun bind(data: MonthCardData) {
            tvMonth.text = "${data.month}月"

            // 当前月份高亮
            if (data.isCurrentMonth) {
                card.setCardBackgroundColor(itemView.context.getColor(R.color.month_current_bg))
                card.strokeColor = itemView.context.getColor(R.color.pink_primary)
                tvMonth.setTextColor(itemView.context.getColor(R.color.pink_primary))
            } else {
                card.setCardBackgroundColor(itemView.context.getColor(R.color.card_bg_white))
                card.strokeColor = itemView.context.getColor(R.color.card_stroke_pink)
                tvMonth.setTextColor(itemView.context.getColor(R.color.text_primary))
            }

            // 有事件时显示数量和金额
            if (data.eventCount > 0) {
                tvEventCount.text = "${data.eventCount}个节点"
                tvEventCount.visibility = View.VISIBLE

                if (data.pendingAmount > 0) {
                    tvPendingAmount.text = String.format("¥%.0f", data.pendingAmount)
                    tvPendingAmount.visibility = View.VISIBLE
                } else {
                    tvPendingAmount.visibility = View.GONE
                }
            } else {
                tvEventCount.visibility = View.GONE
                tvPendingAmount.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(data.month) }
        }
    }

    private class MonthDiffCallback : DiffUtil.ItemCallback<MonthCardData>() {
        override fun areItemsTheSame(oldItem: MonthCardData, newItem: MonthCardData): Boolean {
            return oldItem.month == newItem.month
        }

        override fun areContentsTheSame(oldItem: MonthCardData, newItem: MonthCardData): Boolean {
            return oldItem == newItem
        }
    }
}
