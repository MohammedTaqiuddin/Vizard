package com.example.algovisualizer.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun schemeFrom(g: GlassColors) = if (g.isLight) {
    lightColorScheme(
        primary = g.accent,
        onPrimary = g.onAccent,
        primaryContainer = g.accentSoft,
        onPrimaryContainer = g.textPrimary,
        secondary = g.orbSecondary,
        background = g.base,
        onBackground = g.textPrimary,
        surface = g.base,
        onSurface = g.textPrimary,
        surfaceVariant = g.fillStrong,
        onSurfaceVariant = g.textSecondary,
        outline = g.edgeShadow
    )
} else {
    darkColorScheme(
        primary = g.accent,
        onPrimary = g.onAccent,
        primaryContainer = g.accentSoft,
        onPrimaryContainer = g.textPrimary,
        secondary = g.orbSecondary,
        background = g.base,
        onBackground = g.textPrimary,
        surface = g.base,
        onSurface = g.textPrimary,
        surfaceVariant = g.fillStrong,
        onSurfaceVariant = g.textSecondary,
        outline = g.edgeShadow
    )
}

@Composable
fun AlgoVisualizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is deliberately off: the glass palette is the brand, and wallpaper-derived
    // colors would fight the ambient orbs the whole design rests on.
    val glass = if (darkTheme) DarkGlass else LightGlass

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Bars stay transparent (edge-to-edge is enabled in MainActivity); only the
            // icon tint needs to follow the theme.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalGlass provides glass) {
        MaterialTheme(
            colorScheme = schemeFrom(glass),
            typography = Typography,
            content = content
        )
    }
}
