package com.krishiyan.location

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Place(val name: String, val detail: String, val location: LatLng)

/** Village and town search using Photon, a free OpenStreetMap search made for typing suggestions. */
class PlaceSearch(private val http: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchPlaces(query: String): List<Place> {
        val body = http.get(PHOTON_URL) {
            parameter("q", query)
            parameter("limit", 8)
            parameter("bbox", INDIA_BBOX)
        }.bodyAsText()
        return json.decodeFromString<Response>(body).features.mapNotNull { it.toPlace() }.distinct()
    }

    /** The address at a point, for example after the user drops a pin. Null when nothing is known there. */
    suspend fun placeAt(location: LatLng): Place? {
        val body = http.get(PHOTON_REVERSE_URL) {
            parameter("lat", location.lat)
            parameter("lon", location.lng)
        }.bodyAsText()
        val p = json.decodeFromString<Response>(body).features.firstOrNull()?.properties ?: return null
        val name = p.locality ?: p.district ?: p.name ?: p.city ?: p.county ?: return null
        val detail = listOfNotNull(p.city ?: p.county, p.state).filter { it != name }.distinct().joinToString(", ")
        return Place(name, detail, location)
    }

    @Serializable
    private data class Response(val features: List<Feature> = emptyList())

    @Serializable
    private data class Feature(val geometry: Geometry, val properties: Properties) {
        fun toPlace(): Place? {
            val name = properties.name ?: return null
            val (lng, lat) = geometry.coordinates.takeIf { it.size == 2 } ?: return null
            val detail = listOfNotNull(properties.city, properties.county ?: properties.district, properties.state)
                .filter { it != name }
                .distinct()
                .joinToString(", ")
            return Place(name, detail, LatLng(lat, lng))
        }
    }

    @Serializable
    private data class Geometry(val coordinates: List<Double> = emptyList())

    @Serializable
    private data class Properties(
        val name: String? = null,
        val city: String? = null,
        val county: String? = null,
        val district: String? = null,
        val locality: String? = null,
        val state: String? = null,
    )

    private companion object {
        const val PHOTON_URL = "https://photon.komoot.io/api/"
        const val PHOTON_REVERSE_URL = "https://photon.komoot.io/reverse"
        const val INDIA_BBOX = "68.1,6.5,97.4,35.7"
    }
}
