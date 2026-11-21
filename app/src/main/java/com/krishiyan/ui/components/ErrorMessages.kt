package com.krishiyan.ui.components

import androidx.annotation.StringRes
import com.krishiyan.R
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import java.io.IOException

@StringRes
fun Throwable.toMessage(): Int = when (this) {
    is HttpRequestException, is IOException, is ConnectTimeoutException -> R.string.error_no_internet
    is AuthRestException -> when {
        statusCode == 429 -> R.string.error_too_many_tries
        error.contains("otp", ignoreCase = true) || statusCode == 403 -> R.string.error_wrong_code
        else -> R.string.error_generic
    }
    else -> R.string.error_generic
}
