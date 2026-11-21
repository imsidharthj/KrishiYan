package com.krishiyan.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.R
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val name: String = "",
    val phone: String = "",
    val village: String = "",
    val saving: Boolean = false,
    @StringRes val error: Int? = null,
)

class ProfileSetupViewModel(
    private val profiles: ProfileRepository,
    private val onProfileCreated: () -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileSetupUiState())
    val state: StateFlow<ProfileSetupUiState> = _state

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value.filter(Char::isDigit).take(12), error = null) }
    fun onVillageChange(value: String) = _state.update { it.copy(village = value, error = null) }

    fun createProfile() {
        val s = _state.value
        val phone = s.phone.removePrefix("91").takeIf { s.phone.length == 12 } ?: s.phone
        val error = when {
            s.name.isBlank() -> R.string.error_name_required
            phone.length != 10 -> R.string.error_phone_invalid
            s.village.isBlank() -> R.string.error_village_required
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                profiles.createProfile(s.name.trim(), phone, s.village.trim())
                onProfileCreated()
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.toMessage()) }
            }
        }
    }
}
