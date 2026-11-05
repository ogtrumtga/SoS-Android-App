package com.javafinal.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Model chuẩn khớp với cấu trúc dữ liệu bảng sos_requests, location_current,
 * location_history, sos_logs trong đặc tả BANG_API_VA_LUONG_HOAT_DONG.md.
 */
public class SosRequest implements Serializable {

    // Các mã emergency_type chuẩn gửi lên API POST /api/sos
    public static final String TYPE_NGAP_LUT = "NGAP_LUT";
    public static final String TYPE_Y_TE = "Y_TE";
    public static final String TYPE_MAC_KET = "MAC_KET";
    public static final String TYPE_CHAY_NO = "CHAY_NO";
    public static final String TYPE_NHU_YEU_PHAM = "NHU_YEU_PHAM";

    // Chế độ hiển thị visibility
    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    public static final String VISIBILITY_PRIVATE = "PRIVATE";

    // Vòng đời trạng thái chuẩn (State Machine Mục 4 - Luồng 5)
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_RESCUING = "RESCUING";
    public static final String STATUS_ARRIVED = "ARRIVED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Lý do kết thúc (end_reason)
    public static final String END_RESCUER_COMPLETED = "RESCUER_COMPLETED";
    public static final String END_USER_CANCELLED = "USER_CANCELLED";

    private String id; // UUID chuẩn: VD "2cc295a6-5723-4085-8dd8-1967ab544d31"
    private String requesterId;
    private String requesterName;
    private String requesterPhone;

    private String teamId;
    private String rescuerId;
    private String rescuerName;
    private String rescuerPhone;
    private String teamName;

    private String emergencyType;
    private String description;
    private String visibility;
    private String status;
    private String endReason;
    private String createdAt;

    // Tọa độ khởi tạo ban đầu
    private double initialLat;
    private double initialLng;

    // Bảng location_current (VICTIM & RESCUER)
    private double victimLat;
    private double victimLng;
    private double victimAccuracyM;

    private double rescuerLat;
    private double rescuerLng;
    private double rescuerAccuracyM;

    // Nhật ký kiểm toán sos_logs (GET /api/sos/{id}/logs)
    private final List<String> logs = new ArrayList<>();

    // Lộ trình đường đi thực tế trên bản đồ (danh sách các điểm [lat, lng] theo đường giao thông)
    private final List<double[]> routePoints = new ArrayList<>();
    private int routeStepIndex = 0;
    private boolean osrmLoaded = false;

