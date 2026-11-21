package com.krishiyan.equipment

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.R
import com.krishiyan.auth.AuthRepository
import com.krishiyan.location.LatLng
import com.krishiyan.location.SavedLocation
import com.krishiyan.location.displayKm
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EquipmentDetailsUiState(
    val loading: Boolean = true,
    val details: EquipmentDetails? = null,
    val photos: List<String> = emptyList(),
    val busy: List<BusyRange> = emptyList(),
    val distanceKm: Int? = null,
    val isMine: Boolean = false,
    @StringRes val error: Int? = null,
)

class EquipmentDetailsViewModel(
    private val equipmentId: String,
    private val equipment: EquipmentRepository,
    private val auth: AuthRepository,
    private val savedLocation: SavedLocation,
) : ViewModel() {
    private val _state = MutableStateFlow(EquipmentDetailsUiState())
    val state: StateFlow<EquipmentDetailsUiState> = _state

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val photos = async { equipment.listEquipmentPhotos(equipmentId) }
                val busy = async { equipment.listBusyRanges(equipmentId) }
                val details = equipment.getEquipmentDetails(equipmentId)
                if (details == null) {
                    _state.update { it.copy(loading = false, error = R.string.error_equipment_gone) }
                    return@launch
                }
                val here = savedLocation.load()
                _state.update {
                    it.copy(
                        loading = false,
                        details = details,
                        photos = photos.await(),
                        busy = busy.await(),
                        distanceKm = here?.let { displayKm(it, LatLng(details.approxLat, details.approxLng)) },
                        isMine = details.ownerId == auth.currentUserId(),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toMessage()) }
            }
        }
    }
}
