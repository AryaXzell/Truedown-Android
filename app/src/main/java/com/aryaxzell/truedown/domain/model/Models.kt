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
