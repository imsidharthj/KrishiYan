package com.krishiyan.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

data class LatLng(val lat: Double, val lng: Double)

class LocationProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Needs location permission. Returns null when the phone cannot get a fix (for example GPS is off). */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): LatLng? {
        val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            ?: client.lastLocation.await()
        return location?.let { LatLng(it.latitude, it.longitude) }
    }
}
