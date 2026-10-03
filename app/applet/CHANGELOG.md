# 📜 Changelog

All notable changes to the **Truedown Android** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html), and is presented in a bilingual format (**English** & **Bahasa Indonesia**).

[![GitHub Release](https://img.shields.io/badge/Release-v1.1.1-blue?style=flat-square&logo=github)](https://github.com/AryaXzell/Truedown-Android/releases)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-29%20(Android%2010)-green?style=flat-square&logo=android)](https://developer.android.com)
[![Android Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-orange?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-brightgreen?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

---

## 🚀 [1.1.1] — 2026-10-03

> [!NOTE]  
> **Release Highlights**: This patch introduces a resilient multi-candidate Nightly in-app updater with smart GitHub Releases fallback, resolves Media3 opt-in lint warnings, delivers comprehensive WCAG AA accessibility improvements across all interactive controls, and reinforces MediaStore scoped storage reliability.

### 🌐 English

#### 🛠️ Fixes & Reliability
- **Nightly In-App Update Engine**:
  - Resolved an issue where Nightly updates failed due to `nightly.link` returning HTML landing pages instead of raw ZIP archives.
  - Implemented multi-candidate URL resolvers matching device CPU architectures (`arm64-v8a`, `armeabi-v7a`, `universal`).
  - Added an intelligent fallback mechanism to the GitHub Releases API if Nightly artifacts have expired past GitHub's 14-day retention window.
  - Sanitized external browser fallback links to point to active GitHub Actions workflows.
- **Media3 UnstableApi Compliance**:
  - Annotated `QuickMediaPreviewCard` and video preview sections with official `@OptIn(UnstableApi::class)`, eliminating Android Lint check failures.
- **Scoped Storage & Re-download Atomicity**:
  - Added strict null-checks on `ContentResolver.openOutputStream` to prevent ghost 0-byte entries in gallery storage.
  - Implemented proactive cleanup of prior MediaStore URIs during re-downloads (`RE_DOWNLOAD` policy) to eliminate orphaned media files.

#### ♿ Accessibility & UI/UX
- **Unified Switch Semantics**: Consolidated Settings switch rows into single-target `Role.Switch` semantics, eliminating double TalkBack focus.
- **Live Region Throttling**: Throttled `GlobalDownloadProgressIndicator` announcements to 25% milestone intervals (0%, 25%, 50%, 75%, 100%) to prevent screen reader verbal congestion.
- **Interactive Component Semantics**: Added explicit `Role.Button` and `Role.Checkbox` semantics and localized click action labels to Library cards, Recent items, and Slideshow photo selector tiles.
- **Full Localization**: Extracted and localized all remaining accessibility labels across Indonesian (`values-in`) and English (`values-en`).

---

### 🇮🇩 Bahasa Indonesia

#### 🛠️ Perbaikan & Stabilitas
- **Mesin Pembaruan Saluran Nightly In-App**:
  - Memperbaiki kegagalan pembaruan Nightly akibat URL pratinjau HTML pada endpoint `nightly.link`.
  - Mengimplementasikan pengunduh multi-kandidat otomatis yang menyesuaikan dengan arsitektur prosesor perangkat (`arm64-v8a`, `armeabi-v7a`, `universal`).
  - Menambahkan mekanisme fallback cerdas ke GitHub Releases API apabila artefak build Nightly telah melewati masa retensi 14 hari GitHub Actions.
  - Memperbarui tautan eksternal browser ke halaman workflow yang aktif dan bersih.
- **Kepatuhan Anotasi Media3**:
  - Menyematkan anotasi resmi `@OptIn(UnstableApi::class)` pada `QuickMediaPreviewCard`, menyelesaikan peringatan Android Lint.
- **Integritas Penyimpanan MediaStore Scoped Storage**:
  - Validasi ketat aliran penulisan berkas media untuk mencegah berkas kosong (0-byte) pada galeri.
  - Pembersihan otomatis entri MediaStore lama saat melakukan pengunduhan ulang agar tidak meninggalkan file sampah (*orphan files*).

#### ♿ Aksesibilitas & Tampilan
- **Semantik Switch Terpadu**: Menggabungkan baris switch Pengaturan menjadi satu target `Role.Switch` tanpa fokus ganda pada TalkBack.
- **Pembatasan Notifikasi Progres Suara**: Membatasi pengumuman pembaca layar pada progres unduhan menjadi kelipatan 25% agar navigasi tetap nyaman.
- **Peran & Label Aksesibilitas Jelas**: Menambahkan peran `Role.Button`, `Role.Checkbox`, dan label tindakan pada kartu Library, item terkini, dan kisi foto slideshow.
- **Lokalisasi Penuh**: Sinkronisasi 100% string aksesibilitas untuk Bahasa Indonesia dan Bahasa Inggris.

---

<details>
<summary><b>📦 Release Assets & ABI Compatibility Matrix</b></summary>

| Artifact Name | Target Architecture | Recommended Devices | Description |
|---|---|---|---|
| `truedown-arm64-v8a.apk` | 64-bit ARM (`arm64-v8a`) | ~95% Modern Android Phones | Optimized binary size, fastest execution |
| `truedown-armeabi-v7a.apk` | 32-bit ARM (`armeabi-v7a`) | Legacy 32-bit Devices | Minimal resource footprint for older hardware |
| `truedown-universal.apk` | All Supported ABIs | Universal Fallback | Contains native libraries for all architectures |

</details>

---

## 📦 [1.1.0] — 2026-10-02

<details>
<summary><b>Lihat Catatan Rilis Versi 1.1.0 (Click to expand)</b></summary>

### English
#### Added
- Media3 audio focus management across built-in video and audio players (pauses on incoming calls or audio noisy events).
- Accessible slideshow photo grid with standard 48dp touch targets, semantic checkbox roles, and custom accessibility actions.
- Unified media routing resolver for Home and Library screens, preventing blank playback states.
- 3-state notification permission onboarding flow with live resume sync and granular settings navigation.
- Cancel and Retry mechanisms for URL resolution on Home screen with cancellable OkHttp network requests.
- Dynamic version display linked directly to `BuildConfig.VERSION_NAME`.
- WCAG AA compliant color contrast tokens across light and dark themes.
- Enhanced clipboard filtering ignoring long logs and multi-line text, with sensitive clipboard flags on API 33+.

#### Changed
- Optimized background animations (Vinyl rotation & Shimmer) to eliminate unnecessary recompositions.
- Background asynchronous thumbnail loading on `Dispatchers.IO` for smoother library scrolling.
- Reset gallery deletion toggles per modal dialog to prevent unintended file removals.

---

### Bahasa Indonesia
#### Ditambah
- Manajemen fokus audio Media3 pada pemutar video & audio bawaan (otomatis jeda saat panggilan masuk).
- Grid pemilih foto slideshow dengan target sentuh standar 48dp dan aksi aksesibilitas.
- Resolver rute media seragam pada layar Home dan Library untuk mencegah layar kosong.
- Alur izin notifikasi 3-status pada onboarding dengan sinkronisasi status otomatis.
- Tombol Batal & Coba Lagi pada proses pemeriksaan tautan di layar Home.
- Tampilan versi aplikasi dinamis langsung dari `BuildConfig.VERSION_NAME`.
- Penyesuaian kontras warna sesuai standar WCAG AA pada tema terang dan gelap.

</details>

---

## 📦 [1.0.0] — 2026-09-30

<details>
<summary><b>Lihat Catatan Rilis Versi 1.0.0 (Click to expand)</b></summary>

### English
#### Added
- Initial official native release of Truedown for Android (Min SDK 29 / Android 10).
- Automatic background download on receiving TikTok Share intent for single videos and 1-photo posts.
- Selective photo picker grid for multi-photo TikTok slideshows with full photo viewer.
- Standalone audio (MP3) extraction and downloading.
- Built-in video player with loop mode and manual orientation toggle.
- Built-in audio player with theme integration and automated background pause.
- Full bilingual localization support for English and Bahasa Indonesia.
- Local Library to manage, play, share, and delete downloads with optional gallery file removal.
- Material Design 3 UI with dynamic color support for Android 12+.
- ABI-split APK packaging for `arm64-v8a`, `armeabi-v7a`, and `universal` devices.

---

### Bahasa Indonesia
#### Ditambah
- Rilis resmi pertama aplikasi native Truedown untuk Android (Min SDK 29 / Android 10).
- Fitur unduh otomatis di latar belakang saat menerima Share dari TikTok untuk video dan postingan 1 foto.
- Grid pemilih foto slideshow untuk memilih foto tertentu atau unduh semua sekaligus, dilengkapi photo viewer.
- Ekstraksi dan pengunduhan file audio (MP3) dari postingan TikTok.
- Pemutar video built-in dengan mode loop dan tombol rotasi manual.
- Pemutar audio built-in yang mengikuti tema aplikasi dan otomatis pause di background.
- Pengelolaan riwayat Library lokal untuk melihat, memutar, membagikan, dan menghapus file.
- Tampilan modern Material Design 3 dan dukungan warna dinamis untuk Android 12+.

</details>
