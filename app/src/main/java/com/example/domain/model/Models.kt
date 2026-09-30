package com.example.domain.model

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
    val sourceUrl: String
)

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
