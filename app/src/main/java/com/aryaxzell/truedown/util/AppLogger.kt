package com.aryaxzell.truedown.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

data class LogEntry(
    val id: Long,
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)

object AppLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var counter = 0L
    private const val MAX_LOGS = 500
    private val buffer = ArrayDeque<LogEntry>(MAX_LOGS)

    fun d(tag: String, message: String) {
        add("DEBUG", tag, message)
        try { Log.d(tag, message) } catch (_: Throwable) {}
    }

    fun i(tag: String, message: String) {
        add("INFO", tag, message)
        try { Log.i(tag, message) } catch (_: Throwable) {}
    }

    fun w(tag: String, message: String) {
        add("WARN", tag, message)
        try { Log.w(tag, message) } catch (_: Throwable) {}
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$message\n${throwable.stackTraceToString()}" else message
        add("ERROR", tag, fullMsg)
        try { Log.e(tag, message, throwable) } catch (_: Throwable) {}
    }

    private fun add(level: String, tag: String, message: String) {
        val time = dateFormat.format(Date())
        val entry = LogEntry(++counter, time, level, tag, message)
        synchronized(buffer) {
            if (buffer.size >= MAX_LOGS) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
            _logs.value = buffer.toList()
        }
    }

    fun clear() {
        synchronized(buffer) {
            buffer.clear()
            _logs.value = emptyList()
        }
    }

    fun getFormattedLogText(): String {
        return _logs.value.joinToString("\n") { log ->
            "[${log.timestamp}] [${log.level}] [${log.tag}]: ${log.message}"
        }
    }
}
