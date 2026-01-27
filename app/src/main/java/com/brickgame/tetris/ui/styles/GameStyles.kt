package com.brickgame.tetris.ui.styles

/**
 * Animation styles for different visual experiences
 */
enum class AnimationStyle(val displayName: String, val description: String) {
    NONE("None", "No animations, instant"),
    RETRO("Retro", "Blink effect like original"),
    MODERN("Modern", "Smooth fade and slide"),
    FLASHY("Flashy", "Bold flash with shake")
}

/**
 * Vibration patterns for different haptic experiences
 */
enum class VibrationStyle(val displayName: String, val description: String) {
    NONE("None", "No vibration"),
    SUBTLE("Subtle", "Light, minimal"),
    CLASSIC("Classic", "Standard pulse"),
    RETRO("Retro", "Double-tap pattern"),
    MODERN("Modern", "Smooth ramping"),
    HEAVY("Heavy", "Strong feedback")
}

/**
 * Sound styles for different audio experiences
 */
enum class SoundStyle(val displayName: String, val description: String) {
    NONE("None", "No sounds"),
    RETRO_BEEP("Retro Beep", "Simple beeps"),
    MODERN_SOFT("Modern Soft", "Gentle tones"),
    ARCADE("Arcade", "Classic arcade"),
    MECHANICAL("Mechanical", "Clicky sounds")
}

/**
 * Preset combinations for quick setup
 */
enum class StylePreset(
    val displayName: String,
    val description: String,
    val animationStyle: AnimationStyle,
    val vibrationStyle: VibrationStyle,
    val soundStyle: SoundStyle
) {
    AUTHENTIC_RETRO(
        "🕹️ Authentic Retro",
        "Like the original 90s handheld",
        AnimationStyle.RETRO,
        VibrationStyle.RETRO,
        SoundStyle.RETRO_BEEP
    ),
    MODERN_CLEAN(
        "✨ Modern Clean", 
        "Smooth and satisfying",
        AnimationStyle.MODERN,
        VibrationStyle.MODERN,
        SoundStyle.MODERN_SOFT
    ),
    ARCADE_MODE(
        "🎮 Arcade Mode",
        "Bold and exciting",
        AnimationStyle.FLASHY,
        VibrationStyle.HEAVY,
        SoundStyle.ARCADE
    ),
    SILENT(
        "🔇 Silent",
        "No feedback at all",
        AnimationStyle.NONE,
        VibrationStyle.NONE,
        SoundStyle.NONE
    ),
    CUSTOM(
        "⚙️ Custom",
        "Your own combination",
        AnimationStyle.MODERN,
        VibrationStyle.CLASSIC,
        SoundStyle.RETRO_BEEP
    )
}
