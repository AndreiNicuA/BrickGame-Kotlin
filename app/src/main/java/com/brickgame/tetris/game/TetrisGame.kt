package com.brickgame.tetris.game

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TetrisGame {
    
    companion object {
        private const val TAG = "TetrisGame"
        const val BOARD_WIDTH = 10
        const val BOARD_HEIGHT = 20
        
        val TETROMINOS = mapOf(
            TetrominoType.I to arrayOf(intArrayOf(1, 1, 1, 1)),
            TetrominoType.O to arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)),
            TetrominoType.T to arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)),
            TetrominoType.S to arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)),
            TetrominoType.Z to arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)),
            TetrominoType.J to arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)),
            TetrominoType.L to arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1))
        )
        
        val SCORE_TABLE = intArrayOf(0, 100, 300, 500, 800)
    }
    
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()
    
    private var board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
    private var currentPiece: Tetromino? = null
    private var currentPosition = Position(0, 0)
    private var nextPiece: Tetromino? = null
    private var isRunning = false
    private var isPaused = false
    private var difficulty = Difficulty.NORMAL
    
    // Track if we're waiting for line clear animation
    private var pendingLineClear = false
    private var pendingClearedLines: List<Int> = emptyList()
    private var pendingPoints = 0
    private var pendingNewLines = 0
    private var pendingNewLevel = 0
    
    fun setDifficulty(diff: Difficulty) {
        this.difficulty = diff
    }
    
    fun startGame() {
        board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
        currentPiece = null
        nextPiece = null
        pendingLineClear = false
        pendingClearedLines = emptyList()
        
        _state.update { 
            GameState(
                status = GameStatus.PLAYING,
                score = 0,
                level = difficulty.startLevel,
                lines = 0,
                board = board.map { it.toList() },
                difficulty = difficulty
            )
        }
        
        nextPiece = generateRandomPiece()
        spawnPiece()
    }
    
    fun pauseGame() {
        if (_state.value.status == GameStatus.PLAYING) {
            isRunning = false
            isPaused = true
            _state.update { it.copy(status = GameStatus.PAUSED) }
        }
    }
    
    fun resumeGame() {
        if (_state.value.status == GameStatus.PAUSED) {
            isRunning = true
            isPaused = false
            _state.update { it.copy(status = GameStatus.PLAYING) }
        }
    }
    
    fun isGameActive(): Boolean = isRunning && _state.value.status != GameStatus.MENU && _state.value.status != GameStatus.GAME_OVER
    
    fun isPendingLineClear(): Boolean = pendingLineClear
    
    /**
     * Called by ViewModel after animation completes to actually clear the lines
     * and spawn the next piece.
     */
    fun completePendingLineClear() {
        if (!pendingLineClear) return
        
        // Actually remove the lines from the board now
        clearLines(pendingClearedLines)
        
        // Update state with cleared board and new scores
        _state.update { 
            it.copy(
                score = it.score + pendingPoints,
                lines = pendingNewLines,
                level = pendingNewLevel.coerceAtMost(20),
                linesCleared = 0,
                clearedLineRows = emptyList(),
                board = board.map { row -> row.toList() }
            )
        }
        
        pendingLineClear = false
        pendingClearedLines = emptyList()
        
        // Now spawn the next piece
        spawnPiece()
    }
    
    private fun spawnPiece() {
        currentPiece = nextPiece
        nextPiece = generateRandomPiece()
        
        val piece = currentPiece ?: return
        
        val startX = (BOARD_WIDTH - piece.width) / 2
        currentPosition = Position(startX, 0)
        
        if (checkCollision(piece.shape, currentPosition)) {
            gameOver()
            return
        }
        
        isRunning = true
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
        return if (!checkCollision(currentPiece!!.shape, newPos)) {
            currentPosition = newPos
            updateState()
            MoveResult.MOVED
        } else {
            lockPiece()
            MoveResult.LOCKED
        }
    }
    
    fun hardDrop(): Int {
        if (!canMove()) return 0
        
        var dropDistance = 0
        while (!checkCollision(currentPiece!!.shape, Position(currentPosition.x, currentPosition.y + 1))) {
            currentPosition = Position(currentPosition.x, currentPosition.y + 1)
            dropDistance++
        }
        
        if (dropDistance > 0) {
            _state.update { it.copy(score = it.score + dropDistance * 2) }
        }
        
        lockPiece()
        return dropDistance
    }
    
    fun rotate(): Boolean {
        if (!canMove()) return false
        
        val piece = currentPiece ?: return false
        val rotated = piece.rotate()
        
        // Try basic rotation
        if (!checkCollision(rotated.shape, currentPosition)) {
            currentPiece = rotated
            updateState()
            return true
        }
        
        // Wall kicks
        val kicks = listOf(-1, 1, -2, 2)
        for (kick in kicks) {
            val kickedPos = Position(currentPosition.x + kick, currentPosition.y)
            if (!checkCollision(rotated.shape, kickedPos)) {
                currentPiece = rotated
                currentPosition = kickedPos
                updateState()
                return true
            }
        }
        
        return false
    }
    
    private fun canMove(): Boolean = isRunning && currentPiece != null && 
                                      _state.value.status == GameStatus.PLAYING && 
                                      !pendingLineClear
    
    private fun checkCollision(shape: Array<IntArray>, pos: Position): Boolean {
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x] != 0) {
                    val boardX = pos.x + x
                    val boardY = pos.y + y
                    
                    if (boardX < 0 || boardX >= BOARD_WIDTH) return true
                    if (boardY >= BOARD_HEIGHT) return true
                    if (boardY >= 0 && board[boardY][boardX] != 0) return true
                }
            }
        }
        return false
    }
    
    private fun lockPiece() {
        val piece = currentPiece ?: return
        
        // Place the piece on the board
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
        
        // Clear the current piece so it's not drawn twice
        currentPiece = null
        
        val clearedLines = findCompletedLines()
        val clearedCount = clearedLines.size
        
        if (clearedCount > 0) {
            // Calculate scores
            val currentState = _state.value
            val basePoints = SCORE_TABLE[clearedCount.coerceAtMost(4)] * currentState.level
            val points = (basePoints * difficulty.scoreMultiplier).toInt()
            val newLines = currentState.lines + clearedCount
            val newLevel = maxOf(difficulty.startLevel, (newLines / 10) + 1)
            
            // Store pending clear info
            pendingLineClear = true
            pendingClearedLines = clearedLines
            pendingPoints = points
            pendingNewLines = newLines
            pendingNewLevel = newLevel
            
            // Update state with the FULL lines still visible for animation
            // The board shows the complete lines that are about to be cleared
            _state.update { 
                it.copy(
                    linesCleared = clearedCount,
                    clearedLineRows = clearedLines,
                    board = board.map { row -> row.toList() },
                    currentPiece = null  // Don't show piece during animation
                )
            }
            
            // Don't spawn next piece yet - wait for animation to complete
            // ViewModel will call completePendingLineClear() after animation
        } else {
            // No lines to clear, spawn next piece immediately
            spawnPiece()
        }
    }
    
    private fun findCompletedLines(): List<Int> {
        val completed = mutableListOf<Int>()
        for (y in 0 until BOARD_HEIGHT) {
            if (isRowComplete(y)) {
                completed.add(y)
            }
        }
        return completed
    }
    
    /**
     * Clear all complete lines from the board.
     * This method repeatedly finds and removes complete lines until none remain.
     */
    private fun clearLines(lines: List<Int>) {
        var clearedAny: Boolean
        do {
            clearedAny = false
            for (y in BOARD_HEIGHT - 1 downTo 0) {
                if (isRowComplete(y)) {
                    removeRow(y)
                    clearedAny = true
                    break
                }
            }
        } while (clearedAny)
    }
    
    private fun isRowComplete(row: Int): Boolean {
        for (x in 0 until BOARD_WIDTH) {
            if (board[row][x] == 0) return false
        }
        return true
    }
    
    private fun removeRow(row: Int) {
        for (y in row downTo 1) {
            for (x in 0 until BOARD_WIDTH) {
                board[y][x] = board[y - 1][x]
            }
        }
        for (x in 0 until BOARD_WIDTH) {
            board[0][x] = 0
        }
    }
    
    private fun gameOver() {
        isRunning = false
        _state.update { 
            it.copy(
                status = GameStatus.GAME_OVER,
                board = board.map { row -> row.toList() },
                currentPiece = null
            )
        }
    }
    
    fun calculateGhostY(): Int {
        val piece = currentPiece ?: return currentPosition.y
        var ghostY = currentPosition.y
        
        while (!checkCollision(piece.shape, Position(currentPosition.x, ghostY + 1))) {
            ghostY++
        }
        
        return ghostY
    }
    
    private fun updateState() {
        val displayBoard = getDisplayBoard()
        val ghostY = calculateGhostY()
        
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
                linesCleared = 0,
                clearedLineRows = emptyList()
            )
        }
    }
    
    private fun getDisplayBoard(): List<List<Int>> {
        val display = board.map { it.toMutableList() }
        
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
        
        return display.map { it.toList() }
    }
    
    fun getDropSpeed(): Long {
        val level = _state.value.level
        val baseSpeed = maxOf(100L, 1000L - (level - 1) * 50L)
        return (baseSpeed * difficulty.speedMultiplier).toLong().coerceAtLeast(50L)
    }
    
    private fun generateRandomPiece(): Tetromino {
        val types = TetrominoType.values()
        val type = types.random()
        return Tetromino(type, TETROMINOS[type]!!.map { it.clone() }.toTypedArray())
    }
}

enum class TetrominoType { I, O, T, S, Z, J, L }

enum class GameStatus { MENU, PLAYING, PAUSED, GAME_OVER }

enum class MoveResult { MOVED, LOCKED, BLOCKED }

data class Position(val x: Int, val y: Int)

data class Tetromino(
    val type: TetrominoType,
    val shape: Array<IntArray>
) {
    val width: Int get() = shape[0].size
    val height: Int get() = shape.size
    
    fun rotate(): Tetromino {
        val newShape = Array(width) { IntArray(height) }
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                newShape[x][height - 1 - y] = shape[y][x]
            }
        }
        return Tetromino(type, newShape)
    }
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
    val board: List<List<Int>> = List(TetrisGame.BOARD_HEIGHT) { List(TetrisGame.BOARD_WIDTH) { 0 } },
    val currentPiece: PieceState? = null,
    val nextPiece: PieceState? = null,
    val ghostY: Int = 0,
    val linesCleared: Int = 0,
    val clearedLineRows: List<Int> = emptyList(),
    val highScore: Int = 0,
    val difficulty: Difficulty = Difficulty.NORMAL
)
