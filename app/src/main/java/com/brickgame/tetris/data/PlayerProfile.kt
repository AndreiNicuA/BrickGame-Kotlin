package com.brickgame.tetris.data

import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfile(
    val name: String = "Player",
    val avatarId: String = "default",
    val createdAt: Long = System.currentTimeMillis(),
    val difficulty: String = "NORMAL",
    val ghostPieceEnabled: Boolean = true,
    val multiColorPieces: Boolean = false,
    val themeName: String = "Classic Green",
    val portraitLayout: String = "PORTRAIT_CLASSIC",
    val landscapeLayout: String = "LANDSCAPE_DEFAULT",
    val dpadStyle: String = "STANDARD",
    val activeCustomLayoutId: String? = null,
    val freeformElements: Map<String, FreeformElement> = defaultFreeformElements(),
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val soundStyle: String = "RETRO_BEEP",
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Float = 0.7f,
    val vibrationStyle: String = "CLASSIC",
    val animationStyle: String = "MODERN",
    val animationDuration: Float = 0.5f,
    val highScore: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalLinesCleared: Int = 0,
    val totalPlayTimeSeconds: Long = 0,
    // Legacy compat
    val freeformPositions: Map<String, FreeformPosition> = emptyMap(),
    val freeformInfoPositions: Map<String, FreeformPosition> = emptyMap()
) {
    companion object {
        fun defaultFreeformElements(): Map<String, FreeformElement> = mapOf(
            // Board
            "BOARD" to FreeformElement("BOARD", 0.5f, 0.38f, size = 0.85f),
            // Controls
            "DPAD" to FreeformElement("DPAD", 0.15f, 0.82f),
            "ROTATE" to FreeformElement("ROTATE", 0.85f, 0.82f),
            "HOLD_BTN" to FreeformElement("HOLD_BTN", 0.5f, 0.72f),
            "PAUSE_BTN" to FreeformElement("PAUSE_BTN", 0.5f, 0.78f),
            "MENU_BTN" to FreeformElement("MENU_BTN", 0.5f, 0.95f, size = 0.6f),
            // Info
            "SCORE" to FreeformElement("SCORE", 0.5f, 0.02f),
            "LEVEL" to FreeformElement("LEVEL", 0.15f, 0.02f),
            "LINES" to FreeformElement("LINES", 0.85f, 0.02f),
            "HOLD_PREVIEW" to FreeformElement("HOLD_PREVIEW", 0.08f, 0.07f),
            "NEXT_PREVIEW" to FreeformElement("NEXT_PREVIEW", 0.92f, 0.07f)
        )

        fun availableElements(): List<FreeformElementType> = FreeformElementType.entries.toList()
    }
}

@Serializable
data class FreeformElement(
    val key: String,
    val x: Float,
    val y: Float,
    val size: Float = 1.0f,
    val alpha: Float = 1.0f,
    val visible: Boolean = true
)

enum class FreeformElementType(
    val key: String,
    val displayName: String,
    val category: ElementCategory,
    val description: String
) {
    BOARD("BOARD", "Game Board", ElementCategory.BOARD, "The LCD game screen"),
    DPAD("DPAD", "D-Pad Cross", ElementCategory.CONTROL, "4-directional cross"),
    DPAD_ROTATE("DPAD_ROTATE", "D-Pad + Rotate", ElementCategory.CONTROL, "Cross with rotate center"),
    BTN_UP("BTN_UP", "Up Button", ElementCategory.CONTROL, "Hard drop"),
    BTN_DOWN("BTN_DOWN", "Down Button", ElementCategory.CONTROL, "Soft drop"),
    BTN_LEFT("BTN_LEFT", "Left Button", ElementCategory.CONTROL, "Move left"),
    BTN_RIGHT("BTN_RIGHT", "Right Button", ElementCategory.CONTROL, "Move right"),
    ROTATE("ROTATE", "Rotate", ElementCategory.CONTROL, "Rotate piece"),
    HOLD_BTN("HOLD_BTN", "Hold", ElementCategory.CONTROL, "Hold piece"),
    PAUSE_BTN("PAUSE_BTN", "Pause/Start", ElementCategory.CONTROL, "Pause or start"),
    MENU_BTN("MENU_BTN", "Menu ···", ElementCategory.CONTROL, "Open settings"),
    SCORE("SCORE", "Score", ElementCategory.INFO, "Score display"),
    LEVEL("LEVEL", "Level", ElementCategory.INFO, "Level indicator"),
    LINES("LINES", "Lines", ElementCategory.INFO, "Lines cleared"),
    HOLD_PREVIEW("HOLD_PREVIEW", "Hold Piece", ElementCategory.INFO, "Held piece preview"),
    NEXT_PREVIEW("NEXT_PREVIEW", "Next Pieces", ElementCategory.INFO, "Next piece queue");

    companion object {
        fun fromKey(key: String): FreeformElementType? = entries.find { it.key == key }
    }
}

enum class ElementCategory { CONTROL, INFO, BOARD }

@Serializable
data class FreeformPosition(val x: Float, val y: Float)
