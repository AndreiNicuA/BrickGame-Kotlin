package com.brickgame.tetris.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.LocalGameTheme
import kotlinx.coroutines.delay

enum class LayoutMode { CLASSIC, MODERN, FULLSCREEN }
enum class LandscapeMode { DEFAULT, LEFTY }

@Composable
fun GameScreen(
    gameState: GameState,
    clearingLines: List<Int>,
    @Suppress("UNUSED_PARAMETER") vibrationEnabled: Boolean,
    ghostPieceEnabled: Boolean,
    animationEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    layoutMode: LayoutMode,
    landscapeMode: LandscapeMode = LandscapeMode.DEFAULT,
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
    @Suppress("UNUSED_PARAMETER") onRotateCCW: () -> Unit = {},
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
                gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                landscapeMode,
                onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease,
                onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease,
                onHardDrop, onRotate, onHold, onOpenSettings, onToggleSound
            )
        } else {
            when (layoutMode) {
                LayoutMode.CLASSIC -> ClassicPortraitLayout(
                    gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                    onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease,
                    onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease,
                    onHardDrop, onRotate, onHold, onOpenSettings, onToggleSound
                )
                LayoutMode.MODERN -> ModernPortraitLayout(
                    gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                    onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease,
                    onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease,
                    onHardDrop, onRotate, onHold, onOpenSettings, onToggleSound
                )
                LayoutMode.FULLSCREEN -> FullscreenPortraitLayout(
                    gameState, clearingLines, ghostPieceEnabled, effectiveStyle, animationDuration,
                    onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease,
                    onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease,
                    onHardDrop, onRotate, onHold, onOpenSettings, onToggleSound
                )
            }
        }

        // ── ACTION LABEL POPUP (overlaid, not embedded) ──
        ActionLabelPopup(gameState.lastActionLabel)

        // ── OVERLAYS ──
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

// ══════════════════════════════════════
// ACTION LABEL POPUP — animated overlay
// ══════════════════════════════════════

