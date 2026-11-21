package com.krishiyan.equipment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyEquipment(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    @SerialName("price_unit") val priceUnit: String,
    @SerialName("approx_lat") val approxLat: Double,
    @SerialName("approx_lng") val approxLng: Double,
    @SerialName("distance_km") val distanceKm: Int,
    @SerialName("available_now") val availableNow: Boolean,
    @SerialName("photo_path") val photoPath: String? = null,
    val photoUrl: String? = null,
)
