package com.brickgame.tetris.ui.layout

import kotlinx.serialization.Serializable

/**
 * Represents a single UI element that can be repositioned by the player
 */
@Serializable
data class LayoutElement(
    val id: String,
    val label: String,
    val type: ElementType,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val widthFraction: Float = 0.25f,
    val heightFraction: Float = 0.15f,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val zone: LayoutZone = LayoutZone.LEFT
)

@Serializable
enum class ElementType {
    DPAD, ROTATE_BUTTON, HARD_DROP_BUTTON, HOLD_BUTTON,
    SCORE_PANEL, NEXT_PIECE_QUEUE, START_PAUSE_BUTTONS,
    SOUND_TOGGLE, MENU_BUTTON, ACTION_LABEL
}

@Serializable
enum class LayoutZone {
    LEFT, CENTER, RIGHT
}

/**
 * A saved layout profile containing all element positions
 */
@Serializable
data class LayoutProfile(
    val id: String = "",
    val name: String,
    val isLandscape: Boolean = true,
    val elements: List<LayoutElement>,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val isBuiltIn: Boolean = false
)

/**
 * Built-in preset layouts
 */
object LayoutPresets {
    
    fun getDefaultLandscape(): LayoutProfile = LayoutProfile(
        id = "default_landscape",
        name = "Classic",
        isLandscape = true,
        isDefault = true,
        isBuiltIn = true,
        elements = listOf(
            // Left zone
            LayoutElement("score_panel", "Score", ElementType.SCORE_PANEL, 0f, 0f, 0.22f, 0.35f, zone = LayoutZone.LEFT),
            LayoutElement("dpad", "D-Pad", ElementType.DPAD, 0f, 0.40f, 0.22f, 0.35f, zone = LayoutZone.LEFT),
            LayoutElement("hold_button", "Hold", ElementType.HOLD_BUTTON, 0f, 0.78f, 0.10f, 0.08f, zone = LayoutZone.LEFT),
            LayoutElement("start_pause", "Start/Pause", ElementType.START_PAUSE_BUTTONS, 0.12f, 0.78f, 0.10f, 0.08f, zone = LayoutZone.LEFT),
            // Center zone (game board is fixed, these are overlays)
            LayoutElement("action_label", "Action", ElementType.ACTION_LABEL, 0f, 0.02f, 0.48f, 0.06f, zone = LayoutZone.CENTER),
            LayoutElement("sound_toggle", "Sound", ElementType.SOUND_TOGGLE, 0.15f, 0.92f, 0.08f, 0.06f, zone = LayoutZone.CENTER),
            LayoutElement("menu_button", "Menu", ElementType.MENU_BUTTON, 0.26f, 0.92f, 0.08f, 0.06f, zone = LayoutZone.CENTER),
            // Right zone
            LayoutElement("next_queue", "Next", ElementType.NEXT_PIECE_QUEUE, 0f, 0f, 0.22f, 0.40f, zone = LayoutZone.RIGHT),
            LayoutElement("rotate_button", "Rotate", ElementType.ROTATE_BUTTON, 0.03f, 0.45f, 0.16f, 0.20f, zone = LayoutZone.RIGHT),
            LayoutElement("hard_drop", "Drop", ElementType.HARD_DROP_BUTTON, 0.03f, 0.70f, 0.16f, 0.12f, zone = LayoutZone.RIGHT),
        )
    )
    
    fun getDefaultPortrait(): LayoutProfile = LayoutProfile(
        id = "default_portrait",
        name = "Classic Portrait",
        isLandscape = false,
        isDefault = true,
        isBuiltIn = true,
        elements = listOf(
            LayoutElement("score_panel", "Score", ElementType.SCORE_PANEL, zone = LayoutZone.RIGHT),
            LayoutElement("next_queue", "Next", ElementType.NEXT_PIECE_QUEUE, zone = LayoutZone.RIGHT),
            LayoutElement("dpad", "D-Pad", ElementType.DPAD, zone = LayoutZone.LEFT),
            LayoutElement("rotate_button", "Rotate", ElementType.ROTATE_BUTTON, zone = LayoutZone.RIGHT),
            LayoutElement("hold_button", "Hold", ElementType.HOLD_BUTTON, zone = LayoutZone.LEFT),
            LayoutElement("hard_drop", "Drop", ElementType.HARD_DROP_BUTTON, zone = LayoutZone.RIGHT),
            LayoutElement("start_pause", "Start/Pause", ElementType.START_PAUSE_BUTTONS, zone = LayoutZone.CENTER),
            LayoutElement("menu_button", "Menu", ElementType.MENU_BUTTON, zone = LayoutZone.CENTER),
        )
    )
    
