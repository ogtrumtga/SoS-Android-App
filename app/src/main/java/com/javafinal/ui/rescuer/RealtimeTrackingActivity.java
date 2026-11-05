package com.javafinal.ui.rescuer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.model.SosRequest;
import com.javafinal.util.MapHelper;
import com.javafinal.util.NavigationHelper;

import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.util.Locale;

public class RealtimeTrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private SosRequest currentMission;

    private TextView tvMissionCode;
    private TextView tvDistanceRemain;
    private TextView tvSpeed;
    private TextView tvEta;
    private TextView tvVictimTitle;
    private TextView tvVictimDetail;
    private TextView tvTypeBadge;

    private MaterialButton btnArrived;
    private MaterialButton btnYieldMission;

    private final Handler locationBroadcastHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationBroadcastRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || currentMission == null) return;
            if (SosRequest.STATUS_RESCUING.equals(currentMission.getStatus())) {
                boolean moved = currentMission.advanceAlongRoute();
                syncUiWithStatus();
                renderTacticalMap();
                if (moved) {
                    locationBroadcastHandler.postDelayed(this, 3200);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        MapHelper.initMapLibre(this);
        setContentView(R.layout.activity_realtime_tracking);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MockDataRepository repo = MockDataRepository.getInstance();
        String sosId = getIntent().getStringExtra("SOS_ID");
        currentMission = repo.getMyActiveRescuerMission();
        if (currentMission == null) {
            currentMission = repo.getSosById(sosId);
            if (SosRequest.STATUS_WAITING.equals(currentMission.getStatus())) {
                repo.acceptSosMission(currentMission);
            }
        }

        MaterialButton btnBack = findViewById(R.id.btnBack);
        tvMissionCode = findViewById(R.id.tvMissionCode);
        MaterialButton btnCallVictim = findViewById(R.id.btnCallVictim);

        MaterialButton fabLayers = findViewById(R.id.fabLayers);
        MaterialButton fabMyLocation = findViewById(R.id.fabMyLocation);

        tvDistanceRemain = findViewById(R.id.tvDistanceRemain);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvEta = findViewById(R.id.tvEta);

        tvVictimTitle = findViewById(R.id.tvVictimTitle);
        tvVictimDetail = findViewById(R.id.tvVictimDetail);
        tvTypeBadge = findViewById(R.id.tvTypeBadge);

        btnArrived = findViewById(R.id.btnArrived);
        btnYieldMission = findViewById(R.id.btnYieldMission);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        syncUiWithStatus();

        mapView.getMapAsync(map -> {
            this.mapLibreMap = map;
            MapHelper.applyDefaultStyle(map, style -> renderTacticalMap());
        });

        btnBack.setOnClickListener(v -> finish());
        btnCallVictim.setOnClickListener(v ->
                NavigationHelper.dialPhone(this, currentMission.getRequesterPhone()));

        fabLayers.setOnClickListener(v ->
                MapHelper.showLayerSwitchDialog(this, mapLibreMap, this::renderTacticalMap));

        fabMyLocation.setOnClickListener(v -> renderTacticalMap());

        btnArrived.setOnClickListener(v -> {
            String st = currentMission.getStatus();
            if (SosRequest.STATUS_ACCEPTED.equals(st)) {
                repo.updateMissionStatus(currentMission, SosRequest.STATUS_RESCUING);
                syncUiWithStatus();
                renderTacticalMap();
                locationBroadcastHandler.postDelayed(locationBroadcastRunnable, 2500);
            } else if (SosRequest.STATUS_RESCUING.equals(st)) {
                locationBroadcastHandler.removeCallbacks(locationBroadcastRunnable);
                repo.updateMissionStatus(currentMission, SosRequest.STATUS_ARRIVED);
                currentMission.snapRescuerToDestination();
                syncUiWithStatus();
                renderTacticalMap();
            } else if (SosRequest.STATUS_ARRIVED.equals(st)) {
                openConfirmComplete();
            }
        });

        btnYieldMission.setOnClickListener(v -> {
            if (SosRequest.STATUS_ARRIVED.equals(currentMission.getStatus())) {
                return;
            }
            NavigationHelper.showConfirmSheet(
                    this,
                    null,
                    "Nhả ca cứu hộ?",
                    "Yêu cầu sẽ quay lại hàng đợi để đội khác tiếp nhận.",
                    "Xác nhận nhả ca",
                    true,
                    () -> {
                        repo.releaseMission(currentMission);
                        finish();
                    },
                    "Hủy",
                    null
            );
        });
    }

    private void openConfirmComplete() {
        Intent intent = new Intent(RealtimeTrackingActivity.this, ConfirmCompleteActivity.class);
        intent.putExtra("SOS_ID", currentMission.getId());
        startActivity(intent);
    }

    private void syncUiWithStatus() {
        String st = currentMission.getStatus();
        tvMissionCode.setText("SOS " + currentMission.getShortId());
        tvVictimTitle.setText(currentMission.getDescription());
        tvVictimDetail.setText(currentMission.getRequesterName() + " • " + currentMission.getRequesterPhone());
        tvTypeBadge.setText(currentMission.getEmergencyTypeLabel());

        double distKm = currentMission.getRemainingRouteDistanceKm();
        tvDistanceRemain.setText(SosRequest.STATUS_ARRIVED.equals(st)
                ? "0.0 km"
                : String.format(Locale.US, "%.1f km", distKm));
        tvSpeed.setText(String.format(Locale.US, "±%.1fm", currentMission.getRescuerAccuracyM()));
        tvEta.setText(currentMission.getStatusLabel());

        if (SosRequest.STATUS_ACCEPTED.equals(st)) {
            btnArrived.setText("Bắt đầu di chuyển");
            btnArrived.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.ink)));
            btnYieldMission.setVisibility(View.VISIBLE);
        } else if (SosRequest.STATUS_RESCUING.equals(st)) {
            btnArrived.setText("Đã tới hiện trường");
            btnArrived.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.red)));
            btnYieldMission.setVisibility(View.VISIBLE);
        } else {
            btnArrived.setText("Hoàn thành ca cứu hộ");
            btnArrived.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.green)));
            btnYieldMission.setVisibility(View.GONE);
        }
    }

    private void renderTacticalMap() {
        if (mapLibreMap == null || currentMission == null) return;
        mapLibreMap.clear();
        MockDataRepository repo = MockDataRepository.getInstance();
        MapHelper.drawRescueRoute(
                this,
                mapLibreMap,
                currentMission,
                repo.getRescuerBaseLat(),
                repo.getRescuerBaseLng(),
                currentMission.getTeamName(),
                currentMission.getRequesterName(),
                this::syncUiWithStatus
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        syncUiWithStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationBroadcastHandler.removeCallbacksAndMessages(null);
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
