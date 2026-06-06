# CLAUDE.md — SENSE-LINK Project Brief

> This file is the single source of truth for Claude Code. Read it fully before doing anything.
> It carries over the entire context from the original planning + build session.

---

## 1. What this is

**SENSE-LINK** (internal codename: *Project Monster Transmit*) is a **100% on-device, offline
edge-AI accessibility app** that removes communication barriers between **Deaf / mute** and
**hearing** people. Built for the **iQOO Hackathon 2026** ("Red Light, Green Light"),
demoed live on an iQOO phone.

**Core loop:**
- Deaf/mute user signs → camera + hand tracking → gesture tokens → on-device language model
  rewrites fragments into a fluent sentence → phone speaks it aloud (TTS).
- Hearing person replies → mic → offline speech-to-text → text on screen + haptic buzz
  (relayed to a smartwatch in the full vision).
- Shake phone to clear. Everything runs locally — works in airplane mode.

The problem it solves: existing tools produce broken literal "Tarzan text" ("Water give need"),
depend on the cloud (fail in metros/elevators/crowds), and overheat the phone. SENSE-LINK fixes
all three by running on-device.

---

## 2. Current state (IMPORTANT — already built)

A **fully working web MVP** already exists and is **live**:

- **File:** `index.html` (repo root) — single self-contained file, no build step.
- **Live URL:** https://prakharsachan5342.github.io/SENSE-LINK/  (GitHub Pages, HTTPS)
- **Runs on the iQOO phone's browser** with real camera + mic permissions.

### What is GENUINELY REAL in the web MVP (runs on-device, no server):
- Live camera via `getUserMedia` (rear/front, flip button)
- On-device hand tracking via **MediaPipe Hand Landmarker** (21 3D landmarks, GPU delegate),
  loaded client-side from CDN — measured inference latency shown live in the UI
- Real gesture classifier: Euclidean distances + finger-extension geometry over the 21 landmarks
  → semantic tokens (YES, COFFEE, PLEASE, HELLO, HELP, WATER, GO)
- Real Text-to-Speech (Web Speech API) — locale, voice profile (M/F/Neutral) and rate wired to config
- Real Speech-to-Text reverse loop (Web Speech API)
- Real shake-to-clear via DeviceMotion accelerometer (window-averaging detector)
- Real haptics via `navigator.vibrate` (phone stands in for the watch)
- Full UI flow: Splash → Onboarding/Permissions → Main Terminal → System Config,
  plus Library / Sync / Profile tabs, and the "Monster Halo" breathing border while listening

### What are STAND-INS (be honest about these):
- The **Gemma-3 1B INT4** "rewrite fragments → fluent sentence" step is a **deterministic
  rule-based composer** in the web MVP (the architecture is designed for a real on-device LLM).
- The **BLE smartwatch GATT link** is represented by **phone haptics + a sync indicator**.

### Demo-safety design (do not remove):
Every gesture is also a **tappable chip** in the Gesture Lexicon, so the operator can drive the
exact same pipeline by tapping if camera/lighting is poor on stage. The app never hard-fails:
if MediaPipe or the camera don't load, it drops to manual mode with a clear status.

---

## 3. THE GOAL FOR CLAUDE CODE (next task)

Turn the working web app into a **real native Android app in Kotlin + Jetpack Compose**, the
fastest low-risk way, and produce an **installable APK** for the demo.

**Chosen approach: Kotlin + Jetpack Compose WebView shell.**
This is a genuine native Android app (Compose UI, builds to an APK) that reuses 100% of the
existing web code. Steps:

1. New Android project, Empty Compose Activity, min SDK ~26+, package e.g. `com.kraftshala.senselink`.
2. `AndroidManifest.xml`: permissions `INTERNET`, `CAMERA`, `RECORD_AUDIO`, `VIBRATE`;
   `android:hardwareAccelerated="true"`. (The original spec also sets `appCategory="game"` to
   trigger iQOO Multi-Turbo — keep that for the pitch.)
3. A single Composable hosting `AndroidView { WebView }`:
   - `settings.javaScriptEnabled = true`
   - `settings.mediaPlaybackRequiresUserGesture = false`
   - `settings.domStorageEnabled = true`
   - load the live URL **or** bundle `index.html` in `assets/` and load `file:///android_asset/index.html`
4. **Critical gotcha that makes the camera work inside WebView:**
   set a `WebChromeClient` overriding `onPermissionRequest { it.grant(it.resources) }`,
   AND request `CAMERA` + `RECORD_AUDIO` at runtime before loading the WebView.
5. Build → APK. Test on the iQOO phone.

**Stretch (only if time allows):** replace pieces of the web pipeline with the real native stack
described in `docs/PROJECT_SPEC.md` (MediaPipe Tasks Android, on-device Gemma via MediaPipe
LLM Inference / Google AI Edge, native Android TTS/SpeechRecognizer, real BLE GATT). Do NOT
attempt the full native rewrite the night before — the WebView APK is the deliverable.

---

## 4. Constraints & judging (why decisions were made)

Scoring rewards **phone-first execution (25%)** and **on-device AI (20%)**, plus iQOO Office Kit
usage (25%), problem fit (20%), craft & pitch (10%). The demo MUST run on the iQOO phone with a
native, on-device feel. That's why the WebView-APK path matters: it gives a real installable app
while keeping the genuinely on-device MediaPipe + speech pipeline.

---

## 5. Repo layout

```
/index.html            → the working web MVP (also the WebView payload)
/CLAUDE.md             → this file
/docs/PROJECT_SPEC.md  → full Monster Transmit architecture spec
/docs/THEME.md         → Sense-Link visual system (colors, fonts, screens)
/docs/BUILD_PROMPT.md  → original Kotlin generation prompt (for the native stretch)
```

## 6. Working style notes
- Iterative and precise. Read files before acting. Keep the existing aesthetic exactly.
- Prefer minimal, low-overhead UI; don't bloat. Keep the demo-safety manual fallback.
