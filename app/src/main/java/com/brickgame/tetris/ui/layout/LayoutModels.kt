package com.brickgame.tetris.ui.layout

import kotlinx.serialization.Serializable

/**
 * Every movable UI element on screen.
 * Position/size are fractions of the full screen (0.0 to 1.0).
 */
@Serializable
data class LayoutElement(
    val id: String,
    val label: String,
    val type: ElementType,
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0.20f,
    val h: Float = 0.15f,
    val isVisible: Boolean = true
) {
    fun overlaps(other: LayoutElement): Boolean {
        if (!isVisible || !other.isVisible) return false
        if (id == other.id) return false
        val m = 0.003f
        return x < other.x + other.w - m &&
               x + w > other.x + m &&
               y < other.y + other.h - m &&
               y + h > other.y + m
    }

    fun clampToScreen(): LayoutElement = copy(
        x = x.coerceIn(0f, (1f - w).coerceAtLeast(0f)),
        y = y.coerceIn(0f, (1f - h).coerceAtLeast(0f))
    )

    fun clampSize(): LayoutElement {
        val (minW, minH) = type.minSize
        val (maxW, maxH) = type.maxSize
        return copy(
            w = w.coerceIn(minW, maxW),
            h = h.coerceIn(minH, maxH)
        )
    }
}

@Serializable
enum class ElementType(
    val minSize: Pair<Float, Float>,
    val maxSize: Pair<Float, Float>
) {
    GAME_BOARD(      Pair(0.15f, 0.30f), Pair(0.60f, 0.95f)),
    SCORE_PANEL(     Pair(0.10f, 0.04f), Pair(0.35f, 0.35f)),
    HOLD_PREVIEW(    Pair(0.05f, 0.04f), Pair(0.20f, 0.20f)),
    NEXT_PIECE_QUEUE(Pair(0.05f, 0.06f), Pair(0.25f, 0.50f)),
    DPAD(            Pair(0.15f, 0.18f), Pair(0.45f, 0.55f)),
    ROTATE_BUTTON(   Pair(0.06f, 0.06f), Pair(0.22f, 0.25f)),
    HOLD_BUTTON(     Pair(0.05f, 0.03f), Pair(0.22f, 0.10f)),
    START_BUTTON(    Pair(0.05f, 0.03f), Pair(0.22f, 0.10f)),
    PAUSE_BUTTON(    Pair(0.04f, 0.03f), Pair(0.15f, 0.10f)),
    SOUND_TOGGLE(    Pair(0.04f, 0.03f), Pair(0.12f, 0.10f)),
    MENU_BUTTON(     Pair(0.04f, 0.03f), Pair(0.12f, 0.10f)),
    ACTION_LABEL(    Pair(0.10f, 0.02f), Pair(0.50f, 0.08f));
}

@Serializable
data class LayoutProfile(
    val id: String = "",
    val name: String,
    val isLandscape: Boolean = true,
    val elements: List<LayoutElement>,
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false
) {
    fun hasOverlaps(): Boolean {
        val vis = elements.filter { it.isVisible }
        for (i in vis.indices) for (j in i + 1 until vis.size)
            if (vis[i].overlaps(vis[j])) return true
        return false
    }

    fun findOverlaps(): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val vis = elements.filter { it.isVisible }
        for (i in vis.indices) for (j in i + 1 until vis.size)
            if (vis[i].overlaps(vis[j])) out.add(vis[i].id to vis[j].id)
        return out
    }
}

object LayoutPresets {

    val requiredElementIds = listOf(
        "game_board", "score_panel", "hold_preview", "next_queue",
        "dpad", "rotate_button", "hold_button", "start_button",
        "pause_button", "sound_toggle", "menu_button", "action_label"
    )

    // ── LANDSCAPE ──

    fun getDefaultLandscape(): LayoutProfile = LayoutProfile(
        id = "builtin_landscape_default", name = "Default",
        isLandscape = true, isBuiltIn = true,
        elements = listOf(
            LayoutElement("game_board",    "Game Board",   ElementType.GAME_BOARD,       0.30f, 0.04f, 0.40f, 0.92f),
            LayoutElement("action_label",  "Action Label", ElementType.ACTION_LABEL,     0.32f, 0.00f, 0.36f, 0.04f),
            LayoutElement("dpad",          "D-Pad",        ElementType.DPAD,             0.01f, 0.20f, 0.27f, 0.54f),
            LayoutElement("hold_button",   "Hold",         ElementType.HOLD_BUTTON,      0.01f, 0.02f, 0.12f, 0.07f),
            LayoutElement("start_button",  "Start",        ElementType.START_BUTTON,     0.01f, 0.80f, 0.14f, 0.07f),
            LayoutElement("pause_button",  "Pause",        ElementType.PAUSE_BUTTON,     0.16f, 0.80f, 0.12f, 0.07f),
            LayoutElement("score_panel",   "Score",        ElementType.SCORE_PANEL,      0.72f, 0.02f, 0.27f, 0.22f),
            LayoutElement("hold_preview",  "Hold Preview", ElementType.HOLD_PREVIEW,     0.72f, 0.26f, 0.12f, 0.16f),
            LayoutElement("next_queue",    "Next Queue",   ElementType.NEXT_PIECE_QUEUE, 0.85f, 0.26f, 0.14f, 0.40f),
            LayoutElement("rotate_button", "Rotate",       ElementType.ROTATE_BUTTON,    0.75f, 0.70f, 0.14f, 0.18f),
            LayoutElement("sound_toggle",  "Sound",        ElementType.SOUND_TOGGLE,     0.72f, 0.91f, 0.06f, 0.06f),
            LayoutElement("menu_button",   "Menu",         ElementType.MENU_BUTTON,      0.80f, 0.91f, 0.06f, 0.06f),
        )
    )

