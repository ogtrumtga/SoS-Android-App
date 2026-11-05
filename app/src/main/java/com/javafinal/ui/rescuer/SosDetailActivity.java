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
        toolbar.setTitle("SOS " + currentSos.getShortId());
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvPrivacyLevel = findViewById(R.id.tvPrivacyLevel);
        TextView tvIncidentTitle = findViewById(R.id.tvIncidentTitle);
        TextView tvCreatedAt = findViewById(R.id.tvCreatedAt);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvCoordinates = findViewById(R.id.tvCoordinates);
        TextView tvVictimContact = findViewById(R.id.tvVictimContact);

        MaterialButton btnCallVictim = findViewById(R.id.btnCallVictim);
        MaterialButton btnSmsVictim = findViewById(R.id.btnSmsVictim);
        MaterialButton btnAcceptMission = findViewById(R.id.btnAcceptMission);

        tvPrivacyLevel.setText(currentSos.getEmergencyTypeLabel() + " • " + currentSos.getVisibilityLabel());
        tvIncidentTitle.setText(currentSos.getDescription());
        tvCreatedAt.setText(currentSos.getCreatedAt());

        tvAddress.setText(String.format(Locale.US, "%.5f, %.5f (±%.1fm)",
                currentSos.getVictimLat(), currentSos.getVictimLng(), currentSos.getVictimAccuracyM()));

        double distKm = currentSos.getDistanceKmFrom(repo.getRescuerBaseLat(), repo.getRescuerBaseLng());
        tvCoordinates.setText(String.format(Locale.US, "Cách ~%.1f km", distKm));

        tvVictimContact.setText(currentSos.getRequesterName() + " • " + currentSos.getRequesterPhone());

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        MapHelper.enableScrollViewTouch(mapView);

        mapView.getMapAsync(map -> {
            this.mapLibreMap = map;
            MapHelper.applyDefaultStyle(map, style -> {
                MapHelper.drawRescueRoute(
                        this,
                        map,
                        currentSos,
                        repo.getRescuerBaseLat(),
                        repo.getRescuerBaseLng(),
                        repo.getRescuerTeamName(),
                        currentSos.getRequesterName(),
                        () -> {
                            double updatedDist = currentSos.getRemainingRouteDistanceKm();
                            tvCoordinates.setText(String.format(Locale.US, "Cách ~%.1f km", updatedDist));
                        }
                );
            });
        });

        btnCallVictim.setOnClickListener(v ->
                NavigationHelper.dialPhone(this, currentSos.getRequesterPhone()));

        btnSmsVictim.setOnClickListener(v ->
                NavigationHelper.sendSms(
                        this,
                        currentSos.getRequesterPhone(),
                        "Đội cứu hộ đã nhận được vị trí SOS của bạn và đang chuẩn bị tiếp cận."
                ));

        btnAcceptMission.setOnClickListener(v -> {
            SosRequest busyMission = repo.getMyActiveRescuerMission();
            if (busyMission != null && !busyMission.getId().equals(currentSos.getId())) {
                NavigationHelper.showConfirmSheet(
                        this,
                        null,
                        "Đội đang xử lý ca khác",
                        busyMission.getEmergencyTypeLabel() + " • " + busyMission.getStatusLabel(),
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
