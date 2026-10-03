package com.aryaxzell.truedown

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient

class TruedownApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .cache(okhttp3.Cache(
                directory = cacheDir.resolve("okhttp_cache"),
                maxSize = 128L * 1024L * 1024L // 128MB Disk Cache
            ))
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()

                // Set User-Agent and Referer headers globally if missing
                if (original.header("User-Agent").isNullOrBlank()) {
                    requestBuilder.header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                }
                if (original.header("Referer").isNullOrBlank()) {
                    requestBuilder.header("Referer", "https://www.tiktok.com/")
                }

                chain.proceed(requestBuilder.build())
            }
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                // Force rewrite cache headers to allow local offline cache for 30 days
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=2592000") // 30 days
                    .removeHeader("Pragma")
                    .removeHeader("Expires")
                    .build()
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                // Enable video frame decoding for local video thumbnails
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    // Strict limit: 25% of max RAM memory (increased from 15%)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(128L * 1024L * 1024L) // 128MB max disk cache (increased from 40MB)
                    .build()
            }
            .allowRgb565(true) // 50% memory saving per bitmap
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            coil.Coil.imageLoader(this).memoryCache?.clear()
        } catch (_: Exception) {}
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            coil.Coil.imageLoader(this).memoryCache?.clear()
        } catch (_: Exception) {}
    }
}
