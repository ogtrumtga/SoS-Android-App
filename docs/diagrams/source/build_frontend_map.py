"""Build the source-backed SOS Rescue architecture poster (standard library only).

Run from the repository: python docs/diagrams/source/build_frontend_map.py
Then: powershell -File docs/diagrams/source/render_frontend_map.ps1
"""

from pathlib import Path
from html import escape
import json
import math

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "docs/diagrams"
W, H = 3000, 4450
S = []
SOURCES = set()
C = {
    "ink": "#15253E", "muted": "#51647C", "light": "#E1E8F1",
    "bg": "#F3F6FB", "white": "#FFFFFF", "blue": "#2563C5",
    "green": "#16816A", "purple": "#8454BA", "amber": "#B87512",
    "red": "#C34C58", "teal": "#117C89",
}


def rect(x, y, w, h, fill, stroke=None, radius=18, sw=2):
    S.append(dict(type="rect", x=x, y=y, w=w, h=h, fill=fill,
                  stroke=stroke or fill, radius=radius, sw=sw))


def text(x, y, value, size=24, color=None, bold=False, mono=False,
         width=None, source=None):
    if source:
        assert (ROOT / source).is_file(), source
        SOURCES.add(source)
    S.append(dict(type="text", x=x, y=y, text=value, size=size,
                  color=color or C["ink"], bold=bold,
                  font="Consolas" if mono else "Segoe UI",
                  max_width=width, source=source))


def lines(x, y, values, size=24, gap=35, color=None, bold=False,
          mono=False, width=None):
    for i, value in enumerate(values):
        text(x, y + gap * i, value, size, color, bold, mono, width)


def file(x, y, name, folder, size=26, color=None, width=None):
    source = f"app/src/main/java/com/javafinal/{folder}/{name}"
    text(x, y, name, size, color, True, width=width, source=source)


def edge(points, color=None, dashed=False, arrow=True, inheritance=False, sw=3):
    color = color or C["blue"]
    S.append(dict(type="line", points=points, color=color,
                  dashed=dashed, sw=sw))
    if arrow or inheritance:
        a, b = points[-2:]
        angle = math.atan2(b[1] - a[1], b[0] - a[0])
        length, spread = (19, 10) if inheritance else (14, 7)
        p = [b,
             [b[0] - length * math.cos(angle) + spread * math.sin(angle),
              b[1] - length * math.sin(angle) - spread * math.cos(angle)],
             [b[0] - length * math.cos(angle) - spread * math.sin(angle),
              b[1] - length * math.sin(angle) + spread * math.cos(angle)]]
        S.append(dict(type="polygon", points=p, stroke=color,
                      fill=C["white"] if inheritance else color, sw=2))


def label(x, y, value, color=None, size=20):
    # Backing keeps connector captions readable on the canvas.
    width = len(value) * size * .57 + 24
    rect(x - 10, y - 3, width, size * 1.5, C["bg"], radius=6)
    text(x, y, value, size, color or C["muted"])


def section(y, number, title, subtitle=None):
    rect(80, y + 2, 50, 43, C["ink"], radius=12)
    text(91, y + 5, number, 27, C["white"], True)
    text(150, y, title, 34, bold=True)
    if subtitle:
        text(150, y + 48, subtitle, 22, C["muted"])


def card(x, y, w, h, color, tint="#FFFFFF", title=None):
    rect(x, y + 5, w, h, "#E5EBF4", radius=20)
    rect(x, y, w, h, tint, C["light"], radius=20)
    rect(x, y + 20, 6, h - 40, color, radius=3)
    if title:
        text(x + 25, y + 20, title, 23, color, True, width=w - 50)


rect(0, 0, W, H, C["bg"], radius=0)
rect(0, 0, W, 14, C["ink"], radius=0)
text(80, 55, "SOS RESCUE / FRONT-END", 60, bold=True)
text(82, 139, "Bản đồ file quan trọng • hàm được gọi • API • lớp nền • đường đi dữ liệu", 29, C["muted"])
rect(2555, 60, 365, 61, "#E7EDF8", radius=15)
text(2580, 74, "ANDROID · JAVA + XML", 25, C["blue"], True)

