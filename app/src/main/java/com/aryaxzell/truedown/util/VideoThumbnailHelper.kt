package com.aryaxzell.truedown.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VideoThumbnailHelper {

    fun getThumbnailFile(context: Context, postId: String): File {
        val dir = File(context.cacheDir, "thumbnails").apply { mkdirs() }
        return File(dir, "thumb_${postId}.jpg")
    }

    fun hasThumbnail(context: Context, postId: String): Boolean {
        val file = getThumbnailFile(context, postId)
        return file.exists() && file.length() > 0
    }

    suspend fun generateThumbnail(
        context: Context,
        videoUriOrPath: String,
        postId: String
    ): File? = withContext(Dispatchers.IO) {
        val targetFile = getThumbnailFile(context, postId)
        if (targetFile.exists() && targetFile.length() > 0) {
            return@withContext targetFile
        }

        var bitmap: Bitmap? = null

        // 1. Try ContentResolver.loadThumbnail for content URIs on Android Q+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && videoUriOrPath.startsWith("content://")) {
            try {
                val uri = Uri.parse(videoUriOrPath)
                bitmap = context.contentResolver.loadThumbnail(uri, Size(360, 360), null)
            } catch (_: Throwable) {}
        }

        // 2. Fallback to MediaMetadataRetriever (works for both file paths and content URIs)
        if (bitmap == null) {
            val retriever = MediaMetadataRetriever()
            try {
                if (videoUriOrPath.startsWith("content://")) {
                    retriever.setDataSource(context, Uri.parse(videoUriOrPath))
                } else {
                    retriever.setDataSource(videoUriOrPath)
                }

                // Extract frame at 1s (1,000,000 microseconds) or closest sync frame
                bitmap = retriever.getFrameAtTime(1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            } catch (_: Throwable) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {}
            }
        }

        if (bitmap != null) {
            try {
                // Scale down if too large to conserve memory and disk
                val maxDim = 360
                val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val targetW = if (ratio >= 1) maxDim else (maxDim * ratio).toInt()
                    val targetH = if (ratio >= 1) (maxDim / ratio).toInt() else maxDim
                    Bitmap.createScaledBitmap(bitmap, targetW.coerceAtLeast(1), targetH.coerceAtLeast(1), true)
                } else {
                    bitmap
                }

                FileOutputStream(targetFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.flush()
                }
                return@withContext targetFile
            } catch (_: Throwable) {
                targetFile.delete()
            }
        }

        null
    }
}
