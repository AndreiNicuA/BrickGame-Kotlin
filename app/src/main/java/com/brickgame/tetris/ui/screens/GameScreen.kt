package com.brickgame.tetris.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.brickgame.tetris.ui.layout.BoardShape
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
val LocalPieceMaterial = compositionLocalOf { "CLASSIC" }
val LocalHighContrast = compositionLocalOf { false }
val LocalUiScale = compositionLocalOf { 1.0f }
val LocalLeftHanded = compositionLocalOf { false }

// ========================= SHARED GAME EFFECTS =========================
// Extracted from 5 layout functions to eliminate ~300 lines of duplication.
// All modern layouts now use rememberGameEffects() + GameEffectsLayer().

/** Particle data for explosion effects */
data class EffectParticle(val x: Float, val y: Float, val vx: Float, val vy: Float,
                          val size: Float, val color: Int, val life: Float, val type: Int = 0)

/** Holds all mutable state for game visual effects */
class GameEffectsState {
    var screenShakeX by mutableFloatStateOf(0f)
    var screenShakeY by mutableFloatStateOf(0f)
    var clearFlashAlpha by mutableFloatStateOf(0f)
    val clearSize = mutableIntStateOf(0)
    // Score flyup
    var flyupKey by mutableIntStateOf(0)
    var flyupText by mutableStateOf("")
    var flyupProgress by mutableFloatStateOf(0f)
    // Level up burst
    var levelUpFlash by mutableFloatStateOf(0f)
    var prevLevel by mutableIntStateOf(0)
    // Score glow (level 9+)
    var scoreGlowAlpha by mutableFloatStateOf(0f)
    // Particles + shockwave
    var particles by mutableStateOf(emptyList<EffectParticle>())
    var shockwaveProgress by mutableFloatStateOf(0f)
    var shockwaveY by mutableFloatStateOf(0.5f)
    var shockwaveCount by mutableIntStateOf(0)
    // Spawn animation — quick scale + fade on new piece
    var spawnScale by mutableFloatStateOf(1f)
    var spawnAlpha by mutableFloatStateOf(1f)
}

/**
 * Creates and wires up all game visual effects for a layout.
 * @param shakeSteps number of shake iterations (18 for full, 14 for compact)
 * @param shakeDelay ms between shake frames
 * @param shakeMultiplier scale factor for shake intensity (1f = full, 0.8f = reduced)
 * @param flashMultiplier scale factor for flash intensity
 * @param enableParticles whether to enable explosion particles + shockwave
 * @param enableFlyup whether to enable score flyup text
 * @param enableLevelBurst whether to enable level-up ring burst
 * @param enableScoreGlow whether to enable score glow at level 9+
 */
@Composable
fun rememberGameEffects(
    gs: GameState,
    shakeSteps: Int = 18,
    shakeDelay: Long = 25L,
    shakeMultiplier: Float = 1f,
    flashMultiplier: Float = 1f,
    enableParticles: Boolean = true,
    enableFlyup: Boolean = true,
    enableLevelBurst: Boolean = true,
    enableScoreGlow: Boolean = true
): GameEffectsState {
    val state = remember { GameEffectsState() }

    // Screen shake + flash on line clears
    LaunchedEffect(gs.clearedLineRows) {
        if (gs.clearedLineRows.isNotEmpty()) {
            state.clearSize.intValue = gs.clearedLineRows.size
            state.clearFlashAlpha = (when (gs.clearedLineRows.size) {
                4 -> 0.7f; 3 -> 0.45f; 2 -> 0.3f; else -> 0.15f
            }) * flashMultiplier
            val shakeIntensity = (when (gs.clearedLineRows.size) {
                4 -> 20f; 3 -> 14f; 2 -> 8f; else -> 4f
            }) * shakeMultiplier
            val rng = kotlin.random.Random
            repeat(shakeSteps) { i ->
                val decay = 1f - i.toFloat() / shakeSteps
                state.screenShakeX = (rng.nextFloat() - 0.5f) * shakeIntensity * decay * 2f
                state.screenShakeY = (rng.nextFloat() - 0.5f) * shakeIntensity * decay * 2f
                state.clearFlashAlpha *= 0.82f
                delay(shakeDelay)
            }
            state.screenShakeX = 0f; state.screenShakeY = 0f; state.clearFlashAlpha = 0f
        }
    }

    // Spawn animation — quick scale-up + fade-in on new piece spawn
    LaunchedEffect(gs.spawnEvent) {
        if (gs.spawnEvent > 0) {
            state.spawnScale = 0.85f
            state.spawnAlpha = 0.4f
            val steps = 8
            repeat(steps) { i ->
                val t = (i + 1).toFloat() / steps
                state.spawnScale = 0.85f + t * 0.15f
                state.spawnAlpha = 0.4f + t * 0.6f
                delay(18)
            }
            state.spawnScale = 1f
            state.spawnAlpha = 1f
        }
    }

    // Score flyup
    if (enableFlyup) {
        var lastScore by remember { mutableIntStateOf(gs.score) }
        LaunchedEffect(gs.score) {
            val diff = gs.score - lastScore
            if (diff > 50 && gs.status == GameStatus.PLAYING) {
                state.flyupText = "+$diff"
                state.flyupKey++
            }
            lastScore = gs.score
        }
        LaunchedEffect(state.flyupKey) {
            if (state.flyupKey > 0) {
                state.flyupProgress = 1f
                val steps = 20
                repeat(steps) {
                    state.flyupProgress = 1f - (it + 1).toFloat() / steps
                    delay(30)
                }
                state.flyupProgress = 0f
            }
        }
    }

    // Level up burst
    if (enableLevelBurst) {
        LaunchedEffect(gs.level) {
            if (gs.level > state.prevLevel && state.prevLevel > 0) {
                state.levelUpFlash = 0.8f
                repeat(16) { state.levelUpFlash *= 0.85f; delay(30) }
                state.levelUpFlash = 0f
            }
            state.prevLevel = gs.level
        }
    }

    // Score glow (level 9+)
    if (enableScoreGlow) {
        LaunchedEffect(gs.score) {
            if (gs.score > 0 && gs.level >= 9) {
                state.scoreGlowAlpha = 1f
                repeat(10) { state.scoreGlowAlpha *= 0.8f; delay(30) }
                state.scoreGlowAlpha = 0f
            }
        }
    }

    // Explosion particles + shockwave
    if (enableParticles) {
        LaunchedEffect(gs.clearedLineRows) {
            if (gs.clearedLineRows.isNotEmpty()) {
                val rng = kotlin.random.Random
                val newParticles = mutableListOf<EffectParticle>()
                val colors = listOf(0xFFF4D03F.toInt(), 0xFFFF6B6B.toInt(), 0xFF4ECDC4.toInt(),
                    0xFFFF9F43.toInt(), 0xFFA8E6CF.toInt(), 0xFFFF85A2.toInt(),
                    0xFF6C5CE7.toInt(), 0xFF00B894.toInt(), 0xFFE17055.toInt())
                val isTetrisClear = gs.clearedLineRows.size >= 4

                gs.clearedLineRows.forEach { row ->
                    val rowY = row.toFloat() / 20f
                    val count = when (gs.clearedLineRows.size) { 4 -> 60; 3 -> 40; 2 -> 25; else -> 15 }
                    repeat(count) {
                        val speed = if (isTetrisClear) 0.06f else 0.04f
                        newParticles.add(EffectParticle(
                            x = rng.nextFloat(), y = rowY,
                            vx = (rng.nextFloat() - 0.5f) * speed,
                            vy = (rng.nextFloat() - 0.5f) * speed - 0.015f,
                            size = 2f + rng.nextFloat() * (if (isTetrisClear) 10f else 7f),
                            color = colors[rng.nextInt(colors.size)],
                            life = 1f, type = 0
                        ))
                    }
                    repeat(if (isTetrisClear) 20 else 8) {
                        val angle = rng.nextFloat() * 6.28f
                        val spd = 0.03f + rng.nextFloat() * 0.05f
                        newParticles.add(EffectParticle(
                            x = rng.nextFloat(), y = rowY,
                            vx = kotlin.math.cos(angle) * spd,
                            vy = kotlin.math.sin(angle) * spd,
                            size = 1.5f + rng.nextFloat() * 2f,
                            color = 0xFFFFFFFF.toInt(),
                            life = 1f, type = 1
                        ))
                    }
                }
                state.particles = newParticles
                state.shockwaveY = gs.clearedLineRows.average().toFloat() / 20f
                state.shockwaveCount++

                val totalSteps = if (isTetrisClear) 40 else 30
                repeat(totalSteps) {
                    state.particles = state.particles.mapNotNull { p ->
                        val decay = if (p.type == 1) 0.06f else 0.033f
                        val newLife = p.life - decay
                        if (newLife <= 0f) null
                        else p.copy(x = p.x + p.vx, y = p.y + p.vy,
                            vy = p.vy + 0.0015f, vx = p.vx * 0.98f, life = newLife)
                    }
                    delay(20)
                }
                state.particles = emptyList()
            }
        }
        LaunchedEffect(state.shockwaveCount) {
            if (state.shockwaveCount > 0) {
                state.shockwaveProgress = 0f
                repeat(20) {
                    state.shockwaveProgress = (it + 1) / 20f
                    delay(15)
                }
                state.shockwaveProgress = 0f
            }
        }
    }

    return state
}

