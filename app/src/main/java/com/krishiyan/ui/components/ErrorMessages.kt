package com.krishiyan.ui.components

import androidx.annotation.StringRes
import com.krishiyan.R
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import java.io.IOException

@StringRes
fun Throwable.toMessage(): Int = when (this) {
    is HttpRequestException, is IOException, is ConnectTimeoutException -> R.string.error_no_internet
    is RestException if (message ?: "").contains("dates_unavailable") || (message ?: "").contains("dates_already_booked") ->
        R.string.error_dates_unavailable
    is RestException if (message ?: "").contains("start_in_past") -> R.string.error_start_in_past
    is AuthRestException -> when {
        statusCode == 429 -> R.string.error_too_many_tries
        error.contains("otp", ignoreCase = true) || statusCode == 403 -> R.string.error_wrong_code
        else -> R.string.error_generic
    }
    else -> R.string.error_generic
}
