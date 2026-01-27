package com.brickgame.tetris.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.animations.LineClearAnimationState
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * Main Game Screen - Classic Layout (restored from v1.0)
 */
@Composable
fun GameScreen(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    vibrationEnabled: Boolean,
    isFullscreen: Boolean,
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
            .background(theme.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (isFullscreen) {
            FullscreenLayout(
                gameState = gameState,
                lineClearAnimation = lineClearAnimation,
                onStartGame = onStartGame,
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
        } else {
            ClassicLayout(
                gameState = gameState,
                lineClearAnimation = lineClearAnimation,
                onStartGame = onStartGame,
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
        }
        
        // Game Over Overlay
        if (gameState.status == GameStatus.GAME_OVER) {
            GameOverOverlay(
                score = gameState.score,
                level = gameState.level,
                lines = gameState.lines,
                highScore = gameState.highScore,
                onRestart = onStartGame
            )
        }
    }
}

/**
 * Classic Layout - Original v1.0 design restored
 */
@Composable
private fun ClassicLayout(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
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
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(theme.deviceColor)
            .border(
                width = 3.dp,
                color = theme.deviceBorderColor,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top decoration row
        DecorationRow(modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Screen area with side decorations
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left decoration
            DecorationColumn(
                color = theme.decoColor1,
                count = 22,
                modifier = Modifier.width(12.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Game board
            GameBoard(
                board = gameState.board,
                lineClearAnimation = lineClearAnimation,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.5f)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Info panel
            InfoPanel(
                score = gameState.score,
                level = gameState.level,
                lines = gameState.lines,
                nextShape = gameState.nextPiece?.shape,
                modifier = Modifier.width(70.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Right decoration
            DecorationColumn(
                color = theme.decoColor2,
                count = 22,
                modifier = Modifier.width(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bottom decoration row
        DecorationRow(modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Pause indicator
        if (gameState.status == GameStatus.PAUSED) {
            PauseIndicator()
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Small buttons row (ON/OFF, SOUND, START/RESUME, RESET)
        SmallButtonsRow(
            gameStatus = gameState.status,
            onTogglePause = onTogglePause,
            onResetGame = onResetGame,
            onToggleSound = onToggleSound
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons (D-Pad + Rotate)
        ControlsSection(
            onMoveLeft = onMoveLeft,
            onMoveLeftRelease = onMoveLeftRelease,
            onMoveRight = onMoveRight,
            onMoveRightRelease = onMoveRightRelease,
            onMoveDown = onMoveDown,
            onMoveDownRelease = onMoveDownRelease,
            onHardDrop = onHardDrop,
            onRotate = onRotate
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Branding
        Text(
            text = "BRICK GAME",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary,
            letterSpacing = 4.sp
        )
        Text(
            text = "9999 in 1",
            fontSize = 12.sp,
            color = theme.textSecondary,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Settings button at bottom (original position)
        SettingsButton(onClick = onOpenSettings)
    }
}

/**
 * Fullscreen Layout - Game only, minimal UI
 */
@Composable
private fun FullscreenLayout(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
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
        // Top bar with score and settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score
            Column {
                Text(
                    text = "SCORE",
                    fontSize = 10.sp,
                    color = theme.pixelOn.copy(alpha = 0.6f)
                )
                Text(
                    text = gameState.score.toString().padStart(6, '0'),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.pixelOn
                )
            }
            
            // Level and Lines
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LV.${gameState.level}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.pixelOn
                )
                Text(
                    text = "${gameState.lines} lines",
                    fontSize = 12.sp,
                    color = theme.pixelOn.copy(alpha = 0.7f)
                )
            }
            
            // Settings button
            SettingsButton(onClick = onOpenSettings)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Game board - larger in fullscreen
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
            )
            
            // Pause overlay
            if (gameState.status == GameStatus.PAUSED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⏸ PAUSED",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap START to resume",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
                SmallFunctionButton(
                    label = "RESET",
                    onClick = onResetGame
                )
            }
            
            // Rotate button
            RotateButton(
                onClick = onRotate,
                size = 64.dp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    level: Int,
    lines: Int,
    highScore: Int,
    onRestart: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gameOver")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "GAME OVER",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray.copy(alpha = 0.8f))
                    .padding(24.dp)
            ) {
                Text(
                    text = "FINAL SCORE",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = score.toString().padStart(6, '0'),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF4D03F),
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LEVEL", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            level.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LINES", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            lines.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                if (score >= highScore && score > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🏆 NEW HIGH SCORE! 🏆",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4D03F)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Press START to play again",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

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
        text = "⏸ PAUSED - Press START to resume",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = theme.accentColor.copy(alpha = alpha)
    )
}

@Composable
private fun SmallButtonsRow(
    gameStatus: GameStatus,
    onTogglePause: () -> Unit,
    onResetGame: () -> Unit,
    onToggleSound: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SmallFunctionButton(
            label = "ON/OFF",
            onClick = onResetGame
        )
        SmallFunctionButton(
            label = "SOUND",
            onClick = onToggleSound
        )
        SmallFunctionButton(
            label = when (gameStatus) {
                GameStatus.PAUSED -> "RESUME"
                GameStatus.GAME_OVER -> "START"
                else -> "START"
            },
            onClick = onTogglePause
        )
        SmallFunctionButton(
            label = "RESET",
            onClick = onResetGame
        )
    }
}

@Composable
private fun ControlsSection(
    onMoveLeft: () -> Unit,
    onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit,
    onRotate: () -> Unit
) {
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
            onRightRelease = onMoveRightRelease
        )
        
        RotateButton(onClick = onRotate)
    }
}
