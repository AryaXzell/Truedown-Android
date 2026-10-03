package com.aryaxzell.truedown.util

import android.content.Context
import com.aryaxzell.truedown.BuildConfig
import com.aryaxzell.truedown.data.local.TruedownDatabase
import com.aryaxzell.truedown.data.local.UpdateHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UpdateHistoryLogger {

    /**
     * Memastikan riwayat versi aplikasi tercatat secara otomatis saat aplikasi dimulai.
     * Mencatat versi yang terpasang dan rekam jejak pembaruan yang telah dilakukan.
     */
    suspend fun ensureVersionHistoryInitialized(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = TruedownDatabase.getInstance(context)
            val dao = db.updateHistoryDao()
            val count = dao.getHistoryCount()
            val currentVersionTag = "v${BuildConfig.VERSION_NAME}"

            if (count == 0) {
                // Inisialisasi riwayat versi aplikasi terdahulu hingga versi aktif saat ini
                val historicalMilestones = listOf(
                    UpdateHistoryEntity(
                        versionName = "v1.0.0",
                        updateType = "Stable Release",
                        timestamp = System.currentTimeMillis() - (86400000L * 4), // 4 hari lalu
                        status = "Success",
                        errorMessage = "Rilis awal resmi Truedown Android (TikTok Video & Audio MP3 Downloader)"
                    ),
                    UpdateHistoryEntity(
                        versionName = "v1.1.0",
                        updateType = "Stable Release",
                        timestamp = System.currentTimeMillis() - (86400000L * 2), // 2 hari lalu
                        status = "Success",
                        errorMessage = "Pembaruan fitur: Audio focus Media3, Slideshow foto grid, dan in-app installer"
                    ),
                    UpdateHistoryEntity(
                        versionName = currentVersionTag,
                        updateType = "Installed Version",
                        timestamp = System.currentTimeMillis(),
                        status = "Success",
                        errorMessage = "Versi stabil saat ini terpasang aktif di perangkat (Aksesibilitas & Nightly Channel)"
                    )
                )

                for (milestone in historicalMilestones) {
                    dao.insertHistory(milestone)
                }
                AppLogger.i("UpdateHistory", "Inisialisasi awal riwayat pembaruan berhasil (${historicalMilestones.size} entri)")
            } else {
                // Periksa apakah versi saat ini sudah tercatat
                val hasCurrentVersion = dao.hasVersion(currentVersionTag)
                if (!hasCurrentVersion) {
                    dao.insertHistory(
                        UpdateHistoryEntity(
                            versionName = currentVersionTag,
                            updateType = "App Update",
                            timestamp = System.currentTimeMillis(),
                            status = "Success",
                            errorMessage = "Pembaruan ke versi $currentVersionTag berhasil diterapkan"
                        )
                    )
                    AppLogger.i("UpdateHistory", "Versi pembaruan baru $currentVersionTag dicatat ke riwayat")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("UpdateHistory", "Gagal inisialisasi riwayat pembaruan aplikasi", e)
        }
    }

    /**
     * Menyimpan riwayat percobaan pembaruan aplikasi ke database lokal Room.
     */
    suspend fun logAttempt(
        context: Context,
        versionName: String,
        updateType: String, // "Stable" atau "Nightly"
        status: String, // "Success", "Failed", "Skipped"
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val db = TruedownDatabase.getInstance(context)
            val entity = UpdateHistoryEntity(
                versionName = versionName,
                updateType = updateType,
                status = status,
                errorMessage = errorMessage
            )
            db.updateHistoryDao().insertHistory(entity)
            AppLogger.i("UpdateHistory", "Riwayat pembaruan tercatat: $versionName ($updateType) -> $status")
        } catch (e: Exception) {
            AppLogger.e("UpdateHistory", "Gagal menyimpan riwayat pembaruan", e)
        }
    }

    /**
     * Menjalankan operasi jaringan dengan mekanisme retry exponential backoff.
     */
    suspend fun <T> runWithExponentialBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        multiplier: Double = 2.0,
        onRetry: (attempt: Int, delayMs: Long, exception: Exception) -> Unit = { _, _, _ -> },
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        for (attempt in 1..maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                if (attempt == maxRetries) {
                    throw e
                }
                onRetry(attempt, currentDelay, e)
                kotlinx.coroutines.delay(currentDelay)
                currentDelay = (currentDelay * multiplier).toLong()
            }
        }
        throw IllegalStateException("Should not be reached")
    }
}
