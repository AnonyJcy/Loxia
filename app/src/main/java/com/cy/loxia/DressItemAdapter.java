package com.cy.loxia;

import android.view.HapticFeedbackConstants;
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

public class DressItemAdapter extends ListAdapter<DressItem, DressItemAdapter.ViewHolder> {
    private OnDressItemClickListener clickListener;
    private OnDressItemLongPressListener longPressListener;

    public interface OnDressItemClickListener {
        void onDressItemClick(DressItem item);
    }

    public interface OnDressItemLongPressListener {
        void onDressItemLongPress(DressItem item, int position);
    }

    public DressItemAdapter() {
        super(DIFF_CALLBACK);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    public void setOnDressItemClickListener(OnDressItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnDressItemLongPressListener(OnDressItemLongPressListener listener) {
        this.longPressListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dress_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DressItem item = getItem(position);
        holder.tvName.setText(item.getName());
        holder.tvDetail.setText(String.format("¥%.2f · %s · %s", item.getEffectiveTotal(), item.getBuyDate(), item.getStore()));

        ImageUtils.loadIntoView(holder.itemView.getContext(), item.getImageUri(), holder.ivImage, R.drawable.bg_image_placeholder);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDressItemClick(item);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longPressListener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                longPressListener.onDressItemLongPress(item, pos);
            }
            return true;
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvName;
        final TextView tvDetail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivDressImage);
            tvName = itemView.findViewById(R.id.tvDressName);
            tvDetail = itemView.findViewById(R.id.tvDressDetail);
        }
    }

    private static boolean priceEquals(double a, double b) {
        return Double.compare(a, b) == 0;
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
                    && priceEquals(oldItem.getPrice(), newItem.getPrice())
                    && Objects.equals(oldItem.getBuyDate(), newItem.getBuyDate())
                    && Objects.equals(oldItem.getStore(), newItem.getStore())
                    && oldItem.isPinned() == newItem.isPinned()
                    && oldItem.getSortOrder() == newItem.getSortOrder()
                    && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                    && Objects.equals(oldItem.getImageUri(), newItem.getImageUri())
                    && Objects.equals(oldItem.getChannel(), newItem.getChannel())
                    && priceEquals(oldItem.getEarnestMoney(), newItem.getEarnestMoney())
                    && oldItem.isFullPayment() == newItem.isFullPayment()
                    && priceEquals(oldItem.getFullPaymentAmount(), newItem.getFullPaymentAmount())
                    && priceEquals(oldItem.getTailPayment(), newItem.getTailPayment())
                    && Objects.equals(oldItem.getShippingFee(), newItem.getShippingFee())
                    && priceEquals(oldItem.getDeposit(), newItem.getDeposit());
            }
        };
}
