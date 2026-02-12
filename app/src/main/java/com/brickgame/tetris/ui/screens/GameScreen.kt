package com.brickgame.tetris.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.brickgame.tetris.game.*
import com.brickgame.tetris.data.CustomLayoutData
import com.brickgame.tetris.data.ElementPosition
import com.brickgame.tetris.data.FreeformElement
import com.brickgame.tetris.game.GameMode
import com.brickgame.tetris.data.LayoutElements
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.components.LocalButtonShape
import com.brickgame.tetris.ui.layout.ButtonShape
import com.brickgame.tetris.ui.layout.DPadStyle
import com.brickgame.tetris.ui.layout.FreeformGameLayout
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.LocalGameTheme

// Color helper
private fun Color.darken(f: Float) = Color((red * (1 - f)).coerceIn(0f, 1f), (green * (1 - f)).coerceIn(0f, 1f), (blue * (1 - f)).coerceIn(0f, 1f), alpha)

// CompositionLocal for multicolor mode — avoids threading through every layout function
val LocalMultiColor = compositionLocalOf { false }

@Composable
fun GameScreen(
    gameState: GameState,
    layoutPreset: LayoutPreset,
    dpadStyle: DPadStyle,
    ghostEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    multiColor: Boolean = false,
    customLayout: CustomLayoutData? = null,
    scoreHistory: List<com.brickgame.tetris.data.ScoreEntry> = emptyList(),
    // Freeform layout
    freeformElements: Map<String, FreeformElement> = emptyMap(),
    // New features
    levelEventsEnabled: Boolean = false,
    buttonStyle: String = "ROUND",
    controllerLayoutMode: String = "normal",
    controllerConnected: Boolean = false,
    timerExpired: Boolean = false,
    remainingSeconds: Int = 0,
    onCloseApp: () -> Unit = {},
    showOnboarding: Boolean = false,
    onDismissOnboarding: () -> Unit = {},
    onStartGame: () -> Unit, onPause: () -> Unit, onResume: () -> Unit,
    onRotate: () -> Unit, onRotateCCW: () -> Unit,
    onHardDrop: () -> Unit, onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onOpenSettings: () -> Unit, onToggleSound: () -> Unit,
    onQuit: () -> Unit = {}
) {
    val theme = LocalGameTheme.current

    // Level events: compute effective ghost/hold based on level milestones
    val lvl = gameState.level
    val isMarathon = gameState.gameMode == GameMode.MARATHON
    val eventsActive = levelEventsEnabled && isMarathon
    val effectiveGhost = ghostEnabled && !(eventsActive && lvl >= 14)
    val effectiveNextCount = when {
        eventsActive && lvl >= 10 -> 1
        eventsActive && lvl >= 6 -> 2
        else -> 3
    }
    val boardDimAlpha = when {
        eventsActive && lvl >= 18 -> 0.70f
        eventsActive && lvl >= 12 -> 0.85f
        else -> 1f
    }
    val holdDisabled = eventsActive && lvl >= 16
    val effectiveHold: () -> Unit = if (holdDisabled) { {} } else onHold

    // Screen shake on hard drop
    var shakeOffset by remember { mutableStateOf(0f) }
    val animShake by animateFloatAsState(shakeOffset, spring(dampingRatio = 0.3f, stiffness = 800f), label = "shake",
        finishedListener = { shakeOffset = 0f })
    // Piece lock flash
    var lockFlash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(if (lockFlash) 0.3f else 0f, tween(150), label = "flash",
        finishedListener = { lockFlash = false })

    val btnShape = ButtonShape.entries.find { it.name == buttonStyle } ?: ButtonShape.ROUND
    val useControllerLayout = controllerConnected &&
        (controllerLayoutMode == "minimal" || controllerLayoutMode == "auto")

    CompositionLocalProvider(
        LocalMultiColor provides multiColor,
        LocalButtonShape provides btnShape
    ) {
    val isMenu = gameState.status == GameStatus.MENU
    Box(Modifier.fillMaxSize()) {
        if (isMenu) {
            MenuOverlay(gameState.highScore, scoreHistory, onStartGame, onOpenSettings)
        } else if (useControllerLayout) {
            // Controller-optimized: full-screen board + floating HUD + pause only
            Box(Modifier.fillMaxSize().background(theme.backgroundColor)) {
                GameBoard(gameState.board, Modifier.fillMaxSize().alpha(boardDimAlpha),
                    gameState.currentPiece, gameState.ghostY, effectiveGhost,
                    gameState.clearedLineRows, animationStyle, animationDuration,
                    multiColor = multiColor)
                // Floating HUD
                Box(Modifier.fillMaxSize().systemBarsPadding().padding(8.dp)) {
                    // Top-left: Hold
                    Column(Modifier.align(Alignment.TopStart).background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp)).padding(6.dp)) {
                        Tag("HOLD"); HoldPiecePreview(gameState.holdPiece?.shape, gameState.holdUsed, Modifier.size(32.dp))
                    }
                    // Top-center: Score + Level
                    Column(Modifier.align(Alignment.TopCenter).background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(gameState.score.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace, color = theme.accentColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tag("LV ${gameState.level}"); Tag("${gameState.lines}L")
                        }
                    }
                    // Top-right: Next + Pause
                    Column(Modifier.align(Alignment.TopEnd).background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp)).padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Tag("NEXT")
                        gameState.nextPieces.take(effectiveNextCount).forEachIndexed { i, p ->
                            NextPiecePreview(p.shape, Modifier.size(if (i == 0) 32.dp else 24.dp))
                        }
                    }
                    // Bottom-right: Pause button
                    ActionButton("PAUSE", onPause, modifier = Modifier.align(Alignment.BottomEnd),
                        width = 70.dp, height = 30.dp, backgroundColor = theme.buttonSecondary.copy(0.7f))
                }
                if (gameState.status == GameStatus.PAUSED) PauseOverlay(onResume, onOpenSettings, onQuit)
                if (gameState.status == GameStatus.GAME_OVER) GameOverOverlay(gameState.score, gameState.level, gameState.lines, onStartGame, onOpenSettings, onQuit)
            }
        } else {
            // Normal game content
            Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
                if (customLayout != null) {
                    CustomGameLayout(gameState, customLayout, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame)
                } else when (layoutPreset) {
                    LayoutPreset.PORTRAIT_CLASSIC -> ClassicLayout(gameState, dpadStyle, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame, boardDimAlpha, effectiveNextCount)
                    LayoutPreset.PORTRAIT_MODERN -> ModernLayout(gameState, dpadStyle, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame, boardDimAlpha, effectiveNextCount)
                    LayoutPreset.PORTRAIT_FULLSCREEN -> FullscreenLayout(gameState, dpadStyle, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame, boardDimAlpha, effectiveNextCount)
                    LayoutPreset.PORTRAIT_ONEHAND -> OneHandLayout(gameState, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame)
                    LayoutPreset.PORTRAIT_FREEFORM -> FreeformGameLayout(gameState, dpadStyle, effectiveGhost, animationStyle, animationDuration, freeformElements, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame)
                    LayoutPreset.LANDSCAPE_DEFAULT -> LandscapeLayout(gameState, dpadStyle, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, false)
                    LayoutPreset.LANDSCAPE_LEFTY -> LandscapeLayout(gameState, dpadStyle, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, true)
                    LayoutPreset.PORTRAIT_3D -> {}
                }
                if (gameState.status == GameStatus.PAUSED) PauseOverlay(onResume, onOpenSettings, onQuit)
                if (gameState.status == GameStatus.GAME_OVER) GameOverOverlay(gameState.score, gameState.level, gameState.lines, onStartGame, onOpenSettings, onQuit)
            }
        }
        ActionPopup(gameState.lastActionLabel, gameState.linesCleared)
        // Combo counter display (top of screen)
        if (gameState.comboCount >= 2 && gameState.status == GameStatus.PLAYING) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                .background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("COMBO x${gameState.comboCount}", color = Color(0xFFF4D03F), fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            }
        }
        // B2B streak
        if (gameState.backToBackCount >= 2 && gameState.status == GameStatus.PLAYING) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                .background(Color(0xFF8B5CF6).copy(0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("B2B x${gameState.backToBackCount}", color = Color.White, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        // Countdown timer badge
        if (remainingSeconds > 0 && gameState.status == GameStatus.PLAYING) {
            val h = remainingSeconds / 3600; val m = (remainingSeconds % 3600) / 60; val s = remainingSeconds % 60
            val isWarning = remainingSeconds <= 60
            val pulseAlpha = if (isWarning) {
                val inf = rememberInfiniteTransition(label = "tp")
                val a by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pa")
                a
            } else 0.8f
            Box(Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 8.dp)
                .background((if (isWarning) Color(0xFFB91C1C) else Color.Black).copy(pulseAlpha), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("%d:%02d:%02d".format(h, m, s), color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        // Screen shake overlay
        if (animShake != 0f) {
            Box(Modifier.fillMaxSize().offset(y = animShake.dp))
        }
        // Lock flash overlay
        if (flashAlpha > 0f) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(flashAlpha)))
        }
        // Timer expired — blocks all gameplay
        if (timerExpired) TimerExpiredOverlay(onCloseApp)
        // Onboarding overlay
        if (showOnboarding && gameState.status == GameStatus.MENU) {
            OnboardingOverlay(onDismissOnboarding)
        }
    }
    } // end CompositionLocalProvider
}

