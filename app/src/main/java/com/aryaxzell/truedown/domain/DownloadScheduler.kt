package com.aryaxzell.truedown.domain

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aryaxzell.truedown.data.local.MediaItemEntity
import com.aryaxzell.truedown.data.local.PostEntity
import com.aryaxzell.truedown.data.local.TruedownDatabase
import com.aryaxzell.truedown.data.preferences.UserPreferencesRepository
import com.aryaxzell.truedown.data.storage.MediaStoreDownloader
import com.aryaxzell.truedown.domain.model.MediaKind
import com.aryaxzell.truedown.domain.model.MediaStatus
import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.domain.model.ResolvedPost
import com.aryaxzell.truedown.work.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DownloadScheduler(private val context: Context) {
    private val db = TruedownDatabase.getInstance(context)
    private val workManager = WorkManager.getInstance(context)
    private val prefsRepo = UserPreferencesRepository(context)

    suspend fun scheduleDownload(
        post: ResolvedPost,
        kind: MediaKind,
        photoIndices: List<Int> = emptyList(),
        explicitQuality: String? = null
    ): Result<List<Long>> = withContext(Dispatchers.IO) {
        try {
            val prefs = prefsRepo.userPreferencesFlow.first()
            val chosenQuality = explicitQuality ?: prefs.defaultQuality
            val isWifiOnly = prefs.wifiOnly

            val postEntity = PostEntity(
                id = post.id,
                type = post.type.name,
                title = post.title,
                authorName = post.authorName,
                authorHandle = post.authorHandle,
                sourceUrl = post.sourceUrl,
                hasAudio = !post.audioUrl.isNullOrBlank(),
                photoCount = post.photoUrls.size
            )
            val insertedRowId = db.postDao().insertPostIgnore(postEntity)
            if (insertedRowId == -1L) {
                db.postDao().updatePostMetadata(
                    id = postEntity.id, type = postEntity.type, title = postEntity.title,
                    authorName = postEntity.authorName, authorHandle = postEntity.authorHandle,
                    sourceUrl = postEntity.sourceUrl, hasAudio = postEntity.hasAudio, photoCount = postEntity.photoCount
                )
            }

            val scheduledIds = mutableListOf<Long>()

            when (kind) {
                MediaKind.VIDEO -> {
                    val mediaUrl = if (chosenQuality == "HD" && !post.videoHdUrl.isNullOrBlank()) {
                        post.videoHdUrl
                    } else {
                        post.videoStandardUrl ?: post.videoHdUrl
                    } ?: return@withContext Result.failure(Exception("URL video tidak tersedia"))

                    val fileName = "truedown_${post.id}_video.mp4"
                    val existing = db.mediaItemDao().findMediaItem(post.id, MediaKind.VIDEO.name, 0)

                    val mediaItemId = if (existing != null) {
                        if (existing.status == MediaStatus.DONE.name && prefs.duplicateRule == "SKIP") {
                            return@withContext Result.success(listOf(existing.id))
                        }
                        existing.id
                    } else {
                        db.mediaItemDao().insertMediaItem(
                            MediaItemEntity(
                                postId = post.id,
                                kind = MediaKind.VIDEO.name,
                                itemIndex = 0,
                                fileName = fileName,
                                status = MediaStatus.PENDING.name,
                                quality = chosenQuality
                            )
                        )
                    }

                    scheduledIds.add(mediaItemId)
                    enqueueWorker(
                        mediaItemId = mediaItemId,
                        post = post,
                        mediaUrl = mediaUrl,
                        kind = MediaKind.VIDEO,
                        index = 0,
                        quality = chosenQuality,
                        isWifiOnly = isWifiOnly
                    )
                }

                MediaKind.AUDIO -> {
                    val mediaUrl = post.audioUrl ?: return@withContext Result.failure(Exception("Audio tidak tersedia"))
                    val fileName = "truedown_${post.id}_audio.mp3"
                    val existing = db.mediaItemDao().findMediaItem(post.id, MediaKind.AUDIO.name, 0)

                    val mediaItemId = if (existing != null) {
                        if (existing.status == MediaStatus.DONE.name && prefs.duplicateRule == "SKIP") {
                            return@withContext Result.success(listOf(existing.id))
                        }
                        existing.id
                    } else {
                        db.mediaItemDao().insertMediaItem(
                            MediaItemEntity(
                                postId = post.id,
                                kind = MediaKind.AUDIO.name,
                                itemIndex = 0,
                                fileName = fileName,
                                status = MediaStatus.PENDING.name,
                                quality = "STANDARD"
                            )
                        )
                    }

                    scheduledIds.add(mediaItemId)
                    enqueueWorker(
                        mediaItemId = mediaItemId,
                        post = post,
                        mediaUrl = mediaUrl,
                        kind = MediaKind.AUDIO,
                        index = 0,
                        quality = "STANDARD",
                        isWifiOnly = isWifiOnly
                    )
                }

                MediaKind.PHOTO -> {
                    val indicesToDownload = if (photoIndices.isEmpty()) {
                        post.photoUrls.indices.toList()
                    } else {
                        photoIndices
                    }

                    for (idx in indicesToDownload) {
                        val photoUrl = post.photoUrls.getOrNull(idx) ?: continue
                        val fileName = "truedown_${post.id}_photo_${idx + 1}.jpg"
                        val existing = db.mediaItemDao().findMediaItem(post.id, MediaKind.PHOTO.name, idx)

                        val mediaItemId = if (existing != null) {
                            if (existing.status == MediaStatus.DONE.name && prefs.duplicateRule == "SKIP") {
                                scheduledIds.add(existing.id)
                                continue
                            }
                            existing.id
                        } else {
                            db.mediaItemDao().insertMediaItem(
                                MediaItemEntity(
                                    postId = post.id,
                                    kind = MediaKind.PHOTO.name,
                                    itemIndex = idx,
                                    fileName = fileName,
                                    status = MediaStatus.PENDING.name,
                                    quality = "STANDARD"
                                )
                            )
                        }

                        scheduledIds.add(mediaItemId)
                        enqueueWorker(
                            mediaItemId = mediaItemId,
                            post = post,
                            mediaUrl = photoUrl,
                            kind = MediaKind.PHOTO,
                            index = idx,
                            quality = "STANDARD",
                            isWifiOnly = isWifiOnly
                        )
                    }
                }
            }

            Result.success(scheduledIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun enqueueWorker(
        mediaItemId: Long,
        post: ResolvedPost,
        mediaUrl: String,
        kind: MediaKind,
        index: Int,
        quality: String,
        isWifiOnly: Boolean
    ) {
        val inputData = Data.Builder()
            .putLong(DownloadWorker.KEY_MEDIA_ITEM_ID, mediaItemId)
            .putString(DownloadWorker.KEY_POST_ID, post.id)
            .putString(DownloadWorker.KEY_MEDIA_URL, mediaUrl)
            .putString(DownloadWorker.KEY_KIND, kind.name)
            .putInt(DownloadWorker.KEY_INDEX, index)
            .putString(DownloadWorker.KEY_TITLE, post.title)
            .putString(DownloadWorker.KEY_QUALITY, quality)
            .putInt(DownloadWorker.KEY_TOTAL_PHOTOS, post.photoUrls.size)
            .putBoolean(DownloadWorker.KEY_HAS_AUDIO, !post.audioUrl.isNullOrBlank())
            .putString(DownloadWorker.KEY_SOURCE_URL, post.sourceUrl)
            .build()

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(
                if (isWifiOnly) androidx.work.NetworkType.UNMETERED
                else androidx.work.NetworkType.CONNECTED
            )
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("post_${post.id}")
            .addTag("download")
            .build()

        val uniqueWorkName = "download_${post.id}_${kind.name}_$index"
        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.KEEP, workRequest)
    }

    fun cancelDownload(postId: String) {
        workManager.cancelAllWorkByTag("post_$postId")
        com.aryaxzell.truedown.domain.DownloadProgressTracker.clear(postId)
    }

    suspend fun deletePost(postId: String, deleteFromGallery: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (deleteFromGallery) {
                val downloader = MediaStoreDownloader(context)
                val items = db.mediaItemDao().getMediaItemsByPost(postId).first()
                for (item in items) {
                    if (item.mediaStoreUri.isNotBlank()) {
                        downloader.deleteFromMediaStore(item.mediaStoreUri)
                    }
                }
            }
            db.postDao().deletePostById(postId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAllHistory(deleteFromGallery: Boolean) = withContext(Dispatchers.IO) {
        if (deleteFromGallery) {
            val downloader = MediaStoreDownloader(context)
            val posts = db.postDao().getAllPostsWithMedia().first()
            for (pwm in posts) {
                for (item in pwm.mediaItems) {
                    if (item.mediaStoreUri.isNotBlank()) {
                        downloader.deleteFromMediaStore(item.mediaStoreUri)
                    }
                }
            }
        }
        db.mediaItemDao().deleteAllMediaItems()
        db.postDao().deleteAllPosts()
    }

    suspend fun reconcileOrphanDownloads() = withContext(Dispatchers.IO) {
        db.mediaItemDao().reconcileOrphanStatus(MediaStatus.DOWNLOADING.name, MediaStatus.FAILED.name)
    }
}