@Composable
private fun ActionLabelPopup(label: String) {
    var visibleLabel by remember { mutableStateOf("") }
    var showPopup by remember { mutableStateOf(false) }

    LaunchedEffect(label) {
        if (label.isNotEmpty()) {
            visibleLabel = label
            showPopup = true
            delay(1500)
            showPopup = false
        }
    }

    AnimatedVisibility(
        visible = showPopup,
        enter = fadeIn(tween(150)) + scaleIn(tween(200), initialScale = 0.7f),
        exit = fadeOut(tween(400)) + slideOutVertically(tween(400)) { -it / 3 },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.padding(top = 40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    visibleLabel,
                    fontSize = 20.sp, fontWeight = FontWeight.Black,
                    color = actionLabelColor(visibleLabel),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════
// LANDSCAPE — 3-zone, buttons grouped
// ══════════════════════════════════════

@Composable
private fun LandscapeLayout(
    gameState: GameState, clearingLines: List<Int>,
    ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    landscapeMode: LandscapeMode,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit,
    onHold: () -> Unit, onOpenSettings: () -> Unit, onToggleSound: () -> Unit
) {
    val theme = LocalGameTheme.current
    val isLefty = landscapeMode == LandscapeMode.LEFTY

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── ZONE 1: Controls or Info (depending on lefty) ──
        if (isLefty) {
            InfoZone(gameState, theme, 0.28f, onToggleSound, onOpenSettings)
        } else {
            ControlZone(gameState, theme, 0.30f, onHold, onStartGame, onPauseGame,
                onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease,
                onMoveDown, onMoveDownRelease, onHardDrop, onRotate)
        }

        // ── ZONE 2: Game Board (centre) ──
        Box(
            modifier = Modifier.weight(0.42f).fillMaxHeight().padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                board = gameState.board, currentPiece = gameState.currentPiece,
                ghostY = gameState.ghostY, showGhost = ghostPieceEnabled,
                clearingLines = clearingLines, animationStyle = animationStyle,
                animationDuration = animationDuration,
                modifier = Modifier.aspectRatio(0.5f).fillMaxHeight()
            )
        }

        // ── ZONE 3: Info or Controls ──
        if (isLefty) {
            ControlZone(gameState, theme, 0.30f, onHold, onStartGame, onPauseGame,
                onMoveLeft, onMoveLeftRelease, onMoveRight, onMoveRightRelease,
                onMoveDown, onMoveDownRelease, onHardDrop, onRotate)
        } else {
            InfoZone(gameState, theme, 0.28f, onToggleSound, onOpenSettings)
        }
    }
}

@Composable
private fun RowScope.ControlZone(
    gameState: GameState, theme: com.brickgame.tetris.ui.theme.GameTheme, weight: Float,
    onHold: () -> Unit, onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit
) {
    Column(
        modifier = Modifier.weight(weight).fillMaxHeight().padding(4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top: HOLD + START + PAUSE grouped in one row
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WideActionButton("HOLD", onHold, width = 56.dp, height = 30.dp,
                backgroundColor = if (gameState.holdUsed) theme.buttonSecondary.copy(alpha = 0.3f) else theme.buttonSecondary)
            WideActionButton("▶", onStartGame, width = 36.dp, height = 30.dp)
            WideActionButton("❚❚", onPauseGame, width = 36.dp, height = 30.dp,
                enabled = gameState.status == GameStatus.PLAYING)
        }

        // Centre: D-Pad with rotate in centre
        DPad(
            buttonSize = 60.dp, rotateInCenter = true,
            onUpPress = onHardDrop, onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
            onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
            onRightPress = onMoveRight, onRightRelease = onMoveRightRelease,
            onRotate = onRotate
        )
    }
}

@Composable
private fun RowScope.InfoZone(
    gameState: GameState, theme: com.brickgame.tetris.ui.theme.GameTheme, weight: Float,
    onToggleSound: () -> Unit, onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.weight(weight).fillMaxHeight().padding(4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score panel
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(theme.screenBackground.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScoreRow("SCORE", gameState.score.toString().padStart(6, '0'))
            ScoreRow("LEVEL", gameState.level.toString())
            ScoreRow("LINES", gameState.lines.toString())
        }

        // Hold + Next side by side
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 8.sp, color = theme.pixelOn.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                HoldPiecePreview(shape = gameState.holdPiece?.shape, isUsed = gameState.holdUsed,
                    modifier = Modifier.size(36.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", fontSize = 8.sp, color = theme.pixelOn.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                val next = gameState.nextPieces.ifEmpty {
                    gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList()
                }
                next.forEachIndexed { i, piece ->
                    NextPiecePreview(shape = piece.shape,
                        modifier = Modifier.size(if (i == 0) 36.dp else 24.dp).padding(1.dp),
                        alpha = if (i == 0) 1f else 0.5f)
                }
            }
        }

        // Sound + Menu grouped tiny
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WideActionButton("♪", onToggleSound, width = 34.dp, height = 26.dp)
            WideActionButton("☰", onOpenSettings, width = 34.dp, height = 26.dp)
        }
    }
}

// ══════════════════════════════════════
// CLASSIC PORTRAIT — device frame + LCD
// ══════════════════════════════════════

