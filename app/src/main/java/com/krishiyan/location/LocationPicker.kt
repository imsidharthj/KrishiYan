package com.krishiyan.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krishiyan.R
import com.krishiyan.map.MapCredit
import com.krishiyan.ui.components.BigButton
import kotlinx.coroutines.delay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/** India's rough centre, used when we know nothing about where the user is. */
val INDIA_CENTER = LatLng(22.5, 79.0)

private const val ADDRESS_DELAY_MS = 600L

/**
 * Full-screen map with a fixed pin in the middle. The user moves the map under the pin
 * and sees the address of the pinned spot before confirming.
 */
@Composable
fun LocationPickerDialog(
    start: LatLng?,
    placeSearch: PlaceSearch,
    onPicked: (Place) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var center by remember { mutableStateOf(start ?: INDIA_CENTER) }
    var address by remember { mutableStateOf<Place?>(null) }
    var lookingUp by remember { mutableStateOf(false) }

    val map = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(if (start != null) 16.0 else 5.0)
            controller.setCenter(GeoPoint(center.lat, center.lng))
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    center = LatLng(mapCenter.latitude, mapCenter.longitude)
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean = false
            })
        }
    }
    DisposableEffect(map) {
        map.onResume()
        onDispose { map.onPause(); map.onDetach() }
    }
    LaunchedEffect(center) {
        lookingUp = true
        delay(ADDRESS_DELAY_MS)
        address = try { placeSearch.placeAt(center) } catch (e: Exception) { null }
        lookingUp = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Text(
                    stringResource(R.string.pick_location_hint),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
                    MapCredit(Modifier.align(Alignment.BottomStart))
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center).offset(y = (-24).dp).size(48.dp),
                    )
                }
                Column(Modifier.padding(16.dp)) {
                    Column(Modifier.heightIn(min = 56.dp).padding(bottom = 8.dp)) {
                        when {
                            lookingUp -> Text(stringResource(R.string.finding_address), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            address != null -> {
                                Text(address!!.name, style = MaterialTheme.typography.titleMedium)
                                if (address!!.detail.isNotEmpty()) {
                                    Text(address!!.detail, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            else -> Text(stringResource(R.string.address_unknown), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    BigButton(stringResource(R.string.pick_location_confirm), onClick = {
                        val here = LatLng(map.mapCenter.latitude, map.mapCenter.longitude)
                        onPicked(address?.takeIf { !lookingUp }?.copy(location = here) ?: Place(context.getString(R.string.pinned_place), "", here))
                    })
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}
