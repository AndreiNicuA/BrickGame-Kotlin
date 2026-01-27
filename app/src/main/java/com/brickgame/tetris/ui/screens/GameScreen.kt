package com.brickgame.tetris.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.animations.LineClearAnimationState
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.LocalGameTheme

enum class LayoutMode {
    CLASSIC,    // Design A - Unified LCD
    MODERN,     // Design C - Modern minimal  
    FULLSCREEN  // Pure game, maximum screen
}

/**
 * Main Game Screen
 */
@Composable
fun GameScreen(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    vibrationEnabled: Boolean,
    layoutMode: LayoutMode,
    onStartGame: () -> Unit,
    onTogglePause: () -> Unit,
    onResetGame: () -> Unit,
    onToggleSound: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit,
    onRotate: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
    ) {
        when (layoutMode) {
            LayoutMode.CLASSIC -> ClassicLayout(
                gameState = gameState,
                lineClearAnimation = lineClearAnimation,
                onTogglePause = onTogglePause,
                onResetGame = onResetGame,
                onToggleSound = onToggleSound,
                onMoveLeft = onMoveLeft,
                onMoveLeftRelease = onMoveLeftRelease,
                onMoveRight = onMoveRight,
                onMoveRightRelease = onMoveRightRelease,
                onMoveDown = onMoveDown,
                onMoveDownRelease = onMoveDownRelease,
                onHardDrop = onHardDrop,
                onRotate = onRotate,
                onOpenSettings = onOpenSettings
            )
            
            LayoutMode.MODERN -> ModernLayout(
                gameState = gameState,
                lineClearAnimation = lineClearAnimation,
                onTogglePause = onTogglePause,
                onResetGame = onResetGame,
                onMoveLeft = onMoveLeft,
                onMoveLeftRelease = onMoveLeftRelease,
                onMoveRight = onMoveRight,
                onMoveRightRelease = onMoveRightRelease,
                onMoveDown = onMoveDown,
                onMoveDownRelease = onMoveDownRelease,
                onHardDrop = onHardDrop,
                onRotate = onRotate,
                onOpenSettings = onOpenSettings
            )
            
            LayoutMode.FULLSCREEN -> FullscreenLayout(
                gameState = gameState,
                lineClearAnimation = lineClearAnimation,
                onTogglePause = onTogglePause,
                onResetGame = onResetGame,
                onMoveLeft = onMoveLeft,
                onMoveLeftRelease = onMoveLeftRelease,
                onMoveRight = onMoveRight,
                onMoveRightRelease = onMoveRightRelease,
                onMoveDown = onMoveDown,
                onMoveDownRelease = onMoveDownRelease,
                onHardDrop = onHardDrop,
                onRotate = onRotate,
                onOpenSettings = onOpenSettings
            )
        }
        
        // Game Over Overlay (50% transparent)
        if (gameState.status == GameStatus.GAME_OVER) {
            GameOverOverlay(
                score = gameState.score,
                level = gameState.level,
                lines = gameState.lines,
                highScore = gameState.highScore,
                onPlayAgain = onStartGame
            )
        }
    }
}

/**
 * Design A: Classic Layout - Unified LCD Screen
 * All info inside single LCD display like original hardware
 */
@Composable
private fun ClassicLayout(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    onTogglePause: () -> Unit,
    onResetGame: () -> Unit,
    onToggleSound: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit,
    onRotate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Device frame
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.deviceColor)
                .border(3.dp, theme.deviceBorderColor, RoundedCornerShape(20.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Unified LCD Screen (game + info together)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.screenBackground)
                    .border(2.dp, theme.screenBorderColor, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Game board area
                    GameBoard(
                        board = gameState.board,
                        lineClearAnimation = lineClearAnimation,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    
                    // Divider line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(theme.pixelOff)
                    )
                    
                    // Info panel (inside LCD)
                    Column(
                        modifier = Modifier
                            .width(65.dp)
                            .fillMaxHeight()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LcdInfoItem(label = "SCORE", value = gameState.score.toString().padStart(6, '0'))
                        LcdInfoItem(label = "LEVEL", value = gameState.level.toString())
                        LcdInfoItem(label = "LINES", value = gameState.lines.toString())
                        LcdInfoItem(label = "NEXT", value = null)
                        NextPiecePreview(
                            shape = gameState.nextPiece?.shape,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pause indicator
            if (gameState.status == GameStatus.PAUSED) {
                PauseIndicator()
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Small function buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SmallFunctionButton(label = "SOUND", onClick = onToggleSound)
                SmallFunctionButton(
                    label = if (gameState.status == GameStatus.PAUSED) "RESUME" else "START",
                    onClick = onTogglePause
                )
                SmallFunctionButton(label = "RESET", onClick = onResetGame)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // D-Pad and Rotate (no overlap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPad(
                    onUpPress = onHardDrop,
                    onDownPress = onMoveDown,
                    onDownRelease = onMoveDownRelease,
                    onLeftPress = onMoveLeft,
                    onLeftRelease = onMoveLeftRelease,
                    onRightPress = onMoveRight,
                    onRightRelease = onMoveRightRelease,
                    buttonSize = 50.dp
                )
                
                RotateButton(onClick = onRotate, size = 60.dp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Branding
            Text(
                text = "BRICK GAME",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary,
                letterSpacing = 3.sp
            )
            Text(
                text = "9999 in 1",
                fontSize = 10.sp,
                color = theme.textSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Settings button (hamburger menu)
            HamburgerMenuButton(onClick = onOpenSettings)
        }
    }
}

/**
 * Design C: Modern Layout - Clean minimal look
 */
@Composable
private fun ModernLayout(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    onTogglePause: () -> Unit,
    onResetGame: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit,
    onRotate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score
            Column {
                Text(
                    text = "SCORE",
                    fontSize = 9.sp,
                    color = theme.textSecondary
                )
                Text(
                    text = gameState.score.toString().padStart(6, '0'),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = theme.accentColor
                )
            }
            
            // Level & Lines
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LV.${gameState.level}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
                Text(
                    text = "${gameState.lines} lines",
                    fontSize = 11.sp,
                    color = theme.textSecondary
                )
            }
            
            // Next piece preview
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NEXT",
                    fontSize = 8.sp,
                    color = theme.textSecondary
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.screenBackground.copy(alpha = 0.5f))
                        .border(1.dp, theme.screenBorderColor, RoundedCornerShape(6.dp))
                        .padding(2.dp)
                ) {
                    NextPiecePreview(
                        shape = gameState.nextPiece?.shape,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Game board
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.5f)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.screenBackground)
                .border(2.dp, theme.screenBorderColor, RoundedCornerShape(8.dp))
        ) {
            GameBoard(
                board = gameState.board,
                lineClearAnimation = lineClearAnimation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
            
            // Pause overlay
            if (gameState.status == GameStatus.PAUSED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PAUSED",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Controls row (no overlap)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // D-Pad
            DPad(
                onUpPress = onHardDrop,
                onDownPress = onMoveDown,
                onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft,
                onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight,
                onRightRelease = onMoveRightRelease,
                buttonSize = 48.dp
            )
            
            // Center buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFunctionButton(
                    label = if (gameState.status == GameStatus.PAUSED) "RESUME" else "START",
                    onClick = onTogglePause
                )
                SmallFunctionButton(label = "RESET", onClick = onResetGame)
            }
            
            // Rotate button
            RotateButton(onClick = onRotate, size = 56.dp)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Settings button
        HamburgerMenuButton(onClick = onOpenSettings)
    }
}

