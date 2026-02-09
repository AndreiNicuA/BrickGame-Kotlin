package com.brickgame.tetris.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.brickgame.tetris.game.*
import kotlin.math.*

/** Material style for 3D pieces */
enum class PieceMaterial { CLASSIC, STONE, GRANITE, GLASS, CRYSTAL }

@Composable
fun Tetris3DBoard(
    state: Game3DState, modifier: Modifier = Modifier, showGhost: Boolean = true,
    cameraAngleY: Float = 35f, cameraAngleX: Float = 25f,
    panOffsetX: Float = 0f, panOffsetY: Float = 0f, zoom: Float = 1f,
    themePixelOn: Color = Color(0xFF22C55E), themeBg: Color = Color(0xFF0A0A0A),
    starWarsMode: Boolean = false, material: PieceMaterial = PieceMaterial.CLASSIC
) {
    if (starWarsMode) StarWarsBoard(state, modifier, showGhost, themePixelOn)
    else FreeCameraBoard(state, modifier, showGhost, cameraAngleY, cameraAngleX, panOffsetX, panOffsetY, zoom, themePixelOn, material)
}

/** Fusion 360-style ViewCube with FRONT/BACK/LEFT/RIGHT/TOP face labels. */
@Composable
fun ViewCube(azimuth: Float, elevation: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val s = minOf(w, h) * 0.38f
        val radAz = Math.toRadians(azimuth.toDouble()); val radEl = Math.toRadians(elevation.toDouble())
        val cosAz = cos(radAz).toFloat(); val sinAz = sin(radAz).toFloat()
        val cosEl = cos(radEl).toFloat(); val sinEl = sin(radEl).toFloat()
        val cx = w / 2f; val cy = h / 2f

        fun proj(px: Float, py: Float, pz: Float): Offset {
            val rx = px * cosAz - py * sinAz
            val ry = px * sinAz + py * cosAz
            val rz = pz * cosEl - ry * sinEl
            return Offset(cx + rx * s, cy - rz * s)
        }

        val c = arrayOf(
            proj(-1f,-1f,-1f), proj(1f,-1f,-1f), proj(1f,1f,-1f), proj(-1f,1f,-1f),
            proj(-1f,-1f,1f), proj(1f,-1f,1f), proj(1f,1f,1f), proj(-1f,1f,1f)
        )
        val corners = arrayOf(
            floatArrayOf(-1f,-1f,-1f), floatArrayOf(1f,-1f,-1f), floatArrayOf(1f,1f,-1f), floatArrayOf(-1f,1f,-1f),
            floatArrayOf(-1f,-1f,1f), floatArrayOf(1f,-1f,1f), floatArrayOf(1f,1f,1f), floatArrayOf(-1f,1f,1f)
        )

        data class CF(val i: IntArray, val col: Color, val lbl: String)
        val allFaces = listOf(
            CF(intArrayOf(4,5,6,7), Color(0xFF4488FF).copy(0.65f), "TOP"),
            CF(intArrayOf(0,3,2,1), Color(0xFF4488FF).copy(0.3f), ""),
            CF(intArrayOf(0,1,5,4), Color(0xFF44FF44).copy(0.5f), "FRONT"),
            CF(intArrayOf(3,7,6,2), Color(0xFF44FF44).copy(0.3f), "BACK"),
            CF(intArrayOf(0,4,7,3), Color(0xFFFF4444).copy(0.5f), "LEFT"),
            CF(intArrayOf(1,2,6,5), Color(0xFFFF4444).copy(0.35f), "RIGHT")
        )

        data class SF(val i: IntArray, val col: Color, val lbl: String, val d: Float)
        val sorted = allFaces.map { f ->
            val d = f.i.map { idx ->
                val cr = corners[idx]
                val ry = cr[0] * sinAz + cr[1] * cosAz
                cr[2] * sinEl + ry * cosEl
            }.average().toFloat()
            SF(f.i, f.col, f.lbl, d)
        }.sortedBy { it.d }

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }

        for (face in sorted) {
            val path = Path().apply {
                moveTo(c[face.i[0]].x, c[face.i[0]].y)
                for (j in 1 until face.i.size) lineTo(c[face.i[j]].x, c[face.i[j]].y)
                close()
            }
            drawPath(path, face.col, style = Fill)
            drawPath(path, Color.White.copy(0.3f), style = Stroke(1f))

            if (face.d > 0.1f && face.lbl.isNotEmpty()) {
                val fcx = face.i.map { c[it].x }.average().toFloat()
                val fcy = face.i.map { c[it].y }.average().toFloat()
                val alpha = (face.d.coerceIn(0f, 1f) * 230).toInt()
                val faceSize = s * face.d.coerceIn(0.3f, 1f)
                textPaint.textSize = faceSize * 0.42f
                textPaint.alpha = alpha
                drawContext.canvas.nativeCanvas.drawText(face.lbl, fcx, fcy + textPaint.textSize * 0.35f, textPaint)
            }
        }

        val xE = proj(1.5f, 0f, 0f); val yE = proj(0f, 1.5f, 0f); val zE = proj(0f, 0f, 1.5f)
        val origin = Offset(cx, cy)
        drawLine(Color(0xFFFF4444), origin, xE, 2.5f)
        drawLine(Color(0xFF44FF44), origin, yE, 2.5f)
        drawLine(Color(0xFF4488FF), origin, zE, 2.5f)

        val axisPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            isAntiAlias = true; textSize = s * 0.4f
        }
        axisPaint.color = android.graphics.Color.rgb(255, 68, 68)
        drawContext.canvas.nativeCanvas.drawText("X", xE.x, xE.y + axisPaint.textSize * 0.35f, axisPaint)
        axisPaint.color = android.graphics.Color.rgb(68, 255, 68)
        drawContext.canvas.nativeCanvas.drawText("Y", yE.x, yE.y + axisPaint.textSize * 0.35f, axisPaint)
        axisPaint.color = android.graphics.Color.rgb(68, 136, 255)
        drawContext.canvas.nativeCanvas.drawText("Z", zE.x, zE.y + axisPaint.textSize * 0.35f, axisPaint)
    }
}

