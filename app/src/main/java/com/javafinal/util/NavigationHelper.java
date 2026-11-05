package com.javafinal.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.ui.auth.LoginActivity;
import com.javafinal.ui.rescuer.ConfirmCompleteActivity;
import com.javafinal.ui.rescuer.RealtimeTrackingActivity;
import com.javafinal.ui.rescuer.RescuerHomeActivity;
import com.javafinal.ui.rescuer.SosDetailActivity;
import com.javafinal.ui.splash.SplashActivity;
import com.javafinal.ui.user.HomeActivity;
import com.javafinal.ui.user.SendSosActivity;
import com.javafinal.ui.user.SosResultActivity;
import com.javafinal.ui.user.TrackSosActivity;

import java.util.List;

public class NavigationHelper {

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    /**
     * Hiển thị thông báo ngắn (Snackbar tối giản thay cho Toast mặc định).
     */
    public static void showSnack(Activity activity, String message) {
        if (activity == null || activity.isFinishing()) return;
        View root = activity.findViewById(R.id.main);
        if (root == null) {
            root = activity.findViewById(android.R.id.content);
        }
        if (root != null) {
            Snackbar sb = Snackbar.make(root, message, Snackbar.LENGTH_SHORT);
            sb.setBackgroundTint(ContextCompat.getColor(activity, R.color.ink));
            sb.setTextColor(ContextCompat.getColor(activity, R.color.white));
            sb.show();
        } else {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Tạo khung BottomSheet chuẩn phong cách tối giản với thanh kéo ở đầu.
     */
    public static LinearLayout createSheetRoot(Context context, String badgeText, String titleText, String subtitleText) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 24), dp(context, 12), dp(context, 24), dp(context, 24));

