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
import kotlin.random.Random

/**
 * Main game board with ghost piece and line clear animations
 */
@Composable
fun GameBoard(
    board: List<List<Int>>,
    modifier: Modifier = Modifier,
    currentPiece: PieceState? = null,
    ghostY: Int = 0,
    showGhost: Boolean = true,
    clearedLines: List<Int> = emptyList(),
    animationStyle: AnimationStyle = AnimationStyle.MODERN
) {
    val theme = LocalGameTheme.current
    
    // Animation for cleared lines
    val infiniteTransition = rememberInfiniteTransition(label = "lineClear")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    // Flash animation
    var flashPhase by remember { mutableStateOf(0) }
    LaunchedEffect(clearedLines) {
        if (clearedLines.isNotEmpty()) {
            flashPhase = (flashPhase + 1) % 10
        }
    }
    
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
            
            // Draw board cells
            for (y in 0 until TetrisGame.BOARD_HEIGHT) {
                for (x in 0 until TetrisGame.BOARD_WIDTH) {
                    val cellValue = board[y][x]
                    val isClearing = clearedLines.contains(y)
                    
                    val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                    val cellSizeWithGap = Size(cellSize - gap * 2, cellSize - gap * 2)
                    
                    // Background
                    drawRoundRect(
                        color = theme.pixelOff,
                        topLeft = offset,
                        size = cellSizeWithGap,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    
                    // Filled cell or clearing animation
                    if (cellValue > 0) {
                        val color = if (isClearing) {
                            getLineClearColor(animationStyle, animationProgress, x, flashPhase, theme.pixelOn)
                        } else {
                            theme.pixelOn
                        }
                        
                        // Scale effect for clearing
                        val scale = if (isClearing && animationStyle != AnimationStyle.NONE) {
                            when (animationStyle) {
                                AnimationStyle.RETRO -> if (animationProgress > 0.5f) 1f else 0f
                                AnimationStyle.MODERN -> 1f - (animationProgress * 0.3f)
                                AnimationStyle.FLASHY -> 1f + sin(animationProgress * 6.28f) * 0.1f
                                else -> 1f
                            }
                        } else 1f
                        
                        val scaledSize = Size(cellSizeWithGap.width * scale, cellSizeWithGap.height * scale)
                        val scaledOffset = Offset(
                            offset.x + (cellSizeWithGap.width - scaledSize.width) / 2,
                            offset.y + (cellSizeWithGap.height - scaledSize.height) / 2
                        )
                        
                        drawRoundRect(
                            color = color,
                            topLeft = scaledOffset,
                            size = scaledSize,
                            cornerRadius = CornerRadius(cornerRadius * scale, cornerRadius * scale)
                        )
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

private fun getLineClearColor(
    style: AnimationStyle,
    progress: Float,
    x: Int,
    phase: Int,
    baseColor: Color
): Color {
    return when (style) {
        AnimationStyle.NONE -> baseColor
        
        AnimationStyle.RETRO -> {
            // Simple blink
            if (progress > 0.5f) Color.White else baseColor
        }
        
        AnimationStyle.MODERN -> {
            // Smooth fade to white
            val alpha = progress
            Color(
                red = baseColor.red + (1f - baseColor.red) * alpha,
                green = baseColor.green + (1f - baseColor.green) * alpha,
                blue = baseColor.blue + (1f - baseColor.blue) * alpha,
                alpha = 1f
            )
        }
        
        AnimationStyle.FLASHY -> {
            // Rainbow wave effect
            val colors = listOf(
                Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00),
                Color(0xFF00FF00), Color(0xFF0000FF), Color(0xFF8B00FF)
            )
            val colorIndex = ((x + phase + (progress * 10).toInt()) % colors.size)
            colors[colorIndex]
        }
    }
}

private fun DrawScope.drawGhostPiece(
    piece: PieceState,
    ghostY: Int,
    cellSize: Float,
    gap: Float,
    cornerRadius: Float,
    baseColor: Color
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
                        color = ghostColor,
                        topLeft = offset,
                        size = cellSizeWithGap,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = offset,
                        size = cellSizeWithGap,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        style = Stroke(width = gap * 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Next piece preview
 */
@Composable
fun NextPiecePreview(
    shape: List<List<Int>>?,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(theme.pixelOff)
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
                                color = theme.pixelOn,
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
