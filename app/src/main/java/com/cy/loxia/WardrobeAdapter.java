package com.cy.loxia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Objects;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WardrobeAdapter extends ListAdapter<Wardrobe, WardrobeAdapter.ViewHolder> {
    private final OnWardrobeClickListener listener;
    private OnWardrobeLongPressListener longPressListener;
    private Map<String, List<String>> previewImages = new HashMap<>();

    public interface OnWardrobeClickListener {
        void onWardrobeClick(Wardrobe wardrobe);
    }

    public interface OnWardrobeLongPressListener {
        void onWardrobeLongPress(Wardrobe wardrobe, int position);
    }

    public WardrobeAdapter(OnWardrobeClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setLongPressListener(OnWardrobeLongPressListener longPressListener) {
        this.longPressListener = longPressListener;
    }

    public void setPreviewImages(Map<String, List<String>> previewImages) {
        this.previewImages = previewImages != null ? previewImages : new HashMap<>();
        // 只刷新可见 item，避免整个列表重绑
        int count = getItemCount();
        if (count > 0) {
            notifyItemRangeChanged(0, count, "preview");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wardrobe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Wardrobe wardrobe = getItem(position);
        holder.tvName.setText(wardrobe.getName());
        // 格式化 updatedAt 时间戳为可读日期
        String dateStr = "";
        long ts = wardrobe.getUpdatedAt();
        if (ts > 0) {
            dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(new java.util.Date(ts));
        }
        holder.tvCount.setText(String.format("%d 件 · 最近更新 %s", wardrobe.getCount(), dateStr.isEmpty() ? "-" : dateStr));
        holder.tvBadge.setText("查看详情");
        holder.tvDemoBadge.setVisibility(wardrobe.isDemo() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onWardrobeClick(wardrobe));
        holder.itemView.setOnLongClickListener(v -> {
            if (longPressListener != null) {
                longPressListener.onWardrobeLongPress(wardrobe, holder.getBindingAdapterPosition());
            }
            return true;
        });

        List<String> images = previewImages.get(wardrobe.getId());
        bindPreviewImages(holder, images);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            // 只更新预览图，不重绑其他视图
            String payload = (String) payloads.get(0);
            if ("preview".equals(payload)) {
                Wardrobe wardrobe = getItem(position);
                List<String> images = previewImages.get(wardrobe.getId());
                bindPreviewImages(holder, images);
            }
        }
    }

    private void bindPreviewImages(ViewHolder holder, List<String> images) {
        // Hide all preview images first
        for (ImageView iv : holder.previewImageViews) {
            iv.setVisibility(View.GONE);
        }
        holder.ivPreviewDefault.setVisibility(View.VISIBLE);

        if (images == null || images.isEmpty()) return;

        holder.ivPreviewDefault.setVisibility(View.GONE);
        int count = Math.min(images.size(), 4);

        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        int containerSize = (int) (84 * density); // matches item_wardrobe.xml previewContainer 84dp
        int gap = (int) (2 * density);
        int half = (containerSize - gap) / 2;

        if (count == 1) {
            layoutImage(holder.previewImageViews[0], 0, 0, containerSize, containerSize);
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(0), holder.previewImageViews[0], R.drawable.bg_image_placeholder);
            holder.previewImageViews[0].setVisibility(View.VISIBLE);
        } else if (count == 2) {
            layoutImage(holder.previewImageViews[0], 0, 0, half, containerSize);
            layoutImage(holder.previewImageViews[1], half + gap, 0, half, containerSize);
            for (int i = 0; i < 2; i++) {
                ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(i), holder.previewImageViews[i], R.drawable.bg_image_placeholder);
                holder.previewImageViews[i].setVisibility(View.VISIBLE);
            }
        } else if (count == 3) {
            layoutImage(holder.previewImageViews[0], 0, 0, half, containerSize);
            layoutImage(holder.previewImageViews[1], half + gap, 0, half, half);
            layoutImage(holder.previewImageViews[2], half + gap, half + gap, half, half);
            for (int i = 0; i < 3; i++) {
                ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(i), holder.previewImageViews[i], R.drawable.bg_image_placeholder);
                holder.previewImageViews[i].setVisibility(View.VISIBLE);
            }
        } else {
            // 4 images in 2x2 grid
            layoutImage(holder.previewImageViews[0], 0, 0, half, half);
            layoutImage(holder.previewImageViews[1], half + gap, 0, half, half);
            layoutImage(holder.previewImageViews[2], 0, half + gap, half, half);
            layoutImage(holder.previewImageViews[3], half + gap, half + gap, half, half);
            for (int i = 0; i < 4; i++) {
                ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(i), holder.previewImageViews[i], R.drawable.bg_image_placeholder);
                holder.previewImageViews[i].setVisibility(View.VISIBLE);
            }
        }
    }

    private void layoutImage(ImageView iv, int left, int top, int width, int height) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) iv.getLayoutParams();
        lp.leftMargin = left;
        lp.topMargin = top;
        lp.width = width;
        lp.height = height;
        iv.setLayoutParams(lp);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout previewContainer;
        final ImageView ivPreviewDefault;
        final ImageView[] previewImageViews = new ImageView[4];
        final TextView tvName;
        final TextView tvCount;
        final TextView tvBadge;
        final TextView tvDemoBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            previewContainer = itemView.findViewById(R.id.previewContainer);
            ivPreviewDefault = itemView.findViewById(R.id.ivPreviewDefault);
            tvName = itemView.findViewById(R.id.tvWardrobeName);
            tvCount = itemView.findViewById(R.id.tvWardrobeCount);
            tvBadge = itemView.findViewById(R.id.tvWardrobeBadge);
            tvDemoBadge = itemView.findViewById(R.id.tvDemoBadge);

            // Pre-create 4 ImageViews for preview grid
            for (int i = 0; i < 4; i++) {
                ImageView iv = new ImageView(itemView.getContext());
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackgroundResource(R.drawable.rounded_image_bg);
                iv.setClipToOutline(true);
                iv.setVisibility(View.GONE);
                previewContainer.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
                previewImageViews[i] = iv;
            }
        }
    }

    private static final DiffUtil.ItemCallback<Wardrobe> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<Wardrobe>() {
            @Override
            public boolean areItemsTheSame(@NonNull Wardrobe oldItem, @NonNull Wardrobe newItem) {
                return Objects.equals(oldItem.getId(), newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Wardrobe oldItem, @NonNull Wardrobe newItem) {
                return Objects.equals(oldItem.getName(), newItem.getName())
                    && oldItem.getCount() == newItem.getCount()
                    && Objects.equals(oldItem.getUpdatedAt(), newItem.getUpdatedAt())
                    && oldItem.isDemo() == newItem.isDemo()
                    && Objects.equals(oldItem.getCover(), newItem.getCover())
                    && oldItem.getSortOrder() == newItem.getSortOrder();
            }
        };
}
