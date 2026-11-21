package com.krishiyan.equipment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.krishiyan.R
import com.krishiyan.ui.components.BigButton
import com.krishiyan.ui.components.StatusChip
import com.krishiyan.ui.components.priceText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentDetailsScreen(viewModel: EquipmentDetailsViewModel, onBack: () -> Unit, onRequestBooking: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val details = state.details

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            if (details != null) {
                Surface(shadowElevation = 8.dp) {
                    Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                        if (state.isMine) {
                            Text(stringResource(R.string.this_is_your_listing), style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                        } else {
                            BigButton(stringResource(R.string.request_booking), onClick = { onRequestBooking(details.id) })
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                details == null -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(stringResource(state.error ?: R.string.error_generic), style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    BigButton(stringResource(R.string.try_again), viewModel::load)
                }
                else -> DetailsContent(state, details)
            }
        }
    }
}

@Composable
private fun DetailsContent(state: EquipmentDetailsUiState, details: EquipmentDetails) {
    val busy = remember(state.busy) { busyDays(state.busy) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Photos(state.photos)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusChip(
                stringResource(Category.fromDb(details.category).label),
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(details.name, style = MaterialTheme.typography.headlineMedium)
            Text(priceText(details.price, details.priceUnit), style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary)
            InfoRow(Icons.Filled.Person, stringResource(R.string.owner_from, details.ownerFirstName, details.ownerVillage))
            state.distanceKm?.let { InfoRow(Icons.Filled.Place, stringResource(R.string.about_km_away, it)) }

            val facts = listOfNotNull(
                details.brandModel.takeIf { it.isNotBlank() },
                details.year?.let { stringResource(R.string.made_in_year, it) },
            )
            if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
            if (details.description.isNotBlank()) Text(details.description, style = MaterialTheme.typography.bodyLarge)

            HorizontalDivider()
            Text(stringResource(R.string.availability), style = MaterialTheme.typography.titleMedium)
            AvailabilityCalendar(busy)

            HorizontalDivider()
            Text(stringResource(R.string.rental_terms), style = MaterialTheme.typography.titleMedium)
            listOf(R.string.term_owner_accepts, R.string.term_pay_owner, R.string.term_cancel, R.string.term_location)
                .forEach { Text("• " + stringResource(it), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun Photos(photos: List<String>) {
    val shape = Modifier.fillMaxWidth().height(260.dp).background(MaterialTheme.colorScheme.secondaryContainer)
    if (photos.isEmpty()) {
        Box(shape)
        return
    }
    val pager = rememberPagerState { photos.size }
    Box {
        HorizontalPager(pager) { page ->
            AsyncImage(photos[page], contentDescription = null, contentScale = ContentScale.Crop, modifier = shape)
        }
        if (photos.size > 1) {
            Text(
                "${pager.currentPage + 1} / ${photos.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
