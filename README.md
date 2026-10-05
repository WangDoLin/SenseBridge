# SenseBridge - On-Device Multimodal Accessibility & Situational Cognitive AI Platform

[![License](https://img.shields.io/badge/License-Non--Commercial_(Medical_%26_Charity_Exempt)-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![TensorFlow Lite](https://img.shields.io/badge/AI-TensorFlow_Lite-orange.svg)](https://www.tensorflow.org/lite)
[![MediaPipe](https://img.shields.io/badge/GenAI-MediaPipe_Tasks-red.svg)](https://ai.google.dev/edge/mediapipe)
[![Tests](https://img.shields.io/badge/Tests-100%25_Passing-brightgreen.svg)]()

**SenseBridge** is a production-grade, **100% on-device multimodal sensory substitution and situational cognitive AI platform** built for Android. 

The application transforms commodity smartphones into real-time perceptual assistive bridges for visually impaired and hard-of-hearing individuals. Operating completely offline without external sensors, cloud infrastructure, or internet connectivity, SenseBridge preserves absolute user privacy while delivering ultra-low-latency (<10ms) emergency alerts and contextual conversational intelligence.

---

## 1. System Architecture

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
      - Vehicle horns, Sirens           - Spatial Direction (L/C/R)               |
      - Screams, Human speech           [ML Kit OCR with Crop Box]                |
                |                       - Signage, Bus routes, Rooms              |
                +--------------------------------+--------------------------------+
                                                 |
                                                 v
                       +---------------------------------------------------+
                       |        MULTIMODAL COGNITIVE SCENE MEMORY          |
                       |          (6-Second Dynamic Rolling Window)        |
                       +-------------------------+-------------------------+
                                                 |
                   +-----------------------------+-----------------------------+
                   v                                                           v
   +-------------------------------+                           +-------------------------------+
   |    AiMultimodalReasoner       |                           |      SenseAiDialogueModel     |
   | ----------------------------- |                           | ----------------------------- |
   | - Cross-modal threat analysis |                           | - 6 Situational states        |
   | - P0-P3 collision/hazard eval |                           | - Multi-turn conversation     |
   +---------------+---------------+                           +---------------+---------------+
                   |                                                           |
                   |                                           +---------------+---------------+
                   |                                           v                               v
                   |                           +-------------------------------+  +-------------------------------+
                   |                           |    SenseAiNeuralClassifier    |  |  OnDeviceSlmInferenceEngine   |
                   |                           | ----------------------------- |  | ----------------------------- |
                   |                           | - Self-trained TFLite (48.4KB)|  | - MediaPipe Tasks GenAI       |
                   |                           | - 90.37% Accuracy, 10 Classes |  | - Generative SLM Response     |
                   |                           +---------------+---------------+  +---------------+---------------+
                   |                                           |                               |
                   |                                           +---------------+---------------+
                   |                                                           |
                   |                                                           v
                   |                                           +-------------------------------+
                   |                                           |    ContinualLearningEngine    |
                   |                                           | ----------------------------- |
                   |                                           | - On-device pattern learning  |
                   |                                           | - Online adaptive weights     |
                   |                                           | - Episodic location memory    |
                   |                                           +---------------+---------------+
                   |                                                           |
                   +-----------------------------+-----------------------------+
                                                 |
                                                 v
                       +---------------------------------------------------+
                       |             CORE EVENT DISPATCH ENGINE            |
                       | ------------------------------------------------- |
                       | - Serialized Priority Queue (P0 -> P3)            |
                       | - Adaptive Speech Cooldown & Anti-Spam Filter     |
                       | - Cross-Modal Preemption (Emergency Interrupts)   |
                       +-------------------------+-------------------------+
                                                 |
                +--------------------------------+--------------------------------+
                |                                |                                |
                v                                v                                v
       [Haptic Dispatcher]              [Speech Dispatcher]            [Visual Alert Overlay]
       - P0: Continuous Alert           - Tiered Priority TTS          - High-contrast HUD
       - P1: Double Pulse Warning       - Auto-mute on BT disconnect   - Fullscreen color flashing
       - P2/P3: Directional Pulses      - Speaker bypass for AAC       - Large-font captions
```

---

## 2. Core Technical Pillars

### 2.1. Situational Cognitive Edge AI & Continual Learning
* **MultimodalSceneMemory**: Maintains a dynamic 6-second rolling window across all active sensory streams (detected objects, OCR text, environmental audio classifications, ambient decibel level), automatically evicting stale observations.
* **AiMultimodalReasoner**: Evaluates cross-modal threat correlations in real time (e.g., vehicle detected ahead + acoustic car horn $\rightarrow$ P0 Critical Collision Alert; detected staircase or hole nearby $\rightarrow$ P1 Fall Hazard).
* **SenseAiNeuralClassifier (TFLite)**: A customized, self-trained Deep Neural Network for intent recognition. Compact footprint of only **48.4 KB** with **90.37% test accuracy** across 10 dialog intent classes, packaged directly in `assets/` and computed via the TensorFlow Lite Interpreter.
* **OnDeviceSlmInferenceEngine (MediaPipe Tasks GenAI)**: Native on-device Small Language Model (SLM) inference on CPU/GPU (supporting Gemma, SmolLM, Qwen) using Google MediaPipe `tasks-genai`, allowing free-form natural language dialogue grounded in real-time sensor context.
* **ContinualLearningEngine (On-Device Self-Training & Episodic Memory)**:
  * **Personal Linguistic Adaptation**: Automatically learns unique user phrasings, dialects, and slang, dynamically scaling association weights (`confidenceWeight`) upon repeated usage.
  * **Episodic Location Memory**: Retains contextual snapshots (OCR signs, visual landmarks, noise levels) inside local Room Database v2, allowing the AI to recall past visits (e.g., *"Location memory: You were here earlier when the camera read 'Bus Stop 10'"*).
  * **Zero-Cloud Privacy**: All user patterns, weights, and session histories are stored locally on-device.

### 2.2. Environmental Acoustic Intelligence
* **AudioRecorderManager**: Captures 16kHz Mono PCM audio with strict mutex synchronization to eliminate microphone contention.
* **DecibelEnergyGate**: Filters ambient sound levels against a configurable threshold (40–85 dB), gating TFLite inference when quiet to minimize battery consumption.
* **TFLiteAudioClassifier (YAMNet)**: On-device classification of 521 audio classes (sirens, car horns, alarms, glass breaking, screams, human speech).

### 2.3. Computer Vision & Sanitized OCR
* **CameraManager**: Coordinates CameraX with an optimized 10fps `ImageAnalysis` pipeline to reduce thermal load and a high-resolution `ImageCapture` pipeline.
* **ObjectDetectorEngine**: Real-time object tracking with spatial categorization (Left, Center, Right) and proximity estimation (Near vs. Far).
* **OcrEngine (Viewport Cropping & Text Sanitization)**:
  * Viewport Cropping: Focuses optical recognition on the user's primary center field of view.
  * Noise Sanitizer: Strips optical artifacts, garbled characters, and symbols (such as `##`), producing clean text for Text-to-Speech synthesis.

### 2.4. Multimodal Sensory Output & Dispatch
* **AndroidHapticManager**: Controls Linear Resonant Actuators (LRA) and Eccentric Rotating Mass (ERM) vibration motors, triggering distinct haptic signatures in under 10ms.
* **TextToSpeechManager**: Prioritized spoken announcements with Bluetooth Communication Device API routing, instantly auto-muting upon headphone disconnection for personal privacy.
* **AlertOverlayManager**: Fullscreen, high-contrast visual Heads-Up Display (HUD) with color-coded pulsing for deaf and hard-of-hearing users.
* **SenseBridgeForegroundService**: Persistent background execution maintaining sensor vigilance even when the screen is locked or the device is pocketed.

### 2.5. Augmentative & Alternative Communication (AAC & STT)
* **Emergency & Phrase Cards**: High-contrast, large touch targets (>64dp) for non-verbal users to communicate urgent needs.
* **Type-to-Speak**: Direct text entry routed to the external loudspeaker.
* **Speech-to-Text (STT)**: Real-time speech transcription displaying conversation partners' spoken words in large, readable text.

---

## 3. Repository Structure

```
e:/Health/
├── ml/                                  # Machine Learning Training Pipeline
│   ├── dataset/
│   │   └── intent_dataset_vi.json       # Vietnamese intent training dataset
│   └── train_sense_ai_model.py          # Keras DNN training script -> TFLite export
├── app/src/main/
│   ├── assets/                          # On-device packaged AI models
│   │   ├── sense_ai_neural_model.tflite # Neural intent classifier (48.4 KB)
│   │   ├── sense_ai_vocab.json          # Tokenizer vocabulary (218 tokens)
│   │   ├── sense_ai_labels.json         # 10 intent class definitions
│   │   └── yamnet.tflite                # YAMNet audio classification model
│   └── java/com/sensebridge/
│       ├── core/
│       │   ├── ai/                      # Situational Cognitive & Generative AI
│       │   │   ├── AiMultimodalReasoner.kt          # Cross-modal threat correlation
│       │   │   ├── ContinualLearningEngine.kt       # On-device learning & episodic memory
│       │   │   ├── MultimodalAiAssistantEngine.kt   # Central AI sensory coordinator
│       │   │   ├── MultimodalSceneMemory.kt         # 6s rolling contextual memory
│       │   │   ├── OnDeviceSlmInferenceEngine.kt    # MediaPipe GenAI SLM engine
│       │   │   ├── SenseAiDialogueModel.kt          # Dialog management & situations
│       │   │   ├── SenseAiNeuralClassifier.kt       # TFLite neural inference engine
│       │   │   └── SlmModelManager.kt               # Model file manager & RAM safety
│       │   ├── engine/                  # EventEngine, MultimodalSynthesizer
│       │   ├── model/                   # SenseEvent, PriorityLevel, SensorySource
│       │   ├── monitoring/              # MonitoringCoordinator
│       │   └── service/                 # SenseBridgeForegroundService
│       ├── data/
│       │   ├── local/                   # Room Database v2 (Sessions & Patterns)
│       │   │   ├── dao/                 # UserAiSessionDao, LearnedUserPatternDao
│       │   │   └── entity/              # UserAiSessionEntity, LearnedUserPatternEntity
│       │   └── repository/              # UserPreferencesRepository, PhraseRepository
│       ├── di/                          # Dagger Hilt dependency injection modules
│       ├── input/                       # CameraX, Sound Recorder, ML Kit Vision
│       ├── output/                      # Haptic, Bluetooth Audio, Alert Overlay
│       └── ui/                          # Jetpack Compose UI (Blind, Deaf, AAC, Home)
```

---

## 4. Retraining & Customizing the AI Models

SenseBridge provides an end-to-end Python pipeline to retrain the neural network model:

### 1. Prerequisites:
```bash
pip install tensorflow-cpu numpy
```

### 2. Execute Training Pipeline:
```bash
python ml/train_sense_ai_model.py
```
The script performs:
1. Loading and tokenizing patterns from `ml/dataset/intent_dataset_vi.json`.
2. Constructing and training a Deep Neural Network (`Embedding -> GlobalAveragePooling1D -> Dense -> Dropout -> Softmax`).
3. Quantizing and exporting `sense_ai_neural_model.tflite` along with `sense_ai_vocab.json` directly into `app/src/main/assets/`.

### 3. Deploying Small Language Models (SLMs) (Gemma / SmolLM / Qwen):
To enable generative natural language dialogue:
* **Option A (Packaged inside APK)**: Place your `.task` or `.bin` model file (e.g., `sense_slm.task` ~150–250MB) into `app/src/main/assets/`. The application automatically extracts and initializes it on first run.
* **Option B (Direct ADB transfer for developers)**:
  ```bash
  adb push my_slm_model.bin /data/local/tmp/sense_slm.bin
  ```

---

## 5. Build & Test Instructions

### Environment Requirements
* **JDK**: OpenJDK 21
* **Android SDK**: Compile / Target SDK 34, Min SDK 26 (Android 8.0+)
* **Android Studio**: Jellyfish / Ladybug (2024.2.1+) or newer

### Build Commands

* **Execute Unit Test Suite (100% Offline)**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
* **Execute AI Module Unit Tests**:
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.sensebridge.core.ai.*"
  ```
* **Assemble Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
  Artifact output: `app/build/outputs/apk/debug/app-debug.apk`

* **Assemble Release APK (R8 Optimization & Shrinking)**:
  ```bash
  ./gradlew assembleRelease
  ```

---

## 6. Privacy & Ethical AI Principles

* **100% On-Device Processing**: Neither camera video frames, microphone raw audio, nor transcribed dialogues are transmitted off the device.
* **No Telemetry, No Identity Tracking**: Designed specifically to preserve user dignity, safety, and confidentiality.
* **Fail-Safe & Graceful Fallback**: If the device experiences high memory pressure or lacks an optional SLM model, the system gracefully falls back to the lightweight TFLite neural classifier and deterministic reactive engine without crashing.

---

## 7. License

SenseBridge is licensed under the **[SenseBridge Non-Commercial (Healthcare & Humanitarian Exception) License v1.0](LICENSE)**.

* **Free for Personal, Academic & Research Use**: You are free to use, modify, study, and distribute this software for personal, educational, scientific research, and non-commercial community purposes.
* **Commercial Use Restriction**: General commercial exploitation, proprietary sublicensing, or monetization is strictly prohibited without written consent, **with two explicit exceptions**:
  1. **Healthcare & Medical Services**: Hospitals, clinics, rehabilitation facilities, medical practitioners, and assistive healthcare providers are explicitly authorized to use, integrate, and deploy this software royalty-free for patient diagnosis, caregiving, safety, and rehabilitation.
  2. **Charitable & Philanthropic Causes**: Recognized non-profit organizations, humanitarian initiatives, and donation-funded public welfare campaigns are explicitly authorized to use, fundraise with, and distribute this software for community accessibility and social welfare programs.

For complete legal terms and conditions, please consult the [LICENSE](LICENSE) file.
