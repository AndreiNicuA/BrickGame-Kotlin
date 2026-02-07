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
    val linesCount = clearingLines.size

    val infiniteTransition = rememberInfiniteTransition(label = "lineClear")
    val durationMs = (animationDuration * 500).toInt().coerceAtLeast(100)

    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "progress"
    )

    val flashSpeed = when (linesCount) { 1 -> 150; 2 -> 100; 3 -> 70; else -> 50 }
    val flashState by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(flashSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "flash"
    )

    val rainbowSpeed = when (linesCount) {
        1 -> durationMs; 2 -> (durationMs * 0.7).toInt()
        3 -> (durationMs * 0.5).toInt(); else -> (durationMs * 0.3).toInt()
    }
    val rainbowOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(rainbowSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rainbow"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (linesCount >= 3) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val explosionProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "explosion"
    )

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(theme.screenBackground)
            .padding(2.dp)
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val pixelWidth = availableWidth / TetrisGame.BOARD_WIDTH
        val pixelHeight = availableHeight / TetrisGame.BOARD_HEIGHT
        val pixelSize = minOf(pixelWidth, pixelHeight)
        val boardWidth = pixelSize * TetrisGame.BOARD_WIDTH
        val boardHeight = pixelSize * TetrisGame.BOARD_HEIGHT

        Canvas(modifier = Modifier.width(boardWidth).height(boardHeight)) {
            val cellSize = size.width / TetrisGame.BOARD_WIDTH
            val gap = cellSize * 0.06f
            val cornerRadius = cellSize * 0.12f

            for (y in 0 until TetrisGame.BOARD_HEIGHT) {
                for (x in 0 until TetrisGame.BOARD_WIDTH) {
                    val cellValue = board[y][x]
                    val isClearing = clearingLines.contains(y)
                    val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                    val cellSizeWithGap = Size(cellSize - gap * 2, cellSize - gap * 2)

                    // Background cell
                    drawRoundRect(
                        color = theme.pixelOff,
                        topLeft = offset,
                        size = cellSizeWithGap,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )

                    if (cellValue > 0) {
                        if (isClearing && animationStyle != AnimationStyle.NONE) {
                            val (color, scale) = getClearingAnimation(
                                animationStyle, linesCount, x, y,
                                theme.pixelOn, flashState, animationProgress,
                                rainbowOffset, pulseScale, explosionProgress
                            )

                            val scaledSize = Size(cellSizeWithGap.width * scale, cellSizeWithGap.height * scale)
                            val scaledOffset = Offset(
                                offset.x + (cellSizeWithGap.width - scaledSize.width) / 2,
                                offset.y + (cellSizeWithGap.height - scaledSize.height) / 2
                            )

                            drawRoundRect(
                                color = color, topLeft = scaledOffset,
                                size = scaledSize,
                                cornerRadius = CornerRadius(cornerRadius * scale, cornerRadius * scale)
                            )

                            // Glow for Tetris in FLASHY
                            if (animationStyle == AnimationStyle.FLASHY && linesCount >= 4) {
                                val glowSize = Size(scaledSize.width * 1.3f, scaledSize.height * 1.3f)
                                val glowOffset = Offset(
                                    scaledOffset.x - (glowSize.width - scaledSize.width) / 2,
                                    scaledOffset.y - (glowSize.height - scaledSize.height) / 2
                                )
                                drawRoundRect(
                                    color = color.copy(alpha = 0.3f * (1f - explosionProgress)),
                                    topLeft = glowOffset, size = glowSize,
                                    cornerRadius = CornerRadius(cornerRadius * 1.3f, cornerRadius * 1.3f)
                                )
                            }
                        } else {
                            drawRoundRect(
                                color = theme.pixelOn, topLeft = offset,
                                size = cellSizeWithGap,
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        }
                    }
                }
            }

            // Ghost piece
            if (showGhost && currentPiece != null && ghostY > currentPiece.position.y) {
                drawGhostPiece(currentPiece, ghostY, cellSize, gap, cornerRadius, theme.pixelOn)
            }
        }
    }
}

