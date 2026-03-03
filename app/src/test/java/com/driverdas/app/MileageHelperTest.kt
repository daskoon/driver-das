package com.driverdas.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MileageHelperTest {
    @Test
    fun testMetersToMiles() {
        val meters = 1609.34f // Approximately 1 mile
        val miles = MileageHelper.metersToMiles(meters)
        assertEquals(1.0, miles, 0.001)
    }

    @Test
    fun testCalculateEfficiency() {
        val miles = 10.0
        val earnings = 20.0
        val efficiency = MileageHelper.calculateEfficiency(miles, earnings)
        assertEquals(2.0, efficiency, 0.001)
    }

    @Test
    fun testEfficiencyWithZeroMiles() {
        val miles = 0.0
        val earnings = 20.0
        val efficiency = MileageHelper.calculateEfficiency(miles, earnings)
        assertEquals(0.0, efficiency, 0.001)
    }
}
