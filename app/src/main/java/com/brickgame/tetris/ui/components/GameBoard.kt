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
    classicLCD: Boolean = false,
    hardDropTrail: List<Triple<Int, Int, Int>> = emptyList(),
    lockEvent: Int = 0,
    pieceMaterial: String = "CLASSIC",
    highContrast: Boolean = false,
    boardOpacity: Float = 1f
) {
    val theme = LocalGameTheme.current
    val clearProgress = remember { Animatable(0f) }

    LaunchedEffect(clearingLines) {
        if (clearingLines.isNotEmpty()) {
            clearProgress.snapTo(0f)
            clearProgress.animateTo(1f, tween((animationDuration * 500).toInt().coerceAtLeast(150), easing = LinearEasing))
        } else clearProgress.snapTo(0f)
    }

    // Hard drop trail fade animation
    val trailProgress = remember { Animatable(0f) }
    LaunchedEffect(hardDropTrail) {
        if (hardDropTrail.isNotEmpty()) {
            trailProgress.snapTo(0f)
            trailProgress.animateTo(1f, tween(200, easing = LinearEasing))
        } else trailProgress.snapTo(0f)
    }

    // Piece lock flash animation
    val lockFlashProgress = remember { Animatable(0f) }
    LaunchedEffect(lockEvent) {
        if (lockEvent > 0) {
            lockFlashProgress.snapTo(0f)
            lockFlashProgress.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
        }
    }

    val progress = clearProgress.value
    val isClearing = clearingLines.isNotEmpty()
    val isTetris = clearingLines.size >= 4

    BoxWithConstraints(
        modifier = modifier.clip(RoundedCornerShape(6.dp))
            .background(theme.screenBackground.copy(alpha = theme.screenBackground.alpha * boardOpacity))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        val pixelSize = minOf(maxWidth / TetrisGame.BOARD_WIDTH, maxHeight / TetrisGame.BOARD_HEIGHT)
        val boardWidth = pixelSize * TetrisGame.BOARD_WIDTH
        val boardHeight = pixelSize * TetrisGame.BOARD_HEIGHT

        Canvas(modifier = Modifier.width(boardWidth).height(boardHeight)) {
            val cellSize = size.width / TetrisGame.BOARD_WIDTH
            val gap = cellSize * 0.06f
            val corner = cellSize * 0.15f
            // High contrast: boost grid visibility; apply boardOpacity to empty cells
            val rawEmpty = if (highContrast) theme.pixelOff.boostContrast(theme.screenBackground) else theme.pixelOff
            val emptyColor = rawEmpty.copy(alpha = rawEmpty.alpha * boardOpacity)

            // Draw board cells
            for (y in 0 until TetrisGame.BOARD_HEIGHT) {
                for (x in 0 until TetrisGame.BOARD_WIDTH) {
                    val cellValue = board[y][x]
                    val isClearingRow = clearingLines.contains(y)
                    val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                    val cs = Size(cellSize - gap * 2, cellSize - gap * 2)

                    if (classicLCD) {
                        // Classic LCD mode with authentic blink for line clears
                        if (isClearingRow && isClearing && animationStyle != AnimationStyle.NONE) {
                            // Authentic Brick Game blink: cells toggle ON/OFF rapidly (3-4 blinks)
                            val blinkOn = (progress * 8).toInt() % 2 == 0
                            drawLCDCell(offset, cs, blinkOn, theme.pixelOff, theme.pixelOn)
                        } else {
                            drawLCDCell(offset, cs, cellValue > 0, theme.pixelOff, theme.pixelOn)
                        }
                    } else {
                        drawRoundRect(emptyColor, offset, cs, CornerRadius(corner))

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
                                    drawPieceMaterial(offset, cs, corner, gap, pieceColor, pieceMaterial, isActive = false)
                                }
                            }
                        }
                    }
                }
            }

            // Current piece — with material rendering
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
                                    drawPieceMaterial(offset, cs, corner, gap, pColor, pieceMaterial, isActive = true)
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

            // Hard drop trail — vertical streaks fade out quickly
            if (hardDropTrail.isNotEmpty() && trailProgress.value < 1f && !classicLCD) {
                val trailAlpha = (1f - trailProgress.value) * 0.6f
                for ((col, startRow, endRow) in hardDropTrail) {
                    if (col in 0 until TetrisGame.BOARD_WIDTH) {
                        val topY = startRow * cellSize
                        val bottomY = endRow * cellSize
                        val trailHeight = (bottomY - topY) * (1f - trailProgress.value * 0.5f)
                        val x = col * cellSize + cellSize * 0.3f
                        val w = cellSize * 0.4f
                        // Gradient trail: brighter at bottom, fading at top
                        val color = if (multiColor && currentPiece != null)
                            PIECE_COLORS.getOrElse(currentPiece.type.ordinal + 1) { Color.White }
                        else Color.White
                        drawRect(
                            color.copy(alpha = trailAlpha * 0.3f),
                            Offset(x, topY + (bottomY - topY - trailHeight)),
                            Size(w, trailHeight)
                        )
                        // Bright tip at landing position
                        drawRoundRect(
                            color.copy(alpha = trailAlpha),
                            Offset(col * cellSize + gap, (endRow - 1) * cellSize + gap),
                            Size(cellSize - gap * 2, cellSize - gap * 2),
                            CornerRadius(corner)
                        )
                    }
                }
            }

            // Lock flash — brief glow on recently placed cells
            if (lockFlashProgress.value < 1f && lockFlashProgress.value > 0f && !classicLCD) {
                val flashAlpha = (1f - lockFlashProgress.value) * 0.35f
                // Flash the entire bottom area where pieces typically lock
                for (y in 0 until TetrisGame.BOARD_HEIGHT) {
                    for (x in 0 until TetrisGame.BOARD_WIDTH) {
                        if (board[y][x] > 0) {
                            val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                            val cs = Size(cellSize - gap * 2, cellSize - gap * 2)
                            drawRoundRect(
                                Color.White.copy(alpha = flashAlpha * (1f - y.toFloat() / TetrisGame.BOARD_HEIGHT)),
                                offset, cs, CornerRadius(corner)
                            )
                        }
                    }
                }
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

/** Draw material-specific effects on a piece cell.
 *  Called AFTER the base color rect is already drawn.
 *  isActive = true for the currently falling piece (slightly brighter effects). */
private fun DrawScope.drawPieceMaterial(
    offset: Offset, cs: Size, corner: Float, gap: Float,
    baseColor: Color, material: String, isActive: Boolean
) {
    val glowAlpha = if (isActive) 0.15f else 0.08f
    when (material) {
        "STONE" -> {
            // Matte, rough look — strong dark border, no glossy highlight, subtle texture lines
            drawRoundRect(Color.Black.copy(alpha = 0.5f), offset, cs, CornerRadius(corner), style = Stroke(gap * 1.5f))
            drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(offset.x, offset.y + cs.height * 0.6f), Size(cs.width, cs.height * 0.4f), CornerRadius(corner))
            // Subtle horizontal texture lines
            val lineCount = 3
            for (i in 1..lineCount) {
                val ly = offset.y + cs.height * i / (lineCount + 1)
                drawRect(Color.Black.copy(alpha = 0.08f), Offset(offset.x + gap, ly), Size(cs.width - gap * 2, 1f))
            }
        }
        "GRANITE" -> {
            // Darker, more contrast — thicker border, darker shadow, subtle speckle effect
            drawRoundRect(Color.Black.copy(alpha = 0.6f), offset, cs, CornerRadius(corner), style = Stroke(gap * 1.8f))
            drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(offset.x, offset.y + cs.height * 0.55f), Size(cs.width, cs.height * 0.45f), CornerRadius(corner))
            // Very faint top edge light
            drawRoundRect(Color.White.copy(alpha = 0.08f), offset, Size(cs.width, cs.height * 0.15f), CornerRadius(corner))
        }
        "GLASS" -> {
            // Glossy marble — transparent edges, strong specular, inner glow
            val glowSize = gap * 1.5f
            drawRoundRect(baseColor.copy(alpha = glowAlpha),
                Offset(offset.x - glowSize, offset.y - glowSize),
                Size(cs.width + glowSize * 2, cs.height + glowSize * 2),
                CornerRadius(corner + glowSize))
            // Thin elegant border
            drawRoundRect(Color.White.copy(alpha = 0.25f), offset, cs, CornerRadius(corner), style = Stroke(gap * 0.6f))
            // Strong specular highlight — top third
            drawRoundRect(Color.White.copy(alpha = 0.35f), offset, Size(cs.width, cs.height * 0.35f), CornerRadius(corner))
            // Subtle bottom reflection
            drawRoundRect(Color.White.copy(alpha = 0.08f), Offset(offset.x, offset.y + cs.height * 0.85f), Size(cs.width, cs.height * 0.15f), CornerRadius(corner))
            // Inner shadow for depth
            drawRoundRect(Color.Black.copy(alpha = 0.1f), Offset(offset.x, offset.y + cs.height * 0.5f), Size(cs.width, cs.height * 0.5f), CornerRadius(corner))
        }
        "CRYSTAL" -> {
            // Maximum shine — prismatic edge glow, bright specular, diamond-like
            val glowSize = gap * 2.5f
            drawRoundRect(baseColor.copy(alpha = glowAlpha * 1.5f),
                Offset(offset.x - glowSize, offset.y - glowSize),
                Size(cs.width + glowSize * 2, cs.height + glowSize * 2),
                CornerRadius(corner + glowSize))
            // Prismatic edge — slightly shifted color on border
            drawRoundRect(Color.White.copy(alpha = 0.3f), offset, cs, CornerRadius(corner), style = Stroke(gap * 0.8f))
            // Bright specular — top 40%
            drawRoundRect(Color.White.copy(alpha = 0.45f), offset, Size(cs.width, cs.height * 0.4f), CornerRadius(corner))
            // Small bright center highlight (diamond facet)
            val facetSize = cs.width * 0.3f
            drawRoundRect(Color.White.copy(alpha = 0.25f),
                Offset(offset.x + cs.width * 0.35f, offset.y + cs.height * 0.15f),
                Size(facetSize, facetSize * 0.6f), CornerRadius(corner * 0.5f))
            // Bottom edge reflection
            drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(offset.x, offset.y + cs.height * 0.8f), Size(cs.width, cs.height * 0.2f), CornerRadius(corner))
        }
        else -> {
            // CLASSIC — standard 3D bevel
            val glowSize = gap * 1.2f
            drawRoundRect(baseColor.copy(alpha = glowAlpha),
                Offset(offset.x - glowSize, offset.y - glowSize),
                Size(cs.width + glowSize * 2, cs.height + glowSize * 2),
                CornerRadius(corner + glowSize))
            drawRoundRect(Color.Black.copy(alpha = 0.35f), offset, cs, CornerRadius(corner), style = Stroke(gap * 1.2f))
            drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(offset.x, offset.y + cs.height * 0.65f), Size(cs.width, cs.height * 0.35f), CornerRadius(corner))
            drawRoundRect(Color.White.copy(alpha = if (isActive) 0.25f else 0.2f), offset, Size(cs.width, cs.height * 0.3f), CornerRadius(corner))
        }
    }
}