/**
 * Renders the visual effects overlay: edge glow, particles, shockwave, flyup, level-up burst, combo glow.
 * Place this INSIDE the board area Box (after GameBoard) so it overlays the board.
 */
@Composable
fun GameEffectsLayer(
    fx: GameEffectsState,
    gs: GameState,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current

    // Combo glow + pulse
    val comboGlow = (gs.comboCount.coerceAtLeast(0) / 8f).coerceIn(0f, 1f)
    val comboPulse = rememberInfiniteTransition(label = "combo")
    val comboPulseAlpha by comboPulse.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "cp"
    )

    // Edge glow flash on line clears
    if (fx.clearFlashAlpha > 0.01f) {
        val flashColor = when {
            fx.clearSize.intValue >= 4 -> Color(0xFFF4D03F)
            fx.clearSize.intValue >= 3 -> Color(0xFFFF9F43)
            fx.clearSize.intValue >= 2 -> Color(0xFF4ECDC4)
            else -> Color.White
        }
        Canvas(modifier) {
            val a = fx.clearFlashAlpha.coerceIn(0f, 1f)
            val edgeW = size.width * 0.12f; val edgeH = size.height * 0.06f
            drawRect(Brush.horizontalGradient(listOf(flashColor.copy(a), Color.Transparent)),
                Offset.Zero, Size(edgeW, size.height))
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, flashColor.copy(a))),
                Offset(size.width - edgeW, 0f), Size(edgeW, size.height))
            drawRect(Brush.verticalGradient(listOf(flashColor.copy(a * 0.7f), Color.Transparent)),
                Offset.Zero, Size(size.width, edgeH))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, flashColor.copy(a * 0.7f))),
                Offset(0f, size.height - edgeH), Size(size.width, edgeH))
        }
    }

    // Level up burst — ring + edge glow
    if (fx.levelUpFlash > 0.01f) {
        Canvas(modifier) {
            val ringProgress = 1f - fx.levelUpFlash / 0.8f
            val ringRadius = size.minDimension * 0.2f + ringProgress * size.maxDimension * 0.6f
            val ringWidth = 8f + (1f - ringProgress) * 20f
            drawCircle(Color(0xFFF4D03F).copy(alpha = fx.levelUpFlash),
                radius = ringRadius, center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(ringWidth))
            val a = (fx.levelUpFlash * 0.5f).coerceIn(0f, 1f)
            val ew = size.width * 0.1f
            drawRect(Brush.horizontalGradient(listOf(Color(0xFFF4D03F).copy(a), Color.Transparent)),
                Offset.Zero, Size(ew, size.height))
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFF4D03F).copy(a))),
                Offset(size.width - ew, 0f), Size(ew, size.height))
        }
    }

    // Combo glow — pulsing border
    if (comboGlow > 0.05f) {
        val pulseWidth = (1.5f + comboGlow * 2f).dp
        val comboColor = Color(0xFFF4D03F).copy(comboGlow * comboPulseAlpha * 0.6f)
        Box(modifier.border(pulseWidth, comboColor))
    }

    // Explosion particles + spark streaks
    if (fx.particles.isNotEmpty()) {
        Canvas(modifier) {
            fx.particles.forEach { p ->
                if (p.type == 1) {
                    drawLine(
                        Color(p.color).copy(alpha = (p.life * p.life).coerceIn(0f, 1f)),
                        start = Offset(p.x * size.width, p.y * size.height),
                        end = Offset((p.x - p.vx * 8f) * size.width, (p.y - p.vy * 8f) * size.height),
                        strokeWidth = p.size * p.life
                    )
                } else {
                    drawCircle(
                        Color(p.color).copy(alpha = (p.life * p.life).coerceIn(0f, 1f)),
                        radius = p.size * (0.5f + p.life * 0.5f),
                        center = Offset(p.x * size.width, p.y * size.height)
                    )
                    if (p.life > 0.4f) {
                        drawCircle(
                            Color.White.copy(alpha = ((p.life - 0.4f) * 1.5f).coerceIn(0f, 0.9f)),
                            radius = p.size * 0.25f * p.life,
                            center = Offset(p.x * size.width, p.y * size.height)
                        )
                    }
                }
            }
        }
    }

    // Shockwave ring
    if (fx.shockwaveProgress > 0.01f && fx.shockwaveProgress < 1f) {
        Canvas(modifier) {
            val maxRadius = size.maxDimension * 0.8f
            val radius = fx.shockwaveProgress * maxRadius
            val ringAlpha = (1f - fx.shockwaveProgress) * 0.6f
            val ringWidth = (1f - fx.shockwaveProgress) * 6f + 2f
            drawCircle(Color.White.copy(alpha = ringAlpha), radius = radius,
                center = Offset(size.width / 2f, fx.shockwaveY * size.height),
                style = Stroke(ringWidth))
            if (fx.shockwaveProgress > 0.1f) {
                val r2 = (fx.shockwaveProgress - 0.1f) / 0.9f * maxRadius
                val a2 = (1f - fx.shockwaveProgress) * 0.3f
                drawCircle(Color(0xFFF4D03F).copy(alpha = a2), radius = r2,
                    center = Offset(size.width / 2f, fx.shockwaveY * size.height),
                    style = Stroke(ringWidth * 0.5f))
            }
        }
    }

    // Score flyup
    if (fx.flyupProgress > 0.01f) {
        val yOff = (1f - fx.flyupProgress) * -80f
        val scale = 0.8f + fx.flyupProgress * 0.4f
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(fx.flyupText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFF4D03F).copy(alpha = (fx.flyupProgress * fx.flyupProgress).coerceIn(0f, 0.95f)),
                modifier = Modifier.graphicsLayer {
                    translationY = yOff; scaleX = scale; scaleY = scale; shadowElevation = 12f
                })
        }
    }
}

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
    boardShape: String = "STANDARD",
    controllerLayoutMode: String = "normal",
    controllerConnected: Boolean = false,
    timerExpired: Boolean = false,
    remainingSeconds: Int = 0,
    pieceMaterial: String = "CLASSIC",
    highContrast: Boolean = false,
    uiScale: Float = 1.0f,
    leftHanded: Boolean = false,
    portraitLayout: LayoutPreset = LayoutPreset.PORTRAIT_CLASSIC,
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

    // Scale only text (sp) by uiScale — board dp/px dimensions stay unaffected
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(baseDensity.density, baseDensity.fontScale * uiScale)

    CompositionLocalProvider(
        LocalMultiColor provides multiColor,
        LocalPieceMaterial provides pieceMaterial,
        LocalHighContrast provides highContrast,
        LocalUiScale provides uiScale,
        LocalButtonShape provides btnShape,
        LocalLeftHanded provides leftHanded,
        LocalDensity provides scaledDensity
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
                    LayoutPreset.LANDSCAPE_DEFAULT -> LandscapeLayout(gameState, dpadStyle, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame, portraitLayout, boardDimAlpha, effectiveNextCount)
                    LayoutPreset.LANDSCAPE_LEFTY -> LandscapeLayout(gameState, dpadStyle, effectiveGhost, animationStyle, animationDuration, onRotate, onHardDrop, effectiveHold, onLeftPress, onLeftRelease, onRightPress, onRightRelease, onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame, portraitLayout, boardDimAlpha, effectiveNextCount)
                    LayoutPreset.PORTRAIT_3D -> {}
                }
                if (gameState.status == GameStatus.PAUSED) PauseOverlay(onResume, onOpenSettings, onQuit)
                // Classic layout handles its own game-over with LCD curtain animation
                if (gameState.status == GameStatus.GAME_OVER && layoutPreset != LayoutPreset.PORTRAIT_CLASSIC)
                    GameOverOverlay(gameState.score, gameState.level, gameState.lines, onStartGame, onOpenSettings, onQuit)
            }
        }
        // Modern notifications — hidden in Classic layout (portrait and landscape) to maintain authentic LCD feel
        val isClassicStyle = layoutPreset == LayoutPreset.PORTRAIT_CLASSIC ||
            (layoutPreset.isLandscape && portraitLayout == LayoutPreset.PORTRAIT_CLASSIC)
        if (!isClassicStyle) {
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
    // Authentic LCD colors — lighter sage green matching real hardware
    val lcdBg = Color(0xFFC8D0B4)       // lighter sage LCD background
    val lcdDark = Color(0xFF2A2E22)      // very dark LCD pixel color
    val lcdGhost = Color(0xFFB0B8A0)     // faded "ghost segment" color
    val lcdLabel = Color(0xFF3A4030)     // bold label text
    val bezelColor = Color(0xFFB0B89C)   // inner bezel

    // === Classic-specific overrides ===
    // No ghost piece, force RETRO blink animation, instant lock
    val classicGhost = false
    val classicAnim = AnimationStyle.RETRO

    // === Game Over curtain animation ===
    // When game over: fill board bottom���top, then show overlay
    var curtainRow by remember { mutableIntStateOf(-1) }
    var showGameOverText by remember { mutableStateOf(false) }
    var curtainPhase by remember { mutableIntStateOf(0) } // 0=idle, 1=filling, 2=clearing, 3=done

    LaunchedEffect(gs.status) {
        if (gs.status == GameStatus.GAME_OVER) {
            showGameOverText = false
            curtainPhase = 1
            // Phase 1: Fill from bottom to top
            for (row in 19 downTo 0) {
                curtainRow = row
                delay(35L)
            }
            delay(300L)
            // Phase 2: Clear from top to bottom
            curtainPhase = 2
            for (row in 0..19) {
                curtainRow = row
                delay(35L)
            }
            curtainPhase = 3
            curtainRow = -1
            delay(200L)
            showGameOverText = true
        } else {
            curtainRow = -1
            curtainPhase = 0
            showGameOverText = false
        }
    }

    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Device frame — olive bezel like real hardware
        Row(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)).background(lcdBg)
            .border(3.dp, bezelColor, RoundedCornerShape(8.dp)).padding(4.dp)) {

            // Board — classic LCD 3-layer cell style with curtain overlay
            Box(Modifier.weight(1f).fillMaxHeight()) {
                GameBoard(gs.board, Modifier.fillMaxSize().alpha(boardDimAlpha),
                    gs.currentPiece, gs.ghostY, classicGhost, gs.clearedLineRows, classicAnim, ad,
                    multiColor = false, classicLCD = true)

                // Curtain overlay — draws filled/empty rows during game over animation
                if (curtainPhase > 0 && curtainRow >= 0) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cellW = size.width / 10f
                        val cellH = size.height / 20f
                        val gap = cellW * 0.06f
                        val lcdLightColor = Color(0xFFC2CCAE)
                        val darkColor = Color(0xFF2A2E22)

                        for (row in 0 until 20) {
                            val shouldFill = when (curtainPhase) {
                                1 -> row >= curtainRow  // filling bottom→top
                                2 -> row <= curtainRow  // clearing top→bottom (row becomes empty)
                                else -> false
                            }
                            val shouldBeOn = (curtainPhase == 1 && shouldFill) || (curtainPhase == 2 && !shouldFill)
                            if (shouldBeOn) {
                                for (col in 0 until 10) {
                                    val x = col * cellW + gap
                                    val y = row * cellH + gap
                                    val w = cellW - gap * 2
                                    val h = cellH - gap * 2
                                    // Draw filled LCD cell
                                    drawRect(darkColor, Offset(x, y), Size(w, h))
                                    val borderW = w * 0.14f
                                    drawRect(lcdLightColor, Offset(x + borderW, y + borderW), Size(w - borderW * 2, h - borderW * 2))
                                    val centerInset = w * 0.28f
                                    drawRect(darkColor, Offset(x + centerInset, y + centerInset), Size(w - centerInset * 2, h - centerInset * 2))
                                }
                            }
                        }
                    }
                }

                // Classic Game Over overlay — simple LCD text, tap to restart
                if (showGameOverText) {
                    Box(Modifier.fillMaxSize().background(lcdBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onStart() }, Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GAME", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace, color = lcdDark, letterSpacing = 6.sp)
                            Text("OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace, color = lcdDark, letterSpacing = 6.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(gs.score.toString(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace, color = lcdDark)
                            Spacer(Modifier.height(16.dp))
                            Text("TAP TO RESTART", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace, color = lcdLabel.copy(0.6f), letterSpacing = 1.sp)
                        }
                    }
                }
            }

            // Vertical separator line
            Box(Modifier.fillMaxHeight().width(2.dp).background(lcdDark.copy(0.4f)))

            // Right info panel — labels LEFT-aligned, numbers RIGHT-aligned
            Column(
                Modifier.width(82.dp).fillMaxHeight().padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // SCORE
                Column {
                    Text("SCORE", fontSize = 12.sp, color = lcdLabel, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        LCDNumber(gs.score, 6, lcdDark, lcdGhost, 14.sp)
                    }
                }

                // LEVELS
                Column {
                    Text("LEVELS", fontSize = 12.sp, color = lcdLabel, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        LCDNumber(gs.level, 6, lcdDark, lcdGhost, 14.sp)
                    }
                }

                // SPEED
                Column {
                    Text("SPEED", fontSize = 12.sp, color = lcdLabel, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    val speed = gs.level.coerceIn(1, 20)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("$speed", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace, color = lcdDark)
                    }
                }

                // NEXT — piece preview with 3-layer LCD cells
                Column {
                    Text("NEXT", fontSize = 12.sp, color = lcdLabel, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(52.dp).background(lcdBg)) {
                        gs.nextPieces.firstOrNull()?.let { p ->
                            LCDPiecePreview(p.shape, lcdDark, lcdBg)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        ClassicControls(dp, onHD, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
    }
}

// === MODERN: Clean info bar + edge-to-edge board + dramatic effects ===
@Composable private fun ModernLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val theme = LocalGameTheme.current
    val isDark = com.brickgame.tetris.ui.theme.LocalIsDarkMode.current
    val animatedScore by animateIntAsState(gs.score, animationSpec = tween(300), label = "score")

    // === Danger zone detection ===
    val currentPieceCells = remember(gs.currentPiece) {
        val cells = mutableSetOf<Long>()
        gs.currentPiece?.let { cp ->
            for (py in cp.shape.indices) for (px in cp.shape[py].indices) {
                if (cp.shape[py][px] > 0) cells.add((cp.position.y + py).toLong() * 100 + (cp.position.x + px))
            }
        }
        cells
    }
    val highestLockedRow = run {
        for (y in gs.board.indices) {
            for (x in gs.board[y].indices) {
                if (gs.board[y][x] > 0 && !currentPieceCells.contains(y.toLong() * 100 + x)) return@run y
            }
        }
        -1
    }
    val dangerLevel = if (highestLockedRow in 0..4 && gs.status == GameStatus.PLAYING) {
        ((5 - highestLockedRow) / 5f).coerceIn(0f, 1f)
    } else 0f
    val dangerPulse = rememberInfiniteTransition(label = "danger")
    val dangerAlpha by dangerPulse.animateFloat(
        0f, if (dangerLevel > 0) dangerLevel * 0.4f else 0f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "da"
    )

    // === Dynamic background — adapt to light/dark mode ===
    val levelHue = (gs.level * 27f) % 360f
    val bgGradientColor = if (isDark) Color.hsl(levelHue, 0.3f, 0.08f) else Color.hsl(levelHue, 0.15f, 0.88f)

    // === Shared game effects (shake, flash, particles, flyup, level burst, score glow) ===
    val fx = rememberGameEffects(gs)

    // === Level 8+: Board breathing — subtle scale pulse ===
    val breathTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by breathTransition.animateFloat(
        0.998f, 1.002f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bs"
    )

    Box(Modifier.fillMaxSize()) {
        // === Falling pieces background — higher alpha, adapts to theme ===
        // Level 10+: background falls faster
        val bgSpeed = if (gs.level >= 10) 1f + (gs.level - 10) * 0.15f else 1f
        Box(Modifier.matchParentSize().alpha(if (isDark) 0.4f else 0.25f)) {
            FallingPiecesBackground(theme, isDark, bgSpeed)
        }

        Column(Modifier.fillMaxSize()) {
            // === COMPACT INFO BAR ===
            Row(Modifier.fillMaxWidth()
                .shadow(6.dp)
                .background((if (isDark) Color.Black else Color.White).copy(0.6f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HOLD", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.45f),
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                    HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(28.dp))
                }
                Spacer(Modifier.width(4.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LVL", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                        Text("${gs.level}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    }
                    // Level 9+: Score glows on change
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SCORE", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                        Text(animatedScore.toString().padStart(7, '0'), fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            color = if (gs.level >= 9 && fx.scoreGlowAlpha > 0.01f)
                                Color(0xFFF4D03F).copy((0.9f + fx.scoreGlowAlpha * 0.1f).coerceAtMost(1f))
                            else (if (isDark) Color.White else Color.Black).copy(0.9f),
                            letterSpacing = 1.sp,
                            modifier = if (gs.level >= 9 && fx.scoreGlowAlpha > 0.01f)
                                Modifier.graphicsLayer { scaleX = 1f + fx.scoreGlowAlpha * 0.15f; scaleY = 1f + fx.scoreGlowAlpha * 0.15f }
                            else Modifier)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LINES", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                        Text("${gs.lines}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace, color = (if (isDark) Color.White else Color.Black).copy(0.7f))
                    }
                }
                Spacer(Modifier.width(4.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NEXT", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.45f),
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        gs.nextPieces.take(nextCount.coerceAtMost(3)).forEachIndexed { i, p ->
                            NextPiecePreview(p.shape, Modifier.size(if (i == 0) 28.dp else 20.dp),
                                alpha = if (i == 0) 1f else 0.5f)
                        }
                    }
                }
            }

            // === Board area with screen shake + breathing ===
            Box(Modifier.weight(1f).fillMaxWidth()
                .graphicsLayer {
                    translationX = fx.screenShakeX; translationY = fx.screenShakeY
                    if (gs.level >= 8) { scaleX = breathScale; scaleY = breathScale }
                }) {
                // Game board — transparent modern grid
                GameBoard(gs.board, Modifier.fillMaxSize().alpha(boardDimAlpha),
                    gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = true,
                    hardDropTrail = gs.hardDropTrail, lockEvent = gs.lockEvent,
                    pieceMaterial = LocalPieceMaterial.current, highContrast = LocalHighContrast.current,
                    boardOpacity = if (isDark) 0.12f else 0.18f, gameLevel = gs.level)

                // === Danger zone overlay ===
                if (dangerAlpha > 0.01f) {
                    Canvas(Modifier.matchParentSize()) {
                        for (i in 0..5) {
                            val rowH = size.height / 20f
                            drawRect(Color.Red.copy(alpha = dangerAlpha * (1f - i / 6f)), Offset(0f, i * rowH), Size(size.width, rowH))
                        }
                    }
                }

                // === All effects: edge glow, particles, shockwave, flyup, level burst, combo ===
                GameEffectsLayer(fx, gs, Modifier.matchParentSize())
            }

            // === Controls ===
            FullControls(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
        }
    }
}

