package com.driverdas.app

object TaxConfig {
    /**
     * Standard mileage rate for 2024: $0.67 per mile.
     */
    const val IRS_MILEAGE_RATE_2024 = 0.67

    fun calculateDeduction(miles: Double): Double {
        return miles * IRS_MILEAGE_RATE_2024
    }
}
