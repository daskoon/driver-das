package com.driverdas.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.driverdas.app.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        // Filter for delivery apps
        if (packageName.contains("doordash") || packageName.contains("gopuff")) {
            parseEarnings(text)
        }
    }

    private fun parseEarnings(text: String) {
        // Simple regex to find dollar amounts like $12.50
        val regex = Regex("""\$(\d+\.\d{2})""")
        val match = regex.find(text)
        
        match?.let {
            val amount = it.groupValues[1].toDoubleOrNull()
            if (amount != null) {
                updateCurrentShiftEarnings(amount)
            }
        }
    }

    private fun updateCurrentShiftEarnings(amount: Double) {
        serviceScope.launch {
            // Logic to find the active shift and increment earnings
            // This is a simplified version; in production, you'd match the active shift ID
            val shifts = db.shiftDao().getAllShifts() // Note: getAllShifts returns a Flow
            // Implementation detail: for simplicity, we'd need a non-flow way or collect latest
        }
    }
}
