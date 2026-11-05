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

import java.util.Locale;

public class TrackSosActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private SosRequest currentSos;

    private TextView tvStep;
    private LinearProgressIndicator progressRescue;
    private TextView tvTeamName;
    private TextView tvTeamStatus;
    private MaterialButton btnCallTeam;

    private TextView tvCoordinates;
    private TextView tvEmergencyTypeBadge;
    private TextView tvSituation;
    private TextView tvAddress;
    private MaterialButton btnChangeMode;

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
        toolbar.setTitle("SOS " + currentSos.getShortId());
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStep = findViewById(R.id.tvStep);
        progressRescue = findViewById(R.id.progressRescue);
        tvTeamName = findViewById(R.id.tvTeamName);
        tvTeamStatus = findViewById(R.id.tvTeamStatus);

        tvCoordinates = findViewById(R.id.tvCoordinates);
        MaterialButton btnCenterMap = findViewById(R.id.btnCenterMap);
        btnCallTeam = findViewById(R.id.btnCallTeam);

        tvEmergencyTypeBadge = findViewById(R.id.tvEmergencyTypeBadge);
        tvSituation = findViewById(R.id.tvSituation);
        tvAddress = findViewById(R.id.tvAddress);
        btnChangeMode = findViewById(R.id.btnChangeMode);
        MaterialButton btnCancelSos = findViewById(R.id.btnCancelSos);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        MapHelper.enableScrollViewTouch(mapView);

        bindSosStateToUi();

        mapView.getMapAsync(map -> {
            this.mapLibreMap = map;
            MapHelper.applyDefaultStyle(map, style -> renderMapState());
        });

        btnCenterMap.setOnClickListener(v -> renderMapState());

        btnCallTeam.setOnClickListener(v -> {
            if (currentSos.getRescuerPhone() != null) {
                NavigationHelper.dialPhone(this, currentSos.getRescuerPhone());
            } else {
                NavigationHelper.showSnack(this, "Đang chờ đội cứu hộ tiếp nhận");
            }
        });

        btnChangeMode.setOnClickListener(v -> {
            boolean isPublic = SosRequest.VISIBILITY_PUBLIC.equals(currentSos.getVisibility());
            String nextVis = isPublic ? SosRequest.VISIBILITY_PRIVATE : SosRequest.VISIBILITY_PUBLIC;
            currentSos.setVisibility(nextVis);
            bindSosStateToUi();
            NavigationHelper.showSnack(this, "Chế độ: " + currentSos.getVisibilityLabel());
        });

        btnCancelSos.setOnClickListener(v -> {
            NavigationHelper.showConfirmSheet(
                    this,
                    null,
                    "Hủy yêu cầu SOS?",
                    "Bạn xác nhận đã an toàn và muốn hủy yêu cầu cứu hộ này?",
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

    private void bindSosStateToUi() {
        String status = currentSos.getStatus();
        tvStep.setText(currentSos.getStatusLabel());

        if (SosRequest.STATUS_WAITING.equals(status)) {
            progressRescue.setProgressCompat(1, true);
            tvTeamName.setText("Đang tìm đội cứu hộ...");
            tvTeamStatus.setText("Đã gửi tín hiệu tới các đội gần bạn");
            btnCallTeam.setEnabled(false);
        } else if (SosRequest.STATUS_ACCEPTED.equals(status)) {
            progressRescue.setProgressCompat(2, true);
            tvTeamName.setText(currentSos.getTeamName());
            tvTeamStatus.setText(currentSos.getRescuerName() + " • " + currentSos.getRescuerPhone());
            btnCallTeam.setEnabled(true);
        } else if (SosRequest.STATUS_RESCUING.equals(status)) {
            progressRescue.setProgressCompat(3, true);
            tvTeamName.setText(currentSos.getTeamName());
            double dist = currentSos.getDistanceKmFrom(currentSos.getRescuerLat(), currentSos.getRescuerLng());
            tvTeamStatus.setText("Cách ~" + dist + " km • " + currentSos.getRescuerPhone());
            btnCallTeam.setEnabled(true);
        } else {
            progressRescue.setProgressCompat(4, true);
            tvTeamName.setText(currentSos.getTeamName());
            tvTeamStatus.setText("Đã tới nơi • " + currentSos.getRescuerName());
            btnCallTeam.setEnabled(true);
        }

        tvCoordinates.setText(String.format(Locale.US, "%.5f, %.5f (±%.1fm)",
                currentSos.getVictimLat(), currentSos.getVictimLng(), currentSos.getVictimAccuracyM()));

        tvEmergencyTypeBadge.setText(currentSos.getEmergencyTypeLabel());
        tvSituation.setText(currentSos.getDescription());
        tvAddress.setText(currentSos.getCreatedAt());
        btnChangeMode.setText(currentSos.getVisibilityLabel());
    }

    private void renderMapState() {
        if (mapLibreMap == null) return;
        mapLibreMap.clear();

        if (SosRequest.STATUS_WAITING.equals(currentSos.getStatus()) || currentSos.getTeamId() == null) {
            Icon victimIcon = MapHelper.createPinIcon(this, Color.parseColor("#E5383B"), "SOS");
            mapLibreMap.addMarker(new MarkerOptions()
                    .position(new LatLng(currentSos.getVictimLat(), currentSos.getVictimLng()))
                    .title("Vị trí của bạn")
                    .icon(victimIcon));
            MapHelper.moveCamera(mapLibreMap, currentSos.getVictimLat(), currentSos.getVictimLng(), 15.2);
        } else {
            MockDataRepository repo = MockDataRepository.getInstance();
            MapHelper.drawRescueRoute(
                    this,
                    mapLibreMap,
                    currentSos,
                    repo.getRescuerBaseLat(),
                    repo.getRescuerBaseLng(),
                    currentSos.getTeamName(),
                    currentSos.getRequesterName(),
                    this::bindSosStateToUi
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
