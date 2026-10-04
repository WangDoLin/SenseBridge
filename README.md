# SenseBridge - On-Device Multimodal Accessibility Platform

SenseBridge là nền tảng hỗ trợ tiếp cận đa giác quan mã nguồn mở trên hệ điều hành Android. Ứng dụng biến thiết bị di động thành một bộ chuyển đổi tín hiệu giác quan thời gian thực (Signal Transformation Engine) hoạt động hoàn toàn trên thiết bị (On-Device), không yêu cầu phần cứng ngoại vi chuyên dụng và không phụ thuộc vào kết nối Internet hay máy chủ đám mây.

Mục tiêu cốt lõi của hệ thống là: **Chuyển đổi các dạng tín hiệu môi trường mà người dùng gặp rào cản tiếp nhận thành các dạng tín hiệu thay thế phù hợp với khả năng cảm nhận của họ.**

---

## 1. Kiến trúc Hệ thống (Signal Transformation Architecture)

Hệ thống được thiết kế theo mô hình luồng sự kiện hướng dữ liệu hợp nhất (Unified Event-Driven Stream), loại bỏ kiến trúc phân mảnh truyền thống để tối ưu hóa tài nguyên phần cứng và độ trễ phản hồi:

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

## 2. Các Phân Hệ Kỹ Thuật Chính

### 2.1. Phân hệ Xử lý Âm thanh (Audio Intelligence Subsystem)
* **AudioRecorderManager**: Quản lý luồng thu âm 16kHz Mono PCM từ microphone thiết bị. Tích hợp cơ chế khóa phần cứng (Mutex Lock) giữa mô hình phân loại âm thanh TFLite và bộ nhận diện giọng nói `SpeechRecognizer` (STT), loại bỏ xung đột tranh chấp microphone trên hệ điều hành Android.
* **DecibelEnergyGate**: Bộ lọc năng lượng âm thanh động (Noise Gate). Chỉ chuyển dữ liệu âm thanh đến mô hình AI khi biên độ vượt ngưỡng cấu hình (mặc định 68 dB ngoài trời, 55 dB trong phòng), giảm hơn 60% chu kỳ đánh thức CPU khi môi trường xung quanh yên tĩnh.
* **TFLiteAudioClassifier**: Chạy mô hình YAMNet on-device thông qua TensorFlow Lite Task Audio library.
* **AudioLabelTaxonomy**: Bảng ánh xạ nhãn âm thanh chuẩn hóa kèm mức độ ưu tiên và nhãn hiển thị tiếng Việt (tiếng còi xe, chuông báo cháy, tiếng trẻ khóc, còi cứu thương, tiếng đập cửa, tiếng bước chân, tiếng kính vỡ).

### 2.2. Phân hệ Thị giác và Không gian (Vision & Spatial Context Subsystem)
* **CameraManager**: Điều phối vòng đời camera thông qua Jetpack CameraX với luồng `ImageAnalysis` (VGA 640x480 để xử lý AI độ trễ thấp) và `ImageCapture` (độ phân giải cao để phục vụ chụp quét văn bản).
* **ObjectDetectorEngine**: Nhận diện vật thể theo thời gian thực (Stream Mode) với cơ sở dữ liệu mô hình ML Kit, hỗ trợ theo dõi chuyển động (Tracking IDs) giữa các khung hình liên tiếp.
* **SpatialContextEngine**: Tính toán vị trí không gian của vật thể trong khung nhìn camera dựa trên tọa độ tâm bounding box và tỷ lệ kích thước:
  * Phương hướng ngang: Trái (Left), Trung tâm (Center), Phải (Right).
  * Khoảng cách ước tính: Rất gần (< 1m), Gần (1 - 2m), Vừa (2 - 4m), Xa (> 4m).
* **OcrEngine**: Trích xuất văn bản on-device bằng Google ML Kit Text Recognition hỗ trợ đầy đủ bộ ký tự tiếng Việt, tự động chuẩn hóa vùng quan tâm (ROI) và trích xuất khối văn bản có ý nghĩa.

