package com.brickgame.tetris.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TetrisGameTest {

    private var now = 1_000L
    private lateinit var game: TetrisGame

    @Before
    fun setUp() {
        now = 1_000L
        game = TetrisGame(clock = { now })
    }

    private val state get() = game.state.value

    private fun emptyBoard() = Array(TetrisGame.TOTAL_HEIGHT) { IntArray(TetrisGame.BOARD_WIDTH) }

    @Test
    fun `start game spawns a piece and fills the next queue`() {
        game.startGame()
        assertEquals(GameStatus.PLAYING, state.status)
        assertNotNull(state.currentPiece)
        assertEquals(TetrisGame.NEXT_QUEUE_SIZE, state.nextPieces.size)
        assertEquals(0, state.score)
    }

    @Test
    fun `first seven pieces come from one 7-bag`() {
        game.startGame()
        val seen = mutableListOf<TetrominoType>()
        repeat(7) {
            assertEquals(GameStatus.PLAYING, state.status)
            seen += state.currentPiece!!.type
            game.hardDrop()
            game.completePendingLineClear()
        }
        assertEquals(TetrominoType.entries.toSet(), seen.toSet())
    }

    @Test
    fun `gravity does not award points but soft drop does`() {
        game.startGame()
        assertEquals(MoveResult.MOVED, game.gravityStep())
        assertEquals(0, state.score)
        assertEquals(MoveResult.MOVED, game.moveDown())
        assertEquals(1, state.score)
    }

    @Test
    fun `hard drop awards two points per row and locks the piece`() {
        game.startGame()
        val piece = state.currentPiece!!
        val distance = state.ghostY - piece.position.y
        val lockEvent = state.lockEvent
        assertEquals(distance, game.hardDrop())
        assertEquals(distance * 2, state.score)
        assertEquals(lockEvent + 1, state.lockEvent)
    }

    @Test
    fun `completing a line clears it after the animation step`() {
        val rows = emptyBoard()
        for (x in 0..5) rows[TetrisGame.TOTAL_HEIGHT - 1][x] = 1
        game.startGame()
        game.setUpForTest(rows, TetrominoType.I)
        repeat(3) { assertTrue(game.moveRight()) }  // horizontal I now covers columns 6..9
        game.hardDrop()

        assertTrue(game.isPendingLineClear())
        assertEquals(1, state.linesCleared)
        assertEquals(listOf(TetrisGame.BOARD_HEIGHT - 1), state.clearedLineRows)

        game.completePendingLineClear()
        assertFalse(game.isPendingLineClear())
        assertEquals(1, state.lines)
        assertTrue(state.score > 0)
        // Bottom row is empty again (the piece that completed it was removed with it)
        assertTrue(state.board.last().all { it == 0 })
    }

    @Test
    fun `identical clears in a row each raise a new action event`() {
        val rows = emptyBoard()
        for (x in 0..5) rows[TetrisGame.TOTAL_HEIGHT - 1][x] = 1
        game.startGame()
        game.setUpForTest(rows, TetrominoType.I)
        repeat(3) { game.moveRight() }
        game.hardDrop()
        val first = state.actionEvent
        game.completePendingLineClear()

        game.setUpForTest(rows, TetrominoType.I)
        repeat(3) { game.moveRight() }
        game.hardDrop()
        assertTrue(state.actionEvent > first)
    }

    @Test
    fun `ultra ends at two minutes even without a line clear`() {
        game.setGameMode(GameMode.ULTRA)
        game.startGame()
        now += TetrisGame.ULTRA_TIME_LIMIT_MS - 1
        assertFalse(game.checkTimeLimit())
        now += 1
        assertTrue(game.checkTimeLimit())
        assertEquals(GameStatus.GAME_OVER, state.status)
        assertEquals(TetrisGame.ULTRA_TIME_LIMIT_MS, state.elapsedTimeMs)
    }

    @Test
    fun `paused time does not count towards the ultra limit`() {
        game.setGameMode(GameMode.ULTRA)
        game.startGame()
        now += 60_000
        game.pauseGame()
        now += 10 * 60_000  // ten minutes in the pause menu
        game.resumeGame()
        now += 59_000
        assertFalse(game.checkTimeLimit())
        assertEquals(GameStatus.PLAYING, state.status)
    }

    @Test
    fun `time limit is ignored outside ultra`() {
        game.setGameMode(GameMode.MARATHON)
        game.startGame()
        now += 10 * TetrisGame.ULTRA_TIME_LIMIT_MS
        assertFalse(game.checkTimeLimit())
        assertEquals(GameStatus.PLAYING, state.status)
    }

    @Test
    fun `lock delay locks a grounded piece after 500 ms`() {
        game.startGame()
        while (game.moveDown() == MoveResult.MOVED) { /* fall to the floor */ }
        val lockEvent = state.lockEvent
        now += TetrisGame.LOCK_DELAY_MS - 1
        assertFalse(game.checkLockDelay())
        now += 1
        assertTrue(game.checkLockDelay())
        assertEquals(lockEvent + 1, state.lockEvent)
    }

    @Test
    fun `hold swaps once per piece`() {
        game.startGame()
        val first = state.currentPiece!!.type
        assertTrue(game.holdCurrentPiece())
        assertEquals(first, state.holdPiece!!.type)
        assertFalse(game.holdCurrentPiece())
    }

    @Test
    fun `garbage rises from the bottom when the next piece spawns`() {
        game.startGame()
        game.setUpForTest(emptyBoard(), TetrominoType.O)
        game.queueGarbage(2)
        assertEquals(2, game.pendingGarbageRows())
        game.hardDrop()   // locking spawns the next piece, which applies the garbage
        assertEquals(0, game.pendingGarbageRows())
        val bottom = state.board.takeLast(2)
        val holes = bottom.map { row -> row.indexOf(0) }
        for (row in bottom) {
            assertEquals(1, row.count { it == 0 })
            assertEquals(TetrisGame.BOARD_WIDTH - 1, row.count { it == TetrisGame.GARBAGE_CELL })
        }
        assertEquals(holes[0], holes[1])   // one gap column for the whole batch
    }

    @Test
    fun `garbage pushes the existing stack up`() {
        val rows = emptyBoard()
        rows[TetrisGame.TOTAL_HEIGHT - 1][0] = 3
        game.startGame()
        game.setUpForTest(rows, TetrominoType.O)
        game.applyGarbage(3, holeColumn = 5)
        game.moveLeft()   // emits a fresh state
        val b = state.board
        assertEquals(3, b[TetrisGame.BOARD_HEIGHT - 4][0])
        for (y in TetrisGame.BOARD_HEIGHT - 3 until TetrisGame.BOARD_HEIGHT) {
            assertEquals(0, b[y][5])
            assertEquals(TetrisGame.GARBAGE_CELL, b[y][0])
        }
    }
}
