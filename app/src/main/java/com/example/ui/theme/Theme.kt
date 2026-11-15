package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FarmDarkColorScheme = darkColorScheme(
    primary = AgroSprout,
    onPrimary = CabinDark,
    primaryContainer = AgroForest,
    onPrimaryContainer = TextPrimary,
    secondary = HarvestGold,
    onSecondary = CabinDark,
    secondaryContainer = TerraRoxa,
    onSecondaryContainer = TextPrimary,
    tertiary = HarvestAmber,
    onTertiary = CabinDark,
    background = CabinDark,
    onBackground = TextPrimary,
    surface = CabinSurface,
    onSurface = TextPrimary,
    surfaceVariant = CabinCard,
    onSurfaceVariant = TextSecondary,
    outline = CabinBorder,
    outlineVariant = CabinBorder.copy(alpha = 0.6f),
    error = StatusDanger,
    onError = CabinDark
)

private val FarmShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FarmBrasilTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FarmDarkColorScheme,
        typography = FarmBrasilTypography,
        shapes = FarmShapes,
        content = content
    )
}
