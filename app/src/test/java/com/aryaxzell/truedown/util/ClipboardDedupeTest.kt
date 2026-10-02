package com.aryaxzell.truedown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test deduplikasi clipboard sesuai PRD v1.1 AC-44 dan AC-45 (R-31 / F-45).
 * Memverifikasi bahwa URL clipboard tidak memicu deteksi/auto-download berulang kali
 * saat pengguna kembali ke Home Screen hingga 6 siklus.
 */
class ClipboardDedupeTest {

    private lateinit var deduplicator: ClipboardDeduplicator

    private fun isTikTokUrl(text: String): Boolean {
        if (text.isBlank()) return false
        val regex = Regex("https?://([a-zA-Z0-9_-]+\\.)?tiktok\\.com(/.*)?", RegexOption.IGNORE_CASE)
        val shortRegex = Regex("https?://(vt|vm)\\.tiktok\\.com/([a-zA-Z0-9]+)", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text) || shortRegex.containsMatchIn(text) || text.contains("tiktok.com/")
    }

    @Before
    fun setUp() {
        deduplicator = ClipboardDeduplicator()
    }

    @Test
    fun testCaseA_cleanUrl_triggeredOnlyOnceAcross6Cycles() {
        val cleanUrl = "https://vt.tiktok.com/ZSabc123/"
        val timestamp = 10000L

        var triggeredCount = 0

        // Siklus 1: Masuk ke app pertama kali
        if (deduplicator.shouldProcess(cleanUrl, timestamp, ::isTikTokUrl)) {
            triggeredCount++
            deduplicator.markHandled(cleanUrl, timestamp)
        }
        assertEquals("Siklus 1 harus terpicu", 1, triggeredCount)

        // Siklus 2 s/d 7: 6 siklus kembali ke Home Screen tanpa menyalin baru
        for (cycle in 1..6) {
            val shouldTrigger = deduplicator.shouldProcess(cleanUrl, timestamp, ::isTikTokUrl)
            assertFalse("Siklus $cycle kembali ke Home tidak boleh memicu deteksi ulang", shouldTrigger)
            if (shouldTrigger) triggeredCount++
        }

        assertEquals("Total terpicu harus tepat 1 kali", 1, triggeredCount)
    }

    @Test
    fun testCaseB_textWithLinkAndHashtags_triggeredOnlyOnceAcross6Cycles() {
        val textWithLink = "Lihat video ini https://vt.tiktok.com/ZSabc123/ #fyp"
        val timestamp = 15000L

        var triggeredCount = 0

        // Siklus 1: Masuk pertama kali
        if (deduplicator.shouldProcess(textWithLink, timestamp, ::isTikTokUrl)) {
            triggeredCount++
            // Auto download atau resolve memanggil markHandled
            deduplicator.markHandled(textWithLink, timestamp)
        }
        assertEquals("Siklus 1 harus terpicu", 1, triggeredCount)

        // Siklus 2 s/d 7: 6 siklus kembali ke Home Screen
        for (cycle in 1..6) {
            val shouldTrigger = deduplicator.shouldProcess(textWithLink, timestamp, ::isTikTokUrl)
            assertFalse("Siklus $cycle dengan teks+hashtag tidak boleh memicu deteksi ulang", shouldTrigger)
            if (shouldTrigger) triggeredCount++
        }

        assertEquals("Total terpicu harus tepat 1 kali", 1, triggeredCount)
    }

    @Test
    fun testCaseC_urlWithTrailingParenthesis_triggeredOnlyOnceAcross6Cycles() {
        val urlWithParenthesis = "https://www.tiktok.com/@user.name/video/7312345678901234567?is_from_webapp=1)"
        val timestamp = 20000L

        var triggeredCount = 0

        // Siklus 1: Masuk pertama kali
        if (deduplicator.shouldProcess(urlWithParenthesis, timestamp, ::isTikTokUrl)) {
            triggeredCount++
            deduplicator.markHandled(urlWithParenthesis, timestamp)
        }
        assertEquals("Siklus 1 harus terpicu", 1, triggeredCount)

        // Siklus 2 s/d 7: 6 siklus kembali ke Home Screen
        for (cycle in 1..6) {
            val shouldTrigger = deduplicator.shouldProcess(urlWithParenthesis, timestamp, ::isTikTokUrl)
            assertFalse("Siklus $cycle dengan URL berakhiran kurung tutup tidak boleh memicu deteksi ulang", shouldTrigger)
            if (shouldTrigger) triggeredCount++
        }

        assertEquals("Total terpicu harus tepat 1 kali", 1, triggeredCount)
    }

    @Test
    fun testCopyingDifferentLink_triggersAgain() {
        val url1 = "https://vt.tiktok.com/ZS111111/"
        val url2 = "https://vt.tiktok.com/ZS222222/"

        // Link pertama terpicu
        assertTrue(deduplicator.shouldProcess(url1, 1000L, ::isTikTokUrl))
        deduplicator.markHandled(url1, 1000L)

        // Kembali ke Home dengan link 1 tidak terpicu
        assertFalse(deduplicator.shouldProcess(url1, 1000L, ::isTikTokUrl))

        // Link kedua disalin -> harus terpicu
        assertTrue(deduplicator.shouldProcess(url2, 1050L, ::isTikTokUrl))
        deduplicator.markHandled(url2, 1050L)

        // Kembali ke Home dengan link 2 tidak terpicu
        assertFalse(deduplicator.shouldProcess(url2, 1050L, ::isTikTokUrl))
    }

    @Test
    fun testRecopyingSameLinkWithNewTimestamp_triggersAgain() {
        val url = "https://vt.tiktok.com/ZSabc123/"

        // Salin pertama pada t=1000
        assertTrue(deduplicator.shouldProcess(url, 1000L, ::isTikTokUrl))
        deduplicator.markHandled(url, 1000L)

        // Masih di t=1000 -> tidak terpicu
        assertFalse(deduplicator.shouldProcess(url, 1000L, ::isTikTokUrl))

        // Pengguna menyalin ulang link yang sama persis di TikTok pada t=2500
        assertTrue("Menyalin ulang link yang sama dengan timestamp baru harus terpicu",
            deduplicator.shouldProcess(url, 2500L, ::isTikTokUrl))
        deduplicator.markHandled(url, 2500L)

        // Setelah di-handle, pada t=2500 tidak terpicu lagi
        assertFalse(deduplicator.shouldProcess(url, 2500L, ::isTikTokUrl))
    }

    @Test
    fun testClearHandled_preventsReTrigger() {
        val url = "https://vt.tiktok.com/ZSabc123/"
        assertTrue(deduplicator.shouldProcess(url, 1000L, ::isTikTokUrl))
        // User menekan "Batal" pada banner
        deduplicator.clearHandled(url)

        // Kembali ke Home Screen -> tidak terpicu lagi
        assertFalse(deduplicator.shouldProcess(url, 1000L, ::isTikTokUrl))
    }
}
