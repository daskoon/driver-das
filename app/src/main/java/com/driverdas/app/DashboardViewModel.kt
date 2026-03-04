package com.driverdas.app

import android.app.Application
import android.content.Intent
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
        viewModelScope.launch {
            val newShift = ShiftEntity(startTime = System.currentTimeMillis())
            currentShiftId = shiftDao.insertShift(newShift)

            val intent = Intent(getApplication(), LocationService::class.java).apply {
                putExtra("SHIFT_ID", currentShiftId)
            }
            getApplication<Application>().startForegroundService(intent)
            
            // Also start floating dashboard
            val floatIntent = Intent(getApplication(), FloatingService::class.java)
            getApplication<Application>().startService(floatIntent)
        }
    }

    private fun stopShift() {
        val intent = Intent(getApplication(), LocationService::class.java)
        getApplication<Application>().stopService(intent)
        
        val floatIntent = Intent(getApplication(), FloatingService::class.java)
        getApplication<Application>().stopService(floatIntent)

        viewModelScope.launch {
            val lastShift = shiftDao.getAllShifts().first().firstOrNull()
            lastShift?.let {
                val updatedShift = it.copy(
                    endTime = System.currentTimeMillis(),
                    totalMiles = currentMileage.value
                )
                shiftDao.updateShift(updatedShift)
            }
            currentShiftId = -1
        }
    }
}
