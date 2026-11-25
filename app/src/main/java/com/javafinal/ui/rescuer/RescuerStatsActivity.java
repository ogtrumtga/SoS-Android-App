package com.javafinal.ui.rescuer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.javafinal.R;
import com.javafinal.model.SosApiModels;
import com.javafinal.model.SosApiModels.EmergencyTypeStat;
import com.javafinal.model.SosApiModels.RescuerStatsResponse;
import com.javafinal.model.SosApiModels.StatsSummary;
import com.javafinal.model.User;
import com.javafinal.ui.ApiActivity;
import com.javafinal.util.NavigationHelper;
import com.javafinal.viewmodel.RealtimeViewModel;
import com.javafinal.viewmodel.RescuerViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình thống kê cứu hộ cho RESCUER:
 * - Số vụ đã giải cứu thành công (COMPLETED), phân theo loại sự cố (tất cả, ngập lụt, ...).
 * - Lọc theo kỳ: Tất cả, Theo ngày, Theo tháng.
 * - Tổng quãng đường đã di chuyển tính từ location_history (actor='RESCUER').
 */
public class RescuerStatsActivity extends ApiActivity {
    private static final String PERIOD_ALL = "ALL";
    private static final String PERIOD_DAY = "DAY";
    private static final String PERIOD_MONTH = "MONTH";

    private RescuerViewModel rescuer;
    private RealtimeViewModel realtime;
    private RealtimeViewModel.ConnectionState realtimeState = RealtimeViewModel.ConnectionState.DISCONNECTED;

    private String period = PERIOD_ALL;
    private final Calendar selectedDay = Calendar.getInstance();
    private final Calendar selectedMonth = Calendar.getInstance();
    private String selectedTypeId;
    private RescuerStatsResponse latestStats;

    private boolean bindingPeriod;
    private boolean bindingTypes;
    private boolean loadingStats;
    private BottomSheetDialog monthPickerDialog;

    private final SimpleDateFormat apiDayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat apiMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
    private final SimpleDateFormat uiDayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setupScreen(R.layout.activity_rescuer_stats);
        if (!requireSession()) return;
        User user = factory.getSession().getUser();
        if (user == null) { goToLogin(); return; }
        if (!user.isRescuer()) { goToRoleHome(user); return; }

        if (state != null) {
            period = state.getString("period", PERIOD_ALL);
            long dayMillis = state.getLong("day_millis", 0L);
            if (dayMillis > 0L) selectedDay.setTimeInMillis(dayMillis);
            long monthMillis = state.getLong("month_millis", 0L);
            if (monthMillis > 0L) selectedMonth.setTimeInMillis(monthMillis);
            selectedTypeId = state.getString("type_id");
        }
        selectedMonth.set(Calendar.DAY_OF_MONTH, 1);

        rescuer = vm(RescuerViewModel.class);
        realtime = vm(RealtimeViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.statsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.getMenu().add(Menu.NONE, 1, Menu.NONE, "Tải lại")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        toolbar.setOnMenuItemClickListener(item -> {
            refresh();
            return true;
        });

        setupPeriodControls();
        setupTypeFilter();
        setupBottomNavigation();

        bind("stats", (RescuerStatsResponse response) -> {
            latestStats = response;
            clearStatus();
            renderTypeChips(response.getByEmergencyType());
            renderStats();
        }, error -> showApiError(error, this::refresh), busy -> {
            loadingStats = busy;
            if (busy) showLoading("Đang tải thống kê cứu hộ…");
        });

        realtime.getMessages().observe(this, message -> {
            if (message != null && !"PONG".equals(message.getEvent())
                    && !"SUBSCRIBED".equals(message.getEvent())
                    && !"AUTH_SUCCESS".equals(message.getEvent())) {
                refresh();
            }
        });
        realtime.getConnectionState().observe(this, value -> {
            boolean reconnecting = realtimeState == RealtimeViewModel.ConnectionState.RECONNECTING;
            realtimeState = value;
            if (value == RealtimeViewModel.ConnectionState.CONNECTED && reconnecting) refresh();
        });
        realtime.getErrors().observe(this, error -> {
            if (error != null && error.isUnauthorized()) showApiError(error, null);
        });

        updatePeriodNavigatorUi();
    }

