package com.javafinal.ui.rescuer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.model.SosRequest;
import com.javafinal.util.MapHelper;
import com.javafinal.util.NavigationHelper;

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.util.Locale;

public class RealtimeTrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private SosRequest currentMission;

    private TextView tvMissionCode;
    private TextView tvSyncStatus;
    private TextView tvDistanceRemain;
    private TextView tvSpeed;
    private TextView tvEta;
    private TextView tvVictimTitle;
    private TextView tvVictimDetail;
    private TextView tvTypeBadge;

    private MaterialButton btnArrived;
    private MaterialButton btnComplete;
    private MaterialButton btnYieldMission;

    private final Handler locationBroadcastHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationBroadcastRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || currentMission == null) return;
            if (SosRequest.STATUS_RESCUING.equals(currentMission.getStatus())) {
                double rLat = currentMission.getRescuerLat();
                double rLng = currentMission.getRescuerLng();
                double vLat = currentMission.getVictimLat();
                double vLng = currentMission.getVictimLng();

                double nextLat = rLat + (vLat - rLat) * 0.25;
                double nextLng = rLng + (vLng - rLng) * 0.25;
                currentMission.setRescuerLat(nextLat);
                currentMission.setRescuerLng(nextLng);

                tvSyncStatus.setText("Đang đồng bộ vị trí trực tiếp");
                syncUiWithStatus();
                renderTacticalMap();
                locationBroadcastHandler.postDelayed(this, 4000);
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
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        MaterialButton btnCallVictim = findViewById(R.id.btnCallVictim);

        FloatingActionButton fabLayers = findViewById(R.id.fabLayers);
        FloatingActionButton fabMyLocation = findViewById(R.id.fabMyLocation);

        tvDistanceRemain = findViewById(R.id.tvDistanceRemain);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvEta = findViewById(R.id.tvEta);

        tvVictimTitle = findViewById(R.id.tvVictimTitle);
        tvVictimDetail = findViewById(R.id.tvVictimDetail);
        tvTypeBadge = findViewById(R.id.tvTypeBadge);

        btnArrived = findViewById(R.id.btnArrived);
        btnComplete = findViewById(R.id.btnComplete);
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

        fabMyLocation.setOnClickListener(v -> {
            if (mapLibreMap != null) {
                MapHelper.fitTwoPoints(
                        mapLibreMap,
                        new LatLng(currentMission.getRescuerLat(), currentMission.getRescuerLng()),
                        new LatLng(currentMission.getVictimLat(), currentMission.getVictimLng()),
                        120
                );
            }
        });

        // Cập nhật tiến trình tuần tự: ACCEPTED -> RESCUING -> ARRIVED
        btnArrived.setOnClickListener(v -> {
            String st = currentMission.getStatus();
            if (SosRequest.STATUS_ACCEPTED.equals(st)) {
                repo.updateMissionStatus(currentMission, SosRequest.STATUS_RESCUING);
                NavigationHelper.showSnack(this, "Đội đã bắt đầu di chuyển tới hiện trường");
                syncUiWithStatus();
                renderTacticalMap();
                locationBroadcastHandler.postDelayed(locationBroadcastRunnable, 3500);
            } else if (SosRequest.STATUS_RESCUING.equals(st)) {
                locationBroadcastHandler.removeCallbacks(locationBroadcastRunnable);
                repo.updateMissionStatus(currentMission, SosRequest.STATUS_ARRIVED);
                currentMission.setRescuerLat(currentMission.getVictimLat() - 0.0002);
                currentMission.setRescuerLng(currentMission.getVictimLng() - 0.0002);
                NavigationHelper.showSnack(this, "Đã tới hiện trường");
                syncUiWithStatus();
                renderTacticalMap();
            } else if (SosRequest.STATUS_ARRIVED.equals(st)) {
                openConfirmComplete();
            }
        });

        // Hoàn thành ca (bắt buộc phải tới nơi trước)
        btnComplete.setOnClickListener(v -> {
            if (!SosRequest.STATUS_ARRIVED.equals(currentMission.getStatus())) {
                NavigationHelper.showSnack(this,
                        "Vui lòng cập nhật trạng thái đã tới nơi trước khi hoàn thành ca");
                return;
            }
            openConfirmComplete();
        });

        // Nhả ca (chỉ cho phép khi chưa tới nơi)
        btnYieldMission.setOnClickListener(v -> {
            if (SosRequest.STATUS_ARRIVED.equals(currentMission.getStatus())) {
                NavigationHelper.showSnack(this,
                        "Không thể nhượng ca khi đội đã có mặt tại hiện trường");
                return;
            }
            NavigationHelper.showConfirmSheet(
                    this,
                    "NHƯỢNG LẠI CA",
                    "Xác nhận nhượng lại ca cứu hộ?",
                    "Đội gặp sự cố hoặc cần chuyển giao ca cứu hộ này cho đội khác?\n\n"
                            + "Yêu cầu sẽ quay lại danh sách chờ để đội cứu hộ gần nhất tiếp nhận.",
                    "Xác nhận nhượng ca",
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
        tvMissionCode.setText(currentMission.getShortId() + " • " + currentMission.getStatusLabel());
        tvVictimTitle.setText(currentMission.getDescription());
        tvVictimDetail.setText("Nạn nhân: " + currentMission.getRequesterName() + " • " + currentMission.getRequesterPhone());
        tvTypeBadge.setText(currentMission.getEmergencyTypeLabel());

        double distKm = currentMission.getDistanceKmFrom(
                currentMission.getRescuerLat(), currentMission.getRescuerLng());
        tvDistanceRemain.setText(SosRequest.STATUS_ARRIVED.equals(st)
                ? "0.0 km"
                : String.format(Locale.US, "%.2f km", distKm));
        tvSpeed.setText(String.format(Locale.US, "±%.1fm", currentMission.getRescuerAccuracyM()));
        tvEta.setText(currentMission.getStatusLabel());

        if (SosRequest.STATUS_ACCEPTED.equals(st)) {
            btnArrived.setText("Bước 1: Bắt đầu di chuyển");
            btnArrived.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.ink)));
            btnComplete.setAlpha(0.5f);
            btnYieldMission.setEnabled(true);
        } else if (SosRequest.STATUS_RESCUING.equals(st)) {
            btnArrived.setText("Bước 2: Xác nhận đã tới hiện trường");
            btnArrived.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.red)));
            btnComplete.setAlpha(0.5f);
            btnYieldMission.setEnabled(true);
        } else {
            btnArrived.setText("Đã tới nơi • Bấm để hoàn thành ca");
            btnArrived.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.green)));
            btnComplete.setAlpha(1.0f);
            btnYieldMission.setEnabled(false);
        }
    }

    private void renderTacticalMap() {
        if (mapLibreMap == null || currentMission == null) return;
        mapLibreMap.clear();
        MapHelper.drawRescueRoute(
                this,
                mapLibreMap,
                currentMission.getRescuerLat(),
                currentMission.getRescuerLng(),
                currentMission.getVictimLat(),
                currentMission.getVictimLng(),
                "Đội: " + currentMission.getTeamName(),
                "Nạn nhân: " + currentMission.getRequesterName()
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
