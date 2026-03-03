package com.driverdas.app

object MileageHelper {
    fun metersToMiles(meters: Float): Double {
        return meters * 0.000621371
    }

    fun calculateEfficiency(miles: Double, earnings: Double): Double {
        return if (miles > 0) earnings / miles else 0.0
    }
}