        View handle = new View(context);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(context, 36), dp(context, 4));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = dp(context, 18);
        handle.setLayoutParams(handleLp);
        handle.setBackgroundResource(R.drawable.bg_handle);
        root.addView(handle);

        if (badgeText != null && !badgeText.isEmpty()) {
            TextView badge = new TextView(context);
            badge.setText(badgeText);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            badge.setTextColor(ContextCompat.getColor(context, R.color.ink_secondary));
            badge.setBackgroundResource(R.drawable.bg_pill);
            badge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fill)));
            badge.setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4));
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeLp.bottomMargin = dp(context, 8);
            badge.setLayoutParams(badgeLp);
            root.addView(badge);
        }

        if (titleText != null && !titleText.isEmpty()) {
            TextView title = new TextView(context);
            title.setText(titleText);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setTextColor(ContextCompat.getColor(context, R.color.ink));
            root.addView(title);
        }

        if (subtitleText != null && !subtitleText.isEmpty()) {
            TextView sub = new TextView(context);
            sub.setText(subtitleText);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            sub.setTextColor(ContextCompat.getColor(context, R.color.ink_secondary));
            sub.setLineSpacing(dp(context, 2), 1f);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = dp(context, 6);
            sub.setLayoutParams(subLp);
            root.addView(sub);
        }

        return root;
    }

    public static void addKeyValueRow(Context context, LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(context, 8), 0, dp(context, 8));

        TextView tvLabel = new TextView(context);
        tvLabel.setText(label);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvLabel.setTextColor(ContextCompat.getColor(context, R.color.ink_secondary));
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(lpLabel);

        TextView tvValue = new TextView(context);
        tvValue.setText(value);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvValue.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvValue.setTextColor(ContextCompat.getColor(context, R.color.ink));
        tvValue.setGravity(Gravity.END);

        row.addView(tvLabel);
        row.addView(tvValue);
        parent.addView(row);
    }

    public static MaterialButton createSheetButton(Context context, String text, boolean isPrimary, boolean isDanger) {
        int styleAttr = isPrimary
                ? com.google.android.material.R.attr.materialButtonStyle
                : com.google.android.material.R.attr.materialButtonOutlinedStyle;
        MaterialButton btn = new MaterialButton(context, null, styleAttr);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setCornerRadius(dp(context, 14));
        btn.setMinHeight(dp(context, 50));
        btn.setInsetTop(0);
        btn.setInsetBottom(0);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (isPrimary) {
            int bg = ContextCompat.getColor(context, isDanger ? R.color.red : R.color.ink);
            btn.setBackgroundTintList(ColorStateList.valueOf(bg));
            btn.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
            btn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context,
                    isDanger ? R.color.red : R.color.line_strong)));
            btn.setStrokeWidth(dp(context, 1));
            btn.setTextColor(ContextCompat.getColor(context, isDanger ? R.color.red : R.color.ink));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(context, 10);
        btn.setLayoutParams(lp);
        return btn;
    }

    /**
     * Hộp thoại xác nhận / cảnh báo dạng BottomSheet tối giản thay thế hoàn toàn AlertDialog.
     */
    public static void showConfirmSheet(
            Activity activity,
            String badge,
            String title,
            String message,
            String primaryText,
            boolean isDanger,
            Runnable onPrimary,
            String secondaryText,
            Runnable onSecondary
    ) {
        if (activity == null || activity.isFinishing()) return;
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        LinearLayout root = createSheetRoot(activity, badge, title, message);

        View spacer = new View(activity);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 8)));
        root.addView(spacer);

        if (primaryText != null) {
            MaterialButton btnPrimary = createSheetButton(activity, primaryText, true, isDanger);
            btnPrimary.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPrimary != null) onPrimary.run();
            });
            root.addView(btnPrimary);
        }

        if (secondaryText != null) {
            MaterialButton btnSecondary = createSheetButton(activity, secondaryText, false, false);
            btnSecondary.setOnClickListener(v -> {
                dialog.dismiss();
                if (onSecondary != null) onSecondary.run();
            });
            root.addView(btnSecondary);
        }

        dialog.setContentView(root);
        dialog.show();
    }

    /**
     * Hộp thoại Hồ sơ người dùng chuẩn API #03 (/api/auth/me), #05 (GET /api/users/profile), #06 (PUT /api/users/profile)
     */
    public static void showProfileDialog(Activity activity, Runnable onProfileUpdated) {
        if (activity == null || activity.isFinishing()) return;
        MockDataRepository repo = MockDataRepository.getInstance();
        BottomSheetDialog dialog = new BottomSheetDialog(activity);

        LinearLayout root = createSheetRoot(
                activity,
                "TÀI KHOẢN",
                repo.getCurrentFullName(),
                "Thông tin tài khoản đang đăng nhập."
        );

        LinearLayout infoBox = new LinearLayout(activity);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        infoBox.setBackgroundResource(R.drawable.bg_box);
        infoBox.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxLp.topMargin = dp(activity, 16);
        boxLp.bottomMargin = dp(activity, 8);
        infoBox.setLayoutParams(boxLp);

        addKeyValueRow(activity, infoBox, "Số điện thoại", repo.getCurrentPhone());
        addKeyValueRow(activity, infoBox, "Vai trò",
                MockDataRepository.ROLE_RESCUER.equals(repo.getCurrentRole()) ? "Đội cứu hộ" : "Người dân");
        addKeyValueRow(activity, infoBox, "Trạng thái", "Đang hoạt động");
        if (MockDataRepository.ROLE_RESCUER.equals(repo.getCurrentRole())) {
            addKeyValueRow(activity, infoBox, "Đơn vị", repo.getRescuerTeamName());
        }
        root.addView(infoBox);

        MaterialButton btnEditName = createSheetButton(activity, "Đổi họ tên hiển thị", true, false);
        btnEditName.setOnClickListener(v -> {
            dialog.dismiss();
            showEditFullNameDialog(activity, onProfileUpdated);
        });
        root.addView(btnEditName);

        MaterialButton btnSwitchScreen = createSheetButton(activity, "Chuyển nhanh màn hình", false, false);
        btnSwitchScreen.setOnClickListener(v -> {
            dialog.dismiss();
            showAllScreensMenu(activity);
        });
        root.addView(btnSwitchScreen);

        MaterialButton btnLogout = createSheetButton(activity, "Đăng xuất / Đổi vai trò", false, true);
        btnLogout.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        });
        root.addView(btnLogout);

        dialog.setContentView(root);
        dialog.show();
    }

    /**
     * Hộp thoại thông tin đội cứu hộ
     */
    public static void showRescueTeamSheet(Activity activity, Runnable onProfileUpdated) {
        if (activity == null || activity.isFinishing()) return;
        MockDataRepository repo = MockDataRepository.getInstance();
        BottomSheetDialog dialog = new BottomSheetDialog(activity);

        LinearLayout root = createSheetRoot(
                activity,
                "ĐƠN VỊ CỨU HỘ",
                repo.getRescuerTeamName(),
                "Thông tin đơn vị trực chiến và vị trí đóng quân hiện tại."
        );

        LinearLayout infoBox = new LinearLayout(activity);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        infoBox.setBackgroundResource(R.drawable.bg_box);
        infoBox.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxLp.topMargin = dp(activity, 16);
        boxLp.bottomMargin = dp(activity, 8);
        infoBox.setLayoutParams(boxLp);

        addKeyValueRow(activity, infoBox, "Thành viên trực", repo.getRescuerFullName());
        addKeyValueRow(activity, infoBox, "Điện thoại", repo.getRescuerPhone());
        addKeyValueRow(activity, infoBox, "Tọa độ trực ban",
                repo.getRescuerBaseLat() + ", " + repo.getRescuerBaseLng());
        addKeyValueRow(activity, infoBox, "Trạng thái trực", "Tối đa 1 ca cùng lúc");
        root.addView(infoBox);

        MaterialButton btnProfile = createSheetButton(activity, "Mở hồ sơ tài khoản", false, false);
        btnProfile.setOnClickListener(v -> {
            dialog.dismiss();
            showProfileDialog(activity, onProfileUpdated);
        });
        root.addView(btnProfile);

        MaterialButton btnClose = createSheetButton(activity, "Đóng", true, false);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.setContentView(root);
        dialog.show();
    }

    private static void showEditFullNameDialog(Activity activity, Runnable onProfileUpdated) {
        MockDataRepository repo = MockDataRepository.getInstance();
        BottomSheetDialog dialog = new BottomSheetDialog(activity);

        LinearLayout root = createSheetRoot(
                activity,
                "HỒ SƠ CÁ NHÂN",
                "Cập nhật họ và tên",
                "Nhập họ và tên hiển thị mới của bạn."
        );

        TextInputLayout til = new TextInputLayout(activity, null, com.google.android.material.R.attr.textInputStyle);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        float r = dp(activity, 12);
        til.setBoxCornerRadii(r, r, r, r);
        til.setHint("Họ và tên");
        LinearLayout.LayoutParams tilLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tilLp.topMargin = dp(activity, 16);
        tilLp.bottomMargin = dp(activity, 8);
        til.setLayoutParams(tilLp);

        TextInputEditText input = new TextInputEditText(til.getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(repo.getCurrentFullName());
        til.addView(input);
        root.addView(til);

        MaterialButton btnSave = createSheetButton(activity, "Lưu thay đổi", true, false);
        btnSave.setOnClickListener(v -> {
            String newName = input.getText() != null ? input.getText().toString().trim() : "";
            if (!newName.isEmpty()) {
                repo.updateProfileFullName(newName);
                dialog.dismiss();
                showSnack(activity, "Đã cập nhật họ tên: " + newName);
                if (onProfileUpdated != null) onProfileUpdated.run();
            }
        });
        root.addView(btnSave);

        MaterialButton btnCancel = createSheetButton(activity, "Hủy", false, false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnCancel);

        dialog.setContentView(root);
        dialog.show();
    }

    /**
     * Hộp thoại Thông báo
     */
    public static void showNotificationsDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        List<String> notifs = MockDataRepository.getInstance().getNotifications();
        BottomSheetDialog dialog = new BottomSheetDialog(activity);

        LinearLayout root = createSheetRoot(
                activity,
                "THÔNG BÁO",
                "Thông báo hệ thống",
                "Cập nhật mới nhất về các yêu cầu cứu hộ của bạn."
        );

        ScrollView scroll = new ScrollView(activity);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollLp.topMargin = dp(activity, 14);
        scrollLp.bottomMargin = dp(activity, 8);
        scroll.setLayoutParams(scrollLp);

        LinearLayout listContainer = new LinearLayout(activity);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setBackgroundResource(R.drawable.bg_box);
        listContainer.setPadding(dp(activity, 16), dp(activity, 6), dp(activity, 16), dp(activity, 6));

        for (int i = 0; i < notifs.size(); i++) {
            TextView tv = new TextView(activity);
            tv.setText(notifs.get(i));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            tv.setTextColor(ContextCompat.getColor(activity, R.color.ink));
            tv.setLineSpacing(dp(activity, 2), 1f);
            tv.setPadding(0, dp(activity, 10), 0, dp(activity, 10));
            listContainer.addView(tv);

            if (i < notifs.size() - 1) {
                View divider = new View(activity);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 1)));
                divider.setBackgroundColor(ContextCompat.getColor(activity, R.color.line));
                listContainer.addView(divider);
            }
        }
        scroll.addView(listContainer);
        root.addView(scroll);

        MaterialButton btnReadAll = createSheetButton(
                activity, "Đánh dấu tất cả đã đọc", true, false);
        btnReadAll.setOnClickListener(v -> {
            dialog.dismiss();
            showSnack(activity, "Đã đánh dấu đã đọc tất cả thông báo");
        });
        root.addView(btnReadAll);

        MaterialButton btnClose = createSheetButton(activity, "Đóng", false, false);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.setContentView(root);
        dialog.show();
    }

    public static void showAllScreensMenu(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        String[][] screens = new String[][]{
                {"01", "Màn hình khởi động", "Kiểm tra kết nối & đồng bộ hệ thống"},
                {"02", "Đăng nhập & Xác thực OTP", "Chọn vai trò và xác minh qua Telegram"},
                {"03", "Trang chủ Người dân", "Bản đồ cảnh báo cộng đồng & nút SOS"},
                {"04", "Gửi tín hiệu SOS", "Chia sẻ vị trí, loại sự cố và mô tả"},
                {"05", "Theo dõi tiến trình cứu hộ", "Bản đồ trực tiếp & trạng thái đội cứu hộ"},
                {"06", "Kết quả phiên cứu hộ", "Tóm tắt ca cứu hộ & nhật ký hoạt động"},
                {"07", "Trang chủ Đội cứu hộ", "Danh sách yêu cầu cứu hộ đang chờ"},
                {"08", "Chi tiết yêu cầu SOS", "Thông tin hiện trường & liên hệ người gửi"},
                {"09", "Bản đồ điều phối trực tiếp", "Lộ trình tiếp cận & cập nhật tiến trình"},
                {"10", "Xác nhận hoàn thành ca", "Đóng phiên cứu hộ khi nạn nhân an toàn"}
        };

        Class<?>[] targets = new Class<?>[]{
                SplashActivity.class,
                LoginActivity.class,
                HomeActivity.class,
                SendSosActivity.class,
                TrackSosActivity.class,
                SosResultActivity.class,
                RescuerHomeActivity.class,
                SosDetailActivity.class,
                RealtimeTrackingActivity.class,
                ConfirmCompleteActivity.class
        };

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        LinearLayout root = createSheetRoot(
                activity,
                "ĐIỀU HƯỚNG NHANH",
                "Danh sách màn hình",
                "Chọn màn hình bất kỳ để xem nhanh giao diện."
        );

        ScrollView scroll = new ScrollView(activity);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 360));
        scrollLp.topMargin = dp(activity, 12);
        scroll.setLayoutParams(scrollLp);

        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < screens.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
            TypedValue outValue = new TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            row.setBackgroundResource(outValue.resourceId);

            TextView badge = new TextView(activity);
            badge.setText(screens[i][0]);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            badge.setTextColor(ContextCompat.getColor(activity, R.color.ink));
            badge.setBackgroundResource(R.drawable.bg_box);
            badge.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(activity, 36), dp(activity, 36));
            badgeLp.rightMargin = dp(activity, 12);
            badge.setLayoutParams(badgeLp);
            row.addView(badge);

            LinearLayout textCol = new LinearLayout(activity);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView t1 = new TextView(activity);
            t1.setText(screens[i][1]);
            t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            t1.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            t1.setTextColor(ContextCompat.getColor(activity, R.color.ink));
            textCol.addView(t1);

            TextView t2 = new TextView(activity);
            t2.setText(screens[i][2]);
            t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            t2.setTextColor(ContextCompat.getColor(activity, R.color.ink_tertiary));
            textCol.addView(t2);

            row.addView(textCol);

            ImageView arrow = new ImageView(activity);
            arrow.setImageResource(R.drawable.ic_chevron_right);
            arrow.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.ink_tertiary)));
            arrow.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 20), dp(activity, 20)));
            row.addView(arrow);

            row.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(activity, targets[index]);
                activity.startActivity(intent);
            });
            list.addView(row);

            if (i < screens.length - 1) {
                View div = new View(activity);
                div.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 1)));
                div.setBackgroundColor(ContextCompat.getColor(activity, R.color.line));
                list.addView(div);
            }
        }

        scroll.addView(list);
        root.addView(scroll);

        MaterialButton btnClose = createSheetButton(activity, "Đóng", false, false);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.setContentView(root);
        dialog.show();
    }

    public static void dialPhone(Context context, String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            context.startActivity(intent);
        } catch (Exception e) {
            if (context instanceof Activity) {
                showSnack((Activity) context, "Đang gọi: " + phoneNumber);
            }
        }
    }

    public static void sendSms(Context context, String phoneNumber, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phoneNumber));
            intent.putExtra("sms_body", message);
            context.startActivity(intent);
        } catch (Exception e) {
            if (context instanceof Activity) {
                showSnack((Activity) context, "Gửi SMS tới " + phoneNumber);
            }
        }
    }
}
