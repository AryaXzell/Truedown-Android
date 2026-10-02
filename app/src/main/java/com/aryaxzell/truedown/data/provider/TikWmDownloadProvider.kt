package com.aryaxzell.truedown.data.provider

import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.domain.model.ProviderError
import com.aryaxzell.truedown.domain.model.ResolvedPost
import com.aryaxzell.truedown.util.AppLogger
import com.aryaxzell.truedown.util.DohDns
import com.aryaxzell.truedown.util.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit

class TikWmDownloadProvider(
    private var dohProviderKey: String = "SYSTEM"
) : DownloadProvider {

    private fun buildClient(providerKey: String): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(DohDns(providerKey))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private var client: OkHttpClient = buildClient(dohProviderKey)

    fun updateDohProvider(providerKey: String) {
        if (dohProviderKey != providerKey) {
            dohProviderKey = providerKey
            client = buildClient(providerKey)
            AppLogger.i("TikWmProvider", "Updated OkHttpClient DoH provider to: $providerKey")
        }
    }

    data class CachedEntry(val post: ResolvedPost, val timestamp: Long)

    val memoryCache: MutableMap<String, CachedEntry> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedEntry>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedEntry>?): Boolean {
                return size > 15
            }
        }
    )
    private val cacheDurationMs = 300_000L // 5 mins

    override fun getCached(url: String): ResolvedPost? {
        val entry = memoryCache[url.trim()] ?: return null
        if (System.currentTimeMillis() - entry.timestamp < cacheDurationMs) {
            return entry.post
        }
        memoryCache.remove(url.trim())
        return null
    }

    override suspend fun resolve(url: String): Result<ResolvedPost> {
        val cleanUrl = UrlExtractor.extractFirstUrl(url) ?: UrlExtractor.cleanCandidate(url)
        AppLogger.i("TikWmProvider", "Resolving URL: $cleanUrl")
        val validation = validateTikTokUrl(cleanUrl)
        if (validation.isFailure) {
            val err = validation.exceptionOrNull() ?: ProviderError.InvalidLink
            AppLogger.w("TikWmProvider", "Validation failed for $cleanUrl: ${err.message}")
            return Result.failure(err)
        }

        val cached = getCached(cleanUrl)
        if (cached != null) {
            AppLogger.d("TikWmProvider", "Loaded from memory cache: ${cached.id}")
            return Result.success(cached)
        }

        return withContext(Dispatchers.IO) {
            try {
                val formBody = FormBody.Builder()
                    .add("url", cleanUrl)
                    .add("count", "12")
                    .add("cursor", "0")
                    .add("web", "1")
                    .add("hd", "1")
                    .build()

                val request = Request.Builder()
                    .url("https://www.tikwm.com/api/")
                    .post(formBody)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                    .build()

                val call = client.newCall(request)
                val response = kotlinx.coroutines.suspendCancellableCoroutine<Response> { continuation ->
                    continuation.invokeOnCancellation {
                        call.cancel()
                    }
                    call.enqueue(object : okhttp3.Callback {
                        override fun onResponse(call: okhttp3.Call, response: Response) {
                            continuation.resumeWith(Result.success(response))
                        }
                        override fun onFailure(call: okhttp3.Call, e: IOException) {
                            if (continuation.isCancelled) return
                            continuation.resumeWith(Result.failure(e))
                        }
                    })
                }
                val bodyString = response.body?.string()

                if (!response.isSuccessful || bodyString.isNullOrBlank()) {
                    if (response.code == 429) {
                        return@withContext Result.failure(ProviderError.RateLimited)
                    }
                    return@withContext Result.failure(ProviderError.NetworkError)
                }

                val json = JSONObject(bodyString)
                val code = json.optInt("code", -1)
                val msg = json.optString("msg", "")

                if (code != 0) {
                    val error = when {
                        msg.contains("rate limit", ignoreCase = true) -> ProviderError.RateLimited
                        msg.contains("not found", ignoreCase = true) || msg.contains("private", ignoreCase = true) ->
                            ProviderError.PostUnavailable(msg)
                        else -> ProviderError.Unknown(if (msg.isNotBlank()) msg else "Kode status: $code")
                    }
                    return@withContext Result.failure(error)
                }

                val data = json.optJSONObject("data")
                    ?: return@withContext Result.failure(ProviderError.Unknown("Format respons tidak valid"))

                val id = data.optString("id", "")
                if (id.isBlank()) {
                    return@withContext Result.failure(ProviderError.Unknown("ID postingan tidak ditemukan"))
                }

                val rawTitle = data.optString("title", "").trim()
                val title = if (rawTitle.isBlank()) "TikTok $id" else rawTitle

                val authorObj = data.optJSONObject("author")
                val authorName = authorObj?.optString("nickname")?.takeIf { it.isNotBlank() } ?: "TikTok Creator"
                val authorHandle = authorObj?.optString("unique_id")?.takeIf { it.isNotBlank() } ?: ""

                val imagesArray = data.optJSONArray("images")
                val photoUrls = mutableListOf<String>()
                if (imagesArray != null) {
                    for (i in 0 until imagesArray.length()) {
                        val img = imagesArray.optString(i)
                        if (!img.isNullOrBlank()) {
                            photoUrls.add(normalizeUrl(img))
                        }
                    }
                }

                val rawPlayStr = data.optString("play", "")
                val rawHdPlayStr = data.optString("hdplay", "")
                val rawMusicStr = data.optString("music", "")
                var rawCoverStr = data.optString("cover", "")
                if (rawCoverStr.isBlank()) rawCoverStr = data.optString("origin_cover", "")
                if (rawCoverStr.isBlank()) rawCoverStr = data.optString("dynamic_cover", "")

                val standardVideoUrl = if (rawPlayStr.isNotBlank()) normalizeUrl(rawPlayStr) else null
                val hdVideoUrl = if (rawHdPlayStr.isNotBlank()) normalizeUrl(rawHdPlayStr) else null
                val audioUrl = if (rawMusicStr.isNotBlank()) normalizeUrl(rawMusicStr) else null
                val coverUrl = if (rawCoverStr.isNotBlank()) normalizeUrl(rawCoverStr) else photoUrls.firstOrNull()
                val durationSec = data.optInt("duration", 0)

                val rawStdSize = data.optLong("size", 0L)
                val rawStandardSize: Long? = if (rawStdSize > 0L) rawStdSize else null
                val rawHdSizeVal = data.optLong("hd_size", 0L)
                val rawHdSize: Long? = if (rawHdSizeVal > 0L) rawHdSizeVal else null
                val rawAudioSizeVal = data.optJSONObject("music_info")?.optLong("size", 0L) ?: 0L
                val rawAudioSize: Long? = if (rawAudioSizeVal > 0L) rawAudioSizeVal else null

                val finalStandardSize = rawStandardSize ?: fetchContentLength(standardVideoUrl)
                val finalHdSize = rawHdSize ?: fetchContentLength(hdVideoUrl)
                val finalAudioSize = rawAudioSize ?: fetchContentLength(audioUrl)

                val type = if (photoUrls.size > 1) PostType.SLIDESHOW else PostType.VIDEO

                val resolved = ResolvedPost(
                    id = id,
                    type = type,
                    title = title,
                    authorName = authorName,
                    authorHandle = authorHandle,
                    videoStandardUrl = standardVideoUrl,
                    videoHdUrl = hdVideoUrl,
                    photoUrls = photoUrls,
                    audioUrl = audioUrl,
                    coverUrl = coverUrl,
                    durationSec = durationSec,
                    sourceUrl = cleanUrl,
                    videoStandardSizeBytes = finalStandardSize,
                    videoHdSizeBytes = finalHdSize,
                    audioSizeBytes = finalAudioSize
                )

                memoryCache[cleanUrl] = CachedEntry(resolved, System.currentTimeMillis())
                Result.success(resolved)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SocketTimeoutException) {
                Result.failure(ProviderError.TimeoutError)
            } catch (e: IOException) {
                Result.failure(ProviderError.NetworkError)
            } catch (e: Exception) {
                Result.failure(ProviderError.Unknown(e.localizedMessage ?: "Kesalahan tak terduga"))
            }
        }
    }

    private fun validateTikTokUrl(rawUrl: String): Result<Unit> {
        val url = UrlExtractor.extractFirstUrl(rawUrl) ?: UrlExtractor.cleanCandidate(rawUrl)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return Result.failure(ProviderError.InvalidLink)
        }
        return try {
            val host = url.toHttpUrlOrNull()?.host?.lowercase(Locale.ROOT)
                ?: URI(url).host?.lowercase(Locale.ROOT)
                ?: return Result.failure(ProviderError.InvalidLink)
            if (host.contains("douyin.com")) {
                Result.failure(ProviderError.DouyinUnsupported)
            } else if (host == "tiktok.com" || host.endsWith(".tiktok.com")) {
                Result.success(Unit)
            } else {
                Result.failure(ProviderError.NotTikTokLink)
            }
        } catch (e: Exception) {
            Result.failure(ProviderError.InvalidLink)
        }
    }

    fun normalizeUrl(url: String): String {
        return when {
            url.startsWith("http://") -> "https://" + url.removePrefix("http://")
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "https://www.tikwm.com$url"
            else -> url
        }
    }

    private suspend fun fetchContentLength(url: String?): Long? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.tiktok.com/")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val len = response.header("Content-Length")?.toLongOrNull()
                        if (len != null && len > 0) return@withContext len
                    }
                }
            } catch (_: Exception) {}
            null
        }
    }
}
