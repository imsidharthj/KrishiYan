package com.krishiyan.booking

import com.krishiyan.equipment.PriceUnit
import java.time.Duration
import java.time.Instant

data class RentalCost(val units: Long, val total: Double)

/**
 * Rent is charged per started hour or per started day, matching rental_cost() in the database:
 * 2 hours 10 minutes on an hourly price costs 3 hours.
 */
fun estimateRentalCost(price: Double, priceUnit: PriceUnit, start: Instant, end: Instant): RentalCost? {
    if (!end.isAfter(start)) return null
    val unitSeconds = when (priceUnit) {
        PriceUnit.Hour -> Duration.ofHours(1).seconds
        PriceUnit.Day -> Duration.ofDays(1).seconds
    }
    val seconds = Duration.between(start, end).seconds
    val units = (seconds + unitSeconds - 1) / unitSeconds
    return RentalCost(units, units * price)
}
