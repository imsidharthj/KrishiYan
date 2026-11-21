package com.krishiyan.booking

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.R
import com.krishiyan.equipment.BusyRange
import com.krishiyan.equipment.EquipmentDetails
import com.krishiyan.equipment.EquipmentRepository
import com.krishiyan.equipment.PriceUnit
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class RequestBookingUiState(
    val loading: Boolean = true,
    val details: EquipmentDetails? = null,
    val busy: List<BusyRange> = emptyList(),
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null,
    val message: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    @StringRes val error: Int? = null,
) {
    val priceUnit: PriceUnit get() = if (details?.priceUnit == PriceUnit.Hour.dbValue) PriceUnit.Hour else PriceUnit.Day

    val cost: RentalCost?
        get() {
            val d = details ?: return null
            val s = start ?: return null
            val e = end ?: return null
            return estimateRentalCost(d.price, priceUnit, s.toInstant(), e.toInstant())
        }

    val clashes: Boolean
        get() = start != null && end != null && overlapsBusy(start.toInstant(), end.toInstant(), busy)
}

private fun LocalDateTime.toInstant() = atZone(ZoneId.systemDefault()).toInstant()

class RequestBookingViewModel(
    private val equipmentId: String,
    private val equipment: EquipmentRepository,
    private val bookings: BookingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RequestBookingUiState())
    val state: StateFlow<RequestBookingUiState> = _state

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val busy = async { equipment.listBusyRanges(equipmentId) }
                val details = equipment.getEquipmentDetails(equipmentId)
                _state.update {
                    it.copy(
                        loading = false,
                        details = details,
                        busy = busy.await(),
                        error = if (details == null) R.string.error_equipment_gone else null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toMessage()) }
            }
        }
    }

    /** Picking a start fills in a sensible end: one hour or one day later, whichever the price is per. */
    fun onStartDate(date: LocalDate) = setStart(LocalDateTime.of(date, _state.value.start?.toLocalTime() ?: defaultStartTime(date)))

    /** 8 am, or the next full hour when 8 am today has already passed. */
    private fun defaultStartTime(date: LocalDate): LocalTime {
        val now = LocalDateTime.now()
        if (date != now.toLocalDate() || now.toLocalTime().isBefore(DEFAULT_START)) return DEFAULT_START
        return now.toLocalTime().withMinute(0).withSecond(0).withNano(0).plusHours(1)
    }
    fun onStartTime(time: LocalTime) = _state.value.start?.let { setStart(LocalDateTime.of(it.toLocalDate(), time)) }

    fun onEndDate(date: LocalDate) =
        _state.update { it.copy(end = LocalDateTime.of(date, it.end?.toLocalTime() ?: it.start?.toLocalTime() ?: DEFAULT_START), error = null) }
    fun onEndTime(time: LocalTime) = _state.update { s -> s.end?.let { s.copy(end = LocalDateTime.of(it.toLocalDate(), time), error = null) } ?: s }

    fun onMessageChange(v: String) = _state.update { it.copy(message = v.take(MAX_MESSAGE)) }

    private fun setStart(start: LocalDateTime) = _state.update {
        val end = it.end?.takeIf { e -> e.isAfter(start) }
            ?: if (it.priceUnit == PriceUnit.Hour) start.plusHours(1) else start.plusDays(1)
        it.copy(start = start, end = end, error = null)
    }

    fun requestBooking() {
        val s = _state.value
        val error = when {
            s.start == null || s.end == null -> R.string.error_pick_dates
            !s.end.isAfter(s.start) -> R.string.error_end_before_start
            s.start.toInstant().isBefore(java.time.Instant.now().minusSeconds(3600)) -> R.string.error_start_in_past
            s.clashes -> R.string.error_dates_unavailable
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try {
                bookings.requestBooking(equipmentId, s.start!!.toInstant(), s.end!!.toInstant(), s.message.trim())
                _state.update { it.copy(sending = false, sent = true) }
            } catch (e: Exception) {
                _state.update { it.copy(sending = false, error = e.toMessage()) }
            }
        }
    }

    private companion object {
        val DEFAULT_START: LocalTime = LocalTime.of(8, 0)
        const val MAX_MESSAGE = 300
    }
}
