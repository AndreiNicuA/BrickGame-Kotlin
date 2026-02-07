package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.layout.DPadStyle
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.LocalGameTheme

@Composable
fun GameScreen(
    gameState: GameState,
    layoutPreset: LayoutPreset,
    dpadStyle: DPadStyle,
    ghostEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    onStartGame: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRotate: () -> Unit,
    onRotateCCW: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onLeftPress: () -> Unit,
    onLeftRelease: () -> Unit,
    onRightPress: () -> Unit,
    onRightRelease: () -> Unit,
    onDownPress: () -> Unit,
    onDownRelease: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSound: () -> Unit
) {
    val theme = LocalGameTheme.current

    Box(
        modifier = Modifier.fillMaxSize().background(theme.backgroundColor)
    ) {
        when {
            gameState.status == GameStatus.MENU -> MenuOverlay(gameState.highScore, onStartGame, onOpenSettings)
            else -> {
                if (layoutPreset.isLandscape) {
                    LandscapeLayout(gameState, layoutPreset, dpadStyle, ghostEnabled, animationStyle, animationDuration,
                        onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease,
                        onDownPress, onDownRelease, onPause, onToggleSound, onOpenSettings)
                } else {
                    PortraitLayout(gameState, dpadStyle, ghostEnabled, animationStyle, animationDuration,
                        onRotate, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease,
                        onDownPress, onDownRelease, onPause, onToggleSound, onOpenSettings, onStartGame)
                }

                if (gameState.status == GameStatus.PAUSED)
                    PauseOverlay(onResume, onOpenSettings)
                if (gameState.status == GameStatus.GAME_OVER)
                    GameOverOverlay(gameState.score, gameState.level, gameState.lines, onStartGame, onOpenSettings)
            }
        }

        // Action label (T-Spin, Tetris, Combo etc)
        if (gameState.lastActionLabel.isNotEmpty() && gameState.status == GameStatus.PLAYING) {
            Text(
                text = gameState.lastActionLabel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.accentColor.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color.Black
            )
        }
    }
}

// ========== PORTRAIT ==========

@Composable
private fun PortraitLayout(
    gs: GameState, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onToggleSound: () -> Unit, onOpenSettings: () -> Unit,
    onStartGame: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === Top: unified info strip ===
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Hold preview
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                InfoLabel("HOLD")
                HoldPiecePreview(
                    shape = gs.holdPiece?.shape, isUsed = gs.holdUsed,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Single unified score area - NO separate LCD boxes
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.screenBackground.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = gs.score.toString().padStart(7, '0'),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = theme.pixelOn,
                    letterSpacing = 2.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("LV${gs.level}", fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                        color = theme.textSecondary, fontWeight = FontWeight.Bold)
                    Text("${gs.lines}L", fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                        color = theme.textSecondary, fontWeight = FontWeight.Bold)
                }
            }

            // Next queue
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                InfoLabel("NEXT")
                gs.nextPieces.take(3).forEachIndexed { i, p ->
                    val sz = when (i) { 0 -> 44.dp; 1 -> 34.dp; else -> 28.dp }
                    NextPiecePreview(shape = p.shape, modifier = Modifier.size(sz).padding(1.dp),
                        alpha = when (i) { 0 -> 1f; 1 -> 0.6f; else -> 0.35f })
                }
            }
        }

        // === Board ===
        GameBoard(
            board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghost, clearingLines = gs.clearedLineRows,
            animationStyle = anim, animationDuration = animDur,
            modifier = Modifier.weight(1f).aspectRatio(0.5f).padding(vertical = 2.dp)
        )

        // === Action row ===
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WideActionButton("HOLD", onClick = onHold, width = 80.dp, height = 36.dp)
            WideActionButton(
                if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                onClick = { if (gs.status == GameStatus.MENU) onStartGame() else onPause() },
                width = 80.dp, height = 36.dp
            )
        }

        // === Controls ===
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(
                buttonSize = 56.dp,
                rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHardDrop, onDownPress = onDownPress, onDownRelease = onDownRelease,
                onLeftPress = onLeftPress, onLeftRelease = onLeftRelease,
                onRightPress = onRightPress, onRightRelease = onRightRelease,
                onRotate = onRotate
            )

            // Small utility buttons
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SmallButton("⏸", onPause)
                SmallButton("🔊", onToggleSound)
                SmallButton("☰", onOpenSettings)
            }

            if (dpadStyle == DPadStyle.STANDARD)
                RotateButton(onClick = onRotate, size = 68.dp)
            else
                Spacer(Modifier.size(68.dp))
        }
    }
}

// ========== LANDSCAPE ==========