// === FULLSCREEN: Max board, Modern effects, ghost outline controls ===
@Composable private fun FullscreenLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val theme = LocalGameTheme.current
    val isDark = com.brickgame.tetris.ui.theme.LocalIsDarkMode.current
    val levelHue = (gs.level * 36f) % 360f

    val animatedScore by animateIntAsState(gs.score, animationSpec = tween(300), label = "fsscore")
    val fx = rememberGameEffects(gs, shakeDelay = 20L)

    Box(Modifier.fillMaxSize()) {
        // Falling pieces background
        val bgSpeed = if (gs.level >= 10) 1f + (gs.level - 10) * 0.15f else 1f
        Box(Modifier.matchParentSize().alpha(if (isDark) 0.35f else 0.2f)) {
            FallingPiecesBackground(theme, isDark, bgSpeed)
        }

        // Board with shake
        Box(Modifier.fillMaxSize()
            .graphicsLayer { translationX = fx.screenShakeX; translationY = fx.screenShakeY }) {
            GameBoard(gs.board, Modifier.fillMaxSize().alpha(boardDimAlpha), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current,
                hardDropTrail = gs.hardDropTrail, lockEvent = gs.lockEvent, pieceMaterial = LocalPieceMaterial.current, highContrast = LocalHighContrast.current,
                boardOpacity = if (isDark) 0.10f else 0.15f, gameLevel = gs.level)

            GameEffectsLayer(fx, gs, Modifier.matchParentSize())
        }

        // Floating info bar — same style as Modern
        Row(Modifier.fillMaxWidth().align(Alignment.TopCenter)
            .shadow(6.dp)
            .background((if (isDark) Color.Black else Color.White).copy(0.55f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.45f),
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(28.dp))
            }
            Spacer(Modifier.width(4.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LVL", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("${gs.level}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, color = theme.accentColor)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(animatedScore.toString().padStart(7, '0'), fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        color = (if (isDark) Color.White else Color.Black).copy(0.9f), letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LINES", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("${gs.lines}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = (if (isDark) Color.White else Color.Black).copy(0.7f))
                }
            }
            Spacer(Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.45f),
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    gs.nextPieces.take(nextCount.coerceAtMost(3)).forEachIndexed { i, p ->
                        NextPiecePreview(p.shape, Modifier.size(if (i == 0) 28.dp else 20.dp), if (i == 0) 1f else 0.5f)
                    }
                }
            }
        }

        // Ghost outline controls — force OUTLINE shape, very transparent
        CompositionLocalProvider(LocalButtonShape provides ButtonShape.OUTLINE) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().alpha(0.35f)) {
                FullControls(dp, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onRotate, onPause, onSet, onStart, gs.status)
            }
        }
    }
}

