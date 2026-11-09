package com.javafinal.ui.notification;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.javafinal.R;
import com.javafinal.model.SosApiModels;
import com.javafinal.model.SosNotification;
import com.javafinal.model.User;
import com.javafinal.ui.ApiActivity;
import com.javafinal.ui.rescuer.SosDetailActivity;
import com.javafinal.ui.user.TrackSosActivity;
import com.javafinal.util.NavigationHelper;
import com.javafinal.viewmodel.NotificationViewModel;
import com.javafinal.viewmodel.RealtimeViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationActivity extends ApiActivity {

    private NotificationViewModel notifications;
    private RealtimeViewModel realtime;
    private NotificationAdapter adapter;
    private List<SosNotification> allNotifications = Collections.emptyList();
    private String currentFilter = "ALL";

    private RecyclerView rvNotifications;
    private View layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setupScreen(R.layout.activity_notification);
        if (!requireSession()) return;

        notifications = vm(NotificationViewModel.class);
        realtime = vm(RealtimeViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);

        adapter = new NotificationAdapter(Collections.emptyList(), new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onMarkRead(SosNotification notification) {
                markNotificationAsRead(notification);
            }

            @Override
            public void onOpenSos(SosNotification notification) {
                openSosFromNotification(notification);
            }

            @Override
            public void onItemClick(SosNotification notification) {
                if (notification.getSosId() != null && !notification.getSosId().trim().isEmpty()) {
                    openSosFromNotification(notification);
                } else if (!notification.isRead()) {
                    markNotificationAsRead(notification);
                }
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        ChipGroup filterGroup = findViewById(R.id.chipGroupFilter);
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            if (id == R.id.chipUnread) {
                currentFilter = "UNREAD";
            } else if (id == R.id.chipSos) {
                currentFilter = SosNotification.TYPE_SOS_STATUS;
            } else if (id == R.id.chipAlert) {
                currentFilter = SosNotification.TYPE_ALERT;
            } else if (id == R.id.chipSystem) {
                currentFilter = SosNotification.TYPE_SYSTEM;
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });

        bind("notifications", (SosApiModels.NotificationsResponse response) -> {
            clearStatus();
            allNotifications = response.getData() == null ? Collections.emptyList() : response.getData();
            applyFilter();
        }, error -> showApiError(error, this::refresh), busy -> {
            if (busy) showLoading("Đang tải thông báo…");
        });

        bind("mark_read", (SosApiModels.MarkNotificationReadResponse response) -> {
            clearStatus();
            applyFilter();
        }, error -> {
            showApiError(error, null);
            refresh();
        }, null);

        realtime.getMessages().observe(this, message -> {
            if (message != null && !"PONG".equals(message.getEvent()) && !"SUBSCRIBED".equals(message.getEvent())) {
                refresh();
            }
        });
        realtime.getErrors().observe(this, error -> {
            if (error != null && error.isUnauthorized()) showApiError(error, null);
        });

        refresh();
    }

    private void refresh() {
        execute("notifications", notifications::loadMyNotifications);
    }

    private void markNotificationAsRead(SosNotification notification) {
        if (notification == null || notification.getNotifyId() == null) return;
        notification.setRead(true);
        adapter.updateNotification(notification);
        applyFilter();
        execute("mark_read", () -> notifications.markAsRead(notification.getNotifyId()));
    }

    private void updateSingleNotification(SosNotification updated) {
        if (updated == null) return;
        for (int i = 0; i < allNotifications.size(); i++) {
            SosNotification item = allNotifications.get(i);
            if (item != null && updated.getNotifyId() != null && updated.getNotifyId().equals(item.getNotifyId())) {
                allNotifications.set(i, updated);
                break;
            }
        }
        applyFilter();
    }

    private void openSosFromNotification(SosNotification notification) {
        if (notification == null || notification.getSosId() == null || notification.getSosId().trim().isEmpty()) {
            return;
        }
        if (!notification.isRead()) {
            markNotificationAsRead(notification);
        }
        User user = factory.getSession().getUser();
        Class<?> target = user != null && user.isRescuer() ? SosDetailActivity.class : TrackSosActivity.class;
        openSos(target, notification.getSosId());
    }

    private void applyFilter() {
        List<SosNotification> filtered = new ArrayList<>();
        int unreadCount = 0;

        for (SosNotification item : allNotifications) {
            if (item == null) continue;
            if (!item.isRead()) unreadCount++;

            if ("UNREAD".equals(currentFilter)) {
                if (!item.isRead()) filtered.add(item);
            } else if (SosNotification.TYPE_SOS_STATUS.equalsIgnoreCase(currentFilter)) {
                if (SosNotification.TYPE_SOS_STATUS.equalsIgnoreCase(item.getType())) filtered.add(item);
            } else if (SosNotification.TYPE_ALERT.equalsIgnoreCase(currentFilter)) {
                if (SosNotification.TYPE_ALERT.equalsIgnoreCase(item.getType())) filtered.add(item);
            } else if (SosNotification.TYPE_SYSTEM.equalsIgnoreCase(currentFilter)) {
                if (SosNotification.TYPE_SYSTEM.equalsIgnoreCase(item.getType())) filtered.add(item);
            } else {
                filtered.add(item);
            }
        }

        adapter.submitList(filtered);

        boolean empty = filtered.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            if ("UNREAD".equals(currentFilter)) {
                tvEmptyTitle.setText("Không có thông báo chưa đọc");
                tvEmptySubtitle.setText("Tất cả thông báo đã được đọc.");
            } else if (SosNotification.TYPE_ALERT.equalsIgnoreCase(currentFilter)) {
                tvEmptyTitle.setText("Không có cảnh báo khẩn cấp");
                tvEmptySubtitle.setText("Chưa có tin cảnh báo khẩn cấp nào phát sinh.");
            } else if (SosNotification.TYPE_SOS_STATUS.equalsIgnoreCase(currentFilter)) {
                tvEmptyTitle.setText("Không có thông báo cứu hộ");
                tvEmptySubtitle.setText("Chưa có cập nhật mới về tiến trình cứu hộ.");
            } else {
                tvEmptyTitle.setText("Không có thông báo nào");
                tvEmptySubtitle.setText("Các cập nhật về cứu hộ và thông báo hệ thống sẽ được liệt kê tại đây.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notifications != null) {
            refresh();
        }
    }
}
