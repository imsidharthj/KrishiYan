package com.krishiyan.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(private val supabase: SupabaseClient) {
    val sessionStatus: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun sendLoginCode(email: String) {
        supabase.auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    suspend fun verifyLoginCode(email: String, code: String) {
        supabase.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}
