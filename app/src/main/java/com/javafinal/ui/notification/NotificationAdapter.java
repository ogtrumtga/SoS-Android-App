package com.javafinal.ui.notification;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.javafinal.R;
import com.javafinal.model.SosNotification;
import com.javafinal.util.ApiDisplay;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationActionListener {
        void onMarkRead(SosNotification notification);
        void onOpenSos(SosNotification notification);
        void onItemClick(SosNotification notification);
    }

    private final List<SosNotification> items = new ArrayList<>();
    private final OnNotificationActionListener listener;

    public NotificationAdapter(List<SosNotification> initialItems, OnNotificationActionListener listener) {
        if (initialItems != null) {
            items.addAll(initialItems);
        }
        this.listener = listener;
    }

    public void submitList(List<SosNotification> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void updateNotification(SosNotification updated) {
        if (updated == null || updated.getNotifyId() == null) return;
        for (int i = 0; i < items.size(); i++) {
            SosNotification item = items.get(i);
            if (item != null && updated.getNotifyId().equals(item.getNotifyId())) {
                items.set(i, updated);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        SosNotification item = items.get(position);
        Context context = holder.itemView.getContext();

        // 1. Loại thông báo
        String type = item.getType();
        String typeLabel;
        int typeBg = R.color.fill;
        int typeTextColor = R.color.ink_secondary;

        if (SosNotification.TYPE_ALERT.equalsIgnoreCase(type)) {
            typeLabel = "Khẩn cấp";
            typeBg = R.color.red_soft;
            typeTextColor = R.color.red;
        } else if (SosNotification.TYPE_SOS_STATUS.equalsIgnoreCase(type)) {
            typeLabel = "Cứu hộ";
            typeBg = R.color.amber_soft;
            typeTextColor = R.color.amber;
        } else if (SosNotification.TYPE_SYSTEM.equalsIgnoreCase(type)) {
            typeLabel = "Hệ thống";
            typeBg = R.color.fill;
            typeTextColor = R.color.ink_secondary;
        } else {
            typeLabel = type != null && !type.isEmpty() ? type : "Thông báo";
        }

        holder.tvNotificationType.setText(typeLabel);
        holder.tvNotificationType.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, typeBg)));
        holder.tvNotificationType.setTextColor(ContextCompat.getColor(context, typeTextColor));

        // 2. Thời gian
        holder.tvNotificationTime.setText(ApiDisplay.time(item.getCreatedAt()));

        // 3. Nội dung
        holder.tvNotificationMessage.setText(item.getMessage() != null ? item.getMessage() : "—");

        // 4. Trạng thái đã đọc / chưa đọc
        boolean isUnread = !item.isRead();
        if (isUnread) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvNotificationMessage.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            holder.cardNotification.setStrokeColor(ContextCompat.getColor(context, R.color.line_strong));
            holder.btnMarkRead.setVisibility(View.VISIBLE);
            holder.btnMarkRead.setEnabled(true);
            holder.btnMarkRead.setText("Đã đọc");
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
            holder.tvNotificationMessage.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            holder.cardNotification.setStrokeColor(ContextCompat.getColor(context, R.color.line));
            holder.btnMarkRead.setVisibility(View.GONE);
        }

        // 5. Nút mở chi tiết cứu hộ SOS
        boolean hasSos = item.getSosId() != null && !item.getSosId().trim().isEmpty();
        if (hasSos) {
            holder.btnOpenSos.setVisibility(View.VISIBLE);
            holder.btnOpenSos.setOnClickListener(v -> {
                if (listener != null) listener.onOpenSos(item);
            });
        } else {
            holder.btnOpenSos.setVisibility(View.GONE);
        }

        // Hiện hoặc ẩn hàng nút nếu không có nút nào
        holder.layoutActions.setVisibility(isUnread || hasSos ? View.VISIBLE : View.GONE);

        // Click handlers
        holder.btnMarkRead.setOnClickListener(v -> {
            holder.btnMarkRead.setEnabled(false);
            if (listener != null) listener.onMarkRead(item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardNotification;
        final TextView tvNotificationType;
        final TextView tvUnreadBadge;
        final TextView tvNotificationTime;
        final TextView tvNotificationMessage;
        final View layoutActions;
        final MaterialButton btnMarkRead;
        final MaterialButton btnOpenSos;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            tvNotificationType = itemView.findViewById(R.id.tvNotificationType);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnMarkRead = itemView.findViewById(R.id.btnMarkRead);
            btnOpenSos = itemView.findViewById(R.id.btnOpenSos);
        }
    }
}
