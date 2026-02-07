package com.brickgame.tetris.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
    ) {
        when {
            gameState.status == GameStatus.MENU -> {
                MenuOverlay(
                    highScore = gameState.highScore,
                    onStartGame = onStartGame,
                    onOpenSettings = onOpenSettings
                )
            }
            else -> {
                val isLandscape = layoutPreset.isLandscape
                if (isLandscape) {
                    LandscapeGameLayout(
                        gameState = gameState,
                        layoutPreset = layoutPreset,
                        dpadStyle = dpadStyle,
                        ghostEnabled = ghostEnabled,
                        animationStyle = animationStyle,
                        animationDuration = animationDuration,
                        onRotate = onRotate,
                        onHardDrop = onHardDrop,
                        onHold = onHold,
                        onLeftPress = onLeftPress,
                        onLeftRelease = onLeftRelease,
                        onRightPress = onRightPress,
                        onRightRelease = onRightRelease,
                        onDownPress = onDownPress,
                        onDownRelease = onDownRelease,
                        onPause = onPause,
                        onToggleSound = onToggleSound,
                        onOpenSettings = onOpenSettings
                    )
                } else {
                    PortraitGameLayout(
                        gameState = gameState,
                        layoutPreset = layoutPreset,
                        dpadStyle = dpadStyle,
                        ghostEnabled = ghostEnabled,
                        animationStyle = animationStyle,
                        animationDuration = animationDuration,
                        onRotate = onRotate,
                        onHardDrop = onHardDrop,
                        onHold = onHold,
                        onLeftPress = onLeftPress,
                        onLeftRelease = onLeftRelease,
                        onRightPress = onRightPress,
                        onRightRelease = onRightRelease,
                        onDownPress = onDownPress,
                        onDownRelease = onDownRelease,
                        onPause = onPause,
                        onToggleSound = onToggleSound,
                        onOpenSettings = onOpenSettings,
                        onStartGame = onStartGame
                    )
                }

                // Pause overlay
                if (gameState.status == GameStatus.PAUSED) {
                    PauseOverlay(onResume = onResume, onOpenSettings = onOpenSettings)
                }

                // Game over overlay
                if (gameState.status == GameStatus.GAME_OVER) {
                    GameOverOverlay(
                        score = gameState.score,
                        level = gameState.level,
                        lines = gameState.lines,
                        onRestart = onStartGame,
                        onMenu = onOpenSettings
                    )
                }
            }
        }

        // Action label
        if (gameState.lastActionLabel.isNotEmpty() &&
            gameState.status == GameStatus.PLAYING) {
            ActionLabel(
                text = gameState.lastActionLabel,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
            )
        }
    }
}

// ===== Portrait Layouts =====

@Composable
private fun PortraitGameLayout(
    gameState: GameState,
    layoutPreset: LayoutPreset,
    dpadStyle: DPadStyle,
    ghostEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    onRotate: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onLeftPress: () -> Unit,
    onLeftRelease: () -> Unit,
    onRightPress: () -> Unit,
    onRightRelease: () -> Unit,
    onDownPress: () -> Unit,
    onDownRelease: () -> Unit,
    onPause: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartGame: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top info bar: Hold | Score/Level/Lines | Next
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Hold piece
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 10.sp, color = theme.textSecondary,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                HoldPiecePreview(
                    shape = gameState.holdPiece?.shape,
                    isUsed = gameState.holdUsed,
                    modifier = Modifier.size(52.dp)
                )
            }

            // Score panel
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreLCD(label = "SCORE", value = gameState.score.toString())
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ScoreLCD(label = "LEVEL", value = gameState.level.toString(), small = true)
                    ScoreLCD(label = "LINES", value = gameState.lines.toString(), small = true)
                }
            }

            // Next queue
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", fontSize = 10.sp, color = theme.textSecondary,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                gameState.nextPieces.take(3).forEachIndexed { i, piece ->
                    val size = when (i) { 0 -> 48.dp; 1 -> 38.dp; else -> 32.dp }
                    val alpha = when (i) { 0 -> 1f; 1 -> 0.7f; else -> 0.4f }
                    NextPiecePreview(
                        shape = piece.shape,
                        modifier = Modifier.size(size).padding(vertical = 1.dp),
                        alpha = alpha
                    )
                }
            }
        }

        // Game board
        GameBoard(
            board = gameState.board,
            currentPiece = gameState.currentPiece,
            ghostY = gameState.ghostY,
            showGhost = ghostEnabled,
            clearingLines = gameState.clearedLineRows,
            animationStyle = animationStyle,
            animationDuration = animationDuration,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.5f)
                .padding(vertical = 4.dp)
        )

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WideActionButton("HOLD", onClick = onHold, width = 80.dp, height = 36.dp)
            WideActionButton("START", onClick = { if (gameState.status == GameStatus.MENU) onStartGame() else onPause() },
                width = 80.dp, height = 36.dp)
        }

        // Controls row: D-Pad left, small buttons middle, Rotate right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(
                buttonSize = 56.dp,
                rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHardDrop,
                onDownPress = onDownPress,
                onDownRelease = onDownRelease,
                onLeftPress = onLeftPress,
                onLeftRelease = onLeftRelease,
                onRightPress = onRightPress,
                onRightRelease = onRightRelease,
                onRotate = onRotate
            )

            // Small buttons vertically stacked in the middle
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WideActionButton("⏸", onClick = onPause, width = 48.dp, height = 30.dp)
                WideActionButton("🔊", onClick = onToggleSound, width = 48.dp, height = 30.dp)
                WideActionButton("☰", onClick = onOpenSettings, width = 48.dp, height = 30.dp)
            }

            // Rotate button (only if D-Pad doesn't have rotate centre)
            if (dpadStyle == DPadStyle.STANDARD) {
                RotateButton(onClick = onRotate, size = 68.dp)
            } else {
                // Placeholder for balance
                Spacer(Modifier.size(68.dp))
            }
        }
    }
}

