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

@Suppress("unused")
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
                onHold = onHold,
                onOpenSettings = onOpenSettings,
                onToggleSound = onToggleSound
            )
        } else {
            when (layoutMode) {
                LayoutMode.CLASSIC -> ClassicPortraitLayout(
                    gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                    onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight,
                    onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate,
                    onHold, onOpenSettings
                )
                LayoutMode.MODERN -> ModernPortraitLayout(
                    gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                    onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease, onMoveRight,
                    onMoveRightRelease, onMoveDown, onMoveDownRelease, onHardDrop, onRotate,
                    onHold, onOpenSettings
                )
                LayoutMode.FULLSCREEN -> FullscreenPortraitLayout(
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

// ===========================
// LANDSCAPE - Full screen usage
// Layout: [Left controls] [Board] [Right info+controls]
// Board takes maximum vertical space, controls fill remaining width
// ===========================

@Composable
private fun LandscapeLayout(
    gameState: GameState, clearingLines: List<Int>,
    ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit,
    onHold: () -> Unit, onOpenSettings: () -> Unit, onToggleSound: () -> Unit
) {
    val theme = LocalGameTheme.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT ZONE - D-Pad with rotate in center + hold/start
        Column(
            modifier = Modifier
                .weight(0.30f)
                .fillMaxHeight()
                .padding(start = 4.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hold button at top-left
            WideActionButton(
                text = "HOLD",
                onClick = onHold,
                width = 80.dp,
                height = 36.dp,
                backgroundColor = if (gameState.holdUsed)
                    theme.buttonSecondary.copy(alpha = 0.3f)
                else theme.buttonSecondary
            )

            // D-Pad with rotate in center - BIG buttons
            DPad(
                buttonSize = 62.dp,
                rotateInCenter = true,
                onUpPress = onHardDrop,
                onDownPress = onMoveDown,
                onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft,
                onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight,
                onRightRelease = onMoveRightRelease,
                onRotate = onRotate
            )

            // Start / Pause row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WideActionButton("▶ START", onStartGame, width = 84.dp, height = 32.dp)
                WideActionButton(
                    "❚❚", onPauseGame, width = 50.dp, height = 32.dp,
                    enabled = gameState.status == GameStatus.PLAYING
                )
            }
        }

        // CENTER - Game board taking maximum space
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Action label above board
            if (gameState.lastActionLabel.isNotEmpty()) {
                Text(
                    gameState.lastActionLabel,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = actionLabelColor(gameState.lastActionLabel, theme.pixelOn),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                )
            }

            // Game board - fills available height
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                    modifier = Modifier
                        .aspectRatio(0.5f)
                        .fillMaxHeight()
                )
            }
        }

        // RIGHT ZONE - Score + Next + Hold preview + menu
        Column(
            modifier = Modifier
                .weight(0.28f)
                .fillMaxHeight()
                .padding(start = 2.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score panel (compact)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.screenBackground.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScoreRow("SCORE", gameState.score.toString().padStart(6, '0'))
                ScoreRow("LEVEL", gameState.level.toString())
                ScoreRow("LINES", gameState.lines.toString())
            }

            // Hold piece preview
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(2.dp))
                HoldPiecePreview(
                    shape = gameState.holdPiece?.shape,
                    isUsed = gameState.holdUsed,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Next piece queue
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.screenBackground.copy(alpha = 0.4f))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NEXT", fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val nextPieces = gameState.nextPieces.ifEmpty {
                    gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList()
                }
                nextPieces.forEachIndexed { index, piece ->
                    NextPiecePreview(
                        shape = piece.shape,
                        modifier = Modifier
                            .size(if (index == 0) 40.dp else 28.dp)
                            .padding(2.dp),
                        alpha = if (index == 0) 1f else 0.5f
                    )
                    if (index < nextPieces.lastIndex) Spacer(modifier = Modifier.height(2.dp))
                }
            }

            // Bottom buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WideActionButton("♪", onToggleSound, width = 36.dp, height = 28.dp)
                WideActionButton("☰", onOpenSettings, width = 36.dp, height = 28.dp)
            }
        }
    }
}

// ===========================
// PORTRAIT LAYOUTS
// ===========================

