package com.brickgame.tetris.ui.screens

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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.layout.ElementType
import com.brickgame.tetris.ui.layout.LayoutElement
import com.brickgame.tetris.ui.layout.LayoutProfile
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.LocalGameTheme

enum class LayoutMode { CLASSIC, MODERN, FULLSCREEN }

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
    activeProfile: LayoutProfile? = null,
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

    val callbacks = GameCallbacks(
        onStartGame, onPauseGame, onMoveLeft, onMoveLeftRelease,
        onMoveRight, onMoveRightRelease, onMoveDown, onMoveDownRelease,
        onHardDrop, onRotate, onHold, onOpenSettings, onToggleSound
    )

    Box(modifier = modifier.fillMaxSize().background(theme.backgroundColor)) {
        // Use the LayoutProfile to render
        if (activeProfile != null) {
            ProfileDrivenLayout(
                profile = activeProfile,
                gameState = gameState,
                clearingLines = clearingLines,
                ghostPieceEnabled = ghostPieceEnabled,
                animationStyle = effectiveStyle,
                animationDuration = animationDuration,
                callbacks = callbacks
            )
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

/** All game callbacks bundled for convenience */
data class GameCallbacks(
    val onStartGame: () -> Unit,
    val onPauseGame: () -> Unit,
    val onMoveLeft: () -> Unit,
    val onMoveLeftRelease: () -> Unit,
    val onMoveRight: () -> Unit,
    val onMoveRightRelease: () -> Unit,
    val onMoveDown: () -> Unit,
    val onMoveDownRelease: () -> Unit,
    val onHardDrop: () -> Unit,
    val onRotate: () -> Unit,
    val onHold: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onToggleSound: () -> Unit
)

// ================================================================
// PROFILE-DRIVEN LAYOUT
// Reads LayoutProfile and places real composables at absolute positions
// ================================================================

@Composable
private fun ProfileDrivenLayout(
    profile: LayoutProfile,
    gameState: GameState,
    clearingLines: List<Int>,
    ghostPieceEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    callbacks: GameCallbacks
) {
    val theme = LocalGameTheme.current
    val density = LocalDensity.current
    var screenSize by remember { mutableStateOf(IntSize(0, 0)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenSize = it.size }
    ) {
        if (screenSize.width == 0) return@Box

        val sw = screenSize.width.toFloat()
        val sh = screenSize.height.toFloat()

        for (elem in profile.elements) {
            if (!elem.isVisible) continue

            val xDp = with(density) { (elem.x * sw).toDp() }
            val yDp = with(density) { (elem.y * sh).toDp() }
            val wDp = with(density) { (elem.w * sw).toDp() }
            val hDp = with(density) { (elem.h * sh).toDp() }

            Box(
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
                    .size(width = wDp, height = hDp)
            ) {
                when (elem.type) {
                    ElementType.GAME_BOARD -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    }

                    ElementType.SCORE_PANEL -> {
                        Column(
                            Modifier.fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.screenBackground.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ScoreRow("SCORE", gameState.score.toString().padStart(6, '0'))
                            ScoreRow("LEVEL", gameState.level.toString())
                            ScoreRow("LINES", gameState.lines.toString())
                        }
                    }

                    ElementType.HOLD_PREVIEW -> {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("HOLD", fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            HoldPiecePreview(
                                shape = gameState.holdPiece?.shape,
                                isUsed = gameState.holdUsed,
                                modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f)
                            )
                        }
                    }

                    ElementType.NEXT_PIECE_QUEUE -> {
                        Column(
                            Modifier.fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.screenBackground.copy(alpha = 0.4f))
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("NEXT", fontSize = 9.sp, color = theme.pixelOn.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(2.dp))
                            val next = gameState.nextPieces.ifEmpty {
                                gameState.effectiveNextPiece?.let { listOf(it) } ?: emptyList()
                            }
                            next.forEachIndexed { i, piece ->
                                NextPiecePreview(
                                    shape = piece.shape,
                                    modifier = Modifier
                                        .fillMaxWidth(if (i == 0) 0.6f else 0.4f)
                                        .aspectRatio(1f)
                                        .padding(1.dp),
                                    alpha = if (i == 0) 1f else 0.5f
                                )
                            }
                        }
                    }

                    ElementType.DPAD -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            // Scale button size based on allocated area
                            val btnSize = with(density) {
                                minOf(elem.w * sw / 3.8f, elem.h * sh / 3.8f).toDp()
                            }
                            DPad(
                                buttonSize = btnSize,
                                rotateInCenter = true,
                                onUpPress = callbacks.onHardDrop,
                                onDownPress = callbacks.onMoveDown,
                                onDownRelease = callbacks.onMoveDownRelease,
                                onLeftPress = callbacks.onMoveLeft,
                                onLeftRelease = callbacks.onMoveLeftRelease,
                                onRightPress = callbacks.onMoveRight,
                                onRightRelease = callbacks.onMoveRightRelease,
                                onRotate = callbacks.onRotate
                            )
                        }
                    }

                    ElementType.ROTATE_BUTTON -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val s = with(density) { minOf(elem.w * sw, elem.h * sh).toDp() * 0.8f }
                            RotateButton(onClick = callbacks.onRotate, size = s, showLabel = hDp > 60.dp)
                        }
                    }

                    ElementType.HOLD_BUTTON -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            WideActionButton(
                                "HOLD", callbacks.onHold,
                                width = wDp - 4.dp, height = hDp - 4.dp,
                                backgroundColor = if (gameState.holdUsed)
                                    theme.buttonSecondary.copy(alpha = 0.3f)
                                else theme.buttonSecondary
                            )
                        }
                    }

                    ElementType.START_BUTTON -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            WideActionButton("START", callbacks.onStartGame,
                                width = wDp - 4.dp, height = hDp - 4.dp)
                        }
                    }

                    ElementType.PAUSE_BUTTON -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            WideActionButton("❚❚", callbacks.onPauseGame,
                                width = wDp - 4.dp, height = hDp - 4.dp,
                                enabled = gameState.status == GameStatus.PLAYING)
                        }
                    }

                    ElementType.SOUND_TOGGLE -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            WideActionButton("♪", callbacks.onToggleSound,
                                width = wDp - 2.dp, height = hDp - 2.dp)
                        }
                    }

                    ElementType.MENU_BUTTON -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            WideActionButton("☰", callbacks.onOpenSettings,
                                width = wDp - 2.dp, height = hDp - 2.dp)
                        }
                    }

                    ElementType.ACTION_LABEL -> {
                        if (gameState.lastActionLabel.isNotEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    gameState.lastActionLabel,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = actionLabelColor(gameState.lastActionLabel, theme.pixelOn),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== Shared helpers =====

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

private fun actionLabelColor(label: String, default: Color): Color = when {
    label.contains("Tetris") -> Color(0xFFF4D03F)
    label.contains("T-Spin") -> Color(0xFFE74C3C)
    label.contains("B2B") -> Color(0xFF3498DB)
    label.contains("Combo") -> Color(0xFF2ECC71)
    else -> default
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
