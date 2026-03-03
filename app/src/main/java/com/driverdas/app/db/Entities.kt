package com.driverdas.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val totalMiles: Double = 0.0,
    val earnings: Double = 0.0,
    val platform: String? = null
)

@Entity(tableName = "location_points")
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)
