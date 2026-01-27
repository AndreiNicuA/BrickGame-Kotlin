package com.brickgame.tetris.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GameTheme(
    val name: String,
    val backgroundColor: Color,
    val deviceColor: Color,
    val screenBackground: Color,
    val pixelOn: Color,
    val pixelOff: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val buttonPrimary: Color,
    val buttonPrimaryPressed: Color,
    val buttonSecondary: Color,
    val buttonSecondaryPressed: Color,
    val accentColor: Color
)

object GameThemes {
    // Classic - Pale olive green LCD (original Game Boy style, muted)
    val Classic = GameTheme(
        name = "Classic",
        backgroundColor = Color(0xFF2A2A2A),
        deviceColor = Color(0xFF3D3D3D),
        screenBackground = Color(0xFFB8C4A8),  // Muted olive green
        pixelOn = Color(0xFF3D4A32),           // Dark olive
        pixelOff = Color(0xFFA8B498),          // Slightly darker than background
        textPrimary = Color(0xFFE8E8E8),
        textSecondary = Color(0xFF9A9A9A),
        buttonPrimary = Color(0xFFD4C896),     // Muted yellow
        buttonPrimaryPressed = Color(0xFFBFB682),
        buttonSecondary = Color(0xFF4A4A4A),
        buttonSecondaryPressed = Color(0xFF3A3A3A),
        accentColor = Color(0xFFD4C896)
    )
    
    // Retro - Pale amber/orange LCD
    val Retro = GameTheme(
        name = "Retro",
        backgroundColor = Color(0xFF2D2520),
        deviceColor = Color(0xFF3D3530),
        screenBackground = Color(0xFFD4C4A8),  // Muted amber
        pixelOn = Color(0xFF5A4830),           // Dark brown
        pixelOff = Color(0xFFC4B498),
        textPrimary = Color(0xFFE8E0D8),
        textSecondary = Color(0xFFA09080),
        buttonPrimary = Color(0xFFCAAA78),     // Muted orange
        buttonPrimaryPressed = Color(0xFFB89868),
        buttonSecondary = Color(0xFF4A4038),
        buttonSecondaryPressed = Color(0xFF3A3028),
        accentColor = Color(0xFFCAAA78)
    )
    
    // Ocean - Pale blue-green
    val Ocean = GameTheme(
        name = "Ocean",
        backgroundColor = Color(0xFF202830),
        deviceColor = Color(0xFF2A3540),
        screenBackground = Color(0xFFA8C4C8),  // Muted teal
        pixelOn = Color(0xFF2A4A50),           // Dark teal
        pixelOff = Color(0xFF98B4B8),
        textPrimary = Color(0xFFE0E8EA),
        textSecondary = Color(0xFF8098A0),
        buttonPrimary = Color(0xFF88B8C0),     // Muted cyan
        buttonPrimaryPressed = Color(0xFF78A8B0),
        buttonSecondary = Color(0xFF384048),
        buttonSecondaryPressed = Color(0xFF283038),
        accentColor = Color(0xFF88B8C0)
    )
    
    // Sakura - Pale pink
    val Sakura = GameTheme(
        name = "Sakura",
        backgroundColor = Color(0xFF2A2528),
        deviceColor = Color(0xFF3A3035),
        screenBackground = Color(0xFFD8C4C8),  // Muted pink
        pixelOn = Color(0xFF5A3A40),           // Dark rose
        pixelOff = Color(0xFFC8B4B8),
        textPrimary = Color(0xFFE8E0E2),
        textSecondary = Color(0xFFA08890),
        buttonPrimary = Color(0xFFD0A0A8),     // Muted rose
        buttonPrimaryPressed = Color(0xFFC09098),
        buttonSecondary = Color(0xFF483840),
        buttonSecondaryPressed = Color(0xFF382830),
        accentColor = Color(0xFFD0A0A8)
    )
    
    // Midnight - Pale blue on dark
    val Midnight = GameTheme(
        name = "Midnight",
        backgroundColor = Color(0xFF181820),
        deviceColor = Color(0xFF202030),
        screenBackground = Color(0xFF283040),  // Dark blue-gray
        pixelOn = Color(0xFF8898B0),           // Muted blue
        pixelOff = Color(0xFF202838),
        textPrimary = Color(0xFFD0D8E0),
        textSecondary = Color(0xFF7080A0),
        buttonPrimary = Color(0xFF6878A0),     // Muted indigo
        buttonPrimaryPressed = Color(0xFF586890),
        buttonSecondary = Color(0xFF303848),
        buttonSecondaryPressed = Color(0xFF202838),
        accentColor = Color(0xFF8898B0)
    )
    
    // Forest - Pale green
    val Forest = GameTheme(
        name = "Forest",
        backgroundColor = Color(0xFF202820),
        deviceColor = Color(0xFF283828),
        screenBackground = Color(0xFFB8C8B0),  // Muted sage green
        pixelOn = Color(0xFF384830),           // Dark forest
        pixelOff = Color(0xFFA8B8A0),
        textPrimary = Color(0xFFE0E8E0),
        textSecondary = Color(0xFF80A080),
        buttonPrimary = Color(0xFF90B088),     // Muted green
        buttonPrimaryPressed = Color(0xFF80A078),
        buttonSecondary = Color(0xFF384038),
        buttonSecondaryPressed = Color(0xFF283028),
        accentColor = Color(0xFF90B088)
    )
    
    val allThemes = listOf(Classic, Retro, Ocean, Sakura, Midnight, Forest)
    
    fun getThemeByName(name: String): GameTheme {
        return allThemes.find { it.name == name } ?: Classic
    }
}

val LocalGameTheme = staticCompositionLocalOf { GameThemes.Classic }
