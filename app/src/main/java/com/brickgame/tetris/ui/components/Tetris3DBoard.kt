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
import kotlin.math.*

/**
 * Perspective 3D Tetris board renderer with rotatable camera.
 * Uses a simple 3D→2D perspective projection with configurable camera angle.
 * "Star Wars" mode: dramatic vanishing point, pieces come toward the viewer.
 */
@Composable
fun Tetris3DBoard(
    state: Game3DState,
    modifier: Modifier = Modifier,
    showGhost: Boolean = true,
    cameraAngleY: Float = 35f,   // Horizontal rotation in degrees (swipe to rotate)
    cameraAngleX: Float = 25f,   // Vertical tilt in degrees
    themePixelOn: Color = Color(0xFF22C55E),
    themeBg: Color = Color(0xFF0A0A0A),
    starWarsMode: Boolean = false // Dramatic perspective from below
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bw = Tetris3DGame.BOARD_W.toFloat()
        val bd = Tetris3DGame.BOARD_D.toFloat()
        val bh = Tetris3DGame.BOARD_H.toFloat()

        // Camera setup
        val radY = Math.toRadians(cameraAngleY.toDouble())
        val radX = Math.toRadians(cameraAngleX.toDouble())
        val cosY = cos(radY).toFloat(); val sinY = sin(radY).toFloat()
        val cosX = cos(radX).toFloat(); val sinX = sin(radX).toFloat()

        // Camera distance & projection
        val fov = if (starWarsMode) 350f else 500f
        val camDist = if (starWarsMode) 18f else 22f

        // Center the board at origin
        val cx = bw / 2f; val cz = bd / 2f; val cy = bh / 2f

        fun project(px: Float, py: Float, pz: Float): Offset? {
            // Translate to center
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            // Rotate around Y axis (horizontal)
            val rx = dx * cosY + dz * sinY
            val rz = -dx * sinY + dz * cosY
            // Rotate around X axis (vertical tilt)
            val ry = dy * cosX - rz * sinX
            val rz2 = dy * sinX + rz * cosX
            // Perspective projection
            val z = rz2 + camDist
            if (z < 0.5f) return null
            val scale = fov / z
            val sx = w / 2f + rx * scale
            val sy = h / 2f - ry * scale
            return Offset(sx, sy)
        }

        // Depth sort helper: distance from camera for a 3D point
        fun depth(px: Float, py: Float, pz: Float): Float {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            val rx = dx * cosY + dz * sinY
            val rz = -dx * sinY + dz * cosY
            val ry = dy * cosX - rz * sinX
            val rz2 = dy * sinX + rz * cosX
            return rz2 + camDist
        }

        // ========== Draw walls (back faces only) ==========
        val wallColor = themePixelOn.copy(alpha = 0.04f)
        val wallLineColor = themePixelOn.copy(alpha = 0.08f)

        // Collect all wall quads with depth for sorting
        data class Quad(val p: List<Offset>, val color: Color, val depth: Float)
        val quads = mutableListOf<Quad>()

        // Floor (y=0)
        val f0 = project(0f, 0f, 0f); val f1 = project(bw, 0f, 0f)
        val f2 = project(bw, 0f, bd); val f3 = project(0f, 0f, bd)
        if (f0 != null && f1 != null && f2 != null && f3 != null) {
            quads.add(Quad(listOf(f0, f1, f2, f3), themePixelOn.copy(alpha = 0.03f),
                depth(bw / 2, 0f, bd / 2)))
        }

        // Floor grid lines
        for (x in 0..bw.toInt()) {
            val a = project(x.toFloat(), 0f, 0f); val b = project(x.toFloat(), 0f, bd)
            if (a != null && b != null) drawLine(wallLineColor.copy(alpha = 0.06f), a, b, 0.5f)
        }
        for (z in 0..bd.toInt()) {
            val a = project(0f, 0f, z.toFloat()); val b = project(bw, 0f, z.toFloat())
            if (a != null && b != null) drawLine(wallLineColor.copy(alpha = 0.06f), a, b, 0.5f)
        }

        // Back walls — only draw walls facing the camera
        // Left wall (x=0)
        val lw0 = project(0f, 0f, 0f); val lw1 = project(0f, 0f, bd)
        val lw2 = project(0f, bh, bd); val lw3 = project(0f, bh, 0f)
        if (lw0 != null && lw1 != null && lw2 != null && lw3 != null) {
            quads.add(Quad(listOf(lw0, lw1, lw2, lw3), wallColor,
                depth(0f, bh / 2, bd / 2)))
        }
        // Right wall (x=bw)
        val rw0 = project(bw, 0f, 0f); val rw1 = project(bw, 0f, bd)
        val rw2 = project(bw, bh, bd); val rw3 = project(bw, bh, 0f)
        if (rw0 != null && rw1 != null && rw2 != null && rw3 != null) {
            quads.add(Quad(listOf(rw0, rw1, rw2, rw3), wallColor,
                depth(bw, bh / 2, bd / 2)))
        }
        // Back wall (z=bd)
        val bk0 = project(0f, 0f, bd); val bk1 = project(bw, 0f, bd)
        val bk2 = project(bw, bh, bd); val bk3 = project(0f, bh, bd)
        if (bk0 != null && bk1 != null && bk2 != null && bk3 != null) {
            quads.add(Quad(listOf(bk0, bk1, bk2, bk3), wallColor,
                depth(bw / 2, bh / 2, bd)))
        }
        // Front wall (z=0) — subtle
        val fw0 = project(0f, 0f, 0f); val fw1 = project(bw, 0f, 0f)
        val fw2 = project(bw, bh, 0f); val fw3 = project(0f, bh, 0f)
        if (fw0 != null && fw1 != null && fw2 != null && fw3 != null) {
            quads.add(Quad(listOf(fw0, fw1, fw2, fw3), wallColor.copy(alpha = 0.02f),
                depth(bw / 2, bh / 2, 0f)))
        }

        // Draw walls sorted back-to-front
        quads.sortByDescending { it.depth }
        quads.forEach { q ->
            val path = Path().apply {
                moveTo(q.p[0].x, q.p[0].y)
                for (i in 1 until q.p.size) lineTo(q.p[i].x, q.p[i].y)
                close()
            }
            drawPath(path, q.color)
        }

        // Wall edge lines (wireframe)
        fun drawEdge(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, alpha: Float = 0.1f) {
            val a = project(x1, y1, z1); val b = project(x2, y2, z2)
            if (a != null && b != null) drawLine(themePixelOn.copy(alpha = alpha), a, b, 0.8f)
        }
        // Vertical edges
        drawEdge(0f, 0f, 0f, 0f, bh, 0f); drawEdge(bw, 0f, 0f, bw, bh, 0f)
        drawEdge(0f, 0f, bd, 0f, bh, bd); drawEdge(bw, 0f, bd, bw, bh, bd)
        // Top edges
        drawEdge(0f, bh, 0f, bw, bh, 0f); drawEdge(0f, bh, bd, bw, bh, bd)
        drawEdge(0f, bh, 0f, 0f, bh, bd); drawEdge(bw, bh, 0f, bw, bh, bd)
        // Bottom edges
        drawEdge(0f, 0f, 0f, bw, 0f, 0f, 0.06f); drawEdge(0f, 0f, bd, bw, 0f, bd, 0.06f)
        drawEdge(0f, 0f, 0f, 0f, 0f, bd, 0.06f); drawEdge(bw, 0f, 0f, bw, 0f, bd, 0.06f)

        // ========== Draw blocks ==========
        // Collect all cube faces, sort by depth, then draw
        data class Face(val points: List<Offset>, val color: Color, val depth: Float)
        val faces = mutableListOf<Face>()

        fun addCube(gx: Int, gy: Int, gz: Int, color: Color, alpha: Float) {
            val x = gx.toFloat(); val y = gy.toFloat(); val z = gz.toFloat()
            val a = alpha.coerceIn(0f, 1f)

            // 8 corners
            val p000 = project(x, y, z) ?: return
            val p100 = project(x+1, y, z) ?: return
            val p010 = project(x, y+1, z) ?: return
            val p110 = project(x+1, y+1, z) ?: return
            val p001 = project(x, y, z+1) ?: return
            val p101 = project(x+1, y, z+1) ?: return
            val p011 = project(x, y+1, z+1) ?: return
            val p111 = project(x+1, y+1, z+1) ?: return

            val topC = color.copy(alpha = a)
            val sideC1 = darken(color, 0.6f).copy(alpha = a)
            val sideC2 = darken(color, 0.4f).copy(alpha = a)
            val bottomC = darken(color, 0.3f).copy(alpha = a)

            val mid = (x + 0.5f) to (z + 0.5f)

            // Top face (y+1)
            faces.add(Face(listOf(p010, p110, p111, p011), topC,
                depth(mid.first, y + 1f, mid.second)))
            // Bottom face (y)
            faces.add(Face(listOf(p000, p100, p101, p001), bottomC,
                depth(mid.first, y, mid.second)))
            // Front face (z=gz)
            faces.add(Face(listOf(p000, p100, p110, p010), sideC1,
                depth(mid.first, y + 0.5f, z)))
            // Back face (z=gz+1)
            faces.add(Face(listOf(p001, p101, p111, p011), sideC2,
                depth(mid.first, y + 0.5f, z + 1)))
            // Left face (x=gx)
            faces.add(Face(listOf(p000, p001, p011, p010), sideC2,
                depth(x, y + 0.5f, mid.second)))
            // Right face (x=gx+1)
            faces.add(Face(listOf(p100, p101, p111, p110), sideC1,
                depth(x + 1, y + 0.5f, mid.second)))
        }

        // Board blocks
        if (state.board.isNotEmpty()) {
            for (y in 0 until Tetris3DGame.BOARD_H) {
                for (z in 0 until Tetris3DGame.BOARD_D) {
                    for (x in 0 until Tetris3DGame.BOARD_W) {
                        if (y < state.board.size && z < state.board[y].size && x < state.board[y][z].size) {
                            val colorIdx = state.board[y][z][x]
                            if (colorIdx > 0) addCube(x, y, z, pieceColor(colorIdx, themePixelOn), 1f)
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
                if (gy >= 0) addCube(gx, gy, gz, themePixelOn, 0.15f)
            }
        }

        // Current piece
        if (piece != null) {
            for (b in piece.blocks) {
                val bx = piece.x + b.x; val by = piece.y + b.y; val bz = piece.z + b.z
                if (by >= 0) addCube(bx, by, bz, pieceColor(piece.type.colorIndex, themePixelOn), 1f)
            }
        }

        // Sort faces back-to-front and draw
        faces.sortByDescending { it.depth }
        faces.forEach { face ->
            val path = Path().apply {
                moveTo(face.points[0].x, face.points[0].y)
                for (i in 1 until face.points.size) lineTo(face.points[i].x, face.points[i].y)
                close()
            }
            drawPath(path, face.color, style = Fill)
        }

        // Draw edge highlights on current piece (so it stands out)
        if (piece != null) {
            for (b in piece.blocks) {
                val bx = piece.x + b.x; val by = piece.y + b.y; val bz = piece.z + b.z
                if (by < 0) continue
                val top0 = project(bx.toFloat(), by + 1f, bz.toFloat())
                val top1 = project(bx + 1f, by + 1f, bz.toFloat())
                val top2 = project(bx + 1f, by + 1f, bz + 1f)
                val top3 = project(bx.toFloat(), by + 1f, bz + 1f)
                if (top0 != null && top1 != null && top2 != null && top3 != null) {
                    val edgeColor = Color.White.copy(alpha = 0.3f)
                    drawLine(edgeColor, top0, top1, 1.5f)
                    drawLine(edgeColor, top1, top2, 1f)
                    drawLine(edgeColor, top2, top3, 1f)
                    drawLine(edgeColor, top3, top0, 1f)
                }
            }
        }
    }
}

/** Get color for a piece index, tinted by theme */
private fun pieceColor(idx: Int, themeColor: Color): Color = when (idx) {
    1 -> Color(0xFF00E5FF)  // cyan
    2 -> Color(0xFFFFD600)  // yellow
    3 -> Color(0xFFAA00FF)  // purple
    4 -> Color(0xFF00E676)  // green
    5 -> Color(0xFFFF6D00)  // orange
    6 -> Color(0xFFFF1744)  // red
    7 -> Color(0xFF2979FF)  // blue
    8 -> Color(0xFFFF4081)  // pink
    else -> themeColor
}

private fun darken(color: Color, factor: Float): Color = Color(
    red = color.red * factor,
    green = color.green * factor,
    blue = color.blue * factor,
    alpha = color.alpha
)
