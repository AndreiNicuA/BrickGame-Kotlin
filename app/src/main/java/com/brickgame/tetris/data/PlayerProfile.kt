package com.brickgame.tetris.data

import kotlinx.serialization.Serializable

/**
 * Unified player profile — all player settings in one serializable object.
 * Stored as a single JSON blob in DataStore.
 * 
 * Existing SettingsRepository keys remain untouched for backward compatibility.
 * PlayerProfile is the new "source of truth" going forward, synced on save.
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
    
    // Freeform layout positions (normalized 0-1)
    val freeformPositions: Map<String, FreeformPosition> = defaultFreeformPositions(),
    val freeformInfoPositions: Map<String, FreeformPosition> = defaultFreeformInfoPositions(),
    
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
    val totalPlayTimeSeconds: Long = 0
) {
    companion object {
        /**
         * Default freeform positions for game controls.
         * Normalized coordinates: 0.0 = top/left, 1.0 = bottom/right.
         */
        fun defaultFreeformPositions(): Map<String, FreeformPosition> = mapOf(
            "DPAD" to FreeformPosition(0.15f, 0.82f),
            "ROTATE_BTN" to FreeformPosition(0.85f, 0.82f),
            "HOLD_BTN" to FreeformPosition(0.5f, 0.72f),
            "PAUSE_BTN" to FreeformPosition(0.5f, 0.78f),
            "MENU_BTN" to FreeformPosition(0.5f, 0.95f),
            "HARD_DROP_BTN" to FreeformPosition(0.85f, 0.72f)
        )
        
        /**
         * Default freeform positions for info elements (score, level, etc.)
         */
        fun defaultFreeformInfoPositions(): Map<String, FreeformPosition> = mapOf(
            "SCORE" to FreeformPosition(0.5f, 0.02f),
            "LEVEL" to FreeformPosition(0.15f, 0.02f),
            "LINES" to FreeformPosition(0.85f, 0.02f),
            "HOLD_PREVIEW" to FreeformPosition(0.08f, 0.08f),
            "NEXT_PREVIEW" to FreeformPosition(0.92f, 0.08f)
        )
    }
}

/**
 * Position for a freeform-draggable UI element.
 * x/y are normalized (0.0 to 1.0) relative to the screen/container.
 */
@Serializable
data class FreeformPosition(
    val x: Float,
    val y: Float
)
