package com.krishiyan.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.booking.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(val pendingRequests: Int = 0)

class ProfileViewModel(private val bookings: BookingRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    fun refresh() {
        viewModelScope.launch {
            val count = try { bookings.countOwnerRequests() } catch (e: Exception) { _state.value.pendingRequests }
            _state.value = ProfileUiState(pendingRequests = count)
        }
    }
}
