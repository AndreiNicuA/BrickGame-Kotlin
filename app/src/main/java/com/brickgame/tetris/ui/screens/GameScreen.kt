package com.brickgame.tetris.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onRotateCCW: () -> Unit = {},
    onHold: () -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val effectiveStyle = if (animationEnabled) animationStyle else AnimationStyle.NONE
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = modifier.fillMaxSize().background(theme.backgroundColor)) {
        if (isLandscape) {
            LandscapeLayout(
                gameState = gameState,
                clearingLines = clearingLines,
                ghostPieceEnabled = ghostPieceEnabled,
                animationStyle = effectiveStyle,
                animationDuration = animationDuration,
                onStartGame = onStartGame,
                onPauseGame = onPauseGame,
                onMoveLeft = onMoveLeft,
                onMoveLeftRelease = onMoveLeftRelease,
                onMoveRight = onMoveRight,
                onMoveRightRelease = onMoveRightRelease,
                onMoveDown = onMoveDown,
                onMoveDownRelease = onMoveDownRelease,
                onHardDrop = onHardDrop,
                onRotate = onRotate,
                onRotateCCW = onRotateCCW,
                onHold = onHold,
                onOpenSettings = onOpenSettings,
                onToggleSound = onToggleSound
            )
        } else {
            when (layoutMode) {
                LayoutMode.CLASSIC -> ClassicLayout(
                    gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                    onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight,
                    onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate,
                    onHold, onOpenSettings
                )
                LayoutMode.MODERN -> ModernLayout(
                    gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                    onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight,
                    onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate,
                    onHold, onOpenSettings
                )
                LayoutMode.FULLSCREEN -> FullscreenLayout(
                    gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                    onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight,
                    onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate,
                    onHold, onOpenSettings
                )
            }
        }

        // Overlays
        if (gameState.status == GameStatus.PAUSED) {
            PauseOverlay(onResume = onResumeGame, onSettings = onOpenSettings)
        }
        if (gameState.status == GameStatus.GAME_OVER) {
            GameOverOverlay(
                score = gameState.score, level = gameState.level,
                lines = gameState.lines, highScore = gameState.highScore,
                gameMode = gameState.gameMode, elapsedTimeMs = gameState.elapsedTimeMs,
                onPlayAgain = onStartGame
            )
        }
    }
}

// ===== LANDSCAPE LAYOUT =====

