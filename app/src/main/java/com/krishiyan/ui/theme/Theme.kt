package com.krishiyan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val colors = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color.White,
    secondary = LightGreen,
    onSecondary = Charcoal,
    secondaryContainer = PaleGreen,
    onSecondaryContainer = DeepGreen,
    background = OffWhite,
    onBackground = Charcoal,
    surface = OffWhite,
    onSurface = Charcoal,
    surfaceContainer = Color.White,
    onSurfaceVariant = MutedGrey,
    error = ErrorRed,
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun KrishiYanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = shapes,
        content = content,
    )
}
