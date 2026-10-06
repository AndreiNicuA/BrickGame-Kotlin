package com.brickgame.tetris.ui.brand

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.game.PieceState
import com.brickgame.tetris.ui.components.PIECE_COLORS

// ======================= In-game design system (Neon) =======================

enum class Glyph { LEFT, RIGHT, UP, DOWN, ROTATE, TILT, DROP, PAUSE }

/** Stroke glyphs for the game controls, drawn on a 24×24 grid. */
@Composable
fun GlyphIcon(g: Glyph, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 24f
        val st = Stroke(2.8f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun path(vararg xy: Float) = Path().apply {
            moveTo(xy[0] * k, xy[1] * k); var i = 2
            while (i < xy.size) { lineTo(xy[i] * k, xy[i + 1] * k); i += 2 }
        }
        when (g) {
            Glyph.LEFT -> drawPath(path(15f, 5f, 8f, 12f, 15f, 19f), color, style = st)
            Glyph.RIGHT -> drawPath(path(9f, 5f, 16f, 12f, 9f, 19f), color, style = st)
            Glyph.DOWN -> drawPath(path(5f, 9f, 12f, 16f, 19f, 9f), color, style = st)
            Glyph.UP -> drawPath(path(5f, 15f, 12f, 8f, 19f, 15f), color, style = st)
            Glyph.TILT -> {
                drawArc(color, 120f, 300f, false, Offset(4f * k, 4f * k), Size(16f * k, 16f * k), style = st)
                drawPath(path(4f, 3.5f, 4f, 9f, 9.5f, 9f), color, style = st)
            }
            Glyph.DROP -> {
                drawPath(path(12f, 4f, 12f, 16f), color, style = st)
                drawPath(path(6f, 11f, 12f, 17f, 18f, 11f), color, style = st)
                drawPath(path(5f, 21f, 19f, 21f), color, style = st)
            }
            Glyph.ROTATE -> {
                drawArc(color, -60f, 300f, false, Offset(4f * k, 4f * k), Size(16f * k, 16f * k), style = st)
                drawPath(path(20f, 3.5f, 20f, 9f, 14.5f, 9f), color, style = st)
            }
            Glyph.PAUSE -> {
                drawRoundRect(color, Offset(6f * k, 5f * k), Size(4f * k, 14f * k), CornerRadius(k))
                drawRoundRect(color, Offset(14f * k, 5f * k), Size(4f * k, 14f * k), CornerRadius(k))
            }
        }
    }
}

/**
 * A game button: fires on touch-down (no input lag); [onRelease] fires when the finger lifts or
 * the button leaves the screen, which ends auto-repeat for move buttons.
 */
@Composable
fun BwPadButton(
    label: String,
    modifier: Modifier,
    shape: Shape,
    fill: Color,
    border: Color?,
    onPress: () -> Unit,
    onRelease: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val press by rememberUpdatedState(onPress)
    val release by rememberUpdatedState(onRelease)
    Box(
        modifier
            .graphicsLayer { val s = if (pressed) 0.92f else 1f; scaleX = s; scaleY = s }
            .clip(shape)
            .background(if (pressed) fill.copy(alpha = (fill.alpha * 0.75f).coerceAtLeast(0.2f)) else fill)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressed }) {
                            pressed = true; press()
                            try {
                                while (true) {
                                    val e = awaitPointerEvent()
                                    if (e.changes.all { !it.pressed }) break
                                }
                            } finally { pressed = false; release() }
                        }
                    }
                }
            }
            .semantics { role = Role.Button; contentDescription = label; this.onClick { press(); release(); true } },
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** The Neon control deck: move/soft-drop on the left, hold/rotate/drop on the right. */
@Composable
fun NeonControls(
    leftHanded: Boolean,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onRotate: () -> Unit, onHardDrop: () -> Unit, onHold: () -> Unit,
    accent: Color = Bw.Cyan,
    modifier: Modifier = Modifier
) {
    val move = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BwPadButton("Move left", Modifier.size(68.dp), RoundedCornerShape(22.dp), Bw.Raised, Bw.LineStrong, onLeftPress, onLeftRelease) {
                    GlyphIcon(Glyph.LEFT, accent, 26.dp)
                }
                BwPadButton("Move right", Modifier.size(68.dp), RoundedCornerShape(22.dp), Bw.Raised, Bw.LineStrong, onRightPress, onRightRelease) {
                    GlyphIcon(Glyph.RIGHT, accent, 26.dp)
                }
            }
            BwPadButton("Soft drop", Modifier.size(144.dp, 52.dp), RoundedCornerShape(18.dp), Bw.Raised, Bw.LineStrong, onDownPress, onDownRelease) {
                GlyphIcon(Glyph.DOWN, accent, 26.dp)
            }
        }
    }
    val act = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                BwPadButton("Hold piece", Modifier.size(56.dp), RoundedCornerShape(18.dp), Bw.Raised, Bw.LineStrong, onHold) {
                    Text("HOLD", style = BwType.Overline.copy(color = Bw.Text, fontSize = 11.sp, letterSpacing = 1.sp))
                }
                BwPadButton("Rotate", Modifier.size(84.dp), CircleShape, accent, null, onRotate) {
                    GlyphIcon(Glyph.ROTATE, Bw.Ground, 34.dp)
                }
            }
            BwPadButton("Hard drop", Modifier.size(148.dp, 52.dp), RoundedCornerShape(18.dp), Bw.Pink, null, onHardDrop) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.DROP, Bw.Ground, 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("DROP", style = BwType.Button.copy(fontSize = 15.sp, letterSpacing = 2.sp, color = Bw.Ground))
                }
            }
        }
    }
    Row(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        if (leftHanded) { act(); move() } else { move(); act() }
    }
}