    fun getLeftyLandscape(): LayoutProfile = LayoutProfile(
        id = "lefty_landscape",
        name = "Lefty",
        isLandscape = true,
        isBuiltIn = true,
        elements = listOf(
            // Swap sides: rotate/drop on left, dpad on right
            LayoutElement("next_queue", "Next", ElementType.NEXT_PIECE_QUEUE, 0f, 0f, 0.22f, 0.40f, zone = LayoutZone.LEFT),
            LayoutElement("rotate_button", "Rotate", ElementType.ROTATE_BUTTON, 0.03f, 0.45f, 0.16f, 0.20f, zone = LayoutZone.LEFT),
            LayoutElement("hard_drop", "Drop", ElementType.HARD_DROP_BUTTON, 0.03f, 0.70f, 0.16f, 0.12f, zone = LayoutZone.LEFT),
            LayoutElement("action_label", "Action", ElementType.ACTION_LABEL, 0f, 0.02f, 0.48f, 0.06f, zone = LayoutZone.CENTER),
            LayoutElement("sound_toggle", "Sound", ElementType.SOUND_TOGGLE, 0.15f, 0.92f, 0.08f, 0.06f, zone = LayoutZone.CENTER),
            LayoutElement("menu_button", "Menu", ElementType.MENU_BUTTON, 0.26f, 0.92f, 0.08f, 0.06f, zone = LayoutZone.CENTER),
            LayoutElement("score_panel", "Score", ElementType.SCORE_PANEL, 0f, 0f, 0.22f, 0.35f, zone = LayoutZone.RIGHT),
            LayoutElement("dpad", "D-Pad", ElementType.DPAD, 0f, 0.40f, 0.22f, 0.35f, zone = LayoutZone.RIGHT),
            LayoutElement("hold_button", "Hold", ElementType.HOLD_BUTTON, 0f, 0.78f, 0.10f, 0.08f, zone = LayoutZone.RIGHT),
            LayoutElement("start_pause", "Start/Pause", ElementType.START_PAUSE_BUTTONS, 0.12f, 0.78f, 0.10f, 0.08f, zone = LayoutZone.RIGHT),
        )
    )
    
    fun getCompactLandscape(): LayoutProfile = LayoutProfile(
        id = "compact_landscape",
        name = "Compact",
        isLandscape = true,
        isBuiltIn = true,
        elements = listOf(
            LayoutElement("score_panel", "Score", ElementType.SCORE_PANEL, 0f, 0f, 0.20f, 0.25f, zone = LayoutZone.LEFT),
            LayoutElement("hold_button", "Hold", ElementType.HOLD_BUTTON, 0f, 0.28f, 0.10f, 0.08f, zone = LayoutZone.LEFT),
            LayoutElement("dpad", "D-Pad", ElementType.DPAD, 0f, 0.40f, 0.20f, 0.35f, zone = LayoutZone.LEFT),
            LayoutElement("start_pause", "Start/Pause", ElementType.START_PAUSE_BUTTONS, 0f, 0.80f, 0.20f, 0.08f, zone = LayoutZone.LEFT),
            LayoutElement("action_label", "Action", ElementType.ACTION_LABEL, 0f, 0.02f, 0.48f, 0.06f, zone = LayoutZone.CENTER),
            LayoutElement("menu_button", "Menu", ElementType.MENU_BUTTON, 0.20f, 0.92f, 0.08f, 0.06f, zone = LayoutZone.CENTER),
            LayoutElement("sound_toggle", "Sound", ElementType.SOUND_TOGGLE, 0.35f, 0.92f, 0.08f, 0.06f, zone = LayoutZone.CENTER),
            LayoutElement("next_queue", "Next", ElementType.NEXT_PIECE_QUEUE, 0f, 0f, 0.20f, 0.35f, zone = LayoutZone.RIGHT),
            LayoutElement("rotate_button", "Rotate", ElementType.ROTATE_BUTTON, 0.02f, 0.40f, 0.16f, 0.22f, zone = LayoutZone.RIGHT),
            LayoutElement("hard_drop", "Drop", ElementType.HARD_DROP_BUTTON, 0.02f, 0.68f, 0.16f, 0.14f, zone = LayoutZone.RIGHT),
        )
    )
    
    fun getAllPresets(): List<LayoutProfile> = listOf(
        getDefaultLandscape(),
        getLeftyLandscape(),
        getCompactLandscape(),
        getDefaultPortrait()
    )
}
