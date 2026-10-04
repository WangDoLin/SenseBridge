# SenseBridge - On-Device Multimodal Accessibility Platform

SenseBridge là nền tảng đang trong giai đoạn alpha giúp hỗ trợ tiếp cận đa giác quan mã nguồn mở trên hệ điều hành Android. Ứng dụng biến thiết bị di động thành một bộ chuyển đổi tín hiệu giác quan thời gian thực (Signal Transformation Engine) hoạt động hoàn toàn trên thiết bị (On-Device), không cần phần cứng ngoại vi và không phụ thuộc vào kết nối mạng.

Hệ thống chuyển đổi các tín hiệu môi trường khó tiếp nhận thành các dạng tín hiệu thay thế phù hợp với khả năng cảm nhận của người dùng.

---

## 1. Kiến trúc Hệ thống

```
                            +-----------------------+
                            |     INPUT SIGNALS     |
                            +-----------+-----------+
             +--------------------------+--------------------------+
             |                          |                          |
             v                          v                          v
    Microphone (Audio)           Camera (Vision)             Touch / User Input
             |                          |                          |
             v                          v                          |
   [Decibel Energy Gate]     [Frame Rate Throttle 10fps]           |
             |                          |                          |
             v                          v                          |
    [TFLite YAMNet Sound]     [ML Kit Object & OCR Engine]         |
             |                          |                          |
             +--------------------------+--------------------------+
                                        |
                                        v
                        +-------------------------------+
                        |       CORE EVENT ENGINE       |
                        | ----------------------------- |
                        | - Serialized Channel Queue    |
                        | - Priority Queue (P0 -> P3)   |
                        | - Multimodal Spatial Fusion   |
                        | - Smart Spatial Debouncer     |
                        | - Cross-Modal Preemption      |
                        +---------------+---------------+
                                        |
             +--------------------------+--------------------------+
             |                          |                          |
             v                          v                          v
    [Haptic Dispatcher]        [Speech Dispatcher]        [Visual Alert Overlay]
    - P0: Continuous Alert     - Priority TTS Router      - High Contrast HUD
    - P1: Double Pulse         - Bluetooth CommDevice     - Directional Indicator
    - P2: Single Pulse         - Loudspeaker AAC Bypass   - Fullscreen Flashing
    - P3: Gentle Tick          - Privacy Disconnect Mute  - Text-to-Speech Subtitles
```

---

## 2. Các Phân Hệ Kỹ Thuật

### 2.1. Xử lý Âm thanh
* **AudioRecorderManager**: Thu âm 16kHz Mono PCM, quản lý mutex tránh xung đột microphone giữa TFLite và SpeechRecognizer.
* **DecibelEnergyGate**: Lọc ngưỡng năng lượng âm thanh (40–85 dB) để giảm chu kỳ xử lý AI khi môi trường yên tĩnh.
* **TFLiteAudioClassifier**: Phân loại âm thanh on-device qua mô hình YAMNet bằng TensorFlow Lite Task Audio.
* **AudioLabelTaxonomy**: Ánh xạ phân loại âm thanh sang 4 mức ưu tiên (P0–P3) và nhãn hiển thị tiếng Việt.

### 2.2. Thị giác và Không gian
* **CameraManager**: Quản lý luồng CameraX ImageAnalysis (VGA 640x480) và ImageCapture độ phân giải cao.
* **ObjectDetectorEngine**: Nhận diện và theo dõi vật thể thời gian thực bằng Google ML Kit Object Detection.
* **SpatialContextEngine**: Xác định vị trí (Trái, Giữa, Phải) và ước tính cự ly dựa trên bounding box.
* **OcrEngine**: Trích xuất văn bản tiếng Việt on-device bằng Google ML Kit Text Recognition.

### 2.3. Lõi Xử lý Sự kiện
* **EventEngine**: Hàng đợi tuần tự hóa Coroutine Channel, quản lý 4 cấp độ ưu tiên và cơ chế ngắt lời (Preemption).
* **SmartEventDebouncer**: Chống lặp cảnh báo theo cửa sổ thời gian và tọa độ; hỗ trợ bypass cho thao tác chủ động.
* **MultimodalSynthesizer**: Hợp nhất sự kiện âm thanh và hình ảnh đồng thời thành một cảnh báo duy nhất.
* **MonitoringCoordinator**: Điều phối tập trung trạng thái hoạt động của toàn bộ cảm biến và dịch vụ nền.

### 2.4. Điều phối Đầu ra
* **AndroidHapticManager**: Điều khiển mô-tơ rung xúc giác (VibrationEffect LRA/ERM), kích hoạt trong vòng 10ms khi có nguy hiểm.
* **AudioRouteManager**: Định tuyến âm thanh qua tai nghe Bluetooth; tự động ngắt tiếng (Auto-Mute) khi ngắt kết nối.
* **TextToSpeechManager**: Điều phối giọng đọc theo độ ưu tiên; hỗ trợ phát âm loa ngoài riêng cho giao tiếp AAC.
* **AlertOverlayManager**: Hiển thị cảnh báo trực quan tương phản cao toàn màn hình theo mã màu ưu tiên.

