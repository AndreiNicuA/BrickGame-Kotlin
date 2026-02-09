package com.brickgame.tetris.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.brickgame.tetris.game.*
import kotlin.math.*

/**
 * 3D Tetris board renderer.
 * Two modes:
 * - Free camera: true perspective projection, swipe to rotate
 * - Star Wars: fixed vanishing-point view, bottom close / top far, 2D-like layout with 3D depth
 */
@Composable
fun Tetris3DBoard(
    state: Game3DState,
    modifier: Modifier = Modifier,
    showGhost: Boolean = true,
    cameraAngleY: Float = 35f,
    cameraAngleX: Float = 25f,
    themePixelOn: Color = Color(0xFF22C55E),
    themeBg: Color = Color(0xFF0A0A0A),
    starWarsMode: Boolean = false
) {
    if (starWarsMode) {
        StarWarsBoard(state, modifier, showGhost, themePixelOn)
    } else {
        FreeCameraBoard(state, modifier, showGhost, cameraAngleY, cameraAngleX, themePixelOn)
    }
}

// ==================== STAR WARS MODE ====================
// Board rendered flat like 2D, but with perspective: bottom row is wide/close,
// top row narrows to a vanishing point. Each cell is a 3D extruded block.

@Composable
private fun StarWarsBoard(
    state: Game3DState,
    modifier: Modifier,
    showGhost: Boolean,
    themeColor: Color
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bw = Tetris3DGame.BOARD_W
        val bh = Tetris3DGame.BOARD_H

        // Vanishing point at top center
        val vpX = w / 2f
        val vpY = -h * 0.15f  // above screen top for dramatic perspective

        // Bottom row spans ~90% of width, top row spans ~25%
        val bottomY = h * 0.97f
        val topY = h * 0.05f
        val bottomHalfW = w * 0.45f
        val topHalfW = w * 0.12f

        // For each row y (0=bottom of board, bh-1=top of board):
        // t = y / bh → 0 at bottom, 1 at top
        fun rowParams(row: Int): RowParams {
            val t = row.toFloat() / bh
            val screenY = bottomY + (topY - bottomY) * t
            val halfW = bottomHalfW + (topHalfW - bottomHalfW) * t
            val cellW = halfW * 2f / bw
            val cellH = (bottomY - topY) / bh * (1f - t * 0.35f) // cells get shorter toward top
            return RowParams(screenY, vpX - halfW, cellW, cellH, 1f - t * 0.5f) // alpha fades toward top
        }

        // Grid lines for depth
        for (row in 0..bh) {
            val t = row.toFloat() / bh
            val screenY = bottomY + (topY - bottomY) * t
            val halfW = bottomHalfW + (topHalfW - bottomHalfW) * t
            drawLine(themeColor.copy(alpha = 0.06f * (1f - t * 0.6f)),
                Offset(vpX - halfW, screenY), Offset(vpX + halfW, screenY), 0.5f)
        }
        // Vertical grid converging to vanishing point
        for (col in 0..bw) {
            val bx = vpX - bottomHalfW + col * (bottomHalfW * 2 / bw)
            val tx = vpX - topHalfW + col * (topHalfW * 2 / bw)
            drawLine(themeColor.copy(alpha = 0.04f),
                Offset(bx, bottomY), Offset(tx, topY), 0.5f)
        }

        // Draw blocks bottom-to-top (painter's order: bottom = closest = drawn last)
        // First pass: board blocks from top to bottom
        for (row in bh - 1 downTo 0) {
            val rp = rowParams(row)
            // In star wars mode, collapse Z dimension: show only front slice (z=0..bd-1 merged)
            // Show the "tallest" block at each x,y position
            for (col in 0 until bw) {
                var colorIdx = 0
                if (state.board.isNotEmpty() && row < state.board.size) {
                    for (z in 0 until Tetris3DGame.BOARD_D) {
                        if (z < state.board[row].size && col < state.board[row][z].size) {
                            val c = state.board[row][z][col]
                            if (c > 0) { colorIdx = c; break }
                        }
                    }
                }
                if (colorIdx > 0) {
                    drawSWBlock(rp, col, pieceColor(colorIdx, themeColor), rp.alpha)
                }
            }
        }

        // Ghost piece
        val piece = state.currentPiece
        if (piece != null && showGhost && state.ghostY < piece.y) {
            for (b in piece.blocks) {
                val row = state.ghostY + b.y; val col = piece.x + b.x
                if (row in 0 until bh && col in 0 until bw) {
                    val rp = rowParams(row)
                    drawSWBlock(rp, col, themeColor, 0.15f * rp.alpha)
                }
            }
        }

        // Current piece
        if (piece != null) {
            for (b in piece.blocks) {
                val row = piece.y + b.y; val col = piece.x + b.x
                if (row in 0 until bh && col in 0 until bw) {
                    val rp = rowParams(row)
                    drawSWBlock(rp, col, pieceColor(piece.type.colorIndex, themeColor), rp.alpha)
                }
            }
        }
    }
}

