package com.krishiyan.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krishiyan.R
import com.krishiyan.equipment.PriceUnit
import com.krishiyan.ui.components.BigButton
import com.krishiyan.ui.components.formatRupees
import com.krishiyan.ui.components.priceText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestBookingScreen(viewModel: RequestBookingViewModel, onBack: () -> Unit, onSent: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.sent) { if (state.sent) onSent() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.request_booking)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val details = state.details
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                details == null -> Column(Modifier.align(Alignment.Center).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(state.error ?: R.string.error_generic), style = MaterialTheme.typography.titleMedium)
                    BigButton(stringResource(R.string.try_again), viewModel::load)
                }
                else -> Column(
                    Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column {
                        Text(details.name, style = MaterialTheme.typography.titleLarge)
                        Text(priceText(details.price, details.priceUnit), style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }

                    DateTimeRow(R.string.start, state.start, viewModel::onStartDate, viewModel::onStartTime, minDate = LocalDate.now())
                    DateTimeRow(R.string.end, state.end, viewModel::onEndDate, viewModel::onEndTime,
                        minDate = state.start?.toLocalDate() ?: LocalDate.now(), enabled = state.start != null)

                    CostCard(state)

                    OutlinedTextField(
                        value = state.message,
                        onValueChange = viewModel::onMessageChange,
                        label = { Text(stringResource(R.string.message_optional)) },
                        placeholder = { Text(stringResource(R.string.message_example)) },
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    state.error?.let {
                        Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                    }
                    BigButton(stringResource(R.string.send_request), viewModel::requestBooking, loading = state.sending)
                    Text(stringResource(R.string.request_note), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CostCard(state: RequestBookingUiState) {
    val cost = state.cost
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.estimated_cost), style = MaterialTheme.typography.bodyMedium)
            if (cost == null) {
                Text(stringResource(R.string.pick_dates_for_cost), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(formatRupees(cost.total), style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary)
                val count = cost.units.toInt()
                val units = pluralStringResource(
                    if (state.priceUnit == PriceUnit.Hour) R.plurals.n_hours else R.plurals.n_days, count, count,
                )
                Text("$units × ${formatRupees(state.details!!.price)}", style = MaterialTheme.typography.bodyLarge)
            }
            if (state.clashes) {
                Text(stringResource(R.string.error_dates_unavailable), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeRow(
    label: Int,
    value: LocalDateTime?,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
    minDate: LocalDate,
    enabled: Boolean = true,
) {
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerButton(Icons.Filled.CalendarMonth, value?.format(dateFormat) ?: stringResource(R.string.pick_date),
                enabled = enabled, modifier = Modifier.weight(1.4f)) { pickingDate = true }
            PickerButton(Icons.Filled.Schedule, value?.format(timeFormat) ?: stringResource(R.string.pick_time),
                enabled = enabled && value != null, modifier = Modifier.weight(1f)) { pickingTime = true }
        }
    }

    if (pickingDate) {
        val minMillis = minDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (value?.toLocalDate() ?: minDate).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(pickerState) }
    }

    if (pickingTime && value != null) {
        val timeState = rememberTimePickerState(value.hour, value.minute)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = { onTime(LocalTime.of(timeState.hour, timeState.minute)); pickingTime = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(timeState) },
        )
    }
}

@Composable
private fun PickerButton(icon: ImageVector, text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = MaterialTheme.shapes.medium,
        modifier = modifier.heightIn(min = 56.dp)) {
        Icon(icon, contentDescription = null)
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}