rect(80, 204, 2840, 69, C["white"], C["light"], radius=16)
edge([(108, 239), (185, 239)], C["blue"])
text(203, 222, "Gọi hàm / gửi REST", 23)
edge([(607, 239), (684, 239)], C["green"])
text(702, 222, "Dữ liệu / kết quả về", 23)
edge([(1110, 239), (1187, 239)], C["purple"], dashed=True, inheritance=True)
text(1205, 222, "Lớp con → lớp cha", 23)
edge([(1635, 239), (1712, 239)], C["amber"])
text(1730, 222, "WebSocket", 23)
edge([(2055, 239), (2132, 239)], C["muted"], dashed=True)
text(2150, 222, "Khởi tạo / cấp phụ thuộc", 23)

# 01. Feature branches: actual callers above their API ViewModels.
section(309, "01", "CÁC NHÁNH CHỨC NĂNG", "Đọc từng cột từ màn hình xuống ViewModel; các API ghi bên dưới được gửi qua tầng mạng ở mục 02.")
xs = [80, 810, 1540, 2270]
colors = [C["purple"], C["blue"], C["teal"], C["red"]]
titles = ["KHỞI ĐỘNG / ĐĂNG NHẬP", "NGƯỜI DÂN / USER", "ĐỘI CỨU HỘ / RESCUER", "CHỨC NĂNG BỔ SUNG"]
for x, color, title in zip(xs, colors, titles):
    card(x, 402, 650, 294, color, title=title)
    card(x, 757, 650, 440, color, title="VIEWMODEL → HÀM → API TIÊU BIỂU")
    edge([(x + 325, 696), (x + 325, 757)], C["blue"])
    label(x + 345, 710, "gọi hàm", size=19)

x = xs[0] + 25
file(x, 458, "SplashActivity.java", "ui/splash", width=600)
text(x, 495, "Launcher: kiểm tra server và phiên", 23, C["muted"])
file(x, 537, "LoginActivity.java", "ui/auth", width=600)
text(x, 574, "Nhập số điện thoại → xác thực OTP", 23, C["muted"])
lines(x, 619, ["User từ server → goToRoleHome()", "USER → Home; RESCUER → RescuerHome"], 21, 29)
file(x, 810, "SystemViewModel.java", "viewmodel", 25)
text(x, 847, "checkHealth() → GET /health", 21, mono=True)
file(x, 889, "AuthViewModel.java", "viewmodel", 25)
lines(x, 927, ["requestOtp() → POST /api/auth/request-otp", "verifyOtp()  → POST /api/auth/verify-otp", "loadMe()     → GET  /api/auth/me"], 20, 34, mono=True, width=600)
text(x, 1048, "AuthModels.java + User.java", 23, bold=True,
     source="app/src/main/java/com/javafinal/model/AuthModels.java")
lines(x, 1084, ["JWT + User → SessionStore.save()", "Vai trò điều hướng lấy từ response."], 22, 34, C["muted"])

x = xs[1] + 25
file(x, 458, "HomeActivity.java", "ui/user", width=600)
text(x, 495, "Bản đồ cộng đồng + SOS đang hoạt động", 22, C["muted"])
file(x, 537, "SendSosActivity.java", "ui/user", width=600)
text(x, 574, "Form loại sự cố, mô tả, vị trí, visibility", 22, C["muted"])
file(x, 615, "TrackSosActivity.java", "ui/user", 25)
file(x, 653, "SosResultActivity.java", "ui/user", 25)
text(x + 340, 619, "Theo dõi / hủy", 20, C["muted"], width=260)
text(x + 340, 657, "Kết quả / nhật ký", 20, C["muted"], width=260)
file(x, 810, "SosViewModel.java", "viewmodel", 25)
lines(x, 852, ["createSos() → POST /api/sos", "loadMyActive() → GET /api/sos/my-active", "loadPublicMap() → GET /api/sos/public/map", "loadDetail(id) → GET /api/sos/{id}"], 20, 34, mono=True, width=600)
lines(x, 1005, ["updateVisibility() → PUT .../{id}/visibility", "cancelSos() → POST .../{id}/cancel", "loadLogs() → GET .../{id}/logs"], 20, 34, mono=True, width=600)
text(x, 1135, "Track/Result: theo dõi, đóng ca, xem nhật ký", 22, C["muted"], width=600)

