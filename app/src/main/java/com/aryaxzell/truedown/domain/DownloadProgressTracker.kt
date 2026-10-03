package com.aryaxzell.truedown.domain

import com.aryaxzell.truedown.domain.model.DownloadProgress
import com.aryaxzell.truedown.domain.model.MediaKind
import com.aryaxzell.truedown.domain.model.MediaStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton tracker to monitor download status and progress percentage
 * using Kotlin StateFlow.
 */
object DownloadProgressTracker {

    private val _downloadProgressMap = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, DownloadProgress>> = _downloadProgressMap.asStateFlow()

    // Map mediaItemId to postId for easy lookup if only mediaItemId is provided
    private val itemIdToPostId = ConcurrentHashMap<Long, String>()

    fun start(
        postId: String,
        mediaItemId: Long = -1L,
        title: String = "",
        kind: MediaKind = MediaKind.VIDEO,
        index: Int = 0
    ) {
        if (postId.isBlank()) return
        if (mediaItemId > 0) {
            itemIdToPostId[mediaItemId] = postId
        }
        val initialProgress = DownloadProgress(
            mediaItemId = mediaItemId,
            postId = postId,
            title = title,
            kind = kind,
            index = index,
            progressPercent = 0,
            status = MediaStatus.DOWNLOADING
        )
        _downloadProgressMap.update { current ->
            current + (postId to initialProgress)
        }
    }

    fun update(
        postId: String,
        mediaItemId: Long = -1L,
        progress: Int,
        bytesDownloaded: Long = 0L,
        totalBytes: Long = 0L
    ) {
        if (postId.isBlank()) return
        if (mediaItemId > 0) {
            itemIdToPostId[mediaItemId] = postId
        }
        _downloadProgressMap.update { current ->
            val existing = current[postId]
            val updated = (existing ?: DownloadProgress(
                mediaItemId = mediaItemId,
                postId = postId,
                status = MediaStatus.DOWNLOADING
            )).copy(
                progressPercent = progress.coerceIn(0, 100),
                bytesDownloaded = if (bytesDownloaded > 0) bytesDownloaded else existing?.bytesDownloaded ?: 0L,
                totalBytes = if (totalBytes > 0) totalBytes else existing?.totalBytes ?: 0L,
                status = MediaStatus.DOWNLOADING
            )
            current + (postId to updated)
        }
    }

    fun update(mediaItemId: Long, progress: Int) {
        val postId = itemIdToPostId[mediaItemId]
        if (postId != null) {
            update(postId = postId, mediaItemId = mediaItemId, progress = progress)
        }
    }

    fun complete(postId: String, mediaItemId: Long = -1L) {
        if (postId.isBlank()) return
        _downloadProgressMap.update { current ->
            val existing = current[postId]
            if (existing != null) {
                current + (postId to existing.copy(
                    progressPercent = 100,
                    status = MediaStatus.DONE
                ))
            } else {
                current + (postId to DownloadProgress(
                    mediaItemId = mediaItemId,
                    postId = postId,
                    progressPercent = 100,
                    status = MediaStatus.DONE
                ))
            }
        }
    }

    fun fail(postId: String, mediaItemId: Long = -1L, error: String? = null) {
        if (postId.isBlank()) return
        _downloadProgressMap.update { current ->
            val existing = current[postId]
            if (existing != null) {
                current + (postId to existing.copy(
                    status = MediaStatus.FAILED,
                    error = error
                ))
            } else {
                current
            }
        }
    }

    fun pause(postId: String) {
        if (postId.isBlank()) return
        _downloadProgressMap.update { current ->
            val existing = current[postId]
            if (existing != null) {
                current + (postId to existing.copy(status = MediaStatus.PAUSED))
            } else current
        }
    }

    fun resume(postId: String) {
        if (postId.isBlank()) return
        _downloadProgressMap.update { current ->
            val existing = current[postId]
            if (existing != null) {
                current + (postId to existing.copy(status = MediaStatus.PENDING))
            } else current
        }
    }

    fun clear(postId: String) {
        if (postId.isBlank()) return
        _downloadProgressMap.update { current ->
            current - postId
        }
    }

    fun clear(mediaItemId: Long) {
        val postId = itemIdToPostId.remove(mediaItemId)
        if (postId != null) {
            clear(postId)
        }
    }
}
