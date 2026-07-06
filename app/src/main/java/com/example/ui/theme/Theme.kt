package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SciFiColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.Black,
    primaryContainer = ElectricBlueDim,
    secondary = CyberPurple,
    onSecondary = Color.White,
    secondaryContainer = CyberPurpleDim,
    background = DeepSpaceBlack,
    onBackground = TextPrimary,
    surface = DeepNavy,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = DangerNeon,
    onError = Color.Black
)

@Composable
fun StacyVerseTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SciFiColorScheme,
        typography = Typography,
        content = content
    )
}
