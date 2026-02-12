package com.brickgame.tetris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.brickgame.tetris.game.PieceState
import com.brickgame.tetris.game.TetrisGame
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.LocalGameTheme

// Standard Tetris piece colors
val PIECE_COLORS = listOf(
    Color.Transparent,         // 0 = empty
    Color(0xFF00D4FF),         // 1 = I — Cyan
    Color(0xFFF4D03F),         // 2 = O — Yellow
    Color(0xFFAA44FF),         // 3 = T — Purple
    Color(0xFF44DD44),         // 4 = S — Green
    Color(0xFFFF4444),         // 5 = Z — Red
    Color(0xFF4488FF),         // 6 = J — Blue
    Color(0xFFFF8800),         // 7 = L — Orange
)

@Composable
fun GameBoard(
    board: List<List<Int>>,
    modifier: Modifier = Modifier,
    currentPiece: PieceState? = null,
    ghostY: Int = 0,
    showGhost: Boolean = true,
    clearingLines: List<Int> = emptyList(),
    animationStyle: AnimationStyle = AnimationStyle.MODERN,
    animationDuration: Float = 0.5f,
    multiColor: Boolean = false,
    classicLCD: Boolean = false
) {
    val theme = LocalGameTheme.current
    val clearProgress = remember { Animatable(0f) }

    LaunchedEffect(clearingLines) {
        if (clearingLines.isNotEmpty()) {
            clearProgress.snapTo(0f)
            clearProgress.animateTo(1f, tween((animationDuration * 500).toInt().coerceAtLeast(150), easing = LinearEasing))
        } else clearProgress.snapTo(0f)
    }

    val progress = clearProgress.value
    val isClearing = clearingLines.isNotEmpty()
    val isTetris = clearingLines.size >= 4

    BoxWithConstraints(
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        val pixelSize = minOf(maxWidth / TetrisGame.BOARD_WIDTH, maxHeight / TetrisGame.BOARD_HEIGHT)
        val boardWidth = pixelSize * TetrisGame.BOARD_WIDTH
        val boardHeight = pixelSize * TetrisGame.BOARD_HEIGHT

        Canvas(modifier = Modifier.width(boardWidth).height(boardHeight)) {
            val cellSize = size.width / TetrisGame.BOARD_WIDTH
            val gap = cellSize * 0.06f
            val corner = cellSize * 0.15f

            // Draw board cells
            for (y in 0 until TetrisGame.BOARD_HEIGHT) {
                for (x in 0 until TetrisGame.BOARD_WIDTH) {
                    val cellValue = board[y][x]
                    val isClearingRow = clearingLines.contains(y)
                    val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                    val cs = Size(cellSize - gap * 2, cellSize - gap * 2)

                    if (classicLCD) {
                        // Classic LCD: each cell is a recessed square with beveled inner square
                        drawLCDCell(offset, cs, cellValue > 0, theme.pixelOff, theme.pixelOn)
                    } else {
                        drawRoundRect(theme.pixelOff, offset, cs, CornerRadius(corner))

                        if (cellValue > 0) {
                            val pieceColor = if (multiColor && cellValue in 1..7) PIECE_COLORS[cellValue] else theme.pixelOn

                            if (isClearingRow && isClearing && animationStyle != AnimationStyle.NONE) {
                                if (isTetris && animationStyle == AnimationStyle.FLASHY) {
                                    drawTetrisExplosion(progress, x, y, cellSize, gap, corner, pieceColor)
                                } else {
                                    val (color, scale) = clearingEffect(animationStyle, progress, x, y, pieceColor, clearingLines.size)
                                    val ss = Size(cs.width * scale, cs.height * scale)
                                    val so = Offset(offset.x + (cs.width - ss.width) / 2, offset.y + (cs.height - ss.height) / 2)
                                    drawRoundRect(color, so, ss, CornerRadius(corner * scale))
                                }
                            } else {
                                drawRoundRect(pieceColor, offset, cs, CornerRadius(corner))
                                if (multiColor && cellValue in 1..7) {
                                    drawRoundRect(Color.Black.copy(alpha = 0.35f), offset, cs, CornerRadius(corner), style = Stroke(gap * 1.2f))
                                    drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(offset.x, offset.y + cs.height * 0.65f), Size(cs.width, cs.height * 0.35f), CornerRadius(corner))
                                    drawRoundRect(Color.White.copy(alpha = 0.2f), offset, Size(cs.width, cs.height * 0.3f), CornerRadius(corner))
                                }
                            }
                        }
                    }
                }
            }

            // Current piece
            if (currentPiece != null) {
                val pColor = if (multiColor) PIECE_COLORS.getOrElse(currentPiece.type.ordinal + 1) { theme.pixelOn } else theme.pixelOn
                for (py in currentPiece.shape.indices) for (px in currentPiece.shape[py].indices) {
                    if (currentPiece.shape[py][px] > 0) {
                        val bx = currentPiece.position.x + px; val by = currentPiece.position.y + py
                        if (bx in 0 until TetrisGame.BOARD_WIDTH && by in 0 until TetrisGame.BOARD_HEIGHT) {
                            val offset = Offset(bx * cellSize + gap, by * cellSize + gap)
                            val cs = Size(cellSize - gap * 2, cellSize - gap * 2)
                            if (classicLCD) {
                                drawLCDCell(offset, cs, true, theme.pixelOff, theme.pixelOn)
                            } else {
                                drawRoundRect(pColor, offset, cs, CornerRadius(corner))
                                if (multiColor) {
                                    drawRoundRect(Color.Black.copy(alpha = 0.35f), offset, cs, CornerRadius(corner), style = Stroke(gap * 1.2f))
                                    drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(offset.x, offset.y + cs.height * 0.65f), Size(cs.width, cs.height * 0.35f), CornerRadius(corner))
                                    drawRoundRect(Color.White.copy(alpha = 0.2f), offset, Size(cs.width, cs.height * 0.3f), CornerRadius(corner))
                                }
                            }
                        }
                    }
                }
            }

            // Ghost
            if (showGhost && currentPiece != null && ghostY > currentPiece.position.y) {
                val gc = if (multiColor) PIECE_COLORS.getOrElse(currentPiece.type.ordinal + 1) { theme.pixelOn } else theme.pixelOn
                drawGhost(currentPiece, ghostY, cellSize, gap, corner, gc)
            }
        }
    }
}

