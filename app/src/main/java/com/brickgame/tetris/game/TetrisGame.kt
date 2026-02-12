package com.brickgame.tetris.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TetrisGame {

    companion object {
        const val BOARD_WIDTH = 10
        const val BOARD_HEIGHT = 20
        const val HIDDEN_ROWS = 4
        const val TOTAL_HEIGHT = BOARD_HEIGHT + HIDDEN_ROWS
        const val NEXT_QUEUE_SIZE = 3
        const val LOCK_DELAY_MS = 500L
        const val MAX_LOCK_MOVES = 15

        // SRS-compliant spawn shapes (all in bounding boxes)
        val TETROMINOS = mapOf(
            TetrominoType.I to arrayOf(
                intArrayOf(0, 0, 0, 0),
                intArrayOf(1, 1, 1, 1),
                intArrayOf(0, 0, 0, 0),
                intArrayOf(0, 0, 0, 0)
            ),
            TetrominoType.O to arrayOf(
                intArrayOf(1, 1),
                intArrayOf(1, 1)
            ),
            TetrominoType.T to arrayOf(
                intArrayOf(0, 1, 0),
                intArrayOf(1, 1, 1),
                intArrayOf(0, 0, 0)
            ),
            TetrominoType.S to arrayOf(
                intArrayOf(0, 1, 1),
                intArrayOf(1, 1, 0),
                intArrayOf(0, 0, 0)
            ),
            TetrominoType.Z to arrayOf(
                intArrayOf(1, 1, 0),
                intArrayOf(0, 1, 1),
                intArrayOf(0, 0, 0)
            ),
            TetrominoType.J to arrayOf(
                intArrayOf(1, 0, 0),
                intArrayOf(1, 1, 1),
                intArrayOf(0, 0, 0)
            ),
            TetrominoType.L to arrayOf(
                intArrayOf(0, 0, 1),
                intArrayOf(1, 1, 1),
                intArrayOf(0, 0, 0)
            )
        )
    }

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var board = Array(TOTAL_HEIGHT) { IntArray(BOARD_WIDTH) }
    private var currentPiece: Tetromino? = null
    private var currentPosition = Position(0, 0)
    private var isRunning = false
    private var difficulty = Difficulty.NORMAL

    // 7-bag randomizer
    private val nextQueue = mutableListOf<Tetromino>()
    private var currentBag = mutableListOf<TetrominoType>()

    // Hold piece
    private var holdPiece: Tetromino? = null
    private var holdUsedThisTurn = false

    // Line clear animation pending state
    private var pendingLineClear = false
    private var pendingClearedLines: List<Int> = emptyList()
    private var pendingPoints = 0
    private var pendingNewLines = 0
    private var pendingNewLevel = 0
    private var pendingLabel = ""

    // Scoring
    private var comboCount = -1
    private var lastClearWasDifficult = false
    private var backToBackCount = 0

    // Lock delay
    private var lockDelayActive = false
    private var lockMoveCount = 0
    private var lockDelayStartTime = 0L

    // T-spin tracking
    private var lastActionWasRotation = false
    private var lastKickUsed = false

    // Game mode
    private var gameMode = GameMode.MARATHON
    private var gameStartTime = 0L

    fun setDifficulty(diff: Difficulty) { difficulty = diff }
    fun setGameMode(mode: GameMode) { gameMode = mode }

    fun startGame() {
        board = Array(TOTAL_HEIGHT) { IntArray(BOARD_WIDTH) }
        currentPiece = null
        holdPiece = null
        holdUsedThisTurn = false
        pendingLineClear = false
        pendingClearedLines = emptyList()
        comboCount = -1
        lastClearWasDifficult = false
        backToBackCount = 0
        lockDelayActive = false
        lockMoveCount = 0
        lastActionWasRotation = false
        lastKickUsed = false
        currentBag.clear()
        nextQueue.clear()
        gameStartTime = System.currentTimeMillis()

        // Fill next queue
        repeat(NEXT_QUEUE_SIZE + 1) { nextQueue.add(generateFromBag()) }

        _state.update {
            GameState(
                status = GameStatus.PLAYING,
                score = 0,
                level = difficulty.startLevel,
                lines = 0,
                board = getVisibleBoard(),
                difficulty = difficulty,
                gameMode = gameMode
            )
        }
        spawnPiece()
    }

    fun pauseGame() {
        if (_state.value.status == GameStatus.PLAYING) {
            isRunning = false
            _state.update { it.copy(status = GameStatus.PAUSED) }
        }
    }

    fun resumeGame() {
        if (_state.value.status == GameStatus.PAUSED) {
            isRunning = true
            _state.update { it.copy(status = GameStatus.PLAYING) }
        }
    }

    fun resetToMenu() {
        isRunning = false
        _state.update { GameState(status = GameStatus.MENU, highScore = it.highScore) }
    }

    fun isGameActive(): Boolean =
        isRunning && _state.value.status == GameStatus.PLAYING && !pendingLineClear

    fun isPendingLineClear(): Boolean = pendingLineClear

    // ===== Hold Piece =====

    fun holdCurrentPiece(): Boolean {
        if (!canMove() || holdUsedThisTurn) return false
        val current = currentPiece ?: return false
        val freshPiece = createPiece(current.type)

        if (holdPiece == null) {
            holdPiece = freshPiece
            spawnPiece()
        } else {
            val held = holdPiece!!
            holdPiece = freshPiece
            currentPiece = createPiece(held.type)
            val piece = currentPiece!!
            val startX = (BOARD_WIDTH - piece.shape[0].size) / 2
            currentPosition = Position(startX, HIDDEN_ROWS - 1)
            if (checkCollision(piece.shape, currentPosition)) {
                gameOver()
                return false
            }
        }

        holdUsedThisTurn = true
        lastActionWasRotation = false
        lockDelayActive = false
        lockMoveCount = 0
        updateState()
        return true
    }

    // ===== Line Clear Completion =====

    fun completePendingLineClear() {
        if (!pendingLineClear) return
        actuallyRemoveCompletedLines()

        _state.update {
            it.copy(
                score = it.score + pendingPoints,
                lines = pendingNewLines,
                level = pendingNewLevel.coerceAtMost(20),
                linesCleared = 0,
                clearedLineRows = emptyList(),
                board = getVisibleBoard(),
                lastActionLabel = pendingLabel
            )
        }

        pendingLineClear = false
        pendingClearedLines = emptyList()
        pendingLabel = ""

        if (checkWinCondition()) return
        spawnPiece()
    }

    // ===== Movement =====

    fun moveLeft(): Boolean {
        if (!canMove()) return false
        val newPos = Position(currentPosition.x - 1, currentPosition.y)
        if (!checkCollision(currentPiece!!.shape, newPos)) {
            currentPosition = newPos
            lastActionWasRotation = false
            resetLockDelayOnMove()
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
            lastActionWasRotation = false
            resetLockDelayOnMove()
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
            lastActionWasRotation = false
            // Soft drop: 1 point per cell
            _state.update { it.copy(score = it.score + 1) }
            if (lockDelayActive && !isTouchingGround()) {
                lockDelayActive = false
                lockMoveCount = 0
            }
            updateState()
            MoveResult.MOVED
        } else {
            if (!lockDelayActive) {
                lockDelayActive = true
                lockDelayStartTime = System.currentTimeMillis()
                lockMoveCount = 0
            }
            MoveResult.BLOCKED
        }
    }

    fun hardDrop(): Int {
        if (!canMove()) return 0
        var dropDistance = 0
        while (!checkCollision(currentPiece!!.shape,
                Position(currentPosition.x, currentPosition.y + 1))) {
            currentPosition = Position(currentPosition.x, currentPosition.y + 1)
            dropDistance++
        }
        if (dropDistance > 0) {
            _state.update { it.copy(score = it.score + dropDistance * 2) }
        }
        lastActionWasRotation = false
        lockPiece()
        return dropDistance
    }

    // ===== Lock Delay =====

    fun checkLockDelay(): Boolean {
        if (!lockDelayActive) return false
        if (!isTouchingGround()) {
            lockDelayActive = false
            lockMoveCount = 0
            return false
        }
        val elapsed = System.currentTimeMillis() - lockDelayStartTime
        if (elapsed >= LOCK_DELAY_MS || lockMoveCount >= MAX_LOCK_MOVES) {
            lockPiece()
            return true
        }
        return false
    }

    // ===== Rotation (SRS) =====

    fun rotate(): Boolean = rotateDirection(clockwise = true)
    fun rotateCounterClockwise(): Boolean = rotateDirection(clockwise = false)

    private fun rotateDirection(clockwise: Boolean): Boolean {
        if (!canMove()) return false
        val piece = currentPiece ?: return false
        val fromRotation = piece.rotationState
        val toRotation = if (clockwise) (fromRotation + 1) % 4 else (fromRotation + 3) % 4
        val rotated = piece.rotateToState(toRotation)
        val kicks = SRS.getKickOffsets(piece.type, fromRotation, toRotation)

        for ((i, kick) in kicks.withIndex()) {
            // SRS convention: positive Y = up, but our board Y increases downward
            val kickedPos = Position(currentPosition.x + kick.first, currentPosition.y - kick.second)
            if (!checkCollision(rotated.shape, kickedPos)) {
                currentPiece = rotated
                currentPosition = kickedPos
                lastActionWasRotation = true
                lastKickUsed = i > 0
                resetLockDelayOnMove()
                updateState()
                return true
            }
        }
        return false
    }

    // ===== Ghost / Speed =====

    fun calculateGhostY(): Int {
        val piece = currentPiece ?: return currentPosition.y
        var ghostY = currentPosition.y
        while (!checkCollision(piece.shape, Position(currentPosition.x, ghostY + 1))) {
            ghostY++
        }
        return ghostY
    }

    fun getDropSpeed(): Long {
        // Infinity mode: fixed Level 1 speed regardless of level
        if (gameMode == GameMode.INFINITY) {
            return (1000L * difficulty.speedMultiplier).toLong().coerceAtLeast(16L)
        }
        val level = _state.value.level
        // Guideline-inspired speed curve (NES-like)
        val baseSpeed = when {
            level <= 1 -> 1000L
            level <= 5 -> 1000L - (level - 1) * 100L
            level <= 10 -> 600L - (level - 5) * 60L
            level <= 15 -> 300L - (level - 10) * 30L
            level <= 20 -> 150L - (level - 15) * 15L
            else -> 50L
        }
        return (baseSpeed * difficulty.speedMultiplier).toLong().coerceAtLeast(16L)
    }

    // ===== Private Helpers =====

    private fun canMove(): Boolean =
        isRunning && currentPiece != null &&
                _state.value.status == GameStatus.PLAYING && !pendingLineClear

    private fun isTouchingGround(): Boolean {
        val piece = currentPiece ?: return false
        return checkCollision(piece.shape, Position(currentPosition.x, currentPosition.y + 1))
    }

    private fun resetLockDelayOnMove() {
        if (lockDelayActive && lockMoveCount < MAX_LOCK_MOVES) {
            lockDelayStartTime = System.currentTimeMillis()
            lockMoveCount++
        }
    }

    private fun checkCollision(shape: Array<IntArray>, pos: Position): Boolean {
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x] != 0) {
                    val boardX = pos.x + x
                    val boardY = pos.y + y
                    if (boardX < 0 || boardX >= BOARD_WIDTH) return true
                    if (boardY >= TOTAL_HEIGHT) return true
                    if (boardY >= 0 && board[boardY][boardX] != 0) return true
                }
            }
        }
        return false
    }

    private fun spawnPiece() {
        if (nextQueue.isEmpty()) nextQueue.add(generateFromBag())
        currentPiece = nextQueue.removeAt(0)
        while (nextQueue.size < NEXT_QUEUE_SIZE) nextQueue.add(generateFromBag())

        val piece = currentPiece ?: return
        val startX = (BOARD_WIDTH - piece.shape[0].size) / 2
        currentPosition = Position(startX, HIDDEN_ROWS - piece.shape.size)

        // Try to drop one row immediately (Guideline behaviour)
        val oneBelow = Position(currentPosition.x, currentPosition.y + 1)
        if (!checkCollision(piece.shape, oneBelow)) {
            currentPosition = oneBelow
        }

        if (checkCollision(piece.shape, currentPosition)) {
            gameOver()
            return
        }

        isRunning = true
        holdUsedThisTurn = false
        lastActionWasRotation = false
        lastKickUsed = false
        lockDelayActive = false
        lockMoveCount = 0
        updateState()
    }

    private fun lockPiece() {
        val piece = currentPiece ?: return

        // T-spin detection (must happen before placing)
        val tSpinType = if (piece.type == TetrominoType.T && lastActionWasRotation) {
            SRS.detectTSpin(
                board, currentPosition, piece.rotationState,
                lastKickUsed, BOARD_WIDTH, TOTAL_HEIGHT
            )
        } else TSpinType.NONE

        // Place piece on internal board
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] != 0) {
                    val boardY = currentPosition.y + y
                    val boardX = currentPosition.x + x
                    if (boardY in 0 until TOTAL_HEIGHT && boardX in 0 until BOARD_WIDTH) {
                        board[boardY][boardX] = piece.type.ordinal + 1
                    }
                }
            }
        }

        // Lock-out: piece locked entirely above visible area
        val allAbove = piece.shape.indices.all { y ->
            piece.shape[y].indices.all { x ->
                piece.shape[y][x] == 0 || (currentPosition.y + y < HIDDEN_ROWS)
            }
        }
        if (allAbove) {
            currentPiece = null
            gameOver()
            return
        }

        currentPiece = null
        lockDelayActive = false
        lockMoveCount = 0

        val clearedLines = findCompletedLines()
        val clearedCount = clearedLines.size

        if (clearedCount > 0) {
            comboCount++
            val isDifficult = clearedCount == 4 ||
                    (tSpinType != TSpinType.NONE && clearedCount > 0)
            val isB2B = lastClearWasDifficult && isDifficult

            val scoreResult = ScoreCalculator.calculateScore(
                linesCleared = clearedCount, level = _state.value.level,
                tSpinType = tSpinType, isBackToBack = isB2B,
                comboCount = comboCount,
                difficultyMultiplier = difficulty.scoreMultiplier
            )

            if (isDifficult) {
                if (lastClearWasDifficult) backToBackCount++
                lastClearWasDifficult = true
            } else {
                lastClearWasDifficult = false
                backToBackCount = 0
            }

            val currentState = _state.value
            val newLines = currentState.lines + clearedCount
            val newLevel = maxOf(difficulty.startLevel, (newLines / 10) + 1)

            pendingLineClear = true
            pendingClearedLines = clearedLines.map { it - HIDDEN_ROWS }.filter { it >= 0 }
            pendingPoints = scoreResult.points
            pendingNewLines = newLines
            pendingNewLevel = newLevel
            pendingLabel = scoreResult.label

            _state.update {
                it.copy(
                    linesCleared = clearedCount,
                    clearedLineRows = pendingClearedLines,
                    board = getVisibleBoard(),
                    currentPiece = null,
                    lastActionLabel = scoreResult.label,
                    tSpinType = tSpinType,
                    comboCount = comboCount,
                    backToBackCount = backToBackCount
                )
            }
        } else {
            comboCount = -1
            // T-spin with 0 lines still earns points
            if (tSpinType != TSpinType.NONE) {
                val scoreResult = ScoreCalculator.calculateScore(
                    linesCleared = 0, level = _state.value.level,
                    tSpinType = tSpinType, isBackToBack = false,
                    comboCount = 0, difficultyMultiplier = difficulty.scoreMultiplier
                )
                _state.update {
                    it.copy(
                        score = it.score + scoreResult.points,
                        lastActionLabel = scoreResult.label
                    )
                }
            }
            spawnPiece()
        }
    }

    private fun findCompletedLines(): List<Int> =
        (0 until TOTAL_HEIGHT).filter { y ->
            (0 until BOARD_WIDTH).all { board[y][it] != 0 }
        }

    private fun actuallyRemoveCompletedLines() {
        var removed: Boolean
        do {
            removed = false
            for (y in TOTAL_HEIGHT - 1 downTo 0) {
                if ((0 until BOARD_WIDTH).all { board[y][it] != 0 }) {
                    removeRow(y)
                    removed = true
                    break
                }
            }
        } while (removed)
    }

    private fun removeRow(row: Int) {
        for (y in row downTo 1) { board[y] = board[y - 1].clone() }
        board[0] = IntArray(BOARD_WIDTH)
    }

    private fun gameOver() {
        isRunning = false
        _state.update {
            it.copy(
                status = GameStatus.GAME_OVER,
                board = getVisibleBoard(),
                currentPiece = null
            )
        }
    }

    private fun checkWinCondition(): Boolean {
        when (gameMode) {
            GameMode.SPRINT -> {
                if (_state.value.lines >= 40) {
                    val elapsed = System.currentTimeMillis() - gameStartTime
                    _state.update {
                        it.copy(status = GameStatus.GAME_OVER, elapsedTimeMs = elapsed)
                    }
                    isRunning = false
                    return true
                }
            }
            GameMode.ULTRA -> {
                val elapsed = System.currentTimeMillis() - gameStartTime
                if (elapsed >= 120_000L) {
                    _state.update {
                        it.copy(status = GameStatus.GAME_OVER, elapsedTimeMs = 120_000L)
                    }
                    isRunning = false
                    return true
                }
            }
            else -> {}
        }
        return false
    }

    private fun updateState() {
        val ghostY = calculateGhostY()
        val visibleGhostY = ghostY - HIDDEN_ROWS
        val visiblePieceY = currentPosition.y - HIDDEN_ROWS

        _state.update { state ->
            state.copy(
                board = getVisibleBoard(),
                currentPiece = currentPiece?.let {
                    PieceState(
                        it.type,
                        Position(currentPosition.x, visiblePieceY),
                        it.shape.map { r -> r.toList() }
                    )
                },
                nextPieces = nextQueue.take(NEXT_QUEUE_SIZE).map { p ->
                    PieceState(p.type, Position(0, 0), p.shape.map { r -> r.toList() })
                },
                holdPiece = holdPiece?.let {
                    PieceState(it.type, Position(0, 0), it.shape.map { r -> r.toList() })
                },
                holdUsed = holdUsedThisTurn,
                ghostY = visibleGhostY,
                linesCleared = 0,
                clearedLineRows = emptyList()
            )
        }
    }

    private fun getVisibleBoard(): List<List<Int>> {
        val display = Array(BOARD_HEIGHT) { y -> IntArray(BOARD_WIDTH) { x -> board[y + HIDDEN_ROWS][x] } }
        currentPiece?.let { piece ->
            for (y in piece.shape.indices) {
                for (x in piece.shape[y].indices) {
                    if (piece.shape[y][x] != 0) {
                        val boardY = currentPosition.y + y - HIDDEN_ROWS
                        val boardX = currentPosition.x + x
                        if (boardY in 0 until BOARD_HEIGHT && boardX in 0 until BOARD_WIDTH) {
                            display[boardY][boardX] = piece.type.ordinal + 1
                        }
                    }
                }
            }
        }
        return display.map { it.toList() }
    }

    // ===== 7-Bag Randomiser =====

    private fun generateFromBag(): Tetromino {
        if (currentBag.isEmpty()) {
            currentBag = TetrominoType.entries.toMutableList().also { it.shuffle() }
        }
        return createPiece(currentBag.removeAt(0))
    }

    private fun createPiece(type: TetrominoType): Tetromino =
        Tetromino(type, TETROMINOS[type]!!.map { it.clone() }.toTypedArray(), 0)
}