### 2.3. Lõi Xử lý Sự kiện (Core Event Engine & Fusion)
* **EventEngine**: Trái tim điều phối của ứng dụng. Sử dụng Kotlin Coroutines `Channel(Channel.BUFFERED)` để tuần tự hóa mọi sự kiện từ các cảm biến, loại bỏ race conditions và khóa dữ liệu (thread-safe lock-free architecture).
* **SmartEventDebouncer**: Cơ chế chống trùng lặp sự kiện thông minh. Ngăn chặn hiện tượng cảnh báo lặp lại liên tục cho cùng một vật thể hoặc âm thanh trong một cửa sổ thời gian nhất định, đồng thời nhận diện sự thay đổi về phương hướng di chuyển. Có cơ chế bypass tức thì đối với các thao tác chủ động của người dùng (như phát âm thẻ AAC hoặc nhấn đọc OCR).
* **MultimodalSynthesizer**: Hợp nhất sự kiện đa phương thức. Khi hệ thống đồng thời nhận diện được hình ảnh (ví dụ: Xe ô tô phía trước) và âm thanh (tiếng còi xe cảnh báo), synthesizer tự động gộp thành một thông điệp cảnh báo duy nhất có độ ưu tiên cao nhất: *"Cảnh báo: Xe phía trước đang bấm còi!"*.
* **MonitoringCoordinator**: Điểm điều phối tập trung (Single Source of Truth) quản lý trạng thái kích hoạt, dừng và phục hồi của toàn bộ các cảm biến và dịch vụ nền.

### 2.4. Phân hệ Phản hồi & Điều phối Đa Kênh (Sensory Output Dispatchers)
* **AndroidHapticManager**: Điều khiển mô-tơ rung xúc giác thông qua `VibrationEffect`. Tương thích tối ưu cho cả bộ truyền động cộng hưởng tuyến tính (LRA) với các dải sóng xúc giác mượt mà lẫn mô-tơ quay lệch tâm (ERM). Tín hiệu xúc giác được kích hoạt trong vòng 10ms ngay khi phát hiện sự kiện nguy hiểm (P0) trước khi âm thanh giọng đọc kịp tạo ra (Haptic-First Principle).
* **AudioRouteManager**: Quản lý tuyến định tuyến âm thanh thông qua API `AudioManager.setCommunicationDevice()` trên Android 12+. Giữ âm thanh hướng dẫn không gian riêng tư bên trong tai nghe Bluetooth (SCO/BLE Audio).
* **Privacy Auto-Mute**: Khi tai nghe Bluetooth bị mất kết nối hoặc hết pin đột ngột, toàn bộ giọng đọc chỉ dẫn không gian tự động chuyển sang chế độ im lặng ngay lập tức để tránh phát to thông tin riêng tư ra ngoài môi trường.
* **TextToSpeechManager**: Điều phối giọng đọc Text-to-Speech hỗ trợ đa mức ưu tiên. Tích hợp cơ chế ngắt lời lập tức (Preemption): sự kiện P0 (khẩn cấp) ngay lập tức dừng các câu mô tả ngữ cảnh P2/P3 đang đọc dở. Cung cấp kênh phát âm riêng biệt ra loa ngoài (Loudspeaker Routing) cho tính năng giao tiếp AAC hai chiều.
* **AlertOverlayManager**: Hiển thị cảnh báo trực quan tương phản cao trên toàn màn hình với mã màu tương ứng (Đỏ cho P0, Cam cho P1, Vàng cho P2) cho người khiếm thính.

### 2.5. Phân hệ Giao tiếp Hai Chiều (AAC - Augmentative & Alternative Communication)
* **Thẻ giao tiếp khẩn cấp & thường dùng**: Hệ thống thẻ câu trực quan theo danh mục (Y tế, Hỗ trợ, Di chuyển, Nhu cầu cơ bản) với nút bấm kích thước lớn (> 64dp) và biểu tượng trực quan.
* **Tùy biến thẻ câu cá nhân**: Cho phép tạo, lưu trữ, chỉnh sửa và quản lý danh mục các câu nói thường ngày thông qua cơ sở dữ liệu cục bộ Room SQLite.
* **Bảng nhập văn bản trực tiếp (Type-to-Speak)**: Hỗ trợ người không thể nói nhập nội dung nhanh và nhấn phát âm tức thì qua loa ngoài.
* **Lắng nghe & chuyển đổi lời nói đối diện (STT)**: Chuyển lời nói của người đối diện thành văn bản hiển thị kích thước lớn trên màn hình với độ tương phản cao.

---

## 3. Quản lý Dữ liệu & Cấu hình Cục bộ

* **Jetpack DataStore (`UserPreferencesRepository`)**: Lưu trữ và quản lý phản ứng luồng (Flow) cho các cấu hình người dùng:
  * Ngưỡng năng lượng âm thanh (Decibel Threshold: 40 dB - 85 dB).
  * Độ nhạy mô hình nhận diện âm thanh (Confidence Threshold).
  * Cường độ phản hồi rung xúc giác (Haptic Intensity).
  * Tốc độ và cao độ giọng đọc TTS.
  * Tùy chọn bảo vệ quyền riêng tư Bluetooth Auto-Mute.
