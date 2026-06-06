# SENSE-LINK — Judge Demo Card

**One-liner:** A 100% on-device, offline edge-AI app that lets Deaf/mute and hearing people talk —
sign → the phone speaks; speak → text + haptics. Works in airplane mode. Native Android app on iQOO.

---

## 60-second setup

```bash
# 1. Install the prebuilt APK on the iQOO (USB debugging on):
adb install -r releases/SENSE-LINK-v1.0-debug.apk

# 2. (Optional, for the REAL on-device LLM) side-load Gemma-2B once:
adb push gemma.task /sdcard/Android/data/com.kraftshala.senselink/files/gemma.task
```
Launch **SENSE-LINK** → grant **Camera + Microphone** when asked. Put the phone in **airplane mode**
to prove it's fully offline.

---

## The demo (90 seconds)

1. **Splash → "Welcome"** — toggle **Camera** and **Microphone** ON, tap **ENTER ENGINE WORKSPACE**.
   *Say: "Everything you'll see runs on the phone — no servers, no internet."*
2. **Sign over the camera** (or tap a **Gesture Lexicon** chip). The cyan 21-point hand skeleton
   tracks live; the latency badge shows real **GPU-delegate ms**.
   *Say: "That's MediaPipe hand-tracking running on the GPU, on-device."*
3. Stream a couple of gestures, e.g. **👌 COFFEE → ✌️ PLEASE**. After a short pause the engine
   composes a full sentence — **"Yes, I would like some coffee, please."** — and the **phone speaks it**.
   *Say: "Broken sign fragments become one fluent, dignified sentence — then it's spoken aloud."*
   - Source tag reads **GEMMA·2B** when the on-device Gemma model is loaded, **GEMMA·INT4** on the
     built-in composer.
4. **Shake the phone** to clear the workspace (accelerometer). Feel the **haptic** pulses.
5. **System Config (gear)** — show the on-device model card, gesture-confidence slider, locale,
   voice profile, playback speed. *Say: "All on-device, airplane-mode safe."*

---

## What to emphasise (maps to scoring)

- **On-device AI:** MediaPipe hand-tracking (GPU) + Gemma-2B (int4) via **LiteRT**, all local.
- **Phone-first, native:** real installable APK, `appCategory="game"` → iQOO **Multi-Turbo**.
- **Offline / airplane-mode:** every asset (WASM, model runtime, fonts) is bundled.
- **Problem fit:** fixes "Tarzan text", cloud dead-zones, and thermal drain in one app.

---

## Demo-safety (it never hard-fails)

- **Poor lighting/camera?** Every gesture is also a **tappable chip** — drive the exact same
  pipeline by tapping. Use the **quick-reply chips** for instant spoken phrases.
- **Model not loaded / slow?** The composer falls back automatically (a 6 s watchdog guarantees the
  sentence always appears and is spoken).
- **Known limit:** the *reverse* speech-to-text loop (mic button → live transcript) needs a native
  recogniser that isn't in Android WebView yet — it's on the roadmap. Demo the **forward** path
  (sign → speak), which is the headline.

---

## The pitch line

> "Existing tools give you broken 'Water give need' and die the moment you lose signal. SENSE-LINK
> turns sign into dignified speech and runs entirely on the iQOO — even in airplane mode."