// === COMPACT (was One-Hand): Modern style with side panels, D-Pad at bottom ===
@Composable private fun OneHandLayout(
    gs: GameState, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    val isDark = com.brickgame.tetris.ui.theme.LocalIsDarkMode.current
    val animatedScore by animateIntAsState(gs.score, animationSpec = tween(300), label = "chscore")
    val fx = rememberGameEffects(gs, shakeSteps = 14, shakeDelay = 20L, shakeMultiplier = 0.8f)

    Box(Modifier.fillMaxSize()) {
        // Falling pieces background
        val bgSpeed = if (gs.level >= 10) 1f + (gs.level - 10) * 0.15f else 1f
        Box(Modifier.matchParentSize().alpha(if (isDark) 0.3f else 0.2f)) {
            FallingPiecesBackground(theme, isDark, bgSpeed)
        }

        // Dynamic level tint — subtle hue shift
        val levelHue = (gs.level * 27f) % 360f
        val tintColor = if (isDark) Color.hsl(levelHue, 0.25f, 0.06f) else Color.hsl(levelHue, 0.1f, 0.92f)

        Column(Modifier.fillMaxSize()) {
            // Info bar — Modern style with dynamic tint
            Row(Modifier.fillMaxWidth()
                .shadow(6.dp)
                .background(tintColor.copy(0.7f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HOLD", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.45f),
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                    HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(28.dp))
                }
                Spacer(Modifier.width(4.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LVL", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${gs.level}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SCORE", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(animatedScore.toString().padStart(7, '0'), fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            color = (if (isDark) Color.White else Color.Black).copy(0.9f), letterSpacing = 1.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LINES", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.4f),
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${gs.lines}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace, color = (if (isDark) Color.White else Color.Black).copy(0.7f))
                    }
                }
                Spacer(Modifier.width(4.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NEXT", fontSize = 6.sp, color = (if (isDark) Color.White else Color.Black).copy(0.45f),
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        gs.nextPieces.take(2).forEachIndexed { i, p ->
                            NextPiecePreview(p.shape, Modifier.size(if (i == 0) 28.dp else 20.dp), if (i == 0) 1f else 0.5f)
                        }
                    }
                }
            }

            // Board with shake — transparent modern grid
            Box(Modifier.weight(1f).fillMaxWidth()
                .graphicsLayer { translationX = fx.screenShakeX; translationY = fx.screenShakeY }) {
                GameBoard(gs.board, Modifier.fillMaxSize(), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad,
                    multiColor = LocalMultiColor.current, pieceMaterial = LocalPieceMaterial.current, highContrast = LocalHighContrast.current,
                    boardOpacity = if (isDark) 0.12f else 0.18f, gameLevel = gs.level,
                    hardDropTrail = gs.hardDropTrail, lockEvent = gs.lockEvent)

                GameEffectsLayer(fx, gs, Modifier.matchParentSize())
            }

            // Controls — same Compact style DPad layout, respects handedness
            val lh = LocalLeftHanded.current
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                if (!lh) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ActionButton("HOLD", onHold, width = 64.dp, height = 30.dp)
                        ActionButton(if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                            { if (gs.status == GameStatus.MENU) onStart() else onPause() }, width = 64.dp, height = 30.dp)
                        ActionButton("···", onSet, width = 44.dp, height = 24.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
                    }
                } else { Spacer(Modifier.width(64.dp)) }
                DPad(64.dp, rotateInCenter = true, horizontalSpread = 18.dp,
                    onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                    onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
                if (lh) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ActionButton("HOLD", onHold, width = 64.dp, height = 30.dp)
                        ActionButton(if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                            { if (gs.status == GameStatus.MENU) onStart() else onPause() }, width = 64.dp, height = 30.dp)
                        ActionButton("···", onSet, width = 44.dp, height = 24.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
                    }
                } else { Spacer(Modifier.width(64.dp)) }
            }
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
                GameBoard(gs.board, Modifier.fillMaxSize(), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current, pieceMaterial = LocalPieceMaterial.current, highContrast = LocalHighContrast.current)
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

// === LANDSCAPE: Mirrors portrait layout style — DPad left, board center, buttons right ===
@Composable private fun LandscapeLayout(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    portraitStyle: LayoutPreset = LayoutPreset.PORTRAIT_CLASSIC,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val isClassic = portraitStyle == LayoutPreset.PORTRAIT_CLASSIC
    val isFullscreen = portraitStyle == LayoutPreset.PORTRAIT_FULLSCREEN
    if (isClassic) {
        LandscapeClassic(gs, dp, anim, ad, onRotate, onHD, onLP, onLR, onRP, onRR, onDP, onDR, onPause, onSet, onStart, boardDimAlpha, nextCount)
    } else if (isFullscreen) {
        LandscapeFullscreen(gs, dp, ghost, anim, ad, onRotate, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onPause, onSet, onStart, boardDimAlpha, nextCount)
    } else {
        LandscapeModern(gs, dp, ghost, anim, ad, onRotate, onHD, onHold, onLP, onLR, onRP, onRR, onDP, onDR, onPause, onSet, onStart, boardDimAlpha, nextCount)
    }
}

// --- LANDSCAPE CLASSIC: Same sage-green LCD bezel as portrait ---
@Composable private fun LandscapeClassic(
    gs: GameState, dp: DPadStyle, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val lh = LocalLeftHanded.current
    val lcdBg = Color(0xFFC8D0B4)
    val lcdDark = Color(0xFF2A2E22)
    val lcdGhost = Color(0xFFB0B8A0)
    val lcdLabel = Color(0xFF3A4030)
    val bezelColor = Color(0xFFB0B89C)

    val classicGhost = false
    val classicAnim = AnimationStyle.RETRO

    // DPad block
    val dpadBlock: @Composable () -> Unit = {
        Column(Modifier.fillMaxHeight().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            DPad(58.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        }
    }
    // Buttons block — Rotate center-aligned with DPad, PAUSE+menu below
    val buttonsBlock: @Composable () -> Unit = {
        Column(Modifier.fillMaxHeight().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 68.dp)
            Spacer(Modifier.height(8.dp))
            ActionButton(if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                { if (gs.status == GameStatus.MENU) onStart() else onPause() },
                width = 78.dp, height = 34.dp)
            Spacer(Modifier.height(4.dp))
            ActionButton("···", onSet, width = 48.dp, height = 24.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
        }
    }

    Row(Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        // LEFT — DPad or Buttons, centered in outer third
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
            if (!lh) dpadBlock() else buttonsBlock()
        }

        // CENTER — Classic LCD bezel: board + info panel
        Row(Modifier.fillMaxHeight().clip(RoundedCornerShape(8.dp))
            .background(lcdBg).border(3.dp, bezelColor, RoundedCornerShape(8.dp)).padding(4.dp)) {

            // Board — classic LCD, constrained to 10:20 aspect ratio
            GameBoard(gs.board, Modifier.fillMaxHeight().aspectRatio(0.5f).alpha(boardDimAlpha),
                gs.currentPiece, gs.ghostY, classicGhost, gs.clearedLineRows, classicAnim, ad,
                multiColor = false, classicLCD = true)

            // Vertical separator
            Box(Modifier.fillMaxHeight().width(2.dp).background(lcdDark.copy(0.4f)))

            // Right info panel — same as portrait Classic
            Column(
                Modifier.width(82.dp).fillMaxHeight().padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SCORE", fontSize = 12.sp, color = lcdLabel, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        LCDNumber(gs.score, 6, lcdDark, lcdGhost, 14.sp)
                    }
                }
                Column {
                    Text("LEVELS", fontSize = 12.sp, color = lcdLabel, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        LCDNumber(gs.level, 6, lcdDark, lcdGhost, 14.sp)
                    }
                }
                Column {
                    Text("SPEED", fontSize = 12.sp, color = lcdLabel, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    val speed = gs.level.coerceIn(1, 20)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("$speed", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace, color = lcdDark)
                    }
                }
                Column {
                    Text("NEXT", fontSize = 12.sp, color = lcdLabel, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(52.dp).background(lcdBg)) {
                        gs.nextPieces.firstOrNull()?.let { p ->
                            LCDPiecePreview(p.shape, lcdDark, lcdBg)
                        }
                    }
                }
            }
        }

        // RIGHT — Buttons or DPad, centered in outer third
        Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
            if (!lh) buttonsBlock() else dpadBlock()
        }
    }
}