/** Lighten a color by a factor */
private fun Color.lighten(f: Float) = Color(
    (red + (1f - red) * f).coerceIn(0f, 1f),
    (green + (1f - green) * f).coerceIn(0f, 1f),
    (blue + (1f - blue) * f).coerceIn(0f, 1f),
    alpha
)

/** Boost contrast of a color relative to a background */
private fun Color.boostContrast(bg: Color): Color {
    val bgLum = bg.red * 0.299f + bg.green * 0.587f + bg.blue * 0.114f
    return if (bgLum > 0.5f) darken(0.15f) else lighten(0.15f)
}

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
        // Tetris Effect-inspired: white-hot flash → color bloom → dissolve outward
        val flash = if (progress < 0.15f) (0.15f - progress) / 0.15f else 0f
        val bloomPhase = (progress * 2f).coerceIn(0f, 1f)
        val dissolve = (progress - 0.3f).coerceIn(0f, 1f) / 0.7f
        // Start white-hot, bloom to saturated color, then dissolve
        val r = baseColor.red + (1f - baseColor.red) * (flash + bloomPhase * 0.3f)
        val g = baseColor.green + (1f - baseColor.green) * (flash + bloomPhase * 0.3f)
        val b = baseColor.blue + (1f - baseColor.blue) * (flash + bloomPhase * 0.3f)
        val alpha = 1f - dissolve * dissolve // quadratic fade feels smoother
        val scale = 1f + flash * 0.2f - dissolve * 0.3f // slight pop then shrink
        Color(r.coerceIn(0f,1f), g.coerceIn(0f,1f), b.coerceIn(0f,1f), alpha.coerceIn(0f,1f)) to scale.coerceAtLeast(0.1f)
    }
    AnimationStyle.FLASHY -> {
        val colors = listOf(Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFF8B00FF), Color(0xFFFF00FF))
        val idx = ((x + progress * 16).toInt() % colors.size)
        val alpha = if (progress > 0.6f) 1f - ((progress - 0.6f) / 0.4f) else 1f
        colors[idx].copy(alpha = alpha) to (1f + kotlin.math.sin(progress * 12.56f) * 0.1f)
    }
}

private fun DrawScope.drawGhost(piece: PieceState, ghostY: Int, cellSize: Float, gap: Float, corner: Float, baseColor: Color) {
    val gc = baseColor.copy(alpha = 0.12f)
    val oc = baseColor.copy(alpha = 0.4f)
    val glowColor = baseColor.copy(alpha = 0.06f)
    for (py in piece.shape.indices) for (px in piece.shape[py].indices) {
        if (piece.shape[py][px] > 0) {
            val bx = piece.position.x + px; val by = ghostY + py
            if (bx in 0 until TetrisGame.BOARD_WIDTH && by in 0 until TetrisGame.BOARD_HEIGHT) {
                val o = Offset(bx * cellSize + gap, by * cellSize + gap); val s = Size(cellSize - gap * 2, cellSize - gap * 2)
                // Outer glow
                val glowGap = gap * 1.5f
                drawRoundRect(glowColor, Offset(o.x - glowGap, o.y - glowGap),
                    Size(s.width + glowGap * 2, s.height + glowGap * 2), CornerRadius(corner + glowGap))
                // Fill + outline
                drawRoundRect(gc, o, s, CornerRadius(corner))
                drawRoundRect(oc, o, s, CornerRadius(corner), style = Stroke(gap * 0.8f))
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
