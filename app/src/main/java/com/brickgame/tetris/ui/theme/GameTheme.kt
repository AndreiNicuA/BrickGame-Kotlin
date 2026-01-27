package com.brickgame.tetris.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Game Theme - Defines all visual aspects of the game
 * Supports multiple themes: Classic, Neon, Retro, Ocean, Forest
 */
data class GameTheme(
    val name: String,
    val deviceColor: Color,
    val deviceBorderColor: Color,
    val screenBackground: Color,
    val pixelOn: Color,
    val pixelOff: Color,
    val pixelBorder: Color,
    val buttonPrimary: Color,
    val buttonPrimaryPressed: Color,
    val buttonSecondary: Color,
    val buttonSecondaryPressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentColor: Color,
    val decoColor1: Color,
    val decoColor2: Color,
    val backgroundColor: Color
)

object GameThemes {
    
    // Classic green LCD theme (original Brick Game look)
    val Classic = GameTheme(
        name = "Classic",
        deviceColor = Color(0xFF1A1A1A),
        deviceBorderColor = Color(0xFF0A0A0A),
        screenBackground = Color(0xFF9EAD86),
        pixelOn = Color(0xFF1A1A1A),
        pixelOff = Color(0xFF8A9A78),
        pixelBorder = Color(0xFF7A8A68),
        buttonPrimary = Color(0xFFF4D03F),
        buttonPrimaryPressed = Color(0xFFC4A000),
        buttonSecondary = Color(0xFF555555),
        buttonSecondaryPressed = Color(0xFF333333),
        textPrimary = Color(0xFF1A1A1A),
        textSecondary = Color(0xFF666666),
        accentColor = Color(0xFFF4D03F),
        decoColor1 = Color(0xFF1E90FF),
        decoColor2 = Color(0xFFDC143C),
        backgroundColor = Color(0xFF2D2D2D)
    )
    
    // Neon cyberpunk theme
    val Neon = GameTheme(
        name = "Neon",
        deviceColor = Color(0xFF0D0D0D),
        deviceBorderColor = Color(0xFF00FFFF),
        screenBackground = Color(0xFF0A0A1A),
        pixelOn = Color(0xFF00FFFF),
        pixelOff = Color(0xFF1A1A2E),
        pixelBorder = Color(0xFF0F0F1F),
        buttonPrimary = Color(0xFFFF00FF),
        buttonPrimaryPressed = Color(0xFFCC00CC),
        buttonSecondary = Color(0xFF2A2A4A),
        buttonSecondaryPressed = Color(0xFF1A1A3A),
        textPrimary = Color(0xFF00FFFF),
        textSecondary = Color(0xFFFF00FF),
        accentColor = Color(0xFFFFFF00),
        decoColor1 = Color(0xFF00FFFF),
        decoColor2 = Color(0xFFFF00FF),
        backgroundColor = Color(0xFF050510)
    )
    
    // Retro orange/brown theme
    val Retro = GameTheme(
        name = "Retro",
        deviceColor = Color(0xFF3D2817),
        deviceBorderColor = Color(0xFF2A1A0F),
        screenBackground = Color(0xFFD4A574),
        pixelOn = Color(0xFF2A1A0F),
        pixelOff = Color(0xFFC49464),
        pixelBorder = Color(0xFFB48454),
        buttonPrimary = Color(0xFFFF6B35),
        buttonPrimaryPressed = Color(0xFFCC5528),
        buttonSecondary = Color(0xFF5D4037),
        buttonSecondaryPressed = Color(0xFF3E2723),
        textPrimary = Color(0xFF2A1A0F),
        textSecondary = Color(0xFF5D4037),
        accentColor = Color(0xFFFF6B35),
        decoColor1 = Color(0xFFFF6B35),
        decoColor2 = Color(0xFFFFD93D),
        backgroundColor = Color(0xFF1A0F07)
    )
    
    // Ocean blue theme
    val Ocean = GameTheme(
        name = "Ocean",
        deviceColor = Color(0xFF0A1628),
        deviceBorderColor = Color(0xFF1E3A5F),
        screenBackground = Color(0xFF87CEEB),
        pixelOn = Color(0xFF0A1628),
        pixelOff = Color(0xFF6BB8DB),
        pixelBorder = Color(0xFF5BA8CB),
        buttonPrimary = Color(0xFF00CED1),
        buttonPrimaryPressed = Color(0xFF00A5A8),
        buttonSecondary = Color(0xFF1E3A5F),
        buttonSecondaryPressed = Color(0xFF0F1F3A),
        textPrimary = Color(0xFF0A1628),
        textSecondary = Color(0xFF1E3A5F),
        accentColor = Color(0xFF00CED1),
        decoColor1 = Color(0xFF00CED1),
        decoColor2 = Color(0xFF4169E1),
        backgroundColor = Color(0xFF050A14)
    )
    
    // Forest green theme
    val Forest = GameTheme(
        name = "Forest",
        deviceColor = Color(0xFF1A2F1A),
        deviceBorderColor = Color(0xFF0F1F0F),
        screenBackground = Color(0xFFA8D5A2),
        pixelOn = Color(0xFF1A2F1A),
        pixelOff = Color(0xFF88C582),
        pixelBorder = Color(0xFF78B572),
        buttonPrimary = Color(0xFF4CAF50),
        buttonPrimaryPressed = Color(0xFF388E3C),
        buttonSecondary = Color(0xFF2E5930),
        buttonSecondaryPressed = Color(0xFF1E3920),
        textPrimary = Color(0xFF1A2F1A),
        textSecondary = Color(0xFF2E5930),
        accentColor = Color(0xFF8BC34A),
        decoColor1 = Color(0xFF4CAF50),
        decoColor2 = Color(0xFF8BC34A),
        backgroundColor = Color(0xFF0F170F)
    )
    
    // Midnight dark theme
    val Midnight = GameTheme(
        name = "Midnight",
        deviceColor = Color(0xFF121212),
        deviceBorderColor = Color(0xFF2D2D2D),
        screenBackground = Color(0xFF1E1E1E),
        pixelOn = Color(0xFFE0E0E0),
        pixelOff = Color(0xFF2D2D2D),
        pixelBorder = Color(0xFF252525),
        buttonPrimary = Color(0xFF6200EE),
        buttonPrimaryPressed = Color(0xFF3700B3),
        buttonSecondary = Color(0xFF373737),
        buttonSecondaryPressed = Color(0xFF252525),
        textPrimary = Color(0xFFE0E0E0),
        textSecondary = Color(0xFF9E9E9E),
        accentColor = Color(0xFFBB86FC),
        decoColor1 = Color(0xFF6200EE),
        decoColor2 = Color(0xFFBB86FC),
        backgroundColor = Color(0xFF000000)
    )
    
    val allThemes = listOf(Classic, Neon, Retro, Ocean, Forest, Midnight)
    
    fun getThemeByName(name: String): GameTheme {
        return allThemes.find { it.name == name } ?: Classic
    }
}
