package com.brickgame.tetris.game

/**
 * Game difficulty levels affecting speed and scoring
 */
enum class Difficulty(
    val displayName: String,
    val description: String,
    val startLevel: Int,
    val speedMultiplier: Float,
    val scoreMultiplier: Float
) {
    EASY(
        "Easy",
        "Relaxed pace, great for beginners",
        1,
        1.5f,  // 50% slower
        0.5f   // Half points
    ),
    NORMAL(
        "Normal", 
        "Standard Tetris experience",
        1,
        1.0f,
        1.0f
    ),
    HARD(
        "Hard",
        "Faster drops, for experienced players",
        5,
        0.8f,  // 20% faster
        1.5f   // 50% more points
    ),
    EXPERT(
        "Expert",
        "Very fast, test your skills",
        10,
        0.6f,  // 40% faster
        2.0f   // Double points
    ),
    MASTER(
        "Master",
        "Extreme speed, only for masters",
        15,
        0.4f,  // 60% faster
        3.0f   // Triple points
    )
}