x = xs[2] + 25
file(x, 458, "RescuerHomeActivity.java", "ui/rescuer", 24)
file(x, 496, "SosDetailActivity.java", "ui/rescuer", 24)
file(x, 534, "RealtimeTrackingActivity.java", "ui/rescuer", 24)
file(x, 572, "ConfirmCompleteActivity.java", "ui/rescuer", 24)
file(x, 610, "RescuerMapActivity.java", "ui/rescuer", 24)
file(x, 648, "RescuerMissionsActivity.java", "ui/rescuer", 24)
for y, desc in [(462, "1 · Hàng đợi"), (500, "2 · Xem / nhận ca"), (538, "3 · GPS + tiến trình"), (576, "4 · Hoàn thành"), (614, "↳ Bản đồ cứu hộ"), (652, "↳ Ca / lịch sử")]:
    text(x + 408, y, desc, 19, C["muted"], width=192)
file(x, 810, "RescuerViewModel.java", "viewmodel", 25)
text(x, 850, "loadQueue / loadMyActive / loadMyMissions", 21, mono=True, width=600)
text(x, 887, "GET /api/rescuer/{queue,my-active,missions}", 20, mono=True, width=600)
text(x, 925, "loadMap() → GET /api/rescuer/map", 20, mono=True)
text(x, 970, "acceptSos / updateMissionStatus /", 21, mono=True)
text(x, 1003, "releaseSos / completeSos", 21, mono=True)
text(x, 1046, "POST /api/rescuer/sos/{id}/", 20, mono=True)
text(x, 1078, "     {accept,status,release,complete}", 20, mono=True)
text(x, 1125, "SosViewModel: đọc chi tiết / nhật ký ca", 22, C["muted"])
text(x, 1163, "SosRequestAdapter.java: click → callback về Activity", 19, C["muted"],
     source="app/src/main/java/com/javafinal/ui/rescuer/SosRequestAdapter.java", width=600)

x = xs[3] + 25
file(x, 458, "NotificationActivity.java", "ui/notification", 25)
text(x, 495, "NotificationAdapter.java: danh sách, đã đọc", 21, C["muted"],
     source="app/src/main/java/com/javafinal/ui/notification/NotificationAdapter.java")
file(x, 537, "RescueTeamActivity.java", "ui/rescuer", 25)
text(x, 574, "TeamMemberAdapter.java: thành viên / vai trò", 21, C["muted"],
     source="app/src/main/java/com/javafinal/ui/rescuer/TeamMemberAdapter.java")
file(x, 615, "NavigationHelper.java", "util", 25)
text(x, 652, "Hồ sơ, đổi tên, đăng xuất, gọi điện / SMS", 22, C["muted"])
file(x, 810, "NotificationViewModel.java", "viewmodel", 24)
lines(x, 846, ["loadMyNotifications() / markAsRead()", "GET /api/sos/notifications/my", "PUT /api/sos/notifications/{id}/read"], 19, 28, mono=True)
file(x, 943, "TeamViewModel.java", "viewmodel", 24)
lines(x, 978, ["loadMyTeams() → GET .../teams/mine", "addMember / updateMember / deleteMember", "POST / PUT / DELETE .../teams/{id}/members"], 19, 28, mono=True)
file(x, 1072, "UserViewModel.java", "viewmodel", 24)
lines(x, 1108, ["loadProfile() → GET /api/users/profile", "updateFullName() → PUT /api/users/full-name"], 19, 28, mono=True)
text(x, 1166, ".../teams = /api/rescuer/teams; sửa/xóa có /{memberId}", 18, C["muted"], width=600)

