package com.krishiyan.booking

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant

class BookingRepository(private val supabase: SupabaseClient) {

    /** Pending requests on equipment I own, oldest first so nobody waits too long. */
    suspend fun listOwnerRequests(): List<OwnerRequest> = coroutineScope {
        val me = supabase.auth.currentUserOrNull()?.id ?: return@coroutineScope emptyList()
        val rows = supabase.from("bookings")
            .select(Columns.raw("*, equipment(name)")) {
                filter {
                    eq("owner_id", me)
                    eq("status", BookingStatus.Pending.dbValue)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<BookingWithEquipment>()
        rows.map { row ->
            async {
                val renter = supabase.postgrest.rpc("booking_renter", buildJsonObject { put("p_booking_id", row.id) })
                    .decodeList<RenterRow>().firstOrNull()
                OwnerRequest(row.toBooking(), row.equipment.name, renter?.name.orEmpty(), renter?.village.orEmpty())
            }
        }.awaitAll()
    }

    suspend fun countOwnerRequests(): Int {
        val me = supabase.auth.currentUserOrNull()?.id ?: return 0
        return supabase.from("bookings")
            .select(Columns.raw("id")) {
                filter {
                    eq("owner_id", me)
                    eq("status", BookingStatus.Pending.dbValue)
                }
            }
            .decodeList<IdRow>().size
    }

    suspend fun acceptBooking(bookingId: String) {
        supabase.postgrest.rpc("accept_booking", buildJsonObject { put("p_booking_id", bookingId) })
    }

    suspend fun rejectBooking(bookingId: String) {
        supabase.postgrest.rpc("reject_booking", buildJsonObject { put("p_booking_id", bookingId) })
    }

    /** Creates a pending booking. The database checks dates and works out the cost. Returns the booking id. */
    suspend fun requestBooking(equipmentId: String, start: Instant, end: Instant, message: String): String =
        supabase.postgrest.rpc(
            "request_booking",
            buildJsonObject {
                put("p_equipment_id", equipmentId)
                put("p_start", start.toString())
                put("p_end", end.toString())
                put("p_message", message)
            },
        ).decodeAs<kotlinx.serialization.json.JsonElement>().jsonPrimitive.content
}

@Serializable
private data class BookingWithEquipment(
    val id: String,
    @SerialName("equipment_id") val equipmentId: String,
    @SerialName("renter_id") val renterId: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("estimated_cost") val estimatedCost: Double,
    val message: String,
    val status: String,
    val equipment: NameRow,
) {
    fun toBooking() = Booking(id, equipmentId, renterId, ownerId, startAt, endAt, estimatedCost, message, status)
}

@Serializable
private data class NameRow(val name: String)

@Serializable
private data class IdRow(val id: String)

@Serializable
private data class RenterRow(
    @SerialName("renter_name") val name: String,
    @SerialName("renter_village") val village: String,
    @SerialName("renter_phone") val phone: String,
)
