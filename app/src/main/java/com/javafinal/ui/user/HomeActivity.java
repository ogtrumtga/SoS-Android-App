package com.javafinal.ui.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

public class HomeActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;

    private TextView tvUserGreeting;
    private TextView tvMyLocation;
    private MaterialCardView cardMyActiveSos;
    private TextView tvActiveSosSummary;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        MapHelper.initMapLibre(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MockDataRepository repo = MockDataRepository.getInstance();
        repo.setCurrentRole(MockDataRepository.ROLE_USER);

        tvUserGreeting = findViewById(R.id.tvUserGreeting);
        tvMyLocation = findViewById(R.id.tvMyLocation);
        cardMyActiveSos = findViewById(R.id.cardMyActiveSos);
        tvActiveSosSummary = findViewById(R.id.tvActiveSosSummary);
        MaterialButton btnTrackActiveSos = findViewById(R.id.btnTrackActiveSos);

        ChipGroup chipGroupFilter = findViewById(R.id.chipGroupFilter);
        FloatingActionButton fabLayers = findViewById(R.id.fabLayers);
        FloatingActionButton fabMyLocation = findViewById(R.id.fabMyLocation);
        MaterialButton btnSos = findViewById(R.id.btnSos);
        MaterialButton btnNotifications = findViewById(R.id.btnNotifications);
        MaterialButton btnProfile = findViewById(R.id.btnProfile);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        updateHeaderAndActiveSosBanner();

        mapView.getMapAsync(map -> {
            this.mapLibreMap = map;
            MapHelper.applyDefaultStyle(map, style -> {
                MapHelper.moveCamera(map, repo.getMyLat(), repo.getMyLng(), 13.8);
                renderPublicMapMarkers();
            });
        });

        btnTrackActiveSos.setOnClickListener(v -> {
            SosRequest active = repo.getMyActiveUserSos();
            if (active != null) {
                repo.setLastViewedSos(active);
                startActivity(new Intent(HomeActivity.this, TrackSosActivity.class));
            }
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipBoat) {
                currentFilter = SosRequest.TYPE_NGAP_LUT;
            } else if (id == R.id.chipMedical) {
                currentFilter = SosRequest.TYPE_Y_TE;
            } else if (id == R.id.chipFood) {
                currentFilter = SosRequest.TYPE_NHU_YEU_PHAM;
            } else {
                currentFilter = "ALL";
            }
            renderPublicMapMarkers();
        });

        fabLayers.setOnClickListener(v ->
                MapHelper.showLayerSwitchDialog(this, mapLibreMap, this::renderPublicMapMarkers));

        fabMyLocation.setOnClickListener(v -> {
            MapHelper.moveCamera(mapLibreMap, repo.getMyLat(), repo.getMyLng(), 15.2);
            tvMyLocation.setText(String.format(Locale.US,
                    "GPS: %.4f, %.4f (±%.1fm)",
                    repo.getMyLat(), repo.getMyLng(), repo.getMyAccuracyM()));
            NavigationHelper.showSnack(this, "Đã căn giữa vị trí của bạn");
        });

        // Kiểm tra phiên SOS đang mở khi bấm nút tạo SOS
        btnSos.setOnClickListener(v -> {
            SosRequest active = repo.getMyActiveUserSos();
            if (active != null) {
                NavigationHelper.showConfirmSheet(
                        this,
                        "YÊU CẦU ĐANG MỞ",
                        "Bạn đang có một yêu cầu SOS",
                        "Hệ thống đang xử lý yêu cầu cứu hộ hiện tại của bạn.\n\n"
                                + "Mã yêu cầu: " + active.getShortId() + " (" + active.getStatusLabel() + ")\n"
                                + "Sự cố: " + active.getEmergencyTypeLabel(),
                        "Theo dõi yêu cầu hiện tại",
                        false,
                        () -> {
                            repo.setLastViewedSos(active);
                            startActivity(new Intent(HomeActivity.this, TrackSosActivity.class));
                        },
                        "Tạo yêu cầu mới",
                        () -> startActivity(new Intent(HomeActivity.this, SendSosActivity.class))
                );
            } else {
                startActivity(new Intent(HomeActivity.this, SendSosActivity.class));
            }
        });

        btnNotifications.setOnClickListener(v -> NavigationHelper.showNotificationsDialog(this));
        btnProfile.setOnClickListener(v ->
                NavigationHelper.showProfileDialog(this, this::updateHeaderAndActiveSosBanner));

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_requests) {
                SosRequest active = repo.getMyActiveUserSos();
                if (active != null) {
                    repo.setLastViewedSos(active);
                    startActivity(new Intent(HomeActivity.this, TrackSosActivity.class));
                } else {
                    startActivity(new Intent(HomeActivity.this, SosResultActivity.class));
                }
                return false;
            } else if (itemId == R.id.nav_alerts) {
                NavigationHelper.showNotificationsDialog(this);
                return false;
            } else if (itemId == R.id.nav_settings) {
                NavigationHelper.showProfileDialog(this, this::updateHeaderAndActiveSosBanner);
                return false;
            }
            return false;
        });
    }

    private void updateHeaderAndActiveSosBanner() {
        MockDataRepository repo = MockDataRepository.getInstance();
        tvUserGreeting.setText("Xin chào, " + repo.getCurrentFullName());

        SosRequest active = repo.getMyActiveUserSos();
        if (active != null) {
            cardMyActiveSos.setVisibility(View.VISIBLE);
            tvActiveSosSummary.setText(active.getShortId() + " • " + active.getEmergencyTypeLabel()
                    + " • " + active.getStatusLabel());
        } else {
            cardMyActiveSos.setVisibility(View.GONE);
        }
    }

    /**
     * Hiển thị các điểm cảnh báo cộng đồng trên bản đồ
     */
    private void renderPublicMapMarkers() {
        if (mapLibreMap == null) return;
        mapLibreMap.clear();

        MockDataRepository repo = MockDataRepository.getInstance();

        // 1. Vị trí GPS chính xác của bản thân
        Icon myIcon = MapHelper.createPinIcon(this, Color.parseColor("#111111"), "BẠN");
        mapLibreMap.addMarker(new MarkerOptions()
                .position(new LatLng(repo.getMyLat(), repo.getMyLng()))
                .title("Vị trí của bạn")
                .snippet(String.format(Locale.US, "%.6f, %.6f (±%.1fm)",
                        repo.getMyLat(), repo.getMyLng(), repo.getMyAccuracyM()))
                .icon(myIcon));

        // 2. Các điểm cảnh báo trên bản đồ cộng đồng
        List<SosRequest> publicList = repo.getPublicMapSosList(currentFilter);
        Icon publicSosIcon = MapHelper.createPinIcon(this, Color.parseColor("#E5383B"), "SOS");

        for (SosRequest s : publicList) {
            double areaLat = s.getAreaLat();
            double areaLng = s.getAreaLng();

            MapHelper.addFloodZonePolygon(mapLibreMap, areaLat, areaLng, 0.0042);

            mapLibreMap.addMarker(new MarkerOptions()
                    .position(new LatLng(areaLat, areaLng))
                    .title(s.getShortId() + " • " + s.getEmergencyTypeLabel())
                    .snippet(s.getDescription() + " • " + s.getCreatedAt())
                    .icon(publicSosIcon));
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
        updateHeaderAndActiveSosBanner();
        if (mapLibreMap != null) renderPublicMapMarkers();
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
