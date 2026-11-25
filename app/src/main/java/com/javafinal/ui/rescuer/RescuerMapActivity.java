package com.javafinal.ui.rescuer;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.javafinal.R;
import com.javafinal.model.EmergencyType;
import com.javafinal.model.SosRequest;
import com.javafinal.model.SosApiModels;
import com.javafinal.model.User;
import com.javafinal.network.ApiError;
import com.javafinal.ui.MapApiActivity;
import com.javafinal.util.DeviceLocation;
import com.javafinal.util.MapHelper;
import com.javafinal.util.NavigationHelper;
import com.javafinal.util.SosMapData;
import com.javafinal.viewmodel.EmergencyTypeViewModel;
import com.javafinal.viewmodel.RealtimeViewModel;
import com.javafinal.viewmodel.RescuerViewModel;
import java.util.Collections;
import java.util.List;

/** Bản đồ tổng quan PUBLIC và PRIVATE với tọa độ chính xác dành cho RESCUER. */
public class RescuerMapActivity extends MapApiActivity {
    private RescuerViewModel rescuer;
    private EmergencyTypeViewModel types;
    private RealtimeViewModel realtime;
    private RealtimeViewModel.ConnectionState realtimeState = RealtimeViewModel.ConnectionState.DISCONNECTED;
    private DeviceLocation gps;
    private List<SosRequest> points = Collections.emptyList();
    private String filter = "ALL", filterId;
    private boolean mapLoading, centered, bindingFilters;
    private ApiError mapError;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setupMapScreen(R.layout.activity_rescuer_map);
        if (!requireSession()) return;
        User user = factory.getSession().getUser();
        if (user == null) { goToLogin(); return; }
        if (!user.isRescuer()) { goToRoleHome(user); return; }
        if (state != null) {
            filter = state.getString("filter", "ALL");
            filterId = state.getString("filter_id");
        }
        rescuer = vm(RescuerViewModel.class);
        types = vm(EmergencyTypeViewModel.class);
        realtime = vm(RealtimeViewModel.class);
        gps = new DeviceLocation(this, location -> renderMap(), message -> NavigationHelper.showSnack(this, message));

