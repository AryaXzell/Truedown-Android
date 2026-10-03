package com.aryaxzell.truedown.domain.model

enum class PostType {
    VIDEO,
    SLIDESHOW
}

enum class MediaKind {
    VIDEO,
    PHOTO,
    AUDIO
}

enum class MediaStatus {
    PENDING,
    DOWNLOADING,
    DONE,
    FAILED,
    MISSING
}

enum class VideoQuality(val value: String) {
    STANDARD("STANDARD"),
    HD("HD")
}

data class ResolvedPost(
    val id: String,
    val type: PostType,
    val title: String,
    val authorName: String,
    val authorHandle: String,
    val videoStandardUrl: String?,
    val videoHdUrl: String?,
    val photoUrls: List<String>,
    val audioUrl: String?,
    val coverUrl: String?,
    val durationSec: Int,
    val sourceUrl: String,
    val videoStandardSizeBytes: Long? = null,
    val videoHdSizeBytes: Long? = null,
    val audioSizeBytes: Long? = null
)

fun Long.toHumanReadableSize(): String {
    if (this <= 0) return ""
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
        else -> "$this B"
    }
}

sealed class ProviderError(message: String) : Exception(message) {
    object InvalidLink : ProviderError("Link tidak valid")
    object DouyinUnsupported : ProviderError("Douyin tidak didukung")
    object NotTikTokLink : ProviderError("Bukan link TikTok yang valid")
    object TimeoutError : ProviderError("Waktu habis saat menghubungi server")
    object NetworkError : ProviderError("Masalah koneksi internet")
    object RateLimited : ProviderError("Terlalu banyak permintaan, coba lagi nanti")
    data class PostUnavailable(val reason: String) : ProviderError("Postingan tidak tersedia: $reason")
    data class Unknown(val detail: String) : ProviderError("Gagal: $detail")
}

data class DownloadProgress(
    val mediaItemId: Long = -1L,
    val postId: String = "",
    val title: String = "",
    val kind: MediaKind = MediaKind.VIDEO,
    val index: Int = 0,
    val progressPercent: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val status: MediaStatus = MediaStatus.PENDING,
    val error: String? = null,
    val startEpochMs: Long = System.currentTimeMillis()
)

sealed class OpenTarget {
    data class Video(val postWithMedia: com.aryaxzell.truedown.data.local.PostWithMedia) : OpenTarget()
    data class Audio(val postWithMedia: com.aryaxzell.truedown.data.local.PostWithMedia) : OpenTarget()
    data class Slideshow(val postWithMedia: com.aryaxzell.truedown.data.local.PostWithMedia) : OpenTarget()
    data class MediaDeleted(val postWithMedia: com.aryaxzell.truedown.data.local.PostWithMedia) : OpenTarget()
    object NotFound : OpenTarget()
}

fun resolveOpenTarget(
    postWithMedia: com.aryaxzell.truedown.data.local.PostWithMedia,
    context: android.content.Context? = null
): OpenTarget {
    val items = postWithMedia.mediaItems

    // Periksa apakah media sebelumnya telah diunduh namun kini dihapus dari penyimpanan perangkat
    if (context != null) {
        val hasDoneItems = items.any { (it.status == "DONE" || it.status == MediaStatus.DONE.name) && it.mediaStoreUri.isNotBlank() }
        if (hasDoneItems && !com.aryaxzell.truedown.util.StorageUtil.hasAccessibleMedia(context, postWithMedia)) {
            return OpenTarget.MediaDeleted(postWithMedia)
        }
    }

    val hasDoneVideo = items.any {
        (it.kind == MediaKind.VIDEO.name || it.kind == "VIDEO") &&
        (it.status == MediaStatus.DONE.name || it.status == "DONE") &&
        it.mediaStoreUri.isNotBlank() &&
        (context == null || com.aryaxzell.truedown.util.StorageUtil.isMediaAccessible(context, it.mediaStoreUri))
    }
    if (hasDoneVideo) {
        return OpenTarget.Video(postWithMedia)
    }

    val hasDoneAudio = items.any {
        (it.kind == MediaKind.AUDIO.name || it.kind == "AUDIO") &&
        (it.status == MediaStatus.DONE.name || it.status == "DONE") &&
        it.mediaStoreUri.isNotBlank() &&
        (context == null || com.aryaxzell.truedown.util.StorageUtil.isMediaAccessible(context, it.mediaStoreUri))
    }

    val hasPhotos = items.any {
        (it.kind == MediaKind.PHOTO.name || it.kind == "PHOTO") &&
        (it.status == MediaStatus.DONE.name || it.status == "DONE") &&
        it.mediaStoreUri.isNotBlank() &&
        (context == null || com.aryaxzell.truedown.util.StorageUtil.isMediaAccessible(context, it.mediaStoreUri))
    } || postWithMedia.post.type == PostType.SLIDESHOW.name || postWithMedia.post.type == "SLIDESHOW"

    if (hasDoneAudio && !hasPhotos) {
        return OpenTarget.Audio(postWithMedia)
    }

    if (hasPhotos) {
        return OpenTarget.Slideshow(postWithMedia)
    }

    if (hasDoneAudio) {
        return OpenTarget.Audio(postWithMedia)
    }

    return OpenTarget.NotFound
}
