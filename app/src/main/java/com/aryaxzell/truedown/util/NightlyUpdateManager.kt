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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed class NightlyUpdateState {
    object Idle : NightlyUpdateState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : NightlyUpdateState()
    data class Extracting(val status: String) : NightlyUpdateState()
    data class ReadyToInstall(val apkFile: File) : NightlyUpdateState()
    data class Error(val message: String) : NightlyUpdateState()
}

object NightlyUpdateManager {

    private const val NIGHTLY_URL = "https://nightly.link/AryaXzell/Truedown-Android/workflows/build/main?preview&h=c9122b50d061e55e3d2d601154766a71c9e9de40"

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

    suspend fun downloadAndInstallNightly(context: Context) {
        withContext(Dispatchers.IO) {
            val updateDir = File(context.cacheDir, "nightly_update").apply { mkdirs() }
            val zipFile = File(updateDir, "nightly_artifact.zip")
            var extractedApk: File? = null

            try {
                // 1. Download ZIP Artifact
                _updateState.value = NightlyUpdateState.Downloading(0f, 0L, 0L)
                AppLogger.i("NightlyUpdate", "Mulai mengunduh artefak Nightly dari: $NIGHTLY_URL")

                val request = Request.Builder()
                    .url(NIGHTLY_URL)
                    .header("User-Agent", "Truedown-Android-App")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val err = "Gagal mengunduh artefak Nightly (HTTP ${response.code})"
                    _updateState.value = NightlyUpdateState.Error(err)
                    AppLogger.e("NightlyUpdate", err)
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    val err = "Response body kosong saat mengunduh artefak"
                    _updateState.value = NightlyUpdateState.Error(err)
                    return@withContext
                }

                val contentLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(zipFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    val progress = if (contentLength > 0) (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f) else 0f
                    _updateState.value = NightlyUpdateState.Downloading(progress, totalBytesRead, contentLength)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                AppLogger.i("NightlyUpdate", "Unduhan ZIP selesai (${zipFile.length()} bytes). Mulai mengekstrak...")

                // 2. Ekstrak ZIP & Cari File APK
                _updateState.value = NightlyUpdateState.Extracting("Mengekstrak file APK dari artefak ZIP...")

                val zipInputStream = ZipInputStream(zipFile.inputStream())
                var entry: ZipEntry? = zipInputStream.nextEntry
                var targetApkFile: File? = null

                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory && entryName.endsWith(".apk", ignoreCase = true)) {
                        AppLogger.i("NightlyUpdate", "Ditemukan file APK di dalam ZIP: $entryName")
                        val apkName = "truedown_update.apk"
                        val outputFile = File(updateDir, apkName)
                        val apkOut = FileOutputStream(outputFile)

                        val apkBuffer = ByteArray(8192)
                        var apkBytesRead: Int
                        while (zipInputStream.read(apkBuffer).also { apkBytesRead = it } != -1) {
                            apkOut.write(apkBuffer, 0, apkBytesRead)
                        }
                        apkOut.flush()
                        apkOut.close()

                        targetApkFile = outputFile
                        break
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
                zipInputStream.close()

                // Hapus file ZIP setelah diekstrak untuk menghemat ruang
                if (zipFile.exists()) {
                    zipFile.delete()
                    AppLogger.i("NightlyUpdate", "File ZIP artefak berhasil dihapus setelah ekstraksi")
                }

                if (targetApkFile == null || !targetApkFile.exists()) {
                    val err = "Tidak ditemukan file .apk di dalam artefak ZIP Nightly"
                    _updateState.value = NightlyUpdateState.Error(err)
                    AppLogger.e("NightlyUpdate", err)
                    return@withContext
                }

                extractedApk = targetApkFile
                AppLogger.i("NightlyUpdate", "Ekstraksi APK sukses: ${extractedApk.absolutePath} (${extractedApk.length()} bytes)")
                _updateState.value = NightlyUpdateState.ReadyToInstall(extractedApk)

                // 3. Jalankan Instalasi APK In-App
                withContext(Dispatchers.Main) {
                    installApk(context, extractedApk)
                }

            } catch (e: Exception) {
                AppLogger.e("NightlyUpdate", "Kendala saat proses unduh/ekstrak Nightly", e)
                _updateState.value = NightlyUpdateState.Error(e.localizedMessage ?: "Terjadi kendala tidak dikenal")
            } finally {
                // Bersihkan file zip jika masih ada
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
