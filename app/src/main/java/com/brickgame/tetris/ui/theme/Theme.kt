package com.brickgame.tetris.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

/** Tracks whether the app UI is in dark mode — consumed by Settings and other screens */
val LocalIsDarkMode = compositionLocalOf { true }

@Composable
fun BrickGameTheme(
    gameTheme: GameTheme = GameThemes.ClassicGreen,
    appThemeMode: String = "auto",
    content: @Composable () -> Unit
) {
    val isDark = when (appThemeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalGameTheme provides gameTheme,
        LocalIsDarkMode provides isDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
