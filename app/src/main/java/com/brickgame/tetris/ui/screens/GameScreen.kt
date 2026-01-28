package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.LocalGameTheme

enum class LayoutMode { CLASSIC, MODERN, FULLSCREEN }

@Composable
fun GameScreen(
    gameState: GameState,
    clearingLines: List<Int>,
    vibrationEnabled: Boolean,
    ghostPieceEnabled: Boolean,
    animationEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    layoutMode: LayoutMode,
    onStartGame: () -> Unit,
    onPauseGame: () -> Unit,
    onResumeGame: () -> Unit,
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
    val effectiveStyle = if (animationEnabled) animationStyle else AnimationStyle.NONE
    
    Box(modifier = modifier.fillMaxSize().background(theme.backgroundColor)) {
        when (layoutMode) {
            LayoutMode.CLASSIC -> ClassicLayout(gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration, onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
            LayoutMode.MODERN -> ModernLayout(gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration, onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
            LayoutMode.FULLSCREEN -> FullscreenLayout(gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration, onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
        }
        
        // Pause Overlay
        if (gameState.status == GameStatus.PAUSED) {
            PauseOverlay(onResume = onResumeGame, onSettings = onOpenSettings)
        }
        
        // Game Over Overlay
        if (gameState.status == GameStatus.GAME_OVER) {
            GameOverOverlay(score = gameState.score, level = gameState.level, lines = gameState.lines, highScore = gameState.highScore, onPlayAgain = onStartGame)
        }
    }
}

@Composable
private fun ClassicLayout(
    gameState: GameState, clearingLines: List<Int>, ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(theme.deviceColor).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LCD Screen - full height without shrinking for status text
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).padding(6.dp)
            ) {
                GameBoard(
                    board = gameState.board,
                    currentPiece = gameState.currentPiece,
                    ghostY = gameState.ghostY,
                    showGhost = ghostPieceEnabled,
                    clearingLines = clearingLines,
                    animationStyle = animationStyle,
                    animationDuration = animationDuration,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                
                Spacer(modifier = Modifier.width(2.dp).fillMaxHeight().background(theme.pixelOn.copy(alpha = 0.2f)))
                
                Column(
                    modifier = Modifier.width(70.dp).fillMaxHeight().padding(6.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InfoBlock("SCORE", gameState.score.toString().padStart(6, '0'))
                    InfoBlock("LEVEL", gameState.level.toString())
                    InfoBlock("LINES", gameState.lines.toString())
                    Text("NEXT", fontSize = 12.sp, color = theme.pixelOn.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    NextPiecePreview(shape = gameState.nextPiece?.shape, modifier = Modifier.size(44.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Action buttons - consistent style
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton("START", onStartGame)
                ActionButton("PAUSE", onPauseGame, enabled = gameState.status == GameStatus.PLAYING)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPad(onUpPress = onHardDrop, onDownPress = onMoveDown, onDownRelease = onMoveDownRelease, onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease, onRightPress = onMoveRight, onRightRelease = onMoveRightRelease, buttonSize = 58.dp)
                RotateButton(onClick = onRotate, size = 72.dp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text("BRICK GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary, letterSpacing = 3.sp)
            Spacer(modifier = Modifier.height(8.dp))
            MenuButton(onClick = onOpenSettings)
        }
    }
}

@Composable
private fun ModernLayout(
    gameState: GameState, clearingLines: List<Int>, ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Status bar
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(gameState.score.toString().padStart(6, '0'), fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LV.${gameState.level}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Text("${gameState.lines} lines", fontSize = 13.sp, color = theme.textSecondary)
            }
            NextPiecePreview(shape = gameState.nextPiece?.shape, modifier = Modifier.size(48.dp))
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Game board
        Box(modifier = Modifier.weight(1f).aspectRatio(0.5f).clip(RoundedCornerShape(10.dp)).background(theme.screenBackground), contentAlignment = Alignment.Center) {
            GameBoard(
                board = gameState.board,
                currentPiece = gameState.currentPiece,
                ghostY = gameState.ghostY,
                showGhost = ghostPieceEnabled,
                clearingLines = clearingLines,
                animationStyle = animationStyle,
                animationDuration = animationDuration,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Action buttons row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            ActionButton("START", onStartGame)
            Spacer(modifier = Modifier.width(16.dp))
            ActionButton("PAUSE", onPauseGame, enabled = gameState.status == GameStatus.PLAYING)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(onUpPress = onHardDrop, onDownPress = onMoveDown, onDownRelease = onMoveDownRelease, onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease, onRightPress = onMoveRight, onRightRelease = onMoveRightRelease, buttonSize = 54.dp)
            RotateButton(onClick = onRotate, size = 68.dp)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        MenuButton(onClick = onOpenSettings)
    }
}

@Composable
private fun FullscreenLayout(
    gameState: GameState, clearingLines: List<Int>, ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(modifier = Modifier.fillMaxSize().background(theme.screenBackground).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Compact status row
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(gameState.score.toString().padStart(6, '0'), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
            Text("L${gameState.level} • ${gameState.lines}", fontSize = 14.sp, color = theme.pixelOn.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            Box(modifier = Modifier.size(32.dp).background(theme.pixelOff, RoundedCornerShape(4.dp))) {
                NextPiecePreview(shape = gameState.nextPiece?.shape, modifier = Modifier.fillMaxSize().padding(3.dp))
            }
        }
        
        // Game board - maximum space
        GameBoard(
            board = gameState.board,
            currentPiece = gameState.currentPiece,
            ghostY = gameState.ghostY,
            showGhost = ghostPieceEnabled,
            clearingLines = clearingLines,
            animationStyle = animationStyle,
            animationDuration = animationDuration,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Controls with action buttons
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            DPad(onUpPress = onHardDrop, onDownPress = onMoveDown, onDownRelease = onMoveDownRelease, onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease, onRightPress = onMoveRight, onRightRelease = onMoveRightRelease, buttonSize = 50.dp)
            
            // Center buttons column
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionButton("▶", onStartGame)
                    SmallActionButton("⏸", onPauseGame, enabled = gameState.status == GameStatus.PLAYING)
                }
                SmallActionButton("☰", onOpenSettings)
            }
            
            RotateButton(onClick = onRotate, size = 60.dp)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = theme.pixelOn.copy(alpha = 0.5f), letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) theme.buttonSecondary else theme.buttonSecondary.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f))
    }
}

@Composable
private fun SmallActionButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (enabled) theme.buttonSecondary else theme.buttonSecondary.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 18.sp, color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(theme.buttonSecondary.copy(alpha = 0.6f)).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text("☰ MENU", fontSize = 16.sp, color = theme.textSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 4.sp)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Resume button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4D03F))
                    .clickable(onClick = onResume)
                    .padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("RESUME", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Settings button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF333333))
                    .clickable(onClick = onSettings)
                    .padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SETTINGS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, level: Int, lines: Int, highScore: Int, onPlayAgain: () -> Unit) {
    val isNewHighScore = score >= highScore && score > 0
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 3.sp)
            if (isNewHighScore) { Spacer(modifier = Modifier.height(12.dp)); Text("NEW HIGH SCORE!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F)) }
            Spacer(modifier = Modifier.height(28.dp))
            Text(score.toString(), fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFFF4D03F))
            Text("Level $level  •  $lines lines", fontSize = 18.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(40.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4D03F)).clickable(onClick = onPlayAgain).padding(horizontal = 40.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text("PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
