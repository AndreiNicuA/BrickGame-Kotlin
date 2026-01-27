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
    
    fun startGame() {
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
    
    fun isPaused(): Boolean = isPaused
    
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
        
        if (!checkCollision(rotated, currentPosition)) {
            currentPiece = piece.copy(shape = rotated)
            updateState()
            return true
        }
        
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
        
        // IMMEDIATELY check and clear lines
        val clearedCount = clearCompleteLines()
        
        if (clearedCount > 0) {
            val currentState = _state.value
            val newLines = currentState.lines + clearedCount
            val newLevel = (newLines / 10) + 1
            val points = SCORE_TABLE[clearedCount.coerceAtMost(4)] * currentState.level
            
            Log.d(TAG, "Cleared $clearedCount lines! Score: +$points")
            
            _state.update { 
                it.copy(
                    score = it.score + points,
                    lines = newLines,
                    level = newLevel.coerceAtMost(20),
                    board = board.map { row -> row.toList() }
                )
            }
        }
        
        // Spawn next piece
        spawnPiece()
    }
    
    /**
     * Clear complete lines and return count
     * This modifies the board directly
     */
    private fun clearCompleteLines(): Int {
        var linesCleared = 0
        var y = BOARD_HEIGHT - 1
        
        while (y >= 0) {
            if (isRowComplete(y)) {
                Log.d(TAG, "Row $y is complete, clearing...")
                removeRow(y)
                linesCleared++
                // Don't decrement y - check same position again since rows shifted down
            } else {
                y--
            }
        }
        
        return linesCleared
    }
    
    private fun isRowComplete(row: Int): Boolean {
        for (x in 0 until BOARD_WIDTH) {
            if (board[row][x] == 0) {
                return false
            }
        }
        return true
    }
    
    private fun removeRow(row: Int) {
        // Shift all rows above down by one
        for (y in row downTo 1) {
            for (x in 0 until BOARD_WIDTH) {
                board[y][x] = board[y - 1][x]
            }
        }
        // Clear top row
        for (x in 0 until BOARD_WIDTH) {
            board[0][x] = 0
        }
    }
    
    private fun gameOver() {
        isRunning = false
        _state.update { 
            it.copy(
                status = GameStatus.GAME_OVER,
                lastEvent = GameEvent.GAME_OVER,
                board = board.map { row -> row.toList() }
            )
        }
    }
    
    private fun canMove(): Boolean {
        return isRunning && !isPaused && currentPiece != null
    }
    
    private fun updateState() {
        val displayBoard = getDisplayBoard()
        
        _state.update { state ->
            state.copy(
                board = displayBoard,
                currentPiece = currentPiece?.let { 
                    PieceState(it.type, currentPosition, it.shape.map { row -> row.toList() })
                },
                nextPiece = nextPiece?.let {
                    PieceState(it.type, Position(0, 0), it.shape.map { row -> row.toList() })
                },
                clearedRows = emptyList()
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
        return (1000 - (level - 1) * 80).coerceAtLeast(100).toLong()
    }
    
    fun clearEvent() {
        _state.update { it.copy(clearedRows = emptyList(), lastEvent = GameEvent.NONE) }
    }
}

enum class TetrominoType { I, O, T, S, Z, J, L }

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
    
    override fun hashCode(): Int {
        return 31 * type.hashCode() + shape.contentDeepHashCode()
    }
}

data class Position(val x: Int, val y: Int)

enum class MoveResult { MOVED, LOCKED, BLOCKED }

enum class GameStatus { MENU, PLAYING, PAUSED, GAME_OVER }

enum class GameEvent { NONE, PIECE_LOCKED, LINES_CLEARED, GAME_OVER }

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
    val board: List<List<Int>> = List(20) { List(10) { 0 } },
    val currentPiece: PieceState? = null,
    val nextPiece: PieceState? = null,
    val ghostY: Int = 0,
    val clearedRows: List<Int> = emptyList(),
    val lastEvent: GameEvent = GameEvent.NONE,
    val highScore: Int = 0
)
