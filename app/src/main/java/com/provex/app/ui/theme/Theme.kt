package com.provex.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = SoftBlue,
    onPrimaryContainer = NavyPrimary,
    secondary = DarkGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = DarkGreenBg,
    onSecondaryContainer = DarkGreen,
    tertiary = AmberDark,
    onTertiary = SurfaceWhite,
    tertiaryContainer = AmberBg,
    onTertiaryContainer = AmberDark,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderMedium,
    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRed,
)

@Composable
fun ProvexTheme(
    content: @Composable () -> Unit,
) {
    // PROVEX uses a crisp, clean light developer-tool aesthetic with navy accents
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = ProvexTypography,
        content = content,
    )
}
