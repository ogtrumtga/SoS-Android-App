package com.javafinal.ui.user;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.model.SosRequest;
import com.javafinal.util.NavigationHelper;

import java.util.List;

public class SosResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sos_result);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MockDataRepository repo = MockDataRepository.getInstance();
        SosRequest sos = repo.getLastViewedSos();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> navigateToHome());

        ImageView imgResult = findViewById(R.id.imgResult);
        TextView tvStatusBadge = findViewById(R.id.tvStatusBadge);
        TextView tvResultTitle = findViewById(R.id.tvResultTitle);
        TextView tvResultSubtitle = findViewById(R.id.tvResultSubtitle);

        TextView tvSosCode = findViewById(R.id.tvSosCode);
        TextView tvClosedAt = findViewById(R.id.tvClosedAt);
        TextView tvDuration = findViewById(R.id.tvDuration);
        TextView tvRescueTeam = findViewById(R.id.tvRescueTeam);
        TextView tvEvacuated = findViewById(R.id.tvEvacuated);
        TextView tvShelter = findViewById(R.id.tvShelter);
        TextView tvAuditLogs = findViewById(R.id.tvAuditLogs);

        MaterialButton btnCopyCode = findViewById(R.id.btnCopyCode);
        MaterialButton btnBackToMap = findViewById(R.id.btnBackToMap);
        MaterialButton btnNotifyFamily = findViewById(R.id.btnNotifyFamily);

        boolean isCancelled = SosRequest.STATUS_CANCELLED.equals(sos.getStatus());

        if (isCancelled) {
            imgResult.setImageResource(R.drawable.ic_close);
            imgResult.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.amber_soft)));
            imgResult.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.amber)));
            tvStatusBadge.setText("ĐÃ HỦY YÊU CẦU");
            tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.amber_soft)));
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.amber));

            tvResultTitle.setText("Đã hủy yêu cầu SOS");
            tvResultSubtitle.setText("Phiên cứu hộ đã đóng theo yêu cầu của bạn. Đã dừng chia sẻ vị trí GPS.");
            tvDuration.setText("Người gửi đã hủy");
        } else {
            imgResult.setImageResource(R.drawable.ic_check);
            imgResult.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.green_soft)));
            imgResult.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.green)));
            tvStatusBadge.setText("ĐÃ HOÀN THÀNH");
            tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.green_soft)));
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.green));

            tvResultTitle.setText("Phiên cứu hộ đã hoàn thành");
            tvResultSubtitle.setText("Đội cứu hộ đã hỗ trợ bạn đến nơi an toàn và hoàn tất ca cứu hộ.");
            tvDuration.setText("Đội cứu hộ hoàn tất");
        }

        tvSosCode.setText(sos.getShortId());
        tvClosedAt.setText(sos.getCreatedAt());
        tvRescueTeam.setText(sos.getTeamName() != null
                ? "Đội hỗ trợ: " + sos.getTeamName() + " (" + sos.getRescuerName() + ")"
                : "Đội hỗ trợ: Chưa có đội tiếp nhận trước khi hủy");
        tvEvacuated.setText("Loại sự cố: " + sos.getEmergencyTypeLabel());
        tvShelter.setText("Mô tả: " + sos.getDescription());

        List<String> logs = sos.getLogs();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < logs.size(); i++) {
            sb.append("• ").append(logs.get(i));
            if (i < logs.size() - 1) sb.append("\n");
        }
        tvAuditLogs.setText(sb.toString());

        btnCopyCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("sos_id", sos.getShortId()));
                NavigationHelper.showSnack(this, "Đã sao chép mã yêu cầu: " + sos.getShortId());
            }
        });

        btnBackToMap.setOnClickListener(v -> navigateToHome());

        btnNotifyFamily.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,
                    "[SOS RESCUE] Tôi đã an toàn! Phiên cứu hộ " + sos.getShortId()
                            + " đã kết thúc (" + sos.getStatusLabel() + ").");
            startActivity(Intent.createChooser(share, "Chia sẻ thông báo an toàn"));
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(SosResultActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
