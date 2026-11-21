package com.krishiyan.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.R
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val code: String = "",
    val codeSent: Boolean = false,
    val loading: Boolean = false,
    @StringRes val error: Int? = null,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmailChange(email: String) = _state.update { it.copy(email = email.trim(), error = null) }

    fun onCodeChange(code: String) =
        _state.update { it.copy(code = code.filter(Char::isDigit).take(MAX_CODE_LENGTH), error = null) }

    fun sendCode() {
        val email = _state.value.email
        if (!EMAIL_PATTERN.matches(email)) {
            _state.update { it.copy(error = R.string.error_invalid_email) }
            return
        }
        run { auth.sendLoginCode(email); _state.update { it.copy(codeSent = true, code = "") } }
    }

    fun verifyCode() {
        val s = _state.value
        if (s.code.length !in MIN_CODE_LENGTH..MAX_CODE_LENGTH) {
            _state.update { it.copy(error = R.string.error_wrong_code) }
            return
        }
        run { auth.verifyLoginCode(s.email, s.code) }
    }

    fun changeEmail() = _state.update { it.copy(codeSent = false, code = "", error = null) }

    private fun run(block: suspend () -> Unit) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toMessage()) }
            }
        }
    }

    private companion object {
        const val MIN_CODE_LENGTH = 6
        const val MAX_CODE_LENGTH = 8
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
