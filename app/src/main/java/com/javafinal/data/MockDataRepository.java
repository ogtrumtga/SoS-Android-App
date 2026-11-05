package com.javafinal.data;

import com.javafinal.model.SosRequest;
import com.javafinal.util.MapHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Kho dữ liệu giả lập (Mock Data) mô phỏng đầy đủ 8 bảng PostgreSQL và 27 API
 * trong tài liệu BANG_API_VA_LUONG_HOAT_DONG.md.
 */
public class MockDataRepository {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_RESCUER = "RESCUER";

    private static MockDataRepository instance;

    // Thông tin tài khoản đang đăng nhập (/api/auth/me & /api/users/profile)
    private String currentUserId = "f4139613-5ddd-4d6a-888c-f79629762632";
    private String currentFullName = "Nguyễn Văn An";
    private String currentPhone = "0901234567";
    private String currentRole = ROLE_USER;
    private String accountStatus = "ACTIVE";
    private boolean isValid = true;
    private boolean telegramLinked = true;

    // Thông tin đội cứu hộ khi đăng nhập bằng RESCUER (/api/rescuer/teams)
    private final String rescuerUserId = "11111111-1111-1111-1111-111111111111";
    private String rescuerFullName = "Trần Cứu Hộ";
    private String rescuerPhone = "0901112222";
    private final String rescuerTeamId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private final String rescuerTeamName = "Đội Cứu Hộ Phản Ứng Nhanh Quận 1";

    // Vị trí GPS hiện tại của thiết bị (Lê Lợi - Pasteur, Quận 1)
    private double myLat = 10.774560;
    private double myLng = 106.700420;
    private double myAccuracyM = 4.5;

    // Trụ sở Đội cứu hộ (Trần Hưng Đạo - Nguyễn Thái Học, Quận 1)
    private double rescuerBaseLat = 10.768020;
    private double rescuerBaseLng = 106.695360;
    private double rescuerAccuracyM = 8.0;

    private final List<SosRequest> allSosList = new ArrayList<>();
    private final List<String> notifications = new ArrayList<>();
    private SosRequest lastViewedSos;

    private MockDataRepository() {
        seedInitialData();
    }

    public static synchronized MockDataRepository getInstance() {
        if (instance == null) {
            instance = new MockDataRepository();
        }
        return instance;
    }

