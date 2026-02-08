package com.brickgame.tetris.ui.layout

import kotlinx.serialization.Serializable

/**
 * Identifies which layout preset to use.
 * v3.0: Fixed presets only. v3.3 will add custom profiles.
 */
enum class LayoutPreset(val displayName: String, val isLandscape: Boolean) {
    // Portrait
    PORTRAIT_CLASSIC("Classic", false),
    PORTRAIT_MODERN("Modern", false),
    PORTRAIT_FULLSCREEN("Fullscreen", false),
    PORTRAIT_ONEHAND("One Hand", false),
    // Landscape
    LANDSCAPE_DEFAULT("Default", true),
    LANDSCAPE_LEFTY("Lefty", true);

    companion object {
        fun portraitPresets() = entries.filter { !it.isLandscape }
        fun landscapePresets() = entries.filter { it.isLandscape }
    }
}

/**
 * D-Pad style selection
 * STANDARD: 4 directional buttons with decorative centre
 * ROTATE_CENTRE: 4 directional buttons with functional rotate button in centre
 */
enum class DPadStyle(val displayName: String) {
    STANDARD("Standard Cross"),
    ROTATE_CENTRE("Cross + Rotate Centre")
}
