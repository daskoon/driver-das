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
        db = AppDatabase.getDatabase(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    calculateAndUpdateMileage(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentShiftId = intent?.getLongExtra("SHIFT_ID", -1) ?: -1
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        _isTracking.value = true
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun calculateAndUpdateMileage(newLocation: Location) {
        lastLocation?.let { last ->
            val distanceMeters = last.distanceTo(newLocation)
            val distanceMiles = distanceMeters * 0.000621371
            totalDistanceMiles += distanceMiles
            _mileageFlow.value = totalDistanceMiles
            
            // Update notification with live mileage
            updateNotification()

            // Save point to DB
            if (currentShiftId != -1L) {
                serviceScope.launch {
                    db.locationPointDao().insertLocationPoint(
                        LocationPointEntity(
                            shiftId = currentShiftId,
                            lat = newLocation.latitude,
                            lng = newLocation.longitude,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        lastLocation = newLocation
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
            .setContentTitle("Driver DAS Tracking")
            .setContentText("Mileage: ${"%.2f".format(totalDistanceMiles)} miles")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        _isTracking.value = false
        lastLocation = null
        totalDistanceMiles = 0.0
        _mileageFlow.value = 0.0
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
