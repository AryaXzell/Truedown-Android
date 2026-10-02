package com.aryaxzell.truedown.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardFilterTest {

    private fun isTikTokUrl(text: String): Boolean {
        if (text.isBlank() || text.length > 500) return false
        if (text.count { it == '\n' } > 2) return false
        val cleanUrl = UrlExtractor.extractFirstUrl(text) ?: UrlExtractor.cleanCandidate(text)
        if (cleanUrl.isBlank()) return false
        val regex = Regex("https?://([a-zA-Z0-9_-]+\\.)?tiktok\\.com(/.*)?", RegexOption.IGNORE_CASE)
        val shortRegex = Regex("https?://(vt|vm)\\.tiktok\\.com/([a-zA-Z0-9]+)", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(cleanUrl) || shortRegex.containsMatchIn(cleanUrl) || cleanUrl.contains("tiktok.com/")
    }

    @Test
    fun testValidSingleUrl_isAccepted() {
        val url = "https://vt.tiktok.com/ZS12345/"
        assertTrue("Single valid TikTok URL must be accepted", isTikTokUrl(url))
    }

    @Test
    fun testShortTextWithUrl_isAccepted() {
        val text = "Check out this video: https://vt.tiktok.com/ZS12345/ #trending"
        assertTrue("Short text with TikTok URL must be accepted", isTikTokUrl(text))
    }

    @Test
    fun testLongCopiedLog_isIgnored() {
        val log = buildString {
            appendLine("2026-10-02 12:00:00 [INFO] [TikWmProvider] Resolving URL: https://vt.tiktok.com/ZS12345/")
            appendLine("2026-10-02 12:00:01 [DEBUG] [OkHttp] Request sent to https://www.tikwm.com/api/")
            appendLine("2026-10-02 12:00:02 [INFO] [DownloadWorker] Download started for id: 12345")
            appendLine("2026-10-02 12:00:03 [INFO] [MediaStore] Inserted image into Pictures/Truedown")
        }
        assertFalse("Multi-line application logs must NOT trigger clipboard detection", isTikTokUrl(log))
    }

    @Test
    fun testExceedinglyLongText_isIgnored() {
        val longText = "a".repeat(501) + " https://vt.tiktok.com/ZS12345/"
        assertFalse("Text exceeding 500 characters must be rejected", isTikTokUrl(longText))
    }

    @Test
    fun testNonTikTokUrl_isIgnored() {
        val nonTikTok = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertFalse("Non-TikTok URL must be rejected", isTikTokUrl(nonTikTok))
    }
}
