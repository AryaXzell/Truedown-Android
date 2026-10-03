package com.aryaxzell.truedown.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

sealed class NightlyUpdateState {
    object Idle : NightlyUpdateState()
    object Checking : NightlyUpdateState()
    data class UpdateAvailable(
        val runId: String,
        val commitMessage: String,
        val publishedAt: String
    ) : NightlyUpdateState()
    object AlreadyLatest : NightlyUpdateState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L,
        val remainingSeconds: Long = -1L
    ) : NightlyUpdateState()
    data class Extracting(val status: String) : NightlyUpdateState()
    data class ReadyToInstall(
        val apkFile: File,
        val selectedAbi: String = ""
    ) : NightlyUpdateState()
    data class Error(val message: String) : NightlyUpdateState()
}

object NightlyUpdateManager {

    private const val NIGHTLY_URL = "https://nightly.link/AryaXzell/Truedown-Android/workflows/build/main?preview&h=c9122b50d061e55e3d2d601154766a71c9e9de40"
    private const val GITHUB_RUNS_API = "https://api.github.com/repos/AryaXzell/Truedown-Android/actions/runs?branch=main&status=success&per_page=1"

    private val _updateState = MutableStateFlow<NightlyUpdateState>(NightlyUpdateState.Idle)
    val updateState: StateFlow<NightlyUpdateState> = _updateState.asStateFlow()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                AppLogger.e("NightlyUpdate", "Gagal membuka pengaturan install unknown apps", e)
            }
        }
    }

    fun resetState() {
        _updateState.value = NightlyUpdateState.Idle
    }

    /**
     * Mendeteksi arsitektur CPU utama perangkat pengguna secara pintar.
     * Mengembalikan tag arsitektur: arm64-v8a, armeabi-v7a, x86_64, atau universal.
     */
    fun getDeviceCpuAbi(): String {
        val abis = Build.SUPPORTED_ABIS
        if (abis.isNullOrEmpty()) return "universal"
        for (abi in abis) {
            val lower = abi.lowercase()
            when {
                lower.contains("arm64") || lower.contains("aarch64") -> return "arm64-v8a"
                lower.contains("v7a") || lower.contains("arm") -> return "armeabi-v7a"
                lower.contains("x86_64") -> return "x86_64"
                lower.contains("x86") -> return "x86"
            }
        }
        return abis[0]
    }

    /**
     * Memeriksa ketersediaan build Nightly terbaru dari GitHub API sebelum mengunduh.
     */
    suspend fun checkForNightlyUpdate(): Result<JSONObject> = withContext(Dispatchers.IO) {
        _updateState.value = NightlyUpdateState.Checking
        AppLogger.i("NightlyUpdate", "Memeriksa pembaruan Nightly dari GitHub Actions API...")

        try {
            val request = Request.Builder()
                .url(GITHUB_RUNS_API)
                .header("User-Agent", "Truedown-Android-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                AppLogger.w("NightlyUpdate", "Gagal cek API status HTTP ${response.code}, lanjut unduh langsung...")
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val bodyStr = response.body?.string() ?: ""
            val json = JSONObject(bodyStr)
            val runs = json.optJSONArray("workflow_runs")

            if (runs != null && runs.length() > 0) {
                val latestRun = runs.getJSONObject(0)
                val runId = latestRun.optString("id", "")
                val commitMsg = latestRun.optJSONObject("head_commit")?.optString("message", "") ?: "Build Nightly Terbaru"
                val updatedAt = latestRun.optString("updated_at", "")

                AppLogger.i("NightlyUpdate", "Tersedia Nightly Run #$runId: $commitMsg ($updatedAt)")
                Result.success(latestRun)
            } else {
                Result.failure(Exception("Tidak ditemukan workflow runs di GitHub"))
            }
        } catch (e: Exception) {
            AppLogger.e("NightlyUpdate", "Kendala saat memeriksa update via GitHub API", e)
            Result.failure(e)
        }
    }

    suspend fun downloadAndInstallNightly(context: Context) {
        withContext(Dispatchers.IO) {
            val updateDir = File(context.cacheDir, "nightly_update").apply { mkdirs() }
            val zipFile = File(updateDir, "nightly_artifact.zip")
            var extractedApk: File? = null

            try {
                // 1. Cek ketersediaan build Nightly terbaru dahulu
                _updateState.value = NightlyUpdateState.Checking
                val deviceAbi = getDeviceCpuAbi()
                AppLogger.i("NightlyUpdate", "Mulai alur pembaruan. Arsitektur HP: $deviceAbi")

                val checkResult = checkForNightlyUpdate()
                checkResult.onSuccess { runJson ->
                    val runId = runJson.optString("id", "")
                    val commitMsg = runJson.optJSONObject("head_commit")?.optString("message", "Nightly Build") ?: "Nightly Build"
                    val updatedAt = runJson.optString("updated_at", "")
                    AppLogger.i("NightlyUpdate", "Konfirmasi build aktif untuk diunduh: Run #$runId")
                }

                // 1.5. Periksa ketersediaan penyimpanan internal perangkat
                val estimatedSize = 30L * 1024 * 1024 // Estimasi 30 MB untuk ZIP
                if (!StorageUtil.hasEnoughStorageSpace(updateDir, estimatedSize)) {
                    val availableSpaceText = android.text.format.Formatter.formatFileSize(context, StorageUtil.getAvailableStorageBytes(updateDir))
                    val err = "Penyimpanan HP hampir penuh (Tersedia: $availableSpaceText). Harap kosongkan setidaknya 55 MB ruang penyimpanan internal untuk memasang update."
                    _updateState.value = NightlyUpdateState.Error(err)
                    AppLogger.w("NightlyUpdate", err)
                    return@withContext
                }

                // 2. Download ZIP Artifact
                _updateState.value = NightlyUpdateState.Downloading(0f, 0L, 0L)
                AppLogger.i("NightlyUpdate", "Mengunduh artefak ZIP dari: $NIGHTLY_URL")

                val response = UpdateHistoryLogger.runWithExponentialBackoff(
                    maxRetries = 3,
                    initialDelayMs = 1500L,
                    onRetry = { attempt, delayMs, ex ->
                        AppLogger.w("NightlyUpdate", "Koneksi terputus (Upaya $attempt/3). Mencoba kembali dalam ${delayMs}ms...: ${ex.localizedMessage}")
                    }
                ) {
                    val req = Request.Builder()
                        .url(NIGHTLY_URL)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) Truedown-Android-App")
                        .build()
                    val resp = client.newCall(req).execute()
                    if (!resp.isSuccessful) {
                        throw Exception("Gagal mengunduh artefak Nightly (HTTP ${resp.code})")
                    }
                    resp
                }

                val body = response.body
                if (body == null) {
                    val err = "Respon server kosong saat mengunduh artefak"
                    _updateState.value = NightlyUpdateState.Error(err)
                    UpdateHistoryLogger.logAttempt(context, "Nightly", "Nightly", "Failed", err)
                    return@withContext
                }

                val contentType = body.contentType()?.toString()?.lowercase() ?: ""
                if (contentType.contains("text/html")) {
                    val err = "Artefak Nightly di GitHub belum tersedia atau link telah expired. Silakan coba beberapa saat lagi."
                    _updateState.value = NightlyUpdateState.Error(err)
                    AppLogger.e("NightlyUpdate", "Response berupa HTML, bukan berkas ZIP: $contentType")
                    UpdateHistoryLogger.logAttempt(context, "Nightly", "Nightly", "Failed", err)
                    return@withContext
                }

                val contentLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(zipFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                val startTime = System.currentTimeMillis()
                var lastTime = startTime
                var lastBytesRead = 0L
                var currentSpeedBytesPerSec = 0L
                var remainingSecs = -1L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastTime
                    if (timeDelta >= 400) {
                        val bytesDelta = totalBytesRead - lastBytesRead
                        currentSpeedBytesPerSec = (bytesDelta * 1000L) / timeDelta
                        lastTime = now
                        lastBytesRead = totalBytesRead

                        if (currentSpeedBytesPerSec > 0 && contentLength > 0 && totalBytesRead < contentLength) {
                            val remainingBytes = contentLength - totalBytesRead
                            remainingSecs = remainingBytes / currentSpeedBytesPerSec
                        }
                    }

                    val progress = if (contentLength > 0) (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f) else 0f
                    _updateState.value = NightlyUpdateState.Downloading(
                        progress = progress,
                        downloadedBytes = totalBytesRead,
                        totalBytes = contentLength,
                        speedBytesPerSec = currentSpeedBytesPerSec,
                        remainingSeconds = remainingSecs
                    )
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                AppLogger.i("NightlyUpdate", "Unduhan ZIP selesai (${zipFile.length()} bytes). Validasi file...")

                // Validasi ukuran berkas ZIP terunduh untuk mencegah kesimpulan terlalu cepat
                if (zipFile.length() < 100_000L) {
                    val err = "Gagal mengunduh: Berkas artefak tidak lengkap (${zipFile.length()} bytes). Periksa koneksi internet Anda."
                    _updateState.value = NightlyUpdateState.Error(err)
                    AppLogger.e("NightlyUpdate", err)
                    return@withContext
                }

                // 3. Ekstrak ZIP & Pilih APK Berdasarkan Arsitektur Pintar (Smart ABI Selection)
                _updateState.value = NightlyUpdateState.Extracting("Mengekstrak dan memilih APK untuk arsitektur $deviceAbi...")

                data class ApkCandidate(
                    val entryName: String,
                    val file: File,
                    val score: Int
                )

                val apkCandidates = mutableListOf<ApkCandidate>()

                try {
                    val zipInputStream = ZipInputStream(zipFile.inputStream())
                    var entry: ZipEntry? = zipInputStream.nextEntry

                    while (entry != null) {
                        val entryName = entry.name
                        if (!entry.isDirectory && entryName.endsWith(".apk", ignoreCase = true)) {
                            val candidateFile = File(updateDir, "extracted_${apkCandidates.size}.apk")
                            val apkOut = FileOutputStream(candidateFile)

                            val apkBuffer = ByteArray(8192)
                            var apkBytesRead: Int
                            while (zipInputStream.read(apkBuffer).also { apkBytesRead = it } != -1) {
                                apkOut.write(apkBuffer, 0, apkBytesRead)
                            }
                            apkOut.flush()
                            apkOut.close()

                            val lowerName = entryName.lowercase()
                            val score = when {
                                lowerName.contains(deviceAbi.lowercase()) -> 100
                                lowerName.contains("arm64") && deviceAbi.contains("arm64") -> 95
                                lowerName.contains("v7a") && deviceAbi.contains("v7a") -> 95
                                lowerName.contains("universal") -> 80
                                else -> 50
                            }

                            AppLogger.i("NightlyUpdate", "Ditemukan APK: $entryName (Arsitektur Score: $score)")
                            apkCandidates.add(ApkCandidate(entryName, candidateFile, score))
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                    zipInputStream.close()

                } catch (e: ZipException) {
                    val err = "File ZIP artefak rusak atau terpotong saat pengunduhan. Silakan unduh ulang."
                    _updateState.value = NightlyUpdateState.Error(err)
                    AppLogger.e("NightlyUpdate", err, e)
                    return@withContext
                }

                // Hapus file ZIP setelah diekstrak untuk menghemat ruang
                if (zipFile.exists()) {
                    zipFile.delete()
                    AppLogger.i("NightlyUpdate", "File ZIP artefak berhasil dibersihkan setelah ekstraksi")
                }

                if (apkCandidates.isEmpty()) {
                    val err = "Artefak terunduh tidak berisi berkas .apk. Silakan coba lagi nanti."
                    _updateState.value = NightlyUpdateState.Error(err)
                    AppLogger.e("NightlyUpdate", err)
                    return@withContext
                }

                // Pilih APK kandidat dengan skor arsitektur tertinggi
                val bestCandidate = apkCandidates.maxByOrNull { it.score }!!
                val finalApkFile = File(updateDir, "truedown_update.apk")
                bestCandidate.file.renameTo(finalApkFile)

                // Bersihkan kandidat lain
                apkCandidates.forEach { candidate ->
                    if (candidate.file.exists()) candidate.file.delete()
                }

                extractedApk = finalApkFile
                AppLogger.i("NightlyUpdate", "Pilihan APK Optimal (${bestCandidate.entryName}) untuk $deviceAbi berhasil disiapkan: ${extractedApk.length()} bytes")
                _updateState.value = NightlyUpdateState.ReadyToInstall(extractedApk, deviceAbi)
                UpdateHistoryLogger.logAttempt(context, "Nightly Build", "Nightly", "Success")

                // 4. Jalankan Instalasi APK In-App
                withContext(Dispatchers.Main) {
                    installApk(context, extractedApk)
                }

            } catch (e: Exception) {
                AppLogger.e("NightlyUpdate", "Kendala saat proses unduh/ekstrak Nightly", e)
                val errMsg = e.localizedMessage ?: "Terjadi kendala tidak dikenal"
                _updateState.value = NightlyUpdateState.Error(errMsg)
                UpdateHistoryLogger.logAttempt(context, "Nightly Build", "Nightly", "Failed", errMsg)
            } finally {
                if (zipFile.exists()) {
                    zipFile.delete()
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!canInstallUnknownApps(context)) {
                openInstallPermissionSettings(context)
                return
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            AppLogger.i("NightlyUpdate", "Memulai intent instalasi APK: $apkUri")
            context.startActivity(intent)

        } catch (e: Exception) {
            AppLogger.e("NightlyUpdate", "Gagal memicu instalasi APK in-app", e)
            _updateState.value = NightlyUpdateState.Error("Gagal memasang APK: ${e.localizedMessage}")
        }
    }

    fun cleanupUpdateFiles(context: Context) {
        try {
            val updateDir = File(context.cacheDir, "nightly_update")
            if (updateDir.exists()) {
                updateDir.deleteRecursively()
                AppLogger.i("NightlyUpdate", "File pembaruan temporary berhasil dibersihkan sepenuhnya")
            }
        } catch (e: Exception) {
            AppLogger.e("NightlyUpdate", "Gagal membersihkan file update", e)
        }
    }
}
