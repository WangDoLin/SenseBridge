# 🌉 SenseBridge — AI Accessibility Assistant

> **"Chuyển những gì người dùng khó cảm nhận thành một dạng tín hiệu mà họ có thể cảm nhận được."**

SenseBridge là ứng dụng Android mã nguồn mở biến điện thoại thông minh thành một **Bộ chuyển đổi tín hiệu giác quan On-Device (Signal Transformation Engine)** mà không cần bất kỳ phần cứng chuyên dụng nào:
- **Âm thanh (Microphone)** $\rightarrow$ Nhận diện còi xe, báo cháy $\rightarrow$ **Rung xúc giác + Màn hình đỏ**
- **Hình ảnh (Camera)** $\rightarrow$ Định vị xe cộ, người, bậc thang $\rightarrow$ **Đọc ngữ cảnh không gian qua Tai nghe Bluetooth**
- **Văn bản (OCR)** $\rightarrow$ Đọc biển hiệu, số phòng, menu $\rightarrow$ **Giọng đọc tiếng Việt tự nhiên**
- **Giao tiếp hai chiều (AAC)** $\rightarrow$ Thẻ câu khẩn cấp + Dịch lời người đối diện $\rightarrow$ **Phát âm loa ngoài + Chữ tương phản cao**

---

## 🏛️ KIẾN TRÚC HỆ THỐNG: SIGNAL TRANSFORMATION ENGINE

Thay vì xây dựng các tính năng phân mảnh cho từng dạng khuyết tật, SenseBridge hoạt động như một hệ thống xử lý luồng sự kiện hợp nhất:

```
                            ┌─────────────────────┐
                            │    INPUT SIGNALS    │
                            └──────────┬──────────┘
             ┌─────────────────────────┼─────────────────────────┐
             ▼                         ▼                         ▼
      Microphone (Audio)         Camera (Vision)           Touch / Saved Text
             │                         │                         │
             ▼                         ▼                         │
   [Decibel Energy Gate]        [Frame Throttle 10fps]           │
             │                         │                         │
             ▼                         ▼                         │
    [TFLite YAMNet Sound]       [ML Kit Object / OCR]            │
             │                         │                         │
             └─────────────────────────┼─────────────────────────┘
                                       ▼
                       ┌───────────────────────────────┐
                       │      CORE EVENT ENGINE        │
                       │ ───────────────────────────── │
                       │ • Priority Queue (P0 -> P3)   │
                       │ • Spatial Fusion (Vị trí)     │
                       │ • Smart Debouncer (Chống lặp) │
                       │ • Preemption (Cắt ngang lời)  │
                       └───────────────┬───────────────┘
                                       │
             ┌─────────────────────────┼─────────────────────────┐
             ▼                         ▼                         ▼
      [Haptic Dispatcher]       [Speech Dispatcher]       [UI/Screen Overlay]
      • P0: Rung liên tục       • TTS Audio Router        • High Contrast Text
      • P1: Double Pulse        • Bluetooth SCO/CommDevice• Large Flashing Box
      • P2: Single Tick         • Mute if Disconnected    • Fullscreen Color Alert
```

---

## ⚡ CÁC NGUYÊN TẮC KỸ THUẬT QUAN TRỌNG

1. **Quy tắc Haptic-First (Rung trước — Nói sau):**
   * Trong tình huống khẩn cấp (P0: Còi xe, Báo cháy), Android TTS mất $300\text{--}800\text{ ms}$ để tổng hợp âm thanh.
   * SenseBridge kích hoạt mô-tơ rung ngay tại mili-giây thứ $10$, sau đó TTS mới phát câu mô tả vào tai nghe.
2. **Ngắt lời khẩn cấp (Cross-Modal Preemption):**
   * Nếu người dùng đang nghe mô tả đồ vật thông thường (P3) mà có còi xe tải hoặc báo cháy (P0) xuất hiện $\rightarrow$ EventEngine gọi `stopImmediately()` để ngắt câu P3 ngay lập tức.
3. **Cổng năng lượng tiết kiệm pin (Duty-Cycling):**
   * `DecibelEnergyGate`: Chỉ phân tích AI âm thanh khi âm lượng vượt ngưỡng (ví dụ: $> 68\text{ dB}$ ngoài phố hoặc $> 55\text{ dB}$ trong phòng). Giảm tải CPU khi môi trường yên tĩnh.
   * `FrameRateThrottle`: Khóa phân tích CameraX ở mức 8–10 FPS ở độ phân giải VGA ($640 \times 480$), tự động bỏ frame nếu AI đang bận để chống nóng máy.