    fun getLeftyLandscape(): LayoutProfile = LayoutProfile(
        id = "builtin_landscape_lefty", name = "Lefty",
        isLandscape = true, isBuiltIn = true,
        elements = listOf(
            LayoutElement("game_board",    "Game Board",   ElementType.GAME_BOARD,       0.30f, 0.04f, 0.40f, 0.92f),
            LayoutElement("action_label",  "Action Label", ElementType.ACTION_LABEL,     0.32f, 0.00f, 0.36f, 0.04f),
            LayoutElement("score_panel",   "Score",        ElementType.SCORE_PANEL,      0.01f, 0.02f, 0.27f, 0.22f),
            LayoutElement("hold_preview",  "Hold Preview", ElementType.HOLD_PREVIEW,     0.01f, 0.26f, 0.12f, 0.16f),
            LayoutElement("next_queue",    "Next Queue",   ElementType.NEXT_PIECE_QUEUE, 0.14f, 0.26f, 0.14f, 0.40f),
            LayoutElement("rotate_button", "Rotate",       ElementType.ROTATE_BUTTON,    0.04f, 0.70f, 0.14f, 0.18f),
            LayoutElement("sound_toggle",  "Sound",        ElementType.SOUND_TOGGLE,     0.01f, 0.91f, 0.06f, 0.06f),
            LayoutElement("menu_button",   "Menu",         ElementType.MENU_BUTTON,      0.08f, 0.91f, 0.06f, 0.06f),
            LayoutElement("dpad",          "D-Pad",        ElementType.DPAD,             0.72f, 0.20f, 0.27f, 0.54f),
            LayoutElement("hold_button",   "Hold",         ElementType.HOLD_BUTTON,      0.72f, 0.02f, 0.12f, 0.07f),
            LayoutElement("start_button",  "Start",        ElementType.START_BUTTON,     0.72f, 0.80f, 0.14f, 0.07f),
            LayoutElement("pause_button",  "Pause",        ElementType.PAUSE_BUTTON,     0.87f, 0.80f, 0.12f, 0.07f),
        )
    )

    // ── PORTRAIT ──

    fun getClassicPortrait(): LayoutProfile = LayoutProfile(
        id = "builtin_portrait_classic", name = "Classic",
        isLandscape = false, isBuiltIn = true,
        elements = listOf(
            LayoutElement("game_board",    "Game Board",   ElementType.GAME_BOARD,       0.04f, 0.02f, 0.58f, 0.50f),
            LayoutElement("score_panel",   "Score",        ElementType.SCORE_PANEL,      0.64f, 0.02f, 0.32f, 0.16f),
            LayoutElement("hold_preview",  "Hold Preview", ElementType.HOLD_PREVIEW,     0.64f, 0.20f, 0.14f, 0.10f),
            LayoutElement("next_queue",    "Next Queue",   ElementType.NEXT_PIECE_QUEUE, 0.64f, 0.32f, 0.14f, 0.20f),
            LayoutElement("action_label",  "Action Label", ElementType.ACTION_LABEL,     0.80f, 0.44f, 0.18f, 0.04f),
            LayoutElement("hold_button",   "Hold",         ElementType.HOLD_BUTTON,      0.04f, 0.55f, 0.18f, 0.05f),
            LayoutElement("start_button",  "Start",        ElementType.START_BUTTON,     0.28f, 0.55f, 0.18f, 0.05f),
            LayoutElement("pause_button",  "Pause",        ElementType.PAUSE_BUTTON,     0.52f, 0.55f, 0.18f, 0.05f),
            LayoutElement("menu_button",   "Menu",         ElementType.MENU_BUTTON,      0.76f, 0.55f, 0.14f, 0.05f),
            LayoutElement("dpad",          "D-Pad",        ElementType.DPAD,             0.02f, 0.63f, 0.44f, 0.35f),
            LayoutElement("rotate_button", "Rotate",       ElementType.ROTATE_BUTTON,    0.66f, 0.68f, 0.20f, 0.16f),
            LayoutElement("sound_toggle",  "Sound",        ElementType.SOUND_TOGGLE,     0.50f, 0.68f, 0.10f, 0.06f),
        )
    )