@Composable
private fun ClassicPortraitLayout(
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
        // Device frame
        Column(
            modifier = Modifier
                .weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.deviceColor)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LCD
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

                        Text("HOLD", fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold)
                        HoldPiecePreview(
                            shape = gameState.holdPiece?.shape,
                            isUsed = gameState.holdUsed,
                            modifier = Modifier.size(36.dp)
                        )

                        Text("NEXT", fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold)
                        val nextPieces = gameState.nextPieces.ifEmpty {
                            gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList()
                        }
                        nextPieces.forEachIndexed { i, piece ->
                            NextPiecePreview(
                                shape = piece.shape,
                                modifier = Modifier.size(if (i == 0) 40.dp else 26.dp),
                                alpha = if (i == 0) 1f else 0.5f
                            )
                        }

                        if (gameState.lastActionLabel.isNotEmpty()) {
                            Text(
                                gameState.lastActionLabel, fontSize = 8.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F),
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
                WideActionButton("HOLD", onHold, width = 70.dp, height = 34.dp)
                WideActionButton("START", onStartGame, width = 70.dp, height = 34.dp)
                WideActionButton("PAUSE", onPauseGame, width = 70.dp, height = 34.dp,
                    enabled = gameState.status == GameStatus.PLAYING)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D-Pad + Rotate
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPad(
                    buttonSize = 58.dp,
                    onUpPress = onHardDrop,
                    onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                    onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                    onRightPress = onMoveRight, onRightRelease = onMoveRightRelease
                )
                RotateButton(onClick = onRotate, size = 72.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("BRICK GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = theme.textPrimary, letterSpacing = 3.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Menu button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.buttonSecondary.copy(alpha = 0.6f))
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("☰ MENU", fontSize = 16.sp, color = theme.textSecondary,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ModernPortraitLayout(
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                HoldPiecePreview(
                    shape = gameState.holdPiece?.shape,
                    isUsed = gameState.holdUsed,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gameState.score.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = Color.White)
                Text("SCORE", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LVL ${gameState.level}", fontSize = 14.sp, color = Color.White,
                    fontWeight = FontWeight.Bold)
                Text("${gameState.lines} lines", fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val nextPieces = gameState.nextPieces.ifEmpty {
                        gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList()
                    }
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

        Spacer(modifier = Modifier.height(6.dp))

        // Action label
        if (gameState.lastActionLabel.isNotEmpty()) {
            Text(
                gameState.lastActionLabel,
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = actionLabelColor(gameState.lastActionLabel, Color.White)
            )
            Spacer(modifier = Modifier.height(3.dp))
        }

        // Game board - fills most of the screen
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

        Spacer(modifier = Modifier.height(10.dp))

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WideActionButton("HOLD", onHold, width = 68.dp, height = 36.dp)
            Spacer(modifier = Modifier.width(6.dp))
            WideActionButton("START", onStartGame, width = 76.dp, height = 36.dp)
            Spacer(modifier = Modifier.width(6.dp))
            WideActionButton("PAUSE", onPauseGame, width = 76.dp, height = 36.dp,
                enabled = gameState.status == GameStatus.PLAYING)
            Spacer(modifier = Modifier.width(6.dp))
            WideActionButton("☰", onOpenSettings, width = 40.dp, height = 36.dp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Controls - D-Pad + Rotate
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(
                buttonSize = 56.dp,
                onUpPress = onHardDrop,
                onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight, onRightRelease = onMoveRightRelease
            )
            RotateButton(onClick = onRotate, size = 72.dp)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// ===== FULLSCREEN PORTRAIT - Maximum board, minimal chrome =====

@Composable
private fun FullscreenPortraitLayout(
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
            .background(Color.Black)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Minimal floating stats - single row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hold piece (tiny)
            HoldPiecePreview(
                shape = gameState.holdPiece?.shape,
                isUsed = gameState.holdUsed,
                modifier = Modifier.size(24.dp)
            )

            // Score
            Text(
                gameState.score.toString(),
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = Color.White
            )

            // Level/Lines compact
            Text(
                "L${gameState.level} · ${gameState.lines}",
                fontSize = 11.sp, color = Color.Gray
            )

            // Next piece (tiny, just first)
            val firstNext = gameState.nextPieces.firstOrNull()
                ?: gameState.effectiveNextPiece
            NextPiecePreview(
                shape = firstNext?.shape,
                modifier = Modifier.size(24.dp)
            )
        }

        // Action label
        if (gameState.lastActionLabel.isNotEmpty()) {
            Text(
                gameState.lastActionLabel,
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = actionLabelColor(gameState.lastActionLabel, Color.White)
            )
        }

        // Game board - MAXIMUM SIZE
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

        // Controls - compact, edge to edge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left: D-Pad with rotate center
            DPad(
                buttonSize = 52.dp,
                rotateInCenter = true,
                onUpPress = onHardDrop,
                onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight, onRightRelease = onMoveRightRelease,
                onRotate = onRotate
            )

            // Right: stacked action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WideActionButton("HOLD", onHold, width = 64.dp, height = 32.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WideActionButton("▶", onStartGame, width = 40.dp, height = 28.dp)
                    WideActionButton("❚❚", onPauseGame, width = 40.dp, height = 28.dp,
                        enabled = gameState.status == GameStatus.PLAYING)
                    WideActionButton("☰", onOpenSettings, width = 40.dp, height = 28.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}

// ===== Shared =====

@Composable
private fun ScoreRow(label: String, value: String) {
    val theme = LocalGameTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, color = theme.pixelOn)
    }
}

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

private fun actionLabelColor(label: String, defaultColor: Color): Color = when {
    label.contains("Tetris") -> Color(0xFFF4D03F)
    label.contains("T-Spin") -> Color(0xFFE74C3C)
    label.contains("B2B") -> Color(0xFF3498DB)
    label.contains("Combo") -> Color(0xFF2ECC71)
    else -> defaultColor
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
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4D03F))
                    .clickable(onClick = onResume).padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) { Text("RESUME", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF333333))
                    .clickable(onClick = onSettings).padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) { Text("SETTINGS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
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
            if (elapsedTimeMs > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val sec = elapsedTimeMs / 1000
                val ms = (elapsedTimeMs % 1000) / 10
                Text(
                    "Time: ${sec / 60}:${(sec % 60).toString().padStart(2, '0')}.${ms.toString().padStart(2, '0')}",
                    fontSize = 16.sp, color = Color(0xFF3498DB), fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4D03F))
                    .clickable(onClick = onPlayAgain).padding(horizontal = 40.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
        }
    }
}
