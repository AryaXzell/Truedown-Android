package com.aryaxzell.truedown.util

import android.content.Context
import android.os.Build
import com.aryaxzell.truedown.BuildConfig
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val context = appContext
        if (context != null) {
            writeCrashLog(context, thread, throwable)
        }

        // Pass to original default handler so Android system can handle app termination
        defaultHandler?.uncaughtException(thread, throwable)
    }

    fun getLogsDir(context: Context): File {
        val dir = File(context.filesDir, "logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable): File? {
        return try {
            val dir = getLogsDir(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "crash_$timeStamp.log")

            val logContent = buildString {
                appendLine("==========================================")
                appendLine("           TRUEDOWN CRASH LOG")
                appendLine("==========================================")
                appendLine("App Version    : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Package Name   : ${context.packageName}")
                appendLine("Timestamp      : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.getDefault()).format(Date())}")
                appendLine("Device Model   : ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android OS     : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Thread Name    : ${thread.name} (ID: ${thread.id})")
                appendLine("==========================================")
                appendLine("EXCEPTIONS STACK TRACE:")
                appendLine("==========================================")
                appendLine(throwable.stackTraceToString())
                appendLine()
                appendLine("==========================================")
                appendLine("RECENT APPLICATION LOGS:")
                appendLine("==========================================")
                appendLine(AppLogger.getFormattedLogText())
                appendLine("==========================================")
            }

            FileWriter(file).use { writer ->
                writer.write(logContent)
            }

            AppLogger.e("CrashHandler", "Crash log successfully saved to ${file.absolutePath}", throwable)
            file
        } catch (e: Exception) {
            AppLogger.e("CrashHandler", "Failed to write crash log: ${e.message}", e)
            null
        }
    }

    fun getLogFiles(context: Context): List<File> {
        val dir = getLogsDir(context)
        return dir.listFiles { _, name -> name.endsWith(".log") || name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun clearAllLogs(context: Context): Boolean {
        return try {
            val dir = getLogsDir(context)
            dir.listFiles()?.forEach { file ->
                file.delete()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