/** Draw a single cell in authentic Brick Game LCD style.
 *  Real hardware has 3 concentric layers:
 *  ON cell:  thick black border → light/cream gap → solid black center square
 *  OFF cell: faint border → light gap → subtle darker center indent */
private fun DrawScope.drawLCDCell(offset: Offset, cellSize: Size, isOn: Boolean, offColor: Color, onColor: Color) {
    val w = cellSize.width; val h = cellSize.height
    // LCD background color (light sage green)
    val lcdLight = Color(0xFFC2CCAE)

    if (isOn) {
        // Layer 1: Black outer border (fills entire cell)
        drawRect(Color(0xFF2A2E22), offset, cellSize)
        // Layer 2: Light gap ring — inset from border
        val borderW = w * 0.14f
        val gapOff = Offset(offset.x + borderW, offset.y + borderW)
        val gapSize = Size(w - borderW * 2, h - borderW * 2)
        drawRect(lcdLight, gapOff, gapSize)
        // Layer 3: Black center square
        val centerInset = w * 0.28f
        val centerOff = Offset(offset.x + centerInset, offset.y + centerInset)
        val centerSize = Size(w - centerInset * 2, h - centerInset * 2)
        drawRect(Color(0xFF2A2E22), centerOff, centerSize)
    } else {
        // Layer 1: Faint outer border
        drawRect(offColor, offset, cellSize)
        val borderW = w * 0.08f
        // Top and left edges — slightly darker (shadow)
        drawRect(Color.Black.copy(alpha = 0.07f), offset, Size(w, borderW))
        drawRect(Color.Black.copy(alpha = 0.07f), offset, Size(borderW, h))
        // Bottom and right edges — slightly lighter (highlight)
        drawRect(Color.White.copy(alpha = 0.06f), Offset(offset.x, offset.y + h - borderW), Size(w, borderW))
        drawRect(Color.White.copy(alpha = 0.06f), Offset(offset.x + w - borderW, offset.y), Size(borderW, h))
        // Layer 2: Light gap (background shows through)
        val gapOff = Offset(offset.x + borderW, offset.y + borderW)
        val gapSize = Size(w - borderW * 2, h - borderW * 2)
        drawRect(offColor.lighten(0.03f), gapOff, gapSize)
        // Layer 3: Subtle center indent
        val centerInset = w * 0.28f
        val centerOff = Offset(offset.x + centerInset, offset.y + centerInset)
        val centerSize = Size(w - centerInset * 2, h - centerInset * 2)
        drawRect(offColor.darken(0.04f), centerOff, centerSize)
        // Inner indent shadow edges
        drawRect(Color.Black.copy(alpha = 0.04f), centerOff, Size(centerSize.width, 1f))
        drawRect(Color.Black.copy(alpha = 0.04f), centerOff, Size(1f, centerSize.height))
    }
}

