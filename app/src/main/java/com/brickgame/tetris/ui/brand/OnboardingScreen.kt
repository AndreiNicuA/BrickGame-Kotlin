package com.brickgame.tetris.ui.brand

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.PlayStyle
import com.brickgame.tetris.game.Difficulty
import kotlinx.coroutines.launch

/**
 * The intro: three swipeable "levels" — the goal, the controls, and you.
 * Swipe or use Back / Next; Skip keeps the current settings.
 */
@Composable
fun OnboardingScreen(
    initialName: String,
    initialSwipe: Boolean,
    initialStyle: PlayStyle,
    initialDifficulty: Difficulty,
    onFinish: (name: String, swipe: Boolean, style: PlayStyle, difficulty: Difficulty) -> Unit,
    onSkip: () -> Unit
) {
    val pages = 3
    val pager = rememberPagerState { pages }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var name by remember { mutableStateOf(if (initialName == "Player") "" else initialName) }
    var swipe by remember { mutableStateOf(initialSwipe) }
    var style by remember { mutableStateOf(initialStyle) }
    var difficulty by remember {
        mutableStateOf(if (initialDifficulty in listOf(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)) initialDifficulty else Difficulty.NORMAL)
    }

    fun finish() { focus.clearFocus(); onFinish(name, swipe, style, difficulty) }

    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    Column(Modifier.fillMaxSize().background(Bw.Ground).safeDrawingPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BwSteps(pages, pager.currentPage, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            if (pager.currentPage < pages - 1) {
                Text("Skip", style = BwType.Label.copy(color = Bw.TextMuted),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onSkip)
                        .heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 12.dp))
            } else Spacer(Modifier.size(44.dp))
        }

        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (page) {
                    0 -> GoalPage()
                    1 -> ControlsPage(swipe) { swipe = it }
                    else -> YouPage(name, { name = it }, style, { style = it }, difficulty, { difficulty = it })
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage > 0) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Bw.Surface)
                    .border(1.dp, Bw.Line, RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
                    .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center) { BwIcon(BwIconKind.CHEVRON_LEFT, Bw.Text) }
                Spacer(Modifier.width(10.dp))
            } else {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    BwIcon(BwIconKind.CHEVRON_LEFT, Bw.TextMuted, size = 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Swipe for next", style = BwType.Small)
                }
            }
            if (pager.currentPage > 0) Spacer(Modifier.weight(1f))
            if (pager.currentPage < pages - 1) {
                BwPrimaryButton("NEXT", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, height = 56.dp)
            } else {
                BwPrimaryButton("LET'S GO", { finish() }, Modifier.weight(3f), height = 56.dp,
                    leading = { BwIcon(BwIconKind.PLAY, Bw.Ground, size = 18.dp) })
            }
        }
    }
}

// ---------- Page 1: the goal ----------

@Composable
private fun GoalPage() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DemoBoard() }
    Text("LEVEL 1 · THE GOAL", style = BwType.Overline.copy(color = Bw.Cyan))
    Text("Stack 'em.\nClear 'em.", style = BwType.Title)
    Text("Fill a row edge to edge and it blows up. Clear four at once for a QUAD. Let the stack reach the top and it's game over.",
        style = BwType.Body)
}

/**
 * Looping demo: an I piece slides into the gap, four rows flash and vanish, "+800 QUAD!" pops.
 * Drawn in one Canvas; the animation value is read only while drawing.
 */
