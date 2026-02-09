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
import androidx.compose.ui.graphics.drawscope.Stroke
import com.brickgame.tetris.game.*
import kotlin.math.*

/**
 * 3D Tetris renderer — polished cubes with bevels and highlights.
 *
 * Axis convention (Fusion 360 / CAD standard):
 *   X = East-West (horizontal)
 *   Y = North-South (horizontal depth)
 *   Z = Height-Elevation (vertical distance)
 *
 * Engine board[engineY][engineZ][engineX] maps to display(X=engineX, Y=engineZ, Z=engineY)
 */
@Composable
fun Tetris3DBoard(
    state: Game3DState,
    modifier: Modifier = Modifier,
    showGhost: Boolean = true,
    cameraAngleY: Float = 35f,
    cameraAngleX: Float = 25f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    zoom: Float = 1f,
    themePixelOn: Color = Color(0xFF22C55E),
    themeBg: Color = Color(0xFF0A0A0A),
    starWarsMode: Boolean = false
) {
    if (starWarsMode) {
        StarWarsBoard(state, modifier, showGhost, themePixelOn)
    } else {
        FreeCameraBoard(state, modifier, showGhost, cameraAngleY, cameraAngleX, panOffsetX, panOffsetY, zoom, themePixelOn)
    }
}

// ==================== FREE CAMERA ====================

