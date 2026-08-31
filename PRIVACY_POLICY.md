# Privacy Policy for Flux Recorder

**Last Updated:** August 31, 2026  
**Developer / Publisher:** Icradle Innovations Ltd  
**Contact Email:** icradleinnovations@gmail.com  
**Website:** https://icradle.io

---

## 1. Introduction

**Flux Recorder** ("we", "our", or "the App"), developed by **Icradle Innovations Ltd**, is committed to protecting your privacy. This Privacy Policy explains our practices regarding data handling and permissions when you use our screen recording and video editing application.

---

## 2. Zero Data Collection Policy

**We do NOT collect, store, transmit, sell, or share any personal information, video recordings, audio recordings, or telemetry data.**

Flux Recorder operates **100% locally on your Android device**:
- **No Personal Data Collection**: We do not collect names, email addresses, device IDs, or IP addresses.
- **No Analytics / Telemetry**: The App contains no third-party tracking, profiling, or analytics SDKs (e.g., no Firebase Analytics, no Facebook SDK).
- **No Advertising Networks**: The App does not contain ad banners, interstitials, or tracking identifiers.
- **No External Servers**: Video streams, audio streams, and recordings are never transmitted to external cloud servers.

---

## 3. Permissions Used and Why

Flux Recorder requests only the minimum set of permissions necessary to deliver core screen recording and editing functionalities:

### 🎙️ Audio Capture (`RECORD_AUDIO`)
- **Purpose**: Used to capture microphone commentary and/or internal system audio when enabled by the user.
- **Usage**: Audio processing occurs in real-time in memory and is muxed directly into your local MP4 file.

### 📹 Camera Access (`CAMERA`)
- **Purpose**: Powers the optional **Facecam** floating overlay window.
- **Usage**: Camera preview is rendered locally to a hardware texture overlay. Camera data is only captured when you explicitly toggle Facecam.

### 🪟 Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)
- **Purpose**: Allows Flux Recorder to display the floating control bubble and Facecam window over other applications during active recordings.

### 🔔 Notifications (`POST_NOTIFICATIONS`)
- **Purpose**: Displays the foreground recording status and persistent notification bar with quick Pause, Resume, and Stop controls.

### ⚙️ Modify System Settings (`WRITE_SETTINGS`)
- **Purpose**: Optional — enables automatic toggling of visual screen touch indicators ("Show Screen Taps") when recording starts and restores previous system settings when recording stops.

### 💾 Storage & Media Access (`READ_MEDIA_VIDEO` / Scoped Storage)
- **Purpose**: Enables the in-app recordings library and video editor to list, play, trim, and share your recorded MP4 files. All recordings reside in your public `Movies/FluxRecorder` folder or app-private storage.

### ⚡ Foreground Services (`FOREGROUND_SERVICE_*`)
- **Purpose**: Complies with Android 14+ requirements to keep the recording engine, audio mixer, and MediaProjection active while you interact with other apps.

---

## 4. Local Storage and Data Control

You retain 100% ownership and full control over your media files:
- All MP4 video recordings and thumbnails are stored directly on your device storage (`/Movies/FluxRecorder/`).
- You can play, edit, trim, share, or permanently delete any recording at any time through the in-app library or any Android file manager.
- Uninstalling the application does not delete your saved public recordings in `/Movies/FluxRecorder/`.

---

## 5. Third-Party Services and Links

Flux Recorder does not integrate with any third-party analytics, tracking, or data-broker services. If you choose to share a video using Android's system share sheet (e.g., to YouTube, WhatsApp, Google Drive), data handling is governed by the respective third-party platform's privacy policy.

---

## 6. Children's Privacy (COPPA Compliance)

Flux Recorder does not collect any personal information from any user, including children under the age of 13. The application is completely safe for all age groups.

---

## 7. Security and Best Practices

Because all processing occurs strictly on your local hardware:
- Your recordings are as secure as your physical device.
- We recommend keeping device screen lock (PIN/Fingerprint) enabled to protect your local recordings.

---

## 8. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect new app features or regulatory requirements. Any updates will be published within the app and on our official GitHub repository.

---

## 9. Contact Us

If you have any questions or feedback regarding this Privacy Policy or our privacy practices, please contact:

* **Company:** Icradle Innovations Ltd
* **Email:** icradleinnovations@gmail.com
* **Website:** [icradleinnovations.com](https://icradle.io)
* **GitHub:** [https://github.com/IcradleInnovationsLtd/FluxRecorder](https://github.com/IcradleInnovationsLtd/FluxRecorder)
