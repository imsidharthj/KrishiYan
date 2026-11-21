package com.krishiyan

import android.content.Context
import com.krishiyan.auth.AuthRepository
import com.krishiyan.booking.BookingRepository
import com.krishiyan.equipment.EquipmentRepository
import com.krishiyan.location.LocationProvider
import com.krishiyan.location.PlaceSearch
import com.krishiyan.location.SavedLocation
import com.krishiyan.profile.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

class AppContainer(context: Context) {
    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    val authRepository = AuthRepository(supabase)
    val profileRepository = ProfileRepository(supabase)
    val equipmentRepository = EquipmentRepository(supabase)
    val bookingRepository = BookingRepository(supabase)
    val locationProvider = LocationProvider(context)
    val savedLocation = SavedLocation(context)
    val placeSearch = PlaceSearch(HttpClient(Android))
}
