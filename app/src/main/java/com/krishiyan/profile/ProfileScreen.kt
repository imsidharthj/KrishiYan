package com.krishiyan.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.krishiyan.R
import com.krishiyan.booking.BookingStatus
import com.krishiyan.booking.OwnerBooking
import com.krishiyan.equipment.MyEquipment
import com.krishiyan.ui.components.BigButton
import com.krishiyan.ui.components.StatusChip
import com.krishiyan.ui.components.formatRentalPeriod
import com.krishiyan.ui.components.formatRupees
import com.krishiyan.ui.components.priceText

private const val HISTORY_LIMIT = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onAddEquipment: () -> Unit,
    onOpenRequests: () -> Unit,
    onEditEquipment: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    PullToRefreshBox(isRefreshing = state.loading, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.profile?.let { p ->
                Column {
                    Text(p.name, style = MaterialTheme.typography.headlineMedium)
                    Text("${p.village} · ${p.phone}", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.error?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountTile(state.equipment.size, R.string.count_listed, Modifier.weight(1f))
                CountTile(state.pendingRequests, R.string.count_requests, Modifier.weight(1f), onClick = onOpenRequests)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountTile(state.upcomingRentals, R.string.count_upcoming, Modifier.weight(1f))
                CountTile(state.activeRentals, R.string.count_active, Modifier.weight(1f))
            }
            if (state.pendingRequests > 0) {
                BigButton(stringResource(R.string.booking_requests_count, state.pendingRequests), onOpenRequests)
            }

            OutlinedButton(onClick = onAddEquipment, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.add_equipment), style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp))
            }

            Text(stringResource(R.string.my_equipment), style = MaterialTheme.typography.titleLarge)
            if (!state.loading && state.equipment.isEmpty()) {
                Text(stringResource(R.string.no_equipment_yet), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.equipment.forEach { item ->
                MyEquipmentCard(
                    item,
                    updating = item.id in state.updatingIds,
                    onEdit = { onEditEquipment(item.id) },
                    onToggle = {
                        if (item.isAvailable) viewModel.markEquipmentUnavailable(item.id) else viewModel.markEquipmentAvailable(item.id)
                    },
                )
            }

            Text(stringResource(R.string.rental_history), style = MaterialTheme.typography.titleLarge)
            val history = state.history
            if (!state.loading && history.isEmpty()) {
                Text(stringResource(R.string.no_history_yet), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            history.take(HISTORY_LIMIT).forEach { HistoryRow(it) }

            HorizontalDivider()
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.log_out), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CountTile(count: Int, label: Int, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MyEquipmentCard(item: MyEquipment, updating: Boolean, onEdit: () -> Unit, onToggle: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = item.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(priceText(item.price, item.priceUnit), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary)
                    if (item.isAvailable) {
                        StatusChip(stringResource(R.string.listed), MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer)
                    } else {
                        StatusChip(stringResource(R.string.hidden_from_search), MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.edit), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(onClick = onToggle, enabled = !updating, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1.4f).heightIn(min = 48.dp)) {
                    Text(
                        stringResource(if (item.isAvailable) R.string.mark_unavailable else R.string.mark_available),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: OwnerBooking) {
    val b = item.booking
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(item.equipmentName, style = MaterialTheme.typography.titleMedium)
        Text(formatRentalPeriod(b.start, b.end), style = MaterialTheme.typography.bodyMedium)
        Text(
            "${formatRupees(b.estimatedCost)} · ${stringResource(historyStatus(b.status))}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun historyStatus(status: BookingStatus) = when (status) {
    BookingStatus.Accepted, BookingStatus.Completed -> R.string.status_completed
    BookingStatus.Rejected -> R.string.status_rejected
    BookingStatus.Cancelled -> R.string.status_cancelled
    BookingStatus.Pending -> R.string.status_pending
}
