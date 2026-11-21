package com.krishiyan.equipment

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class EquipmentRepository(private val supabase: SupabaseClient) {

    /** Creates the listing, uploads its photos and saves blocked dates. Returns the new equipment id. */
    suspend fun publishEquipment(equipment: NewEquipment, photos: List<ByteArray>, blocked: List<BlockedRange>): String {
        val ownerId = requireNotNull(supabase.auth.currentUserOrNull()?.id)
        val id = supabase.from("equipment")
            .insert(EquipmentRow.from(ownerId, equipment)) { select() }
            .decodeSingle<IdRow>().id

        try {
            val images = photos.mapIndexed { position, bytes ->
                val path = "$ownerId/$id/$position.jpg"
                supabase.storage.from(PHOTO_BUCKET).upload(path, bytes) {
                    upsert = true
                    contentType = ContentType.Image.JPEG
                }
                ImageRow(id, path, position)
            }
            supabase.from("equipment_images").insert(images)
            if (blocked.isNotEmpty()) {
                supabase.from("equipment_blocked_dates")
                    .insert(blocked.map { BlockedRow(id, it.start.toString(), it.end.toString()) })
            }
        } catch (e: Exception) {
            supabase.from("equipment").delete { filter { eq("id", id) } }
            throw e
        }
        return id
    }

    @Serializable
    private data class EquipmentRow(
        @SerialName("owner_id") val ownerId: String,
        val name: String,
        val category: String,
        @SerialName("brand_model") val brandModel: String,
        val description: String,
        val year: Int?,
        val price: Double,
        @SerialName("price_unit") val priceUnit: String,
        val lat: Double,
        val lng: Double,
    ) {
        companion object {
            fun from(ownerId: String, e: NewEquipment) = EquipmentRow(
                ownerId, e.name, e.category.dbValue, e.brandModel, e.description,
                e.year, e.price, e.priceUnit.dbValue, e.lat, e.lng,
            )
        }
    }

    @Serializable
    private data class IdRow(val id: String)

    @Serializable
    private data class ImageRow(
        @SerialName("equipment_id") val equipmentId: String,
        @SerialName("storage_path") val storagePath: String,
        val position: Int,
    )

    @Serializable
    private data class BlockedRow(
        @SerialName("equipment_id") val equipmentId: String,
        @SerialName("start_at") val startAt: String,
        @SerialName("end_at") val endAt: String,
    )

    companion object {
        const val PHOTO_BUCKET = "equipment-photos"
    }
}
