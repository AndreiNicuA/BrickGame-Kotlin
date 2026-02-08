package com.brickgame.tetris.data

import kotlinx.serialization.Serializable

/**
 * Unified player profile — all player settings in one serializable object.
 * Stored as a single JSON blob in DataStore.
 */
@Serializable
data class PlayerProfile(
    // Identity
    val name: String = "Player",
    val avatarId: String = "default",
    val createdAt: Long = System.currentTimeMillis(),

    // Game preferences
    val difficulty: String = "NORMAL",
    val ghostPieceEnabled: Boolean = true,
    val multiColorPieces: Boolean = false,

    // Theme
    val themeName: String = "Classic Green",

    // Layout
    val portraitLayout: String = "PORTRAIT_CLASSIC",
    val landscapeLayout: String = "LANDSCAPE_DEFAULT",
    val dpadStyle: String = "STANDARD",
    val activeCustomLayoutId: String? = null,

    // Freeform layout — unified element map
    val freeformElements: Map<String, FreeformElement> = defaultFreeformElements(),

    // Sound
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val soundStyle: String = "RETRO_BEEP",

    // Vibration
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Float = 0.7f,
    val vibrationStyle: String = "CLASSIC",

    // Animation
    val animationStyle: String = "MODERN",
    val animationDuration: Float = 0.5f,

    // Stats
    val highScore: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalLinesCleared: Int = 0,
    val totalPlayTimeSeconds: Long = 0,

    // Legacy compat — kept for migration, ignored after first load
    val freeformPositions: Map<String, FreeformPosition> = emptyMap(),
    val freeformInfoPositions: Map<String, FreeformPosition> = emptyMap()
) {
    companion object {
        fun defaultFreeformElements(): Map<String, FreeformElement> = mapOf(
            // Controls
            FreeformElementType.DPAD.key to FreeformElement(FreeformElementType.DPAD.key, 0.15f, 0.82f),
            FreeformElementType.ROTATE.key to FreeformElement(FreeformElementType.ROTATE.key, 0.85f, 0.82f),
            FreeformElementType.HOLD_BTN.key to FreeformElement(FreeformElementType.HOLD_BTN.key, 0.5f, 0.72f),
            FreeformElementType.PAUSE_BTN.key to FreeformElement(FreeformElementType.PAUSE_BTN.key, 0.5f, 0.78f),
            FreeformElementType.MENU_BTN.key to FreeformElement(FreeformElementType.MENU_BTN.key, 0.5f, 0.95f, size = 0.6f),
            // Info
            FreeformElementType.SCORE.key to FreeformElement(FreeformElementType.SCORE.key, 0.5f, 0.02f),
            FreeformElementType.LEVEL.key to FreeformElement(FreeformElementType.LEVEL.key, 0.15f, 0.02f),
            FreeformElementType.LINES.key to FreeformElement(FreeformElementType.LINES.key, 0.85f, 0.02f),
            FreeformElementType.HOLD_PREVIEW.key to FreeformElement(FreeformElementType.HOLD_PREVIEW.key, 0.08f, 0.07f),
            FreeformElementType.NEXT_PREVIEW.key to FreeformElement(FreeformElementType.NEXT_PREVIEW.key, 0.92f, 0.07f)
        )

        /** All available element types the user can add to their layout */
        fun availableElements(): List<FreeformElementType> = FreeformElementType.entries.toList()
    }
}

/**
 * Single freeform element — position, size, alpha, and visibility.
 */
@Serializable
data class FreeformElement(
    val key: String,
    val x: Float,                     // normalized 0-1
    val y: Float,                     // normalized 0-1
    val size: Float = 1.0f,           // scale multiplier: 0.5 = half, 2.0 = double
    val alpha: Float = 1.0f,          // transparency: 0.0 = invisible, 1.0 = opaque
    val visible: Boolean = true       // whether to render at all
)

/**
 * All possible freeform elements. Each has a key, display name, and category.
 */
enum class FreeformElementType(
    val key: String,
    val displayName: String,
    val category: ElementCategory,
    val description: String
) {
    // Compound controls
    DPAD("DPAD", "D-Pad Cross", ElementCategory.CONTROL, "4-directional cross"),
    DPAD_ROTATE("DPAD_ROTATE", "D-Pad + Rotate", ElementCategory.CONTROL, "Cross with rotate in center"),
    // Individual directional buttons
    BTN_UP("BTN_UP", "Up Button", ElementCategory.CONTROL, "Hard drop"),
    BTN_DOWN("BTN_DOWN", "Down Button", ElementCategory.CONTROL, "Soft drop"),
    BTN_LEFT("BTN_LEFT", "Left Button", ElementCategory.CONTROL, "Move left"),
    BTN_RIGHT("BTN_RIGHT", "Right Button", ElementCategory.CONTROL, "Move right"),
    // Action buttons
    ROTATE("ROTATE", "Rotate", ElementCategory.CONTROL, "Rotate piece"),
    HOLD_BTN("HOLD_BTN", "Hold", ElementCategory.CONTROL, "Hold piece"),
    PAUSE_BTN("PAUSE_BTN", "Pause/Start", ElementCategory.CONTROL, "Pause or start game"),
    MENU_BTN("MENU_BTN", "Menu ···", ElementCategory.CONTROL, "Open settings"),
    // Info elements
    SCORE("SCORE", "Score", ElementCategory.INFO, "Score display"),
    LEVEL("LEVEL", "Level", ElementCategory.INFO, "Level indicator"),
    LINES("LINES", "Lines", ElementCategory.INFO, "Lines cleared"),
    HOLD_PREVIEW("HOLD_PREVIEW", "Hold Piece", ElementCategory.INFO, "Held piece preview"),
    NEXT_PREVIEW("NEXT_PREVIEW", "Next Pieces", ElementCategory.INFO, "Next piece queue");

    companion object {
        fun fromKey(key: String): FreeformElementType? = entries.find { it.key == key }
    }
}

enum class ElementCategory { CONTROL, INFO }

/**
 * Legacy compat — still used by some code paths
 */
@Serializable
data class FreeformPosition(
    val x: Float,
    val y: Float
)
