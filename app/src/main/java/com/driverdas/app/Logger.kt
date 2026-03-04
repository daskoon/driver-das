package com.driverdas.app

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val LOG_FILE_NAME = "driver_das_debug_logs.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [$tag] $message ${throwable?.let { "
${Log.getStackTraceString(it)}" } ?: ""}
"
        
        // Console log
        Log.d(tag, message, throwable)

        // File log
        try {
            val file = File(context.getExternalFilesDir(null), LOG_FILE_NAME)
            file.appendText(logEntry)
        } catch (e: Exception) {
            Log.e("Logger", "Failed to write to log file", e)
        }
    }

    fun getLogFile(context: Context): File {
        return File(context.getExternalFilesDir(null), LOG_FILE_NAME)
    }
    
    fun clearLogs(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(null), LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {}
    }
}