@Composable
private fun LandscapeLayout(
    gameState: GameState, clearingLines: List<Int>,
    ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit, onRotateCCW: () -> Unit,
    onHold: () -> Unit, onOpenSettings: () -> Unit, onToggleSound: () -> Unit
) {
    val theme = LocalGameTheme.current

    Row(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT ZONE (25%)
        Column(
            modifier = Modifier.weight(0.25f).fillMaxHeight().padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score panel
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.screenBackground.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InfoBlock("SCORE", gameState.score.toString().padStart(6, '0'))
                Spacer(modifier = Modifier.height(4.dp))
                InfoBlock("LEVEL", gameState.level.toString())
                Spacer(modifier = Modifier.height(4.dp))
                InfoBlock("LINES", gameState.lines.toString())
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hold piece
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 10.sp, color = theme.pixelOn.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                HoldPiecePreview(
                    shape = gameState.holdPiece?.shape,
                    isUsed = gameState.holdUsed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // D-Pad
            DPad(
                onUpPress = onHardDrop,
                onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight, onRightRelease = onMoveRightRelease,
                buttonSize = 48.dp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CompactButton("HOLD", onHold, width = 48.dp, height = 28.dp)
                CompactButton("START", onStartGame, width = 48.dp, height = 28.dp)
                CompactButton("PAUSE", onPauseGame, width = 48.dp, height = 28.dp,
                    enabled = gameState.status == GameStatus.PLAYING)
            }
        }

        // CENTER ZONE (50%)
        Column(
            modifier = Modifier.weight(0.50f).fillMaxHeight().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Action label
            if (gameState.lastActionLabel.isNotEmpty()) {
                Text(
                    gameState.lastActionLabel,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = when {
                        gameState.lastActionLabel.contains("Tetris") -> Color(0xFFF4D03F)
                        gameState.lastActionLabel.contains("T-Spin") -> Color(0xFFE74C3C)
                        gameState.lastActionLabel.contains("B2B") -> Color(0xFF3498DB)
                        else -> theme.pixelOn
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Game board
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GameBoard(
                    board = gameState.board,
                    currentPiece = gameState.currentPiece,
                    ghostY = gameState.ghostY,
                    showGhost = ghostPieceEnabled,
                    clearingLines = clearingLines,
                    animationStyle = animationStyle,
                    animationDuration = animationDuration,
                    modifier = Modifier.aspectRatio(0.5f).fillMaxHeight()
                )
            }

            // Bottom bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactButton("♪", onToggleSound, width = 32.dp, height = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                CompactButton("☰", onOpenSettings, width = 32.dp, height = 24.dp)
            }
        }

        // RIGHT ZONE (25%)
        Column(
            modifier = Modifier.weight(0.25f).fillMaxHeight().padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Next piece queue
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.screenBackground.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NEXT", fontSize = 10.sp, color = theme.pixelOn.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val nextPieces = gameState.nextPieces.ifEmpty {
                    gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList()
                }
                nextPieces.forEachIndexed { index, piece ->
                    val previewSize = if (index == 0) 44.dp else 32.dp
                    val alpha = if (index == 0) 1f else 0.6f
                    NextPiecePreview(
                        shape = piece.shape,
                        modifier = Modifier.size(previewSize).padding(2.dp),
                        alpha = alpha
                    )
                    if (index < nextPieces.lastIndex) Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rotate button (large)
            RotateButton(onClick = onRotate, size = 64.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Hard drop button
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.buttonPrimary)
                    .clickable(onClick = onHardDrop),
                contentAlignment = Alignment.Center
            ) {
                Text("▼ DROP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }
        }
    }
}

// ===== PORTRAIT LAYOUTS =====

@Composable
private fun ClassicLayout(
    gameState: GameState, clearingLines: List<Int>,
    ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit,
    onHold: () -> Unit, onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.deviceColor)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LCD Screen
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.screenBackground)
                        .padding(6.dp)
                ) {
                    GameBoard(
                        board = gameState.board,
                        currentPiece = gameState.currentPiece,
                        ghostY = gameState.ghostY,
                        showGhost = ghostPieceEnabled,
                        clearingLines = clearingLines,
                        animationStyle = animationStyle,
                        animationDuration = animationDuration,
                        modifier = Modifier.aspectRatio(0.5f).fillMaxHeight()
                    )

                    Spacer(modifier = Modifier.width(2.dp)
                        .fillMaxHeight()
                        .background(theme.pixelOn.copy(alpha = 0.2f)))

                    // Side panel
                    Column(
                        modifier = Modifier.width(70.dp).fillMaxHeight().padding(6.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InfoBlock("SCORE", gameState.score.toString().padStart(6, '0'))
                        InfoBlock("LEVEL", gameState.level.toString())
                        InfoBlock("LINES", gameState.lines.toString())

                        // Hold piece
                        Text("HOLD", fontSize = 10.sp, color = theme.pixelOn.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold)
                        HoldPiecePreview(
                            shape = gameState.holdPiece?.shape,
                            isUsed = gameState.holdUsed,
                            modifier = Modifier.size(36.dp)
                        )

                        Text("NEXT", fontSize = 10.sp, color = theme.pixelOn.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold)
                        // Show next-3
                        val nextPieces = gameState.nextPieces.ifEmpty {
                            gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList()
                        }
                        nextPieces.forEachIndexed { index, piece ->
                            val size = if (index == 0) 40.dp else 28.dp
                            NextPiecePreview(
                                shape = piece.shape,
                                modifier = Modifier.size(size),
                                alpha = if (index == 0) 1f else 0.5f
                            )
                        }

                        // Action label
                        if (gameState.lastActionLabel.isNotEmpty()) {
                            Text(
                                gameState.lastActionLabel,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF4D03F),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton("HOLD", onHold)
                ActionButton("START", onStartGame)
                ActionButton("PAUSE", onPauseGame,
                    enabled = gameState.status == GameStatus.PLAYING)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPad(
                    onUpPress = onHardDrop,
                    onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                    onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                    onRightPress = onMoveRight, onRightRelease = onMoveRightRelease,
                    buttonSize = 58.dp
                )
                RotateButton(onClick = onRotate, size = 72.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("BRICK GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = theme.textPrimary, letterSpacing = 3.sp)
            Spacer(modifier = Modifier.height(8.dp))
            MenuButton(onClick = onOpenSettings)
        }
    }
}

@Composable
private fun ModernLayout(
    gameState: GameState, clearingLines: List<Int>,
    ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit,
    onHold: () -> Unit, onOpenSettings: () -> Unit
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
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hold
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                HoldPiecePreview(
                    shape = gameState.holdPiece?.shape,
                    isUsed = gameState.holdUsed,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gameState.score.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = Color.White)
                Text("SCORE", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LVL ${gameState.level}", fontSize = 14.sp, color = Color.White,
                    fontWeight = FontWeight.Bold)
                Text("${gameState.lines} lines", fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f))
            }

            // Next queue (compact)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                val nextPieces = gameState.nextPieces.ifEmpty {
                    gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    nextPieces.take(3).forEachIndexed { i, piece ->
                        NextPiecePreview(
                            shape = piece.shape,
                            modifier = Modifier.size(if (i == 0) 28.dp else 22.dp),
                            alpha = if (i == 0) 1f else 0.5f
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action label
        if (gameState.lastActionLabel.isNotEmpty()) {
            Text(
                gameState.lastActionLabel,
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = when {
                    gameState.lastActionLabel.contains("Tetris") -> Color(0xFFF4D03F)
                    gameState.lastActionLabel.contains("T-Spin") -> Color(0xFFE74C3C)
                    gameState.lastActionLabel.contains("B2B") -> Color(0xFF3498DB)
                    else -> Color.White
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Game board
        Box(
            modifier = Modifier.weight(0.65f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                board = gameState.board,
                currentPiece = gameState.currentPiece,
                ghostY = gameState.ghostY,
                showGhost = ghostPieceEnabled,
                clearingLines = clearingLines,
                animationStyle = animationStyle,
                animationDuration = animationDuration,
                modifier = Modifier.aspectRatio(0.5f).fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FullscreenButton("HOLD", onHold, width = 70.dp, height = 40.dp)
            Spacer(modifier = Modifier.width(8.dp))
            FullscreenButton("START", onStartGame, width = 80.dp, height = 40.dp)
            Spacer(modifier = Modifier.width(8.dp))
            FullscreenButton("PAUSE", onPauseGame, width = 80.dp, height = 40.dp,
                enabled = gameState.status == GameStatus.PLAYING)
            Spacer(modifier = Modifier.width(8.dp))
            FullscreenButton("MENU", onOpenSettings, width = 60.dp, height = 40.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(
                onUpPress = onHardDrop,
                onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight, onRightRelease = onMoveRightRelease,
                buttonSize = 56.dp
            )
            RotateButton(onClick = onRotate, size = 72.dp)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun FullscreenLayout(
    gameState: GameState, clearingLines: List<Int>,
    ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit,
    onHold: () -> Unit, onOpenSettings: () -> Unit
) {
    // Full-screen mode just uses the Modern layout (same thing)
    ModernLayout(
        gameState, clearingLines, ghostPieceEnabled, animationStyle, animationDuration,
        onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight,
        onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate,
        onHold, onOpenSettings
    )
}

// ===== Shared Components =====

@Composable
private fun InfoBlock(label: String, value: String) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = theme.pixelOn.copy(alpha = 0.5f),
            letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, color = theme.pixelOn)
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f))
    }
}

@Composable
private fun CompactButton(
    text: String, onClick: () -> Unit,
    width: Dp, height: Dp, enabled: Boolean = true
) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .width(width).height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) theme.buttonSecondary else theme.buttonSecondary.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f))
    }
}

@Composable
private fun FullscreenButton(
    text: String, onClick: () -> Unit,
    width: Dp, height: Dp, enabled: Boolean = true
) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .width(width).height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) theme.buttonSecondary else theme.buttonSecondary.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f))
    }
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(theme.buttonSecondary.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("☰ MENU", fontSize = 16.sp, color = theme.textSecondary,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                color = Color.White, letterSpacing = 4.sp)
            Spacer(modifier = Modifier.height(48.dp))
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
private fun GameOverOverlay(
    score: Int, level: Int, lines: Int, highScore: Int,
    gameMode: com.brickgame.tetris.game.GameMode,
    elapsedTimeMs: Long,
    onPlayAgain: () -> Unit
) {
    val isNewHighScore = score >= highScore && score > 0

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (gameMode == com.brickgame.tetris.game.GameMode.SPRINT && lines >= 40) "COMPLETE!" else "GAME OVER",
                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                color = Color.White, letterSpacing = 3.sp
            )

            if (isNewHighScore) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("NEW HIGH SCORE!", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFF4D03F))
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(score.toString(), fontSize = 52.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = Color(0xFFF4D03F))
            Text("Level $level  •  $lines lines", fontSize = 18.sp, color = Color.Gray)

            // Show time for sprint/ultra modes
            if (elapsedTimeMs > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val seconds = elapsedTimeMs / 1000
                val millis = (elapsedTimeMs % 1000) / 10
                Text(
                    "Time: ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}.${millis.toString().padStart(2, '0')}",
                    fontSize = 16.sp, color = Color(0xFF3498DB), fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4D03F))
                    .clickable(onClick = onPlayAgain)
                    .padding(horizontal = 40.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
