# Changelog

All notable changes to the Truedown Android project will be documented in this file.
Format follows standard versioning practices in bilingual format (English & Bahasa Indonesia).

---

## [1.1.0] - 2026-10-02

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

## [1.0.0] - 2026-09-30

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

#### Changed
- Enhanced clipboard observer on Home screen to detect and offer one-tap inspection for TikTok links.

#### Fixed
- Initial release stability improvements and graceful handling of network timeouts or expired media URLs.

---

### Bahasa Indonesia

#### Ditambah
- Rilis resmi pertama aplikasi native Truedown untuk Android (Min SDK 29 / Android 10).
- Fitur unduh otomatis di latar belakang saat menerima Share dari TikTok untuk video dan postingan 1 foto.
- Grid pemilih foto slideshow untuk memilih foto tertentu atau unduh semua sekaligus, dilengkapi photo viewer.
- Ekstraksi dan pengunduhan file audio (MP3) dari postingan TikTok.
- Pemutar video built-in dengan mode loop dan tombol rotasi manual.
- Pemutar audio built-in yang mengikuti tema aplikasi dan otomatis pause di background.
- Dukungan lokalisasi penuh untuk Bahasa Indonesia dan Bahasa Inggris.
- Pengelolaan riwayat Library lokal untuk melihat, memutar, membagikan, dan menghapus file dengan opsi hapus dari galeri.
- Tampilan modern Material Design 3 dan dukungan warna dinamis untuk Android 12+.
- Rilis APK split untuk arsitektur `arm64-v8a`, `armeabi-v7a`, dan `universal`.

#### Diubah
- Deteksi link TikTok otomatis di clipboard pada layar Home untuk kemudahan periksa link dalam satu ketukan.

#### Diperbaiki
- Penanganan kestabilan koneksi, timeout jaringan, serta tautan kadaluarsa.