/** A piece drawn in its own colour, centred (hold / next previews). */
@Composable
fun BwPiecePreview(piece: PieceState?, modifier: Modifier, dim: Boolean = false) {
    Canvas(modifier) {
        val shape = piece?.shape ?: return@Canvas
        val cells = ArrayList<Pair<Int, Int>>()
        for (y in shape.indices) for (x in shape[y].indices) if (shape[y][x] > 0) cells += x to y
        if (cells.isEmpty()) return@Canvas
        val minX = cells.minOf { it.first }; val maxX = cells.maxOf { it.first }
        val minY = cells.minOf { it.second }; val maxY = cells.maxOf { it.second }
        val w = maxX - minX + 1; val h = maxY - minY + 1
        val c = minOf(size.width / 4f, size.height / 4f)
        val ox = (size.width - w * c) / 2f; val oy = (size.height - h * c) / 2f
        val color = PIECE_COLORS.getOrElse(piece.type.ordinal + 1) { Bw.Cyan }.copy(alpha = if (dim) 0.35f else 1f)
        for ((x, y) in cells) {
            drawRoundRect(color, Offset(ox + (x - minX) * c + c * 0.06f, oy + (y - minY) * c + c * 0.06f),
                Size(c * 0.88f, c * 0.88f), CornerRadius(c * 0.22f))
        }
    }
}

/** Score that rolls up to its new value; its own scope so the roll recomposes only this text. */
@Composable
fun BwRollingScore(score: Int, fontSize: Int = 26) {
    val animated by animateIntAsState(score, tween(300), label = "bwScore")
    Text("%,d".format(animated), style = BwType.Title.copy(fontSize = fontSize.sp, lineHeight = fontSize.sp, letterSpacing = 1.sp))
}

/** Neon in-game info bar: HOLD · score / level · lines · NEXT · pause. */
@Composable
fun NeonHud(gs: GameState, nextCount: Int, onPause: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        HudBox(Modifier.width(62.dp)) {
            Text("HOLD", style = BwType.Overline.copy(fontSize = 10.sp, letterSpacing = 1.sp))
            BwPiecePreview(gs.holdPiece, Modifier.size(40.dp, 28.dp), dim = gs.holdUsed)
        }
        HudBox(Modifier.weight(1f)) {
            BwRollingScore(gs.score)
            Text(buildString {
                append("LV ${gs.level}  ·  ${gs.lines} LINES")
                if (gs.comboCount >= 2) append("  ·  COMBO x${gs.comboCount}")
            }, style = BwType.Small.copy(fontSize = 11.sp))
        }
        HudBox(Modifier.width(62.dp)) {
            Text("NEXT", style = BwType.Overline.copy(fontSize = 10.sp, letterSpacing = 1.sp))
            gs.nextPieces.take(nextCount.coerceIn(1, 2)).forEachIndexed { i, p ->
                BwPiecePreview(p, Modifier.size(if (i == 0) 40.dp else 30.dp, if (i == 0) 24.dp else 16.dp), dim = i > 0)
            }
        }
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Bw.Surface)
            .border(1.dp, Bw.Line, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onPause).semantics { contentDescription = "Pause" },
            contentAlignment = Alignment.Center) { GlyphIcon(Glyph.PAUSE, Bw.Text, 18.dp) }
    }
}

