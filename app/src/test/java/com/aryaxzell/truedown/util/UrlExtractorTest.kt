package com.aryaxzell.truedown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun testExtractFirstUrl_shortLinkWithHashtag() {
        val input = "Check out this video https://vt.tiktok.com/ZSabc123/ #fyp"
        val result = UrlExtractor.extractFirstUrl(input)
        assertEquals("https://vt.tiktok.com/ZSabc123/", result)
    }

    @Test
    fun testExtractFirstUrl_vmLinkWithTrailingDot() {
        val input = "https://vm.tiktok.com/ZMabc/."
        val result = UrlExtractor.extractFirstUrl(input)
        assertEquals("https://vm.tiktok.com/ZMabc/", result)
    }

    @Test
    fun testExtractFirstUrl_fullUrlWithParamsAndTrailingComma() {
        val input = "Lihat https://www.tiktok.com/@user.name/video/7312345678901234567?is_from_webapp=1&sender_device=pc, bagus (https://example.com/x)"
        val result = UrlExtractor.extractFirstUrl(input)
        assertEquals("https://www.tiktok.com/@user.name/video/7312345678901234567?is_from_webapp=1&sender_device=pc", result)
    }

    @Test
    fun testExtractFirstUrl_textWithoutLink() {
        val input = "teks tanpa link"
        val result = UrlExtractor.extractFirstUrl(input)
        assertNull(result)
    }

    @Test
    fun testExtractFirstUrl_prioritizeTikTokOverOtherUrl() {
        val input = "https://example.com/a lalu https://vt.tiktok.com/ZS1/"
        val result = UrlExtractor.extractFirstUrl(input)
        assertEquals("https://vt.tiktok.com/ZS1/", result)
    }

    @Test
    fun testExtractFirstUrl_douyinUrlFallback() {
        val input = "https://www.douyin.com/video/123"
        val result = UrlExtractor.extractFirstUrl(input)
        assertEquals("https://www.douyin.com/video/123", result)
    }
}
