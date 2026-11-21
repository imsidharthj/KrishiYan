package com.krishiyan.booking

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant

class BookingRepository(private val supabase: SupabaseClient) {

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
