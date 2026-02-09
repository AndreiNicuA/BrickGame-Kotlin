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
 * 3D Tetris renderer.
 *
 * Axis convention (Fusion 360 style):
 *   X = East-West (horizontal)
 *   Y = North-South (horizontal depth)
 *   Z = Height-Elevation (vertical distance)
 *
 * The engine stores board[y][z][x] where y=height.
 * The renderer maps: engine(x,y,z) → display(X=x, Y=z, Z=y)
 */
@Composable
fun Tetris3DBoard(
    state: Game3DState,
    modifier: Modifier = Modifier,
    showGhost: Boolean = true,
    cameraAngleY: Float = 35f,   // Horizontal orbit (azimuth)
    cameraAngleX: Float = 25f,   // Vertical tilt (elevation)
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    themePixelOn: Color = Color(0xFF22C55E),
    themeBg: Color = Color(0xFF0A0A0A),
    starWarsMode: Boolean = false
) {
    if (starWarsMode) {
        StarWarsBoard(state, modifier, showGhost, themePixelOn)
    } else {
        FreeCameraBoard(state, modifier, showGhost, cameraAngleY, cameraAngleX, panOffsetX, panOffsetY, themePixelOn)
    }
}

// ==================== FREE CAMERA MODE ====================

@Composable
private fun FreeCameraBoard(
    state: Game3DState,
    modifier: Modifier,
    showGhost: Boolean,
    azimuth: Float,    // horizontal orbit angle
    elevation: Float,  // vertical tilt angle
    panX: Float,
    panY: Float,
    themeColor: Color
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // Board dimensions in display coords (X=right, Y=depth, Z=up)
        val bx = Tetris3DGame.BOARD_W.toFloat()  // engine W → display X
        val by = Tetris3DGame.BOARD_D.toFloat()   // engine D → display Y
        val bz = Tetris3DGame.BOARD_H.toFloat()   // engine H → display Z (up)

        // Camera rotation — full 360° on both axes
        val radAz = Math.toRadians(azimuth.toDouble())
        val radEl = Math.toRadians(elevation.toDouble())
        val cosAz = cos(radAz).toFloat(); val sinAz = sin(radAz).toFloat()
        val cosEl = cos(radEl).toFloat(); val sinEl = sin(radEl).toFloat()

        val screenScale = minOf(w, h) / 400f
        val fov = 700f * screenScale
        val camDist = 16f

        // Center of the board in display coords
        val cx = bx / 2f; val cy = by / 2f; val cz = bz / 2f

        // Project display coords (X,Y,Z) → screen (sx, sy)
        // X=right, Y=depth, Z=up
        fun project(px: Float, py: Float, pz: Float): Offset? {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            // Rotate around Z axis (azimuth — horizontal orbit)
            val rx = dx * cosAz - dy * sinAz
            val ry = dx * sinAz + dy * cosAz
            // Rotate around X axis (elevation — vertical tilt)
            val rz = dz * cosEl - ry * sinEl
            val ry2 = dz * sinEl + ry * cosEl
            // Perspective
            val depth = ry2 + camDist
            if (depth < 0.5f) return null
            val scale = fov / depth
            return Offset(w / 2f + rx * scale + panX, h / 2f - rz * scale + panY)
        }

        // Depth for sorting
        fun depth(px: Float, py: Float, pz: Float): Float {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            val ry = dx * sinAz + dy * cosAz
            val ry2 = dz * sinEl + ry * cosEl
            return ry2 + camDist
        }

        // === Wireframe edges ===
        val edgeColor = themeColor.copy(alpha = 0.12f)
        val edgeColorDim = themeColor.copy(alpha = 0.06f)
        fun edge(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, c: Color = edgeColor) {
            val a = project(x1, y1, z1); val b = project(x2, y2, z2)
            if (a != null && b != null) drawLine(c, a, b, 0.8f)
        }
        // Vertical edges (Z direction)
        edge(0f,0f,0f, 0f,0f,bz); edge(bx,0f,0f, bx,0f,bz)
        edge(0f,by,0f, 0f,by,bz); edge(bx,by,0f, bx,by,bz)
        // Top edges (at Z=bz)
        edge(0f,0f,bz, bx,0f,bz); edge(0f,by,bz, bx,by,bz)
        edge(0f,0f,bz, 0f,by,bz); edge(bx,0f,bz, bx,by,bz)
        // Bottom edges (at Z=0)
        edge(0f,0f,0f, bx,0f,0f, edgeColorDim); edge(0f,by,0f, bx,by,0f, edgeColorDim)
        edge(0f,0f,0f, 0f,by,0f, edgeColorDim); edge(bx,0f,0f, bx,by,0f, edgeColorDim)

        // Floor grid (at Z=0)
        val gridColor = themeColor.copy(alpha = 0.05f)
        for (ix in 0..bx.toInt()) {
            val a = project(ix.toFloat(), 0f, 0f); val b = project(ix.toFloat(), by, 0f)
            if (a != null && b != null) drawLine(gridColor, a, b, 0.5f)
        }
        for (iy in 0..by.toInt()) {
            val a = project(0f, iy.toFloat(), 0f); val b = project(bx, iy.toFloat(), 0f)
            if (a != null && b != null) drawLine(gridColor, a, b, 0.5f)
        }

        // === Axis indicator (small, bottom-left corner) ===
        val axO = project(0f, 0f, 0f)
        val axX = project(1.5f, 0f, 0f)
        val axY = project(0f, 1.5f, 0f)
        val axZ = project(0f, 0f, 1.5f)
        if (axO != null) {
            if (axX != null) drawLine(Color(0xFFFF4444).copy(0.6f), axO, axX, 2f) // X = East-West (red)
            if (axY != null) drawLine(Color(0xFF44FF44).copy(0.6f), axO, axY, 2f) // Y = North-South (green)
            if (axZ != null) drawLine(Color(0xFF4488FF).copy(0.6f), axO, axZ, 2f) // Z = Height (blue)
        }

        // === Cube faces ===
        data class Face(val pts: List<Offset>, val color: Color, val d: Float)
        val faces = mutableListOf<Face>()

        // Add a cube at display coords (dx, dy, dz)
        fun addCube(dx: Int, dy: Int, dz: Int, color: Color, alpha: Float) {
            val x = dx.toFloat(); val y = dy.toFloat(); val z = dz.toFloat()
            val a = alpha.coerceIn(0f, 1f)

            // 8 corners of unit cube at (x,y,z)
            val p000 = project(x, y, z) ?: return
            val p100 = project(x+1, y, z) ?: return
            val p010 = project(x, y+1, z) ?: return
            val p110 = project(x+1, y+1, z) ?: return
            val p001 = project(x, y, z+1) ?: return
            val p101 = project(x+1, y, z+1) ?: return
            val p011 = project(x, y+1, z+1) ?: return
            val p111 = project(x+1, y+1, z+1) ?: return

            val topC = color.copy(alpha = a)                    // Z+ face (top)
            val botC = darken(color, 0.25f).copy(alpha = a)     // Z- face (bottom)
            val frontC = darken(color, 0.6f).copy(alpha = a)    // Y- face
            val backC = darken(color, 0.45f).copy(alpha = a)    // Y+ face
            val leftC = darken(color, 0.5f).copy(alpha = a)     // X- face
            val rightC = darken(color, 0.7f).copy(alpha = a)    // X+ face

            val mx = x + 0.5f; val my = y + 0.5f; val mz = z + 0.5f

            // Top (Z+): z+1 face
            faces.add(Face(listOf(p001, p101, p111, p011), topC, depth(mx, my, z + 1)))
            // Bottom (Z-): z face
            faces.add(Face(listOf(p000, p100, p110, p010), botC, depth(mx, my, z)))
            // Front (Y-): y face
            faces.add(Face(listOf(p000, p100, p101, p001), frontC, depth(mx, y, mz)))
            // Back (Y+): y+1 face
            faces.add(Face(listOf(p010, p110, p111, p011), backC, depth(mx, y + 1, mz)))
            // Left (X-): x face
            faces.add(Face(listOf(p000, p010, p011, p001), leftC, depth(x, my, mz)))
            // Right (X+): x+1 face
            faces.add(Face(listOf(p100, p110, p111, p101), rightC, depth(x + 1, my, mz)))
        }

        // Board blocks: engine board[y][z][x] → display(X=x, Y=z, Z=y)
        if (state.board.isNotEmpty()) {
            for (ey in 0 until Tetris3DGame.BOARD_H) {     // engine y (height) → display Z
                for (ez in 0 until Tetris3DGame.BOARD_D) {  // engine z (depth) → display Y
                    for (ex in 0 until Tetris3DGame.BOARD_W) { // engine x → display X
                        if (ey < state.board.size && ez < state.board[ey].size && ex < state.board[ey][ez].size) {
                            val c = state.board[ey][ez][ex]
                            if (c > 0) addCube(ex, ez, ey, pieceColor(c, themeColor), 1f)
                        }
                    }
                }
            }
        }

        // Ghost piece: engine(x,y,z) → display(x, z, y)
        val piece = state.currentPiece
        if (piece != null && showGhost && state.ghostY < piece.y) {
            for (b in piece.blocks) {
                val dx = piece.x + b.x
                val dy = piece.z + b.z     // engine z → display Y
                val dz = state.ghostY + b.y // engine y → display Z
                if (dz >= 0) addCube(dx, dy, dz, themeColor, 0.15f)
            }
        }

        // Current piece: engine(x,y,z) → display(x, z, y)
        if (piece != null) {
            for (b in piece.blocks) {
                val dx = piece.x + b.x
                val dy = piece.z + b.z     // engine z → display Y
                val dz = piece.y + b.y     // engine y → display Z
                if (dz >= 0) addCube(dx, dy, dz, pieceColor(piece.type.colorIndex, themeColor), 1f)
            }
        }

        // Sort back-to-front and draw
        faces.sortByDescending { it.d }
        faces.forEach { face ->
            val path = Path().apply {
                moveTo(face.pts[0].x, face.pts[0].y)
                for (i in 1 until face.pts.size) lineTo(face.pts[i].x, face.pts[i].y)
                close()
            }
            drawPath(path, face.color, style = Fill)
        }

        // Edge highlights on current piece
        if (piece != null) {
            for (b in piece.blocks) {
                val dx = piece.x + b.x
                val dy = piece.z + b.z
                val dz = piece.y + b.y
                if (dz < 0) continue
                // Top face edges (Z+ face)
                val t0 = project(dx.toFloat(), dy.toFloat(), dz + 1f)
                val t1 = project(dx + 1f, dy.toFloat(), dz + 1f)
                val t2 = project(dx + 1f, dy + 1f, dz + 1f)
                val t3 = project(dx.toFloat(), dy + 1f, dz + 1f)
                if (t0 != null && t1 != null && t2 != null && t3 != null) {
                    val ec = Color.White.copy(0.3f)
                    drawLine(ec, t0, t1, 1.5f); drawLine(ec, t1, t2, 1f)
                    drawLine(ec, t2, t3, 1f); drawLine(ec, t3, t0, 1f)
                }
            }
        }
    }
}

