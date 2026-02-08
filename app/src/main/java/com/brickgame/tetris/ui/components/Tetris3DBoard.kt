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
 * Auto-sizes to fill available space. Renders back-to-front.
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
        val bw = Tetris3DGame.BOARD_W
        val bd = Tetris3DGame.BOARD_D
        val bh = Tetris3DGame.BOARD_H

        // Compute cell size to maximize board in available space.
        // Isometric width needed: (bw + bd) * cellW/2 (diamond width)
        // Isometric height needed: (bw + bd) * cellH/2 + bh * cellYH (diamond depth + height)
        // cellH = cellW * 0.5, cellYH = cellW * 0.65

        val isoRatioH = 0.5f   // cellH / cellW
        val isoRatioY = 0.65f  // cellYH / cellW

        // Horizontal constraint: (bw + bd) * cellW * 0.5 <= w * 0.92
        val cellFromW = (w * 0.92f) / ((bw + bd) * 0.5f)
        // Vertical constraint: (bw + bd) * cellH * 0.5 + bh * cellYH <= h * 0.95
        val cellFromH = (h * 0.95f) / ((bw + bd) * isoRatioH * 0.5f + bh * isoRatioY)

        val cellW = minOf(cellFromW, cellFromH)
        val cellH = cellW * isoRatioH
        val cellYH = cellW * isoRatioY

        // Total board dimensions in screen space
        val totalW = (bw + bd) * cellW * 0.5f
        val totalH = (bw + bd) * cellH * 0.5f + bh * cellYH

        // Center the board: origin is the top-center of the isometric diamond
        val ox = w / 2f
        // Position so the board is vertically centered
        val topOfBoard = (h - totalH) / 2f
        val oy = topOfBoard + bh * cellYH  // origin is at the floor level, board grows upward

        // Draw back walls first (behind everything)
        drawWalls(ox, oy, cellW, cellH, cellYH, bw, bd, bh)

        // Draw floor grid
        drawFloorGrid(ox, oy, cellW, cellH, cellYH, bw, bd)

        // Draw placed blocks (back to front: high z first, high x first for correct occlusion)
        if (state.board.isNotEmpty()) {
            for (y in 0 until bh) {
                for (z in bd - 1 downTo 0) {
                    for (x in bw - 1 downTo 0) {
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
    val a = alpha.coerceIn(0f, 1f)

    val tfl = isoProject(ox, oy, cellW, cellH, cellYH, x, y + 1, z)
    val tfr = isoProject(ox, oy, cellW, cellH, cellYH, x + 1, y + 1, z)
    val tbl = isoProject(ox, oy, cellW, cellH, cellYH, x, y + 1, z + 1)
    val tbr = isoProject(ox, oy, cellW, cellH, cellYH, x + 1, y + 1, z + 1)
    val bfl = isoProject(ox, oy, cellW, cellH, cellYH, x, y, z)
    val bfr = isoProject(ox, oy, cellW, cellH, cellYH, x + 1, y, z)
    val bbl = isoProject(ox, oy, cellW, cellH, cellYH, x, y, z + 1)

    // Top face (brightest)
    val topPath = Path().apply {
        moveTo(tfl.x, tfl.y); lineTo(tfr.x, tfr.y)
        lineTo(tbr.x, tbr.y); lineTo(tbl.x, tbl.y); close()
    }
    drawPath(topPath, color.copy(alpha = a), style = Fill)

    // Left face (medium shade)
    val leftPath = Path().apply {
        moveTo(tfl.x, tfl.y); lineTo(tbl.x, tbl.y)
        lineTo(bbl.x, bbl.y); lineTo(bfl.x, bfl.y); close()
    }
    drawPath(leftPath, darken(color, 0.65f).copy(alpha = a), style = Fill)

    // Right face (darkest shade)
    val rightPath = Path().apply {
        moveTo(tfl.x, tfl.y); lineTo(tfr.x, tfr.y)
        lineTo(bfr.x, bfr.y); lineTo(bfl.x, bfl.y); close()
    }
    drawPath(rightPath, darken(color, 0.4f).copy(alpha = a), style = Fill)

    // Top edge highlights
    if (a > 0.5f) {
        drawLine(Color.White.copy(alpha = 0.2f * a), tfl, tfr, strokeWidth = 1.2f)
        drawLine(Color.White.copy(alpha = 0.1f * a), tfl, tbl, strokeWidth = 0.8f)
    }
}

/** Draw the floor grid at y=0 */
private fun DrawScope.drawFloorGrid(
    ox: Float, oy: Float, cellW: Float, cellH: Float, cellYH: Float,
    boardW: Int, boardD: Int
) {
    val gridColor = Color.White.copy(alpha = 0.08f)
    for (z in 0..boardD) {
        val start = isoProject(ox, oy, cellW, cellH, cellYH, 0f, 0f, z.toFloat())
        val end = isoProject(ox, oy, cellW, cellH, cellYH, boardW.toFloat(), 0f, z.toFloat())
        drawLine(gridColor, start, end, strokeWidth = 1f)
    }
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
    val wallColor = Color.White.copy(alpha = 0.025f)
    val wallLine = Color.White.copy(alpha = 0.04f)

    // Back-left wall (x=0 plane)
    val bl0 = isoProject(ox, oy, cellW, cellH, cellYH, 0f, 0f, 0f)
    val bl1 = isoProject(ox, oy, cellW, cellH, cellYH, 0f, 0f, boardD.toFloat())
    val bl2 = isoProject(ox, oy, cellW, cellH, cellYH, 0f, boardH.toFloat(), boardD.toFloat())
    val bl3 = isoProject(ox, oy, cellW, cellH, cellYH, 0f, boardH.toFloat(), 0f)
    val wallLeftPath = Path().apply {
        moveTo(bl0.x, bl0.y); lineTo(bl1.x, bl1.y); lineTo(bl2.x, bl2.y); lineTo(bl3.x, bl3.y); close()
    }
    drawPath(wallLeftPath, wallColor)

    // Grid lines on left wall
    for (y in 0..boardH step 2) {
        val s = isoProject(ox, oy, cellW, cellH, cellYH, 0f, y.toFloat(), 0f)
        val e = isoProject(ox, oy, cellW, cellH, cellYH, 0f, y.toFloat(), boardD.toFloat())
        drawLine(wallLine, s, e, strokeWidth = 0.5f)
    }
    for (z in 0..boardD step 2) {
        val s = isoProject(ox, oy, cellW, cellH, cellYH, 0f, 0f, z.toFloat())
        val e = isoProject(ox, oy, cellW, cellH, cellYH, 0f, boardH.toFloat(), z.toFloat())
        drawLine(wallLine, s, e, strokeWidth = 0.5f)
    }

    // Back-right wall (z=boardD plane)
    val br0 = isoProject(ox, oy, cellW, cellH, cellYH, 0f, 0f, boardD.toFloat())
    val br1 = isoProject(ox, oy, cellW, cellH, cellYH, boardW.toFloat(), 0f, boardD.toFloat())
    val br2 = isoProject(ox, oy, cellW, cellH, cellYH, boardW.toFloat(), boardH.toFloat(), boardD.toFloat())
    val br3 = isoProject(ox, oy, cellW, cellH, cellYH, 0f, boardH.toFloat(), boardD.toFloat())
    val wallRightPath = Path().apply {
        moveTo(br0.x, br0.y); lineTo(br1.x, br1.y); lineTo(br2.x, br2.y); lineTo(br3.x, br3.y); close()
    }
    drawPath(wallRightPath, wallColor)

    // Grid lines on right wall
    for (y in 0..boardH step 2) {
        val s = isoProject(ox, oy, cellW, cellH, cellYH, 0f, y.toFloat(), boardD.toFloat())
        val e = isoProject(ox, oy, cellW, cellH, cellYH, boardW.toFloat(), y.toFloat(), boardD.toFloat())
        drawLine(wallLine, s, e, strokeWidth = 0.5f)
    }
    for (x in 0..boardW step 2) {
        val s = isoProject(ox, oy, cellW, cellH, cellYH, x.toFloat(), 0f, boardD.toFloat())
        val e = isoProject(ox, oy, cellW, cellH, cellYH, x.toFloat(), boardH.toFloat(), boardD.toFloat())
        drawLine(wallLine, s, e, strokeWidth = 0.5f)
    }
}

private fun pieceColor(idx: Int): Color = when (idx) {
    1 -> Color(0xFF00E5FF)
    2 -> Color(0xFFFFD600)
    3 -> Color(0xFFAA00FF)
    4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00)
    6 -> Color(0xFFFF1744)
    7 -> Color(0xFF2979FF)
    8 -> Color(0xFFFF4081)
    else -> Color(0xFF888888)
}

private fun darken(color: Color, factor: Float): Color = Color(
    red = color.red * factor,
    green = color.green * factor,
    blue = color.blue * factor,
    alpha = color.alpha
)
