package com.cdelarue.localmusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.cdelarue.localmusic.data.ThemeMode

// Sampled from the app icon: navy ground, the note's turquoise as the accent, the sail's coral
// held back for the few places that need a second voice.
private val Navy = Color(0xFF0E1526)
private val NavySurface = Color(0xFF141C31)
private val NavyRaised = Color(0xFF1B2440)
private val NavySelected = Color(0xFF123A4A)
private val Turquoise = Color(0xFF00DCDF)
private val OnTurquoise = Color(0xFF00363A)
private val Coral = Color(0xFFFF8F6C)
private val Ink = Color(0xFFE6EBF5)
private val InkMuted = Color(0xFF97A3BF)
private val Outline = Color(0xFF29334F)

private val DarkScheme = darkColorScheme(
    primary = Turquoise,
    onPrimary = OnTurquoise,
    secondary = Coral,
    onSecondary = OnTurquoise,
    secondaryContainer = NavySelected,
    onSecondaryContainer = Ink,
    tertiary = Coral,
    background = Navy,
    onBackground = Ink,
    surface = Navy,
    onSurface = Ink,
    surfaceVariant = NavyRaised,
    onSurfaceVariant = InkMuted,
    surfaceContainer = NavySurface,
    surfaceContainerHigh = NavyRaised,
    outline = Outline,
    outlineVariant = Outline,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006A6C),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF9B4020),
    secondaryContainer = Color(0xFFCDF0F0),
    tertiary = Color(0xFF9B4020),
)

val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun LocalMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
