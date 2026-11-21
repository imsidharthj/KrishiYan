package com.krishiyan.booking

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OwnerRequestsUiState(
    val requests: List<OwnerRequest> = emptyList(),
    val loading: Boolean = true,
    val busyIds: Set<String> = emptySet(),
    @StringRes val error: Int? = null,
    @StringRes val actionError: Int? = null,
)

class OwnerRequestsViewModel(private val bookings: BookingRepository) : ViewModel() {
    private val _state = MutableStateFlow(OwnerRequestsUiState())
    val state: StateFlow<OwnerRequestsUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val requests = bookings.listOwnerRequests()
                _state.update { it.copy(requests = requests, loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toMessage()) }
            }
        }
    }

    fun acceptBooking(id: String) = answer(id) { bookings.acceptBooking(id) }

    fun rejectBooking(id: String) = answer(id) { bookings.rejectBooking(id) }

    fun dismissActionError() = _state.update { it.copy(actionError = null) }

    private fun answer(id: String, action: suspend () -> Unit) {
        _state.update { it.copy(busyIds = it.busyIds + id, actionError = null) }
        viewModelScope.launch {
            try {
                action()
                _state.update { s -> s.copy(requests = s.requests.filterNot { it.booking.id == id }, busyIds = s.busyIds - id) }
            } catch (e: Exception) {
                _state.update { it.copy(busyIds = it.busyIds - id, actionError = e.toMessage()) }
                refresh()
            }
        }
    }
}
