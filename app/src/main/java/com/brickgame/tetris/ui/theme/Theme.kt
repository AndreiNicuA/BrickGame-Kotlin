package com.brickgame.tetris.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme()

@Composable
fun BrickGameTheme(
    gameTheme: GameTheme = GameThemes.ClassicGreen,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalGameTheme provides gameTheme) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}
