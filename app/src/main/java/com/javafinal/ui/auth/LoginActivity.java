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
import androidx.activity.OnBackPressedCallback;
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
    private TextInputEditText edtPhone;

    private TextView tvOtpSentTo;
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
        layoutStepPhone = findViewById(R.id.layoutStepPhone);
        layoutStepOtp = findViewById(R.id.layoutStepOtp);

        MaterialButtonToggleGroup toggleRole = findViewById(R.id.toggleRole);
        edtPhone = findViewById(R.id.edtPhone);
        MaterialButton btnSendOtp = findViewById(R.id.btnSendOtp);

        tvOtpSentTo = findViewById(R.id.tvOtpSentTo);
        tvCountdown = findViewById(R.id.tvCountdown);
        btnResend = findViewById(R.id.btnResend);
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

        toggleRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnRoleRescuer) {
                selectedRole = MockDataRepository.ROLE_RESCUER;
                edtPhone.setText("0901112222");
            } else {
                selectedRole = MockDataRepository.ROLE_USER;
                edtPhone.setText("0901234567");
            }
        });

        btnBack.setOnClickListener(v -> showStepPhone());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutStepOtp.getVisibility() == View.VISIBLE) {
                    showStepPhone();
                } else {
                    finish();
                }
            }
        });

        btnSendOtp.setOnClickListener(v -> {
            String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
            if (phone.length() < 9) {
                NavigationHelper.showSnack(this, "Vui lòng nhập số điện thoại hợp lệ");
                return;
            }

            generatedOtp = "123456";
            attemptsLeft = 5;
            showStepOtp(phone);
            fillOtp(generatedOtp);
            startCountdown();
        });

        btnResend.setOnClickListener(v -> {
            generatedOtp = "123456";
            attemptsLeft = 5;
            fillOtp(generatedOtp);
            startCountdown();
            NavigationHelper.showSnack(this, "Đã gửi lại mã OTP");
        });

        btnConfirmOtp.setOnClickListener(v -> verifyOtpAndEnterApp());
    }

    private void showStepPhone() {
        if (countDownTimer != null) countDownTimer.cancel();
        layoutStepOtp.setVisibility(View.GONE);
        layoutStepPhone.setVisibility(View.VISIBLE);
    }

    private void showStepOtp(String phone) {
        layoutStepPhone.setVisibility(View.GONE);
        layoutStepOtp.setVisibility(View.VISIBLE);
        tvOtpSentTo.setText("Mã 6 số đã gửi tới " + phone);
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
                NavigationHelper.showSnack(this, "Đã quá số lần thử, vui lòng gửi lại mã");
                return;
            }
            NavigationHelper.showSnack(this, "Mã OTP không đúng (còn " + attemptsLeft + " lần thử)");
            return;
        }

        String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
        String defaultName = MockDataRepository.ROLE_RESCUER.equals(selectedRole) ? "Trần Cứu Hộ" : "Nguyễn Văn An";

        MockDataRepository repo = MockDataRepository.getInstance();
        repo.loginAsRole(selectedRole, phone, defaultName);

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

    private void fillOtp(String code) {
        for (int i = 0; i < otpInputs.length && i < code.length(); i++) {
            otpInputs[i].setText(String.valueOf(code.charAt(i)));
        }
    }

    private void startCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        btnResend.setEnabled(false);
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000;
                tvCountdown.setText(String.format(Locale.getDefault(),
                        "Gửi lại sau %02ds", sec));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("Có thể gửi lại mã");
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