/** Mini 3D piece preview */
@Composable
fun Mini3DPiecePreview(pieceType: Piece3DType?, modifier: Modifier = Modifier,
                       themeColor: Color = Color(0xFF22C55E), alpha: Float = 1f) {
    Canvas(modifier) {
        if (pieceType == null) return@Canvas
        val w = size.width; val h = size.height; val blocks = pieceType.blocks
        val color = pieceColor(pieceType.colorIndex, themeColor)
        val minX = blocks.minOf { it.x }; val maxX = blocks.maxOf { it.x }
        val minY = blocks.minOf { it.y }; val maxY = blocks.maxOf { it.y }
        val minZ = blocks.minOf { it.z }; val maxZ = blocks.maxOf { it.z }
        val rx = (maxX - minX + 1).toFloat(); val ry = (maxY - minY + 1).toFloat(); val rz = (maxZ - minZ + 1).toFloat()
        val cellW = minOf(w / (rx + rz + 0.5f), h / (ry * 1.1f + (rx + rz) * 0.25f + 0.5f))
        val cellH = cellW * 0.5f; val cellYH = cellW * 0.6f
        val totalW = (rx + rz) * cellW * 0.5f
        val ox = (w - totalW) / 2f + rz * cellW * 0.5f; val oy = (h + ry * cellYH) / 2f
        fun isoP(px: Float, py: Float, pz: Float) = Offset(ox + (px - pz) * cellW * 0.5f, oy + (px + pz) * cellH * 0.5f - py * cellYH)
        val sorted = blocks.sortedWith(compareBy({ -(it.z - minZ) }, { -(it.x - minX) }, { it.y - minY }))
        for (b in sorted) {
            val bx = (b.x - minX).toFloat(); val by = (b.y - minY).toFloat(); val bz = (b.z - minZ).toFloat()
            val tfl = isoP(bx,by+1,bz); val tfr = isoP(bx+1,by+1,bz); val tbl = isoP(bx,by+1,bz+1)
            val tbr = isoP(bx+1,by+1,bz+1); val bfl = isoP(bx,by,bz); val bfr = isoP(bx+1,by,bz); val bbl = isoP(bx,by,bz+1)
            drawPath(Path().apply { moveTo(tfl.x,tfl.y); lineTo(tfr.x,tfr.y); lineTo(tbr.x,tbr.y); lineTo(tbl.x,tbl.y); close() }, brighten(color, 1.15f).copy(alpha))
            drawPath(Path().apply { moveTo(tfl.x,tfl.y); lineTo(tbl.x,tbl.y); lineTo(bbl.x,bbl.y); lineTo(bfl.x,bfl.y); close() }, darken(color, 0.55f).copy(alpha))
            drawPath(Path().apply { moveTo(tfl.x,tfl.y); lineTo(tfr.x,tfr.y); lineTo(bfr.x,bfr.y); lineTo(bfl.x,bfl.y); close() }, darken(color, 0.38f).copy(alpha))
            drawLine(Color.White.copy(0.2f * alpha), tfl, tfr, 0.8f)
        }
    }
}