private data class RowParams(val y: Float, val leftX: Float, val cellW: Float, val cellH: Float, val alpha: Float)

private fun DrawScope.drawSWBlock(rp: RowParams, col: Int, color: Color, alpha: Float) {
    val x = rp.leftX + col * rp.cellW
    val y = rp.y - rp.cellH
    val cw = rp.cellW
    val ch = rp.cellH
    val a = alpha.coerceIn(0f, 1f)
    val depth = ch * 0.18f  // 3D extrusion depth

    // Top face (main color)
    val topPath = Path().apply {
        moveTo(x + 1, y)
        lineTo(x + cw - 1, y)
        lineTo(x + cw - 1 - depth, y - depth)
        lineTo(x + 1 + depth, y - depth)
        close()
    }
    drawPath(topPath, darken(color, 0.8f).copy(alpha = a * 0.7f))

    // Right face (dark side)
    val rightPath = Path().apply {
        moveTo(x + cw - 1, y)
        lineTo(x + cw - 1, y + ch - 1)
        lineTo(x + cw - 1 - depth, y + ch - 1 - depth)
        lineTo(x + cw - 1 - depth, y - depth)
        close()
    }
    drawPath(rightPath, darken(color, 0.5f).copy(alpha = a * 0.6f))

    // Front face (main, brightest)
    drawRect(color.copy(alpha = a), Offset(x + 1, y), Size(cw - 2, ch - 1))

    // Inner highlight (top-left corner shine)
    drawRect(Color.White.copy(alpha = 0.15f * a), Offset(x + 2, y + 1), Size(cw * 0.3f, 2f))
    drawRect(Color.White.copy(alpha = 0.1f * a), Offset(x + 2, y + 1), Size(2f, ch * 0.3f))
}


// ==================== FREE CAMERA MODE ====================

