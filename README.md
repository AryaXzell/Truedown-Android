# Truedown Android

[**English**](README.md) | [**Bahasa Indonesia**](README.id.md)

[![Build and Release](https://github.com/aryaxzell/truedown-android/actions/workflows/build.yml/badge.svg)](https://github.com/aryaxzell/truedown-android/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A fast, lightweight, and modern native Android app to download TikTok videos, photo slideshows, and audio (MP3) without watermarks.

---

## Features

- **Direct Share Support:** Share videos or 1-photo slideshows directly from TikTok to Truedown to start automatic background downloads without extra steps.
- **Watermark-Free Videos:** Download crisp, original-quality videos with optional HD resolution support.
- **Selective Slideshow Picker:** Browse photo slideshows in a gallery grid and selectively download individual or batch photos.
- **Audio (MP3) Extraction:** Extract and save pure background music and audio tracks from TikTok posts.
- **Built-in Media Players:** Integrated video player (with orientation control and loop) and dedicated audio player.
- **Local Library Management:** Browse, share, and manage downloaded media locally with optional device storage cleanup on deletion.
- **Bilingual & Modern UI:** Complete localization in Indonesian and English, with Material Design 3 and Dynamic Color theming.
- **Privacy & Storage First:** Saves directly to `Movies/Truedown`, `Pictures/Truedown`, and `Music/Truedown`. No user tracking, no analytics, and no accounts required.

---

## Downloads

Download the latest release APK from our [GitHub Releases Page](https://github.com/aryaxzell/truedown-android/releases).

### Which APK to Choose?

| APK File | Target Architecture | Recommendation |
|---|---|---|
| `truedown-<version>-arm64-v8a.apk` | 64-bit ARM (`arm64-v8a`) | **Recommended for most modern Android devices.** Smaller file size and best performance. |
| `truedown-<version>-armeabi-v7a.apk` | 32-bit ARM (`armeabi-v7a`) | For older 32-bit Android phones. |
| `truedown-<version>-universal.apk` | All Supported ABIs | Choose this if you are unsure of your device architecture (larger file size). |

#### How to Check Your Device Architecture (ABI):
- **Using ADB:** Run `adb shell getprop ro.product.cpu.abilist`
- **Using Device Info Apps:** Look for the *Instruction Sets* or *CPU Architecture* field in any hardware information app.

---

## Installation & Updates

1. Download the appropriate `.apk` file from the [Releases](https://github.com/aryaxzell/truedown-android/releases) section.
2. If prompted, grant your browser or file manager permission to install apps from external sources.
3. Open the downloaded file to install the application.
4. **Updates:** Updates are published manually on GitHub Releases. To update, simply download and install the newer version APK over the existing installation.

---

## File Verification

Every release includes a SHA-256 checksum file alongside the APKs. To verify the integrity of your downloaded file:

### On Linux / macOS:
```bash
sha256sum -c truedown-<version>-<abi>.apk.sha256
```

### On Windows (PowerShell):
```powershell
Get-FileHash .\truedown-<version>-<abi>.apk -Algorithm SHA256
```
Compare the resulting hash with the contents of the provided `.sha256` file.

---

## Important Architecture & Compatibility Notes

- **Switching Between APK ABIs:** Android may treat changing from an architecture-specific build (e.g., `arm64-v8a`) to a `universal` build on the same version as a downgrade due to version code structures. If you need to switch ABI variants, please uninstall the previous APK first or remain on the matching architecture.
- **Debug vs Release Builds:** Release builds are signed with our official release key. You cannot install a release build over a debug build without uninstalling the debug version first.

---

## System Requirements & Testing Statement

- **Minimum Requirement:** Android 10 (API 29) or higher.
- **Target Platform:** Android 15 (API 35).
- **Verified Devices:** Tested and confirmed fully functional on Android 11 (64-bit, ARM64) on Infinix Hot 11 Play hardware baseline and modern Android emulators (API 29 to 35).
- **32-Bit Notice:** The `armeabi-v7a` build is compiled and signature-verified during CI, but has not been tested on a physical 32-bit device.

---

## Privacy Policy

- **No Analytics:** Truedown does not include third-party trackers, analytics SDKs, or crash beacons.
- **No User Accounts:** No registration or login required.
- **No Cloud Uploads:** Download requests are made directly from your device.

---

## Disclaimer

- Truedown is intended solely for personal use or downloading content that you own or have explicit permission to download.
- Truedown is an independent open-source project and is **not** affiliated with, endorsed by, or associated with TikTok, ByteDance, or TikWM.
- The user assumes full responsibility for compliance with copyright laws and terms of service.
- The application depends on third-party public API endpoints which may change or become unavailable without notice.

---

## Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK with Platform 35 and Build Tools installed

### Build Steps
```bash
# Clone the repository
git clone https://github.com/aryaxzell/truedown-android.git
cd truedown-android

# Build release APKs (unsigned)
./gradlew assembleRelease
```
The resulting split and universal APKs will be located in `app/build/outputs/apk/release/`.

---

## Contributing & Support

- **Bug Reports & Feedback:** Please open an issue on the [GitHub Issues](https://github.com/aryaxzell/truedown-android/issues) page using the provided issue forms.
- **Contributing:** Read our [Contribution Guidelines](CONTRIBUTING.md) before submitting a Pull Request.
- **Security Inquiries:** Review our [Security Policy](SECURITY.md).

---

## License

This project is licensed under the [MIT License](LICENSE) &copy; 2026 Arya Vallencia.
