package com.aryaxzell.truedown.util

object ReleaseNotesFormatter {

    /**
     * Memformat teks catatan rilis mentah (raw GitHub Markdown) menjadi teks bersih,
     * rapi, dan nyaman dibaca oleh pengguna di dalam aplikasi.
     */
    fun format(rawText: String?): String {
        if (rawText.isNullOrBlank()) {
            return "Pembaruan stabilitas, peningkatan performa, dan optimalisasi sistem."
        }

        var text = rawText

        // 1. Hapus boilerplate footer otomatis GitHub
        text = text.replace(Regex("(?i)\\*\\*Full Changelog\\*\\*:?.*"), "")
        text = text.replace(Regex("(?i)Full Changelog:?.*"), "")
        text = text.replace(Regex("(?i)See the full changelog:?.*"), "")

        // 2. Format kontributor: "by @user in https://github.com/..." -> "(oleh @user)"
        text = text.replace(Regex("(?i)by @([A-Za-z0-9_-]+) in https?://github\\.com/[^\\s]+"), "(oleh @$1)")
        text = text.replace(Regex("(?i)by @([A-Za-z0-9_-]+)"), "(oleh @$1)")

        // 3. Hapus tautan pull request mentah: "in https://github.com/..."
        text = text.replace(Regex("(?i)in https?://github\\.com/[^\\s]+"), "")

        // 4. Ubah format markdown link: [Judul](http://...) -> Judul
        text = text.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")

        // 5. Hapus referensi nomor PR: "(#123)" atau "#123" di akhir baris
        text = text.replace(Regex("\\(#\\d+\\)"), "")

        // 6. Hapus URL GitHub langsung yang tersisa
        text = text.replace(Regex("https?://github\\.com/[^\\s]+"), "")

        // 7. Bersihkan format heading markdown (#, ##, ###, ####)
        text = text.replace(Regex("(?m)^#{1,6}\\s*"), "")

        // 8. Standarisasi butir daftar (*, -, +) menjadi bullet bulat rapi (•)
        text = text.replace(Regex("(?m)^[\\*\\-\\+]\\s*"), "• ")

        // 9. Hapus karakter cetak tebal, miring, dan backtick markdown
        text = text.replace("**", "")
        text = text.replace("__", "")
        text = text.replace("`", "")

        // 10. Rapikan spasi berulang dan baris kosong ganda
        val cleanedLines = text.lines().map { it.trimEnd() }
        val result = mutableListOf<String>()
        var previousWasEmpty = false

        for (line in cleanedLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (!previousWasEmpty && result.isNotEmpty()) {
                    result.add("")
                    previousWasEmpty = true
                }
            } else {
                result.add(trimmed)
                previousWasEmpty = false
            }
        }

        val formattedResult = result.joinToString("\n").trim()
        return if (formattedResult.isBlank()) {
            "Pembaruan stabilitas, peningkatan performa, dan optimalisasi sistem."
        } else {
            formattedResult
        }
    }
}
