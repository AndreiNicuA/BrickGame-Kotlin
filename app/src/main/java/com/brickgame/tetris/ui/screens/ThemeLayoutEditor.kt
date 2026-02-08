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
import androidx.compose.ui.zIndex
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

// ======================================================================
// LAYOUT EDITOR v6
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
    val undoStack = remember { mutableStateListOf<CustomLayoutData>() }
    val redoStack = remember { mutableStateListOf<CustomLayoutData>() }

    fun withUndo(newLayout: CustomLayoutData) { undoStack.add(layout); redoStack.clear(); onUpdateLayout(newLayout) }
    fun undo() { if (undoStack.isNotEmpty()) { redoStack.add(layout); onUpdateLayout(undoStack.removeLast()) } }
    fun redo() { if (redoStack.isNotEmpty()) { undoStack.add(layout); onUpdateLayout(redoStack.removeLast()) } }
    fun snap(p: ElementPosition) = if (gridSnap) ElementPosition((p.x / gridStepX).roundToInt() * gridStepX, (p.y / gridStepY).roundToInt() * gridStepY) else p

    // Element sizes map (compact)
    data class ESz(val w: Dp, val h: Dp)
    fun ctrlSz(base: Dp): Dp = when (layout.controlSize) { "SMALL" -> base * 0.75f; "LARGE" -> base * 1.2f; else -> base }

    val sizes = mapOf(
        LayoutElements.BOARD to ESz(140.dp, 200.dp),
        LayoutElements.SCORE to ESz(100.dp, 22.dp),
        LayoutElements.LEVEL to ESz(40.dp, 18.dp),
        LayoutElements.LINES to ESz(36.dp, 18.dp),
        LayoutElements.HOLD_PREVIEW to ESz(44.dp, 52.dp),
        LayoutElements.NEXT_PREVIEW to ESz(44.dp, 52.dp),
        LayoutElements.DPAD to ESz(ctrlSz(120.dp), ctrlSz(120.dp)),
        LayoutElements.ROTATE_BTN to ESz(ctrlSz(64.dp), ctrlSz(64.dp)),
        LayoutElements.HOLD_BTN to ESz(78.dp, 32.dp),
        LayoutElements.PAUSE_BTN to ESz(78.dp, 32.dp),
        LayoutElements.MENU_BTN to ESz(44.dp, 24.dp)
    )

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // HEADER
            Row(Modifier.fillMaxWidth().background(PBG).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = ACC, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(6.dp))
                BasicTextField(layoutName, { layoutName = it; onUpdateLayout(layout.copy(name = it)) },
                    textStyle = TextStyle(color = TX, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(ACC), singleLine = true,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF252525)).padding(horizontal = 10.dp, vertical = 8.dp),
                    decorationBox = { inner -> if (layoutName.isEmpty()) Text("Layout name...", color = DIM, fontSize = 15.sp, fontFamily = FontFamily.Monospace); inner() })
                Spacer(Modifier.width(8.dp))
                Text("SAVE", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 14.dp, vertical = 8.dp))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(if (showSideMenu) ACC.copy(0.2f) else Color(0xFF333333)).clickable { showSideMenu = !showSideMenu; selectedElement = null }, Alignment.Center) { Text("☰", color = TX, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }

            // DRAG CANVAS
            BoxWithConstraints(Modifier.fillMaxSize().pointerInput(selectedElement) { detectTapGestures { selectedElement = null; showSideMenu = false } }) {
                val density = LocalDensity.current
                val maxWPx = with(density) { maxWidth.toPx() }
                val maxHPx = with(density) { maxHeight.toPx() }

                // Grid
                if (gridSnap) Canvas(Modifier.fillMaxSize()) {
                    for (i in 1 until (1f / gridStepX).toInt()) { val x = size.width * i * gridStepX; drawLine(Color.White.copy(0.06f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f) }
                    for (i in 1 until (1f / gridStepY).toInt()) { val y = size.height * i * gridStepY; drawLine(Color.White.copy(0.06f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f) }
                }

                // Render each element
                LayoutElements.allElements.forEach { elemId ->
                    if (elemId != LayoutElements.MENU_BTN && !(layout.visibility.getOrDefault(elemId, true))) return@forEach
                    val pos = layout.positions[elemId] ?: CustomLayoutData.defaultPositions()[elemId] ?: return@forEach
                    val sz = sizes[elemId] ?: return@forEach

                    key(elemId) {
                        DraggableItem(
                            elementId = elemId,
                            position = pos,
                            itemWidth = sz.w, itemHeight = sz.h,
                            maxWPx = maxWPx, maxHPx = maxHPx,
                            isSelected = selectedElement == elemId,
                            onTap = { selectedElement = if (selectedElement == it) null else it },
                            onDragEnd = { id, ep -> withUndo(layout.copy(positions = layout.positions + (id to snap(ep)))) }
                        ) {
                            ElementContent(elemId, sz.w, sz.h, theme, layout)
                        }
                    }
                }

                // FLOATING SETTINGS CARD — appears near selected element, compact
                if (selectedElement != null) {
                    val elem = selectedElement!!
                    val ePos = layout.positions[elem] ?: CustomLayoutData.defaultPositions()[elem] ?: ElementPosition(0.5f, 0.5f)
                    val eSz = sizes[elem] ?: ESz(60.dp, 40.dp)
                    // Position card: prefer below element, or above if too low
                    val cardBelow = ePos.y < 0.65f
                    val cardX = with(density) { (ePos.x * maxWidth.toPx()).toDp() - 80.dp }.coerceIn(4.dp, maxWidth - 164.dp)
                    val cardY = if (cardBelow) with(density) { (ePos.y * maxHeight.toPx()).toDp() + eSz.h / 2 + 8.dp }
                              else with(density) { (ePos.y * maxHeight.toPx()).toDp() - eSz.h / 2 - 120.dp }
                    val adjY = cardY.coerceIn(4.dp, maxHeight - 140.dp)

                    Column(Modifier.offset(x = cardX, y = adjY).width(160.dp).zIndex(300f)
                        .clip(RoundedCornerShape(12.dp)).shadow(16.dp, RoundedCornerShape(12.dp))
                        .background(Color(0xF0181818)).border(1.dp, ACC.copy(0.4f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) { detectTapGestures { /* consume */ } }
                        .padding(10.dp)) {

                        Text(elem.replace("_", " "), color = ACC, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))

                        // Hide (not for menu)
                        if (elem != LayoutElements.MENU_BTN) {
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A1515))
                                .clickable { withUndo(layout.copy(visibility = layout.visibility + (elem to false))); selectedElement = null }
                                .padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text("👁 Hide", color = Color(0xFFFF6B6B), fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Size (for all control elements)
                        if (elem in listOf(LayoutElements.DPAD, LayoutElements.ROTATE_BTN, LayoutElements.HOLD_BTN, LayoutElements.PAUSE_BTN, LayoutElements.BOARD, LayoutElements.SCORE, LayoutElements.LEVEL, LayoutElements.LINES, LayoutElements.HOLD_PREVIEW, LayoutElements.NEXT_PREVIEW)) {
                            Text("Size", color = DIM, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                listOf("SMALL" to "S", "MEDIUM" to "M", "LARGE" to "L").forEach { (k, v) ->
                                    val sel = layout.controlSize == k
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if (sel) ACC.copy(0.25f) else Color(0xFF333333))
                                        .clickable { withUndo(layout.copy(controlSize = k)) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(v, color = if (sel) ACC else DIM, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
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
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if (sel) ACC.copy(0.25f) else Color(0xFF333333))
                                        .clickable { withUndo(layout.copy(nextQueueSize = n)) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("$n", color = if (sel) ACC else DIM, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SIDE MENU
        AnimatedVisibility(showSideMenu, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)) {
            Column(Modifier.width(190.dp).fillMaxHeight().background(Color(0xEE111111))
                .pointerInput(Unit) { detectTapGestures { /* consume */ } }.padding(16.dp), Arrangement.spacedBy(6.dp)) {
                Text("Layout Tools", color = ACC, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (undoStack.isNotEmpty()) Color(0xFF2A2A3A) else CBG)
                        .clickable(enabled = undoStack.isNotEmpty()) { undo() }.padding(10.dp), Alignment.Center) { Text("↶ Undo", color = if (undoStack.isNotEmpty()) Color(0xFF8AB4F8) else DIM.copy(0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (redoStack.isNotEmpty()) Color(0xFF2A2A3A) else CBG)
                        .clickable(enabled = redoStack.isNotEmpty()) { redo() }.padding(10.dp), Alignment.Center) { Text("↷ Redo", color = if (redoStack.isNotEmpty()) Color(0xFF8AB4F8) else DIM.copy(0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (gridSnap) ACC.copy(0.15f) else CBG).clickable { gridSnap = !gridSnap }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (gridSnap) "⊞" else "⊟", fontSize = 16.sp, color = if (gridSnap) ACC else DIM); Spacer(Modifier.width(8.dp)); Text("Grid Snap", color = if (gridSnap) ACC else TX, fontSize = 12.sp) }
                val hidden = LayoutElements.hideable.filter { !(layout.visibility.getOrDefault(it, true)) }
                if (hidden.isNotEmpty()) { Text("Hidden", color = DIM, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    hidden.forEach { elem -> val lbl = elem.replace("_PREVIEW", "").replace("_BTN", "").lowercase().replaceFirstChar { it.uppercase() }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CBG).clickable { withUndo(layout.copy(visibility = layout.visibility + (elem to true))) }.padding(8.dp)) { Text("👁 Show $lbl", color = DRG, fontSize = 11.sp) } } }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF4A1A1A)).clickable { withUndo(layout.copy(positions = CustomLayoutData.defaultPositions(), visibility = LayoutElements.allElements.associateWith { true }, controlSize = "MEDIUM", nextQueueSize = 1)); showSideMenu = false }.padding(10.dp)) { Text("↺ Reset All", color = Color(0xFFFF6B6B), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ======================================================================
// ELEMENT CONTENT — renders the visual for each element type
// ======================================================================
@Composable
private fun ElementContent(elemId: String, w: Dp, h: Dp, theme: GameTheme, layout: CustomLayoutData) {
    when (elemId) {
        LayoutElements.BOARD -> Column(Modifier.size(w, h).clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).padding(3.dp), Arrangement.SpaceEvenly) {
            repeat(8) { r -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(5) { c -> val on = (r == 2 && c in 1..3) || (r == 3 && c == 2); Box(Modifier.size(10.dp).clip(RoundedCornerShape(1.dp)).background(if (on) theme.pixelOn else theme.pixelOff)) } } } }
        LayoutElements.SCORE -> Box(Modifier.size(w, h).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor), Alignment.Center) { Text("0001234", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
        LayoutElements.LEVEL -> Box(Modifier.size(w, h).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor), Alignment.Center) { Text("LV1", color = theme.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
        LayoutElements.LINES -> Box(Modifier.size(w, h).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor), Alignment.Center) { Text("0L", color = theme.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
        LayoutElements.HOLD_PREVIEW -> Column(Modifier.size(w, h).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("HOLD", color = theme.textSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace); Box(Modifier.size(32.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground.copy(0.4f))) }
        LayoutElements.NEXT_PREVIEW -> Column(Modifier.size(w, h).clip(RoundedCornerShape(4.dp)).background(theme.deviceColor).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("NEXT", color = theme.textSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace); Box(Modifier.size(32.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground.copy(0.4f))) }
        LayoutElements.DPAD -> { val bSz = when (layout.controlSize) { "SMALL" -> 26.dp; "LARGE" -> 38.dp; else -> 32.dp }
            Box(Modifier.size(w, h), Alignment.Center) {
                Box(Modifier.size(bSz).align(Alignment.TopCenter).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("▲", color = theme.buttonSecondary, fontSize = 11.sp) }
                Box(Modifier.size(bSz).align(Alignment.BottomCenter).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("▼", color = theme.buttonSecondary, fontSize = 11.sp) }
                Box(Modifier.size(bSz).align(Alignment.CenterStart).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("◄", color = theme.buttonSecondary, fontSize = 11.sp) }
                Box(Modifier.size(bSz).align(Alignment.CenterEnd).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("►", color = theme.buttonSecondary, fontSize = 11.sp) }
                Box(Modifier.size(bSz * 0.6f).clip(CircleShape).background(theme.buttonPrimaryPressed)) } }
        LayoutElements.ROTATE_BTN -> Box(Modifier.size(w, h).shadow(6.dp, CircleShape).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("↻", color = theme.buttonSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        LayoutElements.HOLD_BTN -> Box(Modifier.size(w, h).clip(RoundedCornerShape(16.dp)).background(theme.accentColor.copy(0.8f)), Alignment.Center) { Text("HOLD", color = theme.backgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        LayoutElements.PAUSE_BTN -> Box(Modifier.size(w, h).clip(RoundedCornerShape(16.dp)).background(theme.accentColor.copy(0.8f)), Alignment.Center) { Text("PAUSE", color = theme.backgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        LayoutElements.MENU_BTN -> Box(Modifier.size(w, h).clip(RoundedCornerShape(12.dp)).background(theme.accentColor), Alignment.Center) { Text("≡", color = theme.backgroundColor, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}

// ======================================================================
// DRAGGABLE ITEM v6 — BULLETPROOF position tracking
// Uses a version counter to force state reset when position changes externally.
// During drag: local pixel offsets only.
// On drag end: saves normalized position, version bumps, state resets.
// ======================================================================
@Composable
private fun BoxWithConstraintsScope.DraggableItem(
    elementId: String, position: ElementPosition,
    itemWidth: Dp, itemHeight: Dp, maxWPx: Float, maxHPx: Float,
    isSelected: Boolean,
    onTap: (String) -> Unit,
    onDragEnd: (String, ElementPosition) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val wPx = with(density) { itemWidth.toPx() }
    val hPx = with(density) { itemHeight.toPx() }

    // CORE FIX: derive baseline from position, track drag delta separately
    val baseX = position.x * maxWPx - wPx / 2
    val baseY = position.y * maxHPx - hPx / 2
    var dragDeltaX by remember { mutableStateOf(0f) }
    var dragDeltaY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var wasDragged by remember { mutableStateOf(false) }

    // Reset delta when position changes (element was saved or undo'd)
    LaunchedEffect(position.x, position.y) {
        if (!isDragging) { dragDeltaX = 0f; dragDeltaY = 0f }
    }

    val finalX = baseX + dragDeltaX
    val finalY = baseY + dragDeltaY

    Box(
        Modifier
            .offset { IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
            .zIndex(if (isDragging) 100f else if (isSelected) 50f else 1f)
            .pointerInput(elementId) {
                detectDragGestures(
                    onDragStart = { isDragging = true; wasDragged = false; dragDeltaX = 0f; dragDeltaY = 0f },
                    onDragEnd = {
                        isDragging = false
                        if (wasDragged) {
                            val cx = (finalX + wPx / 2) / maxWPx
                            val cy = (finalY + hPx / 2) / maxHPx
                            dragDeltaX = 0f; dragDeltaY = 0f
                            onDragEnd(elementId, ElementPosition(cx.coerceIn(0.02f, 0.98f), cy.coerceIn(0.02f, 0.98f)))
                        } else { dragDeltaX = 0f; dragDeltaY = 0f }
                    },
                    onDragCancel = { isDragging = false; dragDeltaX = 0f; dragDeltaY = 0f },
                    onDrag = { change, amt ->
                        change.consume()
                        wasDragged = true
                        val newX = (baseX + dragDeltaX + amt.x).coerceIn(0f, maxWPx - wPx)
                        val newY = (baseY + dragDeltaY + amt.y).coerceIn(0f, maxHPx - hPx)
                        dragDeltaX = newX - baseX
                        dragDeltaY = newY - baseY
                    }
                )
            }
            .pointerInput(elementId) { detectTapGestures { onTap(elementId) } }
    ) {
        Box(Modifier.then(if (isDragging || isSelected) Modifier.border(2.dp, if (isDragging) DRG else ACC, RoundedCornerShape(6.dp)) else Modifier)
            .shadow(if (isDragging) 12.dp else if (isSelected) 8.dp else 3.dp, RoundedCornerShape(6.dp))) { content() }
        Text(elementId.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 13.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
    }
}

// ======================================================================
// THEME EDITOR
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
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Box(Modifier.size(36.dp).clip(CircleShape).background(color).border(2.dp, DIM.copy(0.4f), CircleShape)); Row(verticalAlignment = Alignment.CenterVertically) { Text("#", color = DIM, fontSize = 14.sp, fontFamily = FontFamily.Monospace); BasicTextField(hexText, { v -> val c = v.filter { it.isLetterOrDigit() }.take(6); hexText = c; hexFromString(c)?.let { onChange(it) } }, textStyle = TextStyle(color = TX, fontSize = 14.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.width(80.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF252525)).padding(8.dp)) } }
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