### 2.5. Giao tiếp Hai Chiều (AAC)
* **Thẻ giao tiếp**: Danh mục câu nói thông dụng và khẩn cấp với phím bấm lớn (> 64dp).
* **Quản lý câu tùy biến**: Tạo, lưu trữ và chỉnh sửa danh mục câu nói cá nhân qua Room Database.
* **Type-to-Speak**: Nhập văn bản trực tiếp và phát âm nhanh qua loa ngoài.
* **Chuyển lời nói thành chữ (STT)**: Chuyển đổi giọng nói của người đối diện thành văn bản kích thước lớn.

---

## 3. Quản lý Dữ liệu

* **UserPreferencesRepository (DataStore)**: Lưu trữ cấu hình ngưỡng decibel, độ nhạy, cường độ rung và chính sách riêng tư.
* **PhraseRepository (Room SQLite)**: Quản lý cơ sở dữ liệu thẻ câu giao tiếp AAC với khả năng đồng bộ luồng Flow.

---

## 4. Cấu trúc Thư mục

```
app/src/main/java/com/sensebridge/
├── MainActivity.kt                      # Điểm khởi tạo Compose UI & điều hướng
├── SenseBridgeApp.kt                    # Application class khởi tạo Hilt container
├── core/
│   ├── dispatcher/                      # SensoryDispatcher, OutputConfiguration
│   ├── engine/                          # EventEngine, MultimodalSynthesizer, SmartEventDebouncer
│   ├── model/                           # SenseEvent, PriorityLevel, SpatialDirection
│   ├── monitoring/                      # MonitoringCoordinator
│   └── service/                         # SenseBridgeForegroundService
├── data/
│   ├── local/                           # SenseBridgeDatabase, PhraseDao, SavedPhraseEntity
│   └── repository/                      # PhraseRepository, UserPreferencesRepository
├── di/                                  # AppModule (Dagger Hilt)
├── input/
│   ├── sound/                           # AudioRecorderManager, DecibelEnergyGate, TFLiteAudioClassifier
│   ├── speech/                          # SpeechToTextManager
│   └── vision/                          # CameraManager, ObjectDetectorEngine, SpatialContextEngine, OcrEngine
├── output/
│   ├── audio/                           # AudioRouteManager, TextToSpeechManager
│   ├── haptic/                          # AndroidHapticManager, HapticPattern
│   └── visual/                          # AlertOverlayManager
└── ui/
    ├── blind/                           # Giao diện hỗ trợ thị giác
    ├── communication/                   # Giao diện giao tiếp AAC & STT
    ├── components/                      # UI components dùng chung
    ├── deaf/                            # Giao diện radar âm thanh
    ├── home/                            # Bảng điều khiển & mô phỏng tín hiệu
    ├── navigation/                      # Navigation Compose & Bottom Bar
    ├── ocr/                             # Giao diện quét văn bản
    ├── settings/                        # Cài đặt tham số hệ thống
    └── theme/                           # Bảng màu Material 3 tương phản cao
```

---

## 5. Công nghệ Sử dụng

* **Ngôn ngữ**: Kotlin 2.0 (JVM 17).
* **Giao diện**: Jetpack Compose, Material 3, tiếp cận WCAG.
* **Thị giác**: CameraX 1.3.4, Google ML Kit (Object Detection, Text Recognition).
* **Âm thanh**: TensorFlow Lite Task Audio (YAMNet 16kHz).
* **Phần cứng**: AudioRecord, VibrationEffect (LRA/ERM), AudioManager Bluetooth Communication Device API.
* **Dữ liệu**: Room 2.6.1, DataStore Preferences 1.1.1.
* **Kiến trúc**: Clean Architecture, MVVM, Dagger Hilt 2.51.1.
* **Tối ưu hóa**: R8 Code & Resource Shrinking, ProGuard custom rules.

---

## 6. Biên dịch và Kiểm thử

### Yêu cầu môi trường
* **JDK**: OpenJDK 21
* **Android SDK**: Compile/Target SDK 34, Min SDK 26 (Android 8.0+)
* **Android Studio**: Ladybug (2024.2.1) trở lên

### Lệnh thực thi

* **Chạy bộ kiểm thử đơn vị (60 tests)**:
  ```bash
  ./gradlew test --offline --console=plain
  ```

* **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug --offline --console=plain
  ```
  Output: `app/build/outputs/apk/debug/app-debug.apk`

* **Build Release APK (R8 optimized & debug signed)**:
  ```bash
  ./gradlew assembleRelease --offline --console=plain
  ```
  Output: `app/build/outputs/apk/release/app-release.apk` (125.9 MB)

---

## 7. Bảo mật & Quyền Riêng tư

* **100% On-Device**: Xử lý dữ liệu trực tiếp trong bộ nhớ thiết bị; không gửi dữ liệu ra ngoài Internet.
* **Phân quyền tối thiểu**: Quyền micro nền chỉ hoạt động khi người dùng kích hoạt tính năng giám sát.
* **Cô lập âm thanh**: Giọng đọc hướng dẫn phát qua tai nghe cá nhân và tự động ngắt tiếng khi mất kết nối Bluetooth.
