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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.brickgame.tetris.game.PieceState
import com.brickgame.tetris.game.TetrisGame
import com.brickgame.tetris.ui.animations.LineClearAnimationState
import com.brickgame.tetris.ui.animations.LineClearPhase
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * Main game board with ghost piece support
 */
@Composable
fun GameBoard(
    board: List<List<Int>>,
    modifier: Modifier = Modifier,
    currentPiece: PieceState? = null,
    ghostY: Int = 0,
    showGhost: Boolean = true,
    lineClearAnimation: LineClearAnimationState = LineClearAnimationState()
) {
    val theme = LocalGameTheme.current
    
    // Animation for line clear flash
    val flashAlpha by animateFloatAsState(
        targetValue = if (lineClearAnimation.isAnimating && lineClearAnimation.phase == LineClearPhase.FLASH) 
            lineClearAnimation.progress else 0f,
        animationSpec = tween(60),
        label = "flash"
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
        
        Canvas(
            modifier = Modifier
                .width(boardWidth)
                .height(boardHeight)
        ) {
            val cellSize = size.width / TetrisGame.BOARD_WIDTH
            val gap = cellSize * 0.08f
            val cornerRadius = cellSize * 0.15f
            
            // Draw each cell
            for (y in 0 until TetrisGame.BOARD_HEIGHT) {
                for (x in 0 until TetrisGame.BOARD_WIDTH) {
                    val cellValue = board[y][x]
                    val isClearing = lineClearAnimation.isAnimating && 
                                   lineClearAnimation.rows.contains(y)
                    
                    val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                    val cellSizeWithGap = Size(cellSize - gap * 2, cellSize - gap * 2)
                    
                    // Background (empty cell)
                    drawRoundRect(
                        color = theme.pixelOff,
                        topLeft = offset,
                        size = cellSizeWithGap,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    
                    // Filled cell
                    if (cellValue > 0) {
                        val color = if (isClearing && flashAlpha > 0.5f) {
                            Color.White
                        } else {
                            theme.pixelOn
                        }
                        
                        drawRoundRect(
                            color = color,
                            topLeft = offset,
                            size = cellSizeWithGap,
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                }
            }
            
            // Draw ghost piece (if enabled and we have a current piece)
            if (showGhost && currentPiece != null && ghostY > currentPiece.position.y) {
                val ghostColor = theme.pixelOn.copy(alpha = 0.25f)
                
                for (py in currentPiece.shape.indices) {
                    for (px in currentPiece.shape[py].indices) {
                        if (currentPiece.shape[py][px] > 0) {
                            val x = currentPiece.position.x + px
                            val y = ghostY + py
                            
                            if (x in 0 until TetrisGame.BOARD_WIDTH && 
                                y in 0 until TetrisGame.BOARD_HEIGHT) {
                                
                                val offset = Offset(x * cellSize + gap, y * cellSize + gap)
                                val cellSizeWithGap = Size(cellSize - gap * 2, cellSize - gap * 2)
                                
                                // Draw ghost as outline
                                drawRoundRect(
                                    color = ghostColor,
                                    topLeft = offset,
                                    size = cellSizeWithGap,
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )
                                
                                // Also draw a border for better visibility
                                drawRoundRect(
                                    color = theme.pixelOn.copy(alpha = 0.4f),
                                    topLeft = offset,
                                    size = cellSizeWithGap,
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                    style = Stroke(width = gap)
                                )
                            }
                        }
                    }
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
