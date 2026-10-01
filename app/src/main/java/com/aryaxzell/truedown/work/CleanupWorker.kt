package com.aryaxzell.truedown.work

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Worker that automatically clears cached images and temporary files
 * from cache and download temp directories older than 30 days.
 */
class CleanupWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "CleanupWorker"
        const val UNIQUE_WORK_NAME = "cache_temp_cleanup_work"
        const val MAX_CACHE_AGE_DAYS = 30L
        const val KEY_DELETED_COUNT = "deleted_count"
        const val KEY_FREED_BYTES = "freed_bytes"

        /**
         * Schedules the cleanup worker to run periodically (e.g., once every 24 hours)
         * when the device is not low on battery.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_CACHE_AGE_DAYS)
            var totalDeleted = 0
            var totalFreedBytes = 0L

            // Target directories for cached images and temporary files
            val targetDirs = listOfNotNull(
                appContext.cacheDir,
                appContext.externalCacheDir,
                appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                File(appContext.filesDir, "temp"),
                File(appContext.cacheDir, "image_cache"),
                File(appContext.cacheDir, "downloads")
            )

            for (dir in targetDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val (deleted, bytes) = cleanDirectory(dir, cutoffMillis)
                    totalDeleted += deleted
                    totalFreedBytes += bytes
                }
            }

            Log.d(TAG, "Cleanup completed: removed $totalDeleted old files, freed $totalFreedBytes bytes")

            Result.success(
                workDataOf(
                    KEY_DELETED_COUNT to totalDeleted,
                    KEY_FREED_BYTES to totalFreedBytes
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning cache files", e)
            Result.failure()
        }
    }

    private fun cleanDirectory(directory: File, cutoffMillis: Long): Pair<Int, Long> {
        var count = 0
        var freed = 0L

        val files = directory.listFiles() ?: return Pair(0, 0L)
        for (file in files) {
            if (file.isDirectory) {
                val (subCount, subFreed) = cleanDirectory(file, cutoffMillis)
                count += subCount
                freed += subFreed

                // Delete empty directory if it's not a top-level cache folder
                if (file.listFiles()?.isEmpty() == true && file != appContext.cacheDir && file != appContext.externalCacheDir) {
                    file.delete()
                }
            } else {
                if (file.lastModified() < cutoffMillis) {
                    val size = file.length()
                    if (file.delete()) {
                        count++
                        freed += size
                    }
                }
            }
        }
        return Pair(count, freed)
    }
}
