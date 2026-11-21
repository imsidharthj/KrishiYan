package com.krishiyan.profile

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val phone: String,
    val village: String,
)
