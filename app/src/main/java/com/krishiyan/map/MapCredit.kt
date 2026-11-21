package com.krishiyan.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krishiyan.R

/** OpenStreetMap's licence requires this credit wherever its map is shown. */
@Composable
fun MapCredit(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.osm_credit),
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