        MaterialToolbar toolbar = findViewById(R.id.rescuerMapToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.getMenu().add(Menu.NONE, 1, Menu.NONE, "Tải lại").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        toolbar.setOnMenuItemClickListener(item -> { refresh(); return true; });
        bind("types", (SosApiModels.EmergencyTypeListResponse response) -> {
            renderTypeChips(response.getData());
            renderMap();
            updateStatus();
        }, error -> { }, null);
        bind("map", (SosApiModels.RescuerMapResponse response) -> {
            points = response.getData() == null ? Collections.emptyList() : response.getData();
            mapError = null;
            renderMap();
            updateStatus();
        }, error -> {
            mapError = error;
            if (error.isUnauthorized() || error.getStatusCode() == 403) {
                points = Collections.emptyList();
                renderMap();
            }
            updateStatus();
        }, busy -> {
            mapLoading = busy;
            if (busy) updateStatus();
        });
        // Chỉ đọc ca hiện tại khi chọn đúng mục này, không dùng dữ liệu ca để vẽ bản đồ tổng quan.
        bind("mission", (SosApiModels.ActiveSosResponse response) -> {
            clearStatus();
            if (response.isActive() && response.getSos() != null) {
                openSos(RealtimeTrackingActivity.class, response.getSos().getId());
            } else NavigationHelper.showSnack(this, "Đội chưa có ca cứu hộ đang xử lý.");
        }, error -> showApiError(error, this::openCurrentMission), null);

        ((ChipGroup) findViewById(R.id.chipGroupFilter)).setOnCheckedStateChangeListener((group, ids) -> {
            if (bindingFilters) return;
            View checked = ids.isEmpty() ? null : group.findViewById(ids.get(0));
            int checkedId = checked == null ? 0 : checked.getId();
            EmergencyType chosen = checked != null && checked.getTag() instanceof EmergencyType
                    ? (EmergencyType) checked.getTag() : null;
            filter = checkedId == R.id.chipPrivate ? "PRIVATE" : checkedId == R.id.chipPublic ? "PUBLIC"
                    : chosen == null || chosen.getName() == null ? "ALL" : chosen.getName();
            filterId = chosen == null ? null : chosen.getId();
            renderMap();
            updateStatus();
        });
        findViewById(R.id.fabLayers).setOnClickListener(v -> MapHelper.showLayerSwitchDialog(this, map, this::renderMap));
        findViewById(R.id.fabMyLocation).setOnClickListener(v -> {
            gps.request();
            Location location = gps.getLatest();
            if (mapReady && location != null) MapHelper.moveCamera(map, location.getLatitude(), location.getLongitude(), 15);
        });
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        bottom.setSelectedItemId(R.id.nav_rescuer_map);
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_rescuer_map) { refresh(); return true; }
            if (id == R.id.nav_rescuer_requests) {
                startActivity(new Intent(this, RescuerHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            } else if (id == R.id.nav_rescuer_missions) {
                startActivity(new Intent(this, RescuerMissionsActivity.class));
                finish();
            } else if (id == R.id.nav_rescuer_stats) {
                startActivity(new Intent(this, RescuerStatsActivity.class));
                finish();
            } else if (id == R.id.nav_rescuer_team) startActivity(new Intent(this, RescueTeamActivity.class));
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
        initMap(R.id.mapView, state, this::renderMap);
    }

    private void refresh() {
        execute("types", types::loadAll);
        execute("map", rescuer::loadMap);
    }

    private void openCurrentMission() { execute("mission", rescuer::loadMyActive); }

    private void renderTypeChips(List<EmergencyType> items) {
        ChipGroup group = findViewById(R.id.chipGroupFilter);
        bindingFilters = true;
        try {
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                int id = group.getChildAt(i).getId();
                if (id != R.id.chipAll && id != R.id.chipPublic && id != R.id.chipPrivate) group.removeViewAt(i);
            }
            int selectedId = "PRIVATE".equals(filter) ? R.id.chipPrivate
                    : "PUBLIC".equals(filter) ? R.id.chipPublic : R.id.chipAll;
            if (items != null) for (EmergencyType type : items) {
                if (type == null) continue;
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filter, group, false);
                chip.setId(View.generateViewId());
                chip.setText(type.getDisplayLabel());
                chip.setTag(type);
                group.addView(chip);
                if ((filterId != null && type.getId() != null && filterId.equalsIgnoreCase(type.getId()))
                        || (!"ALL".equals(filter) && type.getName() != null && filter.equalsIgnoreCase(type.getName()))) {
                    selectedId = chip.getId();
                }
            }
            group.check(selectedId);
            if (selectedId == R.id.chipAll) { filter = "ALL"; filterId = null; }
        } finally { bindingFilters = false; }
    }

    private boolean matchesFilter(SosRequest point) {
        if ("ALL".equals(filter)) return true;
        if ("PUBLIC".equals(filter) || "PRIVATE".equals(filter)) return filter.equals(point.getVisibility());
        if (filterId != null && point.getEmergencyTypeId() != null && filterId.equalsIgnoreCase(point.getEmergencyTypeId())) return true;
        return point.getEmergencyType() != null && filter.equalsIgnoreCase(point.getEmergencyType());
    }

    private void renderMap() {
        if (!mapReady) return;
        if (SosMapData.drawRescuer(this, map, points, gps.getLatest(), this::matchesFilter, !centered)) centered = true;
    }

    private void updateStatus() {
        if (mapError != null) { showApiError(mapError, this::refresh); return; }
        if (mapLoading) { showLoading("Đang tải bản đồ SOS…"); return; }
        clearStatus();
    }

    @Override protected void onStart() {
        super.onStart();
        if (rescuer != null) { refresh(); realtime.connect(); gps.request(); }
    }
    @Override protected void onStop() { if (realtime != null) realtime.disconnect(); super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("filter", filter);
        state.putString("filter_id", filterId);
        super.onSaveInstanceState(state);
    }
}
