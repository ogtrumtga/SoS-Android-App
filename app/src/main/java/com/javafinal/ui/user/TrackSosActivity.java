package com.javafinal.ui.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.model.SosRequest;
import com.javafinal.util.MapHelper;
import com.javafinal.util.NavigationHelper;

import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.util.List;
import java.util.Locale;

public class TrackSosActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private SosRequest currentSos;

    private TextView tvStep;
    private LinearProgressIndicator progressRescue;
    private TextView tvTeamName;
    private TextView tvTeamStatus;
    private TextView tvEta;
    private MaterialButton btnSimulateWsEvent;
    private MaterialButton btnCallTeam;

    private TextView tvCoordinates;
    private TextView tvAccuracy;
    private TextView tvEmergencyTypeBadge;
    private TextView tvSituation;
    private TextView tvPeople;
    private TextView tvAddress;
    private TextView tvNeeds;
    private TextView tvVisibilityTitle;
    private TextView tvVisibilitySub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        MapHelper.initMapLibre(this);
        setContentView(R.layout.activity_track_sos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MockDataRepository repo = MockDataRepository.getInstance();
        currentSos = repo.getMyActiveUserSos();
        if (currentSos == null) {
            currentSos = repo.getLastViewedSos();
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(currentSos.getShortId());
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStep = findViewById(R.id.tvStep);
        progressRescue = findViewById(R.id.progressRescue);
        tvTeamName = findViewById(R.id.tvTeamName);
        tvTeamStatus = findViewById(R.id.tvTeamStatus);
        tvEta = findViewById(R.id.tvEta);
        btnSimulateWsEvent = findViewById(R.id.btnSimulateWsEvent);

        tvCoordinates = findViewById(R.id.tvCoordinates);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        MaterialButton btnCenterMap = findViewById(R.id.btnCenterMap);
        btnCallTeam = findViewById(R.id.btnCallTeam);

        tvEmergencyTypeBadge = findViewById(R.id.tvEmergencyTypeBadge);
        tvSituation = findViewById(R.id.tvSituation);
        tvPeople = findViewById(R.id.tvPeople);
        tvAddress = findViewById(R.id.tvAddress);
        tvNeeds = findViewById(R.id.tvNeeds);

        tvVisibilityTitle = findViewById(R.id.tvVisibilityTitle);
        tvVisibilitySub = findViewById(R.id.tvVisibilitySub);
        MaterialButton btnChangeMode = findViewById(R.id.btnChangeMode);
        MaterialButton btnCancelSos = findViewById(R.id.btnCancelSos);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        MapHelper.enableScrollViewTouch(mapView);

        bindSosStateToUi();

        mapView.getMapAsync(map -> {
            this.mapLibreMap = map;
            MapHelper.applyDefaultStyle(map, style -> renderMapState());
        });

        btnSimulateWsEvent.setOnClickListener(v -> simulateNextStateMachineStep());

        btnCenterMap.setOnClickListener(v -> renderMapState());

        btnCallTeam.setOnClickListener(v -> {
            if (currentSos.getRescuerPhone() != null) {
                NavigationHelper.dialPhone(this, currentSos.getRescuerPhone());
            } else {
                NavigationHelper.showSnack(this, "Đang chờ đội cứu hộ tiếp nhận");
            }
        });

        // Đổi chế độ hiển thị
        btnChangeMode.setOnClickListener(v -> {
            boolean isPublic = SosRequest.VISIBILITY_PUBLIC.equals(currentSos.getVisibility());
            String nextVis = isPublic ? SosRequest.VISIBILITY_PRIVATE : SosRequest.VISIBILITY_PUBLIC;
            currentSos.setVisibility(nextVis);
            currentSos.addLog("Đã chuyển chế độ hiển thị sang " + currentSos.getVisibilityLabel());
            bindSosStateToUi();
            NavigationHelper.showSnack(this, "Đã chuyển chế độ hiển thị sang " + currentSos.getVisibilityLabel());
        });

        // Nạn nhân hủy SOS
        btnCancelSos.setOnClickListener(v -> {
            NavigationHelper.showConfirmSheet(
                    this,
                    "HỦY YÊU CẦU",
                    "Xác nhận hủy yêu cầu SOS?",
                    "Bạn xác nhận đã an toàn hoặc muốn hủy yêu cầu cứu hộ này?\n\n"
                            + "Hệ thống sẽ đóng phiên cứu hộ và ngừng chia sẻ vị trí trực tiếp của bạn.",
                    "Xác nhận hủy",
                    true,
                    () -> {
                        repo.cancelSosByUser(currentSos);
                        Intent intent = new Intent(TrackSosActivity.this, SosResultActivity.class);
                        startActivity(intent);
                        finish();
                    },
                    "Quay lại",
                    null
            );
        });
    }

    private void simulateNextStateMachineStep() {
        MockDataRepository repo = MockDataRepository.getInstance();
        String status = currentSos.getStatus();

        if (SosRequest.STATUS_WAITING.equals(status)) {
            repo.acceptSosMission(currentSos);
            NavigationHelper.showSnack(this, "Đội cứu hộ đã tiếp nhận yêu cầu");
        } else if (SosRequest.STATUS_ACCEPTED.equals(status)) {
            repo.updateMissionStatus(currentSos, SosRequest.STATUS_RESCUING);
            currentSos.setRescuerLat((currentSos.getRescuerLat() + currentSos.getVictimLat()) / 2.0);
            currentSos.setRescuerLng((currentSos.getRescuerLng() + currentSos.getVictimLng()) / 2.0);
            NavigationHelper.showSnack(this, "Đội cứu hộ đang di chuyển tới bạn");
        } else if (SosRequest.STATUS_RESCUING.equals(status)) {
            repo.updateMissionStatus(currentSos, SosRequest.STATUS_ARRIVED);
            currentSos.setRescuerLat(currentSos.getVictimLat() - 0.0003);
            currentSos.setRescuerLng(currentSos.getVictimLng() - 0.0003);
            NavigationHelper.showSnack(this, "Đội cứu hộ đã tới hiện trường");
        } else {
            repo.completeMission(currentSos);
            startActivity(new Intent(TrackSosActivity.this, SosResultActivity.class));
            finish();
            return;
        }

        bindSosStateToUi();
        renderMapState();
    }

    private void bindSosStateToUi() {
        String status = currentSos.getStatus();
        tvStep.setText(currentSos.getStatusLabel());

        if (SosRequest.STATUS_WAITING.equals(status)) {
            progressRescue.setProgressCompat(1, true);
            tvTeamName.setText("Đang kết nối với đội cứu hộ...");
            tvTeamStatus.setText("Hệ thống đang thông báo cho các đội cứu hộ gần bạn");
            tvEta.setText("Đang chờ");
            btnSimulateWsEvent.setText("Chuyển bước tiếp: Đội cứu hộ nhận ca");
            btnCallTeam.setEnabled(false);
        } else if (SosRequest.STATUS_ACCEPTED.equals(status)) {
            progressRescue.setProgressCompat(2, true);
            tvTeamName.setText(currentSos.getTeamName());
            tvTeamStatus.setText("Phụ trách: " + currentSos.getRescuerName() + " • " + currentSos.getRescuerPhone());
            tvEta.setText("Đã tiếp nhận");
            btnSimulateWsEvent.setText("Chuyển bước tiếp: Đội bắt đầu di chuyển");
            btnCallTeam.setEnabled(true);
        } else if (SosRequest.STATUS_RESCUING.equals(status)) {
            progressRescue.setProgressCompat(3, true);
            tvTeamName.setText(currentSos.getTeamName());
            double dist = currentSos.getDistanceKmFrom(currentSos.getRescuerLat(), currentSos.getRescuerLng());
            tvTeamStatus.setText("Đang di chuyển (Cách ~" + dist + " km) • " + currentSos.getRescuerPhone());
            tvEta.setText("Đang đến");
            btnSimulateWsEvent.setText("Chuyển bước tiếp: Đội tới nơi");
            btnCallTeam.setEnabled(true);
        } else {
            progressRescue.setProgressCompat(4, true);
            tvTeamName.setText(currentSos.getTeamName());
            tvTeamStatus.setText("Đã tới hiện trường (" + currentSos.getRescuerName() + ")");
            tvEta.setText("Đã tới nơi");
            btnSimulateWsEvent.setText("Chuyển bước tiếp: Hoàn thành ca cứu hộ");
            btnCallTeam.setEnabled(true);
        }

        tvCoordinates.setText(String.format(Locale.US, "Vị trí của bạn: %.6f, %.6f",
                currentSos.getVictimLat(), currentSos.getVictimLng()));
        tvAccuracy.setText(String.format(Locale.US, "Độ chính xác GPS: ±%.1fm",
                currentSos.getVictimAccuracyM()));

        tvEmergencyTypeBadge.setText(currentSos.getEmergencyTypeLabel());
        tvSituation.setText(currentSos.getDescription());
        tvPeople.setText("Người gửi: " + currentSos.getRequesterName() + " • " + currentSos.getRequesterPhone());
        tvAddress.setText("Thời gian gửi: " + currentSos.getCreatedAt() + " • Mã: " + currentSos.getShortId());

        List<String> logs = currentSos.getLogs();
        StringBuilder logBuilder = new StringBuilder("Nhật ký hoạt động:\n");
        for (int i = 0; i < logs.size(); i++) {
            logBuilder.append("• ").append(logs.get(i));
            if (i < logs.size() - 1) logBuilder.append("\n");
        }
        tvNeeds.setText(logBuilder.toString());

        boolean isPublic = SosRequest.VISIBILITY_PUBLIC.equals(currentSos.getVisibility());
        tvVisibilityTitle.setText("Chế độ hiển thị: " + currentSos.getVisibilityLabel());
        tvVisibilitySub.setText(isPublic
                ? "Hiển thị vùng cảnh báo (~1km) trên bản đồ cộng đồng"
                : "Chế độ riêng tư: Ẩn khỏi bản đồ cộng đồng, chỉ đội cứu hộ thấy");
    }

    private void renderMapState() {
        if (mapLibreMap == null) return;
        mapLibreMap.clear();

        if (SosRequest.STATUS_WAITING.equals(currentSos.getStatus()) || currentSos.getTeamId() == null) {
            Icon victimIcon = MapHelper.createPinIcon(this, Color.parseColor("#E5383B"), "SOS");
            mapLibreMap.addMarker(new MarkerOptions()
                    .position(new LatLng(currentSos.getVictimLat(), currentSos.getVictimLng()))
                    .title("Vị trí của bạn (" + currentSos.getShortId() + ")")
                    .snippet(currentSos.getDescription())
                    .icon(victimIcon));
            MapHelper.moveCamera(mapLibreMap, currentSos.getVictimLat(), currentSos.getVictimLng(), 15.2);
        } else {
            MapHelper.drawRescueRoute(
                    this,
                    mapLibreMap,
                    currentSos.getRescuerLat(),
                    currentSos.getRescuerLng(),
                    currentSos.getVictimLat(),
                    currentSos.getVictimLng(),
                    "Đội cứu hộ: " + currentSos.getTeamName(),
                    "Người gửi: " + currentSos.getRequesterName()
            );
        }
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
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
