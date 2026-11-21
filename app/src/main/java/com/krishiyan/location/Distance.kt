package com.krishiyan.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0

/** Straight-line distance between two points (haversine), the same formula the database uses. */
fun distanceKm(a: LatLng, b: LatLng): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(h))
}

/** Whole kilometres for display, never less than 1 so a nearby owner's exact spot is not hinted at. */
fun displayKm(a: LatLng, b: LatLng): Int = maxOf(1, Math.round(distanceKm(a, b)).toInt())
