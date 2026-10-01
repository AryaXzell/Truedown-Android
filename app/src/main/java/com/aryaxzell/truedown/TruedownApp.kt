package com.aryaxzell.truedown

import android.app.Application
import android.content.Intent
import com.aryaxzell.truedown.service.RamMonitorService
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient

class TruedownApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        try {
            val serviceIntent = Intent(this, RamMonitorService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
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
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                // Enable video frame decoding for local video thumbnails
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    // Strict limit: 15% of max RAM memory
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(40L * 1024L * 1024L) // 40MB max disk cache
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
