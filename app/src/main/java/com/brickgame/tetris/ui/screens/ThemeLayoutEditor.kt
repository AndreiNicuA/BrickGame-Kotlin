package com.brickgame.tetris.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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

private val PBG = Color(0xFF0D0D0D)
private val CBG = Color(0xFF1A1A1A)
private val ACC = Color(0xFFF4D03F)
private val TX = Color(0xFFE8E8E8)
private val DIM = Color(0xFF888888)
private val DRG = Color(0xFF22C55E)

// Element shapes
object ElementShapes { const val ROUND = "ROUND"; const val SQUARE = "SQUARE"; const val PILL = "PILL"; const val DIAMOND = "DIAMOND" }

// ======================================================================
// LAYOUT EDITOR
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
    var selectedElement by remember { mutableStateOf<String?>(null) }
    var showSideMenu by remember { mutableStateOf(false) }
    var gridSnap by remember { mutableStateOf(false) }
    val gridStepX = 0.05f; val gridStepY = 0.04f

    fun snapPos(pos: ElementPosition) = if (gridSnap)
        ElementPosition((pos.x / gridStepX).roundToInt() * gridStepX, (pos.y / gridStepY).roundToInt() * gridStepY)
    else pos

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(Modifier.fillMaxWidth().background(PBG).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = ACC, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(6.dp))
                BasicTextField(layoutName, { layoutName = it; onUpdateLayout(layout.copy(name = it)) },
                    textStyle = TextStyle(color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                // Hamburger menu button
                Text("☰", color = TX, fontSize = 20.sp, modifier = Modifier.clickable { showSideMenu = !showSideMenu }.padding(6.dp))
                Text("SAVE", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 14.dp, vertical = 6.dp))
            }

            // DRAG CANVAS — tap outside to dismiss popup
            BoxWithConstraints(Modifier.fillMaxSize().pointerInput(selectedElement) {
                detectTapGestures { selectedElement = null }
            }) {
                val density = LocalDensity.current
                val maxWPx = with(density) { maxWidth.toPx() }
                val maxHPx = with(density) { maxHeight.toPx() }

                // Grid overlay
                if (gridSnap) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cols = (1f / gridStepX).toInt(); val rows = (1f / gridStepY).toInt()
                        for (i in 1 until cols) { val x = size.width * i * gridStepX; drawLine(Color.White.copy(0.06f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f) }
                        for (i in 1 until rows) { val y = size.height * i * gridStepY; drawLine(Color.White.copy(0.06f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f) }
                    }
                }

                // All draggable elements
                val pos = layout.positions; val vis = layout.visibility

                if (vis.getOrDefault(LayoutElements.BOARD, true))
                    DraggableItem(LayoutElements.BOARD, "Board", pos[LayoutElements.BOARD] ?: ElementPosition(0.5f, 0.38f),
                        140.dp, 200.dp, maxWPx, maxHPx, selectedElement == LayoutElements.BOARD, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Column(Modifier.size(140.dp, 200.dp).clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).padding(3.dp), Arrangement.SpaceEvenly) {
                            repeat(8) { r -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(5) { c ->
                                val on = (r == 2 && c in 1..3) || (r == 3 && c == 2)
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(1.dp)).background(if (on) theme.pixelOn else theme.pixelOff))
                            } } }
                        }
                    }

                if (vis.getOrDefault(LayoutElements.SCORE, true))
                    DraggableItem(LayoutElements.SCORE, "Score", pos[LayoutElements.SCORE] ?: ElementPosition(0.5f, 0.04f),
                        100.dp, 22.dp, maxWPx, maxHPx, selectedElement == LayoutElements.SCORE, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Box(Modifier.size(100.dp, 22.dp).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor), Alignment.Center) { Text("0001234", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    }

                if (vis.getOrDefault(LayoutElements.LEVEL, true))
                    DraggableItem(LayoutElements.LEVEL, "Level", pos[LayoutElements.LEVEL] ?: ElementPosition(0.15f, 0.04f),
                        40.dp, 18.dp, maxWPx, maxHPx, selectedElement == LayoutElements.LEVEL, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Box(Modifier.size(40.dp, 18.dp).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor), Alignment.Center) { Text("LV1", color = theme.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    }

                if (vis.getOrDefault(LayoutElements.LINES, true))
                    DraggableItem(LayoutElements.LINES, "Lines", pos[LayoutElements.LINES] ?: ElementPosition(0.85f, 0.04f),
                        36.dp, 18.dp, maxWPx, maxHPx, selectedElement == LayoutElements.LINES, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Box(Modifier.size(36.dp, 18.dp).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor), Alignment.Center) { Text("0L", color = theme.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    }

                if (vis.getOrDefault(LayoutElements.HOLD_PREVIEW, true))
                    DraggableItem(LayoutElements.HOLD_PREVIEW, "Hold", pos[LayoutElements.HOLD_PREVIEW] ?: ElementPosition(0.08f, 0.12f),
                        44.dp, 52.dp, maxWPx, maxHPx, selectedElement == LayoutElements.HOLD_PREVIEW, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Column(Modifier.size(44.dp, 52.dp).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HOLD", color = theme.textSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace); Box(Modifier.size(32.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground.copy(0.4f)))
                        }
                    }

                if (vis.getOrDefault(LayoutElements.NEXT_PREVIEW, true))
                    DraggableItem(LayoutElements.NEXT_PREVIEW, "Next", pos[LayoutElements.NEXT_PREVIEW] ?: ElementPosition(0.92f, 0.12f),
                        44.dp, 52.dp, maxWPx, maxHPx, selectedElement == LayoutElements.NEXT_PREVIEW, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Column(Modifier.size(44.dp, 52.dp).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NEXT", color = theme.textSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace); Box(Modifier.size(32.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground.copy(0.4f)))
                        }
                    }

                if (vis.getOrDefault(LayoutElements.DPAD, true)) {
                    val dSz = when (layout.controlSize) { "SMALL" -> 100.dp; "LARGE" -> 140.dp; else -> 120.dp }; val bSz = when (layout.controlSize) { "SMALL" -> 30.dp; "LARGE" -> 42.dp; else -> 36.dp }
                    DraggableItem(LayoutElements.DPAD, "D-Pad", pos[LayoutElements.DPAD] ?: ElementPosition(0.22f, 0.82f),
                        dSz, dSz, maxWPx, maxHPx, selectedElement == LayoutElements.DPAD, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Box(Modifier.size(dSz), Alignment.Center) {
                            Box(Modifier.size(bSz).align(Alignment.TopCenter).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("▲", color = theme.buttonSecondary, fontSize = 12.sp) }
                            Box(Modifier.size(bSz).align(Alignment.BottomCenter).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("▼", color = theme.buttonSecondary, fontSize = 12.sp) }
                            Box(Modifier.size(bSz).align(Alignment.CenterStart).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("◄", color = theme.buttonSecondary, fontSize = 12.sp) }
                            Box(Modifier.size(bSz).align(Alignment.CenterEnd).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("►", color = theme.buttonSecondary, fontSize = 12.sp) }
                            Box(Modifier.size(bSz * 0.6f).clip(CircleShape).background(theme.buttonPrimaryPressed))
                        }
                    }
                }

                if (vis.getOrDefault(LayoutElements.ROTATE_BTN, true)) {
                    val rSz = when (layout.controlSize) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 64.dp }
                    DraggableItem(LayoutElements.ROTATE_BTN, "Rotate", pos[LayoutElements.ROTATE_BTN] ?: ElementPosition(0.82f, 0.82f),
                        rSz, rSz, maxWPx, maxHPx, selectedElement == LayoutElements.ROTATE_BTN, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Box(Modifier.size(rSz).shadow(6.dp, CircleShape).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("↻", color = theme.buttonSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                if (vis.getOrDefault(LayoutElements.HOLD_BTN, true))
                    DraggableItem(LayoutElements.HOLD_BTN, "Hold Btn", pos[LayoutElements.HOLD_BTN] ?: ElementPosition(0.5f, 0.76f),
                        78.dp, 32.dp, maxWPx, maxHPx, selectedElement == LayoutElements.HOLD_BTN, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Box(Modifier.size(78.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(theme.accentColor.copy(0.8f)), Alignment.Center) { Text("HOLD", color = theme.backgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }

                if (vis.getOrDefault(LayoutElements.PAUSE_BTN, true))
                    DraggableItem(LayoutElements.PAUSE_BTN, "Pause", pos[LayoutElements.PAUSE_BTN] ?: ElementPosition(0.5f, 0.84f),
                        78.dp, 32.dp, maxWPx, maxHPx, selectedElement == LayoutElements.PAUSE_BTN, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                        Box(Modifier.size(78.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(theme.accentColor.copy(0.8f)), Alignment.Center) { Text("PAUSE", color = theme.backgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }

                // Menu ≡ always visible
                DraggableItem(LayoutElements.MENU_BTN, "Menu", pos[LayoutElements.MENU_BTN] ?: ElementPosition(0.5f, 0.92f),
                    44.dp, 24.dp, maxWPx, maxHPx, selectedElement == LayoutElements.MENU_BTN, { selectedElement = it }, { onUpdateLayout(layout.copy(positions = pos + (it.first to snapPos(it.second)))) }) {
                    Box(Modifier.size(44.dp, 24.dp).clip(RoundedCornerShape(12.dp)).background(theme.accentColor), Alignment.Center) { Text("≡", color = theme.backgroundColor, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }

                // === ELEMENT POPUP (when tapped, not dragged) ===
                if (selectedElement != null) {
                    val elem = selectedElement!!
                    val ePos = pos[elem] ?: ElementPosition(0.5f, 0.5f)
                    val popX = with(density) { (ePos.x * maxWidth.toPx()).toDp() }
                    val popY = with(density) { (ePos.y * maxHeight.toPx()).toDp() }
                    // Position popup near element, but keep on screen
                    val adjX = (popX - 70.dp).coerceIn(8.dp, maxWidth - 150.dp)
                    val adjY = (popY + 30.dp).coerceIn(8.dp, maxHeight - 120.dp)

                    Column(Modifier.offset(x = adjX, y = adjY).width(140.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xEE1A1A1A)).border(1.dp, ACC.copy(0.3f), RoundedCornerShape(10.dp)).padding(8.dp)) {
                        Text(elem.replace("_", " "), color = ACC, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        // Hide toggle (not for MENU)
                        if (elem != LayoutElements.MENU_BTN) {
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2A)).clickable {
                                onUpdateLayout(layout.copy(visibility = vis + (elem to false))); selectedElement = null
                            }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("👁 Hide", color = Color(0xFFFF6B6B), fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        // Size (for controls only)
                        if (elem in listOf(LayoutElements.DPAD, LayoutElements.ROTATE_BTN, LayoutElements.HOLD_BTN, LayoutElements.PAUSE_BTN)) {
                            Text("Size", color = DIM, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                listOf("SMALL", "MEDIUM", "LARGE").forEach { s ->
                                    val sel = layout.controlSize == s
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if (sel) ACC.copy(0.2f) else Color(0xFF333333)).clickable { onUpdateLayout(layout.copy(controlSize = s)) }.padding(horizontal = 6.dp, vertical = 3.dp)) { Text(s.take(1), color = if (sel) ACC else DIM, fontSize = 9.sp) }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        // Next queue size
                        if (elem == LayoutElements.NEXT_PREVIEW) {
                            Text("Queue", color = DIM, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                (1..3).forEach { n ->
                                    val sel = layout.nextQueueSize == n
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if (sel) ACC.copy(0.2f) else Color(0xFF333333)).clickable { onUpdateLayout(layout.copy(nextQueueSize = n)) }.padding(horizontal = 6.dp, vertical = 3.dp)) { Text("$n", color = if (sel) ACC else DIM, fontSize = 9.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // === SIDE MENU (transparent overlay) ===
        AnimatedVisibility(showSideMenu, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)) {
            Column(Modifier.width(180.dp).fillMaxHeight().background(Color(0xDD111111)).padding(16.dp), Arrangement.spacedBy(8.dp)) {
                Text("Layout Tools", color = ACC, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                // Grid snap toggle
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (gridSnap) ACC.copy(0.15f) else CBG).clickable { gridSnap = !gridSnap }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (gridSnap) "⊞" else "⊟", fontSize = 16.sp, color = if (gridSnap) ACC else DIM)
                    Spacer(Modifier.width(8.dp))
                    Text("Grid Snap", color = if (gridSnap) ACC else TX, fontSize = 12.sp)
                }
                // Show hidden elements
                val hiddenElems = LayoutElements.hideable.filter { !(layout.visibility.getOrDefault(it, true)) }
                if (hiddenElems.isNotEmpty()) {
                    Text("Hidden Elements", color = DIM, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    hiddenElems.forEach { elem ->
                        val label = elem.replace("_PREVIEW", "").replace("_BTN", "").lowercase().replaceFirstChar { it.uppercase() }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CBG).clickable {
                            onUpdateLayout(layout.copy(visibility = layout.visibility + (elem to true)))
                        }.padding(8.dp)) {
                            Text("👁 Show $label", color = DRG, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // Reset all
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF4A1A1A)).clickable {
                    onUpdateLayout(layout.copy(positions = CustomLayoutData.defaultPositions(), visibility = LayoutElements.allElements.associateWith { true }, controlSize = "MEDIUM", nextQueueSize = 1))
                    showSideMenu = false
                }.padding(10.dp)) {
                    Text("↺ Reset All", color = Color(0xFFFF6B6B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                // Close
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CBG).clickable { showSideMenu = false }.padding(10.dp)) {
                    Text("✕ Close", color = DIM, fontSize = 12.sp)
                }
            }
        }
    }
}

// ======================================================================
// DRAGGABLE ITEM — tap = select for popup, drag = move only this item
// ======================================================================
@Composable
private fun BoxWithConstraintsScope.DraggableItem(
    elementId: String, label: String, position: ElementPosition,
    itemWidth: Dp, itemHeight: Dp, maxWPx: Float, maxHPx: Float,
    isSelected: Boolean,
    onSelect: (String?) -> Unit,
    onPositionChanged: (Pair<String, ElementPosition>) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val itemWPx = with(density) { itemWidth.toPx() }
    val itemHPx = with(density) { itemHeight.toPx() }

    var offsetX by remember { mutableStateOf(position.x * maxWPx - itemWPx / 2) }
    var offsetY by remember { mutableStateOf(position.y * maxHPx - itemHPx / 2) }
    var isDragging by remember { mutableStateOf(false) }
    var wasDragged by remember { mutableStateOf(false) }

    LaunchedEffect(position) {
        offsetX = position.x * maxWPx - itemWPx / 2
        offsetY = position.y * maxHPx - itemHPx / 2
    }

    Box(
        Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true; wasDragged = false },
                    onDragEnd = {
                        isDragging = false
                        if (wasDragged) {
                            val cx = (offsetX + itemWPx / 2) / maxWPx
                            val cy = (offsetY + itemHPx / 2) / maxHPx
                            onPositionChanged(elementId to ElementPosition(cx.coerceIn(0.02f, 0.98f), cy.coerceIn(0.02f, 0.98f)))
                        }
                    },
                    onDrag = { change, amt ->
                        change.consume()
                        wasDragged = true
                        offsetX = (offsetX + amt.x).coerceIn(0f, maxWPx - itemWPx)
                        offsetY = (offsetY + amt.y).coerceIn(0f, maxHPx - itemHPx)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    // Tap = toggle selection for popup settings
                    onSelect(if (isSelected) null else elementId)
                }
            }
    ) {
        Box(Modifier.then(if (isDragging || isSelected) Modifier.border(2.dp, if (isDragging) DRG else ACC, RoundedCornerShape(6.dp)) else Modifier)
            .shadow(if (isDragging) 12.dp else if (isSelected) 8.dp else 3.dp, RoundedCornerShape(6.dp))) {
            content()
        }
        // Label
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 14.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
    }
}

// ======================================================================
// THEME EDITOR (unchanged from v3)
// ======================================================================
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

@Composable fun ThemeEditorScreen(theme: GameTheme, onUpdateTheme: (GameTheme) -> Unit, onSave: () -> Unit, onBack: () -> Unit) {
    var selectedTarget by remember { mutableStateOf<ThemeColorTarget?>(null) }
    var themeName by remember(theme.id) { mutableStateOf(theme.name) }
    Column(Modifier.fillMaxSize().background(PBG).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = ACC, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            BasicTextField(themeName, { themeName = it; onUpdateTheme(theme.copy(name = it)) }, textStyle = TextStyle(color = TX, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Text("SAVE", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        Box(Modifier.fillMaxWidth().weight(0.45f).padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp)).background(theme.backgroundColor).border(2.dp, if (selectedTarget?.label == "Background") DRG else Color.Transparent, RoundedCornerShape(12.dp)).clickable { selectedTarget = themeTargets[0] }) {
            Box(Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(8.dp)).background(theme.deviceColor).border(2.dp, if (selectedTarget?.label == "Device Frame") DRG else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedTarget = themeTargets[1] }) {
                Row(Modifier.fillMaxSize().padding(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).border(2.dp, if (selectedTarget?.label == "Screen / LCD") DRG else Color.Transparent, RoundedCornerShape(6.dp)).clickable { selectedTarget = themeTargets[2] }) {
                        Column(Modifier.fillMaxSize().padding(6.dp), Arrangement.SpaceEvenly) { repeat(4) { r -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(6) { c -> val isOn = (r + c) % 3 == 0; Box(Modifier.size(16.dp).clip(RoundedCornerShape(2.dp)).background(if (isOn) theme.pixelOn else theme.pixelOff).border(1.dp, if (selectedTarget?.label == (if (isOn) "Blocks (ON)" else "Empty Cells")) DRG else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = if (isOn) themeTargets[3] else themeTargets[4] }) } } } }
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.width(50.dp).fillMaxHeight(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                        Text("00123", color = theme.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Accent / Score") DRG else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[9] }.padding(2.dp))
                        Text("LV 3", color = theme.textPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Text Primary") DRG else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[5] }.padding(2.dp))
                        Text("Info", color = theme.textSecondary, fontSize = 9.sp, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Text Secondary") DRG else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[6] }.padding(2.dp))
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(theme.buttonPrimary).border(2.dp, if (selectedTarget?.label == "Button Color") DRG else Color.Transparent, CircleShape).clickable { selectedTarget = themeTargets[7] })
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(theme.buttonSecondary).border(2.dp, if (selectedTarget?.label == "Button Text") DRG else Color.Transparent, RoundedCornerShape(12.dp)).clickable { selectedTarget = themeTargets[8] })
            }
        }
        Text("Tap element above or select:", color = DIM, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(themeTargets.size) { i -> val t = themeTargets[i]; val sel = selectedTarget == t; Row(Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) ACC.copy(0.2f) else CBG).border(1.dp, if (sel) ACC else DIM.copy(0.2f), RoundedCornerShape(8.dp)).clickable { selectedTarget = t }.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Box(Modifier.size(16.dp).clip(CircleShape).background(t.getColor(theme)).border(1.dp, DIM.copy(0.3f), CircleShape)); Text(t.label, color = if (sel) ACC else TX, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) } } }
        if (selectedTarget != null) { ColorPickerPanel(selectedTarget!!.getColor(theme)) { onUpdateTheme(selectedTarget!!.setColor(theme, it)) } }
        else { Box(Modifier.fillMaxWidth().weight(0.35f), Alignment.Center) { Text("Tap an element to edit its color", color = DIM, fontSize = 13.sp) } }
    }
}

@Composable private fun ColumnScope.ColorPickerPanel(color: Color, onChange: (Color) -> Unit) {
    var hexText by remember(color) { mutableStateOf(colorToHex(color)) }
    Column(Modifier.fillMaxWidth().weight(0.35f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(color).border(2.dp, DIM.copy(0.4f), CircleShape))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("#", color = DIM, fontSize = 14.sp, fontFamily = FontFamily.Monospace); BasicTextField(hexText, { v -> val c = v.filter { it.isLetterOrDigit() }.take(6); hexText = c; hexFromString(c)?.let { onChange(it) } }, textStyle = TextStyle(color = TX, fontSize = 14.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.width(80.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF252525)).padding(8.dp)) }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { val presets = listOf(Color.Black, Color(0xFF1A1A1A), Color(0xFF2A2A2A), Color(0xFF4A4A4A), Color(0xFF6A6A6A), Color(0xFF8A8A8A), Color(0xFFAAAAAA), Color(0xFFCCCCCC), Color.White, Color(0xFFFF0000), Color(0xFFFF4444), Color(0xFFFF8800), Color(0xFFFFAA00), Color(0xFFF4D03F), Color(0xFFFFFF00), Color(0xFF88FF00), Color(0xFF00FF00), Color(0xFF00FF88), Color(0xFF00FFFF), Color(0xFF0088FF), Color(0xFF0000FF), Color(0xFF8800FF), Color(0xFFFF00FF), Color(0xFF8AB4F8), Color(0xFF9B59B6), Color(0xFFE07030), Color(0xFF2ECC71), Color(0xFF3D4A32), Color(0xFFB8C4A8), Color(0xFFD4C896), Color(0xFF201800), Color(0xFF080810), Color(0xFFFAF6F0)); items(presets.size) { i -> val p = presets[i]; Box(Modifier.size(26.dp).clip(CircleShape).background(p).border(if (colorsClose(p, color)) 2.dp else 1.dp, if (colorsClose(p, color)) ACC else DIM.copy(0.2f), CircleShape).clickable { onChange(p); hexText = colorToHex(p) }) } }
        Spacer(Modifier.height(6.dp))
        val r = (color.red * 255).toInt(); val g = (color.green * 255).toInt(); val b = (color.blue * 255).toInt()
        RGBSlider("R", r, Color.Red) { onChange(Color(it / 255f, color.green, color.blue)); hexText = colorToHex(Color(it / 255f, color.green, color.blue)) }
        RGBSlider("G", g, Color.Green) { onChange(Color(color.red, it / 255f, color.blue)); hexText = colorToHex(Color(color.red, it / 255f, color.blue)) }
        RGBSlider("B", b, Color(0xFF4488FF)) { onChange(Color(color.red, color.green, it / 255f)); hexText = colorToHex(Color(color.red, color.green, it / 255f)) }
    }
}

@Composable private fun RGBSlider(label: String, value: Int, trackColor: Color, onChange: (Int) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(16.dp)); Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = 0f..255f, modifier = Modifier.weight(1f).height(24.dp), colors = SliderDefaults.colors(thumbColor = trackColor, activeTrackColor = trackColor.copy(0.7f), inactiveTrackColor = trackColor.copy(0.12f))); Text(value.toString().padStart(3), color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp)) } }
private fun colorToHex(c: Color) = "%02X%02X%02X".format((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
private fun hexFromString(hex: String): Color? = try { if (hex.length == 6) Color(("FF$hex").toLong(16)) else null } catch (_: Exception) { null }
private fun colorsClose(a: Color, b: Color): Boolean { val d = (a.red-b.red)*(a.red-b.red)+(a.green-b.green)*(a.green-b.green)+(a.blue-b.blue)*(a.blue-b.blue); return d < 0.002f }
