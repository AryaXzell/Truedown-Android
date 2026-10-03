package com.aryaxzell.truedown.data.repository

import com.aryaxzell.truedown.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

object ChangelogRepository {

    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/AryaXzell/Truedown-Android/releases"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    data class ReleaseInfo(
        val tagName: String,
        val name: String,
        val publishedAt: String,
        val body: String,
        val htmlUrl: String,
        val isLatest: Boolean = false
    )

    suspend fun fetchReleases(): Result<List<ReleaseInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(GITHUB_RELEASES_URL)
                    .header("User-Agent", "Truedown-Android-App")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    AppLogger.w("ChangelogRepo", "GitHub API returned HTTP ${response.code}")
                    return@withContext Result.success(getFallbackReleases())
                }

                val jsonStr = response.body?.string()
                if (jsonStr.isNullOrBlank()) {
                    return@withContext Result.success(getFallbackReleases())
                }

                val jsonArray = JSONArray(jsonStr)
                val list = mutableListOf<ReleaseInfo>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val tagName = obj.optString("tag_name", "v1.0.0")
                    val name = obj.optString("name", tagName)
                    val rawDate = obj.optString("published_at", "")
                    val formattedDate = formatDate(rawDate)
                    val body = obj.optString("body", "Tidak ada catatan rilis untuk versi ini.")
                    val htmlUrl = obj.optString("html_url", "https://github.com/AryaXzell/Truedown-Android/releases")

                    list.add(
                        ReleaseInfo(
                            tagName = tagName,
                            name = name,
                            publishedAt = formattedDate,
                            body = cleanMarkdown(body),
                            htmlUrl = htmlUrl,
                            isLatest = (i == 0)
                        )
                    )
                }

                if (list.isEmpty()) {
                    Result.success(getFallbackReleases())
                } else {
                    Result.success(list)
                }
            } catch (e: Exception) {
                AppLogger.e("ChangelogRepo", "Kendala saat mengambil changelog dari GitHub API", e)
                Result.success(getFallbackReleases())
            }
        }
    }

    private fun formatDate(rawDate: String): String {
        if (rawDate.isBlank()) return "Rilis Terbaru"
        return try {
            val dateOnly = rawDate.take(10) // YYYY-MM-DD
            val parts = dateOnly.split("-")
            if (parts.size == 3) {
                "${parts[2]}/${parts[1]}/${parts[0]}"
            } else dateOnly
        } catch (_: Exception) {
            rawDate
        }
    }

    private fun cleanMarkdown(text: String): String {
        return text.replace("### ", "")
            .replace("#### ", "• ")
            .replace("## ", "")
            .replace("**", "")
            .replace("`", "")
            .trim()
    }

    fun getFallbackReleases(): List<ReleaseInfo> {
        return listOf(
            ReleaseInfo(
                tagName = "v1.1.1",
                name = "Truedown v1.1.1 — Pembaruan Nightly, Aksesibilitas & Perbaikan Lint",
                publishedAt = "03/10/2026",
                body = "• Perbaikan pengunduhan pembaruan Saluran Nightly in-app dengan fallback cerdas ke GitHub Releases.\n" +
                        "• Penyematan anotasi Media3 UnstableApi resmi pada kartu pratinjau media cepat.\n" +
                        "• Peningkatan aksesibilitas menyeluruh (TalkBack WCAG AA) dan throttling live region progres.\n" +
                        "• Integritas penulisan MediaStore Scoped Storage dan pembersihan berkas lama saat unduh ulang.\n" +
                        "• Lokalisasi lengkap string aksesibilitas untuk Bahasa Indonesia dan Bahasa Inggris.",
                htmlUrl = "https://github.com/AryaXzell/Truedown-Android/releases",
                isLatest = true
            ),
            ReleaseInfo(
                tagName = "v1.1.0",
                name = "Truedown v1.1.0 — Pembaruan Fitur & Stabilitas",
                publishedAt = "02/10/2026",
                body = "• Manajemen audio focus Media3 pada pemutar video & audio bawaan.\n" +
                        "• Tampilan grid slideshow foto dengan target sentuh standar 48dp dan aksi aksesibilitas.\n" +
                        "• Resolver rute media seragam untuk mencegah layar pemutaran kosong.\n" +
                        "• Manajemen alur izin notifikasi & izin instalasi in-app.\n" +
                        "• Saluran pembaruan Stabil & Nightly in-app installer.",
                htmlUrl = "https://github.com/AryaXzell/Truedown-Android/releases",
                isLatest = false
            ),
            ReleaseInfo(
                tagName = "v1.0.0",
                name = "Truedown v1.0.0 — Rilis Awal Resmi",
                publishedAt = "30/09/2026",
                body = "• Pengunduh Video TikTok tanpa watermark.\n" +
                        "• Fitur ekstraksi Audio TikTok ke format MP3.\n" +
                        "• Pengunduh Foto Slideshow TikTok.\n" +
                        "• Dukungan berbagi langsung dari aplikasi TikTok.\n" +
                        "• Riwayat Library lokal dan pemutar media terintegrasi.",
                htmlUrl = "https://github.com/AryaXzell/Truedown-Android/releases",
                isLatest = false
            )
        )
    }
}
