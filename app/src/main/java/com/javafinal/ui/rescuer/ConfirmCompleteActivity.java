package com.javafinal.ui.rescuer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.model.SosRequest;
import com.javafinal.ui.user.SosResultActivity;
import com.javafinal.util.NavigationHelper;

public class ConfirmCompleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_complete);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MockDataRepository repo = MockDataRepository.getInstance();
        String sosId = getIntent().getStringExtra("SOS_ID");
        SosRequest mission = repo.getSosById(sosId);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvSosCode = findViewById(R.id.tvSosCode);
        TextView tvEmergencyNature = findViewById(R.id.tvEmergencyNature);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvVictimStatus = findViewById(R.id.tvVictimStatus);
        TextView tvTeamSummary = findViewById(R.id.tvTeamSummary);

        MaterialCheckBox cbConfirmComplete = findViewById(R.id.cbConfirmComplete);
        MaterialButton btnCompleteMission = findViewById(R.id.btnCompleteMission);
        MaterialButton btnBackToScene = findViewById(R.id.btnBackToScene);

        tvSosCode.setText(mission.getShortId() + " • " + mission.getVisibilityLabel());
        tvEmergencyNature.setText("Loại sự cố: " + mission.getEmergencyTypeLabel());
        tvAddress.setText("Mô tả: " + mission.getDescription());
        tvVictimStatus.setText("Nạn nhân: " + mission.getRequesterName() + " • " + mission.getRequesterPhone());
        tvTeamSummary.setText("Đội thực hiện: " + repo.getRescuerTeamName() + " (" + repo.getRescuerFullName() + ")");

        cbConfirmComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnCompleteMission.setEnabled(isChecked);
        });

        btnCompleteMission.setOnClickListener(v -> {
            if (!cbConfirmComplete.isChecked()) {
                NavigationHelper.showSnack(this, "Vui lòng tích xác nhận hoàn thành ca cứu hộ");
                return;
            }

            repo.completeMission(mission);

            NavigationHelper.showConfirmSheet(
                    this,
                    "HOÀN TẤT",
                    "Đã hoàn thành ca cứu hộ",
                    "Ca cứu hộ " + mission.getShortId() + " đã được xác nhận hoàn thành an toàn.\n\n"
                            + "Hệ thống đã tự động ngắt chia sẻ vị trí trực tiếp và đưa đội trở về trạng thái sẵn sàng nhận ca mới.",
                    "Về danh sách cứu hộ",
                    false,
                    () -> {
                        Intent intent = new Intent(ConfirmCompleteActivity.this, RescuerHomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    },
                    "Xem kết quả",
                    () -> {
                        Intent intent = new Intent(ConfirmCompleteActivity.this, SosResultActivity.class);
                        startActivity(intent);
                        finish();
                    }
            );
        });

        btnBackToScene.setOnClickListener(v -> finish());
    }
}
