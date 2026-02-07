package com.brickgame.tetris.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Complete visual theme defining every colour in the app.
 * Built-in themes cannot be deleted. Custom themes are user-created.
 */
data class GameTheme(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = true,
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

val LocalGameTheme = compositionLocalOf { GameThemes.ClassicGreen }

object GameThemes {

    val ClassicGreen = GameTheme(
        id = "builtin_classic_green",
        name = "Classic Green",
        backgroundColor = Color(0xFF2A2A2A),
        deviceColor = Color(0xFF4A5A3A),
        screenBackground = Color(0xFFB8C4A8),
        pixelOn = Color(0xFF3D4A32),
        pixelOff = Color(0xFFA8B498),
        textPrimary = Color(0xFFE8E8E8),
        textSecondary = Color(0xFF9A9A9A),
        buttonPrimary = Color(0xFFD4C896),
        buttonPrimaryPressed = Color(0xFFBFB682),
        buttonSecondary = Color(0xFF4A4A4A),
        buttonSecondaryPressed = Color(0xFF3A3A3A),
        accentColor = Color(0xFFD4C896)
    )

    val Midnight = GameTheme(
        id = "builtin_midnight",
        name = "Midnight",
        backgroundColor = Color(0xFF0A0A14),
        deviceColor = Color(0xFF181828),
        screenBackground = Color(0xFF1A2030),
        pixelOn = Color(0xFF8AB4F8),
        pixelOff = Color(0xFF151C28),
        textPrimary = Color(0xFFD0D8E8),
        textSecondary = Color(0xFF6878A0),
        buttonPrimary = Color(0xFF4A6090),
        buttonPrimaryPressed = Color(0xFF3A5080),
        buttonSecondary = Color(0xFF2A3040),
        buttonSecondaryPressed = Color(0xFF1A2030),
        accentColor = Color(0xFF8AB4F8)
    )

    val RetroAmber = GameTheme(
        id = "builtin_retro_amber",
        name = "Retro Amber",
        backgroundColor = Color(0xFF1A1408),
        deviceColor = Color(0xFF2A2010),
        screenBackground = Color(0xFF201800),
        pixelOn = Color(0xFFFF9800),
        pixelOff = Color(0xFF1A1200),
        textPrimary = Color(0xFFE8D0A0),
        textSecondary = Color(0xFF8A7040),
        buttonPrimary = Color(0xFFCAAA58),
        buttonPrimaryPressed = Color(0xFFB89848),
        buttonSecondary = Color(0xFF3A3020),
        buttonSecondaryPressed = Color(0xFF2A2010),
        accentColor = Color(0xFFFF9800)
    )

    val Neon = GameTheme(
        id = "builtin_neon",
        name = "Neon",
        backgroundColor = Color(0xFF050508),
        deviceColor = Color(0xFF0A0A10),
        screenBackground = Color(0xFF080810),
        pixelOn = Color(0xFF00FF88),
        pixelOff = Color(0xFF0A0F0A),
        textPrimary = Color(0xFFE0FFE0),
        textSecondary = Color(0xFF40A060),
        buttonPrimary = Color(0xFF00CC66),
        buttonPrimaryPressed = Color(0xFF00AA55),
        buttonSecondary = Color(0xFF1A1A2A),
        buttonSecondaryPressed = Color(0xFF0A0A1A),
        accentColor = Color(0xFF00FF88)
    )

    val Paper = GameTheme(
        id = "builtin_paper",
        name = "Paper",
        backgroundColor = Color(0xFFF5F0E8),
        deviceColor = Color(0xFFE8E0D0),
        screenBackground = Color(0xFFFAF6F0),
        pixelOn = Color(0xFF3A3530),
        pixelOff = Color(0xFFE8E0D8),
        textPrimary = Color(0xFF2A2520),
        textSecondary = Color(0xFF8A8078),
        buttonPrimary = Color(0xFFB0A898),
        buttonPrimaryPressed = Color(0xFF9A9288),
        buttonSecondary = Color(0xFFD8D0C0),
        buttonSecondaryPressed = Color(0xFFC8C0B0),
        accentColor = Color(0xFFE07030)
    )

    val allThemes = listOf(ClassicGreen, Midnight, RetroAmber, Neon, Paper)

    fun getThemeById(id: String): GameTheme =
        allThemes.find { it.id == id } ?: ClassicGreen

    fun getThemeByName(name: String): GameTheme =
        allThemes.find { it.name == name } ?: ClassicGreen
}