    fun getModernPortrait(): LayoutProfile = LayoutProfile(
        id = "builtin_portrait_modern", name = "Modern",
        isLandscape = false, isBuiltIn = true,
        elements = listOf(
            LayoutElement("score_panel",   "Score",        ElementType.SCORE_PANEL,      0.22f, 0.01f, 0.28f, 0.06f),
            LayoutElement("hold_preview",  "Hold Preview", ElementType.HOLD_PREVIEW,     0.02f, 0.01f, 0.14f, 0.06f),
            LayoutElement("next_queue",    "Next Queue",   ElementType.NEXT_PIECE_QUEUE, 0.68f, 0.01f, 0.30f, 0.06f),
            LayoutElement("action_label",  "Action Label", ElementType.ACTION_LABEL,     0.20f, 0.08f, 0.50f, 0.03f),
            LayoutElement("game_board",    "Game Board",   ElementType.GAME_BOARD,       0.12f, 0.12f, 0.76f, 0.50f),
            LayoutElement("hold_button",   "Hold",         ElementType.HOLD_BUTTON,      0.02f, 0.64f, 0.16f, 0.04f),
            LayoutElement("start_button",  "Start",        ElementType.START_BUTTON,     0.22f, 0.64f, 0.18f, 0.04f),
            LayoutElement("pause_button",  "Pause",        ElementType.PAUSE_BUTTON,     0.44f, 0.64f, 0.18f, 0.04f),
            LayoutElement("menu_button",   "Menu",         ElementType.MENU_BUTTON,      0.66f, 0.64f, 0.12f, 0.04f),
            LayoutElement("dpad",          "D-Pad",        ElementType.DPAD,             0.02f, 0.70f, 0.44f, 0.28f),
            LayoutElement("rotate_button", "Rotate",       ElementType.ROTATE_BUTTON,    0.66f, 0.74f, 0.20f, 0.16f),
            LayoutElement("sound_toggle",  "Sound",        ElementType.SOUND_TOGGLE,     0.50f, 0.74f, 0.10f, 0.06f),
        )
    )

    fun getFullscreenPortrait(): LayoutProfile = LayoutProfile(
        id = "builtin_portrait_fullscreen", name = "Fullscreen",
        isLandscape = false, isBuiltIn = true,
        elements = listOf(
            LayoutElement("hold_preview",  "Hold Preview", ElementType.HOLD_PREVIEW,     0.02f, 0.01f, 0.10f, 0.04f),
            LayoutElement("score_panel",   "Score",        ElementType.SCORE_PANEL,      0.16f, 0.01f, 0.30f, 0.04f),
            LayoutElement("next_queue",    "Next Queue",   ElementType.NEXT_PIECE_QUEUE, 0.78f, 0.01f, 0.14f, 0.04f),
            LayoutElement("action_label",  "Action Label", ElementType.ACTION_LABEL,     0.20f, 0.06f, 0.50f, 0.03f),
            LayoutElement("game_board",    "Game Board",   ElementType.GAME_BOARD,       0.08f, 0.10f, 0.84f, 0.58f),
            LayoutElement("dpad",          "D-Pad",        ElementType.DPAD,             0.01f, 0.70f, 0.40f, 0.28f),
            LayoutElement("hold_button",   "Hold",         ElementType.HOLD_BUTTON,      0.52f, 0.70f, 0.14f, 0.05f),
            LayoutElement("start_button",  "Start",        ElementType.START_BUTTON,     0.52f, 0.77f, 0.10f, 0.04f),
            LayoutElement("pause_button",  "Pause",        ElementType.PAUSE_BUTTON,     0.64f, 0.77f, 0.10f, 0.04f),
            LayoutElement("menu_button",   "Menu",         ElementType.MENU_BUTTON,      0.76f, 0.77f, 0.10f, 0.04f),
            LayoutElement("rotate_button", "Rotate",       ElementType.ROTATE_BUTTON,    0.70f, 0.70f, 0.16f, 0.14f),
            LayoutElement("sound_toggle",  "Sound",        ElementType.SOUND_TOGGLE,     0.88f, 0.70f, 0.10f, 0.06f),
        )
    )

    fun landscapePresets(): List<LayoutProfile> = listOf(getDefaultLandscape(), getLeftyLandscape())
    fun portraitPresets(): List<LayoutProfile> = listOf(getClassicPortrait(), getModernPortrait(), getFullscreenPortrait())
    fun getAllPresets(): List<LayoutProfile> = landscapePresets() + portraitPresets()

    fun fromTemplate(preset: LayoutProfile, newName: String): LayoutProfile = preset.copy(
        id = "custom_${System.currentTimeMillis()}",
        name = newName,
        isBuiltIn = false,
        createdAt = System.currentTimeMillis()
    )
}