// ===== Enums =====

enum class TetrominoType { I, O, T, S, Z, J, L }
enum class GameStatus { MENU, PLAYING, PAUSED, GAME_OVER }
enum class MoveResult { MOVED, LOCKED, BLOCKED }

enum class GameMode(val displayName: String, val description: String) {
    MARATHON("Marathon", "Classic endless mode"),
    SPRINT("Sprint 40L", "Clear 40 lines as fast as possible"),
    ULTRA("Ultra 2min", "Highest score in 2 minutes"),
    INFINITY("Infinity", "Relaxing endless play at constant speed")
}

// ===== Data Classes =====

data class Position(val x: Int, val y: Int)

data class Tetromino(
    val type: TetrominoType,
    val shape: Array<IntArray>,
    val rotationState: Int = 0
) {
    fun rotateToState(targetState: Int): Tetromino {
        var rotated = shape.map { it.clone() }.toTypedArray()
        val rotations = ((targetState - rotationState) + 4) % 4
        repeat(rotations) {
            val h = rotated.size
            val w = rotated[0].size
            val ns = Array(w) { IntArray(h) }
            for (y in rotated.indices) {
                for (x in rotated[y].indices) {
                    ns[x][h - 1 - y] = rotated[y][x]
                }
            }
            rotated = ns
        }
        return Tetromino(type, rotated, targetState)
    }

    fun rotate(): Tetromino = rotateToState((rotationState + 1) % 4)
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
    val nextPieces: List<PieceState> = emptyList(),
    val nextPiece: PieceState? = null,
    val holdPiece: PieceState? = null,
    val holdUsed: Boolean = false,
    val ghostY: Int = 0,
    val linesCleared: Int = 0,
    val clearedLineRows: List<Int> = emptyList(),
    val highScore: Int = 0,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val gameMode: GameMode = GameMode.MARATHON,
    val lastActionLabel: String = "",
    val tSpinType: TSpinType = TSpinType.NONE,
    val comboCount: Int = 0,
    val backToBackCount: Int = 0,
    val elapsedTimeMs: Long = 0
) {
    /** Backward compat: first next piece */
    val effectiveNextPiece: PieceState? get() = nextPieces.firstOrNull() ?: nextPiece
}
