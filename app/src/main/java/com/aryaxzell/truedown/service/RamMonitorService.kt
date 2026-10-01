package com.aryaxzell.truedown.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.aryaxzell.truedown.util.AppLogger
import com.aryaxzell.truedown.util.VideoMemoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class RamMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("RamMonitorService", "Background RAM Monitor Service started")
        VideoMemoryManager.startMonitoring(applicationContext, serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoMemoryManager.stopMonitoring()
        serviceScope.cancel()
        AppLogger.i("RamMonitorService", "Background RAM Monitor Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
