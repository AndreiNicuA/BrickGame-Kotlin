package com.brickgame.tetris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
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
import kotlin.math.sin

@Composable
fun GameBoard(
    board: List<List<Int>>,
    modifier: Modifier = Modifier,
    currentPiece: PieceState? = null,
    ghostY: Int = 0,
    showGhost: Boolean = true,
    clearingLines: List<Int> = emptyList(),
    animationStyle: AnimationStyle = AnimationStyle.MODERN,
    animationDuration: Float = 0.5f
) {
    val theme = LocalGameTheme.current

    // FIXED: Use finite animation instead of infiniteTransition
    // This animatable goes from 0→1 over the animation duration, then stays at 1
    val clearProgress = remember { Animatable(0f) }

    // When clearingLines changes, restart the animation
    LaunchedEffect(clearingLines) {
        if (clearingLines.isNotEmpty()) {
            clearProgress.snapTo(0f)
            clearProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (animationDuration * 500).toInt().coerceAtLeast(150),
                    easing = LinearEasing
                )
            )
        } else {
            clearProgress.snapTo(0f)
        }
    }

    val progress = clearProgress.value
    val isClearing = clearingLines.isNotEmpty()

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(theme.screenBackground)
            .padding(2.dp)
    ) {
        val pixelWidth = maxWidth / TetrisGame.BOARD_WIDTH
        val pixelHeight = maxHeight / TetrisGame.BOARD_HEIGHT
        val pixelSize = minOf(pixelWidth, pixelHeight)
        val boardWidth = pixelSize * TetrisGame.BOARD_WIDTH
        val boardHeight = pixelSize * TetrisGame.BOARD_HEIGHT

        Canvas(modifier = Modifier.width(boardWidth).height(boardHeight)) {
            val cellSize = size.width / TetrisGame.BOARD_WIDTH
            val gap = cellSize * 0.06f
            val corner = cellSize * 0.15f

            for (y in 0 until TetrisGame.BOARD_HEIGHT) {
                for (x in 0 until TetrisGame.BOARD_WIDTH) {
                    val cellValue = board[y][x]
                    val isClearingRow = clearingLines.contains(y)
                    val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                    val cs = Size(cellSize - gap * 2, cellSize - gap * 2)

                    // Background cell
                    drawRoundRect(
                        color = theme.pixelOff,
                        topLeft = offset, size = cs,
                        cornerRadius = CornerRadius(corner)
                    )

                    if (cellValue > 0) {
                        if (isClearingRow && isClearing && animationStyle != AnimationStyle.NONE) {
                            // Animate clearing row
                            val (color, scale) = clearingEffect(animationStyle, progress, x, y, theme.pixelOn, clearingLines.size)
                            val ss = Size(cs.width * scale, cs.height * scale)
                            val so = Offset(offset.x + (cs.width - ss.width) / 2, offset.y + (cs.height - ss.height) / 2)
                            drawRoundRect(color = color, topLeft = so, size = ss, cornerRadius = CornerRadius(corner * scale))
                        } else {
                            drawRoundRect(color = theme.pixelOn, topLeft = offset, size = cs, cornerRadius = CornerRadius(corner))
                        }
                    }
                }
            }

            // Ghost piece
            if (showGhost && currentPiece != null && ghostY > currentPiece.position.y) {
                drawGhost(currentPiece, ghostY, cellSize, gap, corner, theme.pixelOn)
            }
        }
    }
}

