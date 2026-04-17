package com.offgrid.android.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// Ice Blue Minimal Theme - Dark Mode Only
private val IceBlueColorScheme = darkColorScheme(
    // Primary: Cool Ice Blue
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0A0E13),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFF90CAF9),
    
    // Secondary: Steel Blue
    secondary = Color(0xFF42A5F5),
    onSecondary = Color(0xFF0A0E13),
    secondaryContainer = Color(0xFF1565C0),
    onSecondaryContainer = Color(0xFF81D4FA),
    
    // Background: Cool Gray/Black
    background = Color(0xFF0A0E13),
    onBackground = Color(0xFFECEFF1),
    
    // Surface: Layered cool grays
    surface = Color(0xFF1A1F26),
    onSurface = Color(0xFFB0BEC5),
    surfaceVariant = Color(0xFF2A3039),
    onSurfaceVariant = Color(0xFF90A4AE),
    
    // Tertiary: Accent blue
    tertiary = Color(0xFF29B6F6),
    onTertiary = Color(0xFF0A0E13),
    tertiaryContainer = Color(0xFF0277BD),
    onTertiaryContainer = Color(0xFF4FC3F7),
    
    // Error: Soft red that works with blue theme
    error = Color(0xFFEF5350),
    onError = Color(0xFF0A0E13),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFCDD2),
    
    // Outline and other elements
    outline = Color(0xFF546E7A),
    outlineVariant = Color(0xFF37474F),
    scrim = Color(0xFF000000),
    
    // Inverse colors for special cases
    inverseSurface = Color(0xFFECEFF1),
    inverseOnSurface = Color(0xFF263238),
    inversePrimary = Color(0xFF1976D2)
)

@Composable
fun OffGridTheme(
    content: @Composable () -> Unit
) {
    // Force dark mode only - no light theme option
    val colorScheme = IceBlueColorScheme

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            // Always use dark status bar appearance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    0, // Dark status bar
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = 0 // Dark status bar
            }
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