// ===== Landscape Layouts =====

@Composable
private fun LandscapeGameLayout(
    gameState: GameState,
    layoutPreset: LayoutPreset,
    dpadStyle: DPadStyle,
    ghostEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    onRotate: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onLeftPress: () -> Unit,
    onLeftRelease: () -> Unit,
    onRightPress: () -> Unit,
    onRightRelease: () -> Unit,
    onDownPress: () -> Unit,
    onDownRelease: () -> Unit,
    onPause: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    val isLefty = layoutPreset == LayoutPreset.LANDSCAPE_LEFTY

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Left zone
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (isLefty) {
                // Lefty: info on left
                InfoColumn(gameState, onPause, onToggleSound, onOpenSettings)
            } else {
                // Default: controls on left
                ControlsColumn(
                    dpadStyle = dpadStyle,
                    onHardDrop = onHardDrop,
                    onHold = onHold,
                    onLeftPress = onLeftPress,
                    onLeftRelease = onLeftRelease,
                    onRightPress = onRightPress,
                    onRightRelease = onRightRelease,
                    onDownPress = onDownPress,
                    onDownRelease = onDownRelease,
                    onRotate = onRotate,
                    onPause = onPause
                )
            }
        }

        // Centre: Game board
        GameBoard(
            board = gameState.board,
            currentPiece = gameState.currentPiece,
            ghostY = gameState.ghostY,
            showGhost = ghostEnabled,
            clearingLines = gameState.clearedLineRows,
            animationStyle = animationStyle,
            animationDuration = animationDuration,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(0.5f)
                .padding(horizontal = 8.dp)
        )

        // Right zone
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (isLefty) {
                // Lefty: controls on right
                ControlsColumn(
                    dpadStyle = dpadStyle,
                    onHardDrop = onHardDrop,
                    onHold = onHold,
                    onLeftPress = onLeftPress,
                    onLeftRelease = onLeftRelease,
                    onRightPress = onRightPress,
                    onRightRelease = onRightRelease,
                    onDownPress = onDownPress,
                    onDownRelease = onDownRelease,
                    onRotate = onRotate,
                    onPause = onPause
                )
            } else {
                // Default: info on right
                InfoColumn(gameState, onPause, onToggleSound, onOpenSettings)
            }
        }
    }
}

