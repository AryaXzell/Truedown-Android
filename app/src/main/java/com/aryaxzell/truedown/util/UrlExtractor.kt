package com.aryaxzell.truedown.util

import java.net.URI
import java.util.Locale

/**
 * Mengambil URL dari teks bebas (mis. teks share TikTok: "Lihat video ini https://vt.tiktok.com/xxx/").
 * Prioritas: URL host tiktok.com (termasuk subdomain), lalu URL pertama apa pun.
 * Validasi host tetap dilakukan oleh DownloadProvider.
 */
object UrlExtractor {
    private val URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}"

    fun extractFirstUrl(input: String): String? {
        val candidates = URL_REGEX.findAll(input)
            .map { it.value.trimEnd { ch -> TRAILING_PUNCTUATION.indexOf(ch) >= 0 } }
            .filter { it.isNotEmpty() }
            .toList()
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { isTikTokHost(it) } ?: candidates.first()
    }

    private fun isTikTokHost(url: String): Boolean {
        return try {
            val host = URI(url).host?.lowercase(Locale.ROOT) ?: return false
            host == "tiktok.com" || host.endsWith(".tiktok.com")
        } catch (e: Exception) {
            false
        }
    }
}
