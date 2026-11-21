package com.krishiyan.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krishiyan.R
import com.krishiyan.equipment.Category
import com.krishiyan.location.ChooseLocationDialog
import com.krishiyan.location.LocationPickerDialog
import com.krishiyan.location.PlaceSearch
import com.krishiyan.ui.components.BigButton
import com.krishiyan.ui.components.EquipmentCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, placeSearch: PlaceSearch, onOpenEquipment: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var choosingLocation by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.useCurrentLocation() else viewModel.onLocationDenied()
    }
    val askForGps = {
        permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (state.location == null) {
            ChooseLocationPrompt(state, onUseGps = askForGps, onSearch = { choosingLocation = true })
        } else {
            Filters(state, viewModel, onLocationClick = { choosingLocation = true })
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth()) else Box(Modifier.heightIn(min = 4.dp))
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) {
                when {
                    state.error != null && state.items.isEmpty() ->
                        Message(state.error!!, actionText = R.string.try_again, onAction = viewModel::refresh)
                    state.loaded && state.items.isEmpty() -> Message(
                        R.string.no_equipment_nearby,
                        hint = R.string.no_equipment_nearby_hint.takeIf { state.radiusKm < RADIUS_OPTIONS_KM.last() },
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            EquipmentCard(item, onClick = { onOpenEquipment(item.id) })
                        }
                    }
                }
            }
        }
    }

    if (choosingLocation) {
        ChooseLocationDialog(
            placeSearch = placeSearch,
            onPlace = { viewModel.onPlaceChosen(it); choosingLocation = false },
            onUseGps = { choosingLocation = false; askForGps() },
            onPickOnMap = { choosingLocation = false; showMapPicker = true },
            onDismiss = { choosingLocation = false },
        )
    }
    if (showMapPicker) {
        LocationPickerDialog(
            start = state.location,
            onPicked = { viewModel.onLocationChosen(it); showMapPicker = false },
            onDismiss = { showMapPicker = false },
        )
    }
}

@Composable
private fun Filters(state: HomeUiState, viewModel: HomeViewModel, onLocationClick: () -> Unit) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = onLocationClick,
            label = {
                Text(state.placeName ?: stringResource(R.string.your_area), style = MaterialTheme.typography.titleMedium)
            },
            leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.change_location)) },
            modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp),
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.category == null,
                    onClick = { viewModel.onCategoryChange(null) },
                    label = { Text(stringResource(R.string.all)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            items(Category.entries) { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { viewModel.onCategoryChange(category) },
                    label = { Text(stringResource(category.label)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.within), style = MaterialTheme.typography.bodyMedium)
            RADIUS_OPTIONS_KM.forEach { km ->
                FilterChip(
                    selected = state.radiusKm == km,
                    onClick = { viewModel.onRadiusChange(km) },
                    label = { Text(stringResource(R.string.km, km)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun ChooseLocationPrompt(state: HomeUiState, onUseGps: () -> Unit, onSearch: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.where_are_you), style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.where_are_you_hint), style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        BigButton(stringResource(R.string.use_my_location), onUseGps, loading = state.findingLocation)
        LocationButton(Icons.Filled.Search, R.string.search_place, onSearch)
        state.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LocationButton(icon: ImageVector, text: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(icon, contentDescription = null)
        Text(stringResource(text), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Scrollable so pull to refresh still works on an empty or error screen. */
@Composable
private fun Message(text: Int, hint: Int? = null, actionText: Int? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(text), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 48.dp))
        hint?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) }
        actionText?.let { BigButton(stringResource(it), onAction) }
    }
}