* **Room Database (`SenseBridgeDatabase`)**: Quản lý bảng dữ liệu `SavedPhraseEntity` thông qua `PhraseDao`, cung cấp các truy vấn phản ứng bất đồng bộ (Coroutines Flow) để nạp sẵn dữ liệu mặc định và đồng bộ thay đổi từ người dùng.

---

## 4. Cấu trúc Dự án

```
app/src/main/java/com/sensebridge/
├── MainActivity.kt                      # Khởi tạo Compose UI & điều hướng
├── SenseBridgeApp.kt                    # Application class khởi tạo Hilt container
├── core/
│   ├── dispatcher/
│   │   ├── OutputConfiguration.kt       # Cấu hình kênh đầu ra cho từng sự kiện
│   │   └── SensoryDispatcher.kt         # Bộ điều phối trung tâm gửi tín hiệu đến haptic/tts/ui
│   ├── engine/
│   │   ├── EventEngine.kt               # Hàng đợi sự kiện đa ưu tiên & preemption
│   │   ├── MultimodalSynthesizer.kt     # Hợp nhất sự kiện âm thanh + hình ảnh
│   │   └── SmartEventDebouncer.kt       # Bộ lọc chống lặp sự kiện theo không gian
│   ├── model/
│   │   ├── PriorityLevel.kt             # Phân cấp mức ưu tiên (P0, P1, P2, P3)
│   │   ├── SenseEvent.kt                # Dữ liệu sự kiện chuẩn hóa toàn hệ thống
│   │   └── SpatialDirection.kt          # Hướng và khoảng cách không gian
│   ├── monitoring/
│   │   └── MonitoringCoordinator.kt     # Quản lý vòng đời khởi động/dừng cảm biến
│   └── service/
│       └── SenseBridgeForegroundService.kt # Foreground service duy trì giám sát nền
├── data/
│   ├── local/
│   │   ├── SenseBridgeDatabase.kt       # Cơ sở dữ liệu Room SQLite
│   │   ├── dao/PhraseDao.kt             # DAO truy vấn thẻ câu giao tiếp
│   │   └── entity/SavedPhraseEntity.kt  # Entity lưu trữ câu AAC
│   └── repository/
│       ├── PhraseRepository.kt          # Repository quản lý dữ liệu câu nói AAC
│       └── UserPreferencesRepository.kt # Repository Jetpack DataStore cấu hình ứng dụng
├── di/
│   └── AppModule.kt                     # Hilt Dependency Injection module
├── input/
│   ├── sound/
│   │   ├── AudioLabelTaxonomy.kt        # Phân loại nhãn âm thanh YAMNet & mức ưu tiên
│   │   ├── AudioRecorderManager.kt      # Quản lý luồng AudioRecord & hardware mic mutex
│   │   ├── DecibelEnergyGate.kt         # Cổng lọc năng lượng âm thanh tiết kiệm pin
│   │   └── TFLiteAudioClassifier.kt     # Phân loại âm thanh on-device với YAMNet
│   ├── speech/
│   │   └── SpeechToTextManager.kt       # Nhận diện giọng nói người đối diện thành văn bản
│   └── vision/
│       ├── CameraManager.kt             # Quản lý CameraX Preview & ImageAnalysis
│       ├── ObjectDetectorEngine.kt      # Nhận diện vật thể theo thời gian thực (ML Kit)
│       ├── OcrEngine.kt                 # Nhận diện ký tự quang học (ML Kit OCR)
│       └── SpatialContextEngine.kt      # Tính toán hướng và khoảng cách vật thể
├── output/
│   ├── audio/
│   │   ├── AudioRouteManager.kt         # Quản lý tuyến Bluetooth & bảo vệ quyền riêng tư
│   │   └── TextToSpeechManager.kt       # Điều phối giọng đọc TTS & loa ngoài AAC
│   ├── haptic/
│   │   ├── AndroidHapticManager.kt      # Điều phối mô-tơ rung xúc giác thiết bị
│   │   └── HapticPattern.kt             # Định nghĩa mẫu nhịp rung chuẩn hóa
│   └── visual/
│       └── AlertOverlayManager.kt       # Quản lý hiển thị cảnh báo giao diện người dùng
└── ui/
    ├── blind/                           # Giao diện hỗ trợ khiếm thị & radar camera
    ├── communication/                   # Giao diện giao tiếp AAC & nghe đối diện
    ├── components/                      # Các thành phần UI tái sử dụng
    ├── deaf/                            # Giao diện hỗ trợ khiếm thính & visualizer âm thanh
    ├── home/                            # Bảng điều khiển trung tâm & bộ giả lập tín hiệu
    ├── navigation/                      # Cấu hình Navigation Compose & Bottom Bar
    ├── ocr/                             # Giao diện chụp quét và đọc văn bản
    ├── settings/                        # Giao diện cài đặt tham số kỹ thuật
    └── theme/                           # Bảng màu Material 3 tương phản cao
```

