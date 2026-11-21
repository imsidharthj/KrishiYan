package com.krishiyan.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krishiyan.R
import com.krishiyan.equipment.NearbyEquipment
import com.krishiyan.home.HomeViewModel
import com.krishiyan.home.SearchFilters
import com.krishiyan.location.LatLng
import com.krishiyan.ui.components.BigButton
import com.krishiyan.ui.components.EquipmentCard
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.ln

/** Shows the same results as Home, on a map. */
@Composable
fun MapScreen(viewModel: HomeViewModel, onOpenEquipment: (String) -> Unit, onChooseLocation: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<NearbyEquipment?>(null) }
    LaunchedEffect(state.items) { if (selected !in state.items) selected = null }

    val center = state.location
    if (center == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(R.string.map_needs_location), style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            BigButton(stringResource(R.string.choose_location), onChooseLocation)
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        EquipmentMap(center, state.radiusKm, state.items, onMarkerClick = { selected = it }, onMapClick = { selected = null })

        Column(Modifier.fillMaxWidth()) {
            Surface(shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.statusBarsPadding().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.map_showing, state.placeName ?: stringResource(R.string.your_area), state.radiusKm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SearchFilters(state, viewModel)
                }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            val message = when {
                state.error != null -> state.error
                state.loaded && state.items.isEmpty() -> R.string.no_equipment_nearby
                else -> null
            }
            message?.let {
                Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 2.dp, modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(it), style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
        }

        MapCredit(Modifier.align(Alignment.BottomStart))

        selected?.let { item ->
            EquipmentCard(item, onClick = { onOpenEquipment(item.id) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

@Composable
private fun EquipmentMap(
    center: LatLng,
    radiusKm: Int,
    items: List<NearbyEquipment>,
    onMarkerClick: (NearbyEquipment) -> Unit,
    onMapClick: () -> Unit,
) {
    val context = LocalContext.current
    val map = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        map.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            map.onPause()
            map.onDetach()
        }
    }

    LaunchedEffect(center, radiusKm) {
        map.controller.setZoom(zoomForRadius(radiusKm))
        map.controller.setCenter(GeoPoint(center.lat, center.lng))
    }

    AndroidView(
        factory = { map },
        update = { view ->
            view.overlays.clear()
            view.overlays.add(TapOverlay(onMapClick))
            view.overlays.add(Marker(view).apply {
                position = GeoPoint(center.lat, center.lng)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_you_are_here)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                infoWindow = null
                setOnMarkerClickListener { _, _ -> true }
            })
            items.forEach { item ->
                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(item.approxLat, item.approxLng)
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_equipment_marker)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    infoWindow = null
                    setOnMarkerClickListener { _, _ -> onMarkerClick(item); true }
                })
            }
            view.invalidate()
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/** Zoom so the whole search circle roughly fits on a phone screen. */
private fun zoomForRadius(radiusKm: Int): Double = (15.5 - ln(radiusKm.toDouble()) / ln(2.0)).coerceIn(5.0, 16.0)

private class TapOverlay(private val onTap: () -> Unit) : org.osmdroid.views.overlay.Overlay() {
    override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
        onTap()
        return false
    }
}