    public SosRequest(
            String id,
            String requesterId,
            String requesterName,
            String requesterPhone,
            String emergencyType,
            String description,
            String visibility,
            double initialLat,
            double initialLng,
            double victimAccuracyM,
            String status,
            String createdAt
    ) {
        this.id = id;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterPhone = requesterPhone;
        this.emergencyType = emergencyType;
        this.description = description;
        this.visibility = visibility;
        this.initialLat = initialLat;
        this.initialLng = initialLng;
        this.victimLat = initialLat;
        this.victimLng = initialLng;
        this.victimAccuracyM = victimAccuracyM;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void assignRescuer(
            String teamId,
            String rescuerId,
            String rescuerName,
            String rescuerPhone,
            String teamName,
            double rescuerLat,
            double rescuerLng,
            double rescuerAccuracyM
    ) {
        this.teamId = teamId;
        this.rescuerId = rescuerId;
        this.rescuerName = rescuerName;
        this.rescuerPhone = rescuerPhone;
        this.teamName = teamName;
        this.rescuerLat = rescuerLat;
        this.rescuerLng = rescuerLng;
        this.rescuerAccuracyM = rescuerAccuracyM;
    }

    public void clearRescuer() {
        this.teamId = null;
        this.rescuerId = null;
        this.rescuerName = null;
        this.rescuerPhone = null;
        this.teamName = null;
        this.routeStepIndex = 0;
    }

    public synchronized void setRoutePoints(List<double[]> points, boolean fromOsrm) {
        double progressRatio = 0;
        if (this.routePoints.size() > 1 && this.routeStepIndex > 0) {
            progressRatio = (double) this.routeStepIndex / (this.routePoints.size() - 1);
        }
        this.routePoints.clear();
        if (points != null) {
            this.routePoints.addAll(points);
        }
        this.osrmLoaded = fromOsrm;
        if (!this.routePoints.isEmpty()) {
            if (STATUS_ARRIVED.equals(this.status)) {
                this.routeStepIndex = Math.max(0, this.routePoints.size() - 2);
            } else if (progressRatio > 0) {
                this.routeStepIndex = Math.min(
                        Math.max(0, this.routePoints.size() - 2),
                        (int) Math.round(progressRatio * (this.routePoints.size() - 1))
                );
            } else {
                this.routeStepIndex = 0;
            }
            double[] cur = this.routePoints.get(this.routeStepIndex);
            this.rescuerLat = cur[0];
            this.rescuerLng = cur[1];
        }
    }

    public synchronized List<double[]> getRoutePoints() {
        return new ArrayList<>(routePoints);
    }

    public synchronized List<double[]> getRemainingRoutePoints() {
        if (routePoints.isEmpty()) return new ArrayList<>();
        int start = Math.max(0, Math.min(routeStepIndex, routePoints.size() - 1));
        return new ArrayList<>(routePoints.subList(start, routePoints.size()));
    }

    public synchronized boolean advanceAlongRoute() {
        if (routePoints.size() <= 2) return false;
        int step = Math.max(1, routePoints.size() / 5);
        int nextIndex = Math.min(routePoints.size() - 2, routeStepIndex + step);
        if (nextIndex > routeStepIndex) {
            routeStepIndex = nextIndex;
            double[] pt = routePoints.get(routeStepIndex);
            this.rescuerLat = pt[0];
            this.rescuerLng = pt[1];
            return true;
        }
        return false;
    }

    public synchronized void snapRescuerToDestination() {
        if (!routePoints.isEmpty()) {
            routeStepIndex = Math.max(0, routePoints.size() - 2);
            double[] pt = routePoints.get(routeStepIndex);
            this.rescuerLat = pt[0];
            this.rescuerLng = pt[1];
        } else {
            this.rescuerLat = this.victimLat;
            this.rescuerLng = this.victimLng;
        }
    }

    public synchronized boolean isOsrmLoaded() {
        return osrmLoaded;
    }

    public synchronized double getRemainingRouteDistanceKm() {
        List<double[]> rem = getRemainingRoutePoints();
        if (rem.size() >= 2) {
            double totalKm = 0;
            for (int i = 0; i < rem.size() - 1; i++) {
                totalKm += computeHaversineKm(
                        rem.get(i)[0], rem.get(i)[1],
                        rem.get(i + 1)[0], rem.get(i + 1)[1]
                );
            }
            return Math.round(totalKm * 10.0) / 10.0;
        }
        return getDistanceKmFrom(rescuerLat, rescuerLng);
    }

    public static double computeHaversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    public void addLog(String logEntry) {
        logs.add(0, logEntry);
    }

    public List<String> getLogs() {
        return logs;
    }

    public String getShortId() {
        if (id == null) return "#SOS";
        return id.length() > 8 ? "#" + id.substring(0, 8).toUpperCase(Locale.ROOT) : "#" + id;
    }

    public String getEmergencyTypeLabel() {
        return getEmergencyTypeLabel(emergencyType);
    }

    public static String getEmergencyTypeLabel(String type) {
        if (TYPE_NGAP_LUT.equals(type)) return "Ngập lụt";
        if (TYPE_Y_TE.equals(type)) return "Cấp cứu y tế";
        if (TYPE_MAC_KET.equals(type)) return "Mắc kẹt / Sập đổ";
        if (TYPE_CHAY_NO.equals(type)) return "Hỏa hoạn / Cháy nổ";
        if (TYPE_NHU_YEU_PHAM.equals(type)) return "Nhu yếu phẩm";
        return type != null ? type : "Khẩn cấp";
    }

    public String getStatusLabel() {
        return getStatusLabel(status);
    }

    public static String getStatusLabel(String status) {
        if (STATUS_WAITING.equals(status)) return "Đang chờ tiếp nhận";
        if (STATUS_ACCEPTED.equals(status)) return "Đã tiếp nhận";
        if (STATUS_RESCUING.equals(status)) return "Đang di chuyển";
        if (STATUS_ARRIVED.equals(status)) return "Đã tới hiện trường";
        if (STATUS_COMPLETED.equals(status)) return "Đã hoàn thành";
        if (STATUS_CANCELLED.equals(status)) return "Đã hủy";
        return status != null ? status : "Đang xử lý";
    }

    public String getVisibilityLabel() {
        return VISIBILITY_PRIVATE.equals(visibility) ? "Riêng tư" : "Công khai";
    }

    /**
     * Tọa độ làm tròn 2 chữ số thập phân (~1km) dùng cho GET /api/sos/public/map
     */
    public double getAreaLat() {
        return Math.round(victimLat * 100.0) / 100.0;
    }

    public double getAreaLng() {
        return Math.round(victimLng * 100.0) / 100.0;
    }

    public double getDistanceKmFrom(double lat, double lng) {
        synchronized (this) {
            if (routePoints.size() >= 2) {
                double totalKm = 0;
                int start = Math.max(0, Math.min(routeStepIndex, routePoints.size() - 1));
                for (int i = start; i < routePoints.size() - 1; i++) {
                    totalKm += computeHaversineKm(
                            routePoints.get(i)[0], routePoints.get(i)[1],
                            routePoints.get(i + 1)[0], routePoints.get(i + 1)[1]
                    );
                }
                return Math.round(totalKm * 10.0) / 10.0;
            }
        }
        double straightKm = computeHaversineKm(lat, lng, victimLat, victimLng);
        return Math.round((straightKm * 1.3) * 10.0) / 10.0;
    }

    public boolean isActive() {
        return !STATUS_COMPLETED.equals(status) && !STATUS_CANCELLED.equals(status);
    }

    public String getId() {
        return id;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterPhone() {
        return requesterPhone;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getRescuerId() {
        return rescuerId;
    }

    public String getRescuerName() {
        return rescuerName;
    }

    public String getRescuerPhone() {
        return rescuerPhone;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public void setEmergencyType(String emergencyType) {
        this.emergencyType = emergencyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public double getInitialLat() {
        return initialLat;
    }

    public double getInitialLng() {
        return initialLng;
    }

    public double getVictimLat() {
        return victimLat;
    }

    public void setVictimLat(double victimLat) {
        this.victimLat = victimLat;
    }

    public double getVictimLng() {
        return victimLng;
    }

    public void setVictimLng(double victimLng) {
        this.victimLng = victimLng;
    }

    public double getVictimAccuracyM() {
        return victimAccuracyM;
    }

    public void setVictimAccuracyM(double victimAccuracyM) {
        this.victimAccuracyM = victimAccuracyM;
    }

    public double getRescuerLat() {
        return rescuerLat;
    }

    public void setRescuerLat(double rescuerLat) {
        this.rescuerLat = rescuerLat;
    }

    public double getRescuerLng() {
        return rescuerLng;
    }

    public void setRescuerLng(double rescuerLng) {
        this.rescuerLng = rescuerLng;
    }

    public double getRescuerAccuracyM() {
        return rescuerAccuracyM;
    }

    public void setRescuerAccuracyM(double rescuerAccuracyM) {
        this.rescuerAccuracyM = rescuerAccuracyM;
    }
}