# Four branches merge into the shared REST base.
for x in xs:
    edge([(x + 325, 1197), (x + 325, 1243)], arrow=False, sw=2)
edge([(405, 1243), (2595, 1243)], arrow=False, sw=2)
edge([(1130, 1243), (1130, 1426)], sw=3)
label(1280, 1221, "Các ViewModel REST dùng chung request()", C["blue"], 22)

# 02. Common network loop, with the return path explicitly distinct.
section(1300, "02", "LUỒNG GỌI REST VÀ DỮ LIỆU TRẢ VỀ", "Hàng trên: lệnh đi tới server. Hàng dưới: JSON trở về, được parse rồi phát trạng thái cho UI.")
core_xs = [80, 820, 1560, 2300]
for x in core_xs:
    card(x, 1426, 620, 205, C["blue"])
    card(x, 1743, 620, 202, C["green"], "#F9FEFC")

text(105, 1448, "Activity → ViewModel nghiệp vụ", 28, bold=True, width=570)
lines(105, 1495, ["onClick → execute(key, action)", "Ví dụ: SosViewModel.createSos(dto)", "Activity đăng ký bind() để nhận kết quả."], 23, 36, width=570)
file(845, 1448, "BaseApiViewModel.java", "viewmodel", 28, width=570)
lines(845, 1495, ["request(...): validate → LOADING", "Theo dõi Call; bảo vệ phiên đã thay đổi.", "onCleared() → cancelRequests()"], 23, 36, width=570)
file(1585, 1448, "ApiClient.java", "network", 28)
lines(1585, 1495, ["newCall() → enqueue() bằng OkHttp", "Gson: Java object → JSON request", "Authorization: Bearer JWT nếu auth=true"], 23, 36, width=570)
text(2325, 1448, "BACKEND REST API", 28, C["blue"], True)
lines(2325, 1495, ["Nhận HTTP + JSON; xác thực / phân quyền.", "Xử lý nghiệp vụ → trả response JSON.", "Backend nằm ngoài repository này."], 23, 36, width=570)
for a, b in zip(core_xs, core_xs[1:]):
    edge([(a + 620, 1535), (b, 1535)])
label(716, 1495, "request", size=18)
label(1453, 1495, "Call", size=18)
label(2180, 1495, "HTTP", size=18)

edge([(2610, 1631), (2610, 1743)], C["green"])
label(2640, 1666, "JSON response", C["green"], 21)
text(2325, 1765, "ApiClient / Gson → DTO", 27, C["green"], True)
file(2325, 1810, "SosApiModels.java", "model", 23)
lines(2325, 1847, ["AuthModels.java / TeamApiModels.java", "SosRequest, User, LocationCurrent..."], 22, 34, width=570)
SOURCES.add("app/src/main/java/com/javafinal/model/TeamApiModels.java")
file(1585, 1765, "ApiState.java", "viewmodel", 27, C["green"])
lines(1585, 1810, ["BaseApiViewModel nhận callback:", "SUCCESS(data) hoặc ERROR(ApiError)", "Callback được đưa về main thread."], 23, 36, width=570)
file(845, 1765, "ScreenRequests.java", "ui", 27, C["green"])
lines(845, 1810, ["Giữ LiveData<ApiState<T>> theo key.", "ApiActivity.attach() → observe(this,...)", "consume(): xử lý mỗi kết quả một lần."], 23, 36, width=570)
text(105, 1765, "Activity / Adapter / XML", 27, C["green"], True)
lines(105, 1810, ["LOADING: khóa nút / hiện tiến trình", "SUCCESS: render / điều hướng theo data", "ERROR: showApiError() / nút thử lại"], 23, 36, width=570)
for right, left in zip(core_xs[:0:-1], core_xs[-2::-1]):
    edge([(right, 1841), (left + 620, 1841)], C["green"])
edge([(390, 1743), (390, 1631)], C["green"])
label(104, 1671, "cập nhật màn hình", C["green"], 21)