---

## 5. Công nghệ & Thư viện Sử dụng

* **Ngôn ngữ**: Kotlin 2.0 (JVM target 17).
* **Giao diện**: Jetpack Compose kết hợp Material Design 3 với chuẩn tiếp cận WCAG (độ tương phản cao, vùng chạm tối thiểu 48-64dp, hỗ trợ TalkBack).
* **Thị giác máy tính**: CameraX 1.3.4, Google ML Kit Object Detection & Text Recognition.
* **Xử lý âm thanh AI**: TensorFlow Lite / LiteRT Task Audio (YAMNet 16kHz).
* **Tiếp cận phần cứng**: Android AudioRecord, VibrationEffect (LRA/ERM), AudioManager Bluetooth Communication Device API.
* **Lưu trữ dữ liệu**: Room Database 2.6.1 (SQLite), Jetpack DataStore Preferences 1.1.1.
* **Kiến trúc phần mềm**: Clean Architecture kết hợp MVVM, Unidirectional Data Flow (UDF), Dagger Hilt 2.51.1.
* **Tối ưu hóa mã nguồn**: R8 Code & Resource Shrinking với ProGuard rules tùy biến cho TFLite, ML Kit, Room và Coroutines.

---

## 6. Hướng dẫn Biên dịch & Chạy Dự án

### Yêu cầu môi trường
* **Hệ điều hành**: Windows, macOS, hoặc Linux.
* **JDK**: OpenJDK 21 (đã cấu hình cố định qua `gradle/gradle-daemon-jvm.properties`).
* **Android Studio**: Android Studio Ladybug (2024.2.1) hoặc mới hơn.
* **Android SDK**: Compile SDK 34, Target SDK 34, Min SDK 26 (Android 8.0 trở lên). Thiết bị thử nghiệm khuyến nghị chạy Android 12+ có kết nối tai nghe Bluetooth.

### Các lệnh thực thi chính

1. **Kiểm tra và chạy bộ kiểm thử đơn vị (Unit Tests)**:
   ```bash
   ./gradlew test --offline --console=plain
   ```
   *Toàn bộ 60 test cases kiểm thử tự động (EventEngine, Debouncer, Synthesizer, Taxonomy, Repositories) đều vượt qua thành công.*

2. **Biên dịch bản Debug APK**:
   ```bash
   ./gradlew assembleDebug --offline --console=plain
   ```
   *File xuất ra tại: `app/build/outputs/apk/debug/app-debug.apk`*

3. **Biên dịch bản Release APK đã tối ưu hóa R8 & ký số**:
   ```bash
   ./gradlew assembleRelease --offline --console=plain
   ```
   *File xuất ra tại: `app/build/outputs/apk/release/app-release.apk` (Dung lượng: ~125.9 MB, đã ký debug keystore sẵn sàng cài đặt sideload qua `adb install`).*

---

## 7. Nguyên tắc Bảo mật & Quyền Riêng tư

1. **100% On-Device Processing**: Mọi quá trình thu âm micro, xử lý hình ảnh camera và nhận diện văn bản OCR đều diễn ra trực tiếp trong bộ nhớ RAM của thiết bị. Ứng dụng không truyền tải bất kỳ dữ liệu âm thanh, hình ảnh hay thông tin người dùng nào ra ngoài mạng Internet.
2. **Quyền hạn tối thiểu**: Quyền dịch vụ nền (Foreground Service) loại `microphone` được giới hạn chặt chẽ chỉ kích hoạt khi người dùng bật tính năng giám sát âm thanh môi trường.
3. **Bảo vệ ranh giới cá nhân**: Giọng đọc thông tin không gian xung quanh được ưu tiên cô lập trong tai nghe cá nhân, tự ngắt khi ngắt kết nối để bảo vệ sự riêng tư của người khiếm thị ở nơi công cộng.
