package com.krishiyan.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.booking.BookingRepository
import com.krishiyan.booking.BookingStatus
import com.krishiyan.booking.OwnerBooking
import com.krishiyan.booking.RentalTab
import com.krishiyan.booking.rentalTab
import com.krishiyan.equipment.EquipmentRepository
import com.krishiyan.equipment.MyEquipment
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class ProfileUiState(
    val profile: Profile? = null,
    val equipment: List<MyEquipment> = emptyList(),
    val ownerBookings: List<OwnerBooking> = emptyList(),
    val now: Instant = Instant.now(),
    val loading: Boolean = true,
    val updatingIds: Set<String> = emptySet(),
    @StringRes val error: Int? = null,
) {
    private fun tabOf(b: OwnerBooking) = rentalTab(b.booking.status, b.booking.start, b.booking.end, now)

    val pendingRequests: Int get() = ownerBookings.count { tabOf(it) == RentalTab.Pending }
    val upcomingRentals: Int get() = ownerBookings.count { tabOf(it) == RentalTab.Confirmed }
    val activeRentals: Int get() = ownerBookings.count { tabOf(it) == RentalTab.Active }

    /** Finished, rejected and cancelled bookings on my equipment. Unanswered old requests are left out. */
    val history: List<OwnerBooking>
        get() = ownerBookings.filter { tabOf(it) == RentalTab.Past && it.booking.status != BookingStatus.Pending }
}

class ProfileViewModel(
    private val profiles: ProfileRepository,
    private val equipmentRepository: EquipmentRepository,
    private val bookings: BookingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    fun refresh() {
        _state.update { it.copy(loading = true, error = null, now = Instant.now()) }
        viewModelScope.launch {
            try {
                val profile = async { profiles.getMyProfile() }
                val equipment = async { equipmentRepository.listMyEquipment() }
                val ownerBookings = bookings.listOwnerBookings()
                _state.update {
                    it.copy(profile = profile.await(), equipment = equipment.await(), ownerBookings = ownerBookings, loading = false)
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toMessage()) }
            }
        }
    }

    fun markEquipmentUnavailable(id: String) = changeAvailability(id, false) { equipmentRepository.markEquipmentUnavailable(id) }

    fun markEquipmentAvailable(id: String) = changeAvailability(id, true) { equipmentRepository.markEquipmentAvailable(id) }

    private fun changeAvailability(id: String, available: Boolean, action: suspend () -> Unit) {
        _state.update { it.copy(updatingIds = it.updatingIds + id) }
        viewModelScope.launch {
            try {
                action()
                _state.update { s ->
                    s.copy(equipment = s.equipment.map { if (it.id == id) it.copy(isAvailable = available) else it })
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toMessage()) }
            }
            _state.update { it.copy(updatingIds = it.updatingIds - id) }
        }
    }
}