/** Darken a color by a factor */
private fun Color.darken(f: Float) = Color(
    (red * (1 - f)).coerceIn(0f, 1f),
    (green * (1 - f)).coerceIn(0f, 1f),
    (blue * (1 - f)).coerceIn(0f, 1f),
    alpha
)

/** Lighten a color by a factor */
private fun Color.lighten(f: Float) = Color(
    (red + (1f - red) * f).coerceIn(0f, 1f),
    (green + (1f - green) * f).coerceIn(0f, 1f),
    (blue + (1f - blue) * f).coerceIn(0f, 1f),
    alpha
)

private fun DrawScope.drawTetrisExplosion(
    progress: Float, x: Int, y: Int, cellSize: Float,
    gap: Float, corner: Float, baseColor: Color
) {
    val offset = Offset(x * cellSize + gap, y * cellSize + gap)
    val cs = Size(cellSize - gap * 2, cellSize - gap * 2)
    val cx = offset.x + cs.width / 2; val cy = offset.y + cs.height / 2

    if (progress < 0.15f) {
        // Phase 1: Bright flash — cell turns white with glow
        val flashP = progress / 0.15f
        val glow = 1f + flashP * 0.6f
        val gs = Size(cs.width * glow, cs.height * glow)
        val go = Offset(cx - gs.width / 2, cy - gs.height / 2)
        // White-hot center
        drawRoundRect(Color.White.copy(alpha = 1f - flashP * 0.3f), go, gs, CornerRadius(corner * glow))
        // Color glow ring
        drawRoundRect(baseColor.copy(alpha = 0.6f), Offset(go.x - 2, go.y - 2), Size(gs.width + 4, gs.height + 4), CornerRadius(corner * glow + 2), style = Stroke(3f))
    } else {
        // Phase 2: Multiple particles explode outward with gravity
        val t = (progress - 0.15f) / 0.85f
        val gravity = cellSize * 8f * t * t // accelerating downward
        val fadeStart = 0.4f

        // 4 particles per cell — each with unique angle and speed
        for (i in 0..3) {
            val seed = (x * 37 + y * 53 + i * 97).toFloat()
            val angle = seed * 0.7f + i * 1.57f // spread evenly
            val speed = cellSize * (1.5f + (seed % 3f) * 0.8f)
            val px = cx + kotlin.math.cos(angle) * speed * t
            val py = cy + kotlin.math.sin(angle) * speed * t * 0.6f - cellSize * 1.5f * t + gravity
            val alpha = if (t > fadeStart) ((1f - (t - fadeStart) / (1f - fadeStart))).coerceIn(0f, 1f) else 1f
            val pSize = cs.width * (0.5f - i * 0.08f) * (1f - t * 0.6f)

            if (pSize > 0 && alpha > 0) {
                // Particle
                drawRoundRect(
                    baseColor.copy(alpha = alpha * 0.9f),
                    Offset(px - pSize / 2, py - pSize / 2),
                    Size(pSize, pSize),
                    CornerRadius(pSize / 3)
                )
                // Hot white core
                val coreSize = pSize * 0.4f
                drawCircle(Color.White.copy(alpha = alpha * 0.5f), coreSize / 2, Offset(px, py))
            }
        }

        // 2 extra small spark particles
        for (i in 0..1) {
            val seed = (x * 19 + y * 71 + i * 143).toFloat()
            val angle = seed * 1.3f
            val speed = cellSize * (2.5f + (seed % 2f))
            val px = cx + kotlin.math.cos(angle) * speed * t
            val py = cy + kotlin.math.sin(angle) * speed * t * 0.4f - cellSize * 2f * t + gravity * 1.3f
            val alpha = if (t > 0.3f) ((1f - (t - 0.3f) / 0.7f)).coerceIn(0f, 1f) else 1f
            val sparkSize = cs.width * 0.18f * (1f - t)

            if (sparkSize > 0 && alpha > 0) {
                drawCircle(Color.White.copy(alpha = alpha * 0.7f), sparkSize, Offset(px, py))
            }
        }
    }
}

