package com.brickgame.tetris.game

/**
 * Super Rotation System (SRS) - Official Tetris Guideline rotation
 * Implements wall kick offset tables for all piece types and rotation states.
 */
object SRS {
    
    /**
     * Wall kick offset data for J, L, S, T, Z pieces
     * Each entry: from rotation state -> to rotation state -> list of (dx, dy) offsets to try
     * Rotation states: 0=spawn, 1=CW, 2=180, 3=CCW
     */
    private val JLSTZ_KICKS: Map<Pair<Int, Int>, List<Pair<Int, Int>>> = mapOf(
        // 0 -> 1 (spawn -> CW)
        Pair(0, 1) to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, -1), Pair(0, 2), Pair(-1, 2)),
        // 1 -> 0 (CW -> spawn)
        Pair(1, 0) to listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(0, -2), Pair(1, -2)),
        // 1 -> 2 (CW -> 180)
        Pair(1, 2) to listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(0, -2), Pair(1, -2)),
        // 2 -> 1 (180 -> CW)
        Pair(2, 1) to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, -1), Pair(0, 2), Pair(-1, 2)),
        // 2 -> 3 (180 -> CCW)
        Pair(2, 3) to listOf(Pair(0, 0), Pair(1, 0), Pair(1, -1), Pair(0, 2), Pair(1, 2)),
        // 3 -> 2 (CCW -> 180)
        Pair(3, 2) to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, 1), Pair(0, -2), Pair(-1, -2)),
        // 3 -> 0 (CCW -> spawn)
        Pair(3, 0) to listOf(Pair(0, 0), Pair(-1, 0), Pair(-1, 1), Pair(0, -2), Pair(-1, -2)),
        // 0 -> 3 (spawn -> CCW)
        Pair(0, 3) to listOf(Pair(0, 0), Pair(1, 0), Pair(1, -1), Pair(0, 2), Pair(1, 2))
    )
    
    /**
     * Wall kick offset data for I piece (different from other pieces)
     */
    private val I_KICKS: Map<Pair<Int, Int>, List<Pair<Int, Int>>> = mapOf(
        Pair(0, 1) to listOf(Pair(0, 0), Pair(-2, 0), Pair(1, 0), Pair(-2, 1), Pair(1, -2)),
        Pair(1, 0) to listOf(Pair(0, 0), Pair(2, 0), Pair(-1, 0), Pair(2, -1), Pair(-1, 2)),
        Pair(1, 2) to listOf(Pair(0, 0), Pair(-1, 0), Pair(2, 0), Pair(-1, -2), Pair(2, 1)),
        Pair(2, 1) to listOf(Pair(0, 0), Pair(1, 0), Pair(-2, 0), Pair(1, 2), Pair(-2, -1)),
        Pair(2, 3) to listOf(Pair(0, 0), Pair(2, 0), Pair(-1, 0), Pair(2, -1), Pair(-1, 2)),
        Pair(3, 2) to listOf(Pair(0, 0), Pair(-2, 0), Pair(1, 0), Pair(-2, 1), Pair(1, -2)),
        Pair(3, 0) to listOf(Pair(0, 0), Pair(1, 0), Pair(-2, 0), Pair(1, 2), Pair(-2, -1)),
        Pair(0, 3) to listOf(Pair(0, 0), Pair(-1, 0), Pair(2, 0), Pair(-1, -2), Pair(2, 1))
    )
    
    /**
     * O piece has no wall kicks - it never needs them
     */
    private val O_KICKS: Map<Pair<Int, Int>, List<Pair<Int, Int>>> = mapOf(
        Pair(0, 1) to listOf(Pair(0, 0)),
        Pair(1, 0) to listOf(Pair(0, 0)),
        Pair(1, 2) to listOf(Pair(0, 0)),
        Pair(2, 1) to listOf(Pair(0, 0)),
        Pair(2, 3) to listOf(Pair(0, 0)),
        Pair(3, 2) to listOf(Pair(0, 0)),
        Pair(3, 0) to listOf(Pair(0, 0)),
        Pair(0, 3) to listOf(Pair(0, 0))
    )
    
    /**
     * Get wall kick offsets for a piece type and rotation transition
     * @param type The tetromino type
     * @param fromRotation Current rotation state (0-3)
     * @param toRotation Target rotation state (0-3)
     * @return List of (dx, dy) offsets to try, in order
     */
    fun getKickOffsets(type: TetrominoType, fromRotation: Int, toRotation: Int): List<Pair<Int, Int>> {
        val key = Pair(fromRotation, toRotation)
        return when (type) {
            TetrominoType.I -> I_KICKS[key] ?: listOf(Pair(0, 0))
            TetrominoType.O -> O_KICKS[key] ?: listOf(Pair(0, 0))
            else -> JLSTZ_KICKS[key] ?: listOf(Pair(0, 0))
        }
    }
    
    /**
     * Check if a T-spin occurred after rotation.
     * A T-spin is detected when:
     * 1. The last move was a rotation (not movement)
     * 2. The piece is a T
     * 3. At least 3 of the 4 corners around the T center are occupied
     * 
     * @return TSpin type: NONE, MINI, or FULL
     */
    fun detectTSpin(
        board: Array<IntArray>,
        position: Position,
        rotationState: Int,
        usedKick: Boolean,
        boardWidth: Int,
        boardHeight: Int
    ): TSpinType {
        // T center is always at (1, 1) in the 3x3 bounding box
        val centerX = position.x + 1
        val centerY = position.y + 1
        
        // Check corners around the T center
        fun isOccupied(x: Int, y: Int): Boolean {
            if (x < 0 || x >= boardWidth || y < 0 || y >= boardHeight) return true
            return board[y][x] != 0
        }
        
        val topLeft = isOccupied(centerX - 1, centerY - 1)
        val topRight = isOccupied(centerX + 1, centerY - 1)
        val bottomLeft = isOccupied(centerX - 1, centerY + 1)
        val bottomRight = isOccupied(centerX + 1, centerY + 1)
        
        val occupiedCorners = listOf(topLeft, topRight, bottomLeft, bottomRight).count { it }
        
        if (occupiedCorners < 3) return TSpinType.NONE
        
        // Determine which two corners are "facing" based on rotation
        // The two corners in front of the T's flat side must both be occupied for a full T-spin
        val frontCornersOccupied = when (rotationState) {
            0 -> topLeft && topRight      // Flat side up
            1 -> topRight && bottomRight  // Flat side right
            2 -> bottomLeft && bottomRight // Flat side down
            3 -> topLeft && bottomLeft    // Flat side left
            else -> false
        }
        
        return if (frontCornersOccupied) {
            TSpinType.FULL
        } else {
            TSpinType.MINI
        }
    }
}

enum class TSpinType {
    NONE, MINI, FULL
}
