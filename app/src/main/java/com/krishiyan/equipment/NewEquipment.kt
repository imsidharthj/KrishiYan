package com.krishiyan.equipment

import java.time.Instant

data class NewEquipment(
    val name: String,
    val category: Category,
    val brandModel: String,
    val description: String,
    val year: Int?,
    val price: Double,
    val priceUnit: PriceUnit,
    val lat: Double,
    val lng: Double,
)

data class BlockedRange(val start: Instant, val end: Instant)