@Composable
private fun DemoBoard() {
    val t = rememberInfiniteTransition(label = "demo").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "t"
    )
    val stack = remember {
        // 14 rows × 10, bottom 4 rows full except column 9; a few blocks above
        val rows = arrayOf(
            "..........", "..........", "..........", "..........", "..........", "..........",
            "..........", "..........", "L.........", "LL.....OO.",
            "ZZZZSSSSJ.", "ZZSSSSJJJ.", "TTTZZOOJJ.", "TLLLZZOOJ."
        )
        rows.map { it.toCharArray() }
    }
    val colors = mapOf('T' to Bw.Violet, 'L' to Bw.Orange, 'O' to Bw.Amber, 'Z' to Bw.Pink, 'S' to Bw.Lime, 'J' to Color(0xFF60A5FA), 'I' to Bw.Cyan)
    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Bw.SurfaceDeep).border(1.dp, Bw.Line, RoundedCornerShape(16.dp)).padding(10.dp)) {
        Canvas(Modifier.size(width = 218.dp, height = 306.dp)) {
            val v = t.value
            val cols = 10; val rowsN = 14
            val cell = size.width / cols
            val gap = cell * 0.09f
            val dropEnd = 0.45f; val flashEnd = 0.7f
            val cleared = v >= flashEnd
            // Board
            for (y in 0 until rowsN) for (x in 0 until cols) {
                val ch = stack[y][x]
                val isClearRow = y >= 10
                val o = Offset(x * cell + gap, y * cell + gap); val s = Size(cell - gap * 2, cell - gap * 2)
                val filled = ch != '.' && !(cleared && isClearRow)
                if (cleared && !isClearRow && ch != '.') {
                    // rows above fall down by 4 after the clear
                    val fall = ((v - flashEnd) / 0.12f).coerceIn(0f, 1f) * 4 * cell
                    drawRoundRect(Color(0xFF151B30), o, s, CornerRadius(cell * 0.18f))
                    drawRoundRect(colors[ch] ?: Bw.Cyan, Offset(o.x, o.y + fall), s, CornerRadius(cell * 0.18f))
                    continue
                }
                drawRoundRect(Color(0xFF151B30), o, s, CornerRadius(cell * 0.18f))
                if (filled) {
                    val flashing = isClearRow && v in dropEnd..flashEnd
                    val base = colors[ch] ?: Bw.Cyan
                    val col = if (flashing) (if (((v - dropEnd) * 18).toInt() % 2 == 0) Color(0xFFFEF3C7) else base) else base
                    drawRoundRect(col, o, s, CornerRadius(cell * 0.18f))
                }
            }
            // The vertical I piece dropping into column 9
            if (v < flashEnd) {
                val p = (v / dropEnd).coerceIn(0f, 1f)
                val eased = p * p
                val topRow = -4f + eased * (10f + 4f)
                for (i in 0 until 4) {
                    val y = topRow + i
                    if (y >= 0) {
                        val flashing = v in dropEnd..flashEnd && ((v - dropEnd) * 18).toInt() % 2 == 0
                        drawRoundRect(if (flashing) Color(0xFFFEF3C7) else Bw.Cyan,
                            Offset(9 * cell + gap, y * cell + gap), Size(cell - gap * 2, cell - gap * 2), CornerRadius(cell * 0.18f))
                    }
                }
                // ghost outline at the landing spot
                if (v < dropEnd) for (i in 0 until 4) {
                    drawRoundRect(Bw.Cyan.copy(alpha = 0.45f), Offset(9 * cell + gap * 2, (10 + i) * cell + gap * 2),
                        Size(cell - gap * 4, cell - gap * 4), CornerRadius(cell * 0.18f), style = Stroke(cell * 0.07f))
                }
            }
        }
        // "+800 QUAD!" pops up when the rows clear; position/alpha are applied in the graphics layer
        Text("+800 QUAD!",
            style = BwType.Label.copy(color = Bw.Ground, fontFamily = Bw.Display, fontSize = 15.sp),
            modifier = Modifier.align(Alignment.Center)
                .graphicsLayer {
                    val v = t.value
                    alpha = if (v < 0.45f || v > 0.92f) 0f
                        else ((v - 0.45f) / 0.08f).coerceIn(0f, 1f) * (1f - ((v - 0.82f) / 0.1f).coerceIn(0f, 1f))
                    translationY = 80.dp.toPx() - (v - 0.45f).coerceAtLeast(0f) * 160.dp.toPx()
                }
                .clip(RoundedCornerShape(8.dp)).background(Bw.Amber).padding(horizontal = 10.dp, vertical = 4.dp))
    }
}


// ---------- Page 2: controls ----------

@Composable
private fun ControlsPage(swipe: Boolean, onChange: (Boolean) -> Unit) {
    Text("LEVEL 2 · YOUR HANDS", style = BwType.Overline.copy(color = Bw.Cyan))
    Text("How do you want to play?", style = BwType.Title)
    Text("You can switch any time in Settings.", style = BwType.Body)

    BwCard(swipe, { onChange(true) }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Swipe", style = BwType.CardTitle.copy(fontSize = 20.sp), modifier = Modifier.weight(1f))
                RadioDot(swipe)
            }
            val gestures = listOf("↔" to ("Drag" to "Move"), "•" to ("Tap" to "Rotate"), "↓" to ("Flick down" to "Drop"), "↑" to ("Swipe up" to "Hold"))
            gestures.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (glyph, md) ->
                        Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Bw.SurfaceDeep).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Bw.Raised), contentAlignment = Alignment.Center) {
                                Text(glyph, style = BwType.CardTitle.copy(color = Bw.Cyan, fontSize = 16.sp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(md.first, style = BwType.Label.copy(fontSize = 13.sp))
                                Text(md.second, style = BwType.Small.copy(fontSize = 12.sp))
                            }
                        }
                    }
                }
            }
            Text("The whole screen is the board. Nothing in the way.", style = BwType.Small)
        }
    }

    BwCard(!swipe, { onChange(false) }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Canvas(Modifier.size(56.dp)) {
            val c = size.width / 3.4f
            for ((x, y) in listOf(1 to 0, 0 to 1, 2 to 1, 1 to 2)) {
                drawRoundRect(Bw.LineStrong, Offset(x * c * 1.13f, y * c * 1.13f), Size(c, c), CornerRadius(c * 0.25f))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Buttons", style = BwType.CardTitle.copy(fontSize = 20.sp))
            Text("D-pad and action buttons, like the handheld", style = BwType.Small)
        }
        RadioDot(!swipe)
    }
}