// --- LANDSCAPE FULLSCREEN: Board optionally rotated 90° CW, ghost overlay controls ---
@Composable private fun LandscapeFullscreen(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val theme = LocalGameTheme.current
    val isDark = com.brickgame.tetris.ui.theme.LocalIsDarkMode.current
    val lh = LocalLeftHanded.current
    val animatedScore by animateIntAsState(gs.score, animationSpec = tween(300), label = "fslsscore")
    val textColor = if (isDark) Color.White else Color.Black

    // Board rotation state: cycles 0° → -90° → -180° → -270° → 0°
    // 0° = normal (pieces fall down), -90° = horizontal (pieces fall L→R)
    // -180° = upside down (pieces fall up), -270° = horizontal (pieces fall R→L)
    var rotationStep by remember { mutableIntStateOf(1) } // 0=0°, 1=-90°, 2=-180°, 3=-270°
    val boardRotation = rotationStep * -90f
    val isHorizontal = rotationStep == 1 || rotationStep == 3 // needs swapped dims
    val animatedRotation by animateFloatAsState(boardRotation, animationSpec = tween(400), label = "brot")

    // Shared effects — uses shake but custom edge glow for rotated board
    val fx = rememberGameEffects(gs, shakeDelay = 20L, enableParticles = false, enableFlyup = false, enableLevelBurst = false, enableScoreGlow = false)

    val bgSpeed = if (gs.level >= 10) 1f + (gs.level - 10) * 0.15f else 1f

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight

        // Falling pieces background — full screen
        Box(Modifier.matchParentSize().alpha(if (isDark) 0.25f else 0.15f)) {
            FallingPiecesBackground(theme, isDark, bgSpeed)
        }

        // Board — rotated, fills entire screen
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val boardW = if (isHorizontal) screenH else screenW
            val boardH = if (isHorizontal) screenW else screenH
            // Shake vector rotates with the board
            val shakeX = when (rotationStep) {
                0 -> fx.screenShakeX; 1 -> fx.screenShakeY; 2 -> -fx.screenShakeX; else -> -fx.screenShakeY
            }
            val shakeY = when (rotationStep) {
                0 -> fx.screenShakeY; 1 -> -fx.screenShakeX; 2 -> -fx.screenShakeY; else -> fx.screenShakeX
            }
            Box(Modifier
                .width(boardW).height(boardH)
                .graphicsLayer {
                    rotationZ = animatedRotation
                    translationX = shakeX
                    translationY = shakeY
                }
            ) {
                GameBoard(gs.board, Modifier.fillMaxSize().alpha(boardDimAlpha), gs.currentPiece, gs.ghostY, ghost,
                    gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current,
                    hardDropTrail = gs.hardDropTrail, lockEvent = gs.lockEvent,
                    pieceMaterial = LocalPieceMaterial.current, highContrast = LocalHighContrast.current,
                    boardOpacity = if (isDark) 0.10f else 0.15f, gameLevel = gs.level)
            }
        }

        // Rotation-aware edge glow on line clears
        if (fx.clearFlashAlpha > 0.01f) {
            val flashColor = when {
                fx.clearSize.intValue >= 4 -> Color(0xFFF4D03F)
                fx.clearSize.intValue >= 3 -> Color(0xFFFF9F43)
                fx.clearSize.intValue >= 2 -> Color(0xFF4ECDC4)
                else -> Color.White
            }
            Canvas(Modifier.matchParentSize()) {
                val a = fx.clearFlashAlpha.coerceIn(0f, 1f)
                val edgeW = size.width * 0.08f; val edgeH = size.height * 0.06f
                when (rotationStep) {
                    0 -> {
                        drawRect(Brush.verticalGradient(listOf(flashColor.copy(a * 0.3f), Color.Transparent)), Offset.Zero, Size(size.width, edgeH))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, flashColor.copy(a))), Offset(0f, size.height - edgeH), Size(size.width, edgeH))
                    }
                    1 -> {
                        drawRect(Brush.horizontalGradient(listOf(flashColor.copy(a * 0.3f), Color.Transparent)), Offset.Zero, Size(edgeW, size.height))
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, flashColor.copy(a))), Offset(size.width - edgeW, 0f), Size(edgeW, size.height))
                    }
                    2 -> {
                        drawRect(Brush.verticalGradient(listOf(flashColor.copy(a), Color.Transparent)), Offset.Zero, Size(size.width, edgeH))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, flashColor.copy(a * 0.3f))), Offset(0f, size.height - edgeH), Size(size.width, edgeH))
                    }
                    else -> {
                        drawRect(Brush.horizontalGradient(listOf(flashColor.copy(a), Color.Transparent)), Offset.Zero, Size(edgeW, size.height))
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, flashColor.copy(a * 0.3f))), Offset(size.width - edgeW, 0f), Size(edgeW, size.height))
                    }
                }
            }
        }

        // Floating info strip — thin bar at top
        Row(Modifier.fillMaxWidth().align(Alignment.TopCenter)
            .background((if (isDark) Color.Black else Color.White).copy(0.20f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOLD", fontSize = 5.sp, color = textColor.copy(0.35f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(22.dp))
            }
            Spacer(Modifier.width(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Text("LVL ${gs.level}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor.copy(0.7f))
                Text(animatedScore.toString().padStart(7, '0'), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = textColor.copy(0.6f), letterSpacing = 0.5.sp)
                Text("LNS ${gs.lines}", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = textColor.copy(0.5f))
            }
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", fontSize = 5.sp, color = textColor.copy(0.35f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    gs.nextPieces.take(nextCount.coerceAtMost(3)).forEachIndexed { i, p ->
                        NextPiecePreview(p.shape, Modifier.size(if (i == 0) 22.dp else 16.dp), if (i == 0) 0.8f else 0.4f)
                    }
                }
            }
        }

        // Ghost outline controls — DPad remapped based on rotation
        CompositionLocalProvider(LocalButtonShape provides ButtonShape.OUTLINE) {
            // Left side — DPad
            Box(Modifier.align(if (!lh) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 8.dp).alpha(0.25f)) {
                when (rotationStep) {
                    0 -> // 0°: standard mapping
                        DPad(58.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                            onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                            onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR,
                            onRotate = onRotate)
                    1 -> // -90°: pieces fall L→R. Up=game-right, Down=game-left, Left=hard drop, Right=soft drop
                        DPad(58.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                            onUpPress = onRP, onDownPress = onLP, onDownRelease = onLR,
                            onLeftPress = onHD, onLeftRelease = { },
                            onRightPress = onDP, onRightRelease = onDR,
                            onRotate = onRotate)
                    2 -> // -180°: upside down. Up=soft drop, Down=hard drop, Left=game-right, Right=game-left
                        DPad(58.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                            onUpPress = onDP, onDownPress = onHD, onDownRelease = { },
                            onLeftPress = onRP, onLeftRelease = onRR,
                            onRightPress = onLP, onRightRelease = onLR,
                            onRotate = onRotate)
                    else -> // -270°: pieces fall R→L. Up=game-left, Down=game-right, Left=soft drop, Right=hard drop
                        DPad(58.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                            onUpPress = onLP, onDownPress = onRP, onDownRelease = onRR,
                            onLeftPress = onDP, onLeftRelease = onDR,
                            onRightPress = onHD, onRightRelease = { },
                            onRotate = onRotate)
                }
            }

            // Right side — HOLD, Rotate, PAUSE, menu, board rotation toggle
            Column(Modifier.align(if (!lh) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 8.dp).alpha(0.25f),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                ActionButton("HOLD", onHold, width = 78.dp, height = 34.dp)
                Spacer(Modifier.height(6.dp))
                if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 68.dp)
                Spacer(Modifier.height(6.dp))
                ActionButton(if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                    { if (gs.status == GameStatus.MENU) onStart() else onPause() },
                    width = 78.dp, height = 34.dp)
                Spacer(Modifier.height(4.dp))
                ActionButton("···", onSet, width = 48.dp, height = 24.dp, backgroundColor = theme.buttonSecondary)
                Spacer(Modifier.height(4.dp))
                ActionButton("↻",
                    { rotationStep = (rotationStep + 1) % 4 },
                    width = 48.dp, height = 28.dp, backgroundColor = Color(0xFFCC3333))
            }
        }
    }
}