// === CLASSIC: Device frame — Board + info on right side ===
@Composable private fun ClassicLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val theme = LocalGameTheme.current
    // Classic: force mono-color (no colored pieces)
    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Device frame — retro handheld LCD style
        Row(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(theme.deviceColor)
            .border(2.dp, theme.deviceColor.darken(0.2f), RoundedCornerShape(10.dp)).padding(6.dp)) {
            // Board — always mono-color in classic, with LCD scanline overlay
            Box(Modifier.weight(1f).fillMaxHeight().padding(end = 4.dp)) {
                GameBoard(gs.board, Modifier.fillMaxSize().alpha(boardDimAlpha),
                    gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = false)
                // LCD scanline overlay
                Canvas(Modifier.fillMaxSize().alpha(0.08f)) {
                    val lineSpacing = 3.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(Color.Black, start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                        y += lineSpacing
                    }
                }
            }
            // Right info panel — SCORE > LEVELS > SPEED > NEXT (like the real LCD)
            Column(
                Modifier.width(72.dp).fillMaxHeight().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // SCORE
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(gs.score.toString().padStart(6, '0'), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 1.sp)
                }

                // LEVELS
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEVELS", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(gs.level.toString().padStart(6, '0'), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 1.sp)
                }

                // SPEED
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SPEED", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    // Speed derives from level — higher level = higher speed number
                    val speed = gs.level.coerceIn(1, 20)
                    Text("$speed", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, color = theme.accentColor)
                }

                // NEXT — single piece preview only (classic shows just 1)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NEXT", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    gs.nextPieces.take(1).forEach { p ->
                        NextPiecePreview(p.shape, Modifier.size(44.dp), 1f)
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        // Classic controls — no hold button
        ClassicControls(dp, onHD, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
    }
}

// === MODERN: Compact info bar + big board ===
@Composable private fun ModernLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val theme = LocalGameTheme.current
    // Animated score counter
    val animatedScore by animateIntAsState(gs.score, animationSpec = tween(300), label = "score")
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Modern status bar with rounded shadow
        Row(Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)).background(theme.deviceColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(34.dp)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Tag("LV ${gs.level}"); Tag("${gs.lines} LINES") }
                Text(animatedScore.toString().padStart(7, '0'), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("NEXT"); NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(34.dp)) }
        }
        Spacer(Modifier.height(6.dp))
        // Board with rounded corners and shadow
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
            GameBoard(gs.board, Modifier.fillMaxSize().alpha(boardDimAlpha), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = true)
        }
        Spacer(Modifier.height(6.dp))
        FullControls(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
    }
}

