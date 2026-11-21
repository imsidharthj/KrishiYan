package com.krishiyan.booking

import com.krishiyan.equipment.PriceUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class RentalCostTest {
    private val start = Instant.parse("2030-01-01T08:00:00Z")

    @Test
    fun exactHours() {
        assertEquals(RentalCost(3, 2400.0), estimateRentalCost(800.0, PriceUnit.Hour, start, start.plusSeconds(3 * 3600)))
    }

    @Test
    fun startedHourIsCharged() {
        val end = start.plusSeconds(2 * 3600 + 10 * 60)
        assertEquals(RentalCost(3, 2400.0), estimateRentalCost(800.0, PriceUnit.Hour, start, end))
    }

    @Test
    fun exactDays() {
        val end = start.plusSeconds(2 * 86400)
        assertEquals(RentalCost(2, 3000.0), estimateRentalCost(1500.0, PriceUnit.Day, start, end))
    }

    @Test
    fun startedDayIsCharged() {
        val end = start.plusSeconds(86400 + 3600)
        assertEquals(RentalCost(2, 3000.0), estimateRentalCost(1500.0, PriceUnit.Day, start, end))
    }

    @Test
    fun shortDayRentIsOneDay() {
        assertEquals(RentalCost(1, 1500.0), estimateRentalCost(1500.0, PriceUnit.Day, start, start.plusSeconds(4 * 3600)))
    }

    @Test
    fun endBeforeOrAtStartHasNoCost() {
        assertNull(estimateRentalCost(800.0, PriceUnit.Hour, start, start))
        assertNull(estimateRentalCost(800.0, PriceUnit.Hour, start, start.minusSeconds(60)))
    }
}
