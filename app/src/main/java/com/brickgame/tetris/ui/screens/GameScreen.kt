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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.animations.LineClearAnimationState
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.LocalGameTheme

enum class LayoutMode { CLASSIC, MODERN, FULLSCREEN }

@Composable
fun GameScreen(
    gameState: GameState,
    lineClearAnimation: LineClearAnimationState,
    vibrationEnabled: Boolean,
    ghostPieceEnabled: Boolean,
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
            LayoutMode.CLASSIC -> ClassicLayout(gameState, lineClearAnimation, ghostPieceEnabled, onTogglePause, onResetGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
            LayoutMode.MODERN -> ModernLayout(gameState, lineClearAnimation, ghostPieceEnabled, onTogglePause, onResetGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
            LayoutMode.FULLSCREEN -> FullscreenLayout(gameState, lineClearAnimation, ghostPieceEnabled, onTogglePause, onResetGame, onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate, onOpenSettings)
        }
        
        if (gameState.status == GameStatus.GAME_OVER) {
            GameOverOverlay(score = gameState.score, level = gameState.level, lines = gameState.lines, highScore = gameState.highScore, onPlayAgain = onStartGame)
        }
    }
}

@Composable
private fun ClassicLayout(
    gameState: GameState, lineClearAnimation: LineClearAnimationState, ghostPieceEnabled: Boolean,
    onTogglePause: () -> Unit, onResetGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.deviceColor).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(theme.screenBackground).padding(4.dp)
            ) {
                GameBoard(
                    board = gameState.board,
                    currentPiece = gameState.currentPiece,
                    ghostY = gameState.ghostY,
                    showGhost = ghostPieceEnabled,
                    lineClearAnimation = lineClearAnimation,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                
                Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(theme.pixelOn.copy(alpha = 0.3f)))
                
                Column(
                    modifier = Modifier.width(60.dp).fillMaxHeight().padding(4.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InfoBlock("SCORE", gameState.score.toString().padStart(6, '0'))
                    InfoBlock("LEVEL", gameState.level.toString())
                    InfoBlock("LINES", gameState.lines.toString())
                    Text("NEXT", fontSize = 8.sp, color = theme.pixelOn.copy(alpha = 0.6f))
                    NextPiecePreview(shape = gameState.nextPiece?.shape, modifier = Modifier.size(36.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (gameState.status == GameStatus.PAUSED) {
                Text("PAUSED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SimpleButton(if (gameState.status == GameStatus.PAUSED) "PLAY" else "PAUSE", onTogglePause)
                SimpleButton("RESET", onResetGame)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                DPad(onUpPress = onHardDrop, onDownPress = onMoveDown, onDownRelease = onMoveDownRelease, onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease, onRightPress = onMoveRight, onRightRelease = onMoveRightRelease, buttonSize = 46.dp)
                RotateButton(onClick = onRotate, size = 56.dp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("BRICK GAME", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(8.dp))
            MenuButton(onClick = onOpenSettings)
        }
    }
}

@Composable
private fun ModernLayout(
    gameState: GameState, lineClearAnimation: LineClearAnimationState, ghostPieceEnabled: Boolean,
    onTogglePause: () -> Unit, onResetGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(gameState.score.toString().padStart(6, '0'), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LV.${gameState.level}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Text("${gameState.lines} lines", fontSize = 10.sp, color = theme.textSecondary)
            }
            NextPiecePreview(shape = gameState.nextPiece?.shape, modifier = Modifier.size(40.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(modifier = Modifier.weight(1f).aspectRatio(0.5f).clip(RoundedCornerShape(8.dp)).background(theme.screenBackground), contentAlignment = Alignment.Center) {
            GameBoard(board = gameState.board, currentPiece = gameState.currentPiece, ghostY = gameState.ghostY, showGhost = ghostPieceEnabled, lineClearAnimation = lineClearAnimation, modifier = Modifier.fillMaxSize().padding(4.dp))
            
            if (gameState.status == GameStatus.PAUSED) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Text("PAUSED", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            DPad(onUpPress = onHardDrop, onDownPress = onMoveDown, onDownRelease = onMoveDownRelease, onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease, onRightPress = onMoveRight, onRightRelease = onMoveRightRelease, buttonSize = 44.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleButton(if (gameState.status == GameStatus.PAUSED) "▶" else "⏸", onTogglePause)
                SimpleButton("↺", onResetGame)
            }
            RotateButton(onClick = onRotate, size = 52.dp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        MenuButton(onClick = onOpenSettings)
    }
}

@Composable
private fun FullscreenLayout(
    gameState: GameState, lineClearAnimation: LineClearAnimationState, ghostPieceEnabled: Boolean,
    onTogglePause: () -> Unit, onResetGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    
    Column(modifier = Modifier.fillMaxSize().background(theme.screenBackground).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(gameState.score.toString().padStart(6, '0'), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
            Text("L${gameState.level} • ${gameState.lines}", fontSize = 12.sp, color = theme.pixelOn.copy(alpha = 0.7f))
            Box(modifier = Modifier.size(28.dp).background(theme.pixelOff, RoundedCornerShape(4.dp))) {
                NextPiecePreview(shape = gameState.nextPiece?.shape, modifier = Modifier.fillMaxSize().padding(2.dp))
            }
        }
        
        GameBoard(board = gameState.board, currentPiece = gameState.currentPiece, ghostY = gameState.ghostY, showGhost = ghostPieceEnabled, lineClearAnimation = lineClearAnimation, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp))
        
        if (gameState.status == GameStatus.PAUSED) {
            Text("PAUSED", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.pixelOn)
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            DPad(onUpPress = onHardDrop, onDownPress = onMoveDown, onDownRelease = onMoveDownRelease, onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease, onRightPress = onMoveRight, onRightRelease = onMoveRightRelease, buttonSize = 42.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniButton(if (gameState.status == GameStatus.PAUSED) "▶" else "⏸", onTogglePause)
                    MiniButton("↺", onResetGame)
                }
                MiniButton("☰", onOpenSettings)
            }
            RotateButton(onClick = onRotate, size = 50.dp)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 7.sp, color = theme.pixelOn.copy(alpha = 0.5f), letterSpacing = 0.5.sp)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
    }
}

@Composable
private fun SimpleButton(text: String, onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(theme.buttonSecondary).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary) }
}

@Composable
private fun MiniButton(text: String, onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(theme.buttonSecondary).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 14.sp, color = theme.textPrimary) }
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(theme.buttonSecondary.copy(alpha = 0.5f)).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text("☰", fontSize = 16.sp, color = theme.textSecondary) }
}

@Composable
private fun GameOverOverlay(score: Int, level: Int, lines: Int, highScore: Int, onPlayAgain: () -> Unit) {
    val isNewHighScore = score >= highScore && score > 0
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
            
            if (isNewHighScore) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("NEW HIGH SCORE!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(score.toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFFF4D03F))
            Text("Level $level • $lines lines", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFF4D03F)).clickable(onClick = onPlayAgain).padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("PLAY AGAIN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
        }
    }
}
