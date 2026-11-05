package com.javafinal.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.javafinal.R;
import com.javafinal.ui.auth.LoginActivity;
import com.javafinal.util.MapHelper;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        MapHelper.initMapLibre(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvSplashStatus = findViewById(R.id.tvSplashStatus);
        TextView tvSplashSubStatus = findViewById(R.id.tvSplashSubStatus);
        LinearProgressIndicator progressLinear = findViewById(R.id.progressLinear);

        handler.postDelayed(() -> {
            if (progressLinear != null) {
                progressLinear.setProgressCompat(72, true);
            }
            if (tvSplashStatus != null) {
                tvSplashStatus.setText("Đang xác thực phiên làm việc...");
            }
            if (tvSplashSubStatus != null) {
                tvSplashSubStatus.setText("Đang tải dữ liệu bản đồ và vị trí");
            }
        }, 650);

        handler.postDelayed(() -> {
            if (progressLinear != null) {
                progressLinear.setProgressCompat(100, true);
            }
            if (tvSplashStatus != null) {
                tvSplashStatus.setText("Hệ thống đã sẵn sàng");
            }
        }, 1250);

        handler.postDelayed(this::goToLogin, 1650);

        findViewById(R.id.main).setOnClickListener(v -> goToLogin());
    }

    private void goToLogin() {
        if (navigated) return;
        navigated = true;
        handler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
