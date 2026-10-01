package com.aryaxzell.truedown.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.aryaxzell.truedown.domain.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MediaStoreDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    data class DownloadResult(
        val uri: Uri,
        val fileName: String,
        val sizeBytes: Long
    )

    suspend fun downloadMedia(
        url: String,
        postId: String,
        kind: MediaKind,
        index: Int = 0,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<DownloadResult> = withContext(Dispatchers.IO) {
        try {
            val extension = when (kind) {
                MediaKind.VIDEO -> "mp4"
                MediaKind.PHOTO -> "jpg"
                MediaKind.AUDIO -> "mp3"
            }
            val mimeType = when (kind) {
                MediaKind.VIDEO -> "video/mp4"
                MediaKind.PHOTO -> "image/jpeg"
                MediaKind.AUDIO -> "audio/mpeg"
            }
            val fileName = when (kind) {
                MediaKind.PHOTO -> "truedown_${postId}_photo_${index + 1}.$extension"
                MediaKind.AUDIO -> "truedown_${postId}_audio.$extension"
                MediaKind.VIDEO -> "truedown_${postId}_video.$extension"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty body"))
            val contentLength = body.contentLength()

            if (!hasAvailableStorage(contentLength)) {
                return@withContext Result.failure(Exception("Penyimpanan perangkat tidak mencukupi"))
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = when (kind) {
                        MediaKind.VIDEO -> "${Environment.DIRECTORY_MOVIES}/Truedown"
                        MediaKind.PHOTO -> "${Environment.DIRECTORY_PICTURES}/Truedown"
                        MediaKind.AUDIO -> "${Environment.DIRECTORY_MUSIC}/Truedown"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = when (kind) {
                MediaKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                MediaKind.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                MediaKind.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Gagal membuat entri MediaStore"))

            var bytesWritten = 0L
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                            bytesWritten += read
                            onProgress(bytesWritten, contentLength)
                        }
                        outputStream.flush()
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                Result.success(DownloadResult(uri, fileName, bytesWritten))
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteFromMediaStore(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            false
        }
    }

    fun isMediaFileExisting(uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun hasAvailableStorage(requiredBytes: Long): Boolean {
        if (requiredBytes <= 0) return true
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            available > (requiredBytes + 10 * 1024 * 1024)
        } catch (e: Exception) {
            true
        }
    }
}