@Composable
private fun RadioDot(on: Boolean) {
    Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp))
        .then(if (on) Modifier.background(Bw.Cyan) else Modifier.border(2.dp, Bw.LineStrong, RoundedCornerShape(12.dp))),
        contentAlignment = Alignment.Center) {
        if (on) BwIcon(BwIconKind.CHECK, Bw.Ground, size = 14.dp)
    }
}

// ---------- Page 3: you ----------

@Composable
private fun YouPage(
    name: String, onName: (String) -> Unit,
    style: PlayStyle, onStyle: (PlayStyle) -> Unit,
    difficulty: Difficulty, onDifficulty: (Difficulty) -> Unit
) {
    val focus = LocalFocusManager.current
    Text("LEVEL 3 · PLAYER ONE", style = BwType.Overline.copy(color = Bw.Cyan))
    Text("Sign the high score table", style = BwType.Title)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Player name", style = BwType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        BasicTextField(
            value = name,
            onValueChange = { onName(it.take(16)) },
            singleLine = true,
            textStyle = BwType.CardTitle.copy(fontFamily = Bw.Body, fontSize = 18.sp),
            cursorBrush = SolidColor(Bw.Cyan),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Bw.Surface)
                .border(2.dp, Bw.Cyan, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp)
                .semantics { contentDescription = "Player name" },
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (name.isEmpty()) Text("Your name", style = BwType.CardTitle.copy(fontFamily = Bw.Body, fontSize = 18.sp, color = Bw.TextMuted))
                    inner()
                }
            }
        )
    }

    Text("Start in", style = BwType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StyleTile("Classic", style == PlayStyle.CLASSIC, Bw.LcdGreen, Bw.LcdInk, Modifier.weight(1f)) { onStyle(PlayStyle.CLASSIC) }
        StyleTile("Neon", style == PlayStyle.NEON, Bw.Surface, Bw.Pink, Modifier.weight(1f)) { onStyle(PlayStyle.NEON) }
        StyleTile("3D", style == PlayStyle.THREE_D, Bw.Surface, Bw.Violet, Modifier.weight(1f)) { onStyle(PlayStyle.THREE_D) }
    }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Bw.Surface).border(1.dp, Bw.Line, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Speed", style = BwType.Label)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Difficulty.EASY to "Chill", Difficulty.NORMAL to "Normal", Difficulty.HARD to "Fast").forEach { (d, label) ->
                val on = d == difficulty
                Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (on) Bw.Cyan else Bw.SurfaceDeep)
                    .then(if (on) Modifier else Modifier.border(1.dp, Bw.Line, RoundedCornerShape(12.dp)))
                    .clickable(role = Role.Button) { onDifficulty(d) },
                    contentAlignment = Alignment.Center) {
                    Text(label, style = BwType.Label.copy(fontSize = 13.sp, color = if (on) Bw.Ground else Bw.TextSoft))
                }
            }
        }
    }
}

@Composable
private fun StyleTile(label: String, selected: Boolean, bg: Color, ink: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(bg)
        .border(if (selected) 2.dp else 1.dp, if (selected) Bw.Cyan else Bw.Line, RoundedCornerShape(16.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(34.dp, 22.dp)) {
            val c = size.width / 3.2f
            for ((x, y) in listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1)) {
                drawRoundRect(ink, Offset(x * c * 1.1f, y * c * 1.1f), Size(c, c), CornerRadius(c * 0.2f))
            }
        }
        Text(label, style = BwType.Label.copy(fontFamily = Bw.Display, color = if (bg == Bw.LcdGreen) Bw.LcdInk else Bw.Text))
    }
}