// === FULLSCREEN: Max board, tiny info strip above ===
@Composable private fun FullscreenLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val theme = LocalGameTheme.current
    // Fullscreen: board fills maximum area, controls overlay with transparency
    Box(Modifier.fillMaxSize()) {
        // Board fills entire area
        GameBoard(gs.board, Modifier.fillMaxSize().alpha(boardDimAlpha), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current)
        // Floating info strip
        Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 8.dp, vertical = 4.dp)
            .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
            HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(26.dp))
            Tag("LV${gs.level}")
            Text(gs.score.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            Tag("${gs.lines}L")
            NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(26.dp))
        }
        // Controls at bottom with transparency
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().alpha(0.6f)) {
            FullControls(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
        }
    }
}

// === COMPACT (was One-Hand): Board with side panels, D-Pad at bottom ===
@Composable private fun OneHandLayout(
    gs: GameState, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    Column(Modifier.fillMaxSize().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Redesigned info row: Level (left) | Score (center) | Lines (right)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("LEVEL", fontSize = 8.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("${gs.level}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SCORE", fontSize = 8.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(gs.score.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("LINES", fontSize = 8.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("${gs.lines}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            }
        }
        // Board with Hold panel on left and Next panel on right
        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            // Left: Hold preview
            Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("HOLD", fontSize = 7.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(2.dp))
                HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(40.dp))
            }
            // Center: Board
            GameBoard(gs.board, Modifier.weight(1f).fillMaxHeight(), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current)
            // Right: Next queue
            Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("NEXT", fontSize = 7.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(2.dp))
                gs.nextPieces.take(2).forEachIndexed { i, p ->
                    NextPiecePreview(p.shape, Modifier.size(if (i == 0) 40.dp else 30.dp).padding(1.dp), if (i == 0) 1f else 0.5f)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        // Centered D-Pad with rotate in center + action buttons on sides
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            // Hold + Pause on left
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionButton("HOLD", onHold, width = 58.dp, height = 28.dp)
                ActionButton(if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                    { if (gs.status == GameStatus.MENU) onStart() else onPause() }, width = 58.dp, height = 28.dp)
                ActionButton("···", onSet, width = 42.dp, height = 22.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
            }
            // Central D-Pad — always rotate-in-center style, left/right spread out
            DPad(60.dp, rotateInCenter = true, horizontalSpread = 16.dp,
                onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
            // Right spacer to balance — same width as left column
            Spacer(Modifier.width(58.dp))
        }
    }
}

// === CUSTOM GAME LAYOUT: 3-zone rendering using CustomLayoutData settings ===
@Composable private fun CustomGameLayout(
    gs: GameState, cl: CustomLayoutData, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    val isRotateCenter = cl.dpadStyle == "ROTATE_CENTER"
    val dpadSz = when (cl.controlSize) { "SMALL" -> 44.dp; "LARGE" -> 62.dp; else -> 54.dp }
    val rotSz = when (cl.controlSize) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 66.dp }
    val dp = if (isRotateCenter) DPadStyle.ROTATE_CENTRE else DPadStyle.STANDARD

    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {

        // === TOP BAR ===
        if (cl.topBarVisible) {
            when (cl.topBarStyle) {
                "DEVICE_FRAME" -> {
                    // Classic-style device frame bar
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(theme.deviceColor).padding(6.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        cl.topBarElementOrder.filter { cl.isVisible(it) }.forEach { elem ->
                            when (elem) {
                                LayoutElements.HOLD_PREVIEW -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(34.dp)) }
                                LayoutElements.SCORE -> Text(gs.score.toString().padStart(7, '0'), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp)
                                LayoutElements.LEVEL -> Tag("LV ${gs.level}")
                                LayoutElements.LINES -> Tag("${gs.lines} LINES")
                                LayoutElements.NEXT_PREVIEW -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("NEXT"); NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(34.dp)) }
                            }
                        }
                    }
                }
                "MINIMAL" -> {
                    // Tiny strip
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        cl.topBarElementOrder.filter { cl.isVisible(it) }.forEach { elem ->
                            when (elem) {
                                LayoutElements.HOLD_PREVIEW -> HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(24.dp))
                                LayoutElements.SCORE -> Text(gs.score.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                                LayoutElements.LEVEL -> Tag("LV${gs.level}")
                                LayoutElements.LINES -> Tag("${gs.lines}L")
                                LayoutElements.NEXT_PREVIEW -> NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(24.dp))
                            }
                        }
                    }
                }
                else -> {
                    // COMPACT — Modern-style bar
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(theme.deviceColor).padding(horizontal = 10.dp, vertical = 6.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        cl.topBarElementOrder.filter { cl.isVisible(it) }.forEach { elem ->
                            when (elem) {
                                LayoutElements.HOLD_PREVIEW -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(34.dp)) }
                                LayoutElements.SCORE -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(gs.score.toString().padStart(7, '0'), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { if (cl.isVisible(LayoutElements.LEVEL)) Tag("LV ${gs.level}"); if (cl.isVisible(LayoutElements.LINES)) Tag("${gs.lines} LINES") }
                                }
                                LayoutElements.LEVEL -> {} // Handled inside SCORE for compact
                                LayoutElements.LINES -> {} // Handled inside SCORE for compact
                                LayoutElements.NEXT_PREVIEW -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Tag("NEXT")
                                    gs.nextPieces.take(cl.nextQueueSize).forEachIndexed { i, p ->
                                        NextPiecePreview(p.shape, Modifier.size(if (i == 0) 34.dp else 24.dp), if (i == 0) 1f else 0.5f)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // === BOARD ===
        val boardWeight = when (cl.boardSize) { "COMPACT" -> 0.55f; "FULLSCREEN" -> 0.85f; else -> 0.7f }
        val boardAlign = when (cl.boardAlignment) { "LEFT" -> Alignment.CenterStart; "RIGHT" -> Alignment.CenterEnd; else -> Alignment.Center }

        Box(Modifier.fillMaxWidth().weight(boardWeight), contentAlignment = boardAlign) {
            val boardMod = when (cl.boardAlignment) {
                "LEFT" -> Modifier.fillMaxHeight().fillMaxWidth(0.8f)
                "RIGHT" -> Modifier.fillMaxHeight().fillMaxWidth(0.8f)
                else -> Modifier.fillMaxHeight().aspectRatio(0.5f)
            }
            Box(boardMod) {
                GameBoard(gs.board, Modifier.fillMaxSize(), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current)
                // Info overlay when top bar is hidden
                if (!cl.topBarVisible && cl.boardInfoOverlay != "HIDDEN") {
                    Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color.Black.copy(0.35f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            if (cl.isVisible(LayoutElements.HOLD_PREVIEW)) HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(22.dp))
                            if (cl.isVisible(LayoutElements.SCORE)) Text(gs.score.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White.copy(0.9f))
                            if (cl.isVisible(LayoutElements.LEVEL)) Text("LV${gs.level}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(0.7f))
                            if (cl.isVisible(LayoutElements.LINES)) Text("${gs.lines}L", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(0.7f))
                            if (cl.isVisible(LayoutElements.NEXT_PREVIEW)) NextPiecePreview(gs.nextPieces.firstOrNull()?.shape, Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // === CONTROLS — uses stored positions ===
        CustomControls(cl, dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
    }
}

// Renders controls at stored normalized positions within the controls area
@Composable private fun CustomControls(
    cl: CustomLayoutData, dp: DPadStyle, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onRotate: () -> Unit,
    onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit, status: GameStatus
) {
    val dpadSz = when (cl.controlSize) { "SMALL" -> 44.dp; "LARGE" -> 62.dp; else -> 54.dp }
    val rotSz = when (cl.controlSize) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 66.dp }
    val isRotateCenter = cl.dpadStyle == "ROTATE_CENTER"
    val positions = cl.controlPositions

    BoxWithConstraints(Modifier.fillMaxWidth().height(when (cl.controlSize) { "SMALL" -> 140.dp; "LARGE" -> 200.dp; else -> 170.dp })) {
        val mw = maxWidth; val mh = maxHeight

        // D-Pad
        val dpPos = positions[LayoutElements.DPAD] ?: ElementPosition(0.15f, 0.5f)
        val dpadArea = dpadSz * 2.6f
        Box(Modifier.offset(x = mw * dpPos.x - dpadArea / 2, y = mh * dpPos.y - dpadArea / 2)) {
            DPad(dpadSz, rotateInCenter = isRotateCenter, onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR, onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        }

        // Rotate button (only if standard style)
        if (!isRotateCenter && cl.isVisible(LayoutElements.ROTATE_BTN)) {
            val rp = positions[LayoutElements.ROTATE_BTN] ?: ElementPosition(0.85f, 0.5f)
            Box(Modifier.offset(x = mw * rp.x - rotSz / 2, y = mh * rp.y - rotSz / 2)) { RotateButton(onRotate, rotSz) }
        }

        // Hold button
        if (cl.isVisible(LayoutElements.HOLD_BTN)) {
            val hp = positions[LayoutElements.HOLD_BTN] ?: ElementPosition(0.5f, 0.2f)
            Box(Modifier.offset(x = mw * hp.x - 39.dp, y = mh * hp.y - 17.dp)) { ActionButton("HOLD", onHold, width = 78.dp, height = 34.dp) }
        }

        // Pause/Start button
        if (cl.isVisible(LayoutElements.PAUSE_BTN)) {
            val pp = positions[LayoutElements.PAUSE_BTN] ?: ElementPosition(0.5f, 0.55f)
            Box(Modifier.offset(x = mw * pp.x - 39.dp, y = mh * pp.y - 17.dp)) {
                ActionButton(if (status == GameStatus.MENU) "START" else "PAUSE",
                    { if (status == GameStatus.MENU) onStart() else onPause() }, width = 78.dp, height = 34.dp)
            }
        }

        // Menu button (always visible)
        val mp = positions[LayoutElements.MENU_BTN] ?: ElementPosition(0.5f, 0.88f)
        Box(Modifier.offset(x = mw * mp.x - 23.dp, y = mh * mp.y - 12.dp)) { ActionButton("···", onSet, width = 46.dp, height = 24.dp, backgroundColor = LocalGameTheme.current.buttonSecondary) }
    }
}

// === LANDSCAPE ===
@Composable private fun LandscapeLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, lefty: Boolean
) {
    Row(Modifier.fillMaxSize().padding(6.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) { if (lefty) LandInfo(gs, onPause, onSet) else LandCtrl(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause) }
        GameBoard(gs.board, Modifier.fillMaxHeight().aspectRatio(0.5f).padding(horizontal = 6.dp), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current)
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) { if (lefty) LandCtrl(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause) else LandInfo(gs, onPause, onSet) }
    }
}

@Composable private fun LandInfo(gs: GameState, onPause: () -> Unit, onSet: () -> Unit) {
    val theme = LocalGameTheme.current
    Column(Modifier.fillMaxHeight().padding(4.dp), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
        ScoreBlock(gs.score, gs.level, gs.lines)
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(44.dp)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Tag("NEXT")
            gs.nextPieces.take(3).forEachIndexed { i, p -> NextPiecePreview(p.shape, Modifier.size(when(i){0->40.dp;1->32.dp;else->26.dp}), when(i){0->1f;1->0.6f;else->0.35f}) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ActionButton("PAUSE", onPause, width = 64.dp, height = 28.dp); ActionButton("...", onSet, width = 36.dp, height = 28.dp, backgroundColor = LocalGameTheme.current.buttonSecondary) }
    }
}

@Composable private fun LandCtrl(
    dp: DPadStyle, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onRotate: () -> Unit, onPause: () -> Unit
) {
    Column(Modifier.fillMaxHeight().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        ActionButton("HOLD", onHold, width = 76.dp, height = 32.dp)
        DPad(48.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE, onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR, onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 56.dp)
        ActionButton("PAUSE", onPause, width = 76.dp, height = 32.dp)
    }
}

// === SHARED: Full controls row (ALL buttons at bottom) ===
@Composable private fun FullControls(
    dp: DPadStyle, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onRotate: () -> Unit,
    onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit, status: GameStatus
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        // D-Pad — compact size
        DPad(50.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
            onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        // Centre: HOLD + PAUSE/START + SETTINGS
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton("HOLD", onHold, width = 72.dp, height = 30.dp)
            ActionButton(
                if (status == GameStatus.MENU) "START" else "PAUSE",
                { if (status == GameStatus.MENU) onStart() else onPause() },
                width = 72.dp, height = 30.dp
            )
            ActionButton("...", onSet, width = 42.dp, height = 22.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
        }
        // Rotate
        if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 60.dp) else Spacer(Modifier.size(60.dp))
    }
}

// === CLASSIC CONTROLS: Same as FullControls but without HOLD button ===
@Composable private fun ClassicControls(
    dp: DPadStyle, onHD: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onRotate: () -> Unit,
    onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit, status: GameStatus
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        DPad(50.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
            onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
            onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        // Centre: PAUSE/START + SETTINGS only (no HOLD)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton(
                if (status == GameStatus.MENU) "START" else "PAUSE",
                { if (status == GameStatus.MENU) onStart() else onPause() },
                width = 72.dp, height = 30.dp
            )
            ActionButton("...", onSet, width = 42.dp, height = 22.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
        }
        // Rotate
        if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 60.dp) else Spacer(Modifier.size(60.dp))
    }
}

// === CUSTOM LAYOUT: Position-based, uses normalized coordinates ===
@Composable private fun CustomLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    cl: CustomLayoutData,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    val dpadSz = when (cl.sizeFor(LayoutElements.DPAD)) { "SMALL" -> 44.dp; "LARGE" -> 62.dp; else -> 54.dp }
    val rotSz = when (cl.sizeFor(LayoutElements.ROTATE_BTN)) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 66.dp }
    val vis = cl.visibility
    val pos = cl.positions

    fun isVisible(elem: String) = vis.getOrDefault(elem, true)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = maxWidth; val maxH = maxHeight

        // Board — centered, takes most space
        if (isVisible(LayoutElements.BOARD)) {
            val bp = pos[LayoutElements.BOARD] ?: ElementPosition(0.5f, 0.38f)
            Box(Modifier.size(maxW * 0.85f, maxH * 0.6f).offset(x = maxW * bp.x - maxW * 0.425f, y = maxH * bp.y - maxH * 0.3f)) {
                GameBoard(gs.board, Modifier.fillMaxSize(), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current)
            }
        }
        // Score
        if (isVisible(LayoutElements.SCORE)) {
            val sp = pos[LayoutElements.SCORE] ?: ElementPosition(0.5f, 0.02f)
            Text(gs.score.toString().padStart(7, '0'), Modifier.offset(x = maxW * sp.x - 40.dp, y = maxH * sp.y),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
        }
        // Level
        if (isVisible(LayoutElements.LEVEL)) {
            val lp = pos[LayoutElements.LEVEL] ?: ElementPosition(0.15f, 0.02f)
            Tag("LV${gs.level}", Modifier.offset(x = maxW * lp.x - 16.dp, y = maxH * lp.y))
        }
        // Lines
        if (isVisible(LayoutElements.LINES)) {
            val lp = pos[LayoutElements.LINES] ?: ElementPosition(0.85f, 0.02f)
            Tag("${gs.lines}L", Modifier.offset(x = maxW * lp.x - 16.dp, y = maxH * lp.y))
        }
        // Hold preview
        if (isVisible(LayoutElements.HOLD_PREVIEW)) {
            val hp = pos[LayoutElements.HOLD_PREVIEW] ?: ElementPosition(0.08f, 0.08f)
            Column(Modifier.offset(x = maxW * hp.x - 24.dp, y = maxH * hp.y - 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Tag("HOLD"); HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(40.dp))
            }
        }
        // Next preview
        if (isVisible(LayoutElements.NEXT_PREVIEW)) {
            val np = pos[LayoutElements.NEXT_PREVIEW] ?: ElementPosition(0.92f, 0.08f)
            Column(Modifier.offset(x = maxW * np.x - 24.dp, y = maxH * np.y - 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Tag("NEXT"); gs.nextPieces.take(cl.nextQueueSize).forEachIndexed { i, p -> NextPiecePreview(p.shape, Modifier.size(if (i == 0) 34.dp else 24.dp), if (i == 0) 1f else 0.5f) }
            }
        }
        // D-Pad
        if (isVisible(LayoutElements.DPAD)) {
            val dp2 = pos[LayoutElements.DPAD] ?: ElementPosition(0.18f, 0.85f)
            Box(Modifier.offset(x = maxW * dp2.x - 70.dp, y = maxH * dp2.y - 70.dp)) {
                DPad(dpadSz, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                    onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                    onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
            }
        }
        // Rotate
        if (isVisible(LayoutElements.ROTATE_BTN) && dp == DPadStyle.STANDARD) {
            val rp = pos[LayoutElements.ROTATE_BTN] ?: ElementPosition(0.85f, 0.85f)
            Box(Modifier.offset(x = maxW * rp.x - rotSz / 2, y = maxH * rp.y - rotSz / 2)) { RotateButton(onRotate, rotSz) }
        }
        // Hold button
        if (isVisible(LayoutElements.HOLD_BTN)) {
            val hb = pos[LayoutElements.HOLD_BTN] ?: ElementPosition(0.5f, 0.80f)
            Box(Modifier.offset(x = maxW * hb.x - 39.dp, y = maxH * hb.y - 17.dp)) { ActionButton("HOLD", onHold, width = 78.dp, height = 34.dp) }
        }
        // Pause
        if (isVisible(LayoutElements.PAUSE_BTN)) {
            val pb = pos[LayoutElements.PAUSE_BTN] ?: ElementPosition(0.5f, 0.87f)
            Box(Modifier.offset(x = maxW * pb.x - 39.dp, y = maxH * pb.y - 17.dp)) {
                ActionButton(if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                    { if (gs.status == GameStatus.MENU) onStart() else onPause() }, width = 78.dp, height = 34.dp)
            }
        }
        // Menu (always visible — sandwich icon style)
        val mp = pos[LayoutElements.MENU_BTN] ?: ElementPosition(0.5f, 0.94f)
        Box(Modifier.offset(x = maxW * mp.x - 23.dp, y = maxH * mp.y - 12.dp)) { ActionButton("≡", onSet, width = 46.dp, height = 24.dp, backgroundColor = LocalGameTheme.current.buttonSecondary) }
    }
}

// === Helpers ===
@Composable private fun OnboardingOverlay(onDismiss: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("Welcome!", "Swipe or use the D-Pad to move pieces\nTap rotate to spin them", "🎮"),
        Triple("Hold Piece", "Press HOLD to save a piece for later\n(Classic mode has no Hold)", "📦"),
        Triple("Ready?", "Customize everything in Settings\nChoose your layout and theme", "⚙️")
    )
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.88f)).clickable {
        if (page < pages.size - 1) page++ else onDismiss()
    }, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(pages[page].third, fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(pages[page].first, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFFF4D03F))
            Spacer(Modifier.height(12.dp))
            Text(pages[page].second, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            // Page indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { i, _ ->
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == page) Color(0xFFF4D03F) else Color.White.copy(0.3f)))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(if (page < pages.size - 1) "Tap to continue" else "Tap to start!", color = Color.White.copy(0.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable private fun Tag(t: String, modifier: Modifier = Modifier) { Text(t, modifier = modifier, fontSize = 9.sp, color = LocalGameTheme.current.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp) }
@Composable private fun ScoreBlock(score: Int, level: Int, lines: Int) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(score.toString().padStart(7, '0'), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Tag("LV${level}"); Tag("${lines}L") }
    }
}

