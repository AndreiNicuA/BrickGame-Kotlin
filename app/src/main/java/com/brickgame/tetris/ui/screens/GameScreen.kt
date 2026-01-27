package com.brickgame.tetris.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
 * Main Game Screen
 * Supports multiple layout modes: Classic, Compact, Fullscreen
 */
@Composable
fun GameScreen(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    vibrationEnabled: Boolean,
    layoutMode: LayoutMode,
    playerName: String,
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
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when (layoutMode) {
            LayoutMode.CLASSIC -> ClassicLayout(
                gameState = gameState,
                lineClearAnimation = lineClearAnimation,
                playerName = playerName,
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
                onOpenSettings = onOpenSettings,
                onOpenProfile = onOpenProfile
            )
            
            LayoutMode.COMPACT -> CompactLayout(
                gameState = gameState,
                lineClearAnimation = lineClearAnimation,
                playerName = playerName,
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
                onOpenSettings = onOpenSettings,
                onOpenProfile = onOpenProfile
            )
            
            LayoutMode.FULLSCREEN -> FullscreenLayout(
                gameState = gameState,
                lineClearAnimation = lineClearAnimation,
                playerName = playerName,
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
                onOpenSettings = onOpenSettings,
                onOpenProfile = onOpenProfile
            )
        }
    }
}

@Composable
private fun ClassicLayout(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    playerName: String,
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
    onOpenProfile: () -> Unit
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
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Pause indicator
        if (gameState.status == GameStatus.PAUSED) {
            PauseIndicator()
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Small buttons row
        SmallButtonsRow(
            gameStatus = gameState.status,
            onStartGame = onStartGame,
            onTogglePause = onTogglePause,
            onResetGame = onResetGame,
            onToggleSound = onToggleSound
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons
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
        
        // Settings and Profile buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmallIconButton(
                text = "👤",
                onClick = onOpenProfile,
                modifier = Modifier.size(44.dp)
            )
            
            SmallIconButton(
                text = "⚙️",
                onClick = onOpenSettings,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun CompactLayout(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    playerName: String,
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
    onOpenProfile: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top bar with info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Score info
            Column {
                Text(
                    text = "SCORE",
                    fontSize = 10.sp,
                    color = theme.textSecondary
                )
                Text(
                    text = gameState.score.toString().padStart(6, '0'),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
            }
            
            // Level/Lines
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LV.${gameState.level}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accentColor
                )
                Text(
                    text = "${gameState.lines} lines",
                    fontSize = 12.sp,
                    color = theme.textSecondary
                )
            }
            
            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallIconButton(text = "👤", onClick = onOpenProfile)
                SmallIconButton(text = "⚙️", onClick = onOpenSettings)
            }
        }
        
        // Game board - larger in compact mode
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            GameBoard(
                board = gameState.board,
                lineClearAnimation = lineClearAnimation,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.5f)
                    .align(Alignment.Center)
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
                        text = "PAUSED\n\nTap START to resume",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Controls at bottom
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallFunctionButton(
                    label = if (gameState.status == GameStatus.PAUSED) "RESUME" else "START",
                    onClick = onTogglePause
                )
                SmallFunctionButton(label = "RESET", onClick = onResetGame)
            }
            
            // D-Pad and Rotate
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
        }
    }
}

@Composable
private fun FullscreenLayout(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    playerName: String,
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
    onOpenProfile: () -> Unit
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
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gameState.score.toString().padStart(6, '0'),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = theme.pixelOn
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "LV.${gameState.level}",
                    fontSize = 14.sp,
                    color = theme.pixelOn
                )
                Text(
                    text = "•",
                    fontSize = 14.sp,
                    color = theme.pixelOn.copy(alpha = 0.5f)
                )
                Text(
                    text = "${gameState.lines}L",
                    fontSize = 14.sp,
                    color = theme.pixelOn
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallIconButton(text = "👤", onClick = onOpenProfile, size = 36.dp)
                SmallIconButton(text = "⚙️", onClick = onOpenSettings, size = 36.dp)
            }
        }
        
        // Game board takes most space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "⏸️ PAUSED",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Tap START to resume",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        
        // Controls - more compact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - D-Pad
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
            
            // Center - Start/Reset
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFunctionButton(
                    label = if (gameState.status == GameStatus.PAUSED) "▶" else "⏸",
                    onClick = onTogglePause
                )
                SmallFunctionButton(
                    label = "↺",
                    onClick = onResetGame
                )
            }
            
            // Right side - Rotate
            RotateButton(
                onClick = onRotate,
                size = 64.dp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
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
    onStartGame: () -> Unit,
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
            label = if (gameStatus == GameStatus.PAUSED) "RESUME" else "START",
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

@Composable
private fun SmallIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    val theme = LocalGameTheme.current
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(theme.buttonSecondary)
            .shadow(2.dp, CircleShape)
            .then(
                Modifier.clickableWithoutRipple { onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

// Helper extension for clickable without ripple
@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onClick() })
        }
    )
}
