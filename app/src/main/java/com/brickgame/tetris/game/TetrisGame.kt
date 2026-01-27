package com.brickgame.tetris.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tetris Game Engine
 * Handles all game logic, board state, and piece management
 */
class TetrisGame {
    
    companion object {
        const val BOARD_WIDTH = 10
        const val BOARD_HEIGHT = 20
        
        // Tetromino shapes (rotations are generated)
        val TETROMINOS = mapOf(
            TetrominoType.I to arrayOf(intArrayOf(1, 1, 1, 1)),
            TetrominoType.O to arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)),
            TetrominoType.T to arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)),
            TetrominoType.S to arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)),
            TetrominoType.Z to arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)),
            TetrominoType.J to arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)),
            TetrominoType.L to arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1))
        )
        
        // Scoring table
        val SCORE_TABLE = intArrayOf(0, 100, 300, 500, 800)
    }
    
    // Game state
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()
    
    // Board state (0 = empty, 1+ = filled)
    private var board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
    
    // Current piece
    private var currentPiece: Tetromino? = null
    private var currentPosition = Position(0, 0)
    
    // Next piece
    private var nextPiece: Tetromino? = null
    
    // Game control
    private var isRunning = false
    private var isPaused = false
    
    fun startGame() {
        // Reset everything
        board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        currentPiece = null
        nextPiece = null
        
        _state.update { 
            GameState(
                status = GameStatus.PLAYING,
                score = 0,
                level = 1,
                lines = 0,
                board = board.map { it.toList() },
                currentPiece = null,
                nextPiece = null
            )
        }
        
        isRunning = true
        isPaused = false
        
        // Spawn first pieces
        nextPiece = generateRandomPiece()
        spawnPiece()
    }
    
    fun pauseGame() {
        if (_state.value.status == GameStatus.PLAYING) {
            isPaused = true
            _state.update { it.copy(status = GameStatus.PAUSED) }
        }
    }
    
    fun resumeGame() {
        if (_state.value.status == GameStatus.PAUSED) {
            isPaused = false
            _state.update { it.copy(status = GameStatus.PLAYING) }
        }
    }
    
    fun togglePause() {
        if (isPaused) resumeGame() else pauseGame()
    }
    
    // Check if game is currently paused
    fun isPaused(): Boolean = isPaused
    
    // Check if game is running (not menu or game over)
    fun isGameActive(): Boolean = isRunning && _state.value.status != GameStatus.MENU && _state.value.status != GameStatus.GAME_OVER
    
    private fun generateRandomPiece(): Tetromino {
        val type = TetrominoType.entries.toTypedArray().random()
        val shape = TETROMINOS[type]!!.map { it.copyOf() }.toTypedArray()
        return Tetromino(type, shape)
    }
    
    private fun spawnPiece() {
        currentPiece = nextPiece
        nextPiece = generateRandomPiece()
        
        val piece = currentPiece ?: return
        val startX = (BOARD_WIDTH - piece.width) / 2
        currentPosition = Position(startX, 0)
        
        // Check if spawn position is valid
        if (checkCollision(piece.shape, currentPosition)) {
            gameOver()
            return
        }
        
        updateState()
    }
    
    fun moveLeft(): Boolean {
        if (!canMove()) return false
        
        val newPos = Position(currentPosition.x - 1, currentPosition.y)
        if (!checkCollision(currentPiece!!.shape, newPos)) {
            currentPosition = newPos
            updateState()
            return true
        }
        return false
    }
    
    fun moveRight(): Boolean {
        if (!canMove()) return false
        
        val newPos = Position(currentPosition.x + 1, currentPosition.y)
        if (!checkCollision(currentPiece!!.shape, newPos)) {
            currentPosition = newPos
            updateState()
            return true
        }
        return false
    }
    
    fun moveDown(): MoveResult {
        if (!canMove()) return MoveResult.BLOCKED
        
        val newPos = Position(currentPosition.x, currentPosition.y + 1)
        if (!checkCollision(currentPiece!!.shape, newPos)) {
            currentPosition = newPos
            updateState()
            return MoveResult.MOVED
        } else {
            lockPiece()
            return MoveResult.LOCKED
        }
    }
    
    fun hardDrop(): Int {
        if (!canMove()) return 0
        
        var dropDistance = 0
        while (moveDown() == MoveResult.MOVED) {
            dropDistance++
        }
        return dropDistance
    }
    
    fun rotate(): Boolean {
        if (!canMove()) return false
        
        val piece = currentPiece ?: return false
        val rotated = rotateShape(piece.shape)
        
        // Try normal rotation
        if (!checkCollision(rotated, currentPosition)) {
            currentPiece = piece.copy(shape = rotated)
            updateState()
            return true
        }
        
        // Try wall kicks
        val kicks = listOf(-1, 1, -2, 2)
        for (kick in kicks) {
            val kickedPos = Position(currentPosition.x + kick, currentPosition.y)
            if (!checkCollision(rotated, kickedPos)) {
                currentPiece = piece.copy(shape = rotated)
                currentPosition = kickedPos
                updateState()
                return true
            }
        }
        
        return false
    }
    
    private fun rotateShape(shape: Array<IntArray>): Array<IntArray> {
        val rows = shape.size
        val cols = shape[0].size
        val rotated = Array(cols) { IntArray(rows) }
        
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                rotated[x][rows - 1 - y] = shape[y][x]
            }
        }
        return rotated
    }
    
    private fun checkCollision(shape: Array<IntArray>, pos: Position): Boolean {
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x] != 0) {
                    val boardX = pos.x + x
                    val boardY = pos.y + y
                    
                    // Check boundaries
                    if (boardX < 0 || boardX >= BOARD_WIDTH) return true
                    if (boardY >= BOARD_HEIGHT) return true
                    
                    // Check collision with locked pieces
                    if (boardY >= 0 && board[boardY][boardX] != 0) return true
                }
            }
        }
        return false
    }
    
    private fun lockPiece() {
        val piece = currentPiece ?: return
        
        // Place piece on board
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] != 0) {
                    val boardY = currentPosition.y + y
                    val boardX = currentPosition.x + x
                    if (boardY >= 0 && boardY < BOARD_HEIGHT && boardX >= 0 && boardX < BOARD_WIDTH) {
                        board[boardY][boardX] = piece.type.ordinal + 1
                    }
                }
            }
        }
        
        // Check for completed lines - FIXED ALGORITHM
        val clearedLines = findAndClearLines()
        
        if (clearedLines.isNotEmpty()) {
            val linesCleared = clearedLines.size
            val currentState = _state.value
            val newLines = currentState.lines + linesCleared
            val newLevel = (newLines / 10) + 1
            val points = SCORE_TABLE[linesCleared.coerceAtMost(4)] * currentState.level
            
            _state.update { 
                it.copy(
                    score = it.score + points,
                    lines = newLines,
                    level = newLevel.coerceAtMost(20),
                    clearedRows = clearedLines,
                    lastEvent = GameEvent.LINES_CLEARED,
                    board = board.map { row -> row.toList() }
                )
            }
        } else {
            _state.update { it.copy(lastEvent = GameEvent.PIECE_LOCKED) }
        }
        
        // Spawn next piece
        spawnPiece()
    }
    
    /**
     * FIXED line clearing algorithm
     * Properly detects and removes completed lines
     */
    private fun findAndClearLines(): List<Int> {
        val clearedRows = mutableListOf<Int>()
        
        // Find all complete rows (check from bottom to top)
        for (y in BOARD_HEIGHT - 1 downTo 0) {
            var isComplete = true
            for (x in 0 until BOARD_WIDTH) {
                if (board[y][x] == 0) {
                    isComplete = false
                    break
                }
            }
            if (isComplete) {
                clearedRows.add(y)
            }
        }
        
        if (clearedRows.isEmpty()) return emptyList()
        
        // Sort rows in descending order (bottom to top)
        val sortedRows = clearedRows.sortedDescending()
        
        // Remove each cleared row and shift everything down
        for (clearedRow in sortedRows) {
            // Shift all rows above the cleared row down by one
            for (y in clearedRow downTo 1) {
                for (x in 0 until BOARD_WIDTH) {
                    board[y][x] = board[y - 1][x]
                }
            }
            // Clear the top row
            for (x in 0 until BOARD_WIDTH) {
                board[0][x] = 0
            }
        }
        
        return clearedRows
    }
    
    private fun gameOver() {
        isRunning = false
        _state.update { 
            it.copy(
                status = GameStatus.GAME_OVER,
                lastEvent = GameEvent.GAME_OVER
            )
        }
    }
    
    private fun canMove(): Boolean {
        return isRunning && !isPaused && currentPiece != null
    }
    
    private fun updateState() {
        val displayBoard = getDisplayBoard()
        val ghostY = getGhostPosition()
        
        _state.update { state ->
            state.copy(
                board = displayBoard,
                currentPiece = currentPiece?.let { 
                    PieceState(it.type, currentPosition, it.shape.map { row -> row.toList() })
                },
                nextPiece = nextPiece?.let {
                    PieceState(it.type, Position(0, 0), it.shape.map { row -> row.toList() })
                },
                ghostY = ghostY,
                clearedRows = emptyList()
            )
        }
    }
    
    private fun getDisplayBoard(): List<List<Int>> {
        val display = board.map { it.toMutableList() }
        
        // Add current piece
        currentPiece?.let { piece ->
            for (y in piece.shape.indices) {
                for (x in piece.shape[y].indices) {
                    if (piece.shape[y][x] != 0) {
                        val boardY = currentPosition.y + y
                        val boardX = currentPosition.x + x
                        if (boardY >= 0 && boardY < BOARD_HEIGHT && boardX >= 0 && boardX < BOARD_WIDTH) {
                            display[boardY][boardX] = piece.type.ordinal + 1
                        }
                    }
                }
            }
        }
        
        return display
    }
    
    private fun getGhostPosition(): Int {
        val piece = currentPiece ?: return currentPosition.y
        var ghostY = currentPosition.y
        
        while (!checkCollision(piece.shape, Position(currentPosition.x, ghostY + 1))) {
            ghostY++
        }
        
        return ghostY
    }
    
    fun getDropSpeed(): Long {
        val level = _state.value.level
        return (1000L - (level - 1) * 100L).coerceAtLeast(50L)
    }
    
    fun clearEvent() {
        _state.update { it.copy(lastEvent = GameEvent.NONE, clearedRows = emptyList()) }
    }
}

