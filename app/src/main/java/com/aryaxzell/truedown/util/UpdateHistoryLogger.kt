package com.aryaxzell.truedown.util

import android.content.Context
import com.aryaxzell.truedown.data.local.TruedownDatabase
import com.aryaxzell.truedown.data.local.UpdateHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UpdateHistoryLogger {

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