# 03. Separate inheritance from construction/dependencies.
section(2004, "03", "LỚP NỀN VÀ CÁC THÀNH PHẦN DÙNG CHUNG")
rect(80, 2079, 1395, 539, C["white"], C["light"], radius=22)
text(106, 2100, "KẾ THỪA UI · mũi tên hướng về lớp cha", 23, C["purple"], True)
base_nodes = [("ApiActivity.java", 2160), ("MapApiActivity.java", 2334), ("LiveSosActivity.java", 2508)]
for name, y in base_nodes:
    rect(110, y, 535, 87, "#F3EEFB", "#DCD0ED", radius=15)
    file(133, y + 22, name, "ui", 27, C["purple"], 485)
edge([(375, 2334), (375, 2247)], C["purple"], True, inheritance=True)
edge([(375, 2508), (375, 2421)], C["purple"], True, inheritance=True)
text(405, 2271, "extends", 20, C["purple"])
text(405, 2445, "extends", 20, C["purple"])
lines(685, 2165, ["setupScreen / vm / bind / execute / xử lý 401", "Base cho Splash, Login, Notification,", "RescuerHome, RescueTeam, Missions..."], 23, 34, width=755)
lines(685, 2339, ["Thêm MapView, style và vòng đời bản đồ.", "HomeActivity, SendSosActivity,", "SosDetailActivity, RescuerMapActivity."], 23, 34, width=755)
lines(685, 2510, ["Thêm theo dõi SOS, REST, WS, GPS.", "TrackSosActivity + RealtimeTrackingActivity", "kế thừa trực tiếp lớp này."], 23, 32, width=755)

rect(1525, 2079, 1395, 539, C["white"], C["light"], radius=22)
text(1551, 2100, "KHỞI TẠO / PHIÊN / BASE VIEWMODEL", 23, C["muted"], True)
rect(1555, 2158, 1335, 132, "#F0F5FC", C["light"], radius=15)
file(1580, 2175, "ApiViewModelFactory.java", "viewmodel", 28)
text(1580, 2221, "app/build.gradle.kts → BuildConfig.API_BASE_URL → getDefault(context)", 24,
     source="app/build.gradle.kts", width=1285)
edge([(2000, 2290), (2000, 2345)], C["muted"], True)
edge([(2600, 2290), (2600, 2345)], C["muted"], True)
rect(1555, 2345, 675, 125, "#F0F5FC", C["light"], radius=15)
lines(1580, 2360, ["Tạo ViewModel + ApiClient dùng chung", "BaseApiViewModel ← các VM nghiệp vụ", "RealtimeViewModel cũng extends Base."], 23, 33, width=625)
rect(2280, 2345, 610, 125, "#F0F5FC", C["light"], radius=15)
file(2305, 2358, "SessionStore.java", "network", 25)
file(2305, 2394, "PreferencesSessionStore.java", "network", 25, width=560)
text(2305, 2430, "JWT + User ↔ SharedPreferences", 23)
edge([(2280, 2415), (2230, 2415)], C["muted"], True)
text(1555, 2500, "InputValidator.java: kiểm tra đầu vào    •    SosRules.java: điều kiện thao tác SOS", 23, width=1335,
     source="app/src/main/java/com/javafinal/viewmodel/InputValidator.java")
SOURCES.add("app/src/main/java/com/javafinal/viewmodel/SosRules.java")
text(1555, 2545, "EmergencyTypeViewModel.java → loadAll() → GET /api/emergency-types", 23, width=1335,
     source="app/src/main/java/com/javafinal/viewmodel/EmergencyTypeViewModel.java")

# 04. Realtime/GPS/map form a second explicit data path.
section(2680, "04", "GPS, WEBSOCKET VÀ BẢN ĐỒ", "LiveSosActivity phối hợp các nhánh này cho TrackSosActivity và RealtimeTrackingActivity.")
lane_xs = [80, 1060, 2040]
for x, title, color in zip(lane_xs, ["A · GỬI VỊ TRÍ THIẾT BỊ", "B · SERVER ĐẨY SỰ KIỆN", "C · DỮ LIỆU → BẢN ĐỒ"], [C["blue"], C["amber"], C["teal"]]):
    text(x, 2780, title, 25, color, True)
    for y, h in [(2833, 178), (3105, 210), (3411, 188)]:
        card(x, y, 880, h, color)

