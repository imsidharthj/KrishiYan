package com.krishiyan.equipment

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.R
import com.krishiyan.location.LatLng
import com.krishiyan.location.LocationProvider
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class AddEquipmentUiState(
    val photos: List<Uri> = emptyList(),
    val name: String = "",
    val category: Category? = null,
    val brandModel: String = "",
    val description: String = "",
    val year: String = "",
    val price: String = "",
    val priceUnit: PriceUnit = PriceUnit.Day,
    val location: LatLng? = null,
    val findingLocation: Boolean = false,
    val availableNow: Boolean = true,
    val blocked: List<BlockedRange> = emptyList(),
    val publishing: Boolean = false,
    val published: Boolean = false,
    @StringRes val error: Int? = null,
)

class AddEquipmentViewModel(
    private val app: Application,
    private val equipment: EquipmentRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AddEquipmentUiState())
    val state: StateFlow<AddEquipmentUiState> = _state

    fun addPhotos(uris: List<Uri>) = _state.update { it.copy(photos = (it.photos + uris).distinct().take(MAX_PHOTOS), error = null) }
    fun removePhoto(uri: Uri) = _state.update { it.copy(photos = it.photos - uri) }
    fun onNameChange(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onCategoryChange(v: Category) = _state.update { it.copy(category = v, error = null) }
    fun onBrandModelChange(v: String) = _state.update { it.copy(brandModel = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onYearChange(v: String) = _state.update { it.copy(year = v.filter(Char::isDigit).take(4)) }
    fun onPriceChange(v: String) = _state.update { it.copy(price = v.filter { c -> c.isDigit() || c == '.' }.take(8), error = null) }
    fun onPriceUnitChange(v: PriceUnit) = _state.update { it.copy(priceUnit = v) }
    fun onLocationPicked(v: LatLng) = _state.update { it.copy(location = v, error = null) }
    fun onAvailableNowChange(v: Boolean) = _state.update { it.copy(availableNow = v) }

    fun useCurrentLocation() {
        _state.update { it.copy(findingLocation = true, error = null) }
        viewModelScope.launch {
            val location = try { locationProvider.currentLocation() } catch (e: Exception) { null }
            _state.update {
                if (location != null) it.copy(findingLocation = false, location = location)
                else it.copy(findingLocation = false, error = R.string.error_location_unavailable)
            }
        }
    }

    fun onLocationDenied() = _state.update { it.copy(error = R.string.error_location_permission) }

    /** The date picker returns UTC midnights. A blocked range covers whole local days, end day included. */
    fun addBlockedDays(startUtcMillis: Long, endUtcMillis: Long) {
        val zone = ZoneId.systemDefault()
        val startDay = Instant.ofEpochMilli(startUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val endDay = Instant.ofEpochMilli(endUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val range = BlockedRange(startDay.atStartOfDay(zone).toInstant(), endDay.plusDays(1).atStartOfDay(zone).toInstant())
        _state.update { it.copy(blocked = (it.blocked + range).sortedBy(BlockedRange::start)) }
    }

    fun removeBlocked(range: BlockedRange) = _state.update { it.copy(blocked = it.blocked - range) }

    fun publishEquipment() {
        val s = _state.value
        val price = s.price.toDoubleOrNull()
        val year = s.year.toIntOrNull()
        val error = when {
            s.photos.isEmpty() -> R.string.error_photo_required
            s.name.isBlank() -> R.string.error_equipment_name_required
            s.category == null -> R.string.error_category_required
            price == null || price <= 0 -> R.string.error_price_required
            s.year.isNotEmpty() && (year == null || year < 1950 || year > LocalDate.now().year) -> R.string.error_year_invalid
            s.location == null -> R.string.error_location_required
            !s.availableNow && s.blocked.isEmpty() -> R.string.error_blocked_dates_required
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        val newEquipment = NewEquipment(
            name = s.name.trim(),
            category = s.category!!,
            brandModel = s.brandModel.trim(),
            description = s.description.trim(),
            year = year,
            price = price!!,
            priceUnit = s.priceUnit,
            lat = s.location!!.lat,
            lng = s.location.lng,
        )
        _state.update { it.copy(publishing = true, error = null) }
        viewModelScope.launch {
            try {
                val photos = withContext(Dispatchers.Default) { s.photos.map { compressPhoto(app, it) } }
                equipment.publishEquipment(newEquipment, photos, if (s.availableNow) emptyList() else s.blocked)
                _state.update { it.copy(publishing = false, published = true) }
            } catch (e: Exception) {
                _state.update { it.copy(publishing = false, error = e.toMessage()) }
            }
        }
    }

    companion object {
        const val MAX_PHOTOS = 5
    }
}
