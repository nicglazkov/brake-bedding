package com.glazkov.brakebedding.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Ember,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE1D2),
    onPrimaryContainer = EmberDeep,
    secondary = androidx.compose.ui.graphics.Color(0xFF6F5F57),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    // Segmented buttons and filter chips use secondaryContainer. Without this value,
    // they get the default purple. That color does not agree with the app colors.
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFF6DED0),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF4A3427),
    background = Bone,
    onBackground = Ink,
    surface = Bone,
    onSurface = Ink,
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFF3EDE9),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFEDE6E1),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEFE7E2),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF55483F),
    outline = androidx.compose.ui.graphics.Color(0xFF87786F),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFD8CCC4),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = EmberBright,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF3B1503),
    primaryContainer = EmberDeep,
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE1D2),
    secondary = androidx.compose.ui.graphics.Color(0xFFD8C6BC),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF3B2D26),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF564439),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFF3DFD3),
    background = Graphite,
    onBackground = androidx.compose.ui.graphics.Color(0xFFEDE0D9),
    surface = Graphite,
    onSurface = androidx.compose.ui.graphics.Color(0xFFEDE0D9),
    surfaceContainer = GraphiteRaised,
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF2E2621),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF52443C),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD7C3B8),
    outline = Steel,
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF52443C),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
)

/**
 * The app does not use dynamic color. This is intentional. Fixed phase colors carry
 * the meaning of the run screen. If the wallpaper changed the other screens, the app
 * would look like two different products.
 */
@Composable
fun BrakeBeddingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