file(105, 2853, "DeviceLocation.java", "util", 28)
lines(105, 2900, ["FusedLocationProvider → Location(lat,lng,accuracy)", "request() xin quyền; callback nhận tọa độ thật.", "Lifecycle onStop() ngừng cập nhật GPS."], 24, 35, width=830)
edge([(520, 3011), (520, 3105)])
label(547, 3040, "onLocation → publishGps()", C["blue"], 22)
file(105, 3126, "LiveSosActivity.java", "ui", 28)
file(105, 3171, "LocationViewModel.java", "viewmodel", 26)
lines(105, 3218, ["canPublishGps() → updateLocation(sosId,...)", "Chặn lần gửi cách nhau dưới 6 giây.", "REST đi qua BaseApiViewModel → ApiClient."], 23, 33, width=830)
edge([(520, 3315), (520, 3411)])
label(547, 3350, "JSON tọa độ", C["blue"], 22)
text(105, 3432, "POST /api/location/{id}", 28, C["blue"], True, True)
lines(105, 3480, ["lat, lng, accuracy_m, save_history", "Đọc lại: GET .../{id}/current và .../{id}/trail", "Model: LocationCurrent.java / LocationHistory.java"], 23, 34, width=830)
SOURCES.update({"app/src/main/java/com/javafinal/model/LocationCurrent.java", "app/src/main/java/com/javafinal/model/LocationHistory.java"})

text(1085, 2853, "BACKEND · WS /ws?token={JWT}", 28, C["amber"], True)
lines(1085, 2900, ["Client: SUBSCRIBE_SOS(sos_id), PING", "Server: LOCATION_BROADCAST, STATUS_CHANGED...", "Dùng cùng base URL với REST."], 24, 35, width=830)
edge([(1450, 3011), (1450, 3105)], C["amber"])
edge([(1550, 3105), (1550, 3011)], C["amber"])
label(1580, 3040, "hai chiều", C["amber"], 22)
file(1085, 3126, "RealtimeViewModel.java", "viewmodel", 28, C["amber"])
text(1085, 3171, "connect() / subscribeSos() / getMessages()", 24, mono=True)
file(1085, 3214, "RealtimeProtocol.java", "network", 25)
text(1085, 3251, "decode(): type → event → WebSocketMessage.java", 23,
     source="app/src/main/java/com/javafinal/model/WebSocketMessage.java", width=830)
text(1085, 3283, "OkHttp WebSocket dùng api.getHttp(); tự reconnect.", 22, C["muted"])
edge([(1500, 3315), (1500, 3411)], C["amber"])
label(1530, 3350, "LiveData<WebSocketMessage>", C["amber"], 22)
text(1085, 3432, "Activity observe() sự kiện", 28, C["amber"], True)
lines(1085, 3480, ["LOCATION_BROADCAST → sửa currentSos → render", "Đổi trạng thái → refreshSos() bằng REST", "COMPLETED / CANCELLED → endTracking()"], 23, 34, width=830)

file(2065, 2853, "OsrmRouter.java", "util", 28)
lines(2065, 2900, ["fetchRoute() → router.project-osrm.org", "GET /route/v1/driving/{lng,lat};{lng,lat}", "HTTP client riêng + cache tuyến đường."], 24, 35, width=830)
edge([(2480, 3011), (2480, 3105)], C["green"])
label(2510, 3040, "RouteResult: points, km, phút", C["green"], 22)
file(2065, 3126, "SosRequest.java", "model", 28)
lines(2065, 3171, ["currentSos.locations: victim + rescuer", "LocationHistory: trail; setOsrmRoute(): tuyến", "locationVm.loadCurrent() / loadTrail() → vị trí", "Cập nhật model → renderLiveMap()"], 24, 34, width=830)
edge([(2480, 3315), (2480, 3411)], C["green"])
label(2510, 3350, "tọa độ + trail + route", C["green"], 22)
file(2065, 3432, "SosMapData.java", "util", 28)
file(2065, 3477, "MapHelper.java", "util", 26)
lines(2065, 3520, ["draw / drawPublic / drawRescuer → MapLibre", "Marker, vùng công khai, polyline, camera, style"], 23, 33, width=830)

