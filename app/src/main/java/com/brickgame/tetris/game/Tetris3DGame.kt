package com.brickgame.tetris.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 3D Tetris Game Engine.
 * Board is a 3D grid (width × depth × height).
 * Pieces are 3D shapes that fall along the Y (height) axis.
 * Players can rotate pieces on XZ plane and tilt on XY/ZY planes.
 */
class Tetris3DGame {

    companion object {
        const val BOARD_W = 6  // X axis
        const val BOARD_D = 6  // Z axis
        const val BOARD_H = 14 // Y axis (height)
    }

    // 3D board: board[y][z][x] — 0 = empty, >0 = filled (color index)
    private var board = Array(BOARD_H) { Array(BOARD_D) { IntArray(BOARD_W) } }
    private var currentPiece: Piece3D? = null
    private var nextPieces = mutableListOf<Piece3DType>()
    private var holdType: Piece3DType? = null
    private var holdUsed = false
    private var score = 0
    private var level = 1
    private var layers = 0 // total layers cleared
    private var clearingLayersList = listOf<Int>() // layers currently animating
    private var clearAnimProgress = 0f // 0..1
    private var clearAnimTimer = 0L
    private var status = GameStatus.MENU
    private var dropTimer = 0L
    private var lockTimer = 0L
    private var isLocking = false
    private var bag = mutableListOf<Piece3DType>()
    var autoGravity = true  // When false, pieces only drop on manual soft-drop

    fun toggleGravity() {
        autoGravity = !autoGravity
        emitState()
    }

    private val _state = MutableStateFlow(Game3DState())
    val state: StateFlow<Game3DState> = _state.asStateFlow()

    fun start() {
        board = Array(BOARD_H) { Array(BOARD_D) { IntArray(BOARD_W) } }
        score = 0; level = 1; layers = 0; holdType = null; holdUsed = false
        status = GameStatus.PLAYING; dropTimer = 0L; lockTimer = 0L; isLocking = false
        bag.clear(); nextPieces.clear()
        repeat(3) { nextPieces.add(nextFromBag()) }
        spawnPiece()
        emitState()
    }

    fun tick(deltaMs: Long) {
        if (status != GameStatus.PLAYING) return
        // If clearing animation is running, advance it and skip normal tick
        if (tickClearAnimation(deltaMs)) return
        if (!autoGravity) return  // No auto-drop in manual mode
        dropTimer += deltaMs
        val speed = dropSpeed()
        if (isLocking) {
            lockTimer += deltaMs
            if (lockTimer >= 600L) { lockPiece(); return }
        }
        if (dropTimer >= speed) {
            dropTimer = 0L
            moveDown()
        }
    }

    /** Manual soft-drop — works in both auto and manual mode */
    fun softDrop() {
        if (status != GameStatus.PLAYING) return
        moveDown()
    }

    fun moveX(dx: Int): Boolean = tryMove(dx, 0, 0)
    fun moveZ(dz: Int): Boolean = tryMove(0, 0, dz)
    fun hardDrop(): Int {
        val p = currentPiece ?: return 0
        var dy = 0
        while (canPlace(p.blocks, p.x, p.y - dy - 1, p.z)) dy++
        currentPiece = p.copy(y = p.y - dy)
        score += dy * 2
        lockPiece()
        return dy
    }

    fun rotateXZ(): Boolean {
        val p = currentPiece ?: return false
        val rotated = p.blocks.map { Block3D(-it.z, it.y, it.x) }
        if (canPlace(rotated, p.x, p.y, p.z)) {
            currentPiece = p.copy(blocks = rotated)
            resetLock(); emitState(); return true
        } else {
            // Wall kick attempts
            for (kick in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                if (canPlace(rotated, p.x + kick.first, p.y, p.z + kick.second)) {
                    currentPiece = p.copy(blocks = rotated, x = p.x + kick.first, z = p.z + kick.second)
                    resetLock(); emitState(); return true
                }
            }
        }
        emitState(); return false
    }

    fun rotateXY(): Boolean {
        val p = currentPiece ?: return false
        val rotated = p.blocks.map { Block3D(it.x, -it.z, it.y) }
        if (canPlace(rotated, p.x, p.y, p.z)) {
            currentPiece = p.copy(blocks = rotated); resetLock()
            emitState(); return true
        }
        emitState(); return false
    }

