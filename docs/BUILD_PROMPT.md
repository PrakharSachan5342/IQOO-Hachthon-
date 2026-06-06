# BUILD PROMPT — Native Kotlin (stretch goal only)

> Use this ONLY for the optional native rewrite of individual pipeline pieces.
> The primary deliverable is the Kotlin + Jetpack Compose WebView APK (see CLAUDE.md §3).
> Do NOT attempt a full native rebuild the night before the demo.

## Role
Principal Systems Architect / low-level mobile core engineer. Zero-overhead abstractions,
mechanical sympathy, strict algorithmic correctness. No placeholders, no TODOs — complete classes.

## Classes to generate (native target)
1. **IQOOHardwareManager.kt** — hardware config controller; custom CoroutineDispatcher pool
   isolating the 60 FPS CameraX + MediaPipe vision pipeline onto high-perf cores, decoupled
   from the UI thread. AndroidManifest with `appCategory="game"`, BG services, permissions
   (BLE, DSP audio, NNAPI, camera).
2. **ZeroCopyAnalyzer.kt** — CameraX `ImageAnalysis.Analyzer`; frames into `HardwareBuffer`,
   direct GPU-texture pointer, zero CPU copy.
3. **MediaPipeGpuDelegate.kt** — Hand Landmarker on Vulkan/GPU delegate, 21 (x,y,z) landmarks.
4. **KinematicTokenClassifier.kt** — real geometry: Euclidean distances + joint angles among the
   21 landmarks → semantic tokens (thumb-index proximity → `[Coffee]`/`[Thumbs_Up]`, etc.).
5. **NpuGemmaInferenceEngine.kt** — Gemma-3 1B INT4 via Google AI Edge SDK, bound to NNAPI
   delegate, low-latency preference; thread-safe system-prompt matrix keyed by `interactorLanguage`.
6. **SynchronizedAudioSpeaker.kt** — native `TextToSpeech` with init validation, missing-pack
   handling, runtime locale switching.
7. **DspAudioCaptureStream.kt** — low-latency mono capture, AEC + noise suppression.
8. **DeterministicSpeechRecognizer.kt** — native offline `SpeechRecognizer`,
   `EXTRA_PREFER_OFFLINE`, locale = `interactorLanguage`.
9. **GemmaTranslationBridge.kt** — transcribed text → NNAPI Gemma → translate to `userNativeLanguage`.
10. **GattServerBleManager.kt** — full BLE GATT/RFCOMM loop; byte-serialize sentences + haptic
    metadata (double pulse = start, long pulse = complete) to the iQOO smartwatch.
11. **SensorFusionWipeDetector.kt** — accelerometer window-averaging shake detector → clear command.
12. **MonsterTransmitViewModel.kt** — MVI `StateFlow` machine; immutable `ViewState`
    (token stream, localized strings, BLE state, listening, target langs); mutations under `Mutex`.
13. **ShowcaseDashboardActivity.kt** — minimal Compose UI: top hardware-accelerated camera surface,
    bottom high-contrast debug text; `monsterHaloEffect()` modifier = animated gradient driven by
    the active voice-capture state.