// ==================== FREE CAMERA ====================

@Composable
private fun FreeCameraBoard(
    state: Game3DState, modifier: Modifier, showGhost: Boolean,
    azimuth: Float, elevation: Float, panX: Float, panY: Float, zoom: Float,
    themeColor: Color, material: PieceMaterial
) {
    // Animate clearing layers
    val clearAnimProgress = state.clearAnimProgress

    Canvas(modifier) {
        val w = size.width; val h = size.height
        val bx = Tetris3DGame.BOARD_W.toFloat(); val by = Tetris3DGame.BOARD_D.toFloat(); val bz = Tetris3DGame.BOARD_H.toFloat()
        val radAz = Math.toRadians(azimuth.toDouble()); val radEl = Math.toRadians(elevation.toDouble())
        val cosAz = cos(radAz).toFloat(); val sinAz = sin(radAz).toFloat()
        val cosEl = cos(radEl).toFloat(); val sinEl = sin(radEl).toFloat()
        val screenScale = minOf(w, h) / 400f; val fov = 700f * screenScale * zoom; val camDist = 16f
        val cx = bx / 2f; val cy = by / 2f; val cz = bz / 2f

        fun project(px: Float, py: Float, pz: Float): Offset? {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            val rx = dx * cosAz - dy * sinAz; val ry = dx * sinAz + dy * cosAz
            val rz = dz * cosEl - ry * sinEl; val ry2 = dz * sinEl + ry * cosEl
            val depth = ry2 + camDist; if (depth < 0.5f) return null
            val scale = fov / depth
            return Offset(w / 2f + rx * scale + panX, h / 2f - rz * scale + panY)
        }
        fun depth(px: Float, py: Float, pz: Float): Float {
            val dx = px - cx; val dy = py - cy; val dz = pz - cz
            val ry = dx * sinAz + dy * cosAz; return dz * sinEl + ry * cosEl + camDist
        }

        // Wireframe — always visible with contrasting edge color
        val wireColor = Color.White.copy(0.22f); val wireDim = Color.White.copy(0.10f)
        fun edge(x1: Float,y1: Float,z1: Float,x2: Float,y2: Float,z2: Float,c: Color=wireColor,sw: Float=1.2f) {
            val a = project(x1,y1,z1); val b = project(x2,y2,z2); if (a!=null && b!=null) drawLine(c,a,b,sw)
        }
        // Vertical edges (bright — top frame)
        edge(0f,0f,0f,0f,0f,bz); edge(bx,0f,0f,bx,0f,bz); edge(0f,by,0f,0f,by,bz); edge(bx,by,0f,bx,by,bz)
        // Top edges (bright frame)
        edge(0f,0f,bz,bx,0f,bz); edge(0f,by,bz,bx,by,bz); edge(0f,0f,bz,0f,by,bz); edge(bx,0f,bz,bx,by,bz)
        // Bottom edges (dimmer)
        edge(0f,0f,0f,bx,0f,0f,wireDim); edge(0f,by,0f,bx,by,0f,wireDim); edge(0f,0f,0f,0f,by,0f,wireDim); edge(bx,0f,0f,bx,by,0f,wireDim)

        // Floor grid
        val gridColor = Color.White.copy(0.06f)
        for (ix in 0..bx.toInt()) { val a = project(ix.toFloat(),0f,0f); val b = project(ix.toFloat(),by,0f); if (a!=null&&b!=null) drawLine(gridColor,a,b,0.5f) }
        for (iy in 0..by.toInt()) { val a = project(0f,iy.toFloat(),0f); val b = project(bx,iy.toFloat(),0f); if (a!=null&&b!=null) drawLine(gridColor,a,b,0.5f) }

        // Walls — semi-transparent
        val wallColor = Color.White.copy(0.02f)
        fun wallQ(x1:Float,y1:Float,z1:Float,x2:Float,y2:Float,z2:Float,x3:Float,y3:Float,z3:Float,x4:Float,y4:Float,z4:Float) {
            val p1=project(x1,y1,z1);val p2=project(x2,y2,z2);val p3=project(x3,y3,z3);val p4=project(x4,y4,z4)
            if(p1!=null&&p2!=null&&p3!=null&&p4!=null) drawPath(Path().apply{moveTo(p1.x,p1.y);lineTo(p2.x,p2.y);lineTo(p3.x,p3.y);lineTo(p4.x,p4.y);close()},wallColor)
        }
        wallQ(0f,0f,0f,bx,0f,0f,bx,by,0f,0f,by,0f)
        wallQ(0f,by,0f,bx,by,0f,bx,by,bz,0f,by,bz)
        wallQ(0f,0f,0f,0f,by,0f,0f,by,bz,0f,0f,bz)

        // Faces collection
        data class Face(val pts: List<Offset>, val fill: Color, val edge: Color?, val shine: Boolean, val d: Float)
        val faces = mutableListOf<Face>()

        fun addCube(dx: Int, dy: Int, dz: Int, color: Color, alpha: Float, highlight: Boolean = false, isGhost: Boolean = false, clearing: Float = 0f) {
            val x=dx.toFloat();val y=dy.toFloat();val z=dz.toFloat();val a=alpha.coerceIn(0f,1f)
            val p000=project(x,y,z)?:return;val p100=project(x+1,y,z)?:return;val p010=project(x,y+1,z)?:return
            val p110=project(x+1,y+1,z)?:return;val p001=project(x,y,z+1)?:return;val p101=project(x+1,y,z+1)?:return
            val p011=project(x,y+1,z+1)?:return;val p111=project(x+1,y+1,z+1)?:return

            if (isGhost) {
                // Ghost: visible wireframe + tinted fill
                val ghostColor = color.copy(alpha = 0.25f)
                val ghostEdge = color.copy(alpha = 0.5f)
                val mx=x+0.5f;val my=y+0.5f;val mz=z+0.5f
                faces.add(Face(listOf(p001,p101,p111,p011), ghostColor, ghostEdge, false, depth(mx,my,z+1)))
                faces.add(Face(listOf(p000,p100,p101,p001), ghostColor.copy(alpha=0.15f), ghostEdge, false, depth(mx,y,mz)))
                faces.add(Face(listOf(p000,p010,p011,p001), ghostColor.copy(alpha=0.15f), ghostEdge, false, depth(x,my,mz)))
                faces.add(Face(listOf(p100,p110,p111,p101), ghostColor.copy(alpha=0.15f), ghostEdge, false, depth(x+1,my,mz)))
                return
            }

            // Clearing animation: flash white then fade out
            val clearAlpha = if (clearing > 0f) {
                if (clearing < 0.3f) a  // flash white
                else a * (1f - (clearing - 0.3f) / 0.7f) // fade out
            } else a
            val clearFlash = clearing > 0f && clearing < 0.3f

            val baseColor = if (clearFlash) brighten(color, 1.8f) else color

            // Material-based shading — each material has different look
            val (topMul, frontMul, backMul, leftMul, rightMul, botMul, edgeAlpha, shineAlpha) = when (material) {
                PieceMaterial.STONE -> MaterialParams(1.05f, 0.55f, 0.38f, 0.42f, 0.65f, 0.15f, 0.03f, 0.05f)
                PieceMaterial.GRANITE -> MaterialParams(0.95f, 0.45f, 0.32f, 0.38f, 0.55f, 0.12f, 0.02f, 0.03f)
                PieceMaterial.GLASS -> MaterialParams(1.3f, 0.85f, 0.75f, 0.8f, 0.9f, 0.6f, 0.25f, 0.4f)
                PieceMaterial.CRYSTAL -> MaterialParams(1.5f, 0.95f, 0.85f, 0.9f, 1.0f, 0.5f, 0.3f, 0.5f)
                else -> MaterialParams(1.2f, 0.62f, 0.42f, 0.48f, 0.72f, 0.18f, 0.06f, 0.22f) // CLASSIC
            }

            // Glass and Crystal have reduced base alpha for transparency effect
            val matAlpha = when (material) {
                PieceMaterial.GLASS -> 0.55f
                PieceMaterial.CRYSTAL -> 0.45f
                else -> 1f
            }

            val ca = clearAlpha * matAlpha
            val topC = brighten(baseColor, topMul).copy(ca)
            val botC = darken(baseColor, botMul).copy(ca)
            val frontC = darken(baseColor, frontMul).copy(ca)
            val backC = darken(baseColor, backMul).copy(ca)
            val leftC = darken(baseColor, leftMul).copy(ca)
            val rightC = darken(baseColor, rightMul).copy(ca)
            val edgeC = when (material) {
                PieceMaterial.GLASS -> Color.White.copy(0.4f * clearAlpha)
                PieceMaterial.CRYSTAL -> Color.White.copy(0.5f * clearAlpha)
                PieceMaterial.STONE, PieceMaterial.GRANITE -> Color.Black.copy(0.15f * clearAlpha)
                else -> if(highlight) Color.White.copy(0.4f*clearAlpha) else Color.White.copy(edgeAlpha*clearAlpha)
            }
            val mx=x+0.5f;val my=y+0.5f;val mz=z+0.5f

            faces.add(Face(listOf(p001,p101,p111,p011),topC,edgeC,highlight,depth(mx,my,z+1)))
            faces.add(Face(listOf(p000,p100,p110,p010),botC,null,false,depth(mx,my,z)))
            faces.add(Face(listOf(p000,p100,p101,p001),frontC,edgeC,false,depth(mx,y,mz)))
            faces.add(Face(listOf(p010,p110,p111,p011),backC,null,false,depth(mx,y+1,mz)))
            faces.add(Face(listOf(p000,p010,p011,p001),leftC,edgeC,false,depth(x,my,mz)))
            faces.add(Face(listOf(p100,p110,p111,p101),rightC,edgeC,false,depth(x+1,my,mz)))

            // Extra material effects — visible patterns
            if (material == PieceMaterial.GLASS || material == PieceMaterial.CRYSTAL) {
                // Bright specular reflection stripe across top face
                val p0=p001;val p1=p101;val p2=p111;val p3=p011
                val t1=0.1f;val t2=0.3f
                val s0=Offset(p0.x+(p3.x-p0.x)*t1,p0.y+(p3.y-p0.y)*t1)
                val s1=Offset(p1.x+(p2.x-p1.x)*t1,p1.y+(p2.y-p1.y)*t1)
                val s2=Offset(p1.x+(p2.x-p1.x)*t2,p1.y+(p2.y-p1.y)*t2)
                val s3=Offset(p0.x+(p3.x-p0.x)*t2,p0.y+(p3.y-p0.y)*t2)
                faces.add(Face(listOf(s0,s1,s2,s3), Color.White.copy(shineAlpha * clearAlpha), null, false, depth(mx,my,z+1)+0.01f))
                // Side reflection line
                val sr0=Offset(p001.x+(p011.x-p001.x)*0.15f, p001.y+(p011.y-p001.y)*0.15f)
                val sr1=Offset(p000.x+(p010.x-p000.x)*0.15f, p000.y+(p010.y-p000.y)*0.15f)
                faces.add(Face(listOf(sr0,Offset(sr0.x+2f,sr0.y),Offset(sr1.x+2f,sr1.y),sr1),
                    Color.White.copy(shineAlpha * 0.5f * clearAlpha), null, false, depth(x,my,mz)+0.01f))
            }
            if (material == PieceMaterial.STONE || material == PieceMaterial.GRANITE) {
                // Rough surface — dark grain spots on top face
                val topD = depth(mx,my,z+1)+0.01f
                val grainCount = if (material == PieceMaterial.GRANITE) 5 else 3
                for (i in 0 until grainCount) {
                    val t1 = 0.1f + i * (0.8f / grainCount)
                    val t2 = 0.2f + (i * 0.37f) % 0.6f
                    val gx = p001.x + (p111.x - p001.x) * t1
                    val gy = p001.y + (p111.y - p001.y) * t2
                    val grainColor = if (material == PieceMaterial.GRANITE) Color.Black.copy(0.18f * clearAlpha) else Color.Black.copy(0.12f * clearAlpha)
                    faces.add(Face(listOf(
                        Offset(gx-2f,gy-1f), Offset(gx+2f,gy-1f), Offset(gx+2f,gy+1f), Offset(gx-2f,gy+1f)
                    ), grainColor, null, false, topD))
                }
                // Granite gets fine horizontal lines across front face
                if (material == PieceMaterial.GRANITE) {
                    for (i in 0..2) {
                        val t = 0.25f + i * 0.25f
                        val l0 = Offset(p000.x + (p001.x - p000.x) * t, p000.y + (p001.y - p000.y) * t)
                        val l1 = Offset(p100.x + (p101.x - p100.x) * t, p100.y + (p101.y - p100.y) * t)
                        faces.add(Face(listOf(l0, l1, Offset(l1.x, l1.y + 1f), Offset(l0.x, l0.y + 1f)),
                            Color.Black.copy(0.08f * clearAlpha), null, false, depth(mx, y, mz) + 0.01f))
                    }
                }
            }
        }

        // Board blocks
        if (state.board.isNotEmpty()) {
            for (ey in 0 until Tetris3DGame.BOARD_H) {
                val clearing = if (ey in state.clearingLayers) clearAnimProgress else 0f
                for (ez in 0 until Tetris3DGame.BOARD_D) { for (ex in 0 until Tetris3DGame.BOARD_W) {
                    if (ey<state.board.size&&ez<state.board[ey].size&&ex<state.board[ey][ez].size) {
                        val c=state.board[ey][ez][ex]; if (c>0) addCube(ex,ez,ey,pieceColor(c,themeColor),1f,clearing=clearing)
                    }
                }}
            }
        }

        // Ghost piece — visible wireframe
        val piece = state.currentPiece
        if (piece!=null && showGhost && state.ghostY<piece.y) {
            val ghostColor = pieceColor(piece.type.colorIndex, themeColor)
            for (b in piece.blocks) { val gdx=piece.x+b.x;val gdy=piece.z+b.z;val gdz=state.ghostY+b.y
                if(gdz>=0) addCube(gdx,gdy,gdz,ghostColor,0.25f,isGhost=true) }
        }

        // Current piece
        if (piece!=null) {
            for (b in piece.blocks) { val pdx=piece.x+b.x;val pdy=piece.z+b.z;val pdz=piece.y+b.y
                if(pdz>=0) addCube(pdx,pdy,pdz,pieceColor(piece.type.colorIndex,themeColor),1f,true) }
        }

        // Sort & draw
        faces.sortByDescending { it.d }
        faces.forEach { face ->
            val path = Path().apply { moveTo(face.pts[0].x,face.pts[0].y); for(i in 1 until face.pts.size) lineTo(face.pts[i].x,face.pts[i].y); close() }
            drawPath(path, face.fill, style = Fill)
            if (face.shine) {
                val p0=face.pts[0];val p1=face.pts[1];val p2=face.pts[2];val p3=face.pts[3]
                val hlPath = Path().apply {
                    moveTo(p0.x+(p1.x-p0.x)*0.05f, p0.y+(p1.y-p0.y)*0.05f)
                    lineTo(p0.x+(p1.x-p0.x)*0.4f, p0.y+(p1.y-p0.y)*0.4f)
                    lineTo(p0.x+(p2.x-p0.x)*0.35f, p0.y+(p2.y-p0.y)*0.35f)
                    lineTo(p0.x+(p3.x-p0.x)*0.4f, p0.y+(p3.y-p0.y)*0.4f); close()
                }
                drawPath(hlPath, Color.White.copy(0.22f), style = Fill)
            }
            if (face.edge != null) {
                drawPath(path, face.edge, style = Stroke(0.8f))
                drawPath(path, Color.Black.copy(0.08f), style = Stroke(0.3f))
            }

            // Stone/granite texture: noise-like dark spots on top faces
            if ((material == PieceMaterial.STONE || material == PieceMaterial.GRANITE) && face.shine) {
                for (i in 0 until 3) {
                    val t1 = 0.15f + i * 0.28f; val t2 = 0.2f + i * 0.2f
                    val sx = face.pts[0].x + (face.pts[2].x - face.pts[0].x) * t1
                    val sy = face.pts[0].y + (face.pts[2].y - face.pts[0].y) * t2
                    drawCircle(Color.Black.copy(0.12f), 1.5f, Offset(sx, sy))
                }
            }
        }
    }
}

