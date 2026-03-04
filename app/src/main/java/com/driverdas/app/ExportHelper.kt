package com.driverdas.app

import android.content.Context
import com.driverdas.app.db.ShiftEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportShiftsToCsv(context: Context, shifts: List<ShiftEntity>): File? {
        val dir = context.getExternalFilesDir(null) ?: return null
        
        // Cleanup old exports before creating a new one
        try {
            dir.listFiles { _, name -> name.startsWith("DriverDAS_History_") && name.endsWith(".csv") }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            Logger.log(context, "Export", "Failed to clean up old CSVs", e)
        }

        val fileName = "DriverDAS_History_${System.currentTimeMillis()}.csv"
        val file = File(dir, fileName)
        
        try {
            file.printWriter().use { out ->
                // Header
                out.println("Trip ID,Start Date,Start Time,End Date,End Time,Total Miles,Tax Deduction ($)")
                
                shifts.forEachIndexed { index, shift ->
                    val start = Date(shift.startTime)
                    val end = shift.endTime?.let { Date(it) }
                    
                    val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(start)
                    val startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(start)
                    
                    val endDate = end?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: "N/A"
                    val endTime = end?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
                    
                    out.println("${index + 1},$startDate,$startTime,$endDate,$endTime,${"%.2f".format(shift.totalMiles)},${"%.2f".format(shift.earnings)}")
                }
            }
            Logger.log(context, "Export", "Successfully exported ${shifts.size} shifts to CSV")
            return file
        } catch (e: Exception) {
            Logger.log(context, "Export", "Failed to export CSV", e)
            return null
        }
    }
}
