# 🧠 SenseBridge: Comprehensive Algorithmic Monograph & Academic Foundations

> **System Technical Monograph & Scientific Reference**  
> **Project**: SenseBridge — On-Device Multimodal Accessibility Platform  
> **Document Version**: 1.7.0  
> **Primary Author / Contributors**: SenseBridge Contributors (WangDoLin)  
> **Intersecting Disciplines**: Computer Vision, Acoustic Digital Signal Processing (DSP), Sensory Substitution Theory, Embedded Deep Learning, Natural Language Processing (NLP), Human-Computer Interaction (HCI), Assistive Technology.

---

## 📑 TABLE OF CONTENTS

1. [Scientific Foundations & Sensory Substitution Theory](#1-scientific-foundations--sensory-substitution-theory)
2. [Acoustic Digital Signal Processing & Environmental Audio Intelligence](#2-acoustic-digital-signal-processing--environmental-audio-intelligence)
3. [Computer Vision & Bounding Box Spatial Geometry](#3-computer-vision--bounding-box-spatial-geometry)
4. [OCR Image Preprocessing & Morphological Text Sanitization](#4-ocr-image-preprocessing--morphological-text-sanitization)
5. [Multimodal Sensor Fusion & Priority Queue Scheduling](#5-multimodal-sensor-fusion--priority-queue-scheduling)
6. [Tactile Haptic Waveform Synthesis & Psychoacoustics](#6-tactile-haptic-waveform-synthesis--psychoacoustics)
7. [On-Device Artificial Intelligence & Continual Learning Architecture](#7-on-device-artificial-intelligence--continual-learning-architecture)
8. [Embedded Systems Architecture & R8 Compiler Optimization](#8-embedded-systems-architecture--r8-compiler-optimization)
9. [Assistive Ethics, Zero-Cloud Privacy & Non-Commercial Licensing](#9-assistive-ethics-zero-cloud-privacy--non-commercial-licensing)
10. [Appendix: System Constants & Mathematical Parameter Reference](#10-appendix-system-constants--mathematical-parameter-reference)

---

## 1. SCIENTIFIC FOUNDATIONS & SENSORY SUBSTITUTION THEORY

### 1.1. Cross-Modal Neuroplasticity and the Bach-y-Rita Paradigm
SenseBridge is engineered upon the theoretical foundations of **Sensory Substitution**, pioneered by neuroscientist **Dr. Paul Bach-y-Rita**:
> *"We see with the brain, not with the eyes; we hear with the brain, not with the ears."*

When primary sensory pathways suffer from pathological deficits (such as blindness or deafness), the human central nervous system retains remarkable cross-modal neuroplasticity. The cerebral cortex can adaptively re-route and decode non-homologous sensory inputs—translating optical and acoustic stimuli into structured vibrotactile waveforms and localized spatial audio—thereby reconstructing a functional internal spatial cognitive map.

```
[ Environmental Stimuli ]
       │
       ├─► [ Photonic Wavefronts ]  ──► CameraX Subsystem ──► ML Kit Vision / OCR
       │                                                              │
       └─► [ Acoustic Pressure ]    ──► Mic 16-bit PCM    ──► YAMNet Classifier
                                                                      │
                                                                      ▼
                                                       [ EventEngine Core Pipeline ]
                                                                      │
       ┌──────────────────────────────────────────────────────────────┴──────────────────────────┐
       ▼                                                                                         ▼
[ Somatosensory Channel: Tactile Haptics ]                                [ Auditory / Visual Display ]
- Temporal Cadence Signatures (Rhythmic Pulses)                           - Earcon & Spatial TTS Engine
- Sub-10ms Motor Reflex Feedback                                          - Full-Screen High-Contrast Strobe
```

### 1.2. Cognitive Load Theory & Alarm Fatigue Mitigation
In accordance with **John Sweller's Cognitive Load Theory**, working memory possesses strictly constrained bandwidth when processing simultaneous environmental inputs:
* **Intrinsic Load**: The innate complexity of dynamic surroundings (e.g., bustling intersections, urban noise).
* **Extraneous Load**: The mental overhead imposed by redundant, chaotic, or non-prioritized application alerts.

In assistive computing, unrestrained sensory feedback triggers acute **Cognitive Overload** and subsequent **Alarm Fatigue**, causing users to disregard life-critical warnings. SenseBridge enforces three architectural safeguards:
1. **Dynamic Noise Gating**: Eliminates sub-threshold stochastic acoustic fluctuations.
2. **Episode-Based Debouncing**: Collapses prolonged alert bursts into single cognitive episodes, sustaining attention through silent haptic pulses rather than repetitive verbal prompts.
3. **Preemptive Priority Queuing ($P_0 > P_1 > P_2 > P_3$)**: Immediate preemption guarantees that existential threats terminate lower-priority speech output instantly.

---

## 2. ACOUSTIC DIGITAL SIGNAL PROCESSING & ENVIRONMENTAL AUDIO INTELLIGENCE

SenseBridge processes live single-channel microphone streams using a hardware-level 16-bit Linear PCM audio pipeline running at $16\text{ kHz}$.

### 2.1. Root Mean Square (RMS) Energy Computation
Raw discrete audio samples $x_i \in [-32768, 32767]$ over a buffer window of size $N$ are integrated to compute root mean square amplitude:

$$RMS = \sqrt{\frac{1}{N} \sum_{i=0}^{N-1} x_i^2}$$

To prevent numerical singularities and undefined logarithmic evaluations during periods of absolute digital silence, $RMS$ is bounded below:

$$RMS_{clamped} = \max(RMS, 10^{-6})$$

### 2.2. Decibel Full-Scale ($dB_{FS}$) Conversion
Energy levels are projected onto the logarithmic relative decibel scale:

$$dB = 20 \cdot \log_{10}(RMS_{clamped})$$

*Implementation Reference: [`DecibelEnergyGate.kt`](app/src/main/java/com/sensebridge/input/sound/DecibelEnergyGate.kt).*

### 2.3. Capped Adaptive Exponential Moving Average (EMA) Noise Floor
Ambient acoustic baselines fluctuate across environments (quiet bedroom $\approx 35\text{ dB}$, office $\approx 50\text{ dB}$, arterial roadway $\approx 65\text{ dB}$). SenseBridge employs a directionally-capped adaptive EMA filter:

$$NoiseFloor_t = (1 - \alpha) \cdot NoiseFloor_{t-1} + \alpha \cdot dB_t$$

Governing physical parameters:
* Smoothing coefficient: $\alpha = 0.05$ (ensuring a sluggish temporal integration window that resists short transient shocks).
* Update period: $\Delta t \ge 200\text{ ms}$.
* Upper saturation threshold: $MAX\_NOISE\_FLOOR\_CAP = 54.0\text{ dB}$. (Loud ongoing events above $54\text{ dB}$ are barred from raising the noise floor, preventing sirens and vehicle horns from artificially desensitizing the detection gate).

### 2.4. Hysteresis Energy Gating & Impact Onset Detection
Downstream deep learning models are conditionally evaluated upon satisfying any of three physical criteria:
1. **Absolute Threat Threshold**: $dB \ge 68.0\text{ dB}$.
2. **Signal-to-Noise Ratio (SNR) Dynamic Margin**:
   $$SNR = dB - NoiseFloor \ge 8.0\text{ dB} \quad \text{and} \quad dB \ge Threshold$$
3. **Sudden Impact Onset Detection**:  
   Identifies explosive acoustic phenomena (e.g., shattering glass, door kicks, structural impacts) against elevated background noise:
   $$dB \ge 84.0\text{ dB} \quad \text{and} \quad dB - \text{Median}_{24}(Levels) \ge 15.0\text{ dB}$$
   *Evaluated over a rolling 24-frame history buffer via sample median filtering to reject persistent high-amplitude conditions.*

### 2.5. YAMNet Spectrogram Inference & Temporal Consensus Voting
Audio segments exceeding energy gates are classified by **YAMNet** (a deep convolutional architecture leveraging MobileNetV1 topology operating on 64-bin Log-Mel spectrogram frames) spanning 521 AudioSet categories.

To suppress stochastic frame-level false positives, SenseBridge applies a **Temporal $k$-of-$n$ Consensus Filter**:
* History depth: $n = 3$ consecutive inference windows.
* Affirmation rule: An audio group label is emitted if:
  1. Standard voting: Detected in at least $k = 2$ out of $3$ rolling frames.
  2. Or significant confidence margin override:
     $$Confidence \ge Threshold_{group} + 0.15$$

*Implementation Reference: [`TemporalVoter.kt`](app/src/main/java/com/sensebridge/input/sound/TemporalVoter.kt).*

---

## 3. COMPUTER VISION & BOUNDING BOX SPATIAL GEOMETRY

SenseBridge translates 2D bounding box detections into egocentric 3D spatial representations.

```
                      Camera Sensor Plane (W x H)
┌─────────────────────────────────────────────────────────────┐
│ 0.0            0.35                      0.65           1.0 │
│   LEFT SECTOR    │     CENTER SECTOR       │  RIGHT SECTOR  │
│                  │                         │                │
│             ┌─────────┐                    │                │
│             │ BBox    │                    │                │
│             │ Area S  │                    │                │
│             └─────────┘                    │                │
│                  │                         │                │
└──────────────────┼─────────────────────────┼────────────────┘
                   ▼                         ▼
            Azimuth Direction         Area Ratio S / (W x H)
            (Left/Center/Right)       (Near >= 0.28, Far <= 0.10)
```

### 3.1. Spatial Azimuth Discretization
For a detected entity defined by coordinates $[x_{min}, y_{min}, x_{max}, y_{max}]$ across a sensor resolution of $W \times H$:
The normalized horizontal centroid is computed as:

$$X_{norm} = \frac{x_{min} + x_{max}}{2 \cdot W} \in [0.0, 1.0]$$

Angular orientation is discretized into egocentric sectors via piecewise mapping:

$$\text{Direction}(X_{norm}) = \begin{cases} 
\text{LEFT}, & \text{for } X_{norm} < 0.35 \\
\text{CENTER}, & \text{for } 0.35 \le X_{norm} \le 0.65 \\
\text{RIGHT}, & \text{for } X_{norm} > 0.65 
\end{cases}$$

### 3.2. Perspective Depth & Relative Proximity Heuristics
Adhering to pinhole perspective optics and inverse-square surface projection laws, the bounding box area ratio serves as a monotonic proxy for spatial proximity:

$$\text{AreaRatio} = \frac{(x_{max} - x_{min}) \cdot (y_{max} - y_{min})}{W \cdot H}$$

Discrete proximity thresholds:
* **Near Sector (Imminent Obstacle / Collision Danger)**: $\text{AreaRatio} \ge 0.28$ (Object spans $> 28\%$ of camera FOV, roughly $< 1.5\text{ m}$).
* **Far Sector**: $\text{AreaRatio} \le 0.10$ (Object spans $< 10\%$ of camera FOV, roughly $> 4.0\text{ m}$).

### 3.3. Open-Vocabulary Spatial Grounding Engine
Inspired by the **NVIDIA LocateAnything** paradigm (Parallel Box Decoding), `LocateGroundingEngine` provides targeted spatial grounding for specific requested items (e.g., *"find water bottle"*, *"locate empty seat"*):
1. **Semantic Query Matching**: Multi-tier fuzzy matching combining substring containment and curated lexical synsets.
2. **Dynamic Vector Guidance Generation**: Transforms relative bounding box offsets into actionable motor directives:
   * Left Sector: *"Turn camera left to center on [Target]."*
   * Right Sector: *"Turn camera right to center on [Target]."*
   * Center Sector + Near: *"[Target] is immediately in front of you (very close)."*

*Implementation Reference: [`LocateGroundingEngine.kt`](app/src/main/java/com/sensebridge/input/vision/LocateGroundingEngine.kt).*

---

## 4. OCR IMAGE PREPROCESSING & MORPHOLOGICAL TEXT SANITIZATION

Real-world optical character recognition under mobile accessibility conditions suffers from hand tremor, high specular glare, and optical hallucinations.

### 4.1. Viewport Region-of-Interest (ROI) Cropping & Scale Clamping
1. Strict cropping to the CameraX active viewport rectangle (`imageProxy.cropRect`), discarding peripheral distortions.
2. Long-edge dimension constraint: $L_{max} \le 2560\text{ px}$. If $\max(W, H) > 2560$, images undergo bilinear scaling (`Bitmap.createScaledBitmap`) to bound tensor memory allocations and prevent garbage collection pauses during inference.

### 4.2. Morphological Text Sanitization Algorithms

#### A. Alphanumeric Density Ratio Filtering
For each candidate text line $s$, the proportion of valid alphanumeric characters against visible glyphs must satisfy:

$$R_{alnum}(s) = \frac{\sum_{c \in s} [\text{isLetterOrDigit}(c)]}{\sum_{c \in s} [c \ne \text{' '}]} \ge 0.50$$

Any line yielding $R_{alnum} < 0.50$ is discarded as background noise.

#### B. State-Transition Entropy (Letter-Digit Switches)
Natural printed text rarely alternates rapidly between numeric and alphabetic representations. The transition count is evaluated across adjacent characters:

$$\text{Switches}(token) = \sum_{i=0}^{|token|-2} [\text{isDigit}(token_i) \ne \text{isDigit}(token_{i+1})]$$

If $\text{Switches}(token) > 2$, the token is classified as an OCR artifact and purged.

#### C. Vietnamese Vowelless Gibberish Detection
Random background noise frequently manifests as clusters of consonants. The detection algorithm:
1. Normalizes the string to Canonical Decomposition (Unicode NFD).
2. Strips all combining diacritical marks via regex: `\p{Mn}+`.
3. Verifies membership in the Latin vowel set $V = \{a, e, i, o, u, y\}$:

$$\text{isGibberish}(core) = (|core| \ge 6) \land \left( \forall c \in core_{stripped}, c \notin V \right)$$

*Implementation Reference: [`OcrTextSanitizer.kt`](app/src/main/java/com/sensebridge/input/vision/OcrTextSanitizer.kt).*

#### D. Prosody-Aware Text Stitching
To produce fluent Text-to-Speech output matching human speech prosody:
* Terminal punctuation (`.`, `!`, `?`, `:`, `;`): Preserves natural sentence boundaries.
* Subsequent lowercase tokens: Concatenated with a space ` ` as sentence continuations.
* New visual layout blocks: Delimited with period boundaries `. `.
* Intra-block line breaks: Delimited with comma pauses `, ` to induce natural TTS breathing intervals.

---

## 5. MULTIMODAL SENSOR FUSION & PRIORITY QUEUE SCHEDULING

### 5.1. Cross-Modal Temporal Correlation Window
Environmental hazards typically emit correlated signatures across multiple sensory domains simultaneously:
* **Visual Modality**: Camera detects entity `Car` at sector `LEFT`.
* **Acoustic Modality**: Microphones register `Car Horn` within $\Delta t \le 800\text{ ms}$.

```
Visual Detection (Car @ LEFT) ──┐
                                ├──► MultimodalSynthesizer (Δt <= 800ms)
Acoustic Detection (Horn)     ──┘        │
                                         ▼
                            [ Fused Event: car_horn_fused ]
                            - Title: "Vehicle Honking (Left side)"
                            - Spoken: "Warning, vehicle on the left is honking!"
                            - Priority: CRITICAL_P0 (Absolute Preemption)
```

The correlation synthesizer eliminates separate notifications and generates a unified compound event `car_horn_fused` with elevated confidence ($0.95$).

### 5.2. Episode-Based Debouncing State Machine
Continuous environmental events (such as prolonged sirens, repeating car horns, or fire alarms) would overwhelm users if alerted repeatedly. SenseBridge manages sensory state transitions via an **Episode State Machine**:

```
                       ┌────────────────────────────┐
                       │   Urgent Audio Event (P0)  │
                       └──────────────┬─────────────┘
                                      │
                   Silence Gap > 2.5s │ or Duration >= 15s?
                                      ▼
                   ┌──────────────────┴──────────────────┐
                   │                                     │
                 [ YES ]                               [ NO ]
                   │                                     │
                   ▼                                     ▼
           (NEW_EPISODE)                         Time since last
     - Fire Haptic Waveform                     vibration >= 1.0s?
     - Announce Speech (TTS)                             │
     - Log Event to History                ┌─────────────┴─────────────┐
                                           │                           │
                                         [ YES ]                     [ NO ]
                                           │                           │
                                           ▼                           ▼
                                    (CONTINUATION)                 (SUPPRESS)
                               - Pulse Tactile Motor            - Fully Drop Event
                               - Silence Speech Queue
                               - Omit History Duplicates
```

*Implementation Reference: [`SmartEventDebouncer.kt`](app/src/main/java/com/sensebridge/core/engine/SmartEventDebouncer.kt).*

### 5.3. Serialized Non-Blocking Event Pipeline
All asynchronous producer threads (CameraX analyzers, Audio record daemons, UI interaction layers) publish events to a single centralized coroutine channel:

```kotlin
private val eventChannel = Channel<SenseEvent>(capacity = Channel.UNLIMITED)
```

* **Zero Race Conditions**: Eliminates thread contention and deadlocks across sensory modules.
* **Chronological Integrity**: Preserves exact temporal order of multimodal sensory streams.
* **Preemptive Interruption ($P_0$ Priority)**: High-priority events immediately trigger `ttsManager.stopImmediately()`, clearing out non-critical queued utterances ($P_2, P_3$) to convey critical warnings without delay.

---

## 6. TACTILE HAPTIC WAVEFORM SYNTHESIS & PSYCHOACOUSTICS

### 6.1. Temporal Rhythmic Modulation vs. Raw Amplitude
Commercial Android devices incorporate two dominant actuator technologies:
1. **ERM (Eccentric Rotating Mass)**: Mechanical rotational motors found in entry-level hardware; exhibits slow spin-up latency and poor amplitude fidelity.
2. **LRA (Linear Resonant Actuator)**: Precision resonant linear motors with rapid sub-millisecond response.

Because ERM motors cannot faithfully render subtle amplitude gradations, SenseBridge encodes all informational categories into distinct **TEMPORAL RHYTHMS (Cadence & Pulse Duration)** rather than variable intensities:

### 6.2. Haptic Signatures Matrix

| Signature Key | Associated Sound Class | Waveform Array (ms) `[delay, on, off, on...]` | Tactile Perception Profile |
|---|---|---|---|
| `car_horn` | Vehicle Horn | `[0, 150, 100, 150, 100, 150]` | 3 sharp staccato bursts (ta-ta-ta) |
| `siren` | Ambulance / Police Siren | `[0, 250, 100, 600]` | 1 short pulse followed by 1 surging long pulse |
| `fire_alarm` | Industrial Fire Alarm | `[0, 400, 150, 400]` | 2 sustained high-impact warnings |
| `scream` | Human Distress Scream | `[0, 100, 80, 100, 80, 100, 80, 100]` | 4 rapid shivering pulses |
| `doorbell` | Chime / Doorbell | `[0, 150, 250, 150]` | 2 separated rhythmic beats (ding ... dong) |
| `knock` | Door Knock | `[0, 70, 130, 70, 130, 70]` | 3 short, dry percussive taps |
| `glass_break`| Shattering Glass / Impact | `[0, 800]` | Single massive continuous shock |

*Implementation Reference: [`HapticSignatures.kt`](app/src/main/java/com/sensebridge/output/haptic/HapticSignatures.kt).*

---

## 7. ON-DEVICE ARTIFICIAL INTELLIGENCE & CONTINUAL LEARNING ARCHITECTURE

SenseBridge deploys a hierarchical, tiered on-device AI inference pipeline that operates with zero cloud dependence.

```
                         [ User Speech Query / Environmental State ]
                                              │
                 ┌────────────────────────────┴────────────────────────────┐
                 ▼                                                         ▼
       [ Fast Reflex Tier ]                                      [ Deep Reasoning Tier ]
   SenseAiNeuralClassifier (TFLite)                              OnDeviceSlmInferenceEngine
   - Footprint: < 50 KB                                          - Local SLM (Gemma / Phi)
   - Latency: < 1 ms on CPU                                      - MediaPipe Tasks GenAI Runtime
   - 10 Discrete Intent Classes                                  - Context-Injected Autoregression
                 │                                                         │
                 └────────────────────────────┬────────────────────────────┘
                                              ▼
                                [ ContinualLearningEngine ]
                                - Session Episodic Memory
                                - Hebbian Confidence Weight Reinforcement
                                - Elimination of Catastrophic Forgetting
```

### 7.1. Deep Neural Intent Classifier Topology
Trained on domain-specific Vietnamese assistive interaction corpora:

* **Layer Architecture**:
  1. `InputLayer(shape=(20,))`: Fixed token sequence length of 20 words.
  2. `Embedding(vocab_size=337, embedding_dim=32)`: Word projection into 32-dimensional semantic latent space.
  3. `GlobalAveragePooling1D()`: Spatial average pooling providing temporal position invariance.
  4. `Dense(64, activation='relu')` + `Dropout(0.2)`: Non-linear feature combination with regularization.
  5. `Dense(32, activation='relu')`: Feature refinement.
  6. `Dense(num_classes=10, activation='softmax')`: Posterior probability distribution over 10 intent classes.

* **Loss Formulation & Optimization**:
  $$\mathcal{L} = -\sum_{i=1}^{C} y_i \log(\hat{y}_i) \quad (\text{Sparse Categorical Cross-Entropy}), \quad \text{Optimizer: Adam}$$
* **Quantized Edge Deployment**: Converted to flatbuffer format (`.tflite`), yielding a **$48.4\text{ KB}$** binary with sub-millisecond execution times on mobile CPUs.

### 7.2. Small Language Model (SLM) Runtime Engine
Integrated via `com.google.mediapipe.tasks.genai.llminference`:
* Executes quantized 4-bit and 8-bit checkpoints (e.g., `Gemma-2B`, `TinyLlama-1.1B`).
* **Hardware Memory Safety Guard**:
  Before instantiating the model graph in RAM, the engine inspects system memory via `ActivityManager`:
  $$RAM_{available} \ge 1500\text{ MB}$$
  If available memory falls below $1500\text{ MB}$, the runtime transparently falls back to the local neural classifier and template generator, preventing Out-Of-Memory (OOM) operating system terminations.

### 7.3. Continual Learning & Exemplar Episodic Memory
Standard artificial neural networks suffer from **Catastrophic Forgetting** when fine-tuned sequentially on new domain data.

SenseBridge circumvents this constraint through an **Exemplar-Based Episodic Architecture**:
1. **Hebbian Frequency Reinforcement**:  
   Repeated user associations incrementally update lexical pattern confidence weights:
   $$W_{new} = \min(1.0, W_{old} + 0.10)$$
2. **Episodic Scene Memory Retrieval**:  
   Maintains SQLite-persisted spatial session representations. When a user revisits a previously encountered environment, historical memories are surfaced contextually:
   *"Location memory: You were here previously when the camera read 'Eye Hospital'."*

*Implementation Reference: [`ContinualLearningEngine.kt`](app/src/main/java/com/sensebridge/core/ai/ContinualLearningEngine.kt).*

---

## 8. EMBEDDED SYSTEMS ARCHITECTURE & R8 COMPILER OPTIMIZATION

### 8.1. R8 Bytecode Optimization & JNI Surface Retention
Production builds undergo full R8 shrinking and ProGuard optimization:
* **Minification & Dead Code Stripping**: Eliminates unused methods and metadata.
* **Resource Optimization**: Unused audio and visual assets are stripped from the final package.
* **JNI & Reflection Preservation Rules**:
  Crucial native C++ entrypoints for TensorFlow Lite, MediaPipe GenAI, and Room SQLite are preserved:
  ```proguard
  -keep class org.tensorflow.** { *; }
  -keepclassmembers class * { native <methods>; }
  -keep class com.google.mediapipe.** { *; }
  -dontwarn com.google.mediapipe.proto.**
  -dontwarn javax.lang.model.**
  ```

### 8.2. Thermal-Aware Dynamic Frame Throttling
Camera processing rates are dynamically governed by environmental activity through `FrameRateThrottle`:
* Idle/Static surroundings: Frame rates throttle down to $2\text{ fps}$, reducing thermal dissipation and CPU power draw.
* High-velocity motion / Audio triggers: Instantly scales up to $15\text{ - }30\text{ fps}$ to maintain spatial safety margins.

---

## 9. ASSISTIVE ETHICS, ZERO-CLOUD PRIVACY & NON-COMMERCIAL LICENSING

### 9.1. Zero-Cloud Privacy by Design
Individuals with sensory disabilities represent an exceptionally vulnerable population regarding data autonomy. Transmitting video feeds of private living quarters or ambient audio recordings to external cloud servers presents severe surveillance risks.
* **100% On-Device Processing**: Zero outgoing network sockets exist for sensory analysis.
* **Telemetry-Free Guarantee**: Contains no third-party tracking or behavioral monetization SDKs.

### 9.2. Ambient Speech Detection & Privacy Auto-Mute
During automated narration over headphones or external speakers:
* If ambient human conversation is detected in the environment (`speech` class via YAMNet or speech recognizer), narration volume automatically reduces or silences to avoid disclosing private information (e.g., medical prescriptions, identity documents) to nearby individuals.

### 9.3. Humanitarian & Healthcare Exemption Licensing
SenseBridge is protected by a customized public license designed to:
* **Prohibit Commercial Monopolization**: Precludes proprietary vendors from rebranding open assistive technology for predatory pricing.
* **Exempt Healthcare & Charitable Missions**: Hospitals, non-profit organizations, rehabilitation centers, and public welfare foundations maintain unrestricted, royalty-free usage and distribution rights.

---

## 10. APPENDIX: SYSTEM CONSTANTS & MATHEMATICAL PARAMETER REFERENCE

| Constant Identifier | Calibrated Value | Engineering Unit | Scientific / Physical Rationale |
|---|---|---|---|
| `DEFAULT_THRESHOLD_DB` | `58.0` | dB | Baseline sound pressure level triggering initial analysis |
| `ALWAYS_SIGNIFICANT_DB` | `68.0` | dB | High-energy boundary that bypasses SNR margins |
| `MIN_SNR_DB` | `8.0` | dB | Minimum signal-to-noise ratio required for transient alert |
| `LOUD_IMPACT_DB` | `84.0` | dB | Absolute threshold for sudden explosive/collision impact |
| `IMPACT_ONSET_DELTA_DB` | `15.0` | dB | Instantaneous deviation above rolling 24-frame median |
| `EMA_ALPHA` | `0.05` | Dimensionless | Smoothing factor for adaptive noise floor tracking |
| `LEFT_BOUNDARY_THRESHOLD` | `0.35` | Ratio $[0, 1]$ | Normalized lateral boundary for Left sector discretization |
| `RIGHT_BOUNDARY_THRESHOLD`| `0.65` | Ratio $[0, 1]$ | Normalized lateral boundary for Right sector discretization |
| `NEAR_AREA_RATIO_THRESHOLD`| `0.28` | Ratio $[0, 1]$ | Bounding box area ratio indicating imminent proximity ($< 1.5\text{m}$) |
| `FAR_AREA_RATIO_THRESHOLD` | `0.10` | Ratio $[0, 1]$ | Bounding box area ratio indicating safe distance ($> 4\text{m}$) |
| `FUSION_WINDOW_MS` | `800` | ms | Cross-modal temporal correlation window (Vision + Audio) |
| `EPISODE_GAP_MS` | `2500` | ms | Silence interval delimiting discrete warning episodes |
| `HAPTIC_REPEAT_INTERVAL_MS`| `1000` | ms | Cadence interval for ongoing haptic pulses within an episode |
| `REANNOUNCE_INTERVAL_MS` | `15000` | ms | Maximum period before re-announcing prolonged alarms via TTS |
| `MIN_LINE_CONFIDENCE` | `0.40` | Ratio $[0, 1]$ | Minimum acceptance threshold for individual OCR lines |
| `MIN_ALNUM_RATIO` | `0.50` | Ratio $[0, 1]$ | Minimum alphanumeric density threshold for OCR noise rejection |
| `MIN_RECOMMENDED_RAM_MB` | `1500` | MB | Minimum available system RAM required to instantiate local SLM |

---
*Document synchronized with the official codebase of SenseBridge v1.7.0.*
