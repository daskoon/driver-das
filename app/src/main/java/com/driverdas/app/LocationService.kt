package com.driverdas.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.driverdas.app.db.AppDatabase
import com.driverdas.app.db.LocationPointEntity
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: AppDatabase
    
    private var lastLocation: Location? = null
    private var totalDistanceMiles: Double = 0.0
    private var currentShiftId: Long = -1
    
    private var lastPersistTime: Long = 0
    private var lastPersistedMiles: Double = 0.0
    
    private val locationBuffer = mutableListOf<LocationPointEntity>()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1
        
        private val _mileageFlow = MutableStateFlow(0.0)
        val mileageFlow = _mileageFlow.asStateFlow()

        private val _isTracking = MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        Logger.log(this, "LocationService", "Service Created")
        db = AppDatabase.getDatabase(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                calculateAndUpdateMileage(location)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val shiftId = intent?.getLongExtra("SHIFT_ID", -1) ?: -1
        Logger.log(this, "LocationService", "Service Started - Shift ID: $shiftId")
        
        if (shiftId != -1L) {
            currentShiftId = shiftId
            serviceScope.launch {
                val shift = db.shiftDao().getShiftById(currentShiftId)
                shift?.let {
                    totalDistanceMiles = it.totalMiles
                    lastPersistedMiles = it.totalMiles
                    _mileageFlow.value = totalDistanceMiles
                    Logger.log(this@LocationService, "LocationService", "DB Load: Resumed with $totalDistanceMiles miles")
                }
            }
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        _isTracking.value = true
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Logger.log(this, "LocationService", "GPS Updates Requested (1-3s intervals)")
        } catch (e: SecurityException) {
            Logger.log(this, "LocationService", "CRITICAL: Security Exception", e)
            stopSelf()
        }
    }

    private fun calculateAndUpdateMileage(newLocation: Location) {
        val currentTime = System.currentTimeMillis()
        
        // --- IMPROVED FILTERING ---
        
        // 1. Relaxed accuracy filter (50m instead of 20m for Samsung/pockets)
        if (newLocation.accuracy > 50) {
            Logger.log(this, "GPS", "Point Rejected: Bad Accuracy (${newLocation.accuracy}m)")
            return
        }

        if (lastLocation == null) {
            lastLocation = newLocation
            Logger.log(this, "GPS", "Baseline Set: ${newLocation.latitude}, ${newLocation.longitude}")
            return
        }

        val last = lastLocation!!
        
        // 2. Signal Gap Protection
        val timeSinceLastUpdate = newLocation.time - last.time
        if (timeSinceLastUpdate > 180000) { // 3 minutes
            Logger.log(this, "GPS", "Signal gap (>3m). Resetting baseline.")
            lastLocation = newLocation
            return
        }

        val distanceMeters = last.distanceTo(newLocation)
        val distanceMiles = distanceMeters * 0.000621371
        
        // 3. Jitter filter (relaxed to 0.0005 miles ~ 2.6 feet)
        if (distanceMiles < 0.0005) {
            return 
        }

        // 4. Speed sanity check (Keep at 150mph)
        if (timeSinceLastUpdate > 0) {
            val speedMph = (distanceMiles / (timeSinceLastUpdate / 1000.0)) * 3600
            if (speedMph > 150) {
                Logger.log(this, "GPS", "Point Rejected: Impossible Speed ($speedMph mph)")
                return
            }
        }

        // --- CALCULATION SUCCESS ---
        totalDistanceMiles += distanceMiles
        _mileageFlow.value = totalDistanceMiles
        
        updateNotification()

        if (currentShiftId != -1L) {
            locationBuffer.add(
                LocationPointEntity(
                    shiftId = currentShiftId,
                    lat = newLocation.latitude,
                    lng = newLocation.longitude,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            // 5. Throttled Persistence (DB save every 15s or 0.05 miles)
            val milesSinceLastPersist = totalDistanceMiles - lastPersistedMiles
            val timeSinceLastPersist = currentTime - lastPersistTime
            
            if (milesSinceLastPersist > 0.05 || timeSinceLastPersist > 15000) {
                persistCurrentMileage()
                lastPersistTime = currentTime
                lastPersistedMiles = totalDistanceMiles
            }

            if (locationBuffer.size >= 10) {
                flushBuffer()
            }
        }
        lastLocation = newLocation
    }

    private fun persistCurrentMileage() {
        val milesToSave = totalDistanceMiles
        val shiftId = currentShiftId
        serviceScope.launch {
            try {
                val shift = db.shiftDao().getShiftById(shiftId)
                shift?.let {
                    db.shiftDao().updateShift(it.copy(
                        totalMiles = milesToSave,
                        earnings = TaxConfig.calculateDeduction(milesToSave)
                    ))
                    Logger.log(this@LocationService, "DB", "Auto-Saved: $milesToSave miles")
                }
            } catch (e: Exception) {
                Logger.log(this@LocationService, "DB", "Error saving mileage", e)
            }
        }
    }

    private fun flushBuffer() {
        if (locationBuffer.isEmpty()) return
        val pointsToSave = ArrayList(locationBuffer)
        locationBuffer.clear()
        
        serviceScope.launch {
            try {
                db.locationPointDao().insertLocationPoints(pointsToSave)
                Logger.log(this@LocationService, "DB", "Flushed ${pointsToSave.size} breadcrumbs")
            } catch (e: Exception) {
                Logger.log(this@LocationService, "DB", "Error flushing buffer", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Driver DAS Tracking Active")
            .setContentText("Current Trip: ${"%.2f".format(totalDistanceMiles)} miles")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        Logger.log(this, "LocationService", "Service Stopped - Total: $totalDistanceMiles miles")
        flushBuffer()
        persistCurrentMileage()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        _isTracking.value = false
        lastLocation = null
        totalDistanceMiles = 0.0
        _mileageFlow.value = 0.0
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
