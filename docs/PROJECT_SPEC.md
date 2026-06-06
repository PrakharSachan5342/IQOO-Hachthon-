# PROJECT SPEC — Monster Transmit / SENSE-LINK

100% offline, hardware-accelerated, multi-modal accessibility ecosystem for the flagship iQOO
mobile paradigm. Removes communication barriers between Deaf, mute and hearing people.

## Problems addressed
1. **"Tarzan Text"** — sign language uses spatial + topic-comment grammar, not word-for-word.
   Naive translators output broken literal blocks ("Water give need"). SENSE-LINK uses an
   on-device context engine to rewrite fragments into fluent, dignified sentences.
2. **Cloud connectivity trap** — assistive apps depend on the cloud and fail in subways,
   elevators, clinical dead zones, crowded venues. SENSE-LINK is 100% offline / airplane-mode ready.
3. **Thermal & memory constraints** — continuous CV + cellular radio overheat & drain the phone.
   On-device-only computation + iQOO vapor chamber keeps it cool and sustained.

## Solution architecture (target native stack)
- **Vision pipeline:** Android CameraX → MediaPipe Hand Landmarker on GPU/Vulkan delegate →
  21 skeletal landmarks (<15ms).
- **Context layer:** Gemma-3 1B INT4 (4-bit) running fully offline (via Google AI Edge /
  MediaPipe LLM Inference), low temperature (~0.2), rewrites token fragments into natural sentences.
- **Audio out:** native Android `TextToSpeech` (locale-aware).
- **Audio in:** native `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE` (0 MB bloat).
- **Wearable relay:** finalized text → BLE GATT/RFCOMM → iQOO smartwatch text + haptic patterns
  (double pulse = sentence start, long pulse = sentence complete).
- **Sensor fusion:** accelerometer wrist-shake → clear workspace.
- **State:** MVI / Jetpack Compose `StateFlow` ViewModel (`uiOutputText`, `isListeningActive`,
  `currentLocale`), mutations under `Mutex`.

## Example millisecond pipeline (target)
Sign "Thumbs_Up + Coffee" → CameraX frame → MediaPipe 21 vectors (~15ms) → token "YES COFFEE" →
Gemma INT4 inference (~18-45ms) → "Yes, I would like some coffee, please." → TTS speaks →
text mirrored to watch with haptic. Total < 60ms.

## Multilingual
Two state props: `userNativeLanguage`, `interactorLanguage`. Tokens are language-independent;
Gemma is prompted to output the target language; TTS/STT locales switch accordingly.

## iQOO hardware optimizations (pitch points)
- Declare `android:appCategory="game"` → Multi-Turbo keeps high-perf cores hot.
- Route LLM tensors to **NNAPI** (NPU) with low-latency preference (~45ms → ~18ms).
- Zero-copy camera frames via `HardwareBuffer` (saves ~15ms/frame).
- Offload landmark + Monster Halo rendering to the display co-processor at 120/144Hz.
- Vapor chamber → scan every frame at 60 FPS without throttling.

## UI screens
1. **Splash** — #0E0E10 bg, glowing orange infinity/diamond mark, integrity check, ~800ms.
2. **Permissions** — camera + mic toggles, "Initializing On-Device Core Model" bar.
3. **Main Terminal** — top 50% live camera + cyan 21-point overlay; bottom 50% high-contrast
   transcription; "Monster Halo" breathing border pulses while listening.
4. **System Config / Engine Profiler** — LLM model card, gesture confidence slider, wearable
   toggle, target dictionary (locale), vocal profile, playback velocity, tactile + viewport toggles.
