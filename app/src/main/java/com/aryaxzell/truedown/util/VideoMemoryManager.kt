package com.aryaxzell.truedown.util

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object VideoMemoryManager {

    private const val RAM_SOFT_LIMIT_MB = 180L
    private const val RAM_HARD_LIMIT_MB = 200L

    private val _currentRamUsageMb = MutableStateFlow(0L)
    val currentRamUsageMb: StateFlow<Long> = _currentRamUsageMb.asStateFlow()

    private val listeners = mutableSetOf<() -> Unit>()
    private var monitorJob: Job? = null

    fun registerTrimListener(listener: () -> Unit) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun unregisterTrimListener(listener: () -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun startMonitoring(context: Context, scope: CoroutineScope) {
        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch(Dispatchers.Default) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            while (isActive) {
                try {
                    val usedMb = getAppUsedRamMb(activityManager)
                    _currentRamUsageMb.value = usedMb

                    if (usedMb >= RAM_SOFT_LIMIT_MB) {
                        AppLogger.w(
                            "MemoryManager",
                            "RAM Limit Warning: ${usedMb}MB used (Threshold: ${RAM_HARD_LIMIT_MB}MB). Executing proactive memory release."
                        )
                        triggerProactiveTrimming(context)
                    }
                } catch (e: Exception) {
                    AppLogger.e("MemoryManager", "Failed to check memory usage: ${e.message}")
                }
                
                val isBatterySaver = try {
                    val prefsRepo = com.aryaxzell.truedown.data.preferences.UserPreferencesRepository(context)
                    prefsRepo.userPreferencesFlow.first().batterySaver
                } catch (_: Exception) {
                    false
                }
                val pollInterval = if (isBatterySaver) 12000L else 3000L // Reduce background CPU work significantly in battery saver mode
                delay(pollInterval)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun triggerProactiveTrimming(context: Context) {
        // 1. Notify active Media3 video/audio sessions to release non-essential buffers
        synchronized(listeners) {
            listeners.toList().forEach { listener ->
                try {
                    listener.invoke()
                } catch (e: Exception) {
                    AppLogger.e("MemoryManager", "Error in trim listener: ${e.message}")
                }
            }
        }

        // 2. Clear Coil image memory cache
        try {
            coil.Coil.imageLoader(context).memoryCache?.clear()
        } catch (_: Exception) {}
    }

    fun getAppUsedRamMb(activityManager: ActivityManager?): Long {
        return try {
            val pids = intArrayOf(Process.myPid())
            val pMem = activityManager?.getProcessMemoryInfo(pids)
            if (pMem != null && pMem.isNotEmpty()) {
                (pMem[0].totalPss / 1024).toLong()
            } else {
                val runtime = Runtime.getRuntime()
                ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024))
            }
        } catch (e: Exception) {
            val runtime = Runtime.getRuntime()
            ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024))
        }
    }
}
