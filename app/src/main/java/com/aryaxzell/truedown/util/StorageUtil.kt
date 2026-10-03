package com.aryaxzell.truedown.util

import android.os.StatFs
import java.io.File

object StorageUtil {

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
