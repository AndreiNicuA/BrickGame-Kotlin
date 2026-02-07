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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.CustomLayoutData
import com.brickgame.tetris.data.ElementPosition
import com.brickgame.tetris.data.LayoutElements
import com.brickgame.tetris.ui.theme.GameTheme
import kotlin.math.roundToInt

private val PANEL_BG = Color(0xFF0D0D0D)
private val CARD_BG = Color(0xFF1A1A1A)
private val ACC = Color(0xFFF4D03F)
private val TX = Color(0xFFE8E8E8)
private val DIM = Color(0xFF888888)
private val DRAG_BORDER = Color(0xFF22C55E)

// ======================================================================
// LAYOUT EDITOR — Exact RoadTrip DraggableButton pattern per element
// ======================================================================
@Composable
fun LayoutEditorScreen(
    layout: CustomLayoutData,
    theme: GameTheme,
    basePreset: com.brickgame.tetris.ui.layout.LayoutPreset,
    dpadStyle: com.brickgame.tetris.ui.layout.DPadStyle,
    onUpdateLayout: (CustomLayoutData) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var layoutName by remember(layout.id) { mutableStateOf(layout.name) }
    var showPanel by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        // Header
        Row(Modifier.fillMaxWidth().background(PANEL_BG).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = ACC, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }.padding(6.dp))
            BasicTextField(layoutName, { layoutName = it; onUpdateLayout(layout.copy(name = it)) },
                textStyle = TextStyle(color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(ACC), singleLine = true,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Text(if (showPanel) "▼" else "▲", color = DIM, fontSize = 16.sp,
                modifier = Modifier.clickable { showPanel = !showPanel }.padding(6.dp))
            Text("SAVE", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ACC)
                    .clickable { onSave() }.padding(horizontal = 14.dp, vertical = 6.dp))
        }

        // ===== DRAG CANVAS =====
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val density = LocalDensity.current
            val maxWPx = with(density) { maxWidth.toPx() }
            val maxHPx = with(density) { maxHeight.toPx() }

            // Instruction
            Text("Drag each element to reposition it",
                color = Color.White.copy(0.3f), fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 2.dp))

            // --- BOARD: compact preview, NOT fullscreen ---
            if (layout.visibility.getOrDefault(LayoutElements.BOARD, true)) {
                DraggableItem(
                    label = "Board",
                    position = layout.positions[LayoutElements.BOARD] ?: ElementPosition(0.5f, 0.38f),
                    itemWidth = 140.dp, itemHeight = 200.dp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.BOARD to pos)))
                    }
                ) {
                    // Mini board preview
                    Column(Modifier.size(140.dp, 200.dp).clip(RoundedCornerShape(6.dp))
                        .background(theme.screenBackground).padding(3.dp),
                        Arrangement.SpaceEvenly) {
                        repeat(8) { r ->
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                                repeat(5) { c ->
                                    val on = (r == 2 && c in 1..3) || (r == 3 && c == 2)
                                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(1.dp))
                                        .background(if (on) theme.pixelOn else theme.pixelOff))
                                }
                            }
                        }
                    }
                }
            }

            // --- SCORE ---
            if (layout.visibility.getOrDefault(LayoutElements.SCORE, true)) {
                DraggableItem(
                    label = "Score",
                    position = layout.positions[LayoutElements.SCORE] ?: ElementPosition(0.5f, 0.04f),
                    itemWidth = 100.dp, itemHeight = 22.dp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.SCORE to pos)))
                    }
                ) {
                    Box(Modifier.size(100.dp, 22.dp).clip(RoundedCornerShape(4.dp))
                        .background(theme.deviceColor), Alignment.Center) {
                        Text("0001234", color = theme.accentColor, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // --- LEVEL ---
            if (layout.visibility.getOrDefault(LayoutElements.LEVEL, true)) {
                DraggableItem(
                    label = "Level",
                    position = layout.positions[LayoutElements.LEVEL] ?: ElementPosition(0.15f, 0.04f),
                    itemWidth = 40.dp, itemHeight = 18.dp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.LEVEL to pos)))
                    }
                ) {
                    Box(Modifier.size(40.dp, 18.dp).clip(RoundedCornerShape(4.dp))
                        .background(theme.deviceColor), Alignment.Center) {
                        Text("LV1", color = theme.textSecondary, fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // --- LINES ---
            if (layout.visibility.getOrDefault(LayoutElements.LINES, true)) {
                DraggableItem(
                    label = "Lines",
                    position = layout.positions[LayoutElements.LINES] ?: ElementPosition(0.85f, 0.04f),
                    itemWidth = 36.dp, itemHeight = 18.dp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.LINES to pos)))
                    }
                ) {
                    Box(Modifier.size(36.dp, 18.dp).clip(RoundedCornerShape(4.dp))
                        .background(theme.deviceColor), Alignment.Center) {
                        Text("0L", color = theme.textSecondary, fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // --- HOLD PREVIEW ---
            if (layout.visibility.getOrDefault(LayoutElements.HOLD_PREVIEW, true)) {
                DraggableItem(
                    label = "Hold",
                    position = layout.positions[LayoutElements.HOLD_PREVIEW] ?: ElementPosition(0.08f, 0.12f),
                    itemWidth = 44.dp, itemHeight = 52.dp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.HOLD_PREVIEW to pos)))
                    }
                ) {
                    Column(Modifier.size(44.dp, 52.dp).clip(RoundedCornerShape(4.dp))
                        .background(theme.deviceColor).padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HOLD", color = theme.textSecondary, fontSize = 7.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(3.dp))
                            .background(theme.screenBackground.copy(0.4f)))
                    }
                }
            }

            // --- NEXT PREVIEW ---
            if (layout.visibility.getOrDefault(LayoutElements.NEXT_PREVIEW, true)) {
                DraggableItem(
                    label = "Next",
                    position = layout.positions[LayoutElements.NEXT_PREVIEW] ?: ElementPosition(0.92f, 0.12f),
                    itemWidth = 44.dp, itemHeight = 52.dp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.NEXT_PREVIEW to pos)))
                    }
                ) {
                    Column(Modifier.size(44.dp, 52.dp).clip(RoundedCornerShape(4.dp))
                        .background(theme.deviceColor).padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEXT", color = theme.textSecondary, fontSize = 7.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(3.dp))
                            .background(theme.screenBackground.copy(0.4f)))
                    }
                }
            }

            // --- D-PAD (4 arrows + center) ---
            if (layout.visibility.getOrDefault(LayoutElements.DPAD, true)) {
                val dpadDp = when (layout.controlSize) { "SMALL" -> 100.dp; "LARGE" -> 140.dp; else -> 120.dp }
                DraggableItem(
                    label = "D-Pad",
                    position = layout.positions[LayoutElements.DPAD] ?: ElementPosition(0.22f, 0.82f),
                    itemWidth = dpadDp, itemHeight = dpadDp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.DPAD to pos)))
                    }
                ) {
                    // Draw a simple cross shape to represent D-Pad
                    val btnSz = when (layout.controlSize) { "SMALL" -> 30.dp; "LARGE" -> 42.dp; else -> 36.dp }
                    Box(Modifier.size(dpadDp), Alignment.Center) {
                        // Up
                        Box(Modifier.size(btnSz).align(Alignment.TopCenter).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) {
                            Text("▲", color = theme.buttonSecondary, fontSize = 12.sp)
                        }
                        // Down
                        Box(Modifier.size(btnSz).align(Alignment.BottomCenter).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) {
                            Text("▼", color = theme.buttonSecondary, fontSize = 12.sp)
                        }
                        // Left
                        Box(Modifier.size(btnSz).align(Alignment.CenterStart).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) {
                            Text("◄", color = theme.buttonSecondary, fontSize = 12.sp)
                        }
                        // Right
                        Box(Modifier.size(btnSz).align(Alignment.CenterEnd).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) {
                            Text("►", color = theme.buttonSecondary, fontSize = 12.sp)
                        }
                        // Center dot
                        Box(Modifier.size(btnSz * 0.6f).clip(CircleShape).background(theme.buttonPrimaryPressed))
                    }
                }
            }

            // --- ROTATE BUTTON ---
            if (layout.visibility.getOrDefault(LayoutElements.ROTATE_BTN, true)) {
                val rotDp = when (layout.controlSize) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 64.dp }
                DraggableItem(
                    label = "Rotate",
                    position = layout.positions[LayoutElements.ROTATE_BTN] ?: ElementPosition(0.82f, 0.82f),
                    itemWidth = rotDp, itemHeight = rotDp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.ROTATE_BTN to pos)))
                    }
                ) {
                    Box(Modifier.size(rotDp).shadow(6.dp, CircleShape).clip(CircleShape)
                        .background(theme.buttonPrimary), Alignment.Center) {
                        Text("↻", color = theme.buttonSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- HOLD BUTTON ---
            if (layout.visibility.getOrDefault(LayoutElements.HOLD_BTN, true)) {
                DraggableItem(
                    label = "Hold Btn",
                    position = layout.positions[LayoutElements.HOLD_BTN] ?: ElementPosition(0.5f, 0.76f),
                    itemWidth = 78.dp, itemHeight = 32.dp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.HOLD_BTN to pos)))
                    }
                ) {
                    Box(Modifier.size(78.dp, 32.dp).clip(RoundedCornerShape(16.dp))
                        .background(theme.accentColor.copy(0.8f)), Alignment.Center) {
                        Text("HOLD", color = theme.backgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- PAUSE BUTTON ---
            if (layout.visibility.getOrDefault(LayoutElements.PAUSE_BTN, true)) {
                DraggableItem(
                    label = "Pause",
                    position = layout.positions[LayoutElements.PAUSE_BTN] ?: ElementPosition(0.5f, 0.84f),
                    itemWidth = 78.dp, itemHeight = 32.dp,
                    maxWPx = maxWPx, maxHPx = maxHPx,
                    onPositionChanged = { pos ->
                        onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.PAUSE_BTN to pos)))
                    }
                ) {
                    Box(Modifier.size(78.dp, 32.dp).clip(RoundedCornerShape(16.dp))
                        .background(theme.accentColor.copy(0.8f)), Alignment.Center) {
                        Text("PAUSE", color = theme.backgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- MENU ≡ (always visible, cannot hide) ---
            DraggableItem(
                label = "Menu",
                position = layout.positions[LayoutElements.MENU_BTN] ?: ElementPosition(0.5f, 0.92f),
                itemWidth = 44.dp, itemHeight = 24.dp,
                maxWPx = maxWPx, maxHPx = maxHPx,
                onPositionChanged = { pos ->
                    onUpdateLayout(layout.copy(positions = layout.positions + (LayoutElements.MENU_BTN to pos)))
                }
            ) {
                Box(Modifier.size(44.dp, 24.dp).clip(RoundedCornerShape(12.dp))
                    .background(theme.accentColor), Alignment.Center) {
                    Text("≡", color = theme.backgroundColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ===== BOTTOM PANEL =====
        AnimatedVisibility(showPanel, enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxWidth().background(PANEL_BG).padding(10.dp)) {
                Text("Show / Hide Elements", color = ACC, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(LayoutElements.hideable.size) { i ->
                        val elem = LayoutElements.hideable[i]
                        val visible = layout.visibility.getOrDefault(elem, true)
                        val label = elem.replace("_PREVIEW", "").replace("_BTN", "")
                            .lowercase().replaceFirstChar { it.uppercase() }
                        Box(Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (visible) ACC.copy(0.15f) else Color(0xFF252525))
                            .border(1.dp, if (visible) ACC else DIM.copy(0.15f), RoundedCornerShape(6.dp))
                            .clickable {
                                onUpdateLayout(layout.copy(visibility = layout.visibility + (elem to !visible)))
                            }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(label, color = if (visible) ACC else DIM, fontSize = 10.sp,
                                fontWeight = if (visible) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Control Size", color = TX, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("SMALL", "MEDIUM", "LARGE").forEach { s ->
                            val sel = layout.controlSize == s
                            Box(Modifier.clip(RoundedCornerShape(6.dp))
                                .background(if (sel) ACC.copy(0.2f) else Color(0xFF252525))
                                .border(1.dp, if (sel) ACC else DIM.copy(0.15f), RoundedCornerShape(6.dp))
                                .clickable { onUpdateLayout(layout.copy(controlSize = s)) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)) {
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
                            Box(Modifier.clip(RoundedCornerShape(6.dp))
                                .background(if (sel) ACC.copy(0.2f) else Color(0xFF252525))
                                .border(1.dp, if (sel) ACC else DIM.copy(0.15f), RoundedCornerShape(6.dp))
                                .clickable { onUpdateLayout(layout.copy(nextQueueSize = n)) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("$n", color = if (sel) ACC else DIM, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================================================================
// DRAGGABLE ITEM — Exact RoadTrip DraggableButton pattern
// Each item has a FIXED size, positioned at normalized coords.
// Only this exact item is draggable — no expanding, no fill.
// ======================================================================
@Composable
private fun BoxWithConstraintsScope.DraggableItem(
    label: String,
    position: ElementPosition,
    itemWidth: Dp,
    itemHeight: Dp,
    maxWPx: Float,
    maxHPx: Float,
    onPositionChanged: (ElementPosition) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val itemWPx = with(density) { itemWidth.toPx() }
    val itemHPx = with(density) { itemHeight.toPx() }

    // RoadTrip pattern: remember without key, update via LaunchedEffect
    var offsetX by remember { mutableStateOf(position.x * maxWPx - itemWPx / 2) }
    var offsetY by remember { mutableStateOf(position.y * maxHPx - itemHPx / 2) }
    var isDragging by remember { mutableStateOf(false) }

    // Update when position changes externally
    LaunchedEffect(position) {
        offsetX = position.x * maxWPx - itemWPx / 2
        offsetY = position.y * maxHPx - itemHPx / 2
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        // Convert back to normalized (0-1)
                        val centerX = (offsetX + itemWPx / 2) / maxWPx
                        val centerY = (offsetY + itemHPx / 2) / maxHPx
                        onPositionChanged(ElementPosition(
                            x = centerX.coerceIn(0.02f, 0.98f),
                            y = centerY.coerceIn(0.02f, 0.98f)
                        ))
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWPx - itemWPx)
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxHPx - itemHPx)
                    }
                )
            }
    ) {
        // The actual element — rendered at exact fixed size
        Box(
            modifier = Modifier
                .then(
                    if (isDragging)
                        Modifier.border(2.dp, DRAG_BORDER, RoundedCornerShape(6.dp))
                    else Modifier
                )
                .shadow(if (isDragging) 12.dp else 4.dp, RoundedCornerShape(6.dp))
        ) {
            content()
        }

        // Floating label below element
        Text(
            text = label,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 14.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

// ======================================================================
// THEME EDITOR — Tap elements on preview to pick colors
// ======================================================================
private data class ThemeColorTarget(
    val label: String,
    val getColor: (GameTheme) -> Color,
    val setColor: (GameTheme, Color) -> GameTheme
)

private val themeTargets = listOf(
    ThemeColorTarget("Background", { it.backgroundColor }, { t, c -> t.copy(backgroundColor = c) }),
    ThemeColorTarget("Device Frame", { it.deviceColor }, { t, c -> t.copy(deviceColor = c) }),
    ThemeColorTarget("Screen / LCD", { it.screenBackground }, { t, c -> t.copy(screenBackground = c) }),
    ThemeColorTarget("Blocks (ON)", { it.pixelOn }, { t, c -> t.copy(pixelOn = c) }),
    ThemeColorTarget("Empty Cells", { it.pixelOff }, { t, c -> t.copy(pixelOff = c) }),
    ThemeColorTarget("Text Primary", { it.textPrimary }, { t, c -> t.copy(textPrimary = c) }),
    ThemeColorTarget("Text Secondary", { it.textSecondary }, { t, c -> t.copy(textSecondary = c) }),
    ThemeColorTarget("Button Color", { it.buttonPrimary },
        { t, c -> t.copy(buttonPrimary = c, buttonPrimaryPressed = c.copy(alpha = 0.7f)) }),
    ThemeColorTarget("Button Text", { it.buttonSecondary },
        { t, c -> t.copy(buttonSecondary = c, buttonSecondaryPressed = c.copy(alpha = 0.7f)) }),
    ThemeColorTarget("Accent / Score", { it.accentColor }, { t, c -> t.copy(accentColor = c) })
)

@Composable
fun ThemeEditorScreen(
    theme: GameTheme,
    onUpdateTheme: (GameTheme) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTarget by remember { mutableStateOf<ThemeColorTarget?>(null) }
    var themeName by remember(theme.id) { mutableStateOf(theme.name) }

    Column(Modifier.fillMaxSize().background(PANEL_BG).systemBarsPadding()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = ACC, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }.padding(8.dp))
            BasicTextField(themeName, { themeName = it; onUpdateTheme(theme.copy(name = it)) },
                textStyle = TextStyle(color = TX, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(ACC), singleLine = true,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Text("SAVE", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ACC)
                    .clickable { onSave() }.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        // Live preview
        Box(Modifier.fillMaxWidth().weight(0.45f).padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp)).background(theme.backgroundColor)
            .border(2.dp, if (selectedTarget?.label == "Background") DRAG_BORDER else Color.Transparent,
                RoundedCornerShape(12.dp))
            .clickable { selectedTarget = themeTargets[0] }) {
            Box(Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(8.dp))
                .background(theme.deviceColor)
                .border(2.dp, if (selectedTarget?.label == "Device Frame") DRAG_BORDER else Color.Transparent,
                    RoundedCornerShape(8.dp))
                .clickable { selectedTarget = themeTargets[1] }) {
                Row(Modifier.fillMaxSize().padding(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                        .background(theme.screenBackground)
                        .border(2.dp, if (selectedTarget?.label == "Screen / LCD") DRAG_BORDER else Color.Transparent,
                            RoundedCornerShape(6.dp))
                        .clickable { selectedTarget = themeTargets[2] }) {
                        Column(Modifier.fillMaxSize().padding(6.dp), Arrangement.SpaceEvenly) {
                            repeat(4) { row ->
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                                    repeat(6) { col ->
                                        val isOn = (row + col) % 3 == 0
                                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(2.dp))
                                            .background(if (isOn) theme.pixelOn else theme.pixelOff)
                                            .border(1.dp,
                                                if (selectedTarget?.label == (if (isOn) "Blocks (ON)" else "Empty Cells"))
                                                    DRAG_BORDER else Color.Transparent,
                                                RoundedCornerShape(2.dp))
                                            .clickable { selectedTarget = if (isOn) themeTargets[3] else themeTargets[4] })
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.width(50.dp).fillMaxHeight(), Arrangement.SpaceEvenly,
                        Alignment.CenterHorizontally) {
                        Text("00123", color = theme.accentColor, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .border(1.dp, if (selectedTarget?.label == "Accent / Score") DRAG_BORDER else Color.Transparent,
                                    RoundedCornerShape(2.dp))
                                .clickable { selectedTarget = themeTargets[9] }.padding(2.dp))
                        Text("LV 3", color = theme.textPrimary, fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .border(1.dp, if (selectedTarget?.label == "Text Primary") DRAG_BORDER else Color.Transparent,
                                    RoundedCornerShape(2.dp))
                                .clickable { selectedTarget = themeTargets[5] }.padding(2.dp))
                        Text("Info", color = theme.textSecondary, fontSize = 9.sp,
                            modifier = Modifier
                                .border(1.dp, if (selectedTarget?.label == "Text Secondary") DRAG_BORDER else Color.Transparent,
                                    RoundedCornerShape(2.dp))
                                .clickable { selectedTarget = themeTargets[6] }.padding(2.dp))
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(theme.buttonPrimary)
                    .border(2.dp, if (selectedTarget?.label == "Button Color") DRAG_BORDER else Color.Transparent,
                        CircleShape)
                    .clickable { selectedTarget = themeTargets[7] })
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(theme.buttonSecondary)
                    .border(2.dp, if (selectedTarget?.label == "Button Text") DRAG_BORDER else Color.Transparent,
                        RoundedCornerShape(12.dp))
                    .clickable { selectedTarget = themeTargets[8] })
            }
        }

        // Target chips
        Text("Tap element above or select:", color = DIM, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(themeTargets.size) { i ->
                val t = themeTargets[i]
                val sel = selectedTarget == t
                Row(Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (sel) ACC.copy(0.2f) else CARD_BG)
                    .border(1.dp, if (sel) ACC else DIM.copy(0.2f), RoundedCornerShape(8.dp))
                    .clickable { selectedTarget = t }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(t.getColor(theme))
                        .border(1.dp, DIM.copy(0.3f), CircleShape))
                    Text(t.label, color = if (sel) ACC else TX, fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // Color picker
        if (selectedTarget != null) {
            ColorPickerPanel(selectedTarget!!.getColor(theme)) { newColor ->
                onUpdateTheme(selectedTarget!!.setColor(theme, newColor))
            }
        } else {
            Box(Modifier.fillMaxWidth().weight(0.35f), Alignment.Center) {
                Text("Tap an element to edit its color", color = DIM, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ColumnScope.ColorPickerPanel(color: Color, onChange: (Color) -> Unit) {
    var hexText by remember(color) { mutableStateOf(colorToHex(color)) }
    Column(Modifier.fillMaxWidth().weight(0.35f).verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(color)
                .border(2.dp, DIM.copy(0.4f), CircleShape))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#", color = DIM, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                BasicTextField(hexText,
                    { v ->
                        val c = v.filter { it.isLetterOrDigit() }.take(6)
                        hexText = c
                        hexFromString(c)?.let { onChange(it) }
                    },
                    textStyle = TextStyle(color = TX, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(ACC), singleLine = true,
                    modifier = Modifier.width(80.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF252525)).padding(8.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val presets = listOf(
                Color.Black, Color(0xFF1A1A1A), Color(0xFF2A2A2A), Color(0xFF4A4A4A),
                Color(0xFF6A6A6A), Color(0xFF8A8A8A), Color(0xFFAAAAAA), Color(0xFFCCCCCC), Color.White,
                Color(0xFFFF0000), Color(0xFFFF4444), Color(0xFFFF8800), Color(0xFFFFAA00),
                Color(0xFFF4D03F), Color(0xFFFFFF00), Color(0xFF88FF00), Color(0xFF00FF00),
                Color(0xFF00FF88), Color(0xFF00FFFF), Color(0xFF0088FF), Color(0xFF0000FF),
                Color(0xFF8800FF), Color(0xFFFF00FF), Color(0xFF8AB4F8), Color(0xFF9B59B6),
                Color(0xFFE07030), Color(0xFF2ECC71), Color(0xFF3D4A32), Color(0xFFB8C4A8),
                Color(0xFFD4C896), Color(0xFF201800), Color(0xFF080810), Color(0xFFFAF6F0)
            )
            items(presets.size) { i ->
                val p = presets[i]
                Box(Modifier.size(26.dp).clip(CircleShape).background(p)
                    .border(
                        if (colorsClose(p, color)) 2.dp else 1.dp,
                        if (colorsClose(p, color)) ACC else DIM.copy(0.2f), CircleShape)
                    .clickable { onChange(p); hexText = colorToHex(p) })
            }
        }
        Spacer(Modifier.height(6.dp))
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        RGBSlider("R", r, Color.Red) {
            onChange(Color(it / 255f, color.green, color.blue))
            hexText = colorToHex(Color(it / 255f, color.green, color.blue))
        }
        RGBSlider("G", g, Color.Green) {
            onChange(Color(color.red, it / 255f, color.blue))
            hexText = colorToHex(Color(color.red, it / 255f, color.blue))
        }
        RGBSlider("B", b, Color(0xFF4488FF)) {
            onChange(Color(color.red, color.green, it / 255f))
            hexText = colorToHex(Color(color.red, color.green, it / 255f))
        }
    }
}

@Composable
private fun RGBSlider(label: String, value: Int, trackColor: Color, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(16.dp))
        Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = 0f..255f,
            modifier = Modifier.weight(1f).height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor.copy(0.7f),
                inactiveTrackColor = trackColor.copy(0.12f)))
        Text(value.toString().padStart(3), color = DIM, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp))
    }
}

private fun colorToHex(c: Color) =
    "%02X%02X%02X".format((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())

private fun hexFromString(hex: String): Color? = try {
    if (hex.length == 6) Color(("FF$hex").toLong(16)) else null
} catch (_: Exception) { null }

private fun colorsClose(a: Color, b: Color): Boolean {
    val d = (a.red - b.red) * (a.red - b.red) + (a.green - b.green) * (a.green - b.green) +
            (a.blue - b.blue) * (a.blue - b.blue)
    return d < 0.002f
}