private fun clearingEffect(
    style: AnimationStyle, progress: Float, x: Int, y: Int,
    baseColor: Color, lineCount: Int
): Pair<Color, Float> = when (style) {
    AnimationStyle.NONE -> baseColor to 1f
    AnimationStyle.RETRO -> {
        // Simple blink: alternate white/base, then fade out
        val blink = if ((progress * 6).toInt() % 2 == 0) Color.White else baseColor
        val fade = if (progress > 0.7f) 1f - ((progress - 0.7f) / 0.3f) * 0.3f else 1f
        blink.copy(alpha = fade) to 1f
    }
    AnimationStyle.MODERN -> {
        // Smooth fade to white then shrink away
        val fadeToWhite = Color(
            red = baseColor.red + (1f - baseColor.red) * progress,
            green = baseColor.green + (1f - baseColor.green) * progress,
            blue = baseColor.blue + (1f - baseColor.blue) * progress,
            alpha = 1f - progress * 0.5f
        )
        val shrink = 1f - progress * 0.3f
        fadeToWhite to shrink
    }
    AnimationStyle.FLASHY -> {
        // Rainbow sweep across the row
        val colors = listOf(
            Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00),
            Color(0xFF00FF00), Color(0xFF00FFFF), Color(0xFF0000FF),
            Color(0xFF8B00FF), Color(0xFFFF00FF)
        )
        val idx = ((x + progress * 16).toInt() % colors.size)
        val pulse = 1f + sin(progress * 12.56f) * 0.1f
        val alpha = if (progress > 0.6f) 1f - ((progress - 0.6f) / 0.4f) else 1f
        colors[idx].copy(alpha = alpha) to pulse
    }
}

private fun DrawScope.drawGhost(
    piece: PieceState, ghostY: Int,
    cellSize: Float, gap: Float, corner: Float, baseColor: Color
) {
    val ghostColor = baseColor.copy(alpha = 0.15f)
    val outlineColor = baseColor.copy(alpha = 0.35f)

    for (py in piece.shape.indices) {
        for (px in piece.shape[py].indices) {
            if (piece.shape[py][px] > 0) {
                val bx = piece.position.x + px
                val by = ghostY + py
                if (bx in 0 until TetrisGame.BOARD_WIDTH && by in 0 until TetrisGame.BOARD_HEIGHT) {
                    val offset = Offset(bx * cellSize + gap, by * cellSize + gap)
                    val cs = Size(cellSize - gap * 2, cellSize - gap * 2)
                    drawRoundRect(ghostColor, offset, cs, CornerRadius(corner))
                    drawRoundRect(outlineColor, offset, cs, CornerRadius(corner), style = Stroke(gap * 0.8f))
                }
            }
        }
    }
}

// ===== Piece Previews =====

@Composable
fun NextPiecePreview(shape: List<List<Int>>?, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val theme = LocalGameTheme.current
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(theme.pixelOff.copy(alpha = 0.5f)).padding(4.dp)) {
        if (shape != null) {
            Canvas(Modifier.fillMaxSize()) {
                val rows = shape.size; val cols = shape.maxOfOrNull { it.size } ?: 0
                if (rows == 0 || cols == 0) return@Canvas
                val cs = minOf(size.width / cols, size.height / rows)
                val ox = (size.width - cs * cols) / 2; val oy = (size.height - cs * rows) / 2
                val gap = cs * 0.1f; val corner = cs * 0.2f
                for (y in shape.indices) for (x in shape[y].indices) {
                    if (shape[y][x] > 0) drawRoundRect(
                        theme.pixelOn.copy(alpha = alpha),
                        Offset(ox + x * cs + gap, oy + y * cs + gap),
                        Size(cs - gap * 2, cs - gap * 2), CornerRadius(corner)
                    )
                }
            }
        }
    }
}

@Composable
fun HoldPiecePreview(shape: List<List<Int>>?, isUsed: Boolean = false, modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    val a = if (isUsed) 0.3f else 1f
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(theme.pixelOff.copy(alpha = if (isUsed) 0.3f else 0.5f)).padding(4.dp)) {
        if (shape != null) {
            Canvas(Modifier.fillMaxSize()) {
                val rows = shape.size; val cols = shape.maxOfOrNull { it.size } ?: 0
                if (rows == 0 || cols == 0) return@Canvas
                val cs = minOf(size.width / cols, size.height / rows)
                val ox = (size.width - cs * cols) / 2; val oy = (size.height - cs * rows) / 2
                val gap = cs * 0.1f; val corner = cs * 0.2f
                for (y in shape.indices) for (x in shape[y].indices) {
                    if (shape[y][x] > 0) drawRoundRect(
                        theme.pixelOn.copy(alpha = a),
                        Offset(ox + x * cs + gap, oy + y * cs + gap),
                        Size(cs - gap * 2, cs - gap * 2), CornerRadius(corner)
                    )
                }
            }
        }
    }
}
