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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.animations.LineClearAnimationState
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.LocalGameTheme

enum class LayoutMode {
    CLASSIC,
    MODERN,
    FULLSCREEN
}

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
        
        // Game Over Overlay
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
 * CLASSIC LAYOUT - Simple unified LCD like original hardware
 */
@Composable
private fun ClassicLayout(
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
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Device frame
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.deviceColor)
                .border(2.dp, theme.deviceBorderColor, RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LCD Screen with game + info
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.screenBackground)
                    .padding(4.dp)
            ) {
                // Game board
                GameBoard(
                    board = gameState.board,
                    lineClearAnimation = lineClearAnimation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                
                // Divider
                Spacer(modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(theme.pixelOn.copy(alpha = 0.3f)))
                
                // Info panel
                Column(
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InfoBlock("SCORE", gameState.score.toString().padStart(6, '0'))
                    InfoBlock("LEVEL", gameState.level.toString())
                    InfoBlock("LINES", gameState.lines.toString())
                    Text("NEXT", fontSize = 8.sp, color = theme.pixelOn.copy(alpha = 0.6f))
                    NextPiecePreview(
                        shape = gameState.nextPiece?.shape,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pause text
            if (gameState.status == GameStatus.PAUSED) {
                Text(
                    "PAUSED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Simple button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SimpleButton(
                    text = if (gameState.status == GameStatus.PAUSED) "PLAY" else "PAUSE",
                    onClick = onTogglePause
                )
                SimpleButton(text = "RESET", onClick = onResetGame)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
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
                    buttonSize = 46.dp
                )
                
                RotateButton(onClick = onRotate, size = 56.dp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Branding + Settings
            Text("BRICK GAME", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary, letterSpacing = 2.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MenuButton(onClick = onOpenSettings)
        }
    }
}

/**
 * MODERN LAYOUT - Clean status bar at top
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
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(gameState.score.toString().padStart(6, '0'), 
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, 
                    fontFamily = FontFamily.Monospace, color = theme.accentColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LV.${gameState.level}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Text("${gameState.lines} lines", fontSize = 10.sp, color = theme.textSecondary)
            }
            NextPiecePreview(shape = gameState.nextPiece?.shape, modifier = Modifier.size(40.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Game board
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.5f)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.screenBackground),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                board = gameState.board,
                lineClearAnimation = lineClearAnimation,
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
            
            if (gameState.status == GameStatus.PAUSED) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("PAUSED", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
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
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleButton(
                    text = if (gameState.status == GameStatus.PAUSED) "▶" else "⏸",
                    onClick = onTogglePause
                )
                SimpleButton(text = "↺", onClick = onResetGame)
            }
            
            RotateButton(onClick = onRotate, size = 52.dp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        MenuButton(onClick = onOpenSettings)
    }
}

/**
 * FULLSCREEN LAYOUT - Maximum game area
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
        // Minimal top info
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                gameState.score.toString().padStart(6, '0'),
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = theme.pixelOn
            )
            Text(
                "L${gameState.level} • ${gameState.lines}",
                fontSize = 12.sp, color = theme.pixelOn.copy(alpha = 0.7f)
            )
            Box(modifier = Modifier.size(28.dp).background(theme.pixelOff, RoundedCornerShape(4.dp))) {
                NextPiecePreview(shape = gameState.nextPiece?.shape, modifier = Modifier.fillMaxSize().padding(2.dp))
            }
        }
        
        // Game board - maximum size
        GameBoard(
            board = gameState.board,
            lineClearAnimation = lineClearAnimation,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        if (gameState.status == GameStatus.PAUSED) {
            Text("PAUSED", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.pixelOn)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Compact controls
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
                buttonSize = 40.dp
            )
            
            SimpleButton(
                text = if (gameState.status == GameStatus.PAUSED) "▶" else "⏸",
                onClick = onTogglePause
            )
            
            RotateButton(onClick = onRotate, size = 48.dp)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        MenuButton(onClick = onOpenSettings, size = 36.dp)
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 7.sp, color = theme.pixelOn.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.pixelOn, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SimpleButton(text: String, onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.buttonSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
    }
}

@Composable
private fun MenuButton(onClick: () -> Unit, size: Dp = 40.dp) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(theme.buttonSecondary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) {
                Box(modifier = Modifier.width(size * 0.4f).height(2.dp).background(theme.textPrimary, RoundedCornerShape(1.dp)))
            }
        }
    }
}

/**
 * Game Over - 50% transparent, PLAY AGAIN button
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
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(32.dp)
        ) {
            Text(
                "GAME OVER",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = theme.accentColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                score.toString().padStart(6, '0'),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = theme.accentColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Level $level • $lines lines",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            if (score >= highScore && score > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("NEW HIGH SCORE!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(theme.accentColor)
                    .clickable(onClick = onPlayAgain)
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text("PLAY AGAIN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
