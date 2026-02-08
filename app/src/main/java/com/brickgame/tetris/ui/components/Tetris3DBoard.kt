package com.brickgame.tetris.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.brickgame.tetris.game.*

/**
 * Isometric 3D Tetris board renderer.
 * Draws a 3D well with falling pieces using isometric projection.
 * Renders back-to-front for correct occlusion.
 */
@Composable
fun Tetris3DBoard(
    state: Game3DState,
    modifier: Modifier = Modifier,
    showGhost: Boolean = true
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // Isometric cell size — fit the board in available space
        // Isometric: x goes right-down, z goes left-down, y goes straight up
        val cellW = w / (Tetris3DGame.BOARD_W + Tetris3DGame.BOARD_D + 2)
        val cellH = cellW * 0.5f
        val cellYH = cellW * 0.7f  // vertical height per Y level

        // Origin point (top center of the well)
        val ox = w / 2f
        val oy = h * 0.12f

        // Draw grid floor (y=0 plane)
        drawFloorGrid(ox, oy, cellW, cellH, cellYH, Tetris3DGame.BOARD_W, Tetris3DGame.BOARD_D, Tetris3DGame.BOARD_H)

        // Draw back walls (for depth reference)
        drawWalls(ox, oy, cellW, cellH, cellYH, Tetris3DGame.BOARD_W, Tetris3DGame.BOARD_D, Tetris3DGame.BOARD_H)

        // Draw placed blocks (back to front for correct occlusion)
        if (state.board.isNotEmpty()) {
            for (y in Tetris3DGame.BOARD_H - 1 downTo 0) {
                // Back-to-front: high z first, then low z; high x first for right side
                for (z in 0 until Tetris3DGame.BOARD_D) {
                    for (x in 0 until Tetris3DGame.BOARD_W) {
                        if (y < state.board.size && z < state.board[y].size && x < state.board[y][z].size) {
                            val colorIdx = state.board[y][z][x]
                            if (colorIdx > 0) {
                                drawIsoCube(ox, oy, cellW, cellH, cellYH, x, y, z, pieceColor(colorIdx), 1f)
                            }
                        }
                    }
                }
            }
        }

        // Draw ghost piece
        val piece = state.currentPiece
        if (piece != null && showGhost && state.ghostY < piece.y) {
            for (b in piece.blocks) {
                drawIsoCube(ox, oy, cellW, cellH, cellYH,
                    piece.x + b.x, state.ghostY + b.y, piece.z + b.z,
                    pieceColor(piece.type.colorIndex), 0.2f)
            }
        }

        // Draw current piece
        if (piece != null) {
            for (b in piece.blocks) {
                drawIsoCube(ox, oy, cellW, cellH, cellYH,
                    piece.x + b.x, piece.y + b.y, piece.z + b.z,
                    pieceColor(piece.type.colorIndex), 1f)
            }
        }
    }
}

/** Convert 3D grid coords to 2D isometric screen coords */
private fun isoProject(ox: Float, oy: Float, cellW: Float, cellH: Float, cellYH: Float,
                       x: Float, y: Float, z: Float): Offset {
    val sx = ox + (x - z) * cellW * 0.5f
    val sy = oy + (x + z) * cellH * 0.5f - y * cellYH
    return Offset(sx, sy)
}

/** Draw a single isometric cube (3 visible faces: top, left, right) */
private fun DrawScope.drawIsoCube(
    ox: Float, oy: Float, cellW: Float, cellH: Float, cellYH: Float,
    gx: Int, gy: Int, gz: Int, color: Color, alpha: Float
) {
    val x = gx.toFloat(); val y = gy.toFloat(); val z = gz.toFloat()

    // 8 corners of the cube
    val tfl = isoProject(ox, oy, cellW, cellH, cellYH, x, y + 1, z)         // top-front-left
    val tfr = isoProject(ox, oy, cellW, cellH, cellYH, x + 1, y + 1, z)     // top-front-right
    val tbl = isoProject(ox, oy, cellW, cellH, cellYH, x, y + 1, z + 1)     // top-back-left
    val tbr = isoProject(ox, oy, cellW, cellH, cellYH, x + 1, y + 1, z + 1) // top-back-right
    val bfl = isoProject(ox, oy, cellW, cellH, cellYH, x, y, z)             // bottom-front-left
    val bfr = isoProject(ox, oy, cellW, cellH, cellYH, x + 1, y, z)         // bottom-front-right
    val bbl = isoProject(ox, oy, cellW, cellH, cellYH, x, y, z + 1)         // bottom-back-left

    val a = alpha.coerceIn(0f, 1f)

    // Top face (brightest)
    val topPath = Path().apply {
        moveTo(tfl.x, tfl.y); lineTo(tfr.x, tfr.y)
        lineTo(tbr.x, tbr.y); lineTo(tbl.x, tbl.y); close()
    }
    drawPath(topPath, color.copy(alpha = a), style = Fill)

    // Left face (medium)
    val leftPath = Path().apply {
        moveTo(tfl.x, tfl.y); lineTo(tbl.x, tbl.y)
        lineTo(bbl.x, bbl.y); lineTo(bfl.x, bfl.y); close()
    }
    drawPath(leftPath, darken(color, 0.7f).copy(alpha = a), style = Fill)

    // Right face (darkest)
    val rightPath = Path().apply {
        moveTo(tfl.x, tfl.y); lineTo(tfr.x, tfr.y)
        lineTo(bfr.x, bfr.y); lineTo(bfl.x, bfl.y); close()
    }
    drawPath(rightPath, darken(color, 0.5f).copy(alpha = a), style = Fill)

    // Edge highlight on top face (subtle)
    if (a > 0.5f) {
        val hlPath = Path().apply {
            moveTo(tfl.x, tfl.y); lineTo(tfr.x, tfr.y)
        }
        drawPath(hlPath, Color.White.copy(alpha = 0.15f * a))
    }
}