// Material shading parameters
private data class MaterialParams(
    val topMul: Float, val frontMul: Float, val backMul: Float,
    val leftMul: Float, val rightMul: Float, val botMul: Float,
    val edgeAlpha: Float, val shineAlpha: Float
)

// ==================== STAR WARS ====================
@Composable
private fun StarWarsBoard(state: Game3DState, modifier: Modifier, showGhost: Boolean, themeColor: Color) {
    Canvas(modifier) {
        val w=size.width;val h=size.height;val bw=Tetris3DGame.BOARD_W;val bh=Tetris3DGame.BOARD_H
        val vpX=w/2f;val bottomY=h*0.97f;val topY=h*0.05f;val bottomHalfW=w*0.45f;val topHalfW=w*0.12f
        fun rowParams(row:Int):RowParams{val t=row.toFloat()/bh;val sy=bottomY+(topY-bottomY)*t;val hw=bottomHalfW+(topHalfW-bottomHalfW)*t;return RowParams(sy,vpX-hw,hw*2f/bw,(bottomY-topY)/bh*(1f-t*0.35f),1f-t*0.5f)}
        for(row in 0..bh){val t=row.toFloat()/bh;val sy=bottomY+(topY-bottomY)*t;val hw=bottomHalfW+(topHalfW-bottomHalfW)*t;drawLine(themeColor.copy(0.06f*(1f-t*0.6f)),Offset(vpX-hw,sy),Offset(vpX+hw,sy),0.5f)}
        for(col in 0..bw){val bxp=vpX-bottomHalfW+col*(bottomHalfW*2/bw);val txp=vpX-topHalfW+col*(topHalfW*2/bw);drawLine(themeColor.copy(0.04f),Offset(bxp,bottomY),Offset(txp,topY),0.5f)}
        for(row in bh-1 downTo 0){val rp=rowParams(row);for(col in 0 until bw){var ci=0;if(state.board.isNotEmpty()&&row<state.board.size){for(z in 0 until Tetris3DGame.BOARD_D){if(z<state.board[row].size&&col<state.board[row][z].size){val c=state.board[row][z][col];if(c>0){ci=c;break}}}};if(ci>0)drawSWBlock(rp,col,pieceColor(ci,themeColor),rp.alpha)}}
        val piece=state.currentPiece
        if(piece!=null&&showGhost&&state.ghostY<piece.y){for(b in piece.blocks){val row=state.ghostY+b.y;val col=piece.x+b.x;if(row in 0 until bh&&col in 0 until bw)drawSWBlock(rowParams(row),col,themeColor,0.15f*rowParams(row).alpha)}}
        if(piece!=null){for(b in piece.blocks){val row=piece.y+b.y;val col=piece.x+b.x;if(row in 0 until bh&&col in 0 until bw)drawSWBlock(rowParams(row),col,pieceColor(piece.type.colorIndex,themeColor),rowParams(row).alpha)}}
    }
}
private data class RowParams(val y:Float,val leftX:Float,val cellW:Float,val cellH:Float,val alpha:Float)
private fun DrawScope.drawSWBlock(rp:RowParams,col:Int,color:Color,alpha:Float){
    val x=rp.leftX+col*rp.cellW;val y=rp.y-rp.cellH;val cw=rp.cellW;val ch=rp.cellH;val a=alpha.coerceIn(0f,1f);val d=ch*0.18f
    drawRect(color.copy(a),Offset(x+1,y),Size(cw-2,ch-1))
    drawPath(Path().apply{moveTo(x+1,y);lineTo(x+cw-1,y);lineTo(x+cw-1-d,y-d);lineTo(x+1+d,y-d);close()},brighten(color,1.1f).copy(a*0.7f))
    drawPath(Path().apply{moveTo(x+cw-1,y);lineTo(x+cw-1,y+ch-1);lineTo(x+cw-1-d,y+ch-1-d);lineTo(x+cw-1-d,y-d);close()},darken(color,0.5f).copy(a*0.6f))
    drawRect(Color.White.copy(0.18f*a),Offset(x+2,y+1),Size(cw*0.25f,2f))
}

private fun pieceColor(idx:Int,themeColor:Color):Color=when(idx){1->Color(0xFF00E5FF);2->Color(0xFFFFD600);3->Color(0xFFAA00FF);4->Color(0xFF00E676);5->Color(0xFFFF6D00);6->Color(0xFFFF1744);7->Color(0xFF2979FF);8->Color(0xFFFF4081);else->themeColor}
private fun darken(c:Color,f:Float)=Color(c.red*f,c.green*f,c.blue*f,c.alpha)
private fun brighten(c:Color,f:Float)=Color(minOf(c.red*f,1f),minOf(c.green*f,1f),minOf(c.blue*f,1f),c.alpha)
