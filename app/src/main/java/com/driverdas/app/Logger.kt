package com.driverdas.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val LOG_FILE_NAME = "driver_das_debug_logs.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [$tag] $message" + (throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: "") + "\n"
        
        // Console log (synchronous is fine for Logcat)
        Log.d(tag, message, throwable)

        // File log (asynchronous to prevent ANRs and UI stutters)
        logScope.launch {
            try {
                val file = File(context.getExternalFilesDir(null), LOG_FILE_NAME)
                file.appendText(logEntry)
            } catch (e: Exception) {
                Log.e("Logger", "Failed to write to log file", e)
            }
        }
    }

    fun getLogFile(context: Context): File {
        return File(context.getExternalFilesDir(null), LOG_FILE_NAME)
    }
    
    fun clearLogs(context: Context) {
        logScope.launch {
            try {
                val file = File(context.getExternalFilesDir(null), LOG_FILE_NAME)
                if (file.exists()) file.delete()
            } catch (e: Exception) {}
        }
    }
}
