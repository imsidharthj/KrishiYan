package com.krishiyan.equipment

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.krishiyan.R
import com.krishiyan.location.LocationPickerDialog
import com.krishiyan.ui.components.BigButton
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEquipmentScreen(viewModel: AddEquipmentViewModel, onBack: () -> Unit, onPublished: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.published) { if (state.published) onPublished() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_equipment)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PhotosSection(state.photos, viewModel::addPhotos, viewModel::removePhoto)
            DetailsSection(state, viewModel)
            PriceSection(state, viewModel)
            LocationSection(state, viewModel)
            AvailabilitySection(state, viewModel)

            state.error?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }
            BigButton(stringResource(R.string.publish), viewModel::publishEquipment, loading = state.publishing)
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun PhotosSection(photos: List<Uri>, onAdd: (List<Uri>) -> Unit, onRemove: (Uri) -> Unit) {
    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(AddEquipmentViewModel.MAX_PHOTOS),
    ) { onAdd(it) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        cameraUri?.takeIf { saved }?.let { onAdd(listOf(it)) }
    }
    val canAdd = photos.size < AddEquipmentViewModel.MAX_PHOTOS

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.photos)
        if (photos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(104.dp).clip(MaterialTheme.shapes.medium),
                        )
                        FilledIconButton(
                            onClick = { onRemove(uri) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_photo))
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTextButton(Icons.Filled.PhotoLibrary, R.string.gallery, enabled = canAdd, modifier = Modifier.weight(1f)) {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            IconTextButton(Icons.Filled.CameraAlt, R.string.camera, enabled = canAdd, modifier = Modifier.weight(1f)) {
                val uri = newCameraUri(context)
                cameraUri = uri
                camera.launch(uri)
            }
        }
    }
}

private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}

@Composable
private fun IconTextButton(
    icon: ImageVector,
    text: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text(stringResource(text), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun DetailsSection(state: AddEquipmentUiState, viewModel: AddEquipmentViewModel) {
    val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.category)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { viewModel.onCategoryChange(category) },
                    label = { Text(stringResource(category.label), style = MaterialTheme.typography.bodyLarge) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.equipment_name)) },
            placeholder = { Text(stringResource(R.string.equipment_name_example)) },
            singleLine = true,
            keyboardOptions = words,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.brandModel,
            onValueChange = viewModel::onBrandModelChange,
            label = { Text(stringResource(R.string.brand_model)) },
            singleLine = true,
            keyboardOptions = words,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.year,
            onValueChange = viewModel::onYearChange,
            label = { Text(stringResource(R.string.year_optional)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.description)) },
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PriceSection(state: AddEquipmentUiState, viewModel: AddEquipmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.rent_price)
        OutlinedTextField(
            value = state.price,
            onValueChange = viewModel::onPriceChange,
            label = { Text(stringResource(R.string.price_rupees)) },
            prefix = { Text("₹ ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PriceUnit.entries.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = state.priceUnit == unit,
                    onClick = { viewModel.onPriceUnitChange(unit) },
                    shape = SegmentedButtonDefaults.itemShape(index, PriceUnit.entries.size),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text(stringResource(unit.label), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun LocationSection(state: AddEquipmentUiState, viewModel: AddEquipmentViewModel) {
    var showPicker by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.useCurrentLocation() else viewModel.onLocationDenied()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.location)
        Text(stringResource(R.string.location_privacy_hint), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.location != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.location_set), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTextButton(Icons.Filled.MyLocation, R.string.use_my_location, enabled = !state.findingLocation,
                modifier = Modifier.weight(1f)) {
                permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
            IconTextButton(Icons.Filled.Place, R.string.pick_on_map, modifier = Modifier.weight(1f)) { showPicker = true }
        }
        if (state.findingLocation) {
            Text(stringResource(R.string.finding_location), style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showPicker) {
        LocationPickerDialog(
            start = state.location,
            onPicked = { viewModel.onLocationPicked(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailabilitySection(state: AddEquipmentUiState, viewModel: AddEquipmentViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(R.string.availability)
        RadioRow(R.string.available_now, selected = state.availableNow) { viewModel.onAvailableNowChange(true) }
        RadioRow(R.string.not_available_some_days, selected = !state.availableNow) { viewModel.onAvailableNowChange(false) }
        if (!state.availableNow) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.blocked.forEach { range ->
                    val lastDay = range.end.minusSeconds(1)
                    InputChip(
                        selected = false,
                        onClick = { viewModel.removeBlocked(range) },
                        label = { Text(stringResource(R.string.date_range, formatter.format(range.start), formatter.format(lastDay))) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove)) },
                    )
                }
            }
            IconTextButton(Icons.Filled.EventBusy, R.string.add_unavailable_days, modifier = Modifier.fillMaxWidth()) {
                showDatePicker = true
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedStartDateMillis != null,
                    onClick = {
                        val start = pickerState.selectedStartDateMillis!!
                        viewModel.addBlockedDays(start, pickerState.selectedEndDateMillis ?: start)
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RadioRow(text: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}
