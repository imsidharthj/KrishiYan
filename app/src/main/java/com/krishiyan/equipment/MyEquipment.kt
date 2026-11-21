package com.krishiyan.equipment

data class MyEquipment(
    val id: String,
    val name: String,
    val price: Double,
    val priceUnit: String,
    val isAvailable: Boolean,
    val photoUrl: String?,
)