// === Overlays ===
@Composable private fun MenuOverlay(hs: Int, scoreHistory: List<com.brickgame.tetris.data.ScoreEntry>, onStart: () -> Unit, onSet: () -> Unit) {
    // Enhanced: mini leaderboard below high score
    val theme = LocalGameTheme.current
    val isDark = com.brickgame.tetris.ui.theme.LocalIsDarkMode.current
    val bgColor = if (isDark) theme.backgroundColor else Color(0xFFF2F2F2)
    Box(Modifier.fillMaxSize().background(bgColor)) {
        // Falling tetris pieces background
        FallingPiecesBackground(theme, isDark)
        // Content
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("BRICK", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = if (isDark) theme.textPrimary.copy(alpha = 0.9f) else Color(0xFF2A2A2A), letterSpacing = 8.sp)
            Text("GAME", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = if (isDark) theme.accentColor else Color(0xFFB8860B), letterSpacing = 8.sp)
            Spacer(Modifier.height(32.dp))
            if (hs > 0) {
                val bestEntry = scoreHistory.maxByOrNull { it.score }
                Text("$hs", fontSize = 28.sp, fontFamily = FontFamily.Monospace,
                    color = if (isDark) theme.accentColor else Color(0xFFB8860B), fontWeight = FontWeight.Bold)
                if (bestEntry != null) {
                    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                    val dateStr = sdf.format(java.util.Date(bestEntry.timestamp))
                    Text("${bestEntry.playerName} · $dateStr", fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        color = if (isDark) theme.textSecondary else Color(0xFF666666))
                }
                Spacer(Modifier.height(4.dp))
                Text("HIGH SCORE", fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    color = if (isDark) theme.textSecondary.copy(alpha = 0.6f) else Color(0xFF999999), letterSpacing = 4.sp)
                Spacer(Modifier.height(32.dp))
            }
            ActionButton("PLAY", onStart, width = 180.dp, height = 52.dp,
                backgroundColor = if (isDark) theme.accentColor else Color(0xFFB8860B))
            Spacer(Modifier.height(12.dp))
            ActionButton("SETTINGS", onSet, width = 180.dp, height = 44.dp,
                backgroundColor = if (isDark) theme.buttonSecondary else Color(0xFFE0E0E0))
            // Mini leaderboard — top 3 scores
            if (scoreHistory.size > 1) {
                Spacer(Modifier.height(20.dp))
                Text("RECENT BEST", fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                    color = if (isDark) theme.textSecondary.copy(0.5f) else Color(0xFF999999), letterSpacing = 3.sp)
                Spacer(Modifier.height(6.dp))
                val top3 = scoreHistory.sortedByDescending { it.score }.take(3)
                top3.forEachIndexed { i, entry ->
                    val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "" }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(medal, fontSize = 14.sp)
                        Text(entry.score.toString(), fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, color = if (isDark) theme.textPrimary.copy(0.7f) else Color(0xFF444444))
                        Text("Lv${entry.level}", fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            color = if (isDark) theme.textSecondary.copy(0.4f) else Color(0xFF888888))
                    }
                }
            }
        }
    }
}