/**
 * Fullscreen Layout - Maximum game area
 */
@Composable
private fun FullscreenLayout(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    onTogglePause: () -> Unit,
    onResetGame: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit,
    onRotate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.screenBackground)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Minimal top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gameState.score.toString().padStart(6, '0'),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = theme.pixelOn
            )
            
            Text(
                text = "LV.${gameState.level} | ${gameState.lines}L",
                fontSize = 12.sp,
                color = theme.pixelOn.copy(alpha = 0.7f)
            )
            
            // Mini next preview
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.pixelOff)
            ) {
                NextPiecePreview(
                    shape = gameState.nextPiece?.shape,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            }
        }
        
        // Game board - maximum size
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                board = gameState.board,
                lineClearAnimation = lineClearAnimation,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.5f)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, theme.pixelOn.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
            
            if (gameState.status == GameStatus.PAUSED) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PAUSED",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.pixelOn
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Controls (compact, no overlap)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(
                onUpPress = onHardDrop,
                onDownPress = onMoveDown,
                onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft,
                onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight,
                onRightRelease = onMoveRightRelease,
                buttonSize = 44.dp
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallFunctionButton(
                    label = if (gameState.status == GameStatus.PAUSED) "▶" else "⏸",
                    onClick = onTogglePause
                )
                SmallFunctionButton(label = "↺", onClick = onResetGame)
            }
            
            RotateButton(onClick = onRotate, size = 52.dp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Settings button
        HamburgerMenuButton(onClick = onOpenSettings, size = 40.dp)
    }
}

/**
 * LCD Info Item (for Classic layout)
 */
@Composable
private fun LcdInfoItem(label: String, value: String?) {
    val theme = LocalGameTheme.current
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 7.sp,
            color = theme.pixelOn.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace
        )
        if (value != null) {
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = theme.pixelOn,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Hamburger Menu Button (☰)
 */
@Composable
private fun HamburgerMenuButton(
    onClick: () -> Unit,
    size: Dp = 44.dp
) {
    val theme = LocalGameTheme.current
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(theme.buttonSecondary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(size * 0.4f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(theme.textPrimary)
                )
            }
        }
    }
}

/**
 * Pause Indicator
 */
@Composable
private fun PauseIndicator() {
    val theme = LocalGameTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "pause")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pauseAlpha"
    )
    
    Text(
        text = "PAUSED - Press START",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = theme.accentColor.copy(alpha = alpha)
    )
}

/**
 * Game Over Overlay - 50% transparent background with PLAY AGAIN button
 */
@Composable
private fun GameOverOverlay(
    score: Int,
    level: Int,
    lines: Int,
    highScore: Int,
    onPlayAgain: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),  // 50% transparent
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Game Over title (uses theme accent color)
            Text(
                text = "GAME OVER",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = theme.accentColor,
                letterSpacing = 4.sp
            )
            
            // Score box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp)
            ) {
                Text(
                    text = "FINAL SCORE",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = score.toString().padStart(6, '0'),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accentColor,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LEVEL", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            level.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LINES", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            lines.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                if (score >= highScore && score > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🏆 NEW HIGH SCORE!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // PLAY AGAIN button (uses theme accent color)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(theme.accentColor)
                    .clickable(onClick = onPlayAgain)
                    .padding(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "▶  PLAY AGAIN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
