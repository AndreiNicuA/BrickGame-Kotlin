package com.brickgame.tetris.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.brickgame.tetris.data.CustomLayoutData
import com.brickgame.tetris.data.ElementPosition
import com.brickgame.tetris.data.LayoutElements
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.layout.DPadStyle
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.GameTheme
import kotlin.math.roundToInt

private val PANEL_BG = Color(0xFF0D0D0D)
private val CARD_BG = Color(0xFF1A1A1A)
private val ACC = Color(0xFFF4D03F)
private val TX = Color(0xFFE8E8E8)
private val DIM = Color(0xFF888888)
private val EDIT_BORDER = Color(0xFF22C55E)

// ======================================
// LAYOUT EDITOR — Real elements, draggable
// ======================================
@Composable
fun LayoutEditorScreen(
    layout: CustomLayoutData,
    theme: GameTheme,
    basePreset: LayoutPreset,
    dpadStyle: DPadStyle,
    onUpdateLayout: (CustomLayoutData) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var layoutName by remember(layout.id) { mutableStateOf(layout.name) }
    var showPanel by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        // Header bar
        Row(Modifier.fillMaxWidth().background(PANEL_BG).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = ACC, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(6.dp))
            BasicTextField(layoutName, { layoutName = it; onUpdateLayout(layout.copy(name = it)) },
                textStyle = TextStyle(color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Text(if (showPanel) "▼" else "▲", color = DIM, fontSize = 16.sp, modifier = Modifier.clickable { showPanel = !showPanel }.padding(6.dp))
            Text("SAVE", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 14.dp, vertical = 6.dp))
        }

        // ===== DRAG AREA: real elements at their positions =====
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val density = LocalDensity.current
            val maxWPx = with(density) { maxWidth.toPx() }
            val maxHPx = with(density) { maxHeight.toPx() }
            val pos = layout.positions
            val vis = layout.visibility

            // Instruction
            Text("Drag elements to reposition · Use panel below to show/hide", color = Color.White.copy(0.35f), fontSize = 10.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 2.dp))

            // ---- BOARD ----
            if (vis.getOrDefault(LayoutElements.BOARD, true)) {
                val bp = pos[LayoutElements.BOARD] ?: ElementPosition(0.5f, 0.4f)
                val bW = with(density) { (maxWidth * 0.75f).toPx() }
                val bH = with(density) { (maxHeight * 0.55f).toPx() }
                DraggableWrapper("Board", bp, maxWPx, maxHPx, bW, bH) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.BOARD to newPos)))
                } {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(theme.screenBackground)) {
                        // Show a static grid placeholder
                        Column(Modifier.fillMaxSize().padding(2.dp), Arrangement.SpaceEvenly) {
                            repeat(6) { r ->
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                                    repeat(10) { c ->
                                        val on = (r + c) % 4 == 0
                                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(1.dp)).background(if (on) theme.pixelOn else theme.pixelOff))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- SCORE ----
            if (vis.getOrDefault(LayoutElements.SCORE, true)) {
                val sp2 = pos[LayoutElements.SCORE] ?: ElementPosition(0.5f, 0.03f)
                DraggableWrapper("Score", sp2, maxWPx, maxHPx, with(density) { 120.dp.toPx() }, with(density) { 24.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.SCORE to newPos)))
                } {
                    Text("0001234", color = theme.accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(theme.deviceColor.copy(0.5f)).padding(horizontal = 4.dp))
                }
            }

            // ---- LEVEL ----
            if (vis.getOrDefault(LayoutElements.LEVEL, true)) {
                val lp = pos[LayoutElements.LEVEL] ?: ElementPosition(0.15f, 0.03f)
                DraggableWrapper("Level", lp, maxWPx, maxHPx, with(density) { 46.dp.toPx() }, with(density) { 18.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.LEVEL to newPos)))
                } {
                    Text("LV 1", color = theme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(theme.deviceColor.copy(0.5f)).wrapContentAlignment(Alignment.Center))
                }
            }

            // ---- LINES ----
            if (vis.getOrDefault(LayoutElements.LINES, true)) {
                val lp = pos[LayoutElements.LINES] ?: ElementPosition(0.85f, 0.03f)
                DraggableWrapper("Lines", lp, maxWPx, maxHPx, with(density) { 46.dp.toPx() }, with(density) { 18.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.LINES to newPos)))
                } {
                    Text("0 L", color = theme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(theme.deviceColor.copy(0.5f)).wrapContentAlignment(Alignment.Center))
                }
            }

            // ---- HOLD PREVIEW ----
            if (vis.getOrDefault(LayoutElements.HOLD_PREVIEW, true)) {
                val hp = pos[LayoutElements.HOLD_PREVIEW] ?: ElementPosition(0.1f, 0.1f)
                DraggableWrapper("Hold", hp, maxWPx, maxHPx, with(density) { 52.dp.toPx() }, with(density) { 58.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.HOLD_PREVIEW to newPos)))
                } {
                    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(theme.deviceColor.copy(0.5f)).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HOLD", color = theme.textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground.copy(0.3f)))
                    }
                }
            }

            // ---- NEXT PREVIEW ----
            if (vis.getOrDefault(LayoutElements.NEXT_PREVIEW, true)) {
                val np = pos[LayoutElements.NEXT_PREVIEW] ?: ElementPosition(0.9f, 0.1f)
                DraggableWrapper("Next", np, maxWPx, maxHPx, with(density) { 52.dp.toPx() }, with(density) { 58.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.NEXT_PREVIEW to newPos)))
                } {
                    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(theme.deviceColor.copy(0.5f)).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEXT", color = theme.textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground.copy(0.3f)))
                    }
                }
            }

            // ---- D-PAD ----
            if (vis.getOrDefault(LayoutElements.DPAD, true)) {
                val dp2 = pos[LayoutElements.DPAD] ?: ElementPosition(0.2f, 0.82f)
                val dpadSz = when (layout.controlSize) { "SMALL" -> 110f; "LARGE" -> 160f; else -> 140f }
                DraggableWrapper("D-Pad", dp2, maxWPx, maxHPx, with(density) { dpadSz.dp.toPx() }, with(density) { dpadSz.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.DPAD to newPos)))
                } {
                    // Render actual DPad (disabled for interaction, just visual)
                    DPad(when (layout.controlSize) { "SMALL" -> 40.dp; "LARGE" -> 58.dp; else -> 50.dp },
                        rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
                        onUpPress = {}, onDownPress = {}, onDownRelease = {},
                        onLeftPress = {}, onLeftRelease = {}, onRightPress = {}, onRightRelease = {}, onRotate = {})
                }
            }

            // ---- ROTATE BUTTON ----
            if (vis.getOrDefault(LayoutElements.ROTATE_BTN, true)) {
                val rp = pos[LayoutElements.ROTATE_BTN] ?: ElementPosition(0.82f, 0.82f)
                val rotSz = when (layout.controlSize) { "SMALL" -> 52f; "LARGE" -> 74f; else -> 66f }
                DraggableWrapper("Rotate", rp, maxWPx, maxHPx, with(density) { rotSz.dp.toPx() }, with(density) { rotSz.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.ROTATE_BTN to newPos)))
                } {
                    RotateButton({}, when (layout.controlSize) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 66.dp })
                }
            }

            // ---- HOLD BUTTON ----
            if (vis.getOrDefault(LayoutElements.HOLD_BTN, true)) {
                val hb = pos[LayoutElements.HOLD_BTN] ?: ElementPosition(0.5f, 0.78f)
                DraggableWrapper("Hold Btn", hb, maxWPx, maxHPx, with(density) { 82.dp.toPx() }, with(density) { 36.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.HOLD_BTN to newPos)))
                } {
                    ActionButton("HOLD", {}, width = 78.dp, height = 34.dp)
                }
            }

            // ---- PAUSE BUTTON ----
            if (vis.getOrDefault(LayoutElements.PAUSE_BTN, true)) {
                val pb = pos[LayoutElements.PAUSE_BTN] ?: ElementPosition(0.5f, 0.86f)
                DraggableWrapper("Pause Btn", pb, maxWPx, maxHPx, with(density) { 82.dp.toPx() }, with(density) { 36.dp.toPx() }) { newPos ->
                    onUpdateLayout(layout.copy(positions = pos + (LayoutElements.PAUSE_BTN to newPos)))
                } {
                    ActionButton("PAUSE", {}, width = 78.dp, height = 34.dp)
                }
            }

            // ---- MENU ≡ (always visible, always draggable) ----
            val mp = pos[LayoutElements.MENU_BTN] ?: ElementPosition(0.5f, 0.94f)
            DraggableWrapper("Menu ≡", mp, maxWPx, maxHPx, with(density) { 50.dp.toPx() }, with(density) { 28.dp.toPx() }) { newPos ->
                onUpdateLayout(layout.copy(positions = pos + (LayoutElements.MENU_BTN to newPos)))
            } {
                ActionButton("≡", {}, width = 46.dp, height = 24.dp)
            }
        }

        // ===== BOTTOM PANEL: visibility + settings =====
        AnimatedVisibility(showPanel, enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxWidth().background(PANEL_BG).padding(10.dp)) {
                Text("Show / Hide Elements", color = ACC, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(LayoutElements.hideable.size) { i ->
                        val elem = LayoutElements.hideable[i]
                        val visible = layout.visibility.getOrDefault(elem, true)
                        val label = elem.replace("_PREVIEW", "").replace("_BTN", "").lowercase().replaceFirstChar { it.uppercase() }
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (visible) ACC.copy(0.15f) else Color(0xFF252525)).border(1.dp, if (visible) ACC else DIM.copy(0.15f), RoundedCornerShape(6.dp)).clickable {
                            onUpdateLayout(layout.copy(visibility = layout.visibility + (elem to !visible)))
                        }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(label, color = if (visible) ACC else DIM, fontSize = 10.sp, fontWeight = if (visible) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Control Size", color = TX, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("SMALL", "MEDIUM", "LARGE").forEach { s ->
                            val sel = layout.controlSize == s
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (sel) ACC.copy(0.2f) else Color(0xFF252525)).border(1.dp, if (sel) ACC else DIM.copy(0.15f), RoundedCornerShape(6.dp)).clickable { onUpdateLayout(layout.copy(controlSize = s)) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(s, color = if (sel) ACC else DIM, fontSize = 9.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Next Queue", color = TX, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..3).forEach { n ->
                            val sel = layout.nextQueueSize == n
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (sel) ACC.copy(0.2f) else Color(0xFF252525)).border(1.dp, if (sel) ACC else DIM.copy(0.15f), RoundedCornerShape(6.dp)).clickable { onUpdateLayout(layout.copy(nextQueueSize = n)) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("$n", color = if (sel) ACC else DIM, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== DRAGGABLE WRAPPER — wraps any Composable content =====
@Composable
private fun BoxWithConstraintsScope.DraggableWrapper(
    label: String,
    position: ElementPosition,
    maxWPx: Float, maxHPx: Float,
    elementWPx: Float, elementHPx: Float,
    onDragEnd: (ElementPosition) -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember(position) { mutableStateOf(position.x * maxWPx - elementWPx / 2) }
    var offsetY by remember(position) { mutableStateOf(position.y * maxHPx - elementHPx / 2) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .zIndex(if (isDragging) 100f else 1f)
            .then(if (isDragging) Modifier.shadow(12.dp, RoundedCornerShape(8.dp)) else Modifier)
            .border(if (isDragging) 2.dp else 0.dp, if (isDragging) EDIT_BORDER else Color.Transparent, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        val cx = (offsetX + elementWPx / 2) / maxWPx
                        val cy = (offsetY + elementHPx / 2) / maxHPx
                        onDragEnd(ElementPosition(cx.coerceIn(0.02f, 0.98f), cy.coerceIn(0.02f, 0.98f)))
                    },
                    onDrag = { change, amt ->
                        change.consume()
                        offsetX = (offsetX + amt.x).coerceIn(0f, maxWPx - elementWPx)
                        offsetY = (offsetY + amt.y).coerceIn(0f, maxHPx - elementHPx)
                    }
                )
            }
    ) {
        content()
        // Label while dragging
        if (isDragging) {
            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-14).dp)
                    .background(EDIT_BORDER.copy(0.9f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
        }
    }
}

// Modifier extension for center alignment text
private fun Modifier.wrapContentAlignment(alignment: Alignment) = this

// ======================================
// THEME EDITOR — Tap elements on preview
// ======================================
private data class ThemeColorTarget(val label: String, val getColor: (GameTheme) -> Color, val setColor: (GameTheme, Color) -> GameTheme)
private val themeTargets = listOf(
    ThemeColorTarget("Background", { it.backgroundColor }, { t, c -> t.copy(backgroundColor = c) }),
    ThemeColorTarget("Device Frame", { it.deviceColor }, { t, c -> t.copy(deviceColor = c) }),
    ThemeColorTarget("Screen / LCD", { it.screenBackground }, { t, c -> t.copy(screenBackground = c) }),
    ThemeColorTarget("Blocks (ON)", { it.pixelOn }, { t, c -> t.copy(pixelOn = c) }),
    ThemeColorTarget("Empty Cells", { it.pixelOff }, { t, c -> t.copy(pixelOff = c) }),
    ThemeColorTarget("Text Primary", { it.textPrimary }, { t, c -> t.copy(textPrimary = c) }),
    ThemeColorTarget("Text Secondary", { it.textSecondary }, { t, c -> t.copy(textSecondary = c) }),
    ThemeColorTarget("Button Color", { it.buttonPrimary }, { t, c -> t.copy(buttonPrimary = c, buttonPrimaryPressed = c.copy(alpha = 0.7f)) }),
    ThemeColorTarget("Button Text", { it.buttonSecondary }, { t, c -> t.copy(buttonSecondary = c, buttonSecondaryPressed = c.copy(alpha = 0.7f)) }),
    ThemeColorTarget("Accent / Score", { it.accentColor }, { t, c -> t.copy(accentColor = c) })
)

@Composable
fun ThemeEditorScreen(theme: GameTheme, onUpdateTheme: (GameTheme) -> Unit, onSave: () -> Unit, onBack: () -> Unit) {
    var selectedTarget by remember { mutableStateOf<ThemeColorTarget?>(null) }
    var themeName by remember(theme.id) { mutableStateOf(theme.name) }

    Column(Modifier.fillMaxSize().background(PANEL_BG).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = ACC, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            BasicTextField(themeName, { themeName = it; onUpdateTheme(theme.copy(name = it)) },
                textStyle = TextStyle(color = TX, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Text("SAVE", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        // Live preview
        Box(Modifier.fillMaxWidth().weight(0.45f).padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp)).background(theme.backgroundColor).border(2.dp, if (selectedTarget?.label == "Background") EDIT_BORDER else Color.Transparent, RoundedCornerShape(12.dp)).clickable { selectedTarget = themeTargets[0] }) {
            Box(Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(8.dp)).background(theme.deviceColor).border(2.dp, if (selectedTarget?.label == "Device Frame") EDIT_BORDER else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedTarget = themeTargets[1] }) {
                Row(Modifier.fillMaxSize().padding(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).border(2.dp, if (selectedTarget?.label == "Screen / LCD") EDIT_BORDER else Color.Transparent, RoundedCornerShape(6.dp)).clickable { selectedTarget = themeTargets[2] }) {
                        Column(Modifier.fillMaxSize().padding(6.dp), Arrangement.SpaceEvenly) {
                            repeat(4) { row -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(6) { col ->
                                val isOn = (row + col) % 3 == 0
                                Box(Modifier.size(16.dp).clip(RoundedCornerShape(2.dp)).background(if (isOn) theme.pixelOn else theme.pixelOff).border(1.dp, if (selectedTarget?.label == (if (isOn) "Blocks (ON)" else "Empty Cells")) EDIT_BORDER else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = if (isOn) themeTargets[3] else themeTargets[4] })
                            } } }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.width(50.dp).fillMaxHeight(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                        Text("00123", color = theme.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Accent / Score") EDIT_BORDER else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[9] }.padding(2.dp))
                        Text("LV 3", color = theme.textPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Text Primary") EDIT_BORDER else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[5] }.padding(2.dp))
                        Text("Info", color = theme.textSecondary, fontSize = 9.sp, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Text Secondary") EDIT_BORDER else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[6] }.padding(2.dp))
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(theme.buttonPrimary).border(2.dp, if (selectedTarget?.label == "Button Color") EDIT_BORDER else Color.Transparent, CircleShape).clickable { selectedTarget = themeTargets[7] })
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(theme.buttonSecondary).border(2.dp, if (selectedTarget?.label == "Button Text") EDIT_BORDER else Color.Transparent, RoundedCornerShape(12.dp)).clickable { selectedTarget = themeTargets[8] })
            }
        }

        Text("Tap element above or select:", color = DIM, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(themeTargets.size) { i ->
                val t = themeTargets[i]; val sel = selectedTarget == t
                Row(Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) ACC.copy(0.2f) else CARD_BG).border(1.dp, if (sel) ACC else DIM.copy(0.2f), RoundedCornerShape(8.dp)).clickable { selectedTarget = t }.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(t.getColor(theme)).border(1.dp, DIM.copy(0.3f), CircleShape))
                    Text(t.label, color = if (sel) ACC else TX, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        if (selectedTarget != null) {
            ColorPickerPanel(selectedTarget!!.getColor(theme)) { newColor -> onUpdateTheme(selectedTarget!!.setColor(theme, newColor)) }
        } else {
            Box(Modifier.fillMaxWidth().weight(0.35f), Alignment.Center) { Text("Tap an element to edit its color", color = DIM, fontSize = 13.sp) }
        }
    }
}

@Composable private fun ColumnScope.ColorPickerPanel(color: Color, onChange: (Color) -> Unit) {
    var hexText by remember(color) { mutableStateOf(colorToHex(color)) }
    Column(Modifier.fillMaxWidth().weight(0.35f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(color).border(2.dp, DIM.copy(0.4f), CircleShape))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#", color = DIM, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                BasicTextField(hexText, { v -> val c = v.filter { it.isLetterOrDigit() }.take(6); hexText = c; hexFromString(c)?.let { onChange(it) } },
                    textStyle = TextStyle(color = TX, fontSize = 14.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(ACC), singleLine = true,
                    modifier = Modifier.width(80.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF252525)).padding(8.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val presets = listOf(Color.Black, Color(0xFF1A1A1A), Color(0xFF2A2A2A), Color(0xFF4A4A4A), Color(0xFF6A6A6A), Color(0xFF8A8A8A), Color(0xFFAAAAAA), Color(0xFFCCCCCC), Color.White,
                Color(0xFFFF0000), Color(0xFFFF4444), Color(0xFFFF8800), Color(0xFFFFAA00), Color(0xFFF4D03F), Color(0xFFFFFF00), Color(0xFF88FF00), Color(0xFF00FF00), Color(0xFF00FF88), Color(0xFF00FFFF),
                Color(0xFF0088FF), Color(0xFF0000FF), Color(0xFF8800FF), Color(0xFFFF00FF), Color(0xFF8AB4F8), Color(0xFF9B59B6), Color(0xFFE07030), Color(0xFF2ECC71),
                Color(0xFF3D4A32), Color(0xFFB8C4A8), Color(0xFFD4C896), Color(0xFF201800), Color(0xFF080810), Color(0xFFFAF6F0))
            items(presets.size) { i -> val p = presets[i]
                Box(Modifier.size(26.dp).clip(CircleShape).background(p).border(if (colorsClose(p, color)) 2.dp else 1.dp, if (colorsClose(p, color)) ACC else DIM.copy(0.2f), CircleShape).clickable { onChange(p); hexText = colorToHex(p) })
            }
        }
        Spacer(Modifier.height(6.dp))
        val r = (color.red * 255).toInt(); val g = (color.green * 255).toInt(); val b = (color.blue * 255).toInt()
        RGBSlider("R", r, Color.Red) { onChange(Color(it / 255f, color.green, color.blue)); hexText = colorToHex(Color(it / 255f, color.green, color.blue)) }
        RGBSlider("G", g, Color.Green) { onChange(Color(color.red, it / 255f, color.blue)); hexText = colorToHex(Color(color.red, it / 255f, color.blue)) }
        RGBSlider("B", b, Color(0xFF4488FF)) { onChange(Color(color.red, color.green, it / 255f)); hexText = colorToHex(Color(color.red, color.green, it / 255f)) }
    }
}

@Composable private fun RGBSlider(label: String, value: Int, trackColor: Color, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(16.dp))
        Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = 0f..255f, modifier = Modifier.weight(1f).height(24.dp),
            colors = SliderDefaults.colors(thumbColor = trackColor, activeTrackColor = trackColor.copy(0.7f), inactiveTrackColor = trackColor.copy(0.12f)))
        Text(value.toString().padStart(3), color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp))
    }
}

private fun colorToHex(c: Color) = "%02X%02X%02X".format((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
private fun hexFromString(hex: String): Color? = try { if (hex.length == 6) Color(("FF$hex").toLong(16)) else null } catch (_: Exception) { null }
private fun colorsClose(a: Color, b: Color): Boolean { val d = (a.red-b.red)*(a.red-b.red)+(a.green-b.green)*(a.green-b.green)+(a.blue-b.blue)*(a.blue-b.blue); return d < 0.002f }