// --- LANDSCAPE MODERN: Transparent board, falling bg, effects ---
@Composable private fun LandscapeModern(
    gs: GameState, dp: DPadStyle, ghost: Boolean, anim: AnimationStyle, ad: Float,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit,
    boardDimAlpha: Float = 1f, nextCount: Int = 3
) {
    val theme = LocalGameTheme.current
    val isDark = com.brickgame.tetris.ui.theme.LocalIsDarkMode.current
    val lh = LocalLeftHanded.current
    val animatedScore by animateIntAsState(gs.score, animationSpec = tween(300), label = "lsscore")
    val fx = rememberGameEffects(gs, shakeSteps = 14, shakeDelay = 20L, shakeMultiplier = 0.7f, flashMultiplier = 0.85f)

    val bgSpeed = if (gs.level >= 10) 1f + (gs.level - 10) * 0.15f else 1f
    val textColor = if (isDark) Color.White else Color.Black
    // Dynamic level tint
    val levelHue = (gs.level * 27f) % 360f
    val infoPanelTint = if (isDark) Color.hsl(levelHue, 0.2f, 0.08f) else Color.hsl(levelHue, 0.1f, 0.9f)

    // DPad block
    val dpadBlock: @Composable () -> Unit = {
        Column(Modifier.fillMaxHeight().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            DPad(58.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        }
    }
    // Buttons block — HOLD above, Rotate center-aligned with DPad, PAUSE+menu below
    val buttonsBlock: @Composable () -> Unit = {
        Column(Modifier.fillMaxHeight().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ActionButton("HOLD", onHold, width = 78.dp, height = 34.dp)
            Spacer(Modifier.height(8.dp))
            if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 68.dp)
            Spacer(Modifier.height(8.dp))
            ActionButton(if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                { if (gs.status == GameStatus.MENU) onStart() else onPause() },
                width = 78.dp, height = 34.dp)
            Spacer(Modifier.height(4.dp))
            ActionButton("···", onSet, width = 48.dp, height = 24.dp, backgroundColor = theme.buttonSecondary)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Falling pieces background
        Box(Modifier.matchParentSize().alpha(if (isDark) 0.25f else 0.15f)) {
            FallingPiecesBackground(theme, isDark, bgSpeed)
        }

        Row(Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            // LEFT — DPad or Buttons, centered in outer third
            Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
                if (!lh) dpadBlock() else buttonsBlock()
            }

            // CENTER — Board (full height) + vertical info panel
            Row(Modifier.weight(1f).fillMaxHeight()) {
                // Board with shake + effects — fills full height
                Box(Modifier.weight(1f).fillMaxHeight()
                    .graphicsLayer { translationX = fx.screenShakeX; translationY = fx.screenShakeY }) {
                    GameBoard(gs.board, Modifier.fillMaxSize().alpha(boardDimAlpha), gs.currentPiece, gs.ghostY, ghost,
                        gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current,
                        hardDropTrail = gs.hardDropTrail, lockEvent = gs.lockEvent,
                        pieceMaterial = LocalPieceMaterial.current, highContrast = LocalHighContrast.current,
                        boardOpacity = if (isDark) 0.12f else 0.18f, gameLevel = gs.level)

                    GameEffectsLayer(fx, gs, Modifier.matchParentSize())
                }

                // Vertical info panel — flush against board, with dynamic level tint
                Column(Modifier.fillMaxHeight().width(90.dp)
                    .background(infoPanelTint.copy(0.55f), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly) {
                    // HOLD
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HOLD", fontSize = 7.sp, color = textColor.copy(0.45f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(32.dp))
                    }
                    // LVL
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("LVL ", fontSize = 7.sp, color = textColor.copy(0.4f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${gs.level}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    }
                    // SCORE
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SCORE", fontSize = 7.sp, color = textColor.copy(0.4f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(animatedScore.toString().padStart(7, '0'), fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            color = textColor.copy(0.9f), letterSpacing = 0.5.sp)
                    }
                    // LINES
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("LNS ", fontSize = 7.sp, color = textColor.copy(0.4f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${gs.lines}", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = textColor.copy(0.7f))
                    }
                    // NEXT — horizontal row
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEXT", fontSize = 7.sp, color = textColor.copy(0.45f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            gs.nextPieces.take(nextCount.coerceAtMost(3)).forEachIndexed { i, p ->
                                NextPiecePreview(p.shape, Modifier.size(if (i == 0) 26.dp else 20.dp), if (i == 0) 1f else 0.5f)
                            }
                        }
                    }
                }
            }

            // RIGHT — Buttons or DPad, centered in outer third
            Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
                if (!lh) buttonsBlock() else dpadBlock()
            }
        }
    }
}

