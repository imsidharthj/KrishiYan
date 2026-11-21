package com.krishiyan.booking

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.krishiyan.R
import com.krishiyan.ui.components.BigButton
import com.krishiyan.ui.components.StatusChip
import com.krishiyan.ui.components.formatRentalPeriod
import com.krishiyan.ui.components.formatRupees

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRentalsScreen(viewModel: MyRentalsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmCancel by remember { mutableStateOf<MyRental?>(null) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(stringResource(R.string.tab_my_rentals), style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp))
        PrimaryScrollableTabRow(selectedTabIndex = state.tab.ordinal, edgePadding = 16.dp) {
            RentalTab.entries.forEach { tab ->
                val count = state.inTab(tab).size
                Tab(
                    selected = state.tab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            if (count > 0) "${stringResource(tab.label)} ($count)" else stringResource(tab.label),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    modifier = Modifier.heightIn(min = 56.dp),
                )
            }
        }
        PullToRefreshBox(isRefreshing = state.loading, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            val items = state.inTab(state.tab)
            when {
                state.error != null && state.rentals.isEmpty() -> Empty(stringResource(state.error!!), viewModel::refresh)
                !state.loading && items.isEmpty() -> Empty(stringResource(emptyText(state.tab)))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items, key = { it.booking.id }) { rental ->
                        RentalCard(
                            rental,
                            contact = state.contacts[rental.booking.id],
                            canCancel = canCancel(rental.booking.status, rental.booking.start, state.now),
                            cancelling = rental.booking.id in state.cancellingIds,
                            onCancel = { confirmCancel = rental },
                        )
                    }
                }
            }
        }
    }

    confirmCancel?.let { rental ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text(stringResource(R.string.cancel_booking_title)) },
            text = { Text(stringResource(R.string.cancel_booking_text, rental.equipmentName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelBooking(rental.booking.id); confirmCancel = null }) {
                    Text(stringResource(R.string.cancel_booking))
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text(stringResource(R.string.keep_booking)) } },
        )
    }
    state.actionError?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissActionError,
            text = { Text(stringResource(it)) },
            confirmButton = { TextButton(onClick = viewModel::dismissActionError) { Text(stringResource(R.string.ok)) } },
        )
    }
}

private fun emptyText(tab: RentalTab) = when (tab) {
    RentalTab.Pending -> R.string.empty_pending
    RentalTab.Confirmed -> R.string.empty_confirmed
    RentalTab.Active -> R.string.empty_active
    RentalTab.Past -> R.string.empty_past
}

@Composable
private fun RentalCard(rental: MyRental, contact: BookingContact?, canCancel: Boolean, cancelling: Boolean, onCancel: () -> Unit) {
    val b = rental.booking
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = rental.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(rental.equipmentName, style = MaterialTheme.typography.titleMedium)
                    Text(formatRentalPeriod(b.start, b.end), style = MaterialTheme.typography.bodyMedium)
                    Text(formatRupees(b.estimatedCost), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    StatusBadge(b.status)
                }
            }
            if (contact != null) {
                Text(stringResource(R.string.owner_contact, contact.ownerName, contact.ownerPhone),
                    style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Filled.Call, R.string.call, Modifier.weight(1f)) { dial(context, contact.ownerPhone) }
                    ActionButton(Icons.AutoMirrored.Filled.Chat, R.string.whatsapp, Modifier.weight(1f)) {
                        openWhatsApp(context, contact.ownerPhone, context.getString(R.string.whatsapp_hello, rental.equipmentName))
                    }
                }
                ActionButton(Icons.Filled.Map, R.string.open_in_maps, Modifier.fillMaxWidth()) {
                    openMap(context, contact.lat, contact.lng, rental.equipmentName)
                }
            }
            if (canCancel) {
                TextButton(onClick = onCancel, enabled = !cancelling, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cancel_booking), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BookingStatus) {
    val (label, container, content) = when (status) {
        BookingStatus.Pending -> Triple(R.string.status_pending, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
        BookingStatus.Accepted -> Triple(R.string.status_accepted, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        BookingStatus.Rejected -> Triple(R.string.status_rejected, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.error)
        BookingStatus.Cancelled -> Triple(R.string.status_cancelled, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        BookingStatus.Completed -> Triple(R.string.status_completed, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    StatusChip(stringResource(label), container, content)
}

@Composable
private fun ActionButton(icon: ImageVector, text: Int, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = MaterialTheme.shapes.medium, modifier = modifier.heightIn(min = 56.dp)) {
        Icon(icon, contentDescription = null)
        Text(stringResource(text), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Empty(text: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 64.dp))
        onRetry?.let { BigButton(stringResource(R.string.try_again), it) }
    }
}

private const val INDIA_CODE = "91"

private fun dial(context: Context, phone: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$INDIA_CODE$phone")))
}

private fun openWhatsApp(context: Context, phone: String, text: String) {
    val uri = Uri.parse("https://wa.me/$INDIA_CODE$phone?text=${Uri.encode(text)}")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

private fun openMap(context: Context, lat: Double, lng: Double, label: String) {
    val geo = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=16/$lat/$lng")))
    }
}
