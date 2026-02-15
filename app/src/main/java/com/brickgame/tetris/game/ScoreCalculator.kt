package com.brickgame.tetris.game

/**
 * Official Tetris Guideline scoring system with T-spin bonuses,
 * back-to-back chains, combo counter, and soft/hard drop points.
 */
object ScoreCalculator {
    
    /**
     * Base points for line clears (multiplied by level)
     */
    private val LINE_CLEAR_POINTS = mapOf(
        1 to 100,   // Single
        2 to 300,   // Double
        3 to 500,   // Triple
        4 to 800    // Tetris
    )
    
    /**
     * T-spin bonus points (multiplied by level)
     */
    private val TSPIN_POINTS = mapOf(
        TSpinType.MINI to mapOf(0 to 100, 1 to 200, 2 to 400),
        TSpinType.FULL to mapOf(0 to 400, 1 to 800, 2 to 1200, 3 to 1600)
    )
    
    /**
     * Perfect Clear (All-Clear) bonus points by lines cleared (multiplied by level)
     */
    private val PERFECT_CLEAR_POINTS = mapOf(
        1 to 800,    // Single PC
        2 to 1200,   // Double PC
        3 to 1800,   // Triple PC
        4 to 3500    // Tetris PC (the dream)
    )

    /**
     * Back-to-back multiplier: 1.5x for consecutive "difficult" clears
     * Difficult = Tetris or any T-spin line clear
     */
    private const val BACK_TO_BACK_MULTIPLIER = 1.5f
    
    /**
     * Combo bonus: 50 * combo_count * level
     */
    private const val COMBO_BONUS_PER_LEVEL = 50
    
    /**
     * Calculate score for a line clear event
     */
    fun calculateScore(
        linesCleared: Int,
        level: Int,
        tSpinType: TSpinType,
        isBackToBack: Boolean,
        comboCount: Int,
        difficultyMultiplier: Float = 1.0f,
        isPerfectClear: Boolean = false
    ): ScoreResult {
        if (linesCleared == 0 && tSpinType == TSpinType.NONE) {
            return ScoreResult(0, false, "")
        }
        
        // Base points
        val basePoints = if (tSpinType != TSpinType.NONE) {
            TSPIN_POINTS[tSpinType]?.get(linesCleared) ?: 0
        } else {
            LINE_CLEAR_POINTS[linesCleared] ?: 0
        }
        
        var points = basePoints * level
        
        // Perfect Clear bonus — massive reward
        if (isPerfectClear && linesCleared > 0) {
            points += (PERFECT_CLEAR_POINTS[linesCleared] ?: 800) * level
        }
        
        // Back-to-back bonus
        val isDifficult = linesCleared == 4 || (tSpinType != TSpinType.NONE && linesCleared > 0) || isPerfectClear
        if (isBackToBack && isDifficult) {
            points = (points * BACK_TO_BACK_MULTIPLIER).toInt()
        }
        
        // Combo bonus
        if (comboCount > 0) {
            points += COMBO_BONUS_PER_LEVEL * comboCount * level
        }
        
        // Apply difficulty multiplier
        points = (points * difficultyMultiplier).toInt()
        
        // Build action label
        val label = buildActionLabel(linesCleared, tSpinType, isBackToBack, comboCount, isPerfectClear)
        
        return ScoreResult(points, isDifficult, label)
    }
    
    /**
     * Points for soft drop (1 per cell)
     */
    fun softDropPoints(cells: Int): Int = cells
    
    /**
     * Points for hard drop (2 per cell)
     */
    fun hardDropPoints(cells: Int): Int = cells * 2
    
    private fun buildActionLabel(
        linesCleared: Int,
        tSpinType: TSpinType,
        isBackToBack: Boolean,
        comboCount: Int,
        isPerfectClear: Boolean = false
    ): String {
        val parts = mutableListOf<String>()
        
        if (isPerfectClear) {
            parts.add("PERFECT CLEAR")
        }
        
        if (isBackToBack && (linesCleared == 4 || (tSpinType != TSpinType.NONE && linesCleared > 0) || isPerfectClear)) {
            parts.add("B2B")
        }
        
        when (tSpinType) {
            TSpinType.MINI -> parts.add("Mini T-Spin")
            TSpinType.FULL -> parts.add("T-Spin")
            TSpinType.NONE -> {}
        }
        
        when (linesCleared) {
            1 -> parts.add("Single")
            2 -> parts.add("Double")
            3 -> parts.add("Triple")
            4 -> parts.add("Tetris!")
        }
        
        if (comboCount > 1) {
            parts.add("${comboCount}x Combo")
        }
        
        return parts.joinToString(" ")
    }
}

data class ScoreResult(
    val points: Int,
    val isDifficultClear: Boolean,
    val label: String
)
