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

        MaterialCheckBox cbConfirmComplete = findViewById(R.id.cbConfirmComplete);
        MaterialButton btnCompleteMission = findViewById(R.id.btnCompleteMission);

        tvSosCode.setText("SOS " + mission.getShortId());
        tvEmergencyNature.setText(mission.getEmergencyTypeLabel());
        tvVictimStatus.setText(mission.getRequesterName() + " • " + mission.getRequesterPhone());
        tvAddress.setText(mission.getDescription());

        cbConfirmComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnCompleteMission.setEnabled(isChecked);
        });

        btnCompleteMission.setOnClickListener(v -> {
            if (!cbConfirmComplete.isChecked()) {
                NavigationHelper.showSnack(this, "Vui lòng xác nhận đã hỗ trợ nạn nhân an toàn");
                return;
            }

            repo.completeMission(mission);
            Intent intent = new Intent(ConfirmCompleteActivity.this, RescuerHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
