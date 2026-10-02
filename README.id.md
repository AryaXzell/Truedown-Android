# Truedown Android

[**English**](README.md) | [**Bahasa Indonesia**](README.id.md)

[![Build and Release](https://github.com/aryaxzell/truedown-android/actions/workflows/build.yml/badge.svg)](https://github.com/aryaxzell/truedown-android/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Aplikasi Android native yang cepat, ringan, dan modern untuk mengunduh video, slideshow foto, dan audio (MP3) TikTok tanpa watermark.

---

## Fitur Utama

- **Dukungan Share Langsung:** Bagikan video atau slideshow 1 foto langsung dari TikTok ke Truedown untuk mengunduh otomatis di latar belakang tanpa langkah tambahan.
- **Video Tanpa Watermark:** Simpan video berkualitas jernih dan original dengan dukungan opsi resolusi HD.
- **Pemilih Foto Slideshow:** Lihat thumbnail slideshow dalam bentuk grid dan pilih foto tertentu atau unduh semua sekaligus.
- **Ekstraksi Audio (MP3):** Ambil lagu dan audio latar belakang dari postingan TikTok secara praktis.
- **Pemutar Media Built-in:** Pemutar video terintegrasi (dengan kunci orientasi dan loop) serta pemutar audio mandiri.
- **Pengelolaan Library Lokal:** Kelola, putar, dan bagikan file yang telah diunduh, lengkap dengan opsi pembersihan file galeri saat dihapus.
- **Dua Bahasa & Desain Modern:** Mendukung Bahasa Indonesia dan Bahasa Inggris secara penuh, menggunakan Material Design 3 dan Warna Dinamis.
- **Privasi & Penyimpanan Teratur:** File tersimpan rapi di folder `Movies/Truedown`, `Pictures/Truedown`, dan `Music/Truedown`. Tanpa analitik, tanpa pelacak, dan tanpa akun.

---

## Unduhan

Unduh berkas APK rilis terbaru di [Halaman GitHub Releases](https://github.com/aryaxzell/truedown-android/releases).

### Pilih APK yang Mana?

| Berkas APK | Arsitektur Target | Rekomendasi |
|---|---|---|
| `truedown-<versi>-arm64-v8a.apk` | ARM 64-bit (`arm64-v8a`) | **Direkomendasikan untuk sebagian besar HP modern.** Ukuran berkas lebih ringkas dan performa maksimal. |
| `truedown-<versi>-armeabi-v7a.apk` | ARM 32-bit (`armeabi-v7a`) | Untuk perangkat Android 32-bit lawas. |
| `truedown-<versi>-universal.apk` | Semua Arsitektur (Universal) | Pilih ini jika kamu ragu dengan arsitektur perangkatmu (ukuran berkas lebih besar). |

#### Cara Mengecek Arsitektur HP (ABI):
- **Melalui ADB:** Jalankan perintah `adb shell getprop ro.product.cpu.abilist`
- **Melalui Aplikasi Info Perangkat:** Periksa bagian *Instruction Sets* atau *CPU Architecture* di aplikasi informasi spesifikasi perangkat.

---

## Cara Pasang & Memperbarui

1. Unduh berkas `.apk` yang sesuai dari bagian [Releases](https://github.com/aryaxzell/truedown-android/releases).
2. Jika diminta, izinkan browser atau pengelola file untuk memasang aplikasi dari sumber tidak dikenal.
3. Buka file yang selesai diunduh untuk menyelesaikan pemasangan.
4. **Pembaruan:** Pembaruan dirilis secara manual di GitHub Releases. Untuk memperbarui, cukup unduh dan pasang APK versi terbaru di atas instalasi lama.

---

## Verifikasi Berkas

Setiap rilis dilengkapi berkas checksum SHA-256 untuk memastikan keaslian berkas:

### Di Linux / macOS:
```bash
sha256sum -c truedown-<versi>-<abi>.apk.sha256
```

### Di Windows (PowerShell):
```powershell
Get-FileHash .\truedown-<versi>-<abi>.apk -Algorithm SHA256
```
Cocokkan nilai hash yang dihasilkan dengan isi berkas `.sha256` yang disediakan.

---

## Catatan Penting Arsitektur & Kompatibilitas

- **Berpindah Varian ABI:** Android dapat menolak peralihan dari build khusus arsitektur (misal: `arm64-v8a`) ke build `universal` pada nomor versi yang sama karena struktur version code. Jika ingin berganti varian, silakan copot (uninstall) versi sebelumnya terlebih dahulu atau tetap gunakan varian ABI yang sama.
- **Build Debug vs Release:** Build rilis resmi ditandatangani dengan kunci rilis kami. Kamu tidak dapat memasang build release di atas build debug tanpa mencopot build debug terlebih dahulu.

---

## Persyaratan Sistem & Pernyataan Pengujian

- **Kebutuhan Minimum:** Android 10 (API 29) atau lebih baru.
- **Target Platform:** Android 15 (API 35).
- **Perangkat yang Dites:** Diuji dan dipastikan berfungsi pada Android 11 (64-bit, ARM64) pada perangkat baseline Infinix Hot 11 Play serta emulator resmi Android (API 29 hingga 35).
- **Catatan 32-Bit:** Varian `armeabi-v7a` dikompilasi dan lolos verifikasi tanda tangan saat build CI, namun belum diuji langsung pada perangkat fisik 32-bit.

---

## Kebijakan Privasi

- **Tanpa Analitik:** Truedown tidak menyertakan SDK analitik pihak ketiga atau pelacak data.
- **Tanpa Akun:** Tidak memerlukan pendaftaran atau login.
- **Keterbukaan Layanan Pihak Ketiga:** Link yang kamu tempel atau bagikan dikirim ke layanan pihak ketiga (TikWM) untuk mengambil informasi media dan tautan unduhan; Truedown tidak mengirim data pribadi lainnya.
- **Penyimpanan Lokal:** Seluruh berkas unduhan, basis data riwayat, dan preferensi aplikasi disimpan secara lokal di perangkat pengguna.

---

## Disclaimer

- Truedown ditujukan semata-mata untuk penggunaan pribadi atau mengunduh konten milik sendiri / yang telah diizinkan.
- Truedown adalah proyek open-source independen dan **tidak berafiliasi**, didukung, atau terkait dengan TikTok, ByteDance, maupun TikWM.
- Pengguna bertanggung jawab penuh atas kepatuhan terhadap hak cipta dan ketentuan layanan platform.
- Aplikasi bergantung pada endpoint API publik pihak ketiga yang dapat berubah atau dihentikan sewaktu-waktu tanpa pemberitahuan.

---

## Membangun dari Source Code

### Prasyarat
- JDK 17 atau lebih baru
- Android SDK dengan Platform 35 dan Build Tools terpasang

### Langkah Build
```bash
# Clone repository
git clone https://github.com/aryaxzell/truedown-android.git
cd truedown-android

# Build APK release (unsigned)
./gradlew assembleRelease
```
Berkas APK hasil build akan berada di `app/build/outputs/apk/release/`.

---

## Kontribusi & Bantuan

- **Laporan Bug & Masukan:** Silakan buat tiket di [GitHub Issues](https://github.com/aryaxzell/truedown-android/issues) menggunakan formulir yang tersedia.
- **Panduan Kontribusi:** Baca [Panduan Kontribusi](CONTRIBUTING.md) sebelum mengirimkan Pull Request.
- **Kebijakan Keamanan:** Baca [Kebijakan Keamanan](SECURITY.md).

---

## Lisensi

Proyek ini dilisensikan di bawah [Lisensi MIT](LICENSE) &copy; 2026 Arya Vallencia.