4. **Bảo vệ quyền riêng tư qua Bluetooth (Privacy Auto-Mute):**
   * Sử dụng `setCommunicationDevice()` của Android 12+. Nếu tai nghe Bluetooth bị ngắt kết nối đột ngột hoặc hết pin, app **lập tức MUTE** giọng đọc để tránh phát to thông tin riêng tư ra loa ngoài giữa nơi công cộng.
5. **Dung hợp đa giác quan (Multimodal Fusion):**
   * Camera thấy Xe ô tô + Mic nghe thấy Còi xe $\rightarrow$ Hợp nhất thành 1 thông báo duy nhất: *"Cảnh báo, xe phía trước đang bấm còi!"*.

---

## 🛠️ CÔNG NGHỆ (TECH STACK)

* **Language:** Kotlin 2.0 (100% On-Device, Không cần Server)
* **UI:** Jetpack Compose + Material 3 Accessibility (Độ tương phản cao, Nút bấm lớn $> 64\text{ dp}$, tương thích TalkBack)
* **Camera:** CameraX (PreviewView, ImageAnalysis)
* **Vision AI:** Google ML Kit Object Detection (Stream Mode + Tracking IDs) & Text Recognition (OCR)
* **Audio AI:** TensorFlow Lite / LiteRT Task Audio (YAMNet 16kHz mono)
* **Audio Routing:** Android `AudioManager.setCommunicationDevice()` (Bluetooth Headset / BLE Audio)
* **Haptics:** Android `VibrationEffect` (Tương thích cả LRA haptic độ nét cao lẫn ERM)
* **Local Database:** Room + SQLite (Lưu trữ và đồng bộ thẻ câu AAC)
* **Dependency Injection:** Dagger Hilt
* **Architecture:** Clean Architecture + MVVM + Unidirectional Data Flow

---

## 📂 CẤU TRÚC THƯ MỤC CHÍNH

```
app/src/main/java/com/sensebridge/
├── core/
│   ├── model/                  # SenseEvent, PriorityLevel, SpatialDirection
│   ├── engine/                 # EventEngine, SmartEventDebouncer, MultimodalSynthesizer
│   ├── dispatcher/             # SensoryDispatcher, OutputConfiguration
│   └── service/                # SenseBridgeForegroundService (Android 14+ FGS)
├── input/
│   ├── sound/                  # AudioRecorderManager, DecibelEnergyGate, TFLiteAudioClassifier
│   ├── vision/                 # CameraManager, ObjectDetectorEngine, SpatialContextEngine, OcrEngine
│   └── speech/                 # SpeechToTextManager
├── output/
│   ├── haptic/                 # AndroidHapticManager, HapticPattern
│   ├── audio/                  # AudioRouteManager, TextToSpeechManager
│   └── visual/                 # AlertOverlayManager
├── data/
│   ├── local/                  # SenseBridgeDatabase, PhraseDao, SavedPhraseEntity
│   └── repository/             # PhraseRepository
└── ui/
    ├── home/                   # HomeScreen, HomeViewModel (Panel giả lập tín hiệu)
    ├── deaf/                   # DeafAssistScreen (Radar âm thanh & nhịp rung)
    ├── blind/                  # BlindAssistScreen (Camera viewport & TTS định vị)
    ├── ocr/                    # OcrReaderScreen (Chạm để đọc biển báo)
    ├── communication/          # CommunicationScreen (Thẻ câu AAC & Nghe đối diện)
    └── settings/               # SettingsScreen (Cấu hình kênh ra & Cảnh báo an toàn)
```

---

## 🚀 CÁCH BUILD VÀ CHẠY DỰ ÁN

1. Mở thư mục dự án trong **Android Studio Ladybug (hoặc mới hơn)**.
2. Đợi Gradle đồng bộ các dependencies trong `gradle/libs.versions.toml`.
3. Kết nối thiết bị Android thật (khuyến nghị chạy Android 12+ có kết nối tai nghe Bluetooth).
4. Nhấn **Run (Shift + F10)**.
5. Cấp các quyền: Camera, Microphone, Bluetooth, Rung và Thông báo khi được yêu cầu.
6. Trên màn hình Home, sử dụng khu vực **⚡ KIỂM THỬ TÍN HIỆU** để kiểm tra phản hồi rung và giọng đọc Bluetooth tức thì!
