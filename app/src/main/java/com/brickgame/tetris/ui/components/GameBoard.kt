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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import com.brickgame.tetris.game.TetrisGame
import com.brickgame.tetris.ui.animations.LineClearAnimationState
import com.brickgame.tetris.ui.animations.LineClearPhase
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * Main game board displaying the LCD-style pixel grid
 */
@Composable
fun GameBoard(
    board: List<List<Int>>,
    modifier: Modifier = Modifier,
    ghostPiece: List<Pair<Int, Int>>? = null,
    lineClearAnimation: LineClearAnimationState = LineClearAnimationState()
) {
    val theme = LocalGameTheme.current
    
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.screenBackground)
            .padding(4.dp)
    ) {
        val availableWidth = maxWidth - 8.dp
        val availableHeight = maxHeight - 8.dp
        
        // Calculate pixel size to fit board
        val pixelWidth = availableWidth / TetrisGame.BOARD_WIDTH
        val pixelHeight = availableHeight / TetrisGame.BOARD_HEIGHT
        val pixelSize = minOf(pixelWidth, pixelHeight)
        
        // Center the board
        val boardWidth = pixelSize * TetrisGame.BOARD_WIDTH
        val boardHeight = pixelSize * TetrisGame.BOARD_HEIGHT
        
        Box(
            modifier = Modifier
                .size(boardWidth, boardHeight)
                .align(Alignment.Center)
        ) {
            // Draw all pixels using Canvas for performance
            PixelGrid(
                board = board,
                pixelSize = pixelSize,
                theme = theme,
                ghostPositions = ghostPiece,
                lineClearAnimation = lineClearAnimation
            )
        }
    }
}

@Composable
private fun PixelGrid(
    board: List<List<Int>>,
    pixelSize: Dp,
    theme: GameTheme,
    ghostPositions: List<Pair<Int, Int>>?,
    lineClearAnimation: LineClearAnimationState
) {
    val density = LocalDensity.current
    val pixelSizePx = with(density) { pixelSize.toPx() }
    val gapPx = with(density) { 1.dp.toPx() }
    val cornerRadiusPx = with(density) { 2.dp.toPx() }
    
    // Flash animation for line clear
    val flashAlpha by animateFloatAsState(
        targetValue = if (lineClearAnimation.phase == LineClearPhase.FLASH) {
            if (lineClearAnimation.progress > 0.5f) 0.3f else 1f
        } else 1f,
        animationSpec = tween(50),
        label = "flashAlpha"
    )
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val onColor = theme.pixelOn
        val offColor = theme.pixelOff
        val ghostColor = theme.pixelOn.copy(alpha = 0.2f)
        
        for (y in 0 until TetrisGame.BOARD_HEIGHT) {
            val isClearing = y in lineClearAnimation.rows
            
            for (x in 0 until TetrisGame.BOARD_WIDTH) {
                val cellValue = board.getOrNull(y)?.getOrNull(x) ?: 0
                val isGhost = ghostPositions?.any { it.first == y && it.second == x } == true
                
                val color = when {
                    cellValue != 0 -> {
                        if (isClearing) onColor.copy(alpha = flashAlpha) else onColor
                    }
                    isGhost -> ghostColor
                    else -> offColor
                }
                
                val left = x * pixelSizePx + gapPx
                val top = y * pixelSizePx + gapPx
                val size = pixelSizePx - gapPx * 2
                
                // Draw pixel with rounded corners
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
                
                // Draw subtle border
                drawRoundRect(
                    color = theme.pixelBorder,
                    topLeft = Offset(left, top),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
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
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (shape != null && shape.isNotEmpty()) {
            val density = LocalDensity.current
            val pixelSize = 10.dp
            val pixelSizePx = with(density) { pixelSize.toPx() }
            val gapPx = with(density) { 1.dp.toPx() }
            
            Canvas(
                modifier = Modifier
                    .width(pixelSize * shape[0].size)
                    .height(pixelSize * shape.size)
            ) {
                for (y in shape.indices) {
                    for (x in shape[y].indices) {
                        val color = if (shape[y][x] != 0) theme.pixelOn else theme.pixelOff
                        
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x * pixelSizePx + gapPx, y * pixelSizePx + gapPx),
                            size = Size(pixelSizePx - gapPx * 2, pixelSizePx - gapPx * 2),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Info panel showing score, level, lines
 */
@Composable
fun InfoPanel(
    score: Int,
    level: Int,
    lines: Int,
    nextShape: List<List<Int>>?,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoItem(label = "SCORE", value = score.toString().padStart(6, '0'))
        InfoItem(label = "LEVEL", value = level.toString())
        InfoItem(label = "LINES", value = lines.toString())
        
        // Next piece
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "NEXT",
                fontSize = 9.sp,
                color = theme.textPrimary
            )
            NextPiecePreview(
                shape = nextShape,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    val theme = LocalGameTheme.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = theme.textPrimary
        )
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(theme.pixelOff)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )
        }
    }
}

/**
 * Decorative squares (blue/red)
 */
@Composable
fun DecorationRow(
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Blue squares
        repeat(8) {
            Box(
                modifier = Modifier
                    .padding(1.dp)
                    .size(10.dp)
                    .background(theme.decoColor1, RoundedCornerShape(1.dp))
            )
        }
        
        // Red squares
        repeat(8) {
            Box(
                modifier = Modifier
                    .padding(1.dp)
                    .size(10.dp)
                    .background(theme.decoColor2, RoundedCornerShape(1.dp))
            )
        }
    }
}

/**
 * Side decoration column
 */
@Composable
fun SideDecoration(
    color: Color,
    count: Int = 20,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(8.dp, 10.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
    }
}