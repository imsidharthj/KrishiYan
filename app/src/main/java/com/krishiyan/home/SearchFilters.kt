package com.krishiyan.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krishiyan.R
import com.krishiyan.equipment.Category

/** Search text, equipment type and distance. Shown on the Map tab. */
@Composable
fun SearchFilters(state: HomeUiState, viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        TypeAndDistanceChips(state, viewModel)
    }
}

/** Equipment type and distance chips, shared by the Map tab and the place picker. */
@Composable
fun TypeAndDistanceChips(state: HomeUiState, viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            item { Text(stringResource(R.string.within), style = MaterialTheme.typography.bodyMedium) }
            items(RADIUS_OPTIONS_KM) { km ->
                FilterChip(
                    selected = state.radiusKm == km,
                    onClick = { viewModel.onRadiusChange(km) },
                    label = { Text(stringResource(R.string.km, km), maxLines = 1) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}
