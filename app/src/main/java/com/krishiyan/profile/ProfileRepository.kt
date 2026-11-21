package com.krishiyan.profile

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class ProfileRepository(private val supabase: SupabaseClient) {
    suspend fun getMyProfile(): Profile? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        return supabase.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Profile>()
    }

    suspend fun createProfile(name: String, phone: String, village: String) {
        val userId = requireNotNull(supabase.auth.currentUserOrNull()?.id)
        supabase.from("profiles").insert(Profile(userId, name, phone, village))
    }
}