private fun clearingEffect(style: AnimationStyle, progress: Float, x: Int, y: Int, baseColor: Color, lineCount: Int): Pair<Color, Float> = when (style) {
    AnimationStyle.NONE -> baseColor to 1f
    AnimationStyle.RETRO -> {
        val blink = if ((progress * 6).toInt() % 2 == 0) Color.White else baseColor
        val fade = if (progress > 0.7f) 1f - ((progress - 0.7f) / 0.3f) * 0.3f else 1f
        blink.copy(alpha = fade) to 1f
    }
    AnimationStyle.MODERN -> {
        val c = Color(baseColor.red + (1f - baseColor.red) * progress, baseColor.green + (1f - baseColor.green) * progress, baseColor.blue + (1f - baseColor.blue) * progress, 1f - progress * 0.5f)
        c to (1f - progress * 0.3f)
    }
    AnimationStyle.FLASHY -> {
        val colors = listOf(Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFF8B00FF), Color(0xFFFF00FF))
        val idx = ((x + progress * 16).toInt() % colors.size)
        val alpha = if (progress > 0.6f) 1f - ((progress - 0.6f) / 0.4f) else 1f
        colors[idx].copy(alpha = alpha) to (1f + kotlin.math.sin(progress * 12.56f) * 0.1f)
    }
}

private fun DrawScope.drawGhost(piece: PieceState, ghostY: Int, cellSize: Float, gap: Float, corner: Float, baseColor: Color) {
    val gc = baseColor.copy(alpha = 0.15f); val oc = baseColor.copy(alpha = 0.35f)
    for (py in piece.shape.indices) for (px in piece.shape[py].indices) {
        if (piece.shape[py][px] > 0) {
            val bx = piece.position.x + px; val by = ghostY + py
            if (bx in 0 until TetrisGame.BOARD_WIDTH && by in 0 until TetrisGame.BOARD_HEIGHT) {
                val o = Offset(bx * cellSize + gap, by * cellSize + gap); val s = Size(cellSize - gap * 2, cellSize - gap * 2)
                drawRoundRect(gc, o, s, CornerRadius(corner)); drawRoundRect(oc, o, s, CornerRadius(corner), style = Stroke(gap * 0.8f))
            }
        }
    }
}

// ===== Piece Previews =====
@Composable
fun NextPiecePreview(shape: List<List<Int>>?, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val theme = LocalGameTheme.current
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(theme.pixelOff.copy(alpha = 0.5f)).padding(4.dp)) {
        if (shape != null) Canvas(Modifier.fillMaxSize()) {
            val rows = shape.size; val cols = shape.maxOfOrNull { it.size } ?: 0; if (rows == 0 || cols == 0) return@Canvas
            val cs = minOf(size.width / cols, size.height / rows); val ox = (size.width - cs * cols) / 2; val oy = (size.height - cs * rows) / 2; val g = cs * 0.1f; val c = cs * 0.2f
            for (y in shape.indices) for (x in shape[y].indices) if (shape[y][x] > 0) drawRoundRect(theme.pixelOn.copy(alpha = alpha), Offset(ox + x * cs + g, oy + y * cs + g), Size(cs - g * 2, cs - g * 2), CornerRadius(c))
        }
    }
}

@Composable
fun HoldPiecePreview(shape: List<List<Int>>?, isUsed: Boolean = false, modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current; val a = if (isUsed) 0.3f else 1f
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(theme.pixelOff.copy(alpha = if (isUsed) 0.3f else 0.5f)).padding(4.dp)) {
        if (shape != null) Canvas(Modifier.fillMaxSize()) {
            val rows = shape.size; val cols = shape.maxOfOrNull { it.size } ?: 0; if (rows == 0 || cols == 0) return@Canvas
            val cs = minOf(size.width / cols, size.height / rows); val ox = (size.width - cs * cols) / 2; val oy = (size.height - cs * rows) / 2; val g = cs * 0.1f; val c = cs * 0.2f
            for (y in shape.indices) for (x in shape[y].indices) if (shape[y][x] > 0) drawRoundRect(theme.pixelOn.copy(alpha = a), Offset(ox + x * cs + g, oy + y * cs + g), Size(cs - g * 2, cs - g * 2), CornerRadius(c))
        }
    }
}
