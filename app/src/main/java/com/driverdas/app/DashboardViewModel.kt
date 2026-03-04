package com.driverdas.app

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.driverdas.app.db.AppDatabase
import com.driverdas.app.db.ShiftEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val shiftDao = db.shiftDao()

    val isTracking = LocationService.isTracking
    val currentMileage = LocationService.mileageFlow
    
    val pastShifts: StateFlow<List<ShiftEntity>> = shiftDao.getAllShifts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentShiftId: Long = -1

    fun toggleShift() {
        if (isTracking.value) {
            stopShift()
        } else {
            startShift()
        }
    }

    private fun startShift() {
        Logger.log(getApplication(), "UI", "Starting Shift")
        viewModelScope.launch {
            try {
                val newShift = ShiftEntity(startTime = System.currentTimeMillis())
                currentShiftId = shiftDao.insertShift(newShift)

                val intent = Intent(getApplication(), LocationService::class.java).apply {
                    putExtra("SHIFT_ID", currentShiftId)
                }
                getApplication<Application>().startForegroundService(intent)
                
                val floatIntent = Intent(getApplication(), FloatingService::class.java)
                getApplication<Application>().startService(floatIntent)
            } catch (e: Exception) {
                Logger.log(getApplication(), "UI", "Failed to start shift", e)
            }
        }
    }

    private fun stopShift() {
        Logger.log(getApplication(), "UI", "Stopping Shift")
        val shiftIdToUpdate = currentShiftId
        
        val intent = Intent(getApplication(), LocationService::class.java)
        getApplication<Application>().stopService(intent)
        
        val floatIntent = Intent(getApplication(), FloatingService::class.java)
        getApplication<Application>().stopService(floatIntent)

        viewModelScope.launch {
            try {
                val shift = db.shiftDao().getShiftById(shiftIdToUpdate)
                shift?.let {
                    val updatedShift = it.copy(
                        endTime = System.currentTimeMillis(),
                        totalMiles = currentMileage.value,
                        earnings = TaxConfig.calculateDeduction(currentMileage.value)
                    )
                    db.shiftDao().updateShift(updatedShift)
                }
                if (shiftIdToUpdate == currentShiftId) {
                    currentShiftId = -1
                }
            } catch (e: Exception) {
                Logger.log(getApplication(), "UI", "Failed to stop shift properly", e)
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            val shifts = pastShifts.value
            if (shifts.isEmpty()) {
                Toast.makeText(getApplication(), "No shifts to export", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val file = ExportHelper.exportShiftsToCsv(getApplication(), shifts)
            if (file != null) {
                Toast.makeText(getApplication(), "Exported to: ${file.name}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(getApplication(), "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearLogs() {
        Logger.clearLogs(getApplication())
        Toast.makeText(getApplication(), "Logs cleared", Toast.LENGTH_SHORT).show()
    }
}