/** Draw the floor grid at y=0 */
private fun DrawScope.drawFloorGrid(
    ox: Float, oy: Float, cellW: Float, cellH: Float, cellYH: Float,
    boardW: Int, boardD: Int, boardH: Int
) {
    val gridColor = Color.White.copy(alpha = 0.06f)
    // Floor lines along X
    for (z in 0..boardD) {
        val start = isoProject(ox, oy, cellW, cellH, cellYH, 0f, 0f, z.toFloat())
        val end = isoProject(ox, oy, cellW, cellH, cellYH, boardW.toFloat(), 0f, z.toFloat())
        drawLine(gridColor, start, end, strokeWidth = 1f)
    }
    // Floor lines along Z
    for (x in 0..boardW) {
        val start = isoProject(ox, oy, cellW, cellH, cellYH, x.toFloat(), 0f, 0f)
        val end = isoProject(ox, oy, cellW, cellH, cellYH, x.toFloat(), 0f, boardD.toFloat())
        drawLine(gridColor, start, end, strokeWidth = 1f)
    }
}

/** Draw back walls for depth reference */
private fun DrawScope.drawWalls(
    ox: Float, oy: Float, cellW: Float, cellH: Float, cellYH: Float,
    boardW: Int, boardD: Int, boardH: Int
) {
    val wallColor = Color.White.copy(alpha = 0.03f)
    val wallLine = Color.White.copy(alpha = 0.05f)

    // Back-left wall (x=0 plane) — from z=0 to z=boardD, y=0 to boardH
    for (z in 0 until boardD) {
        for (y in 0 until boardH) {
            val bl = isoProject(ox, oy, cellW, cellH, cellYH, 0f, y.toFloat(), z.toFloat())
            val br = isoProject(ox, oy, cellW, cellH, cellYH, 0f, y.toFloat(), (z + 1).toFloat())
            val tr = isoProject(ox, oy, cellW, cellH, cellYH, 0f, (y + 1).toFloat(), (z + 1).toFloat())
            val tl = isoProject(ox, oy, cellW, cellH, cellYH, 0f, (y + 1).toFloat(), z.toFloat())
            val path = Path().apply {
                moveTo(bl.x, bl.y); lineTo(br.x, br.y); lineTo(tr.x, tr.y); lineTo(tl.x, tl.y); close()
            }
            drawPath(path, wallColor)
        }
    }
    // Horizontal lines on back-left wall
    for (y in 0..boardH) {
        val s = isoProject(ox, oy, cellW, cellH, cellYH, 0f, y.toFloat(), 0f)
        val e = isoProject(ox, oy, cellW, cellH, cellYH, 0f, y.toFloat(), boardD.toFloat())
        drawLine(wallLine, s, e, strokeWidth = 0.5f)
    }

    // Back-right wall (z=boardD plane) — from x=0 to x=boardW
    for (x in 0 until boardW) {
        for (y in 0 until boardH) {
            val bl = isoProject(ox, oy, cellW, cellH, cellYH, x.toFloat(), y.toFloat(), boardD.toFloat())
            val br = isoProject(ox, oy, cellW, cellH, cellYH, (x + 1).toFloat(), y.toFloat(), boardD.toFloat())
            val tr = isoProject(ox, oy, cellW, cellH, cellYH, (x + 1).toFloat(), (y + 1).toFloat(), boardD.toFloat())
            val tl = isoProject(ox, oy, cellW, cellH, cellYH, x.toFloat(), (y + 1).toFloat(), boardD.toFloat())
            val path = Path().apply {
                moveTo(bl.x, bl.y); lineTo(br.x, br.y); lineTo(tr.x, tr.y); lineTo(tl.x, tl.y); close()
            }
            drawPath(path, wallColor)
        }
    }
    // Horizontal lines on back-right wall
    for (y in 0..boardH) {
        val s = isoProject(ox, oy, cellW, cellH, cellYH, 0f, y.toFloat(), boardD.toFloat())
        val e = isoProject(ox, oy, cellW, cellH, cellYH, boardW.toFloat(), y.toFloat(), boardD.toFloat())
        drawLine(wallLine, s, e, strokeWidth = 0.5f)
    }
}

/** Get color for a piece color index */
private fun pieceColor(idx: Int): Color = when (idx) {
    1 -> Color(0xFF00E5FF)  // I - cyan
    2 -> Color(0xFFFFD600)  // O - yellow
    3 -> Color(0xFFAA00FF)  // T - purple
    4 -> Color(0xFF00E676)  // S - green
    5 -> Color(0xFFFF6D00)  // L - orange
    6 -> Color(0xFFFF1744)  // Tower - red
    7 -> Color(0xFF2979FF)  // Corner - blue
    8 -> Color(0xFFFF4081)  // Step - pink
    else -> Color(0xFF888888)
}

private fun darken(color: Color, factor: Float): Color = Color(
    red = color.red * factor,
    green = color.green * factor,
    blue = color.blue * factor,
    alpha = color.alpha
)
