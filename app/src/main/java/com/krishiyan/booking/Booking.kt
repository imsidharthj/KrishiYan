package com.krishiyan.booking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

enum class BookingStatus(val dbValue: String) {
    Pending("pending"),
    Accepted("accepted"),
    Rejected("rejected"),
    Cancelled("cancelled"),
    Completed("completed");

    companion object {
        fun fromDb(value: String) = entries.first { it.dbValue == value }
    }
}

/** The same rules the database functions enforce, so the app only offers actions that will work. */
fun canAcceptOrReject(status: BookingStatus) = status == BookingStatus.Pending

fun canCancel(status: BookingStatus, start: Instant, now: Instant): Boolean =
    status == BookingStatus.Pending || (status == BookingStatus.Accepted && start.isAfter(now))

@Serializable
data class Booking(
    val id: String,
    @SerialName("equipment_id") val equipmentId: String,
    @SerialName("renter_id") val renterId: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("estimated_cost") val estimatedCost: Double,
    val message: String,
    @SerialName("status") val statusValue: String,
) {
    val status: BookingStatus get() = BookingStatus.fromDb(statusValue)
    val start: Instant get() = Instant.parse(startAt)
    val end: Instant get() = Instant.parse(endAt)
}

/** A booking on my equipment, with what the owner needs to decide. */
data class OwnerRequest(
    val booking: Booking,
    val equipmentName: String,
    val renterName: String,
    val renterVillage: String,
)

/** A booking I made as a renter. */
data class MyRental(val booking: Booking, val equipmentName: String, val photoUrl: String?)

@Serializable
data class BookingContact(
    val lat: Double,
    val lng: Double,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("owner_phone") val ownerPhone: String,
)

/** A booking on my equipment, as the owner sees it. */
data class OwnerBooking(val booking: Booking, val equipmentName: String)