private fun getClearingAnimation(
    style: AnimationStyle, linesCount: Int, x: Int, y: Int,
    baseColor: Color, flashState: Float, animationProgress: Float,
    rainbowOffset: Float, pulseScale: Float, explosionProgress: Float
): Pair<Color, Float> {
    return when (style) {
        AnimationStyle.NONE -> Pair(baseColor, 1f)
        AnimationStyle.RETRO -> {
            val blinkColor = if (flashState > 0.5f) Color.White else baseColor
            val blinkScale = if (linesCount >= 4 && flashState > 0.5f) 1.1f else 1f
            Pair(blinkColor, blinkScale)
        }
        AnimationStyle.MODERN -> {
            val alpha = animationProgress
            val fadeColor = Color(
                red = baseColor.red + (1f - baseColor.red) * alpha,
                green = baseColor.green + (1f - baseColor.green) * alpha,
                blue = baseColor.blue + (1f - baseColor.blue) * alpha,
                alpha = 1f - (animationProgress * 0.3f)
            )
            val shrinkScale = 1f - (animationProgress * 0.2f * linesCount.coerceAtMost(4) / 4f)
            Pair(fadeColor, shrinkScale)
        }
        AnimationStyle.FLASHY -> {
            val baseColors = listOf(
                Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00),
                Color(0xFF00FF00), Color(0xFF00FFFF), Color(0xFF0000FF),
                Color(0xFF8B00FF), Color(0xFFFF00FF), Color(0xFFFFFFFF),
                Color(0xFFFFC0CB), Color(0xFF00FF7F), Color(0xFFFF1493)
            )
            val colorIndex = when (linesCount) {
                1 -> ((x + rainbowOffset.toInt()) % 6)
                2 -> ((x + y + rainbowOffset.toInt()) % 8)
                3 -> ((x + y * 2 + rainbowOffset.toInt() * 2) % 10)
                else -> ((x + y + (rainbowOffset * 2).toInt() + (explosionProgress * 12).toInt()) % 12)
            }
            val flashyColor = baseColors[colorIndex % baseColors.size]
            val flashyScale = when (linesCount) {
                1 -> 1f + sin(animationProgress * 6.28f) * 0.05f
                2 -> 1f + sin(animationProgress * 6.28f) * 0.08f
                3 -> pulseScale * (1f + sin(animationProgress * 12.56f) * 0.05f)
                else -> pulseScale * (1f + sin(explosionProgress * 12.56f) * 0.15f)
            }
            Pair(flashyColor, flashyScale)
        }
    }
}

private fun DrawScope.drawGhostPiece(
    piece: PieceState, ghostY: Int,
    cellSize: Float, gap: Float, cornerRadius: Float, baseColor: Color
) {
    val ghostColor = baseColor.copy(alpha = 0.2f)
    val outlineColor = baseColor.copy(alpha = 0.4f)

    for (py in piece.shape.indices) {
        for (px in piece.shape[py].indices) {
            if (piece.shape[py][px] > 0) {
                val x = piece.position.x + px
                val y = ghostY + py
                if (x in 0 until TetrisGame.BOARD_WIDTH && y in 0 until TetrisGame.BOARD_HEIGHT) {
                    val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                    val cellSizeWithGap = Size(cellSize - gap * 2, cellSize - gap * 2)
                    drawRoundRect(
                        color = ghostColor, topLeft = offset, size = cellSizeWithGap,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    drawRoundRect(
                        color = outlineColor, topLeft = offset, size = cellSizeWithGap,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        style = Stroke(width = gap * 0.8f)
                    )
                }
            }
        }
    }
}

// ===== Next Piece Preview (supports alpha for queue position) =====

@Composable
fun NextPiecePreview(
    shape: List<List<Int>>?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val theme = LocalGameTheme.current

    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).background(theme.pixelOff).padding(4.dp)) {
        if (shape != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rows = shape.size
                val cols = shape.maxOfOrNull { it.size } ?: 0
                if (rows == 0 || cols == 0) return@Canvas

                val cellSize = minOf(size.width / cols, size.height / rows)
                val offsetX = (size.width - cellSize * cols) / 2
                val offsetY = (size.height - cellSize * rows) / 2
                val gap = cellSize * 0.1f
                val corner = cellSize * 0.2f

                for (y in shape.indices) {
                    for (x in shape[y].indices) {
                        if (shape[y][x] > 0) {
                            drawRoundRect(
                                color = theme.pixelOn.copy(alpha = alpha),
                                topLeft = Offset(offsetX + x * cellSize + gap, offsetY + y * cellSize + gap),
                                size = Size(cellSize - gap * 2, cellSize - gap * 2),
                                cornerRadius = CornerRadius(corner, corner)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== Hold Piece Preview (grayed out when used) =====

@Composable
fun HoldPiecePreview(
    shape: List<List<Int>>?,
    isUsed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val displayAlpha = if (isUsed) 0.3f else 1f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(theme.pixelOff.copy(alpha = if (isUsed) 0.5f else 1f))
            .padding(4.dp)
    ) {
        if (shape != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rows = shape.size
                val cols = shape.maxOfOrNull { it.size } ?: 0
                if (rows == 0 || cols == 0) return@Canvas

                val cellSize = minOf(size.width / cols, size.height / rows)
                val offsetX = (size.width - cellSize * cols) / 2
                val offsetY = (size.height - cellSize * rows) / 2
                val gap = cellSize * 0.1f
                val corner = cellSize * 0.2f

                for (y in shape.indices) {
                    for (x in shape[y].indices) {
                        if (shape[y][x] > 0) {
                            drawRoundRect(
                                color = theme.pixelOn.copy(alpha = displayAlpha),
                                topLeft = Offset(offsetX + x * cellSize + gap, offsetY + y * cellSize + gap),
                                size = Size(cellSize - gap * 2, cellSize - gap * 2),
                                cornerRadius = CornerRadius(corner, corner)
                            )
                        }
                    }
                }
            }
        }
    }
}
