package com.flux.recorder.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Electric Flux Color Scheme - AMOLED Optimized Dark Theme
 * Designed for maximum battery efficiency, visual clarity, and flawless edge-to-edge system bars.
 */
private val ElectricFluxColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = TextPrimary,
    primaryContainer = ElectricVioletDark,
    onPrimaryContainer = TextPrimary,
    
    secondary = FluxCyan,
    onSecondary = VoidBlack,
    secondaryContainer = FluxCyanDark,
    onSecondaryContainer = TextPrimary,
    
    tertiary = FluxCyan,
    onTertiary = VoidBlack,
    
    background = VoidBlack,
    onBackground = TextPrimary,
    
    surface = SurfaceBlack,
    onSurface = TextPrimary,
    surfaceVariant = CardBlack,
    onSurfaceVariant = TextSecondary,
    
    error = RecordingRed,
    onError = TextPrimary,
    
    outline = TextDisabled,
    outlineVariant = CardBlack
)

@Composable
fun FluxRecorderTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ElectricFluxColorScheme,
        typography = Typography,
        content = content
    )
}
