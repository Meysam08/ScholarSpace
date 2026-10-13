package com.example.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = MutedSage,
    onPrimaryContainer = DeepTealDark,
    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBEFD7),
    onSecondaryContainer = Color(0xFF5E3F0F),
    tertiary = CoverDustyBlue,
    onTertiary = Color.White,
    background = WarmIvory,
    onBackground = PrimaryInk,
    surface = WhiteSurface,
    onSurface = PrimaryInk,
    surfaceVariant = Color(0xFFEBF1EE),
    onSurfaceVariant = SecondaryInk,
    outline = OutlineMuted,
    outlineVariant = Color(0xFFE2EBE8)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryTeal,
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF144D46),
    onPrimaryContainer = Color(0xFFA5E6DC),
    secondary = DarkAmberAccent,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF573E18),
    onSecondaryContainer = Color(0xFFFFDFB0),
    tertiary = CoverDustyBlue,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkOutlineMuted,
    outlineVariant = Color(0xFF2E3836)
)

@Composable
fun ScholarSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve carefully curated brand palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
