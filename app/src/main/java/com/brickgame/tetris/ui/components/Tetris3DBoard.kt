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
import androidx.compose.ui.graphics.nativeCanvas
import com.brickgame.tetris.game.*
import kotlin.math.*

@Composable
fun Tetris3DBoard(
    state: Game3DState,
    modifier: Modifier = Modifier,
    showGhost: Boolean = true,
    cameraAngleY: Float = 35f,
    cameraAngleX: Float = 25f,
    zoom: Float = 1f,
    themePixelOn: Color = Color(0xFF22C55E),
    themeBg: Color = Color(0xFF0A0A0A),
    starWarsMode: Boolean = false
) {
    if (starWarsMode) {
        StarWarsBoard(state, modifier, showGhost, themePixelOn)
    } else {
        FreeCameraBoard(state, modifier, showGhost, cameraAngleY, cameraAngleX, zoom, themePixelOn)
    }
}

// ==================== FREE CAMERA MODE ====================

@Composable
private fun FreeCameraBoard(
    state: Game3DState,
    modifier: Modifier,
    showGhost: Boolean,
    cameraAngleY: Float,
    cameraAngleX: Float,
    zoom: Float,
    themeColor: Color
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bw = Tetris3DGame.BOARD_W.toFloat()
        val bd = Tetris3DGame.BOARD_D.toFloat()
        val bh = Tetris3DGame.BOARD_H.toFloat()

        // Full 360 rotation on both axes
        val radY = Math.toRadians(cameraAngleY.toDouble())
        val radX = Math.toRadians(cameraAngleX.toDouble())
        val cosY = cos(radY).toFloat(); val sinY = sin(radY).toFloat()
        val cosX = cos(radX).toFloat(); val sinX = sin(radX).toFloat()

        val screenScale = minOf(w, h) / 400f
        val fov = 700f * screenScale * zoom
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
            val rz = -dx * sinY + dz * cosY
            val rz2 = dy * sinX + rz * cosX
            return rz2 + camDist
        }

        // ===== WIREFRAME WITH LABELS =====
        val edgeColor = themeColor.copy(alpha = 0.15f)
        val edgeBright = themeColor.copy(alpha = 0.25f)
        fun edge(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, c: Color = edgeColor, sw: Float = 0.8f) {
            val a = project(x1, y1, z1); val b = project(x2, y2, z2)
            if (a != null && b != null) drawLine(c, a, b, sw)
        }

        // Bottom edges (bright)
        edge(0f,0f,0f, bw,0f,0f, edgeBright, 1.2f); edge(0f,0f,bd, bw,0f,bd, edgeBright, 1.2f)
        edge(0f,0f,0f, 0f,0f,bd, edgeBright, 1.2f); edge(bw,0f,0f, bw,0f,bd, edgeBright, 1.2f)
        // Vertical edges
        edge(0f,0f,0f, 0f,bh,0f); edge(bw,0f,0f, bw,bh,0f)
        edge(0f,0f,bd, 0f,bh,bd); edge(bw,0f,bd, bw,bh,bd)
        // Top edges
        edge(0f,bh,0f, bw,bh,0f); edge(0f,bh,bd, bw,bh,bd)
        edge(0f,bh,0f, 0f,bh,bd); edge(bw,bh,0f, bw,bh,bd)

        // Floor grid
        val gridColor = themeColor.copy(alpha = 0.05f)
        for (x in 0..bw.toInt()) {
            val a = project(x.toFloat(), 0f, 0f); val b = project(x.toFloat(), 0f, bd)
            if (a != null && b != null) drawLine(gridColor, a, b, 0.5f)
        }
        for (z in 0..bd.toInt()) {
            val a = project(0f, 0f, z.toFloat()); val b = project(bw, 0f, z.toFloat())
            if (a != null && b != null) drawLine(gridColor, a, b, 0.5f)
        }

        // ===== AXIS LABELS =====
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f * screenScale
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            alpha = 140
        }

        // X axis label (along bottom, x direction)
        val xMid = project(bw / 2f, -0.8f, -0.5f)
        if (xMid != null) drawContext.canvas.nativeCanvas.drawText("X", xMid.x, xMid.y, paint)
        // Z axis label (along bottom, z direction)
        val zMid = project(-0.5f, -0.8f, bd / 2f)
        if (zMid != null) drawContext.canvas.nativeCanvas.drawText("Z", zMid.x, zMid.y, paint)
        // Y axis label (vertical)
        val yMid = project(-0.8f, bh / 2f, -0.5f)
        if (yMid != null) drawContext.canvas.nativeCanvas.drawText("Y", yMid.x, yMid.y, paint)

        // Corner labels for orientation
        val cornerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f * screenScale
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true; alpha = 80
        }
        val lFront = project(bw/2f, -1.2f, -0.3f)
        if (lFront != null) drawContext.canvas.nativeCanvas.drawText("FRONT", lFront.x, lFront.y, cornerPaint)
        val lBack = project(bw/2f, -1.2f, bd + 0.3f)
        if (lBack != null) drawContext.canvas.nativeCanvas.drawText("BACK", lBack.x, lBack.y, cornerPaint)
        val lLeft = project(-0.3f, -1.2f, bd/2f)
        if (lLeft != null) drawContext.canvas.nativeCanvas.drawText("L", lLeft.x, lLeft.y, cornerPaint)
        val lRight = project(bw + 0.3f, -1.2f, bd/2f)
        if (lRight != null) drawContext.canvas.nativeCanvas.drawText("R", lRight.x, lRight.y, cornerPaint)

        // ===== BLOCKS =====
        data class Face(val points: List<Offset>, val color: Color, val depth: Float)
        val faces = mutableListOf<Face>()

        fun addCube(gx: Int, gy: Int, gz: Int, baseColor: Color, alpha: Float) {
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

            // Colored faces — similar to 2D multicolor but with shading
            val topC = lighten(baseColor, 1.1f).copy(alpha = a)
            val frontC = baseColor.copy(alpha = a)
            val rightC = darken(baseColor, 0.7f).copy(alpha = a)
            val leftC = darken(baseColor, 0.55f).copy(alpha = a)
            val backC = darken(baseColor, 0.45f).copy(alpha = a)
            val botC = darken(baseColor, 0.35f).copy(alpha = a)
            val mid = (x + 0.5f) to (z + 0.5f)

            // All 6 faces
            faces.add(Face(listOf(p010, p110, p111, p011), topC, depth(mid.first, y+1, mid.second)))
            faces.add(Face(listOf(p000, p100, p101, p001), botC, depth(mid.first, y, mid.second)))
            faces.add(Face(listOf(p000, p100, p110, p010), frontC, depth(mid.first, y+0.5f, z)))
            faces.add(Face(listOf(p001, p101, p111, p011), backC, depth(mid.first, y+0.5f, z+1)))
            faces.add(Face(listOf(p000, p001, p011, p010), leftC, depth(x, y+0.5f, mid.second)))
            faces.add(Face(listOf(p100, p101, p111, p110), rightC, depth(x+1, y+0.5f, mid.second)))
        }

        // Board blocks
        if (state.board.isNotEmpty()) {
            for (y in 0 until Tetris3DGame.BOARD_H) {
                for (z in 0 until Tetris3DGame.BOARD_D) {
                    for (x in 0 until Tetris3DGame.BOARD_W) {
                        if (y < state.board.size && z < state.board[y].size && x < state.board[y][z].size) {
                            val c = state.board[y][z][x]
                            if (c > 0) addCube(x, y, z, pieceColor(c), 1f)
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
                if (gy >= 0) addCube(gx, gy, gz, themeColor.copy(alpha = 0.3f), 0.2f)
            }
        }

        // Current piece
        if (piece != null) {
            for (b in piece.blocks) {
                val bx = piece.x + b.x; val by = piece.y + b.y; val bz = piece.z + b.z
                if (by >= 0) addCube(bx, by, bz, pieceColor(piece.type.colorIndex), 1f)
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
            // Thin edge outline on each face for block definition
            drawPath(path, Color.Black.copy(alpha = face.color.alpha * 0.3f), style = Stroke(width = 0.5f))
        }

        // Bright edge highlights on current piece
        if (piece != null) {
            for (b in piece.blocks) {
                val bx = piece.x + b.x; val by = piece.y + b.y; val bz = piece.z + b.z
                if (by < 0) continue
                val t0 = project(bx.toFloat(), by+1f, bz.toFloat())
                val t1 = project(bx+1f, by+1f, bz.toFloat())
                val t2 = project(bx+1f, by+1f, bz+1f)
                val t3 = project(bx.toFloat(), by+1f, bz+1f)
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
            val bx = vpX - bottomHalfW + col * (bottomHalfW * 2 / bw)
            val tx = vpX - topHalfW + col * (topHalfW * 2 / bw)
            drawLine(themeColor.copy(alpha = 0.04f), Offset(bx, bottomY), Offset(tx, topY), 0.5f)
        }

        // Board blocks top-to-bottom (far to near)
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
                if (colorIdx > 0) drawSWBlock(rp, col, pieceColor(colorIdx), rp.alpha)
            }
        }

        // Ghost + current piece
        val piece = state.currentPiece
        if (piece != null && showGhost && state.ghostY < piece.y) {
            for (b in piece.blocks) {
                val row = state.ghostY + b.y; val col = piece.x + b.x
                if (row in 0 until bh && col in 0 until bw) {
                    drawSWBlock(rowParams(row), col, themeColor, 0.15f * rowParams(row).alpha)
                }
            }
        }
        if (piece != null) {
            for (b in piece.blocks) {
                val row = piece.y + b.y; val col = piece.x + b.x
                if (row in 0 until bh && col in 0 until bw) {
                    drawSWBlock(rowParams(row), col, pieceColor(piece.type.colorIndex), rowParams(row).alpha)
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

    // Top face
    val topPath = Path().apply {
        moveTo(x + 1, y); lineTo(x + cw - 1, y)
        lineTo(x + cw - 1 - depth, y - depth); lineTo(x + 1 + depth, y - depth); close()
    }
    drawPath(topPath, darken(color, 0.8f).copy(alpha = a * 0.7f))

    // Right face
    val rightPath = Path().apply {
        moveTo(x + cw - 1, y); lineTo(x + cw - 1, y + ch - 1)
        lineTo(x + cw - 1 - depth, y + ch - 1 - depth); lineTo(x + cw - 1 - depth, y - depth); close()
    }
    drawPath(rightPath, darken(color, 0.5f).copy(alpha = a * 0.6f))

    // Front face
    drawRect(color.copy(alpha = a), Offset(x + 1, y), Size(cw - 2, ch - 1))

    // Highlight
    drawRect(Color.White.copy(alpha = 0.15f * a), Offset(x + 2, y + 1), Size(cw * 0.3f, 2f))
    drawRect(Color.White.copy(alpha = 0.1f * a), Offset(x + 2, y + 1), Size(2f, ch * 0.3f))
}

// ==================== VIEWCUBE ====================

/**
 * Interactive ViewCube — small 3D cube in corner showing orientation.
 * Tap a face to snap to that view. Rendered as a mini isometric cube with labeled faces.
 */
@Composable
fun ViewCube(
    cameraAngleY: Float,
    cameraAngleX: Float,
    modifier: Modifier = Modifier,
    themeColor: Color = Color(0xFF22C55E),
    onFaceClick: (face: String) -> Unit = {}
) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = minOf(w, h) * 0.38f // cube half-size in screen space

        val radY = Math.toRadians(cameraAngleY.toDouble())
        val radX = Math.toRadians(cameraAngleX.toDouble())
        val cosY = cos(radY).toFloat(); val sinY = sin(radY).toFloat()
        val cosX = cos(radX).toFloat(); val sinX = sin(radX).toFloat()

        fun proj(x: Float, y: Float, z: Float): Offset {
            val rx = x * cosY + z * sinY
            val rz = -x * sinY + z * cosY
            val ry = y * cosX - rz * sinX
            val rz2 = y * sinX + rz * cosX
            val d = rz2 + 4f
            val sc = s * 2.5f / d
            return Offset(w / 2f + rx * sc, h / 2f - ry * sc)
        }

        fun faceDepth(cx: Float, cy: Float, cz: Float): Float {
            val rz = -cx * sinY + cz * cosY
            return cy * sinX + rz * cosX + 4f
        }

        // 8 corners of unit cube centered at origin
        val corners = arrayOf(
            Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f), Triple(1f, 1f, -1f), Triple(-1f, 1f, -1f),
            Triple(-1f, -1f, 1f), Triple(1f, -1f, 1f), Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f)
        )
        val pts = corners.map { (x, y, z) -> proj(x, y, z) }

        // 6 faces with indices, center points, labels, colors
        data class CubeFace(val idx: IntArray, val label: String, val color: Color, val cx: Float, val cy: Float, val cz: Float)
        val cubeFaces = listOf(
            CubeFace(intArrayOf(3,2,6,7), "TOP", themeColor.copy(0.4f), 0f, 1f, 0f),
            CubeFace(intArrayOf(0,1,5,4), "BOT", themeColor.copy(0.2f), 0f, -1f, 0f),
            CubeFace(intArrayOf(0,1,2,3), "FRONT", themeColor.copy(0.35f), 0f, 0f, -1f),
            CubeFace(intArrayOf(4,5,6,7), "BACK", themeColor.copy(0.25f), 0f, 0f, 1f),
            CubeFace(intArrayOf(0,3,7,4), "LEFT", themeColor.copy(0.3f), -1f, 0f, 0f),
            CubeFace(intArrayOf(1,2,6,5), "RIGHT", themeColor.copy(0.3f), 1f, 0f, 0f)
        )

        // Sort by depth (back to front)
        val sorted = cubeFaces.sortedByDescending { faceDepth(it.cx, it.cy, it.cz) }

        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = minOf(w, h) * 0.13f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        sorted.forEach { face ->
            val fPts = face.idx.map { pts[it] }
            val path = Path().apply {
                moveTo(fPts[0].x, fPts[0].y)
                for (i in 1 until fPts.size) lineTo(fPts[i].x, fPts[i].y)
                close()
            }
            drawPath(path, face.color, style = Fill)
            drawPath(path, Color.White.copy(0.3f), style = Stroke(width = 1f))

            // Label at center of face
            val center = proj(face.cx, face.cy, face.cz)
            // Only draw label if face is facing the camera (depth > threshold)
            if (faceDepth(face.cx, face.cy, face.cz) < 4f) {
                drawContext.canvas.nativeCanvas.drawText(
                    face.label, center.x, center.y + labelPaint.textSize * 0.35f, labelPaint
                )
            }
        }
    }
}

// ==================== SHARED UTILS ====================

private fun pieceColor(idx: Int): Color = when (idx) {
    1 -> Color(0xFF00E5FF); 2 -> Color(0xFFFFD600); 3 -> Color(0xFFAA00FF); 4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00); 6 -> Color(0xFFFF1744); 7 -> Color(0xFF2979FF); 8 -> Color(0xFFFF4081)
    else -> Color(0xFF888888)
}

private fun darken(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f, c.alpha)
private fun lighten(c: Color, f: Float) = Color(minOf(c.red * f, 1f), minOf(c.green * f, 1f), minOf(c.blue * f, 1f), c.alpha)
