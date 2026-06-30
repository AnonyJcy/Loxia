package com.cy.loxia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

public class StatsCardAdapter extends ListAdapter<DressItem, StatsCardAdapter.ViewHolder> {

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DressItem item);
    }

    public StatsCardAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stats_dress_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DressItem item = getItem(position);
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText(String.format("¥%.2f", item.getEffectiveTotal()));
        String status = item.getStatus().isEmpty() ? "无状态" : item.getStatus();
        holder.tvStatus.setText(status);
        ImageUtils.loadIntoView(holder.itemView.getContext(), item.getImageUri(), holder.ivImage, R.drawable.bg_image_placeholder);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvName, tvPrice, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivStatsCardImage);
            tvName = itemView.findViewById(R.id.tvStatsCardName);
            tvPrice = itemView.findViewById(R.id.tvStatsCardPrice);
            tvStatus = itemView.findViewById(R.id.tvStatsCardStatus);
        }
    }

    private static final DiffUtil.ItemCallback<DressItem> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<DressItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull DressItem oldItem, @NonNull DressItem newItem) {
                return Objects.equals(oldItem.getId(), newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull DressItem oldItem, @NonNull DressItem newItem) {
                return Objects.equals(oldItem.getName(), newItem.getName())
                    && Double.compare(oldItem.getEffectiveTotal(), newItem.getEffectiveTotal()) == 0
                    && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                    && Objects.equals(oldItem.getImageUri(), newItem.getImageUri());
            }
        };
}
