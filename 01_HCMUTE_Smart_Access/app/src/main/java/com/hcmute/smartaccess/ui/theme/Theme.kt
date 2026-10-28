package com.hcmute.smartaccess.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =====================================================
// LIGHT THEME
// =====================================================

private val LightColors = lightColorScheme(

    primary = Color(0xFF005BAC),
    onPrimary = Color.White,

    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF001B3D),

    secondary = Color(0xFF506274),
    onSecondary = Color.White,

    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF171A1F),

    surface = Color.White,
    onSurface = Color(0xFF171A1F),

    surfaceVariant = Color(0xFFEDF2F8),
    onSurfaceVariant = Color(0xFF56606C),

    outline = Color(0xFFD5DCE5),

    error = Color(0xFFBA1A1A),
    onError = Color.White,

    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)


// =====================================================
// SOFT DARK THEME
// =====================================================

private val DarkColors = darkColorScheme(

    // Accent xanh HCM-UTE
    primary = Color(0xFF74B7FF),
    onPrimary = Color(0xFF00315E),

    primaryContainer = Color(0xFF174A78),
    onPrimaryContainer = Color(0xFFD7E9FF),

    secondary = Color(0xFFBBC9D8),
    onSecondary = Color(0xFF26323E),

    // Nền tổng thể
    background = Color(0xFF111820),
    onBackground = Color(0xFFF4F7FA),

    // Card chính
    surface = Color(0xFF1B2530),
    onSurface = Color(0xFFF4F7FA),

    // Card phụ / Quick Actions
    surfaceVariant = Color(0xFF253241),
    onSurfaceVariant = Color(0xFFBEC8D3),

    // Viền card
    outline = Color(0xFF3A4857),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),

    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6)
)


// =====================================================
// APP THEME
// =====================================================

@Composable
fun HCMUTESmartAccessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                DarkColors
            } else {
                LightColors
            },
        typography = Typography,
        content = content
    )
}