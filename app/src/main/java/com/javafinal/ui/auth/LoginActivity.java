package com.javafinal.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.ui.rescuer.RescuerHomeActivity;
import com.javafinal.ui.user.HomeActivity;
import com.javafinal.util.NavigationHelper;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private LinearLayout layoutStepPhone;
    private LinearLayout layoutStepOtp;
    private TextView tvStep;
    private TextView tvRoleHint;

    private TextInputEditText edtPhone;
    private TextInputEditText edtFullName;
    private TextView tvTelegramBadge;

    private TextView tvOtpSentTo;
    private TextView tvSelectedRoleSummary;
    private TextView tvSelectedUserSummary;
    private TextView tvAttemptsRemaining;
    private TextView tvCountdown;
    private MaterialButton btnResend;
    private EditText[] otpInputs;

    private String selectedRole = MockDataRepository.ROLE_USER;
    private String generatedOtp = "123456";
    private int attemptsLeft = 5;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialButton btnBack = findViewById(R.id.btnBack);
        tvStep = findViewById(R.id.tvStep);
        layoutStepPhone = findViewById(R.id.layoutStepPhone);
        layoutStepOtp = findViewById(R.id.layoutStepOtp);

        MaterialButtonToggleGroup toggleRole = findViewById(R.id.toggleRole);
        tvRoleHint = findViewById(R.id.tvRoleHint);
        edtPhone = findViewById(R.id.edtPhone);
        edtFullName = findViewById(R.id.edtFullName);
        tvTelegramBadge = findViewById(R.id.tvTelegramBadge);

        MaterialButton btnOpenTelegram = findViewById(R.id.btnOpenTelegram);
        MaterialButton btnDevLinkTelegram = findViewById(R.id.btnDevLinkTelegram);
        MaterialButton btnSendOtp = findViewById(R.id.btnSendOtp);

        tvOtpSentTo = findViewById(R.id.tvOtpSentTo);
        tvSelectedRoleSummary = findViewById(R.id.tvSelectedRoleSummary);
        tvSelectedUserSummary = findViewById(R.id.tvSelectedUserSummary);
        tvAttemptsRemaining = findViewById(R.id.tvAttemptsRemaining);
        tvCountdown = findViewById(R.id.tvCountdown);
        btnResend = findViewById(R.id.btnResend);
        MaterialButton btnChangePhone = findViewById(R.id.btnChangePhone);
        MaterialButton btnConfirmOtp = findViewById(R.id.btnConfirmOtp);

        otpInputs = new EditText[]{
                findViewById(R.id.edtOtp1),
                findViewById(R.id.edtOtp2),
                findViewById(R.id.edtOtp3),
                findViewById(R.id.edtOtp4),
                findViewById(R.id.edtOtp5),
                findViewById(R.id.edtOtp6)
        };
        setupOtpAutoAdvance();

        // Chọn vai trò TRƯỚC khi đăng nhập
        toggleRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnRoleRescuer) {
                selectedRole = MockDataRepository.ROLE_RESCUER;
                edtPhone.setText("0901112222");
                edtFullName.setText("Trần Cứu Hộ");
                tvRoleHint.setText("Chế độ Đội cứu hộ: Xem danh sách yêu cầu cứu hộ, tiếp nhận ca và điều phối di chuyển.");
            } else {
                selectedRole = MockDataRepository.ROLE_USER;
                edtPhone.setText("0901234567");
                edtFullName.setText("Nguyễn Văn An");
                tvRoleHint.setText("Chế độ Người dân: Xem bản đồ cảnh báo cộng đồng và gửi tín hiệu SOS khẩn cấp.");
            }
        });

        btnBack.setOnClickListener(v -> {
            if (layoutStepOtp.getVisibility() == View.VISIBLE) {
                showStepPhone();
            } else {
                NavigationHelper.showAllScreensMenu(this);
            }
        });

        btnOpenTelegram.setOnClickListener(v -> {
            MockDataRepository.getInstance().setTelegramLinked(true);
            tvTelegramBadge.setText("Đã liên kết");
            NavigationHelper.showConfirmSheet(
                    this,
                    "HƯỚNG DẪN",
                    "Liên kết Telegram",
                    "1. Mở @SosRescueBot trên ứng dụng Telegram\n"
                            + "2. Nhấn Bắt đầu (/start) và chọn 'Chia sẻ số điện thoại'\n"
                            + "3. Quay lại ứng dụng để nhận mã xác thực 6 chữ số.",
                    "Đã hiểu",
                    false,
                    null,
                    null,
                    null
            );
        });

        btnDevLinkTelegram.setOnClickListener(v -> {
            MockDataRepository.getInstance().setTelegramLinked(true);
            tvTelegramBadge.setText("Đã liên kết");
            NavigationHelper.showSnack(this, "Đã liên kết tài khoản Telegram thành công");
        });

        // Bước 1 -> Bước 2: Gửi yêu cầu OTP
        btnSendOtp.setOnClickListener(v -> {
            String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
            String fullName = edtFullName.getText() != null ? edtFullName.getText().toString().trim() : "";

            if (phone.length() < 9) {
                NavigationHelper.showSnack(this, "Vui lòng nhập số điện thoại hợp lệ");
                return;
            }
            if (fullName.isEmpty()) {
                fullName = MockDataRepository.ROLE_RESCUER.equals(selectedRole) ? "Trần Cứu Hộ" : "Nguyễn Văn An";
                edtFullName.setText(fullName);
            }

            generatedOtp = "123456";
            attemptsLeft = 5;
            showStepOtp(phone, fullName);
            fillMockOtp(generatedOtp);
            startAntiSpamCountdown();

            NavigationHelper.showSnack(this, "Mã xác thực OTP của bạn là: " + generatedOtp);
        });

        btnChangePhone.setOnClickListener(v -> showStepPhone());

        btnResend.setOnClickListener(v -> {
            generatedOtp = "123456";
            attemptsLeft = 5;
            tvAttemptsRemaining.setText("Mã xác thực có hiệu lực trong 5 phút • Còn 5 lần thử");
            fillMockOtp(generatedOtp);
            startAntiSpamCountdown();
            NavigationHelper.showSnack(this, "Đã gửi mã OTP mới: " + generatedOtp);
        });

        // Bước 2: Xác thực OTP & Đăng nhập thẳng vào vai trò đã chọn
        btnConfirmOtp.setOnClickListener(v -> verifyOtpAndEnterApp());
    }

    private void showStepPhone() {
        if (countDownTimer != null) countDownTimer.cancel();
        tvStep.setText("Bước 1 / 2");
        layoutStepOtp.setVisibility(View.GONE);
        layoutStepPhone.setVisibility(View.VISIBLE);
    }

    private void showStepOtp(String phone, String fullName) {
        tvStep.setText("Bước 2 / 2");
        layoutStepPhone.setVisibility(View.GONE);
        layoutStepOtp.setVisibility(View.VISIBLE);

        tvOtpSentTo.setText("Mã OTP 6 chữ số đã được gửi tới Telegram của " + phone + " (hiệu lực 5 phút).");
        tvSelectedRoleSummary.setText(MockDataRepository.ROLE_RESCUER.equals(selectedRole)
                ? "Đội cứu hộ"
                : "Người dân");
        tvSelectedUserSummary.setText(fullName + " • " + phone);
        tvAttemptsRemaining.setText("Mã xác thực có hiệu lực trong 5 phút • Còn 5 lần thử");
    }

    private void verifyOtpAndEnterApp() {
        StringBuilder sb = new StringBuilder();
        for (EditText input : otpInputs) {
            sb.append(input.getText().toString().trim());
        }
        String enteredOtp = sb.toString();

        if (enteredOtp.length() < 6) {
            NavigationHelper.showSnack(this, "Vui lòng nhập đủ 6 chữ số OTP");
            return;
        }

        if (!generatedOtp.equals(enteredOtp)) {
            attemptsLeft--;
            if (attemptsLeft <= 0) {
                tvAttemptsRemaining.setText("Đã nhập sai quá 5 lần. Vui lòng yêu cầu gửi lại mã mới.");
                NavigationHelper.showSnack(this, "Mã OTP đã hết lượt thử, vui lòng gửi lại mã mới");
                return;
            }
            tvAttemptsRemaining.setText("Mã OTP chưa chính xác • Còn " + attemptsLeft + " lần thử");
            NavigationHelper.showSnack(this, "Mã OTP chưa chính xác");
            return;
        }

        String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "0901234567";
        String fullName = edtFullName.getText() != null ? edtFullName.getText().toString().trim() : "Nguyễn Văn An";

        MockDataRepository repo = MockDataRepository.getInstance();
        repo.loginAsRole(selectedRole, phone, fullName);

        if (MockDataRepository.ROLE_RESCUER.equals(selectedRole)) {
            startActivity(new Intent(LoginActivity.this, RescuerHomeActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        }
        finish();
    }

    private void setupOtpAutoAdvance() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpInputs.length - 1) {
                        otpInputs[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            otpInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && otpInputs[index].getText().length() == 0
                        && index > 0) {
                    otpInputs[index - 1].requestFocus();
                    otpInputs[index - 1].setText("");
                    return true;
                }
                return false;
            });
        }
    }

    private void fillMockOtp(String code) {
        for (int i = 0; i < otpInputs.length && i < code.length(); i++) {
            otpInputs[i].setText(String.valueOf(code.charAt(i)));
        }
    }

    private void startAntiSpamCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        btnResend.setEnabled(false);
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000;
                tvCountdown.setText(String.format(Locale.getDefault(),
                        "Gửi lại mã sau %02d giây", sec));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("Có thể yêu cầu mã OTP mới");
                btnResend.setEnabled(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