@Composable
private fun HudBox(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.heightIn(min = 64.dp).clip(RoundedCornerShape(14.dp)).background(Bw.Surface)
        .border(1.dp, Bw.Line, RoundedCornerShape(14.dp)).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        content = content)
}

// ======================= Overlays =======================

@Composable
private fun overlayEnter(): Float {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val t by animateFloatAsState(if (shown) 1f else 0f, tween(320, easing = FastOutSlowInEasing), label = "ovIn")
    return t
}

@Composable
fun BwPauseOverlay(onResume: () -> Unit, onSettings: () -> Unit, onQuit: () -> Unit) {
    val t = overlayEnter()
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = t }.background(Bw.Ground.copy(alpha = 0.88f))
        .clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 340.dp).padding(24.dp).graphicsLayer { translationY = (1f - t) * 40f },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PAUSED", style = BwType.Overline.copy(color = Bw.Cyan, fontSize = 13.sp, letterSpacing = 4.sp))
            Text("Take a breath.", style = BwType.Title)
            Spacer(Modifier.height(12.dp))
            BwPrimaryButton("RESUME", onResume, Modifier.fillMaxWidth(), height = 60.dp,
                leading = { BwIcon(BwIconKind.PLAY, Bw.Ground, size = 18.dp) })
            BwSecondaryButton("Settings", onSettings, Modifier.fillMaxWidth(),
                leading = { BwIcon(BwIconKind.SETTINGS, Bw.Text, size = 18.dp) })
            BwSecondaryButton("Leave game", onQuit, Modifier.fillMaxWidth(),
                leading = { BwIcon(BwIconKind.CHEVRON_LEFT, Bw.Pink, size = 18.dp) })
        }
    }
}

@Composable
fun BwGameOverOverlay(
    score: Int, level: Int, lines: Int, highScore: Int,
    maxCombo: Int, backToBack: Int, elapsedMs: Long,
    onAgain: () -> Unit, onLeave: () -> Unit,
    linesLabel: String = "LINES"
) {
    val t = overlayEnter()
    val isNewBest = score > 0 && score >= highScore
    val shownScore by animateIntAsState(if (t > 0f) score else 0, tween(900, delayMillis = 250), label = "goScore")
    val time = remember(elapsedMs) { val s = (elapsedMs / 1000).toInt(); "%02d:%02d".format(s / 60, s % 60) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = t }.background(Bw.Ground.copy(alpha = 0.92f))
        .clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp).verticalScroll(rememberScrollState()).padding(24.dp)
            .graphicsLayer { translationY = (1f - t) * 40f },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("GAME OVER", style = BwType.Overline.copy(color = Bw.Pink, fontSize = 13.sp, letterSpacing = 4.sp))
            if (isNewBest) BwTag("NEW BEST", Bw.Amber)
            Text("%,d".format(shownScore), style = BwType.Hero.copy(fontSize = 52.sp, lineHeight = 54.sp))
            Text("points", style = BwType.Small)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("LEVEL", "$level", Bw.Cyan, Modifier.weight(1f))
                Stat(linesLabel, "$lines", Bw.Lime, Modifier.weight(1f))
                if (elapsedMs > 0) Stat("TIME", time, Bw.Violet, Modifier.weight(1f))
            }
            if (maxCombo > 1 || backToBack > 0) {
                Text(buildString {
                    if (maxCombo > 1) append("Best combo x$maxCombo")
                    if (backToBack > 0) { if (isNotEmpty()) append("  ·  "); append("Back-to-back x$backToBack") }
                }, style = BwType.Small.copy(color = Bw.Amber))
            }
            Spacer(Modifier.height(10.dp))
            BwPrimaryButton("PLAY AGAIN", onAgain, Modifier.fillMaxWidth(), height = 60.dp,
                leading = { BwIcon(BwIconKind.PLAY, Bw.Ground, size = 18.dp) })
            BwSecondaryButton("Back to menu", onLeave, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Bw.Surface).border(1.dp, Bw.Line, RoundedCornerShape(14.dp))
        .padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = BwType.CardTitle.copy(fontSize = 20.sp, color = color))
        Text(label, style = BwType.Overline.copy(fontSize = 10.sp, letterSpacing = 1.sp))
    }
}

/** Status helper so callers don't need GameStatus imports for the common checks. */
fun GameState.isPlaying() = status == GameStatus.PLAYING
