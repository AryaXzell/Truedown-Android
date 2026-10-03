package com.aryaxzell.truedown.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLoggerTest {

    @Before
    fun setUp() {
        AppLogger.clear()
    }

    @Test
    fun testAppLogger_maintainsFifoAndLimit500On600Entries() {
        AppLogger.clear()
        assertEquals(0, AppLogger.logs.value.size)

        for (i in 1..600) {
            AppLogger.i("TestTag", "Log message #$i")
        }

        val logs = AppLogger.logs.value
        assertEquals("Log size must cap at 500", 500, logs.size)

        // FIFO verification: Oldest entry remaining must be #101, newest must be #600
        assertEquals("Log message #101", logs.first().message)
        assertEquals("Log message #600", logs.last().message)

        // Check monotonically increasing IDs
        for (i in 0 until logs.size - 1) {
            assertTrue(logs[i].id < logs[i + 1].id)
        }
    }

    @Test
    fun testAppLogger_clear_emptiesLogs() {
        AppLogger.i("Tag", "Msg 1")
        AppLogger.i("Tag", "Msg 2")
        assertEquals(2, AppLogger.logs.value.size)

        AppLogger.clear()
        assertEquals(0, AppLogger.logs.value.size)
    }

    @Test
    fun testAppLogger_concurrentLogging_isThreadSafe() = runBlocking {
        AppLogger.clear()
        val coroutinesCount = 20
        val logsPerCoroutine = 50

        val jobs = (1..coroutinesCount).map { cIndex ->
            launch(Dispatchers.Default) {
                for (i in 1..logsPerCoroutine) {
                    AppLogger.d("ThreadTag-$cIndex", "Concurrent message $i from worker $cIndex")
                }
            }
        }
        jobs.joinAll()

        val logs = AppLogger.logs.value
        assertEquals("Total logs should be 500 (capped at MAX_LOGS)", 500, logs.size)
    }
}
