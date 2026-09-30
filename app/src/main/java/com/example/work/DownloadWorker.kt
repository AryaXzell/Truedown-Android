package com.example.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.TruedownDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.storage.MediaStoreDownloader
import com.example.domain.model.MediaKind
import com.example.domain.model.MediaStatus
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.first

class DownloadWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_MEDIA_ITEM_ID = "key_media_item_id"
        const val KEY_POST_ID = "key_post_id"
        const val KEY_MEDIA_URL = "key_media_url"
        const val KEY_KIND = "key_kind"
        const val KEY_INDEX = "key_index"
        const val KEY_TITLE = "key_title"
        const val KEY_QUALITY = "key_quality"
        const val KEY_TOTAL_PHOTOS = "key_total_photos"
        const val KEY_HAS_AUDIO = "key_has_audio"
        const val KEY_SOURCE_URL = "key_source_url"
    }

    override suspend fun doWork(): Result {
        val mediaItemId = inputData.getLong(KEY_MEDIA_ITEM_ID, -1L)
        val postId = inputData.getString(KEY_POST_ID) ?: return Result.failure()
        val mediaUrl = inputData.getString(KEY_MEDIA_URL) ?: return Result.failure()
        val kindStr = inputData.getString(KEY_KIND) ?: MediaKind.VIDEO.name
        val index = inputData.getInt(KEY_INDEX, 0)
        val title = inputData.getString(KEY_TITLE) ?: "TikTok $postId"
        val quality = inputData.getString(KEY_QUALITY) ?: "STANDARD"
        val totalPhotos = inputData.getInt(KEY_TOTAL_PHOTOS, 1)
        val hasAudio = inputData.getBoolean(KEY_HAS_AUDIO, false)
        val sourceUrl = inputData.getString(KEY_SOURCE_URL) ?: ""

        val kind = try { MediaKind.valueOf(kindStr) } catch (_: Exception) { MediaKind.VIDEO }
        val db = TruedownDatabase.getInstance(appContext)
        val downloader = MediaStoreDownloader(appContext)
        val prefsRepo = UserPreferencesRepository(appContext)

        val notifId = postId.hashCode() + index

        val cancelPendingIntent = androidx.work.WorkManager.getInstance(appContext)
            .createCancelPendingIntent(id)

        val progressBuilder = NotificationHelper.buildProgressNotification(
            context = appContext,
            title = title,
            progress = 0,
            max = 100,
            cancelIntent = cancelPendingIntent
        )
        val foregroundInfo = ForegroundInfo(notifId, progressBuilder.build())
        setForeground(foregroundInfo)

        db.mediaItemDao().updateStatusFailed(mediaItemId, MediaStatus.DOWNLOADING.name, null)

        val result = downloader.downloadMedia(
            url = mediaUrl,
            postId = postId,
            kind = kind,
            index = index,
            onProgress = { bytesRead, totalBytes ->
                val progress = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else 0
                val updatedNotif = NotificationHelper.buildProgressNotification(
                    context = appContext,
                    title = title,
                    progress = progress,
                    max = 100,
                    cancelIntent = cancelPendingIntent
                )
                try {
                    androidx.core.app.NotificationManagerCompat.from(appContext).notify(notifId, updatedNotif.build())
                } catch (_: SecurityException) {}
            }
        )

        return if (result.isSuccess) {
            val downloadResult = result.getOrThrow()
            db.mediaItemDao().updateStatusSuccess(
                id = mediaItemId,
                status = MediaStatus.DONE.name,
                completedAt = System.currentTimeMillis(),
                sizeBytes = downloadResult.sizeBytes,
                uri = downloadResult.uri.toString()
            )

            val prefs = prefsRepo.userPreferencesFlow.first()
            val mimeType = when (kind) {
                MediaKind.VIDEO -> "video/mp4"
                MediaKind.PHOTO -> "image/jpeg"
                MediaKind.AUDIO -> "audio/mpeg"
            }

            NotificationHelper.showSuccessNotification(
                context = appContext,
                notificationId = notifId,
                postId = postId,
                title = title,
                mediaUri = downloadResult.uri.toString(),
                mimeType = mimeType,
                hasAudioOption = hasAudio && kind != MediaKind.AUDIO,
                showActions = prefs.showNotificationActions
            )

            Result.success(workDataOf("uri" to downloadResult.uri.toString()))
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Gagal mengunduh"
            db.mediaItemDao().updateStatusFailed(
                id = mediaItemId,
                status = MediaStatus.FAILED.name,
                errorReason = err
            )

            NotificationHelper.showErrorNotification(
                context = appContext,
                notificationId = notifId,
                postId = postId,
                title = title,
                errorMessage = err,
                sourceUrl = sourceUrl
            )

            Result.failure(workDataOf("error" to err))
        }
    }
}
