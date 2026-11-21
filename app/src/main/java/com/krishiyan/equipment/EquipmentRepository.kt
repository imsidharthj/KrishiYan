package com.krishiyan.equipment

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
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

    /** My listings, newest first, with the first photo. Owners can read their own rows directly. */
    suspend fun listMyEquipment(): List<MyEquipment> {
        val me = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        val rows = supabase.from("equipment")
            .select(Columns.raw("id, name, price, price_unit, is_available, equipment_images(storage_path, position)")) {
                filter { eq("owner_id", me) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<MyEquipmentRow>()
        val firstPaths = rows.mapNotNull { row -> row.images.minByOrNull { it.position }?.storagePath }
        val urls = signedPhotoUrls(firstPaths)
        return rows.map { row ->
            MyEquipment(
                id = row.id,
                name = row.name,
                price = row.price,
                priceUnit = row.priceUnit,
                isAvailable = row.isAvailable,
                photoUrl = row.images.minByOrNull { it.position }?.storagePath?.let(urls::get),
            )
        }
    }

    /** One of my listings with everything the edit form needs, including the exact location. */
    suspend fun getMyEquipment(id: String): Pair<NewEquipment, List<BlockedRange>>? {
        val row = supabase.from("equipment")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<EquipmentRow>() ?: return null
        val blocked = supabase.from("equipment_blocked_dates")
            .select { filter { eq("equipment_id", id) } }
            .decodeList<BlockedRow>()
            .map { BlockedRange(Instant.parse(it.startAt), Instant.parse(it.endAt)) }
        return row.toNewEquipment() to blocked
    }

    /** Saves edited details and replaces the unavailable days. Photos are left as they are. */
    suspend fun updateEquipment(id: String, equipment: NewEquipment, blocked: List<BlockedRange>) {
        val ownerId = requireNotNull(supabase.auth.currentUserOrNull()?.id)
        supabase.from("equipment").update(EquipmentRow.from(ownerId, equipment)) { filter { eq("id", id) } }
        supabase.from("equipment_blocked_dates").delete { filter { eq("equipment_id", id) } }
        if (blocked.isNotEmpty()) {
            supabase.from("equipment_blocked_dates")
                .insert(blocked.map { BlockedRow(id, it.start.toString(), it.end.toString()) })
        }
    }

    suspend fun markEquipmentUnavailable(id: String) = setAvailable(id, false)

    suspend fun markEquipmentAvailable(id: String) = setAvailable(id, true)

    private suspend fun setAvailable(id: String, available: Boolean) {
        supabase.from("equipment").update(AvailableRow(available)) { filter { eq("id", id) } }
    }

    @Serializable
    private data class AvailableRow(@SerialName("is_available") val isAvailable: Boolean)

    @Serializable
    private data class MyEquipmentRow(
        val id: String,
        val name: String,
        val price: Double,
        @SerialName("price_unit") val priceUnit: String,
        @SerialName("is_available") val isAvailable: Boolean,
        @SerialName("equipment_images") val images: List<ImageRef> = emptyList(),
    )

    @Serializable
    private data class ImageRef(@SerialName("storage_path") val storagePath: String, val position: Int)

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
        fun toNewEquipment() = NewEquipment(
            name, Category.fromDb(category), brandModel, description, year, price,
            if (priceUnit == PriceUnit.Hour.dbValue) PriceUnit.Hour else PriceUnit.Day, lat, lng,
        )

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