# Cross-lane links run only in the gutters.
edge([(960, 3505), (1010, 3505), (1010, 2910), (1060, 2910)], C["blue"], sw=2)
edge([(1940, 3505), (2040, 3505)], C["green"])
label(1950, 3464, "vẽ lại", C["green"], 18)
text(80, 3630, "Đường vị trí: GPS → POST REST → backend → LOCATION_BROADCAST → currentSos → vẽ lại bản đồ.", 26, bold=True, width=2840)
text(80, 3676, "LiveSos: poll REST mỗi 15 s; WS ping mỗi 25 s; rời màn hình / đóng ca thì dừng các tác vụ theo lifecycle.", 24, C["muted"], width=2840)

# 05. A concrete trace from the source, including payload and navigation.
section(3760, "05", "LẦN THEO MỘT THAO TÁC: NGƯỜI DÂN BẤM “GỬI SOS”")
step_xs = [80 + 480 * i for i in range(6)]
step_titles = ["ĐỌC FORM", "ĐÓNG GÓI DỮ LIỆU", "GỌI NGHIỆP VỤ", "GỬI HTTP", "NHẬN RESPONSE", "MỞ MÀN THEO DÕI"]
for i, (x, title) in enumerate(zip(step_xs, step_titles)):
    card(x, 3843, 440, 268, C["blue"] if i < 4 else C["green"])
    text(x + 23, 3863, f"{i + 1:02d}  {title}", 22, C["blue"] if i < 4 else C["green"], True, width=394)
    if i < 5:
        edge([(x + 440, 3978), (x + 480, 3978)], C["blue"] if i < 3 else C["green"])

lines(103, 3912, ["SendSosActivity.java", "sendSos()", "Loại sự cố + mô tả", "PUBLIC / PRIVATE", "GPS / điểm chọn trên map"], 22, 34, width=394)
lines(583, 3912, ["SosApiModels.java", "CreateSosRequest", "emergency_type_id / type", "description, visibility", "initial_lat, initial_lng"], 22, 34, width=394)
lines(1063, 3912, ["SosViewModel.java", "createSos(request)", "request(POST, api/sos,...)", "InputValidator kiểm tra", "ApiState = LOADING"], 22, 34, width=394)
lines(1543, 3912, ["ApiClient.java", "newCall() → enqueue()", "POST /api/sos", "Bearer JWT + JSON", "OkHttp chạy bất đồng bộ"], 22, 34, width=394)
lines(2023, 3912, ["Gson → SosSingleResponse", "response.getSos().getId()", "ApiState = SUCCESS(data)", "LiveData → bind(\"create\")", "Callback trên main thread"], 22, 34, width=394)
lines(2503, 3912, ["openSos(..., id)", "→ TrackSosActivity.java", "Intent extra: SOS_ID", "initLiveSos() → loadDetail", "REST + WS + GPS ở mục 04"], 22, 34, width=394)

rect(80, 4150, 2840, 95, "#FFF5EF", "#F0D7CA", radius=17)
text(105, 4168, "Nhánh lỗi: ERROR → showApiError(); HTTP 409 → loadMyActive(); HTTP 401 → xóa phiên và về Login.", 25, "#91492E", True, width=2790)
text(105, 4206, "SendSosActivity chỉ mở TrackSosActivity khi đã có SOS ID; trạng thái nghiệp vụ chờ server xác nhận.", 24, "#91492E", width=2790)