// Falling transparent tetris pieces — matrix rain style with colored pieces, long green trails, and sparkle
@Composable
private fun FallingPiecesBackground(theme: com.brickgame.tetris.ui.theme.GameTheme, isDark: Boolean = true) {
    data class FP(val col: Float, val speed: Float, val sz: Float, val shape: Int,
                  val alpha: Float, val startY: Float, val colorIdx: Int, val trailLen: Int,
                  val sparkle: Boolean, val sparklePhase: Float)

    val pieces = remember {
        val rng = java.util.Random(42)
        (0..299).map {
            FP(col = rng.nextFloat(), speed = 0.4f + rng.nextFloat() * 1.2f,
               sz = 5f + rng.nextFloat() * 8f, shape = it % 7,
               alpha = 0.12f + rng.nextFloat() * 0.25f,
               // Large random startY spread ensures pieces are uniformly distributed
               startY = rng.nextFloat() * 10000f,
               colorIdx = it % 7, trailLen = 4 + rng.nextInt(8),
               sparkle = rng.nextFloat() < 0.15f,
               sparklePhase = rng.nextFloat() * 6.28f)
        }
    }
    val t = rememberInfiniteTransition(label = "bg")
    // Very large target value so the animation never visibly restarts
    // Each piece wraps independently via modulo on screen height
    val anim by t.animateFloat(0f, 1_000_000f, infiniteRepeatable(tween(1_500_000, easing = LinearEasing)), label = "fall")

    val pieceColors = remember { listOf(
        Color(0xFFFF4444), Color(0xFF44AAFF), Color(0xFFFFAA00), Color(0xFF44FF44),
        Color(0xFFFF44FF), Color(0xFF44FFFF), Color(0xFFF4D03F)
    ) }
    val trailColor = Color(0xFF22C55E)

    val shapes = remember { listOf(
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),       // O
        listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),       // I
        listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1),       // L
        listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1),       // J
        listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),       // S
        listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),       // Z
        listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1),       // T
    ) }

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val wrapH = h + 600f  // total travel distance before wrapping
        // In light mode, use higher alpha for visibility on light background
        val alphaBoost = if (isDark) 1f else 2.2f
        val actualTrailColor = if (isDark) trailColor else Color(0xFF22A050)
        pieces.forEach { p ->
            // Each piece wraps independently based on its own startY offset
            val rawY = p.startY + anim * p.speed
            val baseY = (rawY % wrapH) - 300f
            val x = p.col * w
            val s = p.sz
            val shape = shapes[p.shape % shapes.size]
            val pColor = pieceColors[p.colorIdx]
            val pa = (p.alpha * alphaBoost).coerceAtMost(0.55f)

            // Draw long green trail (fading upward) — bigger trail
            for (ti in 1..p.trailLen) {
                val trailY = baseY - ti * (s + 2) * 1.2f
                val trailAlpha = pa * 0.5f * (1f - ti.toFloat() / (p.trailLen + 1))
                val trailSz = s * (1f - ti * 0.04f).coerceAtLeast(0.3f)
                shape.forEach { (dx, dy) ->
                    drawRoundRect(
                        color = actualTrailColor.copy(alpha = trailAlpha.coerceIn(0f, 1f)),
                        topLeft = androidx.compose.ui.geometry.Offset(x + dx * (s + 2), trailY + dy * (s + 2)),
                        size = androidx.compose.ui.geometry.Size(trailSz, trailSz),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                }
            }

            // Draw colored piece
            shape.forEach { (dx, dy) ->
                drawRoundRect(
                    color = pColor.copy(alpha = pa),
                    topLeft = androidx.compose.ui.geometry.Offset(x + dx * (s + 2), baseY + dy * (s + 2)),
                    size = androidx.compose.ui.geometry.Size(s, s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
            }

            // Sparkle effect on some pieces — a bright white dot that pulses
            if (p.sparkle) {
                val sparkleAlpha = (0.3f + 0.4f * kotlin.math.sin(anim * 0.01f + p.sparklePhase)).coerceIn(0f, 0.7f)
                val sparkX = x + (s + 2) * 0.5f
                val sparkY = baseY - s * 0.5f
                drawCircle(
                    color = Color.White.copy(alpha = sparkleAlpha * pa * 3f),
                    radius = s * 0.35f,
                    center = androidx.compose.ui.geometry.Offset(sparkX, sparkY)
                )
                // Small outer glow
                drawCircle(
                    color = pColor.copy(alpha = sparkleAlpha * p.alpha * 1.5f),
                    radius = s * 0.6f,
                    center = androidx.compose.ui.geometry.Offset(sparkX, sparkY)
                )
            }
        }
    }
}

@Composable private fun PauseOverlay(onResume: () -> Unit, onSet: () -> Unit, onQuit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color.White, letterSpacing = 4.sp)
            Spacer(Modifier.height(28.dp)); ActionButton("RESUME", onResume, width = 160.dp, height = 48.dp)
            Spacer(Modifier.height(12.dp)); ActionButton("SETTINGS", onSet, width = 160.dp, height = 42.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
            Spacer(Modifier.height(12.dp)); ActionButton("LEAVE", onQuit, width = 160.dp, height = 42.dp, backgroundColor = Color(0xFFB91C1C))
        }
    }
}

