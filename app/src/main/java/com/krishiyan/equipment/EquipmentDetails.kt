package com.krishiyan.equipment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class EquipmentDetails(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val category: String,
    @SerialName("brand_model") val brandModel: String,
    val description: String,
    val year: Int? = null,
    val price: Double,
    @SerialName("price_unit") val priceUnit: String,
    @SerialName("approx_lat") val approxLat: Double,
    @SerialName("approx_lng") val approxLng: Double,
    @SerialName("is_available") val isAvailable: Boolean,
    @SerialName("owner_first_name") val ownerFirstName: String,
    @SerialName("owner_village") val ownerVillage: String,
)

/** A period when the equipment cannot be rented: blocked by the owner or already booked. */
data class BusyRange(val start: Instant, val end: Instant)
