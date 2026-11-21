package com.krishiyan.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krishiyan.R
import com.krishiyan.ui.components.BigButton
import com.krishiyan.ui.components.formatRentalPeriod
import com.krishiyan.ui.components.formatRupees

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerRequestsScreen(viewModel: OwnerRequestsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmReject by remember { mutableStateOf<OwnerRequest?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.booking_requests)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.error != null && state.requests.isEmpty() ->
                    CenterMessage(stringResource(state.error!!), onRetry = viewModel::refresh)
                !state.loading && state.requests.isEmpty() -> CenterMessage(stringResource(R.string.no_requests))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.requests, key = { it.booking.id }) { request ->
                        RequestCard(
                            request,
                            working = request.booking.id in state.busyIds,
                            onAccept = { viewModel.acceptBooking(request.booking.id) },
                            onReject = { confirmReject = request },
                        )
                    }
                }
            }
        }
    }

    confirmReject?.let { request ->
        AlertDialog(
            onDismissRequest = { confirmReject = null },
            title = { Text(stringResource(R.string.reject_title)) },
            text = { Text(stringResource(R.string.reject_text, request.renterName, request.equipmentName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.rejectBooking(request.booking.id); confirmReject = null }) {
                    Text(stringResource(R.string.reject))
                }
            },
            dismissButton = { TextButton(onClick = { confirmReject = null }) { Text(stringResource(R.string.cancel)) } },
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

@Composable
private fun RequestCard(request: OwnerRequest, working: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    val b = request.booking
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(request.equipmentName, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.requested_by, request.renterName, request.renterVillage), style = MaterialTheme.typography.bodyLarge)
            Text(formatRentalPeriod(b.start, b.end), style = MaterialTheme.typography.bodyLarge)
            Text(formatRupees(b.estimatedCost), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (b.message.isNotBlank()) {
                Text("“${b.message}”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !working,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.reject), style = MaterialTheme.typography.labelLarge) }
                BigButton(stringResource(R.string.accept), onAccept, loading = working, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 64.dp))
        onRetry?.let { BigButton(stringResource(R.string.try_again), it) }
    }
}