    fun hold(): Boolean {
        if (holdUsed) return false
        val p = currentPiece ?: return false
        val prev = holdType
        holdType = p.type
        holdUsed = true
        if (prev != null) {
            currentPiece = createPiece(prev)
        } else {
            spawnPiece()
        }
        emitState(); return true
    }

    fun pause() { if (status == GameStatus.PLAYING) { status = GameStatus.PAUSED; emitState() } }
    fun resume() { if (status == GameStatus.PAUSED) { status = GameStatus.PLAYING; emitState() } }
    fun resetToMenu() { status = GameStatus.MENU; emitState() }

    // Ghost Y: lowest Y the piece can drop to
    fun ghostY(): Int {
        val p = currentPiece ?: return 0
        var dy = 0
        while (canPlace(p.blocks, p.x, p.y - dy - 1, p.z)) dy++
        return p.y - dy
    }

    private fun moveDown() {
        val p = currentPiece ?: return
        if (canPlace(p.blocks, p.x, p.y - 1, p.z)) {
            currentPiece = p.copy(y = p.y - 1)
            isLocking = false
        } else {
            isLocking = true
            lockTimer = 0L
        }
        emitState()
    }

    private fun tryMove(dx: Int, dy: Int, dz: Int): Boolean {
        val p = currentPiece ?: return false
        if (canPlace(p.blocks, p.x + dx, p.y + dy, p.z + dz)) {
            currentPiece = p.copy(x = p.x + dx, y = p.y + dy, z = p.z + dz)
            resetLock(); emitState(); return true
        }
        emitState(); return false
    }

    private fun resetLock() { if (isLocking) { lockTimer = 0L } }

    private fun lockPiece() {
        val p = currentPiece ?: return
        // Place blocks on board
        for (b in p.blocks) {
            val bx = p.x + b.x; val by = p.y + b.y; val bz = p.z + b.z
            if (by in 0 until BOARD_H && bz in 0 until BOARD_D && bx in 0 until BOARD_W) {
                board[by][bz][bx] = p.type.colorIndex
            }
        }
        // Check for complete layers
        clearLayers()
        holdUsed = false
        isLocking = false
        spawnPiece()
        emitState()
    }

    private fun clearLayers() {
        // Find all full layers
        val fullLayers = mutableListOf<Int>()
        for (y in BOARD_H - 1 downTo 0) {
            if (isLayerFull(y)) fullLayers.add(y)
        }
        if (fullLayers.isEmpty()) return

        // Start clearing animation
        clearingLayersList = fullLayers
        clearAnimProgress = 0f
        clearAnimTimer = 0L
        // Score and level update happen when animation completes
    }

    /** Called from tick to advance clearing animation. Returns true if still animating. */
    private fun tickClearAnimation(dt: Long): Boolean {
        if (clearingLayersList.isEmpty()) return false
        clearAnimTimer += dt
        val duration = 500L // 500ms animation
        clearAnimProgress = (clearAnimTimer.toFloat() / duration).coerceIn(0f, 1f)
        emitState()

        if (clearAnimProgress >= 1f) {
            // Animation complete — actually remove layers
            val cleared = clearingLayersList.size
            // Sort descending so we remove from top first
            val sortedLayers = clearingLayersList.sortedDescending()
            for (y in sortedLayers) {
                for (yy in y until BOARD_H - 1) {
                    board[yy] = board[yy + 1].map { it.clone() }.toTypedArray()
                }
                board[BOARD_H - 1] = Array(BOARD_D) { IntArray(BOARD_W) }
            }
            layers += cleared
            score += when (cleared) {
                1 -> 100 * level
                2 -> 300 * level
                3 -> 500 * level
                else -> 800 * level
            }
            level = (layers / 10) + 1
            clearingLayersList = emptyList()
            clearAnimProgress = 0f
            clearAnimTimer = 0L
            emitState()
        }
        return clearingLayersList.isNotEmpty()
    }

    private fun isLayerFull(y: Int): Boolean {
        for (z in 0 until BOARD_D) for (x in 0 until BOARD_W) {
            if (board[y][z][x] == 0) return false
        }
        return true
    }

    private fun canPlace(blocks: List<Block3D>, ox: Int, oy: Int, oz: Int): Boolean {
        for (b in blocks) {
            val bx = ox + b.x; val by = oy + b.y; val bz = oz + b.z
            if (bx < 0 || bx >= BOARD_W || bz < 0 || bz >= BOARD_D || by < 0) return false
            if (by >= BOARD_H) continue // above board is ok
            if (board[by][bz][bx] != 0) return false
        }
        return true
    }