@Composable
private fun InfoColumn(
    gameState: GameState,
    onPause: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(
        modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score
        ScoreLCD(label = "SCORE", value = gameState.score.toString())
        ScoreLCD(label = "LEVEL", value = gameState.level.toString(), small = true)
        ScoreLCD(label = "LINES", value = gameState.lines.toString(), small = true)

        // Hold
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("HOLD", fontSize = 10.sp, color = theme.textSecondary,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            HoldPiecePreview(
                shape = gameState.holdPiece?.shape,
                isUsed = gameState.holdUsed,
                modifier = Modifier.size(48.dp)
            )
        }

        // Next
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NEXT", fontSize = 10.sp, color = theme.textSecondary,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            gameState.nextPieces.take(3).forEachIndexed { i, piece ->
                val size = when (i) { 0 -> 44.dp; 1 -> 36.dp; else -> 30.dp }
                val alpha = when (i) { 0 -> 1f; 1 -> 0.7f; else -> 0.4f }
                NextPiecePreview(
                    shape = piece.shape,
                    modifier = Modifier.size(size),
                    alpha = alpha
                )
            }
        }

        // Small buttons
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WideActionButton("⏸", onClick = onPause, width = 50.dp, height = 28.dp)
            WideActionButton("🔊", onClick = onToggleSound, width = 50.dp, height = 28.dp)
            WideActionButton("☰", onClick = onOpenSettings, width = 50.dp, height = 28.dp)
        }
    }
}

@Composable
private fun ControlsColumn(
    dpadStyle: DPadStyle,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onLeftPress: () -> Unit,
    onLeftRelease: () -> Unit,
    onRightPress: () -> Unit,
    onRightRelease: () -> Unit,
    onDownPress: () -> Unit,
    onDownRelease: () -> Unit,
    onRotate: () -> Unit,
    onPause: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp)
    ) {
        WideActionButton("HOLD", onClick = onHold, width = 80.dp, height = 34.dp)

        DPad(
            buttonSize = 52.dp,
            rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHardDrop,
            onDownPress = onDownPress,
            onDownRelease = onDownRelease,
            onLeftPress = onLeftPress,
            onLeftRelease = onLeftRelease,
            onRightPress = onRightPress,
            onRightRelease = onRightRelease,
            onRotate = onRotate
        )

        if (dpadStyle == DPadStyle.STANDARD) {
            RotateButton(onClick = onRotate, size = 64.dp)
        }

        WideActionButton("START", onClick = onPause, width = 80.dp, height = 34.dp)
    }
}

// ===== Reusable Sub-Components =====

@Composable
private fun ScoreLCD(label: String, value: String, small: Boolean = false) {
    val theme = LocalGameTheme.current
    val fontSize = if (small) 14.sp else 20.sp
    val labelSize = if (small) 8.sp else 10.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = labelSize, color = theme.textSecondary,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(theme.screenBackground.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = value.padStart(if (small) 4 else 7, '0'),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = theme.pixelOn,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun ActionLabel(text: String, modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.accentColor.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        color = Color.Black
    )
}

// ===== Overlays =====

@Composable
private fun MenuOverlay(
    highScore: Int,
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current

    Box(
        modifier = Modifier.fillMaxSize().background(theme.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRICK GAME", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = theme.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text("v3.0.0", fontSize = 14.sp, color = theme.textSecondary,
                fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(24.dp))

            if (highScore > 0) {
                Text("HIGH SCORE: $highScore", fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace, color = theme.accentColor)
                Spacer(Modifier.height(24.dp))
            }

            WideActionButton("START GAME", onClick = onStartGame,
                width = 180.dp, height = 50.dp,
                backgroundColor = theme.accentColor)
            Spacer(Modifier.height(12.dp))
            WideActionButton("SETTINGS", onClick = onOpenSettings,
                width = 180.dp, height = 44.dp)
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color.White)
            Spacer(Modifier.height(24.dp))
            WideActionButton("RESUME", onClick = onResume,
                width = 160.dp, height = 46.dp)
            Spacer(Modifier.height(10.dp))
            WideActionButton("SETTINGS", onClick = onOpenSettings,
                width = 160.dp, height = 40.dp)
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    level: Int,
    lines: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    val theme = LocalGameTheme.current

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444))
            Spacer(Modifier.height(16.dp))
            Text("Score: $score", fontSize = 20.sp, fontFamily = FontFamily.Monospace,
                color = theme.accentColor)
            Text("Level: $level  Lines: $lines", fontSize = 14.sp,
                fontFamily = FontFamily.Monospace, color = Color.White)
            Spacer(Modifier.height(24.dp))
            WideActionButton("PLAY AGAIN", onClick = onRestart,
                width = 160.dp, height = 46.dp,
                backgroundColor = theme.accentColor)
            Spacer(Modifier.height(10.dp))
            WideActionButton("MENU", onClick = onMenu,
                width = 160.dp, height = 40.dp)
        }
    }
}
