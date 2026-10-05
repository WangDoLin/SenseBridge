package com.sensebridge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * SenseBridge Lumina Theme: Implements the retro neo-brutalist styling
 * of lumina-byte-website.vercel.app with high accessibility contrast.
 */
private val LuminaColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceCard,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = TextPrimary,
    secondary = P2AttentionGreen,
    onSecondary = SurfaceCard,
    tertiary = P1WarningAmber,
    onTertiary = SurfaceCard,
    error = P0DangerRed,
    onError = SurfaceCard,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark
)

@Composable
fun SenseBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuminaColorScheme,
        typography = Typography,
        content = content
    )
}
