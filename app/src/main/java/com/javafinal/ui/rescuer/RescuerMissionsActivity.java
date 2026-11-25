package com.javafinal.ui.rescuer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.javafinal.R;
import com.javafinal.model.SosApiModels;
import com.javafinal.model.SosLog;
import com.javafinal.model.SosRequest;
import com.javafinal.model.User;
import com.javafinal.ui.ApiActivity;
import com.javafinal.util.ApiDisplay;
import com.javafinal.util.NavigationHelper;
import com.javafinal.viewmodel.EmergencyTypeViewModel;
import com.javafinal.viewmodel.RealtimeViewModel;
import com.javafinal.viewmodel.RescuerViewModel;
import com.javafinal.viewmodel.SosViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Màn hình danh sách tất cả các ca cứu hộ của rescuer (ca hiện tại nổi bật + lịch sử kèm nhật ký). */
public class RescuerMissionsActivity extends ApiActivity {
    private RescuerViewModel rescuer;
    private SosViewModel sos;
    private EmergencyTypeViewModel types;
    private RealtimeViewModel realtime;
    private RealtimeViewModel.ConnectionState realtimeState = RealtimeViewModel.ConnectionState.DISCONNECTED;
    private RescuerMissionAdapter adapter;
    private List<SosRequest> missions = Collections.emptyList();
    private SosRequest activeMission;
    private boolean loadingMissions;
    private String dateFrom;
    private String dateTo;
    private MaterialButton dateFromButton;
    private MaterialButton dateToButton;
    private MaterialButton allDatesButton;
    private TextView statisticsText;
    private BottomSheetDialog logsDialog;
    private LinearLayout logsBody;
    private String selectedLogSosId;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setupScreen(R.layout.activity_rescuer_missions);
        if (!requireSession()) return;
        User user = factory.getSession().getUser();
        if (user == null) { goToLogin(); return; }
        if (!user.isRescuer()) { goToRoleHome(user); return; }

