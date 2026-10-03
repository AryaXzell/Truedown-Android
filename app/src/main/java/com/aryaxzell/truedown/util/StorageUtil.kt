package com.aryaxzell.truedown.util

import android.content.Context
import android.net.Uri
import android.os.StatFs
import com.aryaxzell.truedown.data.local.PostWithMedia
import com.aryaxzell.truedown.domain.model.MediaStatus
import java.io.File

object StorageUtil {

    /**
     * Memeriksa apakah berkas media di URI/MediaStore masih ada dan dapat diakses (belum dihapus pengguna lewat galeri).
     */
    fun isMediaAccessible(context: Context, uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        pfd.statSize > 0
                    } ?: false
                }
                "file" -> {
                    val file = File(uri.path ?: uriString)
                    file.exists() && file.length() > 0
                }
                else -> {
                    val file = File(uriString)
                    file.exists() && file.length() > 0
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Memeriksa apakah setidaknya salah satu berkas media yang selesai diunduh dari sebuah Post masih tersedia di penyimpanan.
     */
    fun hasAccessibleMedia(context: Context, postWithMedia: PostWithMedia): Boolean {
        val doneItems = postWithMedia.mediaItems.filter {
            (it.status == "DONE" || it.status == MediaStatus.DONE.name) && it.mediaStoreUri.isNotBlank()
        }
        if (doneItems.isEmpty()) return false
        return doneItems.any { isMediaAccessible(context, it.mediaStoreUri) }
    }

    /**
     * Mengembalikan jumlah memori penyimpanan internal yang tersedia dalam satuan Bytes.
     */
    fun getAvailableStorageBytes(dir: File): Long {
        return try {
            val stat = StatFs(dir.absolutePath)
            stat.availableBytes
        } catch (e: Exception) {
            dir.usableSpace
        }
    }

    /**
     * Memeriksa apakah penyimpanan internal perangkat mencukupi untuk mengunduh dan memasang berkas.
     * @param dir Direktori tujuan penyimpanan.
     * @param requiredBytes Ukuran perkiraan dari berkas yang akan diunduh.
     * @param safetyBufferBytes Buffer tambahan opsional (default: 25 MB) untuk kelancaran ekstraksi/pemasangan.
     */
    fun hasEnoughStorageSpace(
        dir: File,
        requiredBytes: Long,
        safetyBufferBytes: Long = 25L * 1024 * 1024
    ): Boolean {
        val available = getAvailableStorageBytes(dir)
        val needed = if (requiredBytes > 0) requiredBytes else 15L * 1024 * 1024 // Fallback jika size 0 (biasanya 15MB)
        return available >= (needed + safetyBufferBytes)
    }
}
