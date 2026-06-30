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

public class OverviewDressAdapter extends ListAdapter<DressItem, OverviewDressAdapter.ViewHolder> {
    private OnDressItemClickListener clickListener;

    public interface OnDressItemClickListener {
        void onDressItemClick(DressItem item);
    }

    public OverviewDressAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnDressItemClickListener(OnDressItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_overview_dress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DressItem item = getItem(position);
        holder.tvName.setText(item.getName());
        holder.tvStatus.setText(item.getStatus().isEmpty() ? "无状态" : item.getStatus());
        holder.tvPinned.setVisibility(item.isPinned() ? View.VISIBLE : View.GONE);
        holder.tvPrice.setText(String.format("¥%.2f", item.getEffectiveTotal()));

        ImageUtils.loadIntoView(holder.itemView.getContext(), item.getImageUri(), holder.ivImage, R.drawable.bg_image_placeholder);
        holder.ivImage.setVisibility(View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDressItemClick(item);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvName;
        final TextView tvStatus;
        final TextView tvPrice;
        final TextView tvPinned;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivOverviewImage);
            tvName = itemView.findViewById(R.id.tvOverviewName);
            tvStatus = itemView.findViewById(R.id.tvOverviewStatus);
            tvPrice = itemView.findViewById(R.id.tvOverviewPrice);
            tvPinned = itemView.findViewById(R.id.tvOverviewPinned);
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
                    && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                    && Double.compare(oldItem.getEffectiveTotal(), newItem.getEffectiveTotal()) == 0
                    && Objects.equals(oldItem.getImageUri(), newItem.getImageUri())
                    && oldItem.isPinned() == newItem.isPinned();
            }
        };
}
