# SOS Rescue Android App (Frontend)

Ứng dụng di động cứu hộ khẩn cấp dành cho nền tảng **Android**, được phát triển bằng **Java Native + Jetpack (Lifecycle, ViewModel, LiveData) + MapLibre Native SDK + OkHttp (HTTP/WebSocket) + Gson**.

---

## 1. Kiến trúc & Công nghệ

- **Mô hình kiến trúc**: **MVVM (Model - View - ViewModel)** kết hợp phân tầng rõ ràng:
  - `com.javafinal.model`: Các Data Transfer Object (DTO) tương ứng với 10 bảng của cơ sở dữ liệu và các phản hồi API.
  - `com.javafinal.viewmodel`: Tầng trung gian xử lý nghiệp vụ, quản lý trạng thái (`ApiState<T>`) qua `LiveData`, độc lập với vòng đời giao diện và giữ trạng thái qua các sự kiện xoay màn hình.
  - `com.javafinal.network`: `ApiClient` (OkHttp 4), `SessionStore` (lưu trữ token JWT mã hóa), và cơ chế kết nối `WebSocket` thời gian thực.
  - `com.javafinal.ui`: 10 màn hình chuẩn theo tài liệu nghiệp vụ đồ án chia theo hai nhóm đối tượng: Người dân (`ui/user`) và Đội cứu hộ (`ui/rescuer`), cùng luồng xác thực (`ui/auth`).
  - `com.javafinal.util`: Xử lý bản đồ `MapHelper` (MapLibre vector markers, polyline), định vị GPS `DeviceLocation` (Fused Location Provider) và `NavigationHelper`.
- **Bản đồ số**: **MapLibre Native Android SDK** (Mã nguồn mở, hiển thị bản đồ vector mượt mà, hỗ trợ marker tùy biến và vẽ đường lộ trình Polyline di chuyển của đội cứu hộ).
- **Định vị & Real-time**: Tích hợp Google Play Services Location kết hợp WebSocket hai chiều để đồng bộ tọa độ GPS thời gian thực giữa Nạn nhân và Đội cứu hộ.

---

## 2. Cấu trúc Thư mục Nguồn

```text
frontend/app/src/main/java/com/javafinal/
├── MainActivity.java              # Entry point điều hướng sang SplashActivity
├── model/                         # DTO dữ liệu (Auth, SOS, Team, Log, WebSocket...)
├── network/
│   ├── ApiClient.java             # OkHttp Client cấu hình timeout, header và interceptor
│   ├── PreferencesSessionStore.java # Lưu trữ an toàn JWT Token & thông tin tài khoản
│   └── RealtimeProtocol.java      # Định nghĩa các loại sự kiện WebSocket
├── util/
│   ├── DeviceLocation.java        # Fused Location Provider lấy GPS chính xác từ thiết bị
│   ├── MapHelper.java             # Quản lý lớp bản đồ, Camera, Marker và Polyline
│   ├── NavigationHelper.java      # Quản lý điều hướng, Dialog hồ sơ, thông báo
│   └── SosMapData.java            # Bộ tính toán tọa độ, khoảng cách Haversine
├── viewmodel/
│   ├── AuthViewModel.java         # Quản lý luồng gửi OTP, xác minh OTP, lưu session
│   ├── SosViewModel.java          # Tạo SOS, tải ca đang hoạt động, bản đồ cộng đồng
│   ├── RescuerViewModel.java      # Hàng đợi cứu hộ, nhận ca, cập nhật tiến trình, nhả ca
│   ├── LocationViewModel.java     # Gửi tọa độ GPS realtime, lấy vị trí 2 bên và lịch sử trail
│   ├── RealtimeViewModel.java     # Quản lý kết nối WebSocket và phân phối sự kiện
│   ├── TeamViewModel.java         # Quản lý thành viên đội cứu hộ (LEADER/MEMBER)
│   ├── UserViewModel.java         # Quản lý hồ sơ và cập nhật tên hiển thị
│   └── NotificationViewModel.java # Quản lý thông báo người dùng
└── ui/
    ├── splash/SplashActivity.java # Màn 1: Kiểm tra /health & phiên đăng nhập
    ├── auth/LoginActivity.java    # Màn 2: Đăng nhập OTP Telegram 2 bước (USER / RESCUER)
    ├── user/
    │   ├── HomeActivity.java      # Màn 3: Trang chủ người dân & Bản đồ cộng đồng
    │   ├── SendSosActivity.java   # Màn 4: Tạo yêu cầu cứu hộ SOS
    │   ├── TrackSosActivity.java  # Màn 5: Theo dõi tiến trình cứu hộ thời gian thực
    │   └── SosResultActivity.java # Màn 6: Báo cáo kết quả và nhật ký ca cứu hộ
    └── rescuer/
        ├── RescuerHomeActivity.java      # Màn 7: Hàng đợi tiếp nhận cứu hộ
        ├── SosDetailActivity.java        # Màn 8: Xem chi tiết yêu cầu SOS và thông tin liên hệ
        ├── RealtimeTrackingActivity.java # Màn 9: Bản đồ điều phối GPS & Cập nhật tiến trình
        ├── ConfirmCompleteActivity.java  # Màn 10: Nghiệm thu và hoàn thành ca cứu hộ
        └── RescueTeamActivity.java       # Quản lý thành viên đội cứu hộ
```

---

## 3. Cấu hình Môi trường

### 3.1 Cấu hình Android SDK (`local.properties`)
Tạo hoặc kiểm tra file `frontend/local.properties`:
```properties
sdk.dir=D\:\\Android\\Sdk
```
*(Thay đổi đường dẫn phù hợp với vị trí Android SDK trên máy)*

### 3.2 Cấu hình Địa chỉ Máy chủ Backend (`gradle.properties`)
Mở file `frontend/gradle.properties`:
- Khi chạy trên **Máy thật Android** (cùng mạng Wi-Fi với máy tính chạy backend):
  ```properties
  API_BASE_URL=http://192.168.1.3:8000/
  ```
  *(Thay `192.168.1.3` bằng địa chỉ IP Wi-Fi nội bộ của máy chủ backend).*
- Khi chạy trên **Máy ảo Android Emulator** (Android Studio):
  ```properties
  API_BASE_URL=http://10.0.2.2:8000/
  ```

---

## 4. Biên dịch & Đóng gói

Chạy bằng PowerShell từ thư mục `frontend`:

### Kiểm tra biên dịch mã nguồn:
```powershell
cd frontend
.\gradlew.bat compileDebugSources
```

### Chạy Unit Test tự động:
```powershell
.\gradlew.bat testDebugUnitTest
```

### Đóng gói file cài đặt APK (Debug):
```powershell
.\gradlew.bat assembleDebug
```
- File APK xuất xưởng tại: `app/build/outputs/apk/debug/app-debug.apk`

### Cài đặt nhanh vào thiết bị đang kết nối (qua ADB):
```powershell
& "D:\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
```
Hoặc mở thư mục `frontend` bằng **Android Studio** và nhấn nút **Run 'app'** (`Shift + F10`).
