# Truedown Android — Baseline & Execution Status Part 2

Tanggal: 2026-10-02  
Basis Kode: Truedown Android v1.1.0

Berikut adalah rekapitulasi status implementasi dan verifikasi untuk seluruh requirement FINAL Part 2 (R2-01 s/d R2-16):

| ID | Status | Bukti di Kode (File:Baris) | Keterangan |
|---|---|---|---|
| R2-01 | Sudah terpenuhi | `BuiltInVideoPlayerScreen.kt:165-166`, `BuiltInAudioPlayerScreen.kt:146-147` | `setAudioAttributes(audioAttributes, true)` dan `setHandleAudioBecomingNoisy(true)` terpasang di kedua pemutar. |
| R2-02 | Sudah terpenuhi | `SlideshowGridScreen.kt:257-320` | Lingkaran pilih memiliki target sentuh >= 48dp, `Role.Checkbox`, toggleableState, deskripsi semantik "Foto n dari N", dan `CustomAccessibilityAction`. |
| R2-03 | Sudah terpenuhi | `MainActivity.kt:258-270`, `BuiltInVideoPlayerScreen.kt:85-134` | Unified `resolveOpenTarget` menangani tipe media; player menampilkan state dan pesan informatif saat file/URI kosong atau tidak ditemukan. |
| R2-04 | Sudah terpenuhi | `MainViewModel.kt:384-414`, `LibraryScreen.kt:793-799, 890-896` | `deletePosts` mengembalikan `DeleteResult.DeletedButFilesFailed` dan menampilkan snackbar `delete_files_failed` saat gagal menghapus berkas galeri. |
| R2-05 | Sudah terpenuhi | `LibraryScreen.kt:127-128, 757, 792, 811, 826, 851, 889` | Variabel state hapus galeri dipisah (`deleteFromGallerySingle` & `deleteFromGalleryBulk`) dan direset ke false saat dialog ditutup/dibatalkan/dikonfirmasi. |
| R2-06 | Sudah terpenuhi | `OnboardingScreen.kt:97-152, 214-230, 755-815` | Izin notifikasi 3-state (aktif, minta izin, buka pengaturan) dengan resume lifecycle check; tombol Lewati hanya aktif pada halaman 1 & 2 dan melompat ke halaman 3. |
| R2-07 | Sudah terpenuhi | `HomeScreen.kt:162-178, 551-568`, `MainViewModel.kt:272-315`, `TikWmDownloadProvider.kt:98-112` | Tombol Batal saat loading resolve; aksi Coba lagi pada snackbar error; OkHttp call dibatalkan seketika melalui `suspendCancellableCoroutine`. |
| R2-08 | Sudah terpenuhi | `SettingsScreen.kt:498-506` | Versi aplikasi dinamis mengambil nilai dari `BuildConfig.VERSION_NAME` dengan format resource string. |
| R2-09 | Sudah terpenuhi | `SettingsScreen.kt:580-820`, `OnboardingScreen.kt:439-470, 887-940`, `GlobalDownloadProgressIndicator.kt:105-135`, `FloatingPillSnackbar.kt:40-70` | Radio button menggunakan `Modifier.selectableGroup()` dan `selectable(role = Role.RadioButton)`; chip & indikator ber-role Button; snackbar menghormati durasi aksesibilitas sistem. |
| R2-10 | Sudah terpenuhi | `ui/theme/Color.kt:28,47,78`, `HomeScreen.kt:446`, `DeveloperLogsDialog.kt:252` | Kontras `LightSecondary` (#0369A1) >= 4.5:1; border input >= 3:1; badge log WARN (#8D2F00 di atas #FFE0B2) >= 4.5:1. |
| R2-11 | Sudah terpenuhi | `BuiltInAudioPlayerScreen.kt:173-195, 255`, `Shimmer.kt:43-94` | Rotasi vinyl menggunakan `graphicsLayer { rotationZ = ... }`; Shimmer menggunakan satu shared `InfiniteTransition` via CompositionLocal dan mendukung reduce-motion. |
| R2-12 | Sudah terpenuhi | `LibraryScreen.kt:1013-1031` | Pengecekan file thumbnail dan pembuatan thumbnail video dijalankan asinkron via `produceState` di `Dispatchers.IO`. |
| R2-13 | Sudah terpenuhi | `BuiltInVideoPlayerScreen.kt:399-420` | Slider video hanya memanggil `seekTo` pada `onValueChangeFinished`; waktu otomatis tersinkronisasi dan kontrol tetap terlihat saat dragging slider. |
| R2-14 | Sudah terpenuhi | `MainViewModel.kt:253-261`, `DeveloperLogsDialog.kt:163-174` | `isTikTokUrl` memvalidasi panjang dan baris baru; Salin Log menambahkan flag `EXTRA_IS_SENSITIVE` pada Android 13+. |
| R2-15 | Sudah terpenuhi | `README.md`, `README.id.md`, `CHANGELOG.md` | Bagian Kebijakan Privasi mengungkap keterbukaan layanan pihak ketiga TikWM; CHANGELOG diperbarui untuk rilis v1.1.0 bilingual. |
| R2-16 | Sudah terpenuhi | `themes.xml:2`, `Theme.kt:96`, `AndroidManifest.xml:26` | Sisa nama template `Theme.MyApplication` dan `MyApplicationTheme` telah dibersihkan sepenuhnya menjadi `Theme.Truedown` dan `TruedownTheme`. |
