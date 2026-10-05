# SenseBridge - On-Device Multimodal Accessibility & Cognitive AI Platform

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![TensorFlow Lite](https://img.shields.io/badge/AI-TensorFlow_Lite-orange.svg)](https://www.tensorflow.org/lite)
[![MediaPipe](https://img.shields.io/badge/GenAI-MediaPipe_Tasks-red.svg)](https://ai.google.dev/edge/mediapipe)

**SenseBridge** là nền tảng hỗ trợ tiếp cận đa giác quan và trợ lý trí tuệ nhân tạo nhận thức tình huống (Situational Cognitive AI) hoạt động **100% On-Device** trên hệ điều hành Android. 

Ứng dụng đóng vai trò như một bộ chuyển đổi giác quan thời gian thực (Real-time Sensory Transformation Engine), đồng thời tích hợp **mô hình AI tự học, tự thích ứng và đàm thoại tự do**, biến thiết bị di động thành giác quan số hỗ trợ người khiếm thị và người khiếm thính mà không cần kết nối mạng hay phần cứng chuyên dụng.

---

## 1. Kiến Trúc Tổng Quan Hệ Thống

```
                               +-----------------------------------+
                               |       MULTIMODAL INPUTS           |
                               +-----------------+-----------------+
                +--------------------------------+--------------------------------+
                |                                |                                |
                v                                v                                v
       Microphone (Audio)                 Camera (Vision)                 Touch / User Input
                |                                |                                |
                v                                v                                |
      [Decibel Energy Gate]            [CameraX Stream 10fps]                     |
                |                                |                                |
                v                                v                                |
      [TFLite YAMNet Sound]             [ML Kit Object Detection]                 |
      - Còi xe, cứu thương              - Khoảng cách & Hướng (L/C/R)             |
      - Tiếng la hét, tiếng nói         [ML Kit OCR with Crop Box]                |
                |                       - Quét biển báo, phòng, bảng              |
                +--------------------------------+--------------------------------+
                                                 |
                                                 v
                       +---------------------------------------------------+
                       |        MULTIMODAL COGNITIVE SCENE MEMORY          |
                       |       (Cửa sổ trượt thời gian thực 6 giây)        |
                       +-------------------------+-------------------------+
                                                 |
                   +-----------------------------+-----------------------------+
                   v                                                           v
   +-------------------------------+                           +-------------------------------+
   |    AiMultimodalReasoner       |                           |      SenseAiDialogueModel     |
   | ----------------------------- |                           | ----------------------------- |
   | - Đánh giá nguy cơ chéo P0-P3 |                           | - 6 Tình huống cơ bản         |
   | - Phân tích va chạm & bậc dốc |                           | - Bộ nhớ đối thoại đa lượt    |
   +---------------+---------------+                           +---------------+---------------+
                   |                                                           |
                   |                                           +---------------+---------------+
                   |                                           v                               v
                   |                           +-------------------------------+  +-------------------------------+
                   |                           |    SenseAiNeuralClassifier    |  |  OnDeviceSlmInferenceEngine   |
                   |                           | ----------------------------- |  | ----------------------------- |
                   |                           | - Mạng nơ-ron TFLite (48.4KB) |  | - MediaPipe Tasks GenAI       |
                   |                           | - 90.37% Accuracy, 10 Classes |  | - Sinh lời thoại tự do (SLM)  |
                   |                           +---------------+---------------+  +---------------+---------------+
                   |                                           |                               |
                   |                                           +---------------+---------------+
                   |                                                           |
                   |                                                           v
                   |                                           +-------------------------------+
                   |                                           |    ContinualLearningEngine    |
                   |                                           | ----------------------------- |
                   |                                           | - Tự học cách diễn đạt user   |
                   |                                           | - Trọng số thích ứng online   |
                   |                                           | - Ký ức địa điểm đã từng đến  |
                   |                                           +---------------+---------------+
                   |                                                           |
                   +-----------------------------+-----------------------------+
                                                 |
                                                 v
                       +---------------------------------------------------+
                       |             CORE EVENT DISPATCH ENGINE            |
                       | ------------------------------------------------- |
                       | - Serialized Priority Queue (P0 -> P3)            |
                       | - Adaptive Speech Cooldown & Spam Filter          |
                       | - Cross-Modal Preemption (Ngắt lời khi khẩn cấp)  |
                       +-------------------------+-------------------------+
                                                 |
                +--------------------------------+--------------------------------+
                |                                |                                |
                v                                v                                v
       [Haptic Dispatcher]              [Speech Dispatcher]            [Visual Alert Overlay]
       - P0: Rung liên tục khẩn cấp     - Giọng đọc TTS đa mức         - HUD tương phản cao
       - P1: Xung kép cảnh báo          - Tự ngắt khi rời Bluetooth    - Đèn chớp toàn màn hình
       - P2/P3: Xung đơn định hướng     - Loa ngoài cho giao tiếp AAC  - Phụ đề âm thanh & chữ lớn
```

---

## 2. Các Trụ Cột Kỹ Thuật

### 2.1. Trí Tuệ Nhân Tạo Nhận Thức Tình Huống & Tự Học (Cognitive Edge AI)
* **MultimodalSceneMemory**: Lưu trữ bối cảnh đa giác quan (vật thể, chữ đọc được, âm thanh, decibel) theo cửa sổ trượt 6 giây tự động dọn dẹp.
* **AiMultimodalReasoner**: Đánh giá tương quan chéo giữa hình ảnh và âm thanh (ví dụ: phát hiện xe ô tô + tiếng còi xe $\rightarrow$ cảnh báo P0 nguy cơ đâm va; phát hiện bậc thang cự ly gần $\rightarrow$ cảnh báo P1 té ngã).
* **SenseAiNeuralClassifier (TFLite)**: Mạng nơ-ron phân loại ý định Deep Learning tự huấn luyện, kích thước siêu nhẹ **48.4 KB**, độ chính xác **90.37%**, nhúng sẵn trong `assets/` và tính toán qua TFLite Interpreter.
* **OnDeviceSlmInferenceEngine (MediaPipe Tasks GenAI)**: Động cơ suy luận mô hình ngôn ngữ nhỏ (SLM như Gemma, SmolLM, Qwen) chạy trực tiếp trên CPU/GPU điện thoại, cho phép AI tự do tư duy và sinh lời thoại tự nhiên không theo khuôn mẫu.
* **ContinualLearningEngine (Học Tăng Cường & Thích Ứng Cá Nhân Hóa)**:
  * **Tự học cách nói của người dùng**: Tự động học các từ ngữ cá nhân, tiếng địa phương và tăng trọng số liên kết thích ứng (`confidenceWeight` tăng dần sau mỗi lần dùng).
  * **Ký ức địa điểm dài hạn (Episodic Location Memory)**: Ghi nhớ các biển báo, địa điểm người dùng từng đi qua và tự động nhắc lại khi quay lại chốn cũ.
  * **Bảo mật tuyệt đối**: Dữ liệu tự học lưu trữ 100% trong Room Database cục bộ, không gửi lên Cloud.

### 2.2. Xử Lý Âm Thanh Môi Trường
* **AudioRecorderManager**: Thu âm 16kHz Mono PCM, quản lý luồng ghi âm ngầm và chống xung đột microphone.
* **DecibelEnergyGate**: Lọc ngưỡng decibel (40–85 dB) thông minh, giúp điện thoại tiết kiệm pin tối đa khi môi trường yên tĩnh.
* **TFLiteAudioClassifier (YAMNet)**: Phân loại hơn 500 sự kiện âm thanh môi trường (còi xe, còi báo cháy, cứu thương, tiếng la hét, tiếng người nói).

### 2.3. Thị Giác Máy Tính & OCR Nâng Cao
* **CameraManager**: Điều phối CameraX với luồng ImageAnalysis 10fps tối ưu nhiệt độ và ImageCapture độ nét cao.
* **ObjectDetectorEngine**: Nhận diện vật thể, tính toán hướng không gian (Trái, Giữa, Phải) và phân loại cự ly Gần/Xa.
* **OcrEngine (Khử nhiễu & Cắt khung trung tâm)**:
  * Thuật toán Viewport Cropping: Tập trung nhận diện văn bản ở vùng trung tâm tầm nhìn.
  * Bộ lọc nhiễu Text Sanitizer: Loại bỏ hoàn toàn các ký tự rác (##, ký tự dị thường, nhiễu quang học), đảm bảo phát âm chính xác các biển báo và số hiệu phòng.

### 2.4. Điều Phối Phản Hồi Đa Giác Quan
* **AndroidHapticManager**: Rung xúc giác LRA/ERM với các pattern chuyên biệt, kích hoạt cảnh báo trong vòng dưới 10ms.
* **TextToSpeechManager**: Điều phối giọng nói tiếng Việt theo 4 mức ưu tiên, hỗ trợ định tuyến âm thanh thông minh và ngắt tiếng khi mất kết nối Bluetooth cá nhân.
* **AlertOverlayManager**: Màn hình cảnh báo chớp nháy màu tương phản cao (High Contrast HUD) dành cho người khiếm thính.
* **SenseBridgeForegroundService**: Chạy ngầm bền bỉ bảo vệ người dùng ngay cả khi tắt màn hình hoặc bỏ điện thoại trong túi.

### 2.5. Giao Tiếp Tăng Cường Hai Chiều (AAC & STT)
* **Thẻ giao tiếp nhanh**: Phím bấm lớn (> 64dp) hỗ trợ người mất khả năng phát âm giao tiếp nhanh với người xung quanh.
* **Type-to-Speak**: Chuyển văn bản thành lời nói phát âm lớn qua loa ngoài.
* **Speech-to-Text (STT)**: Chuyển lời nói của người đối diện thành văn bản cỡ lớn trên màn hình.

---

## 3. Cấu Trúc Mã Nguồn

```
e:/Health/
├── ml/                                  # Pipeline huấn luyện Machine Learning
│   ├── dataset/
│   │   └── intent_dataset_vi.json       # Tập dữ liệu huấn luyện tiếng Việt
│   └── train_sense_ai_model.py          # Script huấn luyện Keras -> Xuất TFLite
├── app/src/main/
│   ├── assets/                          # Mô hình AI đóng gói sẵn trong APK
│   │   ├── sense_ai_neural_model.tflite # Model nơ-ron phân loại ý định (48.4 KB)
│   │   ├── sense_ai_vocab.json          # Từ điển Tokenizer 218 từ
│   │   ├── sense_ai_labels.json         # Danh sách 10 phân lớp ý định
│   │   └── yamnet.tflite                # Model phân loại âm thanh YAMNet
│   └── java/com/sensebridge/
│       ├── core/
│       │   ├── ai/                      # Lõi Trí Tuệ Nhân Tạo Nhận Thức
│       │   │   ├── AiMultimodalReasoner.kt          # Phân tích nguy cơ chéo
│       │   │   ├── ContinualLearningEngine.kt       # Tự học & Bộ nhớ dài hạn
│       │   │   ├── MultimodalAiAssistantEngine.kt   # Trợ lý giác quan trung tâm
│       │   │   ├── MultimodalSceneMemory.kt         # Bộ nhớ ngữ cảnh trượt 6s
│       │   │   ├── OnDeviceSlmInferenceEngine.kt    # MediaPipe GenAI SLM Engine
│       │   │   ├── SenseAiDialogueModel.kt          # Hội thoại & Tình huống
│       │   │   ├── SenseAiNeuralClassifier.kt       # Bộ suy luận nơ-ron TFLite
│       │   │   └── SlmModelManager.kt               # Quản lý file model & RAM
│       │   ├── engine/                  # EventEngine, MultimodalSynthesizer
│       │   ├── model/                   # SenseEvent, PriorityLevel, SensorySource
│       │   ├── monitoring/              # MonitoringCoordinator
│       │   └── service/                 # SenseBridgeForegroundService
│       ├── data/
│       │   ├── local/                   # Room Database v2 (Sessions & Patterns)
│       │   │   ├── dao/                 # UserAiSessionDao, LearnedUserPatternDao
│       │   │   └── entity/              # UserAiSessionEntity, LearnedUserPatternEntity
│       │   └── repository/              # UserPreferencesRepository, PhraseRepository
│       ├── di/                          # Dagger Hilt Modules (Database, Sensors)
│       ├── input/                       # CameraX, Sound Recorder, ML Kit Vision
│       ├── output/                      # Haptic, Bluetooth Audio, Alert Overlay
│       └── ui/                          # Jetpack Compose UI (Blind, Deaf, AAC, Home)
```

---

## 4. Hướng Dẫn Tự Huấn Luyện & Tùy Biến Model AI

Dự án cung cấp sẵn pipeline Python hoàn chỉnh để huấn luyện lại mạng nơ-ron của riêng bạn:

### 1. Chuẩn bị môi trường Python:
```bash
pip install tensorflow-cpu numpy
```

### 2. Chạy kịch bản huấn luyện:
```bash
python ml/train_sense_ai_model.py
```
Quá trình huấn luyện sẽ tự động:
1. Đọc dữ liệu từ `ml/dataset/intent_dataset_vi.json`.
2. Huấn luyện mạng nơ-ron sâu với các tầng Embedding, Global Pooling, Dense và Dropout.
3. Xuất trực tiếp file mô hình nhị phân `sense_ai_neural_model.tflite` cùng từ điển `sense_ai_vocab.json` vào thư mục `app/src/main/assets/`.

### 3. Nạp Model Ngôn Ngữ Nhỏ (SLM) Tùy Ý (Gemma / SmolLM / Qwen):
Để AI có khả năng tự do tư duy câu trả lời (Generative Response):
* **Cách 1 (Đóng gói sẵn vào APK)**: Đặt file `sense_slm.task` hoặc `sense_slm.bin` vào thư mục `app/src/main/assets/`. Ứng dụng sẽ tự động trích xuất và nạp vào máy khi cài đặt.
* **Cách 2 (Đẩy nhanh qua cáp ADB)**:
  ```bash
  adb push my_slm_model.bin /data/local/tmp/sense_slm.bin
  ```

---

## 5. Biên Dịch Và Kiểm Thử

### Yêu Cầu Môi Trường
* **JDK**: OpenJDK 21
* **Android SDK**: Compile/Target SDK 34, Min SDK 26 (Android 8.0 trở lên)
* **Android Studio**: Jellyfish / Ladybug trở lên

### Lệnh Thực Thi

* **Chạy toàn bộ bộ kiểm thử tự động (Unit Test Suite)**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
* **Chạy riêng kiểm thử module AI**:
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.sensebridge.core.ai.*"
  ```
* **Build bản Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
  File APK đầu ra: `app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Tiêu Chuẩn Bảo Mật & Đạo Đức AI

* **Không Có Dữ Liệu Rời Khỏi Máy**: Toàn bộ luồng hình ảnh camera, âm thanh micro và lịch sử tương tác đều được xử lý cục bộ trên RAM và SQLite Room nội bộ.
* **Không Quảng Cáo, Không Thu Thập Danh Tính**: Thiết kế hướng tới quyền riêng tư tối thượng cho người khiếm thị và khiếm thính.
* **Fail-Safe & Graceful Fallback**: Khi máy thiếu RAM hoặc chưa có model ngôn ngữ lớn, hệ thống tự động fallback về mạng nơ-ron TFLite và bộ phản xạ an toàn, đảm bảo không bao giờ đơ hoặc tắt đột ngột khi người dùng đang di chuyển.

---

## 7. Giấy Phép Bản Quyền (License)

Dự án được phân phối dưới giấy phép **[Apache License 2.0](LICENSE)**.
Bạn có quyền sử dụng, sửa đổi, phân phối lại hoặc tích hợp vào các dự án thương mại và phi thương mại theo các điều khoản quy định trong giấy phép.

```
Copyright 2026 SenseBridge Contributors (WangDoLin)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