    private void seedInitialData() {
        allSosList.clear();
        notifications.clear();

        // 1. Ca mẫu chuẩn (Lê Thánh Tôn - Hai Bà Trưng, Quận 1)
        SosRequest sos1 = new SosRequest(
                "2cc295a6-5723-4085-8dd8-1967ab544d31",
                "f4139613-5ddd-4d6a-888c-f79629762632",
                "Nguyễn Văn An",
                "0901234567",
                SosRequest.TYPE_NGAP_LUT,
                "Nước ngập tầng 1, cần xuồng đưa 2 người già di chuyển",
                SosRequest.VISIBILITY_PUBLIC,
                10.778250,
                106.703420,
                4.5,
                SosRequest.STATUS_WAITING,
                "09:21 • Hôm nay"
        );
        sos1.setRoutePoints(MapHelper.computeStreetGraphRoute(
                rescuerBaseLat, rescuerBaseLng, 10.778250, 106.703420), false);
        sos1.addLog("09:21 • Đã phát tín hiệu cứu hộ");

        // 2. Ca cấp cứu y tế ở chế độ PRIVATE (Nguyễn Thị Minh Khai - Đinh Tiên Hoàng, Quận 1)
        SosRequest sos2 = new SosRequest(
                "8b9104e2-3a19-4c22-9104-78ef9012ab34",
                "c8192011-1122-4333-8444-556677889900",
                "Lê Thị Hồng Nhung",
                "0918223344",
                SosRequest.TYPE_Y_TE,
                "Người cao tuổi suy hô hấp cần hỗ trợ bình oxy, hẻm ngập 1.2m",
                SosRequest.VISIBILITY_PRIVATE,
                10.786850,
                106.701150,
                3.2,
                SosRequest.STATUS_WAITING,
                "09:15 • Hôm nay"
        );
        sos2.setRoutePoints(MapHelper.computeStreetGraphRoute(
                rescuerBaseLat, rescuerBaseLng, 10.786850, 106.701150), false);
        sos2.addLog("09:15 • Đã phát tín hiệu cứu hộ");

        // 3. Ca mắc kẹt do cây đổ / ngập (Cô Giang - Hồ Hảo Hớn, Quận 1)
        SosRequest sos3 = new SosRequest(
                "5d4412a9-7c88-4100-b222-334455667788",
                "d9991111-2222-3333-4444-555566667777",
                "Phạm Minh Tuấn",
                "0987654321",
                SosRequest.TYPE_MAC_KET,
                "Cây đổ chắn cửa tầng trệt, nước đang dâng cao, có 3 người bên trong",
                SosRequest.VISIBILITY_PUBLIC,
                10.763250,
                106.692850,
                5.0,
                SosRequest.STATUS_WAITING,
                "09:05 • Hôm nay"
        );
        sos3.setRoutePoints(MapHelper.computeStreetGraphRoute(
                rescuerBaseLat, rescuerBaseLng, 10.763250, 106.692850), false);
        sos3.addLog("09:05 • Đã phát tín hiệu cứu hộ");

        // 4. Ca thiếu nhu yếu phẩm & nước uống (Nguyễn Thị Minh Khai - Cống Quỳnh, Quận 1)
        SosRequest sos4 = new SosRequest(
                "9f001122-3344-5566-7788-99aabbccddee",
                "e1234567-89ab-cdef-0123-456789abcdef",
                "Hoàng Quốc Việt",
                "0933557799",
                SosRequest.TYPE_NHU_YEU_PHAM,
                "Khu trọ bị cô lập, thiếu nước sạch và sữa cho trẻ nhỏ",
                SosRequest.VISIBILITY_PUBLIC,
                10.773850,
                106.688250,
                6.0,
                SosRequest.STATUS_WAITING,
                "08:45 • Hôm nay"
        );
        sos4.setRoutePoints(MapHelper.computeStreetGraphRoute(
                rescuerBaseLat, rescuerBaseLng, 10.773850, 106.688250), false);
        sos4.addLog("08:45 • Đã phát tín hiệu cứu hộ");

        allSosList.add(sos1);
        allSosList.add(sos2);
        allSosList.add(sos3);
        allSosList.add(sos4);

        lastViewedSos = sos1;

        notifications.add("Đã phát tín hiệu cứu hộ #2CC295A6 tới các đội cứu hộ trong khu vực.");
        notifications.add("Hệ thống định vị thời gian thực đang hoạt động ổn định.");
        notifications.add("Vị trí trực tiếp của bạn sẽ tự động ngắt ngay khi kết thúc ca cứu hộ.");
    }

    public void loginAsRole(String role, String phone, String fullName) {
        this.currentRole = role;
        this.isValid = true;
        if (ROLE_RESCUER.equals(role)) {
            this.rescuerPhone = phone.isEmpty() ? "0901112222" : phone;
            this.rescuerFullName = fullName.isEmpty() ? "Trần Cứu Hộ" : fullName;
        } else {
            this.currentPhone = phone.isEmpty() ? "0901234567" : phone;
            this.currentFullName = fullName.isEmpty() ? "Nguyễn Văn An" : fullName;
        }
    }

    /**
     * GET /api/sos/my-active (Kiểm tra RB04: 1 nạn nhân chỉ có 1 SOS đang hoạt động)
     */
    public SosRequest getMyActiveUserSos() {
        for (SosRequest s : allSosList) {
            if (currentUserId.equals(s.getRequesterId()) && s.isActive()) {
                return s;
            }
        }
        return null;
    }

    /**
     * GET /api/rescuer/my-active (Kiểm tra RB05: Mỗi đội chỉ nhận 1 ca tại 1 thời điểm)
     */
    public SosRequest getMyActiveRescuerMission() {
        for (SosRequest s : allSosList) {
            if (rescuerTeamId.equals(s.getTeamId()) && s.isActive()) {
                return s;
            }
        }
        return null;
    }