    private void setupPeriodControls() {
        ChipGroup periodGroup = findViewById(R.id.chipGroupPeriod);
        bindingPeriod = true;
        try {
            if (PERIOD_DAY.equals(period)) periodGroup.check(R.id.chipPeriodDay);
            else if (PERIOD_MONTH.equals(period)) periodGroup.check(R.id.chipPeriodMonth);
            else periodGroup.check(R.id.chipPeriodAll);
        } finally {
            bindingPeriod = false;
        }

        periodGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (bindingPeriod || ids.isEmpty()) return;
            int id = ids.get(0);
            String nextPeriod = id == R.id.chipPeriodDay ? PERIOD_DAY
                    : id == R.id.chipPeriodMonth ? PERIOD_MONTH : PERIOD_ALL;
            if (!nextPeriod.equals(period)) {
                period = nextPeriod;
                updatePeriodNavigatorUi();
                refresh();
            }
        });

        findViewById(R.id.btnPeriodPrev).setOnClickListener(v -> shiftPeriod(-1));
        findViewById(R.id.btnPeriodNext).setOnClickListener(v -> shiftPeriod(1));
        findViewById(R.id.tvPeriodLabel).setOnClickListener(v -> {
            if (PERIOD_DAY.equals(period)) openDayPicker();
            else if (PERIOD_MONTH.equals(period)) openMonthPicker();
        });
    }

    private void setupTypeFilter() {
        ChipGroup typeGroup = findViewById(R.id.chipGroupStatType);
        typeGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (bindingTypes) return;
            View checked = ids.isEmpty() ? null : group.findViewById(ids.get(0));
            if (checked != null && checked.getTag() instanceof EmergencyTypeStat) {
                selectedTypeId = ((EmergencyTypeStat) checked.getTag()).getEmergencyTypeId();
            } else {
                selectedTypeId = null;
            }
            renderStats();
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        bottom.setSelectedItemId(R.id.nav_rescuer_stats);
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_rescuer_stats) {
                refresh();
                return true;
            }
            if (id == R.id.nav_rescuer_requests) {
                startActivity(new Intent(this, RescuerHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            } else if (id == R.id.nav_rescuer_map) {
                startActivity(new Intent(this, RescuerMapActivity.class));
                finish();
            } else if (id == R.id.nav_rescuer_missions) {
                startActivity(new Intent(this, RescuerMissionsActivity.class));
                finish();
            } else if (id == R.id.nav_rescuer_team) {
                startActivity(new Intent(this, RescueTeamActivity.class));
            }
            return false;
        });
    }

    private void shiftPeriod(int delta) {
        if (PERIOD_DAY.equals(period)) {
            selectedDay.add(Calendar.DAY_OF_MONTH, delta);
            Calendar today = Calendar.getInstance();
            if (isAfterDay(selectedDay, today)) selectedDay.setTimeInMillis(today.getTimeInMillis());
        } else if (PERIOD_MONTH.equals(period)) {
            selectedMonth.add(Calendar.MONTH, delta);
            selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
            Calendar currentMonth = currentMonthStart();
            if (isAfterMonth(selectedMonth, currentMonth)) selectedMonth.setTimeInMillis(currentMonth.getTimeInMillis());
        } else {
            return;
        }
        updatePeriodNavigatorUi();
        refresh();
    }

    private void openDayPicker() {
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDay.set(year, month, dayOfMonth);
                    updatePeriodNavigatorUi();
                    refresh();
                },
                selectedDay.get(Calendar.YEAR),
                selectedDay.get(Calendar.MONTH),
                selectedDay.get(Calendar.DAY_OF_MONTH)
        );
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void openMonthPicker() {
        dismissMonthPicker();
        monthPickerDialog = new BottomSheetDialog(this);
        LinearLayout root = NavigationHelper.createSheetRoot(
                this, null, "Chọn tháng thống kê", "Chọn năm và tháng để xem thống kê cứu hộ.");

        final int[] chosenYear = { selectedMonth.get(Calendar.YEAR) };
        final Calendar currentMonth = currentMonthStart();
        final int maxYear = currentMonth.get(Calendar.YEAR);
        final int maxMonthInCurrentYear = currentMonth.get(Calendar.MONTH);

        LinearLayout yearBar = new LinearLayout(this);
        yearBar.setOrientation(LinearLayout.HORIZONTAL);
        yearBar.setGravity(Gravity.CENTER_VERTICAL);
        yearBar.setBackgroundResource(R.drawable.bg_box);
        LinearLayout.LayoutParams yearBarLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        yearBarLp.topMargin = dp(14);
        yearBar.setLayoutParams(yearBarLp);

        MaterialButton btnPrevYear = new MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle);
        btnPrevYear.setIconResource(R.drawable.ic_chevron_left);
        btnPrevYear.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ink)));

        TextView tvYear = new TextView(this);
        tvYear.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvYear.setGravity(Gravity.CENTER);
        tvYear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvYear.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvYear.setTextColor(ContextCompat.getColor(this, R.color.ink));

        MaterialButton btnNextYear = new MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle);
        btnNextYear.setIconResource(R.drawable.ic_chevron_right);
        btnNextYear.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ink)));

        yearBar.addView(btnPrevYear);
        yearBar.addView(tvYear);
        yearBar.addView(btnNextYear);
        root.addView(yearBar);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        GridLayout.LayoutParams gridLp = new GridLayout.LayoutParams();
        gridLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        gridLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        gridLp.topMargin = dp(12);
        grid.setLayoutParams(gridLp);

        MaterialButton[] monthButtons = new MaterialButton[12];
        for (int m = 0; m < 12; m++) {
            final int monthIndex = m;
            MaterialButton btn = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText("Tháng " + (m + 1));
            btn.setAllCaps(false);
            btn.setCornerRadius(dp(12));
            btn.setInsetTop(0);
            btn.setInsetBottom(0);
            btn.setMinHeight(dp(44));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(m % 3, 1f);
            lp.rowSpec = GridLayout.spec(m / 3);
            int margin = dp(4);
            lp.setMargins(margin, margin, margin, margin);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> {
                selectedMonth.set(Calendar.YEAR, chosenYear[0]);
                selectedMonth.set(Calendar.MONTH, monthIndex);
                selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
                dismissMonthPicker();
                updatePeriodNavigatorUi();
                refresh();
            });
            monthButtons[m] = btn;
            grid.addView(btn);
        }
        root.addView(grid);

        MaterialButton close = NavigationHelper.createSheetButton(this, "Đóng", false, false);
        close.setOnClickListener(v -> dismissMonthPicker());
        root.addView(close);

        Runnable renderPicker = () -> {
            tvYear.setText("Năm " + chosenYear[0]);
            btnNextYear.setEnabled(chosenYear[0] < maxYear);
            btnNextYear.setAlpha(chosenYear[0] < maxYear ? 1f : 0.35f);
            int activeYear = selectedMonth.get(Calendar.YEAR);
            int activeMonth = selectedMonth.get(Calendar.MONTH);
            for (int m = 0; m < 12; m++) {
                MaterialButton btn = monthButtons[m];
                boolean disabled = chosenYear[0] > maxYear || (chosenYear[0] == maxYear && m > maxMonthInCurrentYear);
                boolean isSelected = chosenYear[0] == activeYear && m == activeMonth;
                btn.setEnabled(!disabled);
                btn.setAlpha(disabled ? 0.35f : 1f);
                if (isSelected) {
                    btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ink)));
                    btn.setTextColor(ContextCompat.getColor(this, R.color.white));
                    btn.setStrokeWidth(0);
                } else {
                    btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
                    btn.setTextColor(ContextCompat.getColor(this, R.color.ink));
                    btn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.line_strong)));
                    btn.setStrokeWidth(dp(1));
                }
            }
        };

        btnPrevYear.setOnClickListener(v -> {
            chosenYear[0]--;
            renderPicker.run();
        });
        btnNextYear.setOnClickListener(v -> {
            if (chosenYear[0] < maxYear) {
                chosenYear[0]++;
                renderPicker.run();
            }
        });
        renderPicker.run();

        monthPickerDialog.setContentView(root);
        monthPickerDialog.setOnDismissListener(d -> monthPickerDialog = null);
        monthPickerDialog.show();
    }

    private void updatePeriodNavigatorUi() {
        View nav = findViewById(R.id.layoutPeriodNavigator);
        TextView tvLabel = findViewById(R.id.tvPeriodLabel);
        View btnNext = findViewById(R.id.btnPeriodNext);

        if (PERIOD_ALL.equals(period)) {
            nav.setVisibility(View.GONE);
            return;
        }
        nav.setVisibility(View.VISIBLE);

        if (PERIOD_DAY.equals(period)) {
            Calendar today = Calendar.getInstance();
            boolean isToday = isSameDay(selectedDay, today);
            String formatted = uiDayFormat.format(selectedDay.getTime());
            tvLabel.setText(isToday ? "Hôm nay · " + formatted : formatted);
            btnNext.setEnabled(!isToday);
            btnNext.setAlpha(isToday ? 0.35f : 1f);
        } else {
            Calendar currentMonth = currentMonthStart();
            boolean isCurrentMonth = isSameMonth(selectedMonth, currentMonth);
            String formatted = String.format(Locale.getDefault(), "Tháng %02d/%d",
                    selectedMonth.get(Calendar.MONTH) + 1, selectedMonth.get(Calendar.YEAR));
            tvLabel.setText(isCurrentMonth ? "Tháng này · " + formatted : formatted);
            btnNext.setEnabled(!isCurrentMonth);
            btnNext.setAlpha(isCurrentMonth ? 0.35f : 1f);
        }
    }

    private void refresh() {
        String dayParam = PERIOD_DAY.equals(period) ? apiDayFormat.format(selectedDay.getTime()) : null;
        String monthParam = PERIOD_MONTH.equals(period) ? apiMonthFormat.format(selectedMonth.getTime()) : null;
        execute("stats", () -> rescuer.loadStats(period, dayParam, monthParam, null));
    }

    private void renderTypeChips(List<EmergencyTypeStat> items) {
        ChipGroup group = findViewById(R.id.chipGroupStatType);
        bindingTypes = true;
        try {
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                if (group.getChildAt(i).getId() != R.id.chipStatTypeAll) {
                    group.removeViewAt(i);
                }
            }
            int checkedChipId = R.id.chipStatTypeAll;
            if (items != null) {
                for (EmergencyTypeStat stat : items) {
                    if (stat == null) continue;
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filter, group, false);
                    chip.setId(View.generateViewId());
                    chip.setText(stat.getDisplayLabel());
                    chip.setTag(stat);
                    group.addView(chip);
                    if (selectedTypeId != null && selectedTypeId.equalsIgnoreCase(stat.getEmergencyTypeId())) {
                        checkedChipId = chip.getId();
                    }
                }
            }
            if (checkedChipId == R.id.chipStatTypeAll) {
                selectedTypeId = null;
            }
            group.check(checkedChipId);
        } finally {
            bindingTypes = false;
        }
    }

    private void renderStats() {
        TextView tvCount = findViewById(R.id.tvStatRescuedCount);
        TextView tvScope = findViewById(R.id.tvStatScopeLabel);
        TextView tvDistance = findViewById(R.id.tvStatDistanceKm);
        TextView tvTrackHint = findViewById(R.id.tvStatTrackHint);
        LinearLayout breakdownList = findViewById(R.id.layoutBreakdownList);
        View cardBreakdown = findViewById(R.id.cardBreakdown);
        TextView tvEmpty = findViewById(R.id.tvStatsEmpty);

        if (latestStats == null) {
            tvCount.setText("0");
            tvDistance.setText("0 km");
            tvScope.setText("Tất cả sự cố");
            tvTrackHint.setVisibility(View.GONE);
            cardBreakdown.setVisibility(View.GONE);
            tvEmpty.setVisibility(loadingStats ? View.GONE : View.VISIBLE);
            return;
        }

        List<EmergencyTypeStat> byType = latestStats.getByEmergencyType() == null
                ? Collections.emptyList() : latestStats.getByEmergencyType();
        StatsSummary overall = latestStats.getOverall();

        int displayCount = overall != null ? overall.getRescuedCount() : 0;
        double displayDistance = overall != null ? overall.getDistanceKm() : 0.0;
        int displayTracked = overall != null ? overall.getTrackedMissions() : 0;
        String scopeLabel = "Tất cả sự cố";

        if (selectedTypeId != null) {
            for (EmergencyTypeStat stat : byType) {
                if (stat != null && selectedTypeId.equalsIgnoreCase(stat.getEmergencyTypeId())) {
                    displayCount = stat.getRescuedCount();
                    displayDistance = stat.getDistanceKm();
                    displayTracked = stat.getTrackedMissions();
                    scopeLabel = stat.getDisplayLabel();
                    break;
                }
            }
        }

        tvCount.setText(String.valueOf(displayCount));
        tvScope.setText(scopeLabel);
        tvDistance.setText(formatDistance(displayDistance));

        if (displayCount > 0) {
            tvTrackHint.setVisibility(View.VISIBLE);
            tvTrackHint.setText(displayTracked > 0
                    ? String.format(Locale.getDefault(),
                    "Dữ liệu lộ trình GPS ghi nhận ở %d/%d ca hoàn thành.", displayTracked, displayCount)
                    : "Các ca hoàn thành trong kỳ chưa có dữ liệu lộ trình GPS liên tục.");
        } else {
            tvTrackHint.setVisibility(View.GONE);
        }

        int overallCount = overall != null ? overall.getRescuedCount() : 0;
        breakdownList.removeAllViews();

        if (byType.isEmpty() || overallCount == 0) {
            cardBreakdown.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        cardBreakdown.setVisibility(View.VISIBLE);

        for (int i = 0; i < byType.size(); i++) {
            EmergencyTypeStat stat = byType.get(i);
            if (stat == null) continue;
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.line));
                breakdownList.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }

            View row = getLayoutInflater().inflate(R.layout.item_rescuer_stat_type, breakdownList, false);
            TextView tvName = row.findViewById(R.id.tvTypeName);
            TextView tvMeta = row.findViewById(R.id.tvTypeMeta);
            ProgressBar progress = row.findViewById(R.id.progressTypeShare);

            boolean isSelected = selectedTypeId != null
                    && selectedTypeId.equalsIgnoreCase(stat.getEmergencyTypeId());
            int percent = overallCount > 0 ? Math.round((stat.getRescuedCount() * 100f) / overallCount) : 0;

            tvName.setText(stat.getDisplayLabel());
            tvName.setTextColor(ContextCompat.getColor(this,
                    isSelected ? R.color.red : (stat.getRescuedCount() > 0 ? R.color.ink : R.color.ink_tertiary)));

            if (stat.getRescuedCount() > 0) {
                tvMeta.setText(String.format(Locale.getDefault(),
                        "%d ca (%d%%) · %s", stat.getRescuedCount(), percent, formatDistance(stat.getDistanceKm())));
                tvMeta.setTextColor(ContextCompat.getColor(this, isSelected ? R.color.red : R.color.ink_secondary));
            } else {
                tvMeta.setText("0 ca");
                tvMeta.setTextColor(ContextCompat.getColor(this, R.color.ink_tertiary));
            }

            progress.setProgress(percent);
            progress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    this, isSelected ? R.color.red : R.color.ink)));

            final String clickedTypeId = stat.getEmergencyTypeId();
            row.setOnClickListener(v -> selectTypeChip(
                    selectedTypeId != null && selectedTypeId.equalsIgnoreCase(clickedTypeId) ? null : clickedTypeId));

            breakdownList.addView(row);
        }
    }

    private void selectTypeChip(String typeId) {
        selectedTypeId = typeId;
        ChipGroup group = findViewById(R.id.chipGroupStatType);
        bindingTypes = true;
        try {
            int targetChipId = R.id.chipStatTypeAll;
            if (typeId != null) {
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child.getTag() instanceof EmergencyTypeStat) {
                        EmergencyTypeStat stat = (EmergencyTypeStat) child.getTag();
                        if (typeId.equalsIgnoreCase(stat.getEmergencyTypeId())) {
                            targetChipId = child.getId();
                            break;
                        }
                    }
                }
            }
            group.check(targetChipId);
        } finally {
            bindingTypes = false;
        }
        renderStats();
    }

    private static String formatDistance(double distanceKm) {
        if (distanceKm <= 0.0) return "0 km";
        if (distanceKm < 1.0) {
            long meters = Math.round(distanceKm * 1000.0);
            return meters + " m";
        }
        return String.format(Locale.US, "%.1f km", distanceKm);
    }

    private static Calendar currentMonthStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal;
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isAfterDay(Calendar a, Calendar b) {
        if (a.get(Calendar.YEAR) != b.get(Calendar.YEAR)) {
            return a.get(Calendar.YEAR) > b.get(Calendar.YEAR);
        }
        return a.get(Calendar.DAY_OF_YEAR) > b.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isSameMonth(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH);
    }

    private static boolean isAfterMonth(Calendar a, Calendar b) {
        if (a.get(Calendar.YEAR) != b.get(Calendar.YEAR)) {
            return a.get(Calendar.YEAR) > b.get(Calendar.YEAR);
        }
        return a.get(Calendar.MONTH) > b.get(Calendar.MONTH);
    }

    private void dismissMonthPicker() {
        if (monthPickerDialog != null) monthPickerDialog.dismiss();
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("period", period);
        outState.putLong("day_millis", selectedDay.getTimeInMillis());
        outState.putLong("month_millis", selectedMonth.getTimeInMillis());
        outState.putString("type_id", selectedTypeId);
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
        dismissMonthPicker();
        super.onDestroy();
    }
}
