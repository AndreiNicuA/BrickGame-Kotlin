package com.brickgame.tetris.ui.layout

import kotlinx.serialization.Serializable

/**
 * Identifies which layout preset to use.
 */
enum class LayoutPreset(val displayName: String, val isLandscape: Boolean) {
    PORTRAIT_CLASSIC("Classic", false),
    PORTRAIT_MODERN("Modern", false),
    PORTRAIT_FULLSCREEN("Fullscreen", false),
    PORTRAIT_ONEHAND("Compact", false),
    PORTRAIT_FREEFORM("Freeform", false),
    PORTRAIT_3D("3D Mode", false),
    LANDSCAPE_DEFAULT("Default", true),
    LANDSCAPE_LEFTY("Lefty", true);

    companion object {
        fun portraitPresets() = entries.filter { !it.isLandscape }
        fun landscapePresets() = entries.filter { it.isLandscape }
    }
}

/**
 * D-Pad style selection
 */
enum class DPadStyle(val displayName: String) {
    STANDARD("Standard Cross"),
    ROTATE_CENTRE("Cross + Rotate Centre")
}

/**
 * Button visual shape for Freeform layout — per-element
 */
enum class ButtonShape(val displayName: String, val description: String) {
    ROUND("Round", "Circular with gradient & shadow"),
    SQUARE("Square", "Rounded rectangle, gamepad feel"),
    OUTLINE("Outline", "Transparent with colored border"),
    FLAT("Flat", "Solid color, no gradient or shadow"),
    PILL("Pill", "Wide capsule shape, larger targets"),
    RETRO("Retro", "Chunky raised 3D, vintage feel"),
    GLASS("Glass", "Frosted semi-transparent")
}

/**
 * Board visual shape type for Freeform layout
 */
enum class BoardShape(val displayName: String, val description: String) {
    STANDARD("Standard", "Simple border with small radius"),
    FRAMELESS("Frameless", "No border, grid only"),
    DEVICE_FRAME("Device Frame", "Thick rounded frame"),
    BEVELED("Beveled", "Raised 3D edge effect"),
    ROUNDED("Rounded", "Large corner radius, clipped grid")
}

/**
 * Information bar arrangement type for Freeform layout
 */
enum class InfoBarType(val displayName: String, val description: String) {
    INDIVIDUAL("Individual", "Each info element placed separately"),
    HORIZONTAL("Horizontal Bar", "All info in one horizontal row"),
    VERTICAL("Vertical Stack", "All info stacked vertically"),
    SPLIT_PAIR("Split Pair", "Stats bar + Pieces bar")
}

/**
 * Info bar container shape variant
 */
enum class InfoBarShape(val displayName: String) {
    PILL("Pill"),
    RECTANGLE("Rectangle"),
    NO_BORDER("No Border"),
    FRAMED("Framed")
}