@Composable
private fun ClassicPortraitLayout(
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

    Column(
        Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Device frame
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)).background(theme.deviceColor).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LCD area
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.fillMaxHeight().clip(RoundedCornerShape(6.dp))
                        .background(theme.screenBackground).padding(6.dp)
                ) {
                    GameBoard(
                        board = gameState.board, currentPiece = gameState.currentPiece,
                        ghostY = gameState.ghostY, showGhost = ghostPieceEnabled,
                        clearingLines = clearingLines, animationStyle = animationStyle,
                        animationDuration = animationDuration,
                        modifier = Modifier.aspectRatio(0.5f).fillMaxHeight()
                    )
                    Spacer(Modifier.width(2.dp).fillMaxHeight().background(theme.pixelOn.copy(alpha = 0.2f)))
                    // Side panel
                    Column(
                        Modifier.width(70.dp).fillMaxHeight().padding(6.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InfoBlock("SCORE", gameState.score.toString().padStart(6, '0'))
                        InfoBlock("LEVEL", gameState.level.toString())
                        InfoBlock("LINES", gameState.lines.toString())
                        Text("HOLD", fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        HoldPiecePreview(shape = gameState.holdPiece?.shape, isUsed = gameState.holdUsed,
                            modifier = Modifier.size(36.dp))
                        Text("NEXT", fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        val next = gameState.nextPieces.ifEmpty { gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList() }
                        next.forEachIndexed { i, piece ->
                            NextPiecePreview(shape = piece.shape,
                                modifier = Modifier.size(if (i == 0) 40.dp else 26.dp),
                                alpha = if (i == 0) 1f else 0.5f)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Buttons: all in one compact grouped row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                WideActionButton("HOLD", onHold, width = 58.dp, height = 30.dp)
                WideActionButton("START", onStartGame, width = 58.dp, height = 30.dp)
                WideActionButton("❚❚", onPauseGame, width = 40.dp, height = 30.dp,
                    enabled = gameState.status == GameStatus.PLAYING)
                WideActionButton("♪", onToggleSound, width = 32.dp, height = 30.dp)
                WideActionButton("☰", onOpenSettings, width = 32.dp, height = 30.dp)
            }

            Spacer(Modifier.height(12.dp))

            // D-Pad + Rotate
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically) {
                DPad(buttonSize = 56.dp, onUpPress = onHardDrop,
                    onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                    onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                    onRightPress = onMoveRight, onRightRelease = onMoveRightRelease)
                RotateButton(onClick = onRotate, size = 70.dp)
            }

            Spacer(Modifier.height(8.dp))
            Text("BRICK GAME", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = theme.textPrimary, letterSpacing = 3.sp)
        }
    }
}

// ══════════════════════════════════════
// MODERN PORTRAIT — clean status bar
// ══════════════════════════════════════

@Composable
private fun ModernPortraitLayout(
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

    Column(
        Modifier.fillMaxSize().background(theme.backgroundColor).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status bar: hold | score | level | next — compact single row
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.5f)).padding(10.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 7.sp, color = Color.White.copy(alpha = 0.5f))
                HoldPiecePreview(shape = gameState.holdPiece?.shape, isUsed = gameState.holdUsed,
                    modifier = Modifier.size(28.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gameState.score.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = Color.White)
                Text("SCORE", fontSize = 7.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LVL ${gameState.level}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${gameState.lines} lines", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", fontSize = 7.sp, color = Color.White.copy(alpha = 0.5f))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val next = gameState.nextPieces.ifEmpty { gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList() }
                    next.take(3).forEachIndexed { i, piece ->
                        NextPiecePreview(shape = piece.shape,
                            modifier = Modifier.size(if (i == 0) 26.dp else 20.dp),
                            alpha = if (i == 0) 1f else 0.5f)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Game board
        Box(Modifier.weight(0.65f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            GameBoard(
                board = gameState.board, currentPiece = gameState.currentPiece,
                ghostY = gameState.ghostY, showGhost = ghostPieceEnabled,
                clearingLines = clearingLines, animationStyle = animationStyle,
                animationDuration = animationDuration,
                modifier = Modifier.aspectRatio(0.5f).fillMaxHeight()
            )
        }

        Spacer(Modifier.height(6.dp))

        // All buttons in one compact row
        Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
            WideActionButton("HOLD", onHold, width = 56.dp, height = 32.dp)
            Spacer(Modifier.width(4.dp))
            WideActionButton("START", onStartGame, width = 64.dp, height = 32.dp)
            Spacer(Modifier.width(4.dp))
            WideActionButton("❚❚", onPauseGame, width = 36.dp, height = 32.dp,
                enabled = gameState.status == GameStatus.PLAYING)
            Spacer(Modifier.width(4.dp))
            WideActionButton("♪", onToggleSound, width = 32.dp, height = 32.dp)
            Spacer(Modifier.width(4.dp))
            WideActionButton("☰", onOpenSettings, width = 32.dp, height = 32.dp)
        }

        Spacer(Modifier.height(10.dp))

        // D-Pad + Rotate
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
            DPad(buttonSize = 54.dp, onUpPress = onHardDrop,
                onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight, onRightRelease = onMoveRightRelease)
            RotateButton(onClick = onRotate, size = 68.dp)
        }

        Spacer(Modifier.height(6.dp))
    }
}

// ══════════════════════════════════════
// FULLSCREEN PORTRAIT — max board
// ══════════════════════════════════════

