package com.brickgame.tetris.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.animations.LineClearAnimationState
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.LocalGameTheme
import kotlinx.coroutines.delay

/**
 * Main Game Screen
 * Displays the device with LCD screen and all controls
 */
@Composable
fun GameScreen(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    vibrationEnabled: Boolean = true,
    onStartGame: () -> Unit,
    onPauseGame: () -> Unit,
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
        // Device container - fills most of the screen
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val maxWidth = maxWidth
            val maxHeight = maxHeight
            
            // Calculate device size (aspect ratio ~0.5)
            val deviceWidth = minOf(maxWidth, maxHeight * 0.52f)
            val deviceHeight = deviceWidth / 0.52f
            
            // Device
            Column(
                modifier = Modifier
                    .width(deviceWidth)
                    .height(minOf(deviceHeight, maxHeight))
                    .align(Alignment.Center)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.deviceColor)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top decoration
                DecorationRow(modifier = Modifier.fillMaxWidth())
                
                // Screen area
                ScreenArea(
                    gameState = gameState,
                    lineClearAnimation = lineClearAnimation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                
                // Bottom decoration
                DecorationRow(modifier = Modifier.fillMaxWidth())
                
                // Small buttons row
                SmallButtonsRow(
                    vibrationEnabled = vibrationEnabled,
                    onOnOff = onResetGame,
                    onSound = onToggleSound,
                    onStart = {
                        when (gameState.status) {
                            GameStatus.MENU, GameStatus.GAME_OVER -> onStartGame()
                            GameStatus.PLAYING -> onPauseGame()
                            GameStatus.PAUSED -> onPauseGame()
                        }
                    },
                    onReset = onResetGame,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Controls area (D-pad and Rotate)
                ControlsArea(
                    vibrationEnabled = vibrationEnabled,
                    onUp = onHardDrop,
                    onDown = onMoveDown,
                    onDownRelease = onMoveDownRelease,
                    onLeft = onMoveLeft,
                    onLeftRelease = onMoveLeftRelease,
                    onRight = onMoveRight,
                    onRightRelease = onMoveRightRelease,
                    onRotate = onRotate,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Branding
                Branding(modifier = Modifier.fillMaxWidth())
            }
        }
        
        // Settings button (bottom right)
        SecondaryGameButton(
            size = 44.dp,
            vibrationEnabled = vibrationEnabled,
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun ScreenArea(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.deviceBorderColor)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left decoration
            SideDecoration(
                color = theme.decoColor1,
                count = 22
            )
            
            // Game screen with overlay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Game board
                GameBoard(
                    board = gameState.board,
                    lineClearAnimation = lineClearAnimation,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Menu overlay
                if (gameState.status != GameStatus.PLAYING) {
                    MenuOverlay(
                        status = gameState.status,
                        score = gameState.score,
                        highScore = gameState.highScore
                    )
                }
            }
            
            // Info panel
            InfoPanel(
                score = gameState.score,
                level = gameState.level,
                lines = gameState.lines,
                nextShape = gameState.nextPiece?.shape,
                modifier = Modifier
                    .width(70.dp)
                    .fillMaxHeight()
            )
            
            // Right decoration
            SideDecoration(
                color = theme.decoColor2,
                count = 22
            )
        }
    }
}

@Composable
private fun MenuOverlay(
    status: GameStatus,
    score: Int,
    highScore: Int
) {
    val theme = LocalGameTheme.current
    
    // Blinking "PRESS START" animation
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.screenBackground.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (status) {
                GameStatus.MENU -> {
                    Text(
                        text = "BRICK",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "GAME",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "PRESS START",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textPrimary.copy(alpha = alpha)
                    )
                    
                    Text(
                        text = "HI: $highScore",
                        fontSize = 12.sp,
                        color = theme.textPrimary
                    )
                }
                
                GameStatus.PAUSED -> {
                    Text(
                        text = "PAUSED",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "PRESS START",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textPrimary.copy(alpha = alpha)
                    )
                }
                
                GameStatus.GAME_OVER -> {
                    Text(
                        text = "GAME",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "OVER",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "SCORE: $score",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textPrimary
                    )
                    
                    if (score >= highScore && score > 0) {
                        Text(
                            text = "NEW HIGH SCORE!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.accentColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "PRESS START",
                        fontSize = 12.sp,
                        color = theme.textPrimary.copy(alpha = alpha)
                    )
                }
                
                else -> {}
            }
        }
    }
}

@Composable
private fun SmallButtonsRow(
    vibrationEnabled: Boolean,
    onOnOff: () -> Unit,
    onSound: () -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabeledSecondaryButton(
            label = "ON/OFF",
            buttonSize = 34.dp,
            vibrationEnabled = vibrationEnabled,
            onClick = onOnOff
        )
        
        LabeledSecondaryButton(
            label = "SOUND",
            buttonSize = 34.dp,
            vibrationEnabled = vibrationEnabled,
            onClick = onSound
        )
        
        LabeledSecondaryButton(
            label = "START",
            buttonSize = 34.dp,
            vibrationEnabled = vibrationEnabled,
            onClick = onStart
        )
        
        LabeledSecondaryButton(
            label = "RESET",
            buttonSize = 34.dp,
            vibrationEnabled = vibrationEnabled,
            onClick = onReset
        )
    }
}

@Composable
private fun ControlsArea(
    vibrationEnabled: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDownRelease: () -> Unit,
    onLeft: () -> Unit,
    onLeftRelease: () -> Unit,
    onRight: () -> Unit,
    onRightRelease: () -> Unit,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // D-Pad
        DPad(
            buttonSize = 52.dp,
            vibrationEnabled = vibrationEnabled,
            onUpPress = onUp,
            onDownPress = onDown,
            onDownRelease = onDownRelease,
            onLeftPress = onLeft,
            onLeftRelease = onLeftRelease,
            onRightPress = onRight,
            onRightRelease = onRightRelease
        )
        
        // Rotate button
        RotateButton(
            size = 64.dp,
            vibrationEnabled = vibrationEnabled,
            onClick = onRotate
        )
    }
}

@Composable
private fun Branding(modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BRICK GAME",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = theme.accentColor,
            letterSpacing = 2.sp
        )
        
        Text(
            text = "9999 in 1",
            fontSize = 11.sp,
            color = theme.textSecondary
        )
    }
}
