package de.juvento.plain.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B2B2B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E4E4),
    onPrimaryContainer = Color(0xFF1B1B1B),
    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9E9),
    onSecondaryContainer = Color(0xFF1B1B1B),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF5A5A5A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    outline = Color(0xFF9E9E9E),
    outlineVariant = Color(0xFFE0E0E0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8E8E8),
    onPrimary = Color(0xFF1B1B1B),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFEDEDED),
    secondary = Color(0xFFB5B5B5),
    onSecondary = Color(0xFF1B1B1B),
    secondaryContainer = Color(0xFF2E2E2E),
    onSecondaryContainer = Color(0xFFEDEDED),
    background = Color(0xFF111111),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFB5B5B5),
    surfaceContainerLowest = Color(0xFF0B0B0B),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1D1D1D),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030),
    outline = Color(0xFF6A6A6A),
    outlineVariant = Color(0xFF333333),
)

private val SettingsShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

/** Minimal monochrome Material 3 theme used by all settings pages. */
@Composable
fun SettingsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = SettingsShapes,
        content = content,
    )
}