// Data classes
data class Position(val x: Int, val y: Int)

data class Tetromino(
    val type: TetrominoType,
    val shape: Array<IntArray>
) {
    val width: Int get() = shape[0].size
    val height: Int get() = shape.size
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tetromino) return false
        return type == other.type && shape.contentDeepEquals(other.shape)
    }
    
    override fun hashCode(): Int = 31 * type.hashCode() + shape.contentDeepHashCode()
}

enum class TetrominoType {
    I, O, T, S, Z, J, L
}

enum class GameStatus {
    MENU, PLAYING, PAUSED, GAME_OVER
}

enum class MoveResult {
    MOVED, LOCKED, BLOCKED
}

enum class GameEvent {
    NONE, PIECE_LOCKED, LINES_CLEARED, LEVEL_UP, GAME_OVER
}

data class PieceState(
    val type: TetrominoType,
    val position: Position,
    val shape: List<List<Int>>
)

data class GameState(
    val status: GameStatus = GameStatus.MENU,
    val score: Int = 0,
    val level: Int = 1,
    val lines: Int = 0,
    val highScore: Int = 0,
    val board: List<List<Int>> = List(TetrisGame.BOARD_HEIGHT) { List(TetrisGame.BOARD_WIDTH) { 0 } },
    val currentPiece: PieceState? = null,
    val nextPiece: PieceState? = null,
    val ghostY: Int = 0,
    val clearedRows: List<Int> = emptyList(),
    val lastEvent: GameEvent = GameEvent.NONE
)
