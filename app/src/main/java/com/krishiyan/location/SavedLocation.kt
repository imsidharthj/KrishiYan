package com.krishiyan.location

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** The place the user searches around. Kept on the phone so Home opens straight away next time. */
class SavedLocation(private val context: Context) {
    private val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)

    fun load(): LatLng? {
        if (!prefs.contains(KEY_LAT)) return null
        return LatLng(prefs.getFloat(KEY_LAT, 0f).toDouble(), prefs.getFloat(KEY_LNG, 0f).toDouble())
    }

    fun loadName(): String? = prefs.getString(KEY_NAME, null)

    fun save(location: LatLng, name: String?) {
        prefs.edit()
            .putFloat(KEY_LAT, location.lat.toFloat())
            .putFloat(KEY_LNG, location.lng.toFloat())
            .putString(KEY_NAME, name)
            .apply()
    }

    /** A short place name such as a town, or null when the phone cannot look it up. */
    @Suppress("DEPRECATION")
    suspend fun placeName(location: LatLng): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            Geocoder(context, Locale.getDefault()).getFromLocation(location.lat, location.lng, 1)
                ?.firstOrNull()
                ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val KEY_LAT = "lat"
        const val KEY_LNG = "lng"
        const val KEY_NAME = "name"
    }
}