// ==================== STAR WARS MODE ====================

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

        val vpX = w / 2f
        val vpY = -h * 0.15f
        val bottomY = h * 0.97f
        val topY = h * 0.05f
        val bottomHalfW = w * 0.45f
        val topHalfW = w * 0.12f

        fun rowParams(row: Int): RowParams {
            val t = row.toFloat() / bh
            val screenY = bottomY + (topY - bottomY) * t
            val halfW = bottomHalfW + (topHalfW - bottomHalfW) * t
            val cellW = halfW * 2f / bw
            val cellH = (bottomY - topY) / bh * (1f - t * 0.35f)
            return RowParams(screenY, vpX - halfW, cellW, cellH, 1f - t * 0.5f)
        }

        // Grid
        for (row in 0..bh) {
            val t = row.toFloat() / bh
            val screenY = bottomY + (topY - bottomY) * t
            val halfW = bottomHalfW + (topHalfW - bottomHalfW) * t
            drawLine(themeColor.copy(alpha = 0.06f * (1f - t * 0.6f)),
                Offset(vpX - halfW, screenY), Offset(vpX + halfW, screenY), 0.5f)
        }
        for (col in 0..bw) {
            val bxp = vpX - bottomHalfW + col * (bottomHalfW * 2 / bw)
            val txp = vpX - topHalfW + col * (topHalfW * 2 / bw)
            drawLine(themeColor.copy(alpha = 0.04f), Offset(bxp, bottomY), Offset(txp, topY), 0.5f)
        }

        // Blocks top-to-bottom (far first)
        for (row in bh - 1 downTo 0) {
            val rp = rowParams(row)
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
                if (colorIdx > 0) drawSWBlock(rp, col, pieceColor(colorIdx, themeColor), rp.alpha)
            }
        }

        // Ghost
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
    val cw = rp.cellW; val ch = rp.cellH
    val a = alpha.coerceIn(0f, 1f)
    val depth = ch * 0.18f

    val topPath = Path().apply {
        moveTo(x + 1, y); lineTo(x + cw - 1, y)
        lineTo(x + cw - 1 - depth, y - depth); lineTo(x + 1 + depth, y - depth); close()
    }
    drawPath(topPath, darken(color, 0.8f).copy(alpha = a * 0.7f))
    val rightPath = Path().apply {
        moveTo(x + cw - 1, y); lineTo(x + cw - 1, y + ch - 1)
        lineTo(x + cw - 1 - depth, y + ch - 1 - depth); lineTo(x + cw - 1 - depth, y - depth); close()
    }
    drawPath(rightPath, darken(color, 0.5f).copy(alpha = a * 0.6f))
    drawRect(color.copy(alpha = a), Offset(x + 1, y), Size(cw - 2, ch - 1))
    drawRect(Color.White.copy(alpha = 0.15f * a), Offset(x + 2, y + 1), Size(cw * 0.3f, 2f))
    drawRect(Color.White.copy(alpha = 0.1f * a), Offset(x + 2, y + 1), Size(2f, ch * 0.3f))
}

// ==================== SHARED ====================

private fun pieceColor(idx: Int, themeColor: Color): Color = when (idx) {
    1 -> Color(0xFF00E5FF); 2 -> Color(0xFFFFD600); 3 -> Color(0xFFAA00FF); 4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00); 6 -> Color(0xFFFF1744); 7 -> Color(0xFF2979FF); 8 -> Color(0xFFFF4081)
    else -> themeColor
}

private fun darken(color: Color, factor: Float): Color = Color(
    red = color.red * factor, green = color.green * factor,
    blue = color.blue * factor, alpha = color.alpha
)
