package com.krishiyan.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.R
import com.krishiyan.equipment.Category
import com.krishiyan.equipment.EquipmentRepository
import com.krishiyan.equipment.NearbyEquipment
import com.krishiyan.location.LatLng
import com.krishiyan.location.LocationProvider
import com.krishiyan.location.Place
import com.krishiyan.location.PlaceSearch
import com.krishiyan.location.SavedLocation
import com.krishiyan.ui.components.toMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val location: LatLng? = null,
    val placeName: String? = null,
    val findingLocation: Boolean = false,
    val query: String = "",
    val category: Category? = null,
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    val items: List<NearbyEquipment> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    @StringRes val error: Int? = null,
) {
    val hasFilters: Boolean get() = query.isNotBlank() || category != null || radiusKm != DEFAULT_RADIUS_KM
}

val RADIUS_OPTIONS_KM = listOf(5, 10, 25, 50)
const val DEFAULT_RADIUS_KM = 25

class HomeViewModel(
    private val equipment: EquipmentRepository,
    private val locationProvider: LocationProvider,
    private val savedLocation: SavedLocation,
    private val placeSearch: PlaceSearch,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState(location = savedLocation.load(), placeName = savedLocation.loadName()))
    val state: StateFlow<HomeUiState> = _state
    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun useCurrentLocation() {
        _state.update { it.copy(findingLocation = true, error = null) }
        viewModelScope.launch {
            val location = try { locationProvider.currentLocation() } catch (e: Exception) { null }
            _state.update { it.copy(findingLocation = false) }
            if (location != null) onLocationChosen(location)
            else _state.update { it.copy(error = R.string.error_location_unavailable) }
        }
    }

    fun onLocationDenied() = _state.update { it.copy(error = R.string.error_location_permission) }

    fun onPlaceChosen(place: Place) {
        savedLocation.save(place.location, place.name)
        _state.update { it.copy(location = place.location, placeName = place.name, error = null) }
        refresh()
    }

    /** From GPS, where we have no name yet, so we look one up. */
    fun onLocationChosen(location: LatLng) {
        savedLocation.save(location, null)
        _state.update { it.copy(location = location, placeName = null, error = null) }
        viewModelScope.launch {
            val name = try { placeSearch.placeAt(location)?.name } catch (e: Exception) { null }
            if (_state.value.location == location) {
                savedLocation.save(location, name)
                _state.update { it.copy(placeName = name) }
            }
        }
        refresh()
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            load()
        }
    }

    fun onCategoryChange(category: Category?) {
        _state.update { it.copy(category = category) }
        refresh()
    }

    fun onRadiusChange(radiusKm: Int) {
        _state.update { it.copy(radiusKm = radiusKm) }
        refresh()
    }

    fun clearFilters() {
        _state.update { it.copy(query = "", category = null, radiusKm = DEFAULT_RADIUS_KM) }
        refresh()
    }

    fun refresh() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val s = _state.value
        val location = s.location ?: return
        _state.update { it.copy(loading = true, error = null) }
        try {
            val items = equipment.listNearbyEquipment(location.lat, location.lng, s.radiusKm, s.category, s.query.trim())
            _state.update { it.copy(items = items, loading = false, loaded = true) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _state.update { it.copy(loading = false, error = e.toMessage()) }
        }
    }

    private companion object {
        const val SEARCH_DELAY_MS = 400L
    }
}