    /**
     * GET /api/sos/public/map (Chỉ trả về các SOS đang hoạt động có visibility = PUBLIC)
     */
    public List<SosRequest> getPublicMapSosList(String emergencyTypeFilter) {
        List<SosRequest> list = new ArrayList<>();
        for (SosRequest s : allSosList) {
            if (s.isActive() && SosRequest.VISIBILITY_PUBLIC.equals(s.getVisibility())) {
                if (emergencyTypeFilter == null || "ALL".equals(emergencyTypeFilter)
                        || emergencyTypeFilter.equals(s.getEmergencyType())) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    /**
     * GET /api/rescuer/queue (Các SOS ở trạng thái WAITING < 2 giờ)
     */
    public List<SosRequest> getRescueQueue(String filter) {
        List<SosRequest> list = new ArrayList<>();
        for (SosRequest s : allSosList) {
            if (SosRequest.STATUS_WAITING.equals(s.getStatus())) {
                boolean match = true;
                if ("PUBLIC".equals(filter)) {
                    match = SosRequest.VISIBILITY_PUBLIC.equals(s.getVisibility());
                } else if ("PRIVATE".equals(filter)) {
                    match = SosRequest.VISIBILITY_PRIVATE.equals(s.getVisibility());
                } else if (filter != null && !"ALL".equals(filter)) {
                    match = filter.equals(s.getEmergencyType());
                }
                if (match) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    /**
     * POST /api/sos (Tạo yêu cầu cứu hộ mới - kiểm tra RB04)
     */
    public SosRequest createNewSos(String emergencyType, String description, String visibility, double lat, double lng) {
        SosRequest existing = getMyActiveUserSos();
        if (existing != null) {
            // Đóng ca cũ nếu tạo lại trong môi trường test
            existing.setStatus(SosRequest.STATUS_CANCELLED);
            existing.setEndReason(SosRequest.END_USER_CANCELLED);
        }

        String timeStr = new SimpleDateFormat("HH:mm:ss • dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String newId = UUID.randomUUID().toString();
        SosRequest created = new SosRequest(
                newId,
                currentUserId,
                currentFullName,
                currentPhone,
                emergencyType,
                description,
                visibility,
                lat,
                lng,
                myAccuracyM,
                SosRequest.STATUS_WAITING,
                timeStr
        );
        created.setRoutePoints(MapHelper.computeStreetGraphRoute(
                rescuerBaseLat, rescuerBaseLng, lat, lng), false);
        created.addLog(timeStr + " • Đã khởi tạo yêu cầu cứu hộ (" + created.getVisibilityLabel() + ")");
        created.addLog(timeStr + " • Đã lưu tọa độ GPS ("
                + String.format(Locale.US, "%.6f, %.6f", lat, lng) + ")");
        created.addLog(timeStr + " • Đã gửi thông báo khẩn cấp tới các đội cứu hộ");

        allSosList.add(0, created);
        lastViewedSos = created;
        notifications.add(0, "Đã gửi yêu cầu cứu hộ " + created.getShortId() + " (" + created.getEmergencyTypeLabel() + ").");
        return created;
    }

    /**
     * POST /api/rescuer/sos/{id}/accept (Tiếp nhận ca - RB05 & RB06)
     */
    public boolean acceptSosMission(SosRequest sos) {
        if (sos == null) return false;
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        sos.setStatus(SosRequest.STATUS_ACCEPTED);
        if (sos.getRoutePoints().isEmpty()) {
            sos.setRoutePoints(MapHelper.computeStreetGraphRoute(
                    rescuerBaseLat, rescuerBaseLng, sos.getVictimLat(), sos.getVictimLng()), false);
        }
        List<double[]> pts = sos.getRoutePoints();
        double startLat = pts.isEmpty() ? rescuerBaseLat : pts.get(0)[0];
        double startLng = pts.isEmpty() ? rescuerBaseLng : pts.get(0)[1];
        sos.assignRescuer(
                rescuerTeamId,
                rescuerUserId,
                rescuerFullName,
                rescuerPhone,
                rescuerTeamName,
                startLat,
                startLng,
                rescuerAccuracyM
        );
        sos.addLog(now + " • " + rescuerTeamName + " (" + rescuerFullName + ") đã tiếp nhận yêu cầu");
        sos.addLog(now + " • Đã thông báo cho người gửi " + sos.getRequesterName());
        lastViewedSos = sos;
        notifications.add(0, rescuerTeamName + " đã tiếp nhận yêu cầu " + sos.getShortId() + ".");
        return true;
    }

    /**
     * POST /api/rescuer/sos/{id}/status (Tuần tự ACCEPTED -> RESCUING -> ARRIVED)
     */
    public void updateMissionStatus(SosRequest sos, String newStatus) {
        if (sos == null) return;
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        sos.setStatus(newStatus);
        sos.addLog(now + " • Cập nhật tiến trình: " + SosRequest.getStatusLabel(newStatus));
        notifications.add(0, "Yêu cầu " + sos.getShortId() + ": " + SosRequest.getStatusLabel(newStatus) + ".");
    }

    /**
     * POST /api/rescuer/sos/{id}/release (Nhả ca -> quay lại WAITING)
     */
    public void releaseMission(SosRequest sos) {
        if (sos == null) return;
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        sos.setStatus(SosRequest.STATUS_WAITING);
        sos.clearRescuer();
        sos.addLog(now + " • Đội cứu hộ gặp sự cố di chuyển, yêu cầu được chuyển lại cho đội khác");
        notifications.add(0, "Đội cứu hộ gặp sự cố di chuyển, yêu cầu " + sos.getShortId() + " đang chờ đội khác tiếp nhận.");
    }

    /**
     * POST /api/rescuer/sos/{id}/complete (Hoàn thành ca từ trạng thái ARRIVED -> COMPLETED, RB09)
     */
    public void completeMission(SosRequest sos) {
        if (sos == null) return;
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        sos.setStatus(SosRequest.STATUS_COMPLETED);
        sos.setEndReason(SosRequest.END_RESCUER_COMPLETED);
        sos.addLog(now + " • Đội cứu hộ xác nhận đã hoàn thành nhiệm vụ an toàn");
        sos.addLog(now + " • Đã ngắt chia sẻ vị trí trực tiếp để bảo mật quyền riêng tư");
        lastViewedSos = sos;
        notifications.add(0, "Ca cứu hộ " + sos.getShortId() + " đã hoàn thành an toàn.");
    }

    /**
     * POST /api/sos/{id}/cancel (Nạn nhân hủy SOS -> CANCELLED, RB09)
     */
    public void cancelSosByUser(SosRequest sos) {
        if (sos == null) return;
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        sos.setStatus(SosRequest.STATUS_CANCELLED);
        sos.setEndReason(SosRequest.END_USER_CANCELLED);
        sos.addLog(now + " • Người gửi xác nhận đã an toàn và hủy yêu cầu");
        sos.addLog(now + " • Đã ngắt chia sẻ vị trí trực tiếp để bảo mật quyền riêng tư");
        lastViewedSos = sos;
        notifications.add(0, "Đã hủy yêu cầu " + sos.getShortId() + " và ngừng chia sẻ vị trí.");
    }

    public SosRequest getSosById(String id) {
        if (id != null) {
            for (SosRequest s : allSosList) {
                if (id.equals(s.getId()) || id.equals(s.getShortId())) {
                    return s;
                }
            }
        }
        if (lastViewedSos != null) return lastViewedSos;
        return allSosList.get(0);
    }

    public SosRequest getLastViewedSos() {
        if (lastViewedSos != null) return lastViewedSos;
        return allSosList.get(0);
    }

    public void setLastViewedSos(SosRequest lastViewedSos) {
        this.lastViewedSos = lastViewedSos;
    }

    public String getCurrentUserId() {
        return ROLE_RESCUER.equals(currentRole) ? rescuerUserId : currentUserId;
    }

    public String getCurrentFullName() {
        return ROLE_RESCUER.equals(currentRole) ? rescuerFullName : currentFullName;
    }

    public void updateProfileFullName(String newFullName) {
        if (ROLE_RESCUER.equals(currentRole)) {
            this.rescuerFullName = newFullName;
        } else {
            this.currentFullName = newFullName;
        }
    }

    public String getCurrentPhone() {
        return ROLE_RESCUER.equals(currentRole) ? rescuerPhone : currentPhone;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public boolean isValid() {
        return isValid;
    }

    public boolean isTelegramLinked() {
        return telegramLinked;
    }

    public void setTelegramLinked(boolean telegramLinked) {
        this.telegramLinked = telegramLinked;
    }

    public String getRescuerTeamName() {
        return rescuerTeamName;
    }

    public String getRescuerFullName() {
        return rescuerFullName;
    }

    public String getRescuerPhone() {
        return rescuerPhone;
    }

    public double getMyLat() {
        return myLat;
    }

    public double getMyLng() {
        return myLng;
    }

    public double getMyAccuracyM() {
        return myAccuracyM;
    }

    public void setMyLocation(double lat, double lng, double accuracyM) {
        this.myLat = lat;
        this.myLng = lng;
        this.myAccuracyM = accuracyM;
    }

    public double getRescuerBaseLat() {
        return rescuerBaseLat;
    }

    public double getRescuerBaseLng() {
        return rescuerBaseLng;
    }

    public List<String> getNotifications() {
        return notifications;
    }
}
