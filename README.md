# Flux Recorder

<div align="center">
  <img src="app/src/main/res/drawable/ic_splash_logo.png" alt="Flux Recorder Logo" width="120"/>
  
  ### Professional, High-Performance Screen Recording for Android
  
  [![Version](https://img.shields.io/badge/version-1.2.0-blue.svg)](https://github.com/IcradleInnovationsLtd/FluxRecorder/releases)
  [![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
  [![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)](https://www.android.com)
  [![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](https://developer.android.com/about/versions/nougat)
  [![Target SDK](https://img.shields.io/badge/targetSdk-35-blueviolet.svg)](https://developer.android.com/about/versions/15)
</div>

---

## 📱 About Flux Recorder

**Flux Recorder** is a modern, lightweight, high-performance screen recording and video editing application built with **Kotlin** and **Jetpack Compose**. Engineered for creators, gamers, educators, and developers, Flux Recorder captures crystal-clear video and synchronized dual audio with minimal battery and CPU footprint.

---

## ✨ Key Features

### 🎬 Ultra HD Video & High Frame Rates
- **Resolution Options**: 360p, 480p SD, 720p HD, 1080p Full HD, **2K (1440p)**, and **4K (2160p)**.
- **High-Refresh Rates**: Smooth capture at **30 FPS**, **60 FPS**, and **90 FPS**.
- **Hardware Acceleration**: Low-latency `MediaCodec` H.264 / AVC video encoding with Variable Bitrate (VBR) optimization.
- **Monotonic Presentation Timestamps (PTS)**: Gapless, desync-free pause and resume recording.

### 🎙️ Dual & Synchronized Audio Mixing
- **Internal Audio**: Direct system sound capture (Android 10+).
- **Microphone Input**: Crisp voiceover narration.
- **Simultaneous Mixing**: Record game/app audio and microphone commentary at the same time.
- **Zero-Allocation Audio Pipeline**: Preallocated buffer pools eliminate Garbage Collection (GC) frame drops.

### 👆 Visual Screen Taps & Touches
- **Native Touch Dots**: Renders visual circular touch indicators during recording for tutorials and app walkthroughs.
- **Auto-Toggle Mode**: Automatically enables touch circles when recording starts and restores previous settings upon completion.

### 📹 Hardware-Accelerated Facecam & Floating Bubble
- **Facecam Preview & Positioning**: Dedicated home screen preview button to position and test your front camera before recording.
- **Smart Auto-Dimming**: Floating control bubble dims to 20% opacity after 2 seconds of inactivity so it never blocks screen content.
- **Draggable Overlay**: Seamlessly reposition the controls anywhere on screen.

### ✂️ In-App Video Player & Editor
- **Media3 ExoPlayer**: High-fidelity in-app playback with scrubber, resolution badge, and metadata.
- **Precision Video Trimming**: Fast, lossless video trimming powered by AndroidX Media3 Transformer.
- **Instant Sharing & Scoped Storage**: Share directly to YouTube, WhatsApp, Discord, or export to Google Photos / Samsung Gallery.

### 📳 Shake-to-Stop Gesture & Quick Settings Tile
- **Shake Detection**: Stop recordings instantly with a quick shake of your device, customizable with a sensitivity slider.
- **Quick Settings Tile**: Start and stop recordings with 1 tap directly from the Android status bar pull-down shade.

### 🛡️ Anti-Black Screen & Unrestricted Capture
- **Continuous Recording**: Switch freely between apps without recordings unexpectedly freezing or pausing.
- **DRM Workaround Guide**: In-app instructions on capturing protected web streams without black screens.

### 📖 Offline User Manual (11 Sections)
- Built-in, comprehensive user guide covering all features, permissions, quick-start workflows, and troubleshooting tips.

---

## 🎨 UI/UX Design & Architecture

- **Material Design 3 (M3)**: Built with Jetpack Compose following Google's modern design standards.
- **True OLED Black (`#000000`)**: Maximizes battery life on AMOLED / OLED displays (Galaxy S22/S23/S24, Pixel, etc.).
- **Edge-to-Edge Support**: Compliant with Android 15 mandatory edge-to-edge drawing around cutouts and gesture bars.
- **Accessibility**: 48dp+ interactive touch targets and high-contrast typography.

---

## 🛠️ Technical Architecture

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.0+ (100%) |
| **UI Framework** | Jetpack Compose (Material 3, Accompanist, Coil 3) |
| **Architecture** | MVVM + Clean Architecture + Unidirectional Data Flow (UDF) |
| **Dependency Injection** | Dagger Hilt 2.51+ |
| **Video & Audio Pipeline** | Android MediaProjection, MediaCodec (H.264/AAC), MediaMuxer, AudioRecord |
| **Video Playback & Editing** | AndroidX Media3 (ExoPlayer 1.5+, Transformer, Effect) |
| **Camera** | Android Camera2 API + CameraX |
| **Min SDK / Target SDK** | Min SDK 24 (Android 7.0) • Target SDK 35 (Android 15) |

---

## 📋 Permissions Overview

Flux Recorder requests permissions dynamically and transparently only when corresponding features are enabled:

| Permission | Purpose |
|---|---|
| `RECORD_AUDIO` | Required for microphone and internal audio capture |
| `CAMERA` | Optional — only requested if Facecam overlay is enabled |
| `SYSTEM_ALERT_WINDOW` | Required to display the floating control bubble and facecam overlay |
| `POST_NOTIFICATIONS` | Displays the persistent recording notification with Pause/Resume/Stop controls |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Complies with Android 14+ foreground service projection requirements |
| `WRITE_SETTINGS` | Optional — allows automatic toggling of visual screen touch dots |

---

## 🔒 Privacy & Security

We believe in complete privacy:
- **100% Local Processing**: All video and audio streams are encoded and saved entirely on your device.
- **Zero Telemetry / Tracking**: No analytics SDKs, advertising libraries, or third-party tracking.
- **No Account Required**: Fully functional offline without sign-up or cloud sync.

---

## 🏗️ Building from Source

### Prerequisites
1. Android Studio Ladybug (2024.2.1+) or newer.
2. JDK 17 or JDK 21.
3. Android SDK Platform 35.

### Steps
```bash
# 1. Clone the repository
git clone https://github.com/IcradleInnovationsLtd/FluxRecorder.git
cd FluxRecorder

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Build Production Release APK and Play Store AAB
./gradlew assembleRelease bundleRelease
```

Build outputs:
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Play Store Bundle (AAB)**: `app/build/outputs/bundle/release/app-release.aab`

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  Crafted with ❤️ by <b>Icradle Innovations Ltd</b>
</div>