    private fun spawnPiece() {
        val type = nextPieces.removeFirst()
        nextPieces.add(nextFromBag())
        val piece = createPiece(type)
        currentPiece = piece
        // Check if spawn position is blocked
        if (!canPlace(piece.blocks, piece.x, piece.y, piece.z)) {
            status = GameStatus.GAME_OVER
        }
    }

    private fun createPiece(type: Piece3DType): Piece3D {
        return Piece3D(
            type = type,
            blocks = type.blocks.toList(),
            x = BOARD_W / 2 - 1,
            y = BOARD_H - 2,
            z = BOARD_D / 2 - 1
        )
    }

    private fun nextFromBag(): Piece3DType {
        if (bag.isEmpty()) {
            bag.addAll(Piece3DType.entries.toList().shuffled())
        }
        return bag.removeFirst()
    }

    private fun dropSpeed(): Long = maxOf(100L, 1000L - (level - 1) * 80L)

    private fun emitState() {
        val p = currentPiece
        _state.update {
            Game3DState(
                status = status,
                score = score,
                level = level,
                layers = layers,
                board = board.map { layer -> layer.map { row -> row.toList() } },
                currentPiece = p?.let { Piece3DState(it.type, it.blocks, it.x, it.y, it.z) },
                ghostY = ghostY(),
                nextPieces = nextPieces.toList(),
                holdPiece = holdType,
                holdUsed = holdUsed,
                autoGravity = autoGravity,
                clearingLayers = clearingLayersList,
                clearAnimProgress = clearAnimProgress
            )
        }
    }
}

// ===== 3D Data Types =====

data class Block3D(val x: Int, val y: Int, val z: Int)

data class Piece3D(
    val type: Piece3DType,
    val blocks: List<Block3D>,
    val x: Int, val y: Int, val z: Int
)

data class Piece3DState(
    val type: Piece3DType,
    val blocks: List<Block3D>,
    val x: Int, val y: Int, val z: Int
)

data class Game3DState(
    val status: GameStatus = GameStatus.MENU,
    val score: Int = 0,
    val level: Int = 1,
    val layers: Int = 0,
    val board: List<List<List<Int>>> = emptyList(), // [y][z][x]
    val currentPiece: Piece3DState? = null,
    val ghostY: Int = 0,
    val nextPieces: List<Piece3DType> = emptyList(),
    val holdPiece: Piece3DType? = null,
    val holdUsed: Boolean = false,
    val autoGravity: Boolean = true,
    val clearingLayers: List<Int> = emptyList(),
    val clearAnimProgress: Float = 0f  // 0..1 animation progress
)

/**
 * 3D piece types. Each defined as a list of Block3D offsets from origin.
 * Flat pieces (same as 2D tetrominos but in 3D space) + new 3D-only shapes.
 */
enum class Piece3DType(val displayName: String, val colorIndex: Int, val blocks: List<Block3D>) {
    // Classic flat pieces (on XZ plane, y=0)
    FLAT_I("I-Flat", 1, listOf(Block3D(0,0,0), Block3D(1,0,0), Block3D(2,0,0), Block3D(3,0,0))),
    FLAT_O("O-Flat", 2, listOf(Block3D(0,0,0), Block3D(1,0,0), Block3D(0,0,1), Block3D(1,0,1))),
    FLAT_T("T-Flat", 3, listOf(Block3D(0,0,0), Block3D(1,0,0), Block3D(2,0,0), Block3D(1,0,1))),
    FLAT_S("S-Flat", 4, listOf(Block3D(1,0,0), Block3D(2,0,0), Block3D(0,0,1), Block3D(1,0,1))),
    FLAT_L("L-Flat", 5, listOf(Block3D(0,0,0), Block3D(1,0,0), Block3D(2,0,0), Block3D(0,0,1))),
    // 3D pieces (use multiple Y levels)
    TOWER("Tower", 6, listOf(Block3D(0,0,0), Block3D(0,1,0), Block3D(0,2,0), Block3D(1,0,0))),
    CORNER("Corner", 7, listOf(Block3D(0,0,0), Block3D(1,0,0), Block3D(0,0,1), Block3D(0,1,0))),
    STEP("Step", 8, listOf(Block3D(0,0,0), Block3D(1,0,0), Block3D(1,1,0), Block3D(1,1,1)));

    companion object {
        fun fromColorIndex(idx: Int): Piece3DType? = entries.find { it.colorIndex == idx }
    }
}
