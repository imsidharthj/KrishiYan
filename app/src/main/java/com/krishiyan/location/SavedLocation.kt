package com.krishiyan.location

import android.content.Context

/** The place the user searches around. Kept on the phone so Home opens straight away next time. */
class SavedLocation(context: Context) {
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

    private companion object {
        const val KEY_LAT = "lat"
        const val KEY_LNG = "lng"
        const val KEY_NAME = "name"
    }
}
