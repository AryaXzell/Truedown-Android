package com.aryaxzell.truedown.util

/**
 * Deduplicator pencegah looping deteksi papan klip (R-31 / F-45).
 * Memastikan kunci clipboard diekstrak secara konsisten dengan [UrlExtractor.extractFirstUrl],
 * serta memvalidasi timestamp clipboard jika user sengaja menyalin ulang URL yang sama.
 */
class ClipboardDeduplicator {
    var lastHandledClipboardKey: String? = null
        private set
    var lastHandledClipboardTimestamp: Long = 0L
        private set

    fun clipboardKey(text: String): String {
        return UrlExtractor.extractFirstUrl(text) ?: text.trim()
    }

    fun shouldProcess(
        text: String,
        timestamp: Long = 0L,
        isValidUrl: (String) -> Boolean
    ): Boolean {
        if (!isValidUrl(text)) return false
        val key = clipboardKey(text)
        val isNewKey = key != lastHandledClipboardKey
        val isNewTimestamp = lastHandledClipboardTimestamp > 0L && timestamp > lastHandledClipboardTimestamp
        return isNewKey || isNewTimestamp
    }

    fun markHandled(text: String, timestamp: Long = 0L) {
        lastHandledClipboardKey = clipboardKey(text)
        if (timestamp > 0L) {
            lastHandledClipboardTimestamp = timestamp
        }
    }

    fun clearHandled(currentText: String?, timestamp: Long = 0L) {
        if (!currentText.isNullOrBlank()) {
            lastHandledClipboardKey = clipboardKey(currentText)
            if (timestamp > 0L) {
                lastHandledClipboardTimestamp = timestamp
            }
        }
    }

    fun reset() {
        lastHandledClipboardKey = null
        lastHandledClipboardTimestamp = 0L
    }
}