@Composable
private fun FreeCameraBoard(
    state: Game3DState,
    modifier: Modifier,
    showGhost: Boolean,
    azimuth: Float,
    elevation: Float,
    panX: Float,
    panY: Float,
    zoom: Float,
    themeColor: Color
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        val bx = Tetris3DGame.BOARD_W.toFloat()
        val by = Tetris3DGame.BOARD_D.toFloat()
        val bz = Tetris3DGame.BOARD_H.toFloat()

        val radAz = Math.toRadians(azimuth.toDouble())
        val radEl = Math.toRadians(elevation.toDouble())
        val cosAz = cos(radAz).toFloat(); val sinAz = sin(radAz).toFloat()
        val cosEl = cos(radEl).toFloat(); val sinEl = sin(radEl).toFloat()

        val screenScale = minOf(w, h) / 400f
        val fov = 700f * screenScale * zoom
        val camDist = 16f
        val cx = bx / 2f; val cy = by / 2f; val cz = bz / 2f

        fun project(px: Float, py: Float, pz: Float): Offset? {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            val rx = dx * cosAz - dy * sinAz
            val ry = dx * sinAz + dy * cosAz
            val rz = dz * cosEl - ry * sinEl
            val ry2 = dz * sinEl + ry * cosEl
            val depth = ry2 + camDist
            if (depth < 0.5f) return null
            val scale = fov / depth
            return Offset(w / 2f + rx * scale + panX, h / 2f - rz * scale + panY)
        }

        fun depth(px: Float, py: Float, pz: Float): Float {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            val ry = dx * sinAz + dy * cosAz
            return dz * sinEl + ry * cosEl + camDist
        }

        // === WIREFRAME BOX ===
        val wireColor = themeColor.copy(alpha = 0.18f)
        val wireDim = themeColor.copy(alpha = 0.08f)
        fun edge(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, c: Color = wireColor, sw: Float = 1.2f) {
            val a = project(x1, y1, z1); val b = project(x2, y2, z2)
            if (a != null && b != null) drawLine(c, a, b, sw)
        }
        // Vertical edges (Z)
        edge(0f,0f,0f, 0f,0f,bz); edge(bx,0f,0f, bx,0f,bz)
        edge(0f,by,0f, 0f,by,bz); edge(bx,by,0f, bx,by,bz)
        // Top ring (Z=bz)
        edge(0f,0f,bz, bx,0f,bz); edge(0f,by,bz, bx,by,bz)
        edge(0f,0f,bz, 0f,by,bz); edge(bx,0f,bz, bx,by,bz)
        // Bottom ring (Z=0)
        edge(0f,0f,0f, bx,0f,0f, wireDim); edge(0f,by,0f, bx,by,0f, wireDim)
        edge(0f,0f,0f, 0f,by,0f, wireDim); edge(bx,0f,0f, bx,by,0f, wireDim)

        // === FLOOR GRID (Z=0) ===
        val gridColor = themeColor.copy(alpha = 0.06f)
        for (ix in 0..bx.toInt()) {
            val a = project(ix.toFloat(), 0f, 0f); val b = project(ix.toFloat(), by, 0f)
            if (a != null && b != null) drawLine(gridColor, a, b, 0.5f)
        }
        for (iy in 0..by.toInt()) {
            val a = project(0f, iy.toFloat(), 0f); val b = project(bx, iy.toFloat(), 0f)
            if (a != null && b != null) drawLine(gridColor, a, b, 0.5f)
        }

        // === SEMI-TRANSPARENT WALLS ===
        val wallColor = themeColor.copy(alpha = 0.02f)
        fun wallQuad(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
                     x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float) {
            val p1 = project(x1,y1,z1); val p2 = project(x2,y2,z2)
            val p3 = project(x3,y3,z3); val p4 = project(x4,y4,z4)
            if (p1 != null && p2 != null && p3 != null && p4 != null) {
                val path = Path().apply { moveTo(p1.x,p1.y); lineTo(p2.x,p2.y); lineTo(p3.x,p3.y); lineTo(p4.x,p4.y); close() }
                drawPath(path, wallColor)
            }
        }
        // Floor
        wallQuad(0f,0f,0f, bx,0f,0f, bx,by,0f, 0f,by,0f)
        // Back walls
        wallQuad(0f,by,0f, bx,by,0f, bx,by,bz, 0f,by,bz)
        wallQuad(0f,0f,0f, 0f,by,0f, 0f,by,bz, 0f,0f,bz)

        // === AXIS INDICATOR ===
        val axO = project(0f, 0f, 0f)
        val axX = project(1.5f, 0f, 0f); val axY = project(0f, 1.5f, 0f); val axZ = project(0f, 0f, 1.5f)
        if (axO != null) {
            if (axX != null) drawLine(Color(0xFFFF4444).copy(0.5f), axO, axX, 2.5f) // X red
            if (axY != null) drawLine(Color(0xFF44FF44).copy(0.5f), axO, axY, 2.5f) // Y green
            if (axZ != null) drawLine(Color(0xFF4488FF).copy(0.5f), axO, axZ, 2.5f) // Z blue
        }

        // === CUBE FACES (polished) ===
        data class Face(val pts: List<Offset>, val fill: Color, val edge: Color?, val d: Float)
        val faces = mutableListOf<Face>()

        fun addCube(dx: Int, dy: Int, dz: Int, color: Color, alpha: Float, highlight: Boolean = false) {
            val x = dx.toFloat(); val y = dy.toFloat(); val z = dz.toFloat()
            val a = alpha.coerceIn(0f, 1f)

            val p000 = project(x,y,z) ?: return; val p100 = project(x+1,y,z) ?: return
            val p010 = project(x,y+1,z) ?: return; val p110 = project(x+1,y+1,z) ?: return
            val p001 = project(x,y,z+1) ?: return; val p101 = project(x+1,y,z+1) ?: return
            val p011 = project(x,y+1,z+1) ?: return; val p111 = project(x+1,y+1,z+1) ?: return

            // Polished shading — lighter top, graduated sides, dark bottom
            val topC = brighten(color, 1.15f).copy(alpha = a)
            val botC = darken(color, 0.2f).copy(alpha = a)
            val frontC = darken(color, 0.65f).copy(alpha = a)
            val backC = darken(color, 0.45f).copy(alpha = a)
            val leftC = darken(color, 0.5f).copy(alpha = a)
            val rightC = darken(color, 0.75f).copy(alpha = a)

            // Edge color for bevel effect
            val edgeC = if (highlight) Color.White.copy(alpha = 0.35f * a) else Color.White.copy(alpha = 0.08f * a)

            val mx = x + 0.5f; val my = y + 0.5f; val mz = z + 0.5f
            faces.add(Face(listOf(p001,p101,p111,p011), topC, edgeC, depth(mx,my,z+1)))
            faces.add(Face(listOf(p000,p100,p110,p010), botC, null, depth(mx,my,z)))
            faces.add(Face(listOf(p000,p100,p101,p001), frontC, edgeC, depth(mx,y,mz)))
            faces.add(Face(listOf(p010,p110,p111,p011), backC, null, depth(mx,y+1,mz)))
            faces.add(Face(listOf(p000,p010,p011,p001), leftC, edgeC, depth(x,my,mz)))
            faces.add(Face(listOf(p100,p110,p111,p101), rightC, edgeC, depth(x+1,my,mz)))
        }

        // Board blocks: engine board[ey][ez][ex] → display(ex, ez, ey)
        if (state.board.isNotEmpty()) {
            for (ey in 0 until Tetris3DGame.BOARD_H) {
                for (ez in 0 until Tetris3DGame.BOARD_D) {
                    for (ex in 0 until Tetris3DGame.BOARD_W) {
                        if (ey < state.board.size && ez < state.board[ey].size && ex < state.board[ey][ez].size) {
                            val c = state.board[ey][ez][ex]
                            if (c > 0) addCube(ex, ez, ey, pieceColor(c, themeColor), 1f)
                        }
                    }
                }
            }
        }

        // Ghost piece
        val piece = state.currentPiece
        if (piece != null && showGhost && state.ghostY < piece.y) {
            for (b in piece.blocks) {
                val dx = piece.x + b.x; val dy = piece.z + b.z; val dz = state.ghostY + b.y
                if (dz >= 0) addCube(dx, dy, dz, themeColor, 0.12f)
            }
        }

        // Current piece — highlighted
        if (piece != null) {
            for (b in piece.blocks) {
                val dx = piece.x + b.x; val dy = piece.z + b.z; val dz = piece.y + b.y
                if (dz >= 0) addCube(dx, dy, dz, pieceColor(piece.type.colorIndex, themeColor), 1f, highlight = true)
            }
        }

        // Sort back-to-front, draw
        faces.sortByDescending { it.d }
        faces.forEach { face ->
            val path = Path().apply {
                moveTo(face.pts[0].x, face.pts[0].y)
                for (i in 1 until face.pts.size) lineTo(face.pts[i].x, face.pts[i].y)
                close()
            }
            drawPath(path, face.fill, style = Fill)
            // Bevel edge
            if (face.edge != null) {
                drawPath(path, face.edge, style = Stroke(width = 1f))
            }
        }

        // Specular highlights on current piece top faces
        if (piece != null) {
            for (b in piece.blocks) {
                val dx = piece.x + b.x; val dy = piece.z + b.z; val dz = piece.y + b.y
                if (dz < 0) continue
                val t0 = project(dx.toFloat(), dy.toFloat(), dz + 1f)
                val t1 = project(dx + 0.35f, dy.toFloat(), dz + 1f)
                val t2 = project(dx + 0.35f, dy + 0.35f, dz + 1f)
                val t3 = project(dx.toFloat(), dy + 0.35f, dz + 1f)
                if (t0 != null && t1 != null && t2 != null && t3 != null) {
                    val shinePath = Path().apply {
                        moveTo(t0.x, t0.y); lineTo(t1.x, t1.y); lineTo(t2.x, t2.y); lineTo(t3.x, t3.y); close()
                    }
                    drawPath(shinePath, Color.White.copy(alpha = 0.2f), style = Fill)
                }
            }
        }
    }
}

