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
    vibrationEnabled: Boolean,
    ghostPieceEnabled: Boolean,
    animationStyle: AnimationStyle,
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
    
    Box(modifier = modifier.fillMaxSize().background(theme.backgroundColor)) {
        when (layoutMode) {
            LayoutMode.CLASSIC -> ClassicLayout(gameState, ghostPieceEnabled, animationStyle, onTogglePause, onResetGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
            LayoutMode.MODERN -> ModernLayout(gameState, ghostPieceEnabled, animationStyle, onTogglePause, onResetGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
            LayoutMode.FULLSCREEN -> FullscreenLayout(gameState, ghostPieceEnabled, animationStyle, onTogglePause, onResetGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
        }
        
        if (gameState.status == GameStatus.GAME_OVER) {
            GameOverOverlay(score = gameState.score, level = gameState.level, lines = gameState.lines, highScore = gameState.highScore, onPlayAgain = onStartGame)
        }
    }
}

@Composable
private fun ClassicLayout(
    gameState: GameState, ghostPieceEnabled: Boolean, animationStyle: AnimationStyle,
    onTogglePause: () -> Unit, onResetGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Device frame
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(theme.deviceColor).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LCD Screen
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).padding(6.dp)
            ) {
                // Game board
                GameBoard(
                    board = gameState.board,
                    currentPiece = gameState.currentPiece,
                    ghostY = gameState.ghostY,
                    showGhost = ghostPieceEnabled,
                    clearedLines = gameState.clearedLineRows,
                    animationStyle = animationStyle,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                
                Spacer(modifier = Modifier.width(2.dp).fillMaxHeight().background(theme.pixelOn.copy(alpha = 0.2f)))
                
                // Info panel
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
            
            // Status
            if (gameState.status == GameStatus.PAUSED) {
                Text("PAUSED", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
                Spacer(modifier = Modifier.height(6.dp))
            }
            
            // Control buttons row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton(if (gameState.status == GameStatus.PAUSED) "PLAY" else "PAUSE", onTogglePause)
                ActionButton("RESET", onResetGame)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main controls - ergonomic layout
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // D-Pad on left
                DPad(
                    onUpPress = onHardDrop,
                    onDownPress = onMoveDown,
                    onDownRelease = onMoveDownRelease,
                    onLeftPress = onMoveLeft,
                    onLeftRelease = onMoveLeftRelease,
                    onRightPress = onMoveRight,
                    onRightRelease = onMoveRightRelease,
                    buttonSize = 58.dp  // 20% larger
                )
                
                // Rotate on right
                RotateButton(onClick = onRotate, size = 72.dp)  // 20% larger
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Brand and menu
            Text("BRICK GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary, letterSpacing = 3.sp)
            Spacer(modifier = Modifier.height(8.dp))
            MenuButton(onClick = onOpenSettings)
        }
    }
}

@Composable
private fun ModernLayout(
    gameState: GameState, ghostPieceEnabled: Boolean, animationStyle: AnimationStyle,
    onTogglePause: () -> Unit, onResetGame: () -> Unit,
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
                clearedLines = gameState.clearedLineRows,
                animationStyle = animationStyle,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            )
            
            if (gameState.status == GameStatus.PAUSED) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Text("PAUSED", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Controls - ergonomic spacing
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                buttonSize = 54.dp
            )
            
            // Middle buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SmallButton(if (gameState.status == GameStatus.PAUSED) "▶" else "⏸", onTogglePause, 42.dp)
                SmallButton("↺", onResetGame, 42.dp)
            }
            
            RotateButton(onClick = onRotate, size = 68.dp)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        MenuButton(onClick = onOpenSettings)
    }
}

@Composable
private fun FullscreenLayout(
    gameState: GameState, ghostPieceEnabled: Boolean, animationStyle: AnimationStyle,
    onTogglePause: () -> Unit, onResetGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(modifier = Modifier.fillMaxSize().background(theme.screenBackground).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Mini status bar
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
            clearedLines = gameState.clearedLineRows,
            animationStyle = animationStyle,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
        )
        
        if (gameState.status == GameStatus.PAUSED) {
            Text("PAUSED", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.pixelOn)
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Compact controls
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton(if (gameState.status == GameStatus.PAUSED) "▶" else "⏸", onTogglePause, 38.dp)
                    SmallButton("↺", onResetGame, 38.dp)
                }
                SmallButton("☰", onOpenSettings, 38.dp)
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
private fun ActionButton(text: String, onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(theme.buttonSecondary).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary) }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit, size: Dp) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(theme.buttonSecondary).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 18.sp, color = theme.textPrimary, fontWeight = FontWeight.Medium) }
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(theme.buttonSecondary.copy(alpha = 0.6f)).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text("☰ MENU", fontSize = 16.sp, color = theme.textSecondary, fontWeight = FontWeight.Medium) }
}

@Composable
private fun GameOverOverlay(score: Int, level: Int, lines: Int, highScore: Int, onPlayAgain: () -> Unit) {
    val isNewHighScore = score >= highScore && score > 0
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 3.sp)
            
            if (isNewHighScore) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("NEW HIGH SCORE!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F))
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(score.toString(), fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFFF4D03F))
            Text("Level $level  •  $lines lines", fontSize = 18.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4D03F)).clickable(onClick = onPlayAgain).padding(horizontal = 40.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
        }
    }
}