@Composable
private fun FreeCameraBoard(
    state: Game3DState,
    modifier: Modifier,
    showGhost: Boolean,
    cameraAngleY: Float,
    cameraAngleX: Float,
    themeColor: Color
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bw = Tetris3DGame.BOARD_W.toFloat()
        val bd = Tetris3DGame.BOARD_D.toFloat()
        val bh = Tetris3DGame.BOARD_H.toFloat()

        val radY = Math.toRadians(cameraAngleY.toDouble())
        val radX = Math.toRadians(cameraAngleX.toDouble())
        val cosY = cos(radY).toFloat(); val sinY = sin(radY).toFloat()
        val cosX = cos(radX).toFloat(); val sinX = sin(radX).toFloat()

        val screenScale = minOf(w, h) / 400f
        val fov = 700f * screenScale
        val camDist = 16f
        val cx = bw / 2f; val cz = bd / 2f; val cy = bh / 2f

        fun project(px: Float, py: Float, pz: Float): Offset? {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            val rx = dx * cosY + dz * sinY
            val rz = -dx * sinY + dz * cosY
            val ry = dy * cosX - rz * sinX
            val rz2 = dy * sinX + rz * cosX
            val z = rz2 + camDist
            if (z < 0.5f) return null
            val scale = fov / z
            return Offset(w / 2f + rx * scale, h / 2f - ry * scale)
        }

        fun depth(px: Float, py: Float, pz: Float): Float {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            val rx = dx * cosY + dz * sinY
            val rz = -dx * sinY + dz * cosY
            val rz2 = dy * sinX + rz * cosX
            return rz2 + camDist
        }

        // Draw wireframe edges
        val edgeColor = themeColor.copy(alpha = 0.1f)
        fun drawEdge(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) {
            val a = project(x1, y1, z1); val b = project(x2, y2, z2)
            if (a != null && b != null) drawLine(edgeColor, a, b, 0.8f)
        }
        // Vertical edges
        drawEdge(0f,0f,0f, 0f,bh,0f); drawEdge(bw,0f,0f, bw,bh,0f)
        drawEdge(0f,0f,bd, 0f,bh,bd); drawEdge(bw,0f,bd, bw,bh,bd)
        // Top edges
        drawEdge(0f,bh,0f, bw,bh,0f); drawEdge(0f,bh,bd, bw,bh,bd)
        drawEdge(0f,bh,0f, 0f,bh,bd); drawEdge(bw,bh,0f, bw,bh,bd)
        // Bottom edges
        drawEdge(0f,0f,0f, bw,0f,0f); drawEdge(0f,0f,bd, bw,0f,bd)
        drawEdge(0f,0f,0f, 0f,0f,bd); drawEdge(bw,0f,0f, bw,0f,bd)

        // Floor grid
        val gridColor = themeColor.copy(alpha = 0.06f)
        for (x in 0..bw.toInt()) {
            val a = project(x.toFloat(), 0f, 0f); val b = project(x.toFloat(), 0f, bd)
            if (a != null && b != null) drawLine(gridColor, a, b, 0.5f)
        }
        for (z in 0..bd.toInt()) {
            val a = project(0f, 0f, z.toFloat()); val b = project(bw, 0f, z.toFloat())
            if (a != null && b != null) drawLine(gridColor, a, b, 0.5f)
        }

        // Collect all cube faces for depth sorting
        data class Face(val points: List<Offset>, val color: Color, val depth: Float)
        val faces = mutableListOf<Face>()

        fun addCube(gx: Int, gy: Int, gz: Int, color: Color, alpha: Float) {
            val x = gx.toFloat(); val y = gy.toFloat(); val z = gz.toFloat()
            val a = alpha.coerceIn(0f, 1f)

            val p000 = project(x, y, z) ?: return
            val p100 = project(x+1, y, z) ?: return
            val p010 = project(x, y+1, z) ?: return
            val p110 = project(x+1, y+1, z) ?: return
            val p001 = project(x, y, z+1) ?: return
            val p101 = project(x+1, y, z+1) ?: return
            val p011 = project(x, y+1, z+1) ?: return
            val p111 = project(x+1, y+1, z+1) ?: return

            val topC = color.copy(alpha = a)
            val s1 = darken(color, 0.6f).copy(alpha = a)
            val s2 = darken(color, 0.4f).copy(alpha = a)
            val botC = darken(color, 0.3f).copy(alpha = a)
            val mid = (x + 0.5f) to (z + 0.5f)

            faces.add(Face(listOf(p010, p110, p111, p011), topC, depth(mid.first, y+1, mid.second)))
            faces.add(Face(listOf(p000, p100, p101, p001), botC, depth(mid.first, y, mid.second)))
            faces.add(Face(listOf(p000, p100, p110, p010), s1, depth(mid.first, y+0.5f, z)))
            faces.add(Face(listOf(p001, p101, p111, p011), s2, depth(mid.first, y+0.5f, z+1)))
            faces.add(Face(listOf(p000, p001, p011, p010), s2, depth(x, y+0.5f, mid.second)))
            faces.add(Face(listOf(p100, p101, p111, p110), s1, depth(x+1, y+0.5f, mid.second)))
        }

        // Board blocks
        if (state.board.isNotEmpty()) {
            for (y in 0 until Tetris3DGame.BOARD_H) {
                for (z in 0 until Tetris3DGame.BOARD_D) {
                    for (x in 0 until Tetris3DGame.BOARD_W) {
                        if (y < state.board.size && z < state.board[y].size && x < state.board[y][z].size) {
                            val c = state.board[y][z][x]
                            if (c > 0) addCube(x, y, z, pieceColor(c, themeColor), 1f)
                        }
                    }
                }
            }
        }

        // Ghost piece
        val piece = state.currentPiece
        if (piece != null && showGhost && state.ghostY < piece.y) {
            for (b in piece.blocks) {
                val gx = piece.x + b.x; val gy = state.ghostY + b.y; val gz = piece.z + b.z
                if (gy >= 0) addCube(gx, gy, gz, themeColor, 0.15f)
            }
        }

        // Current piece
        if (piece != null) {
            for (b in piece.blocks) {
                val bx = piece.x + b.x; val by = piece.y + b.y; val bz = piece.z + b.z
                if (by >= 0) addCube(bx, by, bz, pieceColor(piece.type.colorIndex, themeColor), 1f)
            }
        }

        // Sort back-to-front, draw
        faces.sortByDescending { it.depth }
        faces.forEach { face ->
            val path = Path().apply {
                moveTo(face.points[0].x, face.points[0].y)
                for (i in 1 until face.points.size) lineTo(face.points[i].x, face.points[i].y)
                close()
            }
            drawPath(path, face.color, style = Fill)
        }

        // Current piece edge highlights
        if (piece != null) {
            for (b in piece.blocks) {
                val bx = piece.x + b.x; val by = piece.y + b.y; val bz = piece.z + b.z
                if (by < 0) continue
                val t0 = project(bx.toFloat(), by+1f, bz.toFloat())
                val t1 = project(bx+1f, by+1f, bz.toFloat())
                val t2 = project(bx+1f, by+1f, bz+1f)
                val t3 = project(bx.toFloat(), by+1f, bz+1f)
                if (t0 != null && t1 != null && t2 != null && t3 != null) {
                    val ec = Color.White.copy(0.25f)
                    drawLine(ec, t0, t1, 1.2f); drawLine(ec, t1, t2, 0.8f)
                    drawLine(ec, t2, t3, 0.8f); drawLine(ec, t3, t0, 0.8f)
                }
            }
        }
    }
}

// ==================== SHARED UTILS ====================

private fun pieceColor(idx: Int, themeColor: Color): Color = when (idx) {
    1 -> Color(0xFF00E5FF); 2 -> Color(0xFFFFD600); 3 -> Color(0xFFAA00FF); 4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00); 6 -> Color(0xFFFF1744); 7 -> Color(0xFF2979FF); 8 -> Color(0xFFFF4081)
    else -> themeColor
}

private fun darken(color: Color, factor: Float): Color = Color(
    red = color.red * factor, green = color.green * factor,
    blue = color.blue * factor, alpha = color.alpha
)
