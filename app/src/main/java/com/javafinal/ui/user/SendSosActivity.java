package com.javafinal.ui.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
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

public class SendSosActivity extends AppCompatActivity {

    private MapView mapPreview;
    private MapLibreMap mapLibreMap;

    private TextView tvCoordinates;
    private TextView tvAccuracy;
    private TextInputEditText edtDescription;
    private ChipGroup chipGroupType;
    private RadioGroup rgPrivacy;

    private double selectedLat;
    private double selectedLng;
    private double selectedAccuracy = 4.5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        MapHelper.initMapLibre(this);
        setContentView(R.layout.activity_send_sos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MockDataRepository repo = MockDataRepository.getInstance();
        selectedLat = repo.getMyLat();
        selectedLng = repo.getMyLng();
        selectedAccuracy = repo.getMyAccuracyM();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvCoordinates = findViewById(R.id.tvCoordinates);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        MaterialButton btnRefreshGps = findViewById(R.id.btnRefreshGps);

        mapPreview = findViewById(R.id.mapPreview);
        mapPreview.onCreate(savedInstanceState);
        MapHelper.enableScrollViewTouch(mapPreview);

        updateCoordinatesUi();

        mapPreview.getMapAsync(map -> {
            this.mapLibreMap = map;
            MapHelper.applyDefaultStyle(map, style -> {
                MapHelper.moveCamera(map, selectedLat, selectedLng, 15.5);
                renderPinOnPreview();
            });

            map.addOnMapClickListener(point -> {
                selectedLat = point.getLatitude();
                selectedLng = point.getLongitude();
                selectedAccuracy = 3.2;
                repo.setMyLocation(selectedLat, selectedLng, selectedAccuracy);
                updateCoordinatesUi();
                renderPinOnPreview();
                NavigationHelper.showSnack(this, "Đã cập nhật tọa độ ghim trên bản đồ");
                return true;
            });
        });

        btnRefreshGps.setOnClickListener(v -> {
            selectedLat = repo.getMyLat() + (Math.random() - 0.5) * 0.0005;
            selectedLng = repo.getMyLng() + (Math.random() - 0.5) * 0.0005;
            selectedAccuracy = 3.8;
            repo.setMyLocation(selectedLat, selectedLng, selectedAccuracy);
            updateCoordinatesUi();
            if (mapLibreMap != null) {
                MapHelper.moveCamera(mapLibreMap, selectedLat, selectedLng, 16.0);
                renderPinOnPreview();
            }
            NavigationHelper.showSnack(this, "Đã làm mới tọa độ GPS hiện tại");
        });

        chipGroupType = findViewById(R.id.chipGroupType);
        edtDescription = findViewById(R.id.edtDescription);
        rgPrivacy = findViewById(R.id.rgPrivacy);

        ChipGroup chipGroupQuickTags = findViewById(R.id.chipGroupQuickTags);
        for (int i = 0; i < chipGroupQuickTags.getChildCount(); i++) {
            View child = chipGroupQuickTags.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnClickListener(v -> {
                    String tag = chip.getText().toString().replace("+ ", "").trim();
                    String current = edtDescription.getText() != null
                            ? edtDescription.getText().toString().trim()
                            : "";
                    if (current.isEmpty()) {
                        edtDescription.setText(tag);
                    } else if (!current.contains(tag)) {
                        edtDescription.setText(current + ", " + tag.toLowerCase(Locale.ROOT));
                    }
                    if (edtDescription.getText() != null) {
                        edtDescription.setSelection(edtDescription.getText().length());
                    }
                });
            }
        }

        findViewById(R.id.btnCall112).setOnClickListener(v -> NavigationHelper.dialPhone(this, "112"));
        findViewById(R.id.btnCall114).setOnClickListener(v -> NavigationHelper.dialPhone(this, "114"));
        findViewById(R.id.btnCall115).setOnClickListener(v -> NavigationHelper.dialPhone(this, "115"));
        findViewById(R.id.btnCall113).setOnClickListener(v -> NavigationHelper.dialPhone(this, "113"));

        MaterialButton btnSubmitSos = findViewById(R.id.btnSubmitSos);
        btnSubmitSos.setOnClickListener(v -> submitSosRequest());
    }

    private void updateCoordinatesUi() {
        tvCoordinates.setText(String.format(Locale.US, "%.6f, %.6f", selectedLat, selectedLng));
        tvAccuracy.setText(String.format(Locale.US, "±%.1fm", selectedAccuracy));
    }

    private void renderPinOnPreview() {
        if (mapLibreMap == null) return;
        mapLibreMap.clear();
        Icon sosPin = MapHelper.createPinIcon(this, Color.parseColor("#E5383B"), "SOS");
        mapLibreMap.addMarker(new MarkerOptions()
                .position(new LatLng(selectedLat, selectedLng))
                .title("Vị trí cần cứu hộ")
                .snippet(String.format(Locale.US, "%.6f, %.6f", selectedLat, selectedLng))
                .icon(sosPin));
    }

    private void submitSosRequest() {
        if (selectedLat < -90 || selectedLat > 90 || selectedLng < -180 || selectedLng > 180) {
            NavigationHelper.showSnack(this, "Tọa độ vị trí không hợp lệ");
            return;
        }

        int checkedId = chipGroupType.getCheckedChipId();
        String emergencyType = SosRequest.TYPE_NGAP_LUT;
        if (checkedId == R.id.chipMedical) {
            emergencyType = SosRequest.TYPE_Y_TE;
        } else if (checkedId == R.id.chipCollapse) {
            emergencyType = SosRequest.TYPE_MAC_KET;
        } else if (checkedId == R.id.chipFood) {
            emergencyType = SosRequest.TYPE_NHU_YEU_PHAM;
        } else if (checkedId == R.id.chipFire) {
            emergencyType = SosRequest.TYPE_CHAY_NO;
        }

        String desc = edtDescription.getText() != null ? edtDescription.getText().toString().trim() : "";
        if (desc.isEmpty()) {
            NavigationHelper.showSnack(this, "Vui lòng nhập mô tả tình trạng khẩn cấp");
            return;
        }

        String visibility = rgPrivacy.getCheckedRadioButtonId() == R.id.rbPrivate
                ? SosRequest.VISIBILITY_PRIVATE
                : SosRequest.VISIBILITY_PUBLIC;

        MockDataRepository repo = MockDataRepository.getInstance();
        repo.createNewSos(emergencyType, desc, visibility, selectedLat, selectedLng);

        Intent intent = new Intent(SendSosActivity.this, TrackSosActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapPreview != null) mapPreview.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapPreview != null) mapPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapPreview != null) mapPreview.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapPreview != null) mapPreview.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapPreview != null) mapPreview.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapPreview != null) mapPreview.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapPreview != null) mapPreview.onSaveInstanceState(outState);
    }
}