@Composable
private fun FullscreenPortraitLayout(
    gameState: GameState, clearingLines: List<Int>,
    ghostPieceEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    onStartGame: () -> Unit, onPauseGame: () -> Unit,
    onMoveLeft: () -> Unit, onMoveLeftRelease: () -> Unit,
    onMoveRight: () -> Unit, onMoveRightRelease: () -> Unit,
    onMoveDown: () -> Unit, onMoveDownRelease: () -> Unit,
    onHardDrop: () -> Unit, onRotate: () -> Unit,
    onHold: () -> Unit, onOpenSettings: () -> Unit, onToggleSound: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Minimal top bar: hold | score | level | next — all in one tiny row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            HoldPiecePreview(shape = gameState.holdPiece?.shape, isUsed = gameState.holdUsed,
                modifier = Modifier.size(22.dp))
            Text(gameState.score.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = Color.White)
            Text("L${gameState.level}·${gameState.lines}", fontSize = 10.sp, color = Color.Gray)
            val firstNext = gameState.nextPieces.firstOrNull() ?: gameState.effectiveNextPiece
            NextPiecePreview(shape = firstNext?.shape, modifier = Modifier.size(22.dp))
        }

        // Board — maximum size
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            GameBoard(
                board = gameState.board, currentPiece = gameState.currentPiece,
                ghostY = gameState.ghostY, showGhost = ghostPieceEnabled,
                clearingLines = clearingLines, animationStyle = animationStyle,
                animationDuration = animationDuration,
                modifier = Modifier.aspectRatio(0.5f).fillMaxHeight()
            )
        }

        // Controls: D-Pad left, button cluster right
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            DPad(buttonSize = 50.dp, rotateInCenter = true,
                onUpPress = onHardDrop, onDownPress = onMoveDown, onDownRelease = onMoveDownRelease,
                onLeftPress = onMoveLeft, onLeftRelease = onMoveLeftRelease,
                onRightPress = onMoveRight, onRightRelease = onMoveRightRelease,
                onRotate = onRotate)

            // Compact button cluster: 2 rows
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                WideActionButton("HOLD", onHold, width = 60.dp, height = 28.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    WideActionButton("▶", onStartGame, width = 36.dp, height = 26.dp)
                    WideActionButton("❚❚", onPauseGame, width = 36.dp, height = 26.dp,
                        enabled = gameState.status == GameStatus.PLAYING)
                    WideActionButton("♪", onToggleSound, width = 28.dp, height = 26.dp)
                    WideActionButton("☰", onOpenSettings, width = 28.dp, height = 26.dp)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

// ══════════════════════════
// SHARED HELPERS
// ══════════════════════════

@Composable
private fun ScoreRow(label: String, value: String) {
    val theme = LocalGameTheme.current
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), Arrangement.SpaceBetween) {
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

private fun actionLabelColor(label: String): Color = when {
    label.contains("Tetris") -> Color(0xFFF4D03F)
    label.contains("T-Spin") -> Color(0xFFE74C3C)
    label.contains("B2B") -> Color(0xFF3498DB)
    label.contains("Combo") -> Color(0xFF2ECC71)
    else -> Color.White
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                color = Color.White, letterSpacing = 4.sp)
            Spacer(Modifier.height(48.dp))
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4D03F))
                .clickable(onClick = onResume).padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center) {
                Text("RESUME", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF333333))
                .clickable(onClick = onSettings).padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center) {
                Text("SETTINGS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int, level: Int, lines: Int, highScore: Int,
    gameMode: com.brickgame.tetris.game.GameMode, elapsedTimeMs: Long,
    onPlayAgain: () -> Unit
) {
    val isNewHigh = score >= highScore && score > 0
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (gameMode == com.brickgame.tetris.game.GameMode.SPRINT && lines >= 40) "COMPLETE!" else "GAME OVER",
                fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 3.sp)
            if (isNewHigh) {
                Spacer(Modifier.height(12.dp))
                Text("NEW HIGH SCORE!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F))
            }
            Spacer(Modifier.height(28.dp))
            Text(score.toString(), fontSize = 52.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = Color(0xFFF4D03F))
            Text("Level $level  •  $lines lines", fontSize = 18.sp, color = Color.Gray)
            if (elapsedTimeMs > 0) {
                Spacer(Modifier.height(8.dp))
                val sec = elapsedTimeMs / 1000
                val ms = (elapsedTimeMs % 1000) / 10
                Text("Time: ${sec / 60}:${(sec % 60).toString().padStart(2, '0')}.${ms.toString().padStart(2, '0')}",
                    fontSize = 16.sp, color = Color(0xFF3498DB), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4D03F))
                .clickable(onClick = onPlayAgain).padding(horizontal = 40.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center) {
                Text("PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