        rescuer = vm(RescuerViewModel.class);
        sos = vm(SosViewModel.class);
        types = vm(EmergencyTypeViewModel.class);
        realtime = vm(RealtimeViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.missionsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.getMenu().add(Menu.NONE, 1, Menu.NONE, "Tải lại")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        toolbar.setOnMenuItemClickListener(item -> { refresh(); return true; });

        if (state != null) {
            dateFrom = state.getString("mission_date_from");
            dateTo = state.getString("mission_date_to");
        }
        dateFromButton = findViewById(R.id.btnMissionDateFrom);
        dateToButton = findViewById(R.id.btnMissionDateTo);
        allDatesButton = findViewById(R.id.btnMissionAllDates);
        statisticsText = findViewById(R.id.tvMissionStatistics);
        updateDateButtons();
        dateFromButton.setOnClickListener(v -> pickDate(true));
        dateToButton.setOnClickListener(v -> pickDate(false));
        allDatesButton.setOnClickListener(v -> {
            dateFrom = null;
            dateTo = null;
            updateDateButtons();
            loadMissions();
        });

        adapter = new RescuerMissionAdapter((mission, isCurrent) -> {
            if (mission == null || mission.getId() == null) return;
            if (isCurrent) {
                openSos(RealtimeTrackingActivity.class, mission.getId());
            } else {
                showMissionLogsSheet(mission);
            }
        });
        RecyclerView rv = findViewById(R.id.rvMissions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        bind("types", (SosApiModels.EmergencyTypeListResponse response) -> render(), error -> { }, null);
        bind("active", (SosApiModels.ActiveSosResponse response) -> {
            activeMission = response.isActive() ? response.getSos() : null;
            render();
        }, error -> { }, null);
        bind("missions", (SosApiModels.RescuerMissionsResponse response) -> {
            missions = response.getData() == null ? Collections.emptyList() : response.getData();
            SosApiModels.MissionStatistics stats = response.getStatistics();
            statisticsText.setText(stats == null ? getString(R.string.mission_statistics_error)
                    : getString(R.string.mission_statistics_summary, stats.getCompletedCount(),
                            String.format(Locale.getDefault(), "%.2f", stats.getTotalDistanceKm())));
            clearStatus();
            render();
        }, error -> {
            statisticsText.setText(R.string.mission_statistics_error);
            showApiError(error, this::refresh);
        }, busy -> {
            loadingMissions = busy;
            dateFromButton.setEnabled(!busy);
            dateToButton.setEnabled(!busy);
            allDatesButton.setEnabled(!busy);
            if (busy) {
                showLoading("Đang tải các ca của bạn…");
                statisticsText.setText(R.string.mission_statistics_loading);
            }
            render();
        });
        bind("logs", (SosApiModels.SosLogsResponse response) -> {
            renderLogsInSheet(response.getLogs());
        }, error -> {
            if (logsBody != null && logsDialog != null && logsDialog.isShowing()) {
                logsBody.removeAllViews();
                addLogEmptyText(logsBody, error.getMessage());
                if (selectedLogSosId != null) {
                    MaterialButton retry = NavigationHelper.createSheetButton(this, "Thử lại", false, false);
                    retry.setOnClickListener(v -> loadLogsForSelected());
                    logsBody.addView(retry);
                }
            }
        }, null);

        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        bottom.setSelectedItemId(R.id.nav_rescuer_missions);
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_rescuer_missions) { refresh(); return true; }
            if (id == R.id.nav_rescuer_requests) {
                startActivity(new Intent(this, RescuerHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            } else if (id == R.id.nav_rescuer_map) {
                startActivity(new Intent(this, RescuerMapActivity.class));
                finish();
            } else if (id == R.id.nav_rescuer_team) {
                startActivity(new Intent(this, RescueTeamActivity.class));
            }
            return false;
        });

        realtime.getMessages().observe(this, message -> {
            if (message != null && !"PONG".equals(message.getEvent())
                    && !"SUBSCRIBED".equals(message.getEvent())
                    && !"AUTH_SUCCESS".equals(message.getEvent())) refresh();
        });
        realtime.getConnectionState().observe(this, value -> {
            boolean reconnecting = realtimeState == RealtimeViewModel.ConnectionState.RECONNECTING;
            realtimeState = value;
            if (value == RealtimeViewModel.ConnectionState.CONNECTED && reconnecting) refresh();
        });
        realtime.getErrors().observe(this, error -> {
            if (error != null && error.isUnauthorized()) showApiError(error, null);
        });
    }

    private void refresh() {
        execute("types", types::loadAll);
        execute("active", rescuer::loadMyActive);
        loadMissions();
    }

    private void loadMissions() {
        execute("missions", () -> rescuer.loadMyMissions(dateFrom, dateTo));
    }

