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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WardrobeAdapter extends ListAdapter<Wardrobe, WardrobeAdapter.ViewHolder> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
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
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
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
            dateStr = Instant.ofEpochMilli(ts)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DATE_FORMAT);
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
        // Reset all to invisible
        holder.ivFull.setVisibility(View.INVISIBLE);
        holder.ivLeftHalf.setVisibility(View.INVISIBLE);
        holder.ivRightHalf.setVisibility(View.INVISIBLE);
        holder.ivTopLeftQ.setVisibility(View.INVISIBLE);
        holder.ivTopRightQ.setVisibility(View.INVISIBLE);
        holder.ivBotLeftQ.setVisibility(View.INVISIBLE);
        holder.ivBotRightQ.setVisibility(View.INVISIBLE);

        holder.ivPreviewDefault.setVisibility(View.VISIBLE);

        if (images == null || images.isEmpty()) return;

        holder.ivPreviewDefault.setVisibility(View.INVISIBLE);
        int count = Math.min(images.size(), 4);

        if (count == 1) {
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(0), holder.ivFull, R.drawable.bg_image_placeholder);
            holder.ivFull.setVisibility(View.VISIBLE);
        } else if (count == 2) {
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(0), holder.ivLeftHalf, R.drawable.bg_image_placeholder);
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(1), holder.ivRightHalf, R.drawable.bg_image_placeholder);
            holder.ivLeftHalf.setVisibility(View.VISIBLE);
            holder.ivRightHalf.setVisibility(View.VISIBLE);
        } else if (count == 3) {
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(0), holder.ivLeftHalf, R.drawable.bg_image_placeholder);
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(1), holder.ivTopRightQ, R.drawable.bg_image_placeholder);
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(2), holder.ivBotRightQ, R.drawable.bg_image_placeholder);
            holder.ivLeftHalf.setVisibility(View.VISIBLE);
            holder.ivTopRightQ.setVisibility(View.VISIBLE);
            holder.ivBotRightQ.setVisibility(View.VISIBLE);
        } else {
            // 4 images
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(0), holder.ivTopLeftQ, R.drawable.bg_image_placeholder);
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(1), holder.ivTopRightQ, R.drawable.bg_image_placeholder);
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(2), holder.ivBotLeftQ, R.drawable.bg_image_placeholder);
            ImageUtils.loadIntoView(holder.itemView.getContext(), images.get(3), holder.ivBotRightQ, R.drawable.bg_image_placeholder);
            holder.ivTopLeftQ.setVisibility(View.VISIBLE);
            holder.ivTopRightQ.setVisibility(View.VISIBLE);
            holder.ivBotLeftQ.setVisibility(View.VISIBLE);
            holder.ivBotRightQ.setVisibility(View.VISIBLE);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPreviewDefault;
        final ImageView ivFull, ivLeftHalf, ivRightHalf, ivTopLeftQ, ivTopRightQ, ivBotLeftQ, ivBotRightQ;
        final TextView tvName;
        final TextView tvCount;
        final TextView tvBadge;
        final TextView tvDemoBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPreviewDefault = itemView.findViewById(R.id.ivPreviewDefault);
            ivFull = itemView.findViewById(R.id.ivFull);
            ivLeftHalf = itemView.findViewById(R.id.ivLeftHalf);
            ivRightHalf = itemView.findViewById(R.id.ivRightHalf);
            ivTopLeftQ = itemView.findViewById(R.id.ivTopLeftQ);
            ivTopRightQ = itemView.findViewById(R.id.ivTopRightQ);
            ivBotLeftQ = itemView.findViewById(R.id.ivBotLeftQ);
            ivBotRightQ = itemView.findViewById(R.id.ivBotRightQ);

            tvName = itemView.findViewById(R.id.tvWardrobeName);
            tvCount = itemView.findViewById(R.id.tvWardrobeCount);
            tvBadge = itemView.findViewById(R.id.tvWardrobeBadge);
            tvDemoBadge = itemView.findViewById(R.id.tvDemoBadge);
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