text(80, 4282, "XML chính: activity_send_sos.xml ↔ SendSosActivity • activity_realtime_tracking.xml ↔ RealtimeTrackingActivity • item_sos_request.xml ↔ SosRequestAdapter", 22, C["muted"], width=2840,
     source="app/src/main/res/layout/activity_send_sos.xml")
SOURCES.update({"app/src/main/res/layout/activity_realtime_tracking.xml", "app/src/main/res/layout/item_sos_request.xml"})
text(80, 4320, "Hiển thị dùng chung: values/themes.xml, styles.xml, colors.xml • ApiDisplay.java định dạng nhãn / thời gian.", 23, C["muted"], width=2840,
     source="app/src/main/java/com/javafinal/util/ApiDisplay.java")
SOURCES.update({"app/src/main/res/values/themes.xml", "app/src/main/res/values/styles.xml", "app/src/main/res/values/colors.xml"})
text(80, 4358, "Phạm vi: chọn file đại diện từ app/src/main; MVVM thực dụng, ViewModel gọi ApiClient, chưa có Repository / UseCase riêng.", 22, C["muted"], width=2840)
rect(80, 4390, 2840, 2, "#D7DFEB", radius=0)
text(80, 4408, "SOURCE: app/src/main/java/com/javafinal/  ·  res/  ·  AndroidManifest.xml  ·  app/build.gradle.kts", 20, C["muted"])
text(2465, 4408, "Đối chiếu source: 24/11/2026", 20, C["muted"])
SOURCES.add("app/src/main/AndroidManifest.xml")

for path in SOURCES:
    assert (ROOT / path).is_file(), path

OUT.mkdir(parents=True, exist_ok=True)
(OUT / "source/frontend-map.scene.json").write_text(json.dumps({
    "width": W, "height": H, "scale": 1.6, "shapes": S,
    "sources": sorted(SOURCES),
}, ensure_ascii=False, indent=2), encoding="utf-8")

# The SVG remains crisp at any zoom level and preserves selectable text.
svg = [f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
       '<title>SOS Rescue — Bản đồ kiến trúc front-end và luồng dữ liệu</title>',
       '<desc>Các file quan trọng, kế thừa UI, ViewModel, REST, WebSocket, GPS và bản đồ. Đối chiếu source ngày 24/11/2026.</desc>']
for s in S:
    kind = s["type"]
    if kind == "rect":
        svg.append(f'<rect x="{s["x"]}" y="{s["y"]}" width="{s["w"]}" height="{s["h"]}" rx="{s["radius"]}" fill="{s["fill"]}" stroke="{s["stroke"]}" stroke-width="{s["sw"]}"/>')
    elif kind == "line":
        points = " ".join(f"{x},{y}" for x, y in s["points"])
        dash = ' stroke-dasharray="10 8"' if s["dashed"] else ''
        svg.append(f'<polyline points="{points}" fill="none" stroke="{s["color"]}" stroke-width="{s["sw"]}" stroke-linejoin="round"{dash}/>')
    elif kind == "polygon":
        points = " ".join(f"{x},{y}" for x, y in s["points"])
        svg.append(f'<polygon points="{points}" fill="{s["fill"]}" stroke="{s["stroke"]}" stroke-width="{s["sw"]}"/>')
    elif kind == "text":
        if s["source"]:
            svg.append(f'<a xlink:href="../../{escape(s["source"])}"><title>{escape(s["source"])}</title>')
        svg.append(f'<text x="{s["x"]}" y="{s["y"] + s["size"]}" font-family="{s["font"]},sans-serif" font-size="{s["size"]}" font-weight="{700 if s["bold"] else 400}" fill="{s["color"]}">{escape(s["text"])}</text>')
        if s["source"]:
            svg.append('</a>')
svg.append('</svg>')
(OUT / "frontend-mindmap.svg").write_text("\n".join(svg), encoding="utf-8")
print(json.dumps({"svg": str(OUT / "frontend-mindmap.svg"), "sources_checked": len(SOURCES), "primitives": len(S)}, ensure_ascii=False))