@Composable
private fun LandscapeLayout(
    gs: GameState, layout: LayoutPreset, dpadStyle: DPadStyle, ghost: Boolean,
    anim: AnimationStyle, animDur: Float,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit, onToggleSound: () -> Unit, onOpenSettings: () -> Unit
) {
    val isLefty = layout == LayoutPreset.LANDSCAPE_LEFTY

    Row(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        // Left panel
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (isLefty) LandscapeInfo(gs, onPause, onToggleSound, onOpenSettings)
            else LandscapeControls(gs, dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause)
        }

        // Board
        GameBoard(
            board = gs.board, currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghost, clearingLines = gs.clearedLineRows,
            animationStyle = anim, animationDuration = animDur,
            modifier = Modifier.fillMaxHeight().aspectRatio(0.5f).padding(horizontal = 6.dp)
        )

        // Right panel
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (isLefty) LandscapeControls(gs, dpadStyle, onHardDrop, onHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onRotate, onPause)
            else LandscapeInfo(gs, onPause, onToggleSound, onOpenSettings)
        }
    }
}

@Composable
private fun LandscapeInfo(gs: GameState, onPause: () -> Unit, onToggleSound: () -> Unit, onOpenSettings: () -> Unit) {
    val theme = LocalGameTheme.current

    Column(
        modifier = Modifier.fillMaxHeight().padding(4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Unified score panel
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(theme.screenBackground.copy(alpha = 0.25f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(gs.score.toString().padStart(7, '0'),
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("LV${gs.level}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = theme.textSecondary, fontWeight = FontWeight.Bold)
                Text("${gs.lines}L", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = theme.textSecondary, fontWeight = FontWeight.Bold)
            }
        }

        // Hold
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InfoLabel("HOLD")
            HoldPiecePreview(shape = gs.holdPiece?.shape, isUsed = gs.holdUsed, modifier = Modifier.size(44.dp))
        }

        // Next
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InfoLabel("NEXT")
            gs.nextPieces.take(3).forEachIndexed { i, p ->
                val sz = when (i) { 0 -> 40.dp; 1 -> 32.dp; else -> 26.dp }
                NextPiecePreview(shape = p.shape, modifier = Modifier.size(sz),
                    alpha = when (i) { 0 -> 1f; 1 -> 0.6f; else -> 0.35f })
            }
        }

        // Utility buttons
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SmallButton("⏸", onPause)
            SmallButton("🔊", onToggleSound)
            SmallButton("☰", onOpenSettings)
        }
    }
}

@Composable
private fun LandscapeControls(
    gs: GameState, dpadStyle: DPadStyle,
    onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onRotate: () -> Unit, onPause: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxHeight().padding(4.dp)
    ) {
        WideActionButton("HOLD", onClick = onHold, width = 80.dp, height = 34.dp)

        DPad(
            buttonSize = 50.dp,
            rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHardDrop, onDownPress = onDownPress, onDownRelease = onDownRelease,
            onLeftPress = onLeftPress, onLeftRelease = onLeftRelease,
            onRightPress = onRightPress, onRightRelease = onRightRelease,
            onRotate = onRotate
        )

        if (dpadStyle == DPadStyle.STANDARD)
            RotateButton(onClick = onRotate, size = 60.dp)

        WideActionButton("PAUSE", onClick = onPause, width = 80.dp, height = 34.dp)
    }
}

// ========== Small Reusable ==========

@Composable
private fun InfoLabel(text: String) {
    val theme = LocalGameTheme.current
    Text(text, fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 2.dp))
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    WideActionButton(text, onClick = onClick, width = 44.dp, height = 28.dp)
}

// ========== Overlays ==========

@Composable
private fun MenuOverlay(highScore: Int, onStart: () -> Unit, onSettings: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(theme.backgroundColor), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRICK GAME", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = theme.textPrimary)
            Text("v3.0.0", fontSize = 14.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(24.dp))
            if (highScore > 0) {
                Text("HIGH SCORE: $highScore", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                Spacer(Modifier.height(20.dp))
            }
            WideActionButton("START GAME", onClick = onStart, width = 180.dp, height = 50.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(12.dp))
            WideActionButton("SETTINGS", onClick = onSettings, width = 180.dp, height = 44.dp)
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color.White)
            Spacer(Modifier.height(24.dp))
            WideActionButton("RESUME", onClick = onResume, width = 160.dp, height = 46.dp)
            Spacer(Modifier.height(10.dp))
            WideActionButton("SETTINGS", onClick = onSettings, width = 160.dp, height = 40.dp)
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, level: Int, lines: Int, onRestart: () -> Unit, onMenu: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444))
            Spacer(Modifier.height(16.dp))
            Text("Score: $score", fontSize = 20.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            Text("Level: $level  Lines: $lines", fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = Color.White)
            Spacer(Modifier.height(24.dp))
            WideActionButton("PLAY AGAIN", onClick = onRestart, width = 160.dp, height = 46.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(10.dp))
            WideActionButton("MENU", onClick = onMenu, width = 160.dp, height = 40.dp)
        }
    }
}