@Composable private fun TimerExpiredOverlay(onLeave: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⏰", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("TIME'S", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFF4D03F), letterSpacing = 4.sp)
            Text("UP!", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFF4D03F), letterSpacing = 4.sp)
            Spacer(Modifier.height(16.dp))
            Text("Time to take a break!", fontSize = 15.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(32.dp))
            ActionButton("LEAVE", onLeave, width = 180.dp, height = 52.dp, backgroundColor = Color(0xFFB91C1C))
        }
    }
}

@Composable private fun GameOverOverlay(score: Int, level: Int, lines: Int, onRestart: () -> Unit, onMenu: () -> Unit, onLeave: () -> Unit) {
    val theme = LocalGameTheme.current
    // New high score flash
    val inf = rememberInfiniteTransition(label = "go")
    val titlePulse by inf.animateFloat(0.8f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "gp")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("GAME", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = Color(0xFFFF4444).copy(alpha = titlePulse), letterSpacing = 6.sp)
            Text("OVER", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = Color(0xFFFF4444).copy(alpha = titlePulse), letterSpacing = 6.sp)
            Spacer(Modifier.height(16.dp))
            // Score with glow effect
            Text(score.toString(), fontSize = 32.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            // Stats breakdown
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatChip("LEVEL", "$level")
                StatChip("LINES", "$lines")
                StatChip("LPM", if (level > 0) "${"%.1f".format(lines.toFloat() / level)}" else "0")
            }
            Spacer(Modifier.height(24.dp))
            ActionButton("AGAIN", onRestart, width = 160.dp, height = 48.dp, backgroundColor = theme.accentColor)
            Spacer(Modifier.height(10.dp))
            ActionButton("LEAVE", onLeave, width = 160.dp, height = 42.dp, backgroundColor = Color(0xFFB91C1C))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(Color.White.copy(0.08f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, fontSize = 8.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        Text(value, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// =============================================================================
// ACTION POPUP — Big, centered, color-coded, auto-fading
// =============================================================================
@Composable
private fun ActionPopup(label: String, linesCleared: Int) {
    // Track the label we've already shown to prevent re-triggering on recompose
    var lastShownLabel by remember { mutableStateOf("") }
    var lastShownTime by remember { mutableStateOf(0L) }
    var showPopup by remember { mutableStateOf(false) }
    var popupText by remember { mutableStateOf("") }
    var popupLines by remember { mutableStateOf(0) }

    // Only trigger on NEW labels during active gameplay (not when returning from settings)
    LaunchedEffect(label) {
        if (label.isNotEmpty() && (label != lastShownLabel || System.currentTimeMillis() - lastShownTime > 1500L)) {
            // Skip "Single" — only 1 line, not worth a popup
            if (linesCleared == 1 && !label.contains("T-Spin", ignoreCase = true) && !label.contains("B2B", ignoreCase = true)) {
                lastShownLabel = label
                lastShownTime = System.currentTimeMillis()
                return@LaunchedEffect
            }
            popupText = label
            popupLines = linesCleared
            showPopup = true
            lastShownLabel = label
            lastShownTime = System.currentTimeMillis()
            // Auto-dismiss: longer for bigger clears
            val duration = when {
                linesCleared >= 4 -> 1800L
                linesCleared == 3 -> 1400L
                else -> 1100L
            }
            delay(duration)
            showPopup = false
        }
    }

    if (showPopup && popupText.isNotEmpty()) {
        // Escalating style based on lines cleared
        val popupBg: Color
        val popupTextColor: Color
        val popupFontSize: Int
        val popupShake: Boolean
        val popupGlow: Boolean
        when {
            popupText.contains("Tetris", ignoreCase = true) && popupText.contains("B2B", ignoreCase = true) -> {
                popupBg = Color(0xFFFF2200).copy(0.7f); popupTextColor = Color.White; popupFontSize = 42; popupShake = true; popupGlow = true
            }
            popupText.contains("Tetris", ignoreCase = true) -> {
                popupBg = Color(0xFFFF4400).copy(0.65f); popupTextColor = Color.White; popupFontSize = 40; popupShake = true; popupGlow = true
            }
            popupText.contains("T-Spin", ignoreCase = true) -> {
                popupBg = Color(0xFF9B59B6).copy(0.6f); popupTextColor = Color.White; popupFontSize = 34; popupShake = true; popupGlow = false
            }
            popupText.contains("B2B", ignoreCase = true) -> {
                popupBg = Color(0xFFFF8800).copy(0.6f); popupTextColor = Color.White; popupFontSize = 34; popupShake = false; popupGlow = true
            }
            popupLines >= 3 -> {
                popupBg = Color(0xFFFF6600).copy(0.55f); popupTextColor = Color.White; popupFontSize = 34; popupShake = true; popupGlow = false
            }
            popupLines == 2 -> {
                popupBg = Color(0xFFF4D03F).copy(0.55f); popupTextColor = Color.Black; popupFontSize = 30; popupShake = false; popupGlow = false
            }
            else -> {
                popupBg = Color(0xFF3498DB).copy(0.5f); popupTextColor = Color.White; popupFontSize = 28; popupShake = false; popupGlow = false
            }
        }

        // Animate: scale, alpha, rotation (shake for big clears)
        val animScale = remember { Animatable(0.2f) }
        val animAlpha = remember { Animatable(0f) }
        val animRotation = remember { Animatable(0f) }

        LaunchedEffect(popupText) {
            animScale.snapTo(0.2f)
            animAlpha.snapTo(0f)
            animRotation.snapTo(0f)

            when {
                popupLines >= 4 || popupShake -> {
                    // EPIC: Slam in with shake
                    launch { animAlpha.animateTo(1f, tween(60)) }
                    animScale.animateTo(1.3f, tween(80, easing = LinearOutSlowInEasing))
                    animScale.animateTo(0.95f, tween(60))
                    animScale.animateTo(1.05f, tween(50))
                    animScale.animateTo(1f, tween(40))
                    // Shake
                    repeat(3) {
                        animRotation.animateTo(3f, tween(30))
                        animRotation.animateTo(-3f, tween(30))
                    }
                    animRotation.animateTo(0f, tween(30))
                    // Hold
                    delay(600L)
                    // Dramatic fade + scale out
                    launch { animScale.animateTo(1.8f, tween(400)) }
                    animAlpha.animateTo(0f, tween(350))
                }
                popupLines == 3 -> {
                    // STRONG: Fast pop with bounce
                    launch { animAlpha.animateTo(1f, tween(70)) }
                    animScale.animateTo(1.2f, tween(90, easing = FastOutSlowInEasing))
                    animScale.animateTo(0.97f, tween(60))
                    animScale.animateTo(1.02f, tween(50))
                    animScale.animateTo(1f, tween(40))
                    delay(500L)
                    launch { animScale.animateTo(1.3f, tween(300)) }
                    animAlpha.animateTo(0f, tween(300))
                }
                else -> {
                    // MODERATE: Simple pop in/out for Double and combos
                    launch { animAlpha.animateTo(1f, tween(80)) }
                    animScale.animateTo(1.08f, tween(100, easing = FastOutSlowInEasing))
                    animScale.animateTo(1f, tween(60))
                    delay(400L)
                    animAlpha.animateTo(0f, tween(250))
                }
            }
        }

        Box(Modifier.fillMaxSize(), Alignment.Center) {
            // Optional glow effect behind text for Tetris-level clears
            if (popupGlow && animAlpha.value > 0.1f) {
                Box(
                    Modifier
                        .scale(animScale.value * 1.4f)
                        .alpha(animAlpha.value * 0.3f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(popupBg.copy(alpha = 0.4f))
                        .padding(horizontal = 52.dp, vertical = 28.dp)
                ) {}
            }
            Text(
                popupText,
                modifier = Modifier
                    .scale(animScale.value)
                    .alpha(animAlpha.value)
                    .graphicsLayer { rotationZ = animRotation.value }
                    .clip(RoundedCornerShape(20.dp))
                    .background(popupBg)
                    .padding(horizontal = 36.dp, vertical = 16.dp),
                fontSize = popupFontSize.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = popupTextColor,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }
    }
}
