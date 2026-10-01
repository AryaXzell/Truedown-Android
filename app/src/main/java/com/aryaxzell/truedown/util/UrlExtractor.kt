package com.aryaxzell.truedown.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI
import java.util.Locale

/**
 * Mengambil dan membersihkan URL dari teks bebas (mis. teks share dari aplikasi TikTok:
 * "Lihat video seru ini di TikTok! https://vt.tiktok.com/ZS123456/ #fyp \u200E").
 */
object UrlExtractor {
    private val URL_REGEX = Regex(
        """https?://[^\s<>"'\u2000-\u200F\u2028-\u202F\u2060\uFEFF\u00A0]+""",
        RegexOption.IGNORE_CASE
    )

    fun extractFirstUrl(input: String): String? {
        if (input.isBlank()) return null

        val sanitized = sanitize(input)

        val candidates = URL_REGEX.findAll(sanitized)
            .map { cleanCandidate(it.value) }
            .filter { it.isNotBlank() }
            .toList()

        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { isTikTokHost(it) } ?: candidates.first()
    }

    fun sanitize(text: String): String {
        return text
            .replace(Regex("""[\u200B-\u200D\u200E\u200F\u202A-\u202E\u2060\uFEFF\u00A0]"""), " ")
            .trim()
    }

    fun cleanCandidate(rawUrl: String): String {
        var url = rawUrl.trim()

        // Strip non-ASCII characters
        url = url.replace(Regex("""[^\x20-\x7E]"""), "")

        // Strip trailing punctuation
        val trailingPunctuation = ".,;:!?)]}\"'"
        while (url.isNotEmpty() && trailingPunctuation.indexOf(url.last()) >= 0) {
            url = url.substring(0, url.length - 1)
        }

        return url
    }

    fun isTikTokHost(url: String): Boolean {
        if (url.isBlank()) return false
        val clean = cleanCandidate(url)
        return try {
            val host = clean.toHttpUrlOrNull()?.host?.lowercase(Locale.ROOT)
                ?: URI(clean).host?.lowercase(Locale.ROOT)
                ?: return false
            host == "tiktok.com" || host.endsWith(".tiktok.com") || host == "douyin.com" || host.endsWith(".douyin.com")
        } catch (_: Exception) {
            false
        }
    }
}
