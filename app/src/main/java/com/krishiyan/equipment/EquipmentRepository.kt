package com.krishiyan.equipment

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import kotlin.time.Duration.Companion.hours

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

    suspend fun listNearbyEquipment(
        lat: Double,
        lng: Double,
        radiusKm: Int,
        category: Category?,
        query: String?,
    ): List<NearbyEquipment> {
        val items = supabase.postgrest.rpc(
            "nearby_equipment",
            buildJsonObject {
                put("p_lat", lat)
                put("p_lng", lng)
                put("p_radius_km", radiusKm)
                put("p_category", category?.dbValue)
                put("p_query", query?.takeIf { it.isNotBlank() })
            },
        ).decodeList<NearbyEquipment>()
        val urls = signedPhotoUrls(items.mapNotNull { it.photoPath })
        return items.map { it.copy(photoUrl = it.photoPath?.let(urls::get)) }
    }

    suspend fun getEquipmentDetails(id: String): EquipmentDetails? =
        supabase.postgrest.rpc("equipment_details", buildJsonObject { put("p_equipment_id", id) })
            .decodeList<EquipmentDetails>()
            .firstOrNull()

    /** Signed links to the equipment's photos, in the owner's order. */
    suspend fun listEquipmentPhotos(id: String): List<String> {
        val paths = supabase.from("equipment_images")
            .select { filter { eq("equipment_id", id) }; order("position", Order.ASCENDING) }
            .decodeList<PathRow>()
            .map { it.storagePath }
        val urls = signedPhotoUrls(paths)
        return paths.mapNotNull(urls::get)
    }

    suspend fun listBusyRanges(id: String): List<BusyRange> =
        supabase.postgrest.rpc("equipment_busy_ranges", buildJsonObject { put("p_equipment_id", id) })
            .decodeList<RangeRow>()
            .map { BusyRange(Instant.parse(it.startAt), Instant.parse(it.endAt)) }

    @Serializable
    private data class PathRow(@SerialName("storage_path") val storagePath: String)

    @Serializable
    private data class RangeRow(
        @SerialName("start_at") val startAt: String,
        @SerialName("end_at") val endAt: String,
    )

    private suspend fun signedPhotoUrls(paths: List<String>): Map<String, String> {
        if (paths.isEmpty()) return emptyMap()
        return supabase.storage.from(PHOTO_BUCKET)
            .createSignedUrls(PHOTO_LINK_LIFETIME, paths)
            .associate { it.path to it.signedURL }
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
        private val PHOTO_LINK_LIFETIME = 1.hours
    }
}