// === SHARED: Full controls row (ALL buttons at bottom) ===
@Composable private fun FullControls(
    dp: DPadStyle, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onRotate: () -> Unit,
    onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit, status: GameStatus
) {
    val lh = LocalLeftHanded.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        // Left side: DPad (or Rotate if left-handed)
        if (!lh) {
            Box {
                DPad(56.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                    onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                    onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
                Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-2).dp)) {
                    ActionButton("HOLD", onHold, width = 52.dp, height = 26.dp)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 72.dp) else Spacer(Modifier.size(72.dp))
                Spacer(Modifier.height(2.dp))
                ActionButton("HOLD", onHold, width = 52.dp, height = 26.dp)
            }
        }
        // Centre: PAUSE + menu
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton(
                if (status == GameStatus.MENU) "START" else "PAUSE",
                { if (status == GameStatus.MENU) onStart() else onPause() },
                width = 80.dp, height = 34.dp
            )
            ActionButton("...", onSet, width = 48.dp, height = 24.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
        }
        // Right side: Rotate (or DPad if left-handed)
        if (!lh) {
            if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 72.dp) else Spacer(Modifier.size(72.dp))
        } else {
            DPad(56.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        }
    }
}

// === CLASSIC CONTROLS: Same as FullControls but without HOLD button ===
@Composable private fun ClassicControls(
    dp: DPadStyle, onHD: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onRotate: () -> Unit,
    onPause: () -> Unit, onSet: () -> Unit, onStart: () -> Unit, status: GameStatus
) {
    val lh = LocalLeftHanded.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        // Left side
        if (!lh) {
            DPad(50.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        } else {
            if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 60.dp) else Spacer(Modifier.size(60.dp))
        }
        // Centre: PAUSE/START + SETTINGS only (no HOLD)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionButton(
                if (status == GameStatus.MENU) "START" else "PAUSE",
                { if (status == GameStatus.MENU) onStart() else onPause() },
                width = 72.dp, height = 30.dp
            )
            ActionButton("...", onSet, width = 42.dp, height = 22.dp, backgroundColor = LocalGameTheme.current.buttonSecondary)
        }
        // Right side
        if (!lh) {
            if (dp == DPadStyle.STANDARD) RotateButton(onRotate, 60.dp) else Spacer(Modifier.size(60.dp))
        } else {
            DPad(50.dp, rotateInCenter = dp == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
                onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        }
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
                GameBoard(gs.board, Modifier.fillMaxSize(), gs.currentPiece, gs.ghostY, ghost, gs.clearedLineRows, anim, ad, multiColor = LocalMultiColor.current, pieceMaterial = LocalPieceMaterial.current, highContrast = LocalHighContrast.current)
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

/** LCD-style number display — ghost "8" segments behind active digits, like real Brick Game */
@Composable private fun LCDNumber(value: Int, digits: Int, activeColor: Color, ghostColor: Color, fontSize: androidx.compose.ui.unit.TextUnit) {
    val str = value.toString()
    val padded = str.padStart(digits, ' ')
    Row {
        padded.forEach { ch ->
            Box(contentAlignment = Alignment.Center) {
                // Ghost "8" behind every digit position
                Text("8", fontSize = fontSize, fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, color = ghostColor, letterSpacing = 0.5.sp)
                // Active digit on top (space = invisible)
                if (ch != ' ') {
                    Text("$ch", fontSize = fontSize, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, color = activeColor, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

/** LCD piece preview — draws piece with classic beveled cell style */
/** LCD piece preview — draws piece with same 3-layer cell style as the board:
 *  ON: black border → light gap → black center
 *  OFF: subtle border → light gap → faint indent */
@Composable private fun LCDPiecePreview(shape: List<List<Int>>, onColor: Color, bgColor: Color) {
    val lcdLight = Color(0xFFC2CCAE)
    Canvas(Modifier.fillMaxSize()) {
        val rows = shape.size; val cols = shape.maxOfOrNull { it.size } ?: 0
        if (rows == 0 || cols == 0) return@Canvas
        val cellSz = minOf(size.width / cols, size.height / rows)
        val ox = (size.width - cellSz * cols) / 2f; val oy = (size.height - cellSz * rows) / 2f
        val gap = cellSz * 0.06f
        for (r in shape.indices) for (c in shape[r].indices) {
            val off = Offset(ox + c * cellSz + gap, oy + r * cellSz + gap)
            val cs = Size(cellSz - gap * 2, cellSz - gap * 2)
            val w = cs.width; val h = cs.height
            val isOn = shape[r][c] > 0
            if (isOn) {
                // Layer 1: Black outer border
                drawRect(onColor, off, cs)
                // Layer 2: Light gap
                val borderW = w * 0.14f
                drawRect(lcdLight, Offset(off.x + borderW, off.y + borderW), Size(w - borderW * 2, h - borderW * 2))
                // Layer 3: Black center
                val centerInset = w * 0.28f
                drawRect(onColor, Offset(off.x + centerInset, off.y + centerInset), Size(w - centerInset * 2, h - centerInset * 2))
            } else {
                // Faint empty cell
                drawRect(bgColor, off, cs)
                val borderW = w * 0.08f
                drawRect(Color.Black.copy(0.05f), off, Size(w, borderW))
                drawRect(Color.Black.copy(0.05f), off, Size(borderW, h))
                val centerInset = w * 0.28f
                drawRect(bgColor.copy(alpha = 0.9f), Offset(off.x + centerInset, off.y + centerInset), Size(w - centerInset * 2, h - centerInset * 2))
            }
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
    val theme = LocalGameTheme.current
    val isDark = com.brickgame.tetris.ui.theme.LocalIsDarkMode.current
    val bgColor = if (isDark) theme.backgroundColor else Color(0xFFF2F2F2)

    // Staggered entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val titleAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "mta")
    val titleScale by animateFloatAsState(if (visible) 1f else 0.8f, tween(600, easing = FastOutSlowInEasing), label = "mts")
    val scoreAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(400, delayMillis = 200), label = "msa")
    val buttonsAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(400, delayMillis = 400), label = "mba")
    val buttonsSlide by animateFloatAsState(if (visible) 0f else 30f, tween(400, delayMillis = 400, easing = FastOutSlowInEasing), label = "mbs")

    // Pulsing play button glow
    val inf = rememberInfiniteTransition(label = "menuPulse")
    val playPulse by inf.animateFloat(1f, 1.06f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pp")
    val playGlow by inf.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pg")

    Box(Modifier.fillMaxSize().background(bgColor)) {
        FallingPiecesBackground(theme, isDark)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Title with entrance animation
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale; alpha = titleAlpha }) {
                Text("BRICK", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                    color = if (isDark) theme.textPrimary.copy(alpha = 0.9f) else Color(0xFF2A2A2A), letterSpacing = 8.sp)
                Text("GAME", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                    color = if (isDark) theme.accentColor else Color(0xFFB8860B), letterSpacing = 8.sp)
            }
            Spacer(Modifier.height(32.dp))
            // High score with fade-in
            if (hs > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { alpha = scoreAlpha }) {
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
                }
                Spacer(Modifier.height(32.dp))
            }
            // Buttons with slide-up entrance + pulsing PLAY button
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { translationY = buttonsSlide; alpha = buttonsAlpha }) {
                // Pulsing PLAY button with subtle glow
                val playColor = if (isDark) theme.accentColor else Color(0xFFB8860B)
                Box(contentAlignment = Alignment.Center) {
                    // Glow layer behind the button
                    Box(Modifier.matchParentSize()
                        .graphicsLayer { scaleX = playPulse + 0.08f; scaleY = playPulse + 0.08f; alpha = playGlow * 0.3f }
                        .background(playColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)))
                    Box(Modifier.graphicsLayer { scaleX = playPulse; scaleY = playPulse }) {
                        ActionButton("PLAY", onStart, width = 180.dp, height = 52.dp, backgroundColor = playColor)
                    }
                }
                Spacer(Modifier.height(12.dp))
                ActionButton("SETTINGS", onSet, width = 180.dp, height = 44.dp,
                    backgroundColor = if (isDark) theme.buttonSecondary else Color(0xFFE0E0E0))
            }
            // Mini leaderboard
            if (scoreHistory.size > 1) {
                Spacer(Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { alpha = buttonsAlpha }) {
                    Text("RECENT BEST", fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                        color = if (isDark) theme.textSecondary.copy(0.5f) else Color(0xFF999999), letterSpacing = 3.sp)
                    Spacer(Modifier.height(6.dp))
                    val top3 = scoreHistory.sortedByDescending { it.score }.take(3)
                    top3.forEachIndexed { i, entry ->
                        val medal = when (i) { 0 -> "#1"; 1 -> "#2"; 2 -> "#3"; else -> "" }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(medal, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                                color = if (isDark) theme.accentColor.copy(0.6f) else Color(0xFFB8860B))
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
}

// Falling transparent tetris pieces — matrix rain style with colored pieces, long green trails, and sparkle
@Composable
private fun FallingPiecesBackground(theme: com.brickgame.tetris.ui.theme.GameTheme, isDark: Boolean = true, speedMultiplier: Float = 1f) {
    data class FP(val col: Float, val speed: Float, val sz: Float, val shape: Int,
                  val alpha: Float, val startY: Float, val colorIdx: Int, val trailLen: Int,
                  val sparkle: Boolean, val sparklePhase: Float)

    val pieces = remember {
        val rng = kotlin.random.Random(42)
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
            val rawY = p.startY + anim * p.speed * speedMultiplier
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
    // Entrance animation — fade in + slide up
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val bgAlpha by animateFloatAsState(if (visible) 0.75f else 0f, tween(300), label = "pbg")
    val contentAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(350, delayMillis = 80), label = "pca")
    val slideUp by animateFloatAsState(if (visible) 0f else 40f, tween(350, delayMillis = 80, easing = FastOutSlowInEasing), label = "psl")

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgAlpha)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { translationY = slideUp; alpha = contentAlpha }) {
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
    // Entrance animation — staggered reveal
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val bgAlpha by animateFloatAsState(if (visible) 0.88f else 0f, tween(400), label = "gobg")
    val titleAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(350, delayMillis = 100), label = "gotitle")
    val titleScale by animateFloatAsState(if (visible) 1f else 1.4f, tween(500, delayMillis = 100, easing = FastOutSlowInEasing), label = "gots")
    val statsAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(300, delayMillis = 350), label = "gostats")
    val statsSlide by animateFloatAsState(if (visible) 0f else 30f, tween(300, delayMillis = 350, easing = FastOutSlowInEasing), label = "gossl")
    val buttonsAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(300, delayMillis = 550), label = "gobtn")

    // Title pulse
    val inf = rememberInfiniteTransition(label = "go")
    val titlePulse by inf.animateFloat(0.8f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "gp")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgAlpha)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Title with scale entrance
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale; alpha = titleAlpha }) {
                Text("GAME", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFF4444).copy(alpha = titlePulse), letterSpacing = 6.sp)
                Text("OVER", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFF4444).copy(alpha = titlePulse), letterSpacing = 6.sp)
            }
            Spacer(Modifier.height(16.dp))
            // Score + stats with slide-up entrance
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { translationY = statsSlide; alpha = statsAlpha }) {
                // Animated score counter
                val animatedGoScore by animateIntAsState(if (visible) score else 0, tween(800, delayMillis = 400), label = "gosc")
                Text(animatedGoScore.toString(), fontSize = 32.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                // Stats breakdown
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip("LEVEL", "$level")
                    StatChip("LINES", "$lines")
                    StatChip("LPM", if (level > 0) "${"%.1f".format(lines.toFloat() / level)}" else "0")
                }
            }
            Spacer(Modifier.height(24.dp))
            // Buttons with fade-in
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = buttonsAlpha }) {
                ActionButton("AGAIN", onRestart, width = 160.dp, height = 48.dp, backgroundColor = theme.accentColor)
                Spacer(Modifier.height(10.dp))
                ActionButton("LEAVE", onLeave, width = 160.dp, height = 42.dp, backgroundColor = Color(0xFFB91C1C))
            }
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
