package com.krishiyan.booking

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class MyRentalsUiState(
    val rentals: List<MyRental> = emptyList(),
    val contacts: Map<String, BookingContact> = emptyMap(),
    val tab: RentalTab = RentalTab.Pending,
    val loading: Boolean = true,
    val cancellingIds: Set<String> = emptySet(),
    val now: Instant = Instant.now(),
    @StringRes val error: Int? = null,
    @StringRes val actionError: Int? = null,
) {
    fun inTab(tab: RentalTab) = rentals.filter { rentalTab(it.booking.status, it.booking.start, it.booking.end, now) == tab }
}

class MyRentalsViewModel(private val bookings: BookingRepository) : ViewModel() {
    private val _state = MutableStateFlow(MyRentalsUiState())
    val state: StateFlow<MyRentalsUiState> = _state

    fun selectTab(tab: RentalTab) = _state.update { it.copy(tab = tab) }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null, now = Instant.now()) }
        viewModelScope.launch {
            try {
                val rentals = bookings.listMyRentals()
                val accepted = rentals.filter { it.booking.status == BookingStatus.Accepted }
                val contacts = accepted.map { r -> async { r.booking.id to bookings.getBookingContact(r.booking.id) } }
                    .awaitAll()
                    .mapNotNull { (id, c) -> c?.let { id to it } }
                    .toMap()
                _state.update { it.copy(rentals = rentals, contacts = contacts, loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toMessage()) }
            }
        }
    }

    fun cancelBooking(id: String) {
        _state.update { it.copy(cancellingIds = it.cancellingIds + id, actionError = null) }
        viewModelScope.launch {
            try {
                bookings.cancelBooking(id)
            } catch (e: Exception) {
                _state.update { it.copy(actionError = e.toMessage()) }
            }
            _state.update { it.copy(cancellingIds = it.cancellingIds - id) }
            refresh()
        }
    }

    fun dismissActionError() = _state.update { it.copy(actionError = null) }
}