    private void pickDate(boolean from) {
        Calendar selected = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String value = from ? dateFrom : dateTo;
        if (value != null) {
            String[] parts = value.split("-");
            selected.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]));
        }
        new DatePickerDialog(this, (view, year, month, day) -> {
            String picked = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            String start = from ? picked : dateFrom;
            String end = from ? dateTo : picked;
            if (start != null && end != null && start.compareTo(end) > 0) {
                Toast.makeText(this, R.string.mission_date_range_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            dateFrom = start;
            dateTo = end;
            updateDateButtons();
            loadMissions();
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButtons() {
        dateFromButton.setText(dateButtonText(R.string.mission_date_from, dateFrom));
        dateToButton.setText(dateButtonText(R.string.mission_date_to, dateTo));
    }

    private String dateButtonText(int label, String date) {
        return date == null ? getString(label) : getString(label) + "\n"
                + date.substring(8, 10) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4);
    }

    private void render() {
        if (adapter == null) return;
        List<SosRequest> combined = new ArrayList<>();
        String activeId = activeMission != null ? activeMission.getId() : null;
        if (activeMission != null) {
            combined.add(activeMission);
        }
        for (SosRequest item : missions) {
            if (item == null) continue;
            if (activeId != null && activeId.equals(item.getId())) continue;
            combined.add(item);
        }
        adapter.submitList(combined, activeId);
        TextView empty = findViewById(R.id.tvMissionsEmpty);
        empty.setText(dateFrom != null || dateTo != null
                ? getString(R.string.mission_date_range_empty) : "Chưa có ca cứu hộ nào.");
        empty.setVisibility(
                !loadingMissions && combined.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showMissionLogsSheet(SosRequest mission) {
        dismissLogsSheet();
        selectedLogSosId = mission.getId();
        logsDialog = new BottomSheetDialog(this);
        String badge = mission.getShortId() + " · " + mission.getStatusLabel();
        String title = mission.getEmergencyTypeLabel();
        String sub = text(mission.getRequesterName()) + " • " + text(mission.getRequesterPhone());
        LinearLayout root = NavigationHelper.createSheetRoot(this, badge, title, sub);

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (getResources().getDisplayMetrics().heightPixels * 0.42f));
        scrollLp.topMargin = dp(12);
        scroll.setLayoutParams(scrollLp);

        logsBody = new LinearLayout(this);
        logsBody.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(logsBody);
        root.addView(scroll);

        MaterialButton close = NavigationHelper.createSheetButton(this, "Đóng", true, false);
        close.setOnClickListener(v -> dismissLogsSheet());
        root.addView(close);

        logsDialog.setContentView(root);
        logsDialog.setOnDismissListener(d -> {
            logsDialog = null;
            logsBody = null;
            selectedLogSosId = null;
        });
        logsDialog.show();
        loadLogsForSelected();
    }

    private void loadLogsForSelected() {
        if (selectedLogSosId == null || logsBody == null) return;
        logsBody.removeAllViews();
        addLogEmptyText(logsBody, "Đang tải nhật ký…");
        final String id = selectedLogSosId;
        execute("logs", () -> sos.loadLogs(id));
    }

    private void renderLogsInSheet(List<SosLog> logs) {
        if (logsBody == null || logsDialog == null || !logsDialog.isShowing()) return;
        logsBody.removeAllViews();
        if (logs == null || logs.isEmpty()) {
            addLogEmptyText(logsBody, "Chưa có nhật ký.");
            return;
        }
        for (int i = 0; i < logs.size(); i++) {
            SosLog entry = logs.get(i);
            if (entry == null) continue;
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.line));
                logsBody.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, dp(10), 0, dp(10));

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvAction = new TextView(this);
            tvAction.setText(ApiDisplay.action(entry.getAction()));
            tvAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvAction.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tvAction.setTextColor(ContextCompat.getColor(this, R.color.ink));
            header.addView(tvAction, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvTime = new TextView(this);
            tvTime.setText(ApiDisplay.shortTime(entry.getCreatedAt()));
            tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvTime.setTextColor(ContextCompat.getColor(this, R.color.ink_tertiary));
            header.addView(tvTime);

            item.addView(header);

            String desc = entry.getDescription();
            if (desc != null && !desc.trim().isEmpty()) {
                TextView tvDesc = new TextView(this);
                tvDesc.setText(desc.trim());
                tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tvDesc.setTextColor(ContextCompat.getColor(this, R.color.ink_secondary));
                LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                descLp.topMargin = dp(2);
                tvDesc.setLayoutParams(descLp);
                item.addView(tvDesc);
            }

            logsBody.addView(item);
        }
    }

    private void addLogEmptyText(LinearLayout parent, String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(ContextCompat.getColor(this, R.color.ink_secondary));
        tv.setPadding(0, dp(12), 0, dp(12));
        parent.addView(tv);
    }

    private void dismissLogsSheet() {
        if (logsDialog != null) logsDialog.dismiss();
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("mission_date_from", dateFrom);
        outState.putString("mission_date_to", dateTo);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (rescuer != null) {
            refresh();
            realtime.connect();
        }
    }

    @Override
    protected void onStop() {
        if (realtime != null) realtime.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        dismissLogsSheet();
        super.onDestroy();
    }
}
