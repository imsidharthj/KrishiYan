package com.krishiyan.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krishiyan.R
import com.krishiyan.ui.components.BigButton
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/** India's rough centre, used when we know nothing about where the user is. */
val INDIA_CENTER = LatLng(22.5, 79.0)

/** Full-screen map with a fixed pin in the middle. The user moves the map under the pin. */
@Composable
fun LocationPickerDialog(start: LatLng?, onPicked: (LatLng) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Text(
                    stringResource(R.string.pick_location_hint),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
                val context = LocalContext.current
                val map = remember {
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(if (start != null) 16.0 else 5.0)
                        controller.setCenter(GeoPoint((start ?: INDIA_CENTER).lat, (start ?: INDIA_CENTER).lng))
                    }
                }
                DisposableEffect(map) {
                    map.onResume()
                    onDispose { map.onPause(); map.onDetach() }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center).offset(y = (-24).dp).size(48.dp),
                    )
                }
                Column(Modifier.padding(16.dp)) {
                    BigButton(stringResource(R.string.pick_location_confirm), onClick = {
                        onPicked(LatLng(map.mapCenter.latitude, map.mapCenter.longitude))
                    })
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}
