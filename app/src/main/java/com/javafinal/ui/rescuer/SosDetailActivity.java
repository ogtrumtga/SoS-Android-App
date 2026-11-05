package com.javafinal.ui.rescuer;

import android.content.Intent;
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
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.model.SosRequest;
import com.javafinal.util.MapHelper;
import com.javafinal.util.NavigationHelper;

import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.util.List;
import java.util.Locale;

public class SosDetailActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private SosRequest currentSos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        MapHelper.initMapLibre(this);
        setContentView(R.layout.activity_sos_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MockDataRepository repo = MockDataRepository.getInstance();
        String sosId = getIntent().getStringExtra("SOS_ID");
        currentSos = repo.getSosById(sosId);
        repo.setLastViewedSos(currentSos);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle(currentSos.getShortId() + " • " + currentSos.getStatusLabel());
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvPrivacyLevel = findViewById(R.id.tvPrivacyLevel);
        TextView tvUrgencyLevel = findViewById(R.id.tvUrgencyLevel);
        TextView tvIncidentTitle = findViewById(R.id.tvIncidentTitle);
        TextView tvCreatedAt = findViewById(R.id.tvCreatedAt);

        TextView tvAccuracy = findViewById(R.id.tvAccuracy);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvCoordinates = findViewById(R.id.tvCoordinates);

        TextView tvVictimContact = findViewById(R.id.tvVictimContact);
        TextView tvCommandNote = findViewById(R.id.tvCommandNote);

        MaterialButton btnCallVictim = findViewById(R.id.btnCallVictim);
        MaterialButton btnSmsVictim = findViewById(R.id.btnSmsVictim);
        MaterialButton btnAcceptMission = findViewById(R.id.btnAcceptMission);

        tvPrivacyLevel.setText(currentSos.getVisibilityLabel() + " • " + currentSos.getStatusLabel());
        tvUrgencyLevel.setText("Sự cố: " + currentSos.getEmergencyTypeLabel());
        tvIncidentTitle.setText(currentSos.getDescription());
        tvCreatedAt.setText("Thời gian gửi: " + currentSos.getCreatedAt());

        tvAccuracy.setText(String.format(Locale.US, "±%.1fm", currentSos.getVictimAccuracyM()));
        tvAddress.setText(String.format(Locale.US, "Tọa độ: %.6f, %.6f",
                currentSos.getVictimLat(), currentSos.getVictimLng()));

        double distKm = currentSos.getDistanceKmFrom(repo.getRescuerBaseLat(), repo.getRescuerBaseLng());
        tvCoordinates.setText(String.format(Locale.US,
                "Khoảng cách tới vị trí đội của bạn: ~%.1f km", distKm));

        tvVictimContact.setText(currentSos.getRequesterName() + " • " + currentSos.getRequesterPhone());

        List<String> logs = currentSos.getLogs();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < logs.size(); i++) {
            sb.append("• ").append(logs.get(i));
            if (i < logs.size() - 1) sb.append("\n");
        }
        tvCommandNote.setText(sb.toString());

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        MapHelper.enableScrollViewTouch(mapView);

        mapView.getMapAsync(map -> {
            this.mapLibreMap = map;
            MapHelper.applyDefaultStyle(map, style -> {
                MapHelper.drawRescueRoute(
                        this,
                        map,
                        repo.getRescuerBaseLat(),
                        repo.getRescuerBaseLng(),
                        currentSos.getVictimLat(),
                        currentSos.getVictimLng(),
                        "Đội: " + repo.getRescuerTeamName(),
                        "Người gửi: " + currentSos.getRequesterName()
                );
            });
        });

        btnCallVictim.setOnClickListener(v ->
                NavigationHelper.dialPhone(this, currentSos.getRequesterPhone()));

        btnSmsVictim.setOnClickListener(v ->
                NavigationHelper.sendSms(
                        this,
                        currentSos.getRequesterPhone(),
                        "[" + repo.getRescuerTeamName() + "] Chúng tôi đã nhận được tọa độ SOS "
                                + currentSos.getShortId() + " và đang chuẩn bị tiếp cận."
                ));

        btnAcceptMission.setOnClickListener(v -> {
            SosRequest busyMission = repo.getMyActiveRescuerMission();
            if (busyMission != null && !busyMission.getId().equals(currentSos.getId())) {
                NavigationHelper.showConfirmSheet(
                        this,
                        "ĐANG CÓ CA CỨU HỘ",
                        "Đội đang xử lý một ca khác",
                        "Đội của bạn đang xử lý ca " + busyMission.getShortId()
                                + ". Vui lòng hoàn thành ca hiện tại trước khi nhận ca mới.",
                        "Mở ca hiện tại",
                        false,
                        () -> {
                            Intent intent = new Intent(SosDetailActivity.this, RealtimeTrackingActivity.class);
                            intent.putExtra("SOS_ID", busyMission.getId());
                            startActivity(intent);
                            finish();
                        },
                        "Đóng",
                        null
                );
                return;
            }

            repo.acceptSosMission(currentSos);
            Intent intent = new Intent(SosDetailActivity.this, RealtimeTrackingActivity.class);
            intent.putExtra("SOS_ID", currentSos.getId());
            startActivity(intent);
            finish();
        });
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
