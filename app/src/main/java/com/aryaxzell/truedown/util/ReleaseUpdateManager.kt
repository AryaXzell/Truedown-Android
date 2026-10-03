package com.aryaxzell.truedown.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.aryaxzell.truedown.BuildConfig
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

sealed class ReleaseUpdateState {
    object Idle : ReleaseUpdateState()
    object Checking : ReleaseUpdateState()
    data class UpToDate(
        val currentVersion: String,
        val latestTag: String
    ) : ReleaseUpdateState()
    data class UpdateAvailable(
        val tag: String,
        val title: String,
        val releaseNotes: String,
        val apkUrl: String,
        val apkSizeBytes: Long,
        val selectedAbi: String,
        val sha256Url: String = ""
    ) : ReleaseUpdateState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L,
        val remainingSeconds: Long = -1L
    ) : ReleaseUpdateState()
    data class Verifying(val status: String) : ReleaseUpdateState()
    data class ReadyToInstall(
        val apkFile: File,
        val selectedAbi: String = ""
    ) : ReleaseUpdateState()
    data class Error(val message: String) : ReleaseUpdateState()
}

object ReleaseUpdateManager {

    private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/AryaXzell/Truedown-Android/releases/latest"

    private val _updateState = MutableStateFlow<ReleaseUpdateState>(ReleaseUpdateState.Idle)
    val updateState: StateFlow<ReleaseUpdateState> = _updateState.asStateFlow()

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
                AppLogger.e("ReleaseUpdate", "Gagal membuka pengaturan install unknown apps", e)
            }
        }
    }

    fun resetState() {
        _updateState.value = ReleaseUpdateState.Idle
    }

    /**
     * Memeriksa rilis stabil terbaru dari GitHub Release API.
     */
    suspend fun checkForReleaseUpdate(): Result<ReleaseUpdateState.UpdateAvailable> = withContext(Dispatchers.IO) {
        _updateState.value = ReleaseUpdateState.Checking
        AppLogger.i("ReleaseUpdate", "Memeriksa rilis stabil terbaru dari GitHub Releases...")

        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .header("User-Agent", "Truedown-Android-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Gagal memeriksa rilis terbaru (HTTP ${response.code})"
                _updateState.value = ReleaseUpdateState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val bodyStr = response.body?.string() ?: ""
            val json = JSONObject(bodyStr)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", "Truedown Android $tagName")
            val releaseNotes = json.optString("body", "Pembaruan rilis stabil terbaru.")

            val currentVersion = BuildConfig.VERSION_NAME
            val cleanCurrent = currentVersion.replace("v", "").trim()
            val cleanLatest = tagName.replace("v", "").trim()

            // Deteksi arsitektur pintar HP pengguna
            val deviceAbi = NightlyUpdateManager.getDeviceCpuAbi()
            val assets = json.optJSONArray("assets")

            var bestApkName = ""
            var bestApkUrl = ""
            var bestApkSize = 0L
            var bestScore = -1

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    val size = asset.optLong("size", 0L)

                    if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                        val score = when {
                            assetName.lowercase().contains(deviceAbi.lowercase()) -> 100
                            assetName.lowercase().contains("arm64") && deviceAbi.contains("arm64") -> 95
                            assetName.lowercase().contains("v7a") && deviceAbi.contains("v7a") -> 95
                            assetName.lowercase().contains("universal") -> 80
                            else -> 50
                        }

                        if (score > bestScore) {
                            bestScore = score
                            bestApkName = assetName
                            bestApkUrl = downloadUrl
                            bestApkSize = size
                        }
                    }
                }
            }

            if (bestApkUrl.isBlank()) {
                val err = "Tidak ditemukan file APK rilis untuk arsitektur $deviceAbi di GitHub Release $tagName."
                _updateState.value = ReleaseUpdateState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            var bestSha256Url = ""
            if (bestApkName.isNotBlank() && assets != null) {
                val targetShaName = "$bestApkName.sha256"
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.equals(targetShaName, ignoreCase = true)) {
                        bestSha256Url = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val updateAvailable = ReleaseUpdateState.UpdateAvailable(
                tag = tagName,
                title = releaseTitle,
                releaseNotes = releaseNotes,
                apkUrl = bestApkUrl,
                apkSizeBytes = bestApkSize,
                selectedAbi = deviceAbi,
                sha256Url = bestSha256Url
            )

            if (isVersionNewer(cleanLatest, cleanCurrent)) {
                _updateState.value = updateAvailable
                AppLogger.i("ReleaseUpdate", "Pembaruan rilis stabil ditemukan: $tagName (Saat ini: $currentVersion)")
            } else {
                _updateState.value = ReleaseUpdateState.UpToDate(currentVersion, tagName)
                AppLogger.i("ReleaseUpdate", "Aplikasi sudah dalam versi terbaru: $currentVersion")
            }

            Result.success(updateAvailable)
        } catch (e: Exception) {
            AppLogger.e("ReleaseUpdate", "Kendala saat memeriksa rilis stabil dari GitHub API", e)
            val err = e.localizedMessage ?: "Gagal terhubung ke GitHub Release API"
            _updateState.value = ReleaseUpdateState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Mengunduh file APK rilis resmi secara langsung in-app dan memicu instalasi paket.
     */
    suspend fun downloadAndInstallRelease(context: Context, updateInfo: ReleaseUpdateState.UpdateAvailable) {
        withContext(Dispatchers.IO) {
            val updateDir = File(context.cacheDir, "release_update").apply { mkdirs() }
            val apkFile = File(updateDir, "truedown_release_${updateInfo.tag}_${updateInfo.selectedAbi}.apk")

            try {
                // Periksa ketersediaan penyimpanan internal perangkat
                if (!StorageUtil.hasEnoughStorageSpace(updateDir, updateInfo.apkSizeBytes)) {
                    val availableSpaceText = android.text.format.Formatter.formatFileSize(context, StorageUtil.getAvailableStorageBytes(updateDir))
                    val requiredSpaceText = android.text.format.Formatter.formatFileSize(context, updateInfo.apkSizeBytes + 25 * 1024 * 1024)
                    val err = "Penyimpanan HP hampir penuh (Tersedia: $availableSpaceText). Harap kosongkan setidaknya $requiredSpaceText ruang penyimpanan internal untuk memasang update."
                    _updateState.value = ReleaseUpdateState.Error(err)
                    AppLogger.w("ReleaseUpdate", err)
                    return@withContext
                }

                _updateState.value = ReleaseUpdateState.Downloading(0f, 0L, updateInfo.apkSizeBytes)
                AppLogger.i("ReleaseUpdate", "Mengunduh langsung APK Rilis ${updateInfo.tag} (${updateInfo.selectedAbi}) dari: ${updateInfo.apkUrl}")

                val request = Request.Builder()
                    .url(updateInfo.apkUrl)
                    .header("User-Agent", "Truedown-Android-App")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val err = "Gagal mengunduh APK Rilis (HTTP ${response.code})"
                    _updateState.value = ReleaseUpdateState.Error(err)
                    AppLogger.e("ReleaseUpdate", err)
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    val err = "Response body kosong saat mengunduh APK"
                    _updateState.value = ReleaseUpdateState.Error(err)
                    return@withContext
                }

                val contentLength = if (body.contentLength() > 0) body.contentLength() else updateInfo.apkSizeBytes
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(apkFile)

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
                    _updateState.value = ReleaseUpdateState.Downloading(
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

                AppLogger.i("ReleaseUpdate", "Pengunduhan APK Rilis selesai (${apkFile.length()} bytes)")

                if (apkFile.length() < 1_000_000L) {
                    val err = "File APK terunduh tidak lengkap (${apkFile.length()} bytes). Silakan coba lagi."
                    _updateState.value = ReleaseUpdateState.Error(err)
                    AppLogger.e("ReleaseUpdate", err)
                    return@withContext
                }

                // 3. Verifikasi SHA-256 jika URL tersedia
                if (updateInfo.sha256Url.isNotBlank()) {
                    _updateState.value = ReleaseUpdateState.Verifying("Mengunduh checksum SHA-256 resmi...")
                    AppLogger.i("ReleaseUpdate", "Mengunduh file checksum dari: ${updateInfo.sha256Url}")

                    val shaRequest = Request.Builder()
                        .url(updateInfo.sha256Url)
                        .header("User-Agent", "Truedown-Android-App")
                        .build()

                    val shaResponse = client.newCall(shaRequest).execute()
                    if (shaResponse.isSuccessful) {
                        val shaBody = shaResponse.body?.string() ?: ""
                        // Ekstrak 64 hex karakter pertama dari teks
                        val matchResult = Regex("[a-fA-F0-9]{64}").find(shaBody)
                        val expectedSha = matchResult?.value?.lowercase()

                        if (expectedSha != null) {
                            _updateState.value = ReleaseUpdateState.Verifying("Menghitung checksum berkas APK...")
                            val actualSha = calculateSha256(apkFile).lowercase()

                            AppLogger.i("ReleaseUpdate", "Expected SHA-256: $expectedSha")
                            AppLogger.i("ReleaseUpdate", "Actual SHA-256: $actualSha")

                            if (expectedSha != actualSha) {
                                val err = "Integritas berkas terganggu (SHA-256 Mismatch). Silakan coba lagi."
                                _updateState.value = ReleaseUpdateState.Error(err)
                                AppLogger.e("ReleaseUpdate", err)
                                if (apkFile.exists()) {
                                    apkFile.delete()
                                }
                                return@withContext
                            }
                            AppLogger.i("ReleaseUpdate", "Verifikasi SHA-256 berhasil!")
                        } else {
                            AppLogger.w("ReleaseUpdate", "Format file .sha256 di GitHub tidak dikenali atau kosong")
                        }
                    } else {
                        AppLogger.w("ReleaseUpdate", "Gagal mengunduh berkas checksum (HTTP ${shaResponse.code}). Melompati verifikasi.")
                    }
                }

                _updateState.value = ReleaseUpdateState.ReadyToInstall(apkFile, updateInfo.selectedAbi)

                withContext(Dispatchers.Main) {
                    installApk(context, apkFile)
                }

            } catch (e: Exception) {
                AppLogger.e("ReleaseUpdate", "Kendala saat mengunduh rilis stabil", e)
                _updateState.value = ReleaseUpdateState.Error(e.localizedMessage ?: "Gagal mengunduh berkas rilis")
            }
        }
    }

    fun calculateSha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val sb = StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
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

            AppLogger.i("ReleaseUpdate", "Memulai intent instalasi APK Rilis: $apkUri")
            context.startActivity(intent)

        } catch (e: Exception) {
            AppLogger.e("ReleaseUpdate", "Gagal memicu instalasi APK in-app", e)
            _updateState.value = ReleaseUpdateState.Error("Gagal memasang APK: ${e.localizedMessage}")
        }
    }

    fun cleanupUpdateFiles(context: Context) {
        try {
            val updateDir = File(context.cacheDir, "release_update")
            if (updateDir.exists()) {
                updateDir.deleteRecursively()
                AppLogger.i("ReleaseUpdate", "File pembaruan release temporary berhasil dibersihkan")
            }
        } catch (e: Exception) {
            AppLogger.e("ReleaseUpdate", "Gagal membersihkan file release update", e)
        }
    }

    private fun isVersionNewer(latestStr: String, currentStr: String): Boolean {
        if (latestStr == currentStr) return false
        try {
            val latestParts = latestStr.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = currentStr.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {}
        return latestStr != currentStr
    }
}
