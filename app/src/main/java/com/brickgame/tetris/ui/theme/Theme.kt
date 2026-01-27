package com.brickgame.tetris.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Local composition for game theme
val LocalGameTheme = staticCompositionLocalOf { GameThemes.Classic }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4D03F),
    secondary = Color(0xFF1E90FF),
    tertiary = Color(0xFFDC143C),
    background = Color(0xFF2D2D2D),
    surface = Color(0xFF1A1A1A),
    onPrimary = Color(0xFF1A1A1A),
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun BrickGameTheme(
    gameTheme: GameTheme = GameThemes.Classic,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme.copy(
        primary = gameTheme.accentColor,
        secondary = gameTheme.buttonPrimary,
        tertiary = gameTheme.buttonSecondary,
        background = gameTheme.backgroundColor,
        surface = gameTheme.deviceColor
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = gameTheme.backgroundColor.toArgb()
            window.navigationBarColor = gameTheme.backgroundColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalGameTheme provides gameTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