// ==================== STAR WARS ====================

@Composable
private fun StarWarsBoard(state: Game3DState, modifier: Modifier, showGhost: Boolean, themeColor: Color) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val bw = Tetris3DGame.BOARD_W; val bh = Tetris3DGame.BOARD_H
        val vpX = w / 2f; val bottomY = h * 0.97f; val topY = h * 0.05f
        val bottomHalfW = w * 0.45f; val topHalfW = w * 0.12f

        fun rowParams(row: Int): RowParams {
            val t = row.toFloat() / bh
            val screenY = bottomY + (topY - bottomY) * t
            val halfW = bottomHalfW + (topHalfW - bottomHalfW) * t
            return RowParams(screenY, vpX - halfW, halfW * 2f / bw, (bottomY - topY) / bh * (1f - t * 0.35f), 1f - t * 0.5f)
        }

        for (row in 0..bh) {
            val t = row.toFloat() / bh
            val sy = bottomY + (topY - bottomY) * t
            val hw = bottomHalfW + (topHalfW - bottomHalfW) * t
            drawLine(themeColor.copy(0.06f * (1f - t * 0.6f)), Offset(vpX - hw, sy), Offset(vpX + hw, sy), 0.5f)
        }
        for (col in 0..bw) {
            val bxp = vpX - bottomHalfW + col * (bottomHalfW * 2 / bw)
            val txp = vpX - topHalfW + col * (topHalfW * 2 / bw)
            drawLine(themeColor.copy(0.04f), Offset(bxp, bottomY), Offset(txp, topY), 0.5f)
        }

        for (row in bh - 1 downTo 0) {
            val rp = rowParams(row)
            for (col in 0 until bw) {
                var ci = 0
                if (state.board.isNotEmpty() && row < state.board.size) {
                    for (z in 0 until Tetris3DGame.BOARD_D) {
                        if (z < state.board[row].size && col < state.board[row][z].size) {
                            val c = state.board[row][z][col]; if (c > 0) { ci = c; break }
                        }
                    }
                }
                if (ci > 0) drawSWBlock(rp, col, pieceColor(ci, themeColor), rp.alpha)
            }
        }
        val piece = state.currentPiece
        if (piece != null && showGhost && state.ghostY < piece.y) {
            for (b in piece.blocks) {
                val row = state.ghostY + b.y; val col = piece.x + b.x
                if (row in 0 until bh && col in 0 until bw) drawSWBlock(rowParams(row), col, themeColor, 0.15f * rowParams(row).alpha)
            }
        }
        if (piece != null) {
            for (b in piece.blocks) {
                val row = piece.y + b.y; val col = piece.x + b.x
                if (row in 0 until bh && col in 0 until bw) drawSWBlock(rowParams(row), col, pieceColor(piece.type.colorIndex, themeColor), rowParams(row).alpha)
            }
        }
    }
}

private data class RowParams(val y: Float, val leftX: Float, val cellW: Float, val cellH: Float, val alpha: Float)

private fun DrawScope.drawSWBlock(rp: RowParams, col: Int, color: Color, alpha: Float) {
    val x = rp.leftX + col * rp.cellW; val y = rp.y - rp.cellH
    val cw = rp.cellW; val ch = rp.cellH; val a = alpha.coerceIn(0f, 1f); val d = ch * 0.18f
    drawRect(color.copy(a), Offset(x + 1, y), Size(cw - 2, ch - 1))
    val tp = Path().apply { moveTo(x+1,y); lineTo(x+cw-1,y); lineTo(x+cw-1-d,y-d); lineTo(x+1+d,y-d); close() }
    drawPath(tp, brighten(color, 1.1f).copy(a * 0.7f))
    val rp2 = Path().apply { moveTo(x+cw-1,y); lineTo(x+cw-1,y+ch-1); lineTo(x+cw-1-d,y+ch-1-d); lineTo(x+cw-1-d,y-d); close() }
    drawPath(rp2, darken(color, 0.5f).copy(a * 0.6f))
    drawRect(Color.White.copy(0.18f * a), Offset(x + 2, y + 1), Size(cw * 0.25f, 2f))
    drawRect(color.copy(a * 0.1f), Offset(x + 1, y), Size(cw - 2, ch - 1), style = Stroke(0.5f))
}

// ==================== UTILS ====================

private fun pieceColor(idx: Int, themeColor: Color): Color = when (idx) {
    1 -> Color(0xFF00E5FF); 2 -> Color(0xFFFFD600); 3 -> Color(0xFFAA00FF); 4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00); 6 -> Color(0xFFFF1744); 7 -> Color(0xFF2979FF); 8 -> Color(0xFFFF4081)
    else -> themeColor
}

private fun darken(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f, c.alpha)
private fun brighten(c: Color, f: Float) = Color(minOf(c.red * f, 1f), minOf(c.green * f, 1f), minOf(c.blue * f, 1f), c.alpha)
