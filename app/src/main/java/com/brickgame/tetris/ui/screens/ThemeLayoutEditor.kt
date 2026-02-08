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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

// Colors
private val PBG = Color(0xFF0D0D0D)
private val CARD = Color(0xFF1A1A1A)
private val ZONE = Color(0xFF151515)
private val ACC = Color(0xFFF4D03F)
private val TX = Color(0xFFE8E8E8)
private val DIM = Color(0xFF888888)
private val GRN = Color(0xFF22C55E)
private val SEP = Color(0xFF333333)

// ======================================================================
// LAYOUT EDITOR v9 — 3-ZONE DRAG-AND-DROP
// TOP: draggable info elements | MIDDLE: LCD board | BOTTOM: draggable controls
// Side menu: per-element settings opened by tapping elements or ☰
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
    var showSideMenu by remember { mutableStateOf(false) }
    var selectedElement by remember { mutableStateOf<String?>(null) }
    val undoStack = remember { mutableStateListOf<CustomLayoutData>() }
    val redoStack = remember { mutableStateListOf<CustomLayoutData>() }

    fun withUndo(newLayout: CustomLayoutData) {
        undoStack.add(layout); redoStack.clear(); onUpdateLayout(newLayout)
    }
    fun undo() { if (undoStack.isNotEmpty()) { redoStack.add(layout); onUpdateLayout(undoStack.removeLast()) } }
    fun redo() { if (redoStack.isNotEmpty()) { undoStack.add(layout); onUpdateLayout(redoStack.removeLast()) } }

    fun selectAndOpenMenu(elem: String?) { selectedElement = elem; if (elem != null) showSideMenu = true }

    Box(Modifier.fillMaxSize().background(PBG).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // === HEADER ===
            Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = ACC, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }.padding(horizontal = 6.dp, vertical = 4.dp))
                BasicTextField(layoutName, { layoutName = it; onUpdateLayout(layout.copy(name = it)) },
                    textStyle = TextStyle(color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(ACC), singleLine = true,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF222222)).padding(horizontal = 10.dp, vertical = 7.dp),
                    decorationBox = { inner -> if (layoutName.isEmpty()) Text("Layout name...", color = DIM, fontSize = 14.sp, fontFamily = FontFamily.Monospace); inner() })
                Spacer(Modifier.width(6.dp))
                Text("SAVE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 12.dp, vertical = 7.dp))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (showSideMenu) ACC.copy(0.2f) else Color(0xFF333333))
                    .clickable { showSideMenu = !showSideMenu }, Alignment.Center) {
                    Text("☰", color = TX, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // === TOP ZONE — Info Bar ===
            ZoneLabel("TOP — Info Bar", if (layout.topBarVisible) null else "(hidden)")
            if (layout.topBarVisible) {
                TopBarZone(layout, theme, selectedElement, ::selectAndOpenMenu) { withUndo(it) }
            } else {
                Box(Modifier.fillMaxWidth().height(36.dp).background(ZONE.copy(0.5f))
                    .clickable { withUndo(layout.copy(topBarVisible = true)) }, Alignment.Center) {
                    Text("Tap to show info bar", color = DIM, fontSize = 11.sp)
                }
            }
            ZoneDivider()

            // === MIDDLE ZONE — LCD Board ===
            ZoneLabel("MIDDLE — LCD Screen")
            BoardZone(layout, theme, selectedElement, ::selectAndOpenMenu) { withUndo(it) }
            ZoneDivider()

            // === BOTTOM ZONE — Controls ===
            ZoneLabel("BOTTOM — Controls")
            ControlsZone(layout, theme, selectedElement, ::selectAndOpenMenu) { withUndo(it) }
        }

        // === SIDE MENU ===
        AnimatedVisibility(showSideMenu,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).zIndex(200f)
        ) {
            SideMenu(layout, selectedElement,
                onUpdate = { withUndo(it) },
                onSelectElement = { selectedElement = it },
                onUndo = ::undo, onRedo = ::redo,
                undoAvail = undoStack.isNotEmpty(), redoAvail = redoStack.isNotEmpty(),
                onClose = { showSideMenu = false }
            )
        }
    }
}

// ======================================================================
// ZONE LABELS & DIVIDERS
// ======================================================================
@Composable private fun ZoneLabel(label: String, extra: String? = null) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 12.dp, vertical = 3.dp)) {
        Text(label, color = ACC.copy(0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        if (extra != null) { Spacer(Modifier.width(6.dp)); Text(extra, color = DIM.copy(0.5f), fontSize = 9.sp) }
    }
}
@Composable private fun ZoneDivider() {
    Box(Modifier.fillMaxWidth().height(2.dp).background(ACC.copy(0.15f)))
}

// ======================================================================
// TOP BAR ZONE — Draggable element reorder + 3 visual styles
// ======================================================================
@Composable
private fun TopBarZone(
    layout: CustomLayoutData, theme: GameTheme,
    selectedElement: String?, onSelect: (String?) -> Unit,
    onUpdate: (CustomLayoutData) -> Unit
) {
    val order = layout.topBarElementOrder.filter { layout.isVisible(it) }
    val density = LocalDensity.current

    // The visual container changes based on topBarStyle
    val bgMod = when (layout.topBarStyle) {
        "DEVICE_FRAME" -> Modifier.fillMaxWidth().padding(horizontal = 4.dp).clip(RoundedCornerShape(10.dp)).background(theme.deviceColor).padding(6.dp)
        "MINIMAL" -> Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
        else -> Modifier.fillMaxWidth().padding(horizontal = 4.dp).clip(RoundedCornerShape(10.dp)).background(theme.deviceColor.copy(0.6f)).padding(horizontal = 10.dp, vertical = 6.dp)
    }

    Row(bgMod.height(if (layout.topBarStyle == "MINIMAL") 36.dp else 56.dp),
        Arrangement.SpaceEvenly, Alignment.CenterVertically) {
        order.forEachIndexed { idx, elem ->
            val isSelected = selectedElement == elem
            var dragDX by remember { mutableStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }

            Box(Modifier
                .offset { IntOffset(if (isDragging) dragDX.roundToInt() else 0, 0) }
                .zIndex(if (isDragging) 100f else 1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDragging) GRN.copy(0.15f) else if (isSelected) ACC.copy(0.15f) else Color.Transparent)
                .border(1.dp, if (isDragging) GRN else if (isSelected) ACC else Color.Transparent, RoundedCornerShape(6.dp))
                .pointerInput(elem) {
                    detectDragGestures(
                        onDragStart = { isDragging = true; dragDX = 0f },
                        onDragEnd = {
                            isDragging = false
                            // Reorder based on drag direction
                            val threshold = with(density) { 40.dp.toPx() }
                            val currentOrder = layout.topBarElementOrder.toMutableList()
                            val curIdx = currentOrder.indexOf(elem)
                            if (curIdx >= 0) {
                                if (dragDX > threshold && curIdx < currentOrder.size - 1) {
                                    currentOrder.removeAt(curIdx)
                                    currentOrder.add(curIdx + 1, elem)
                                    onUpdate(layout.copy(topBarElementOrder = currentOrder))
                                } else if (dragDX < -threshold && curIdx > 0) {
                                    currentOrder.removeAt(curIdx)
                                    currentOrder.add(curIdx - 1, elem)
                                    onUpdate(layout.copy(topBarElementOrder = currentOrder))
                                }
                            }
                            dragDX = 0f
                        },
                        onDragCancel = { isDragging = false; dragDX = 0f },
                        onDrag = { change, amt -> change.consume(); dragDX += amt.x }
                    )
                }
                .pointerInput(elem) { detectTapGestures { onSelect(elem) } }
                .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                val isMini = layout.topBarStyle == "MINIMAL"
                when (elem) {
                    LayoutElements.HOLD_PREVIEW -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HOLD", fontSize = if (isMini) 6.sp else 7.sp, color = theme.textSecondary)
                        Box(Modifier.size(if (isMini) 20.dp else 28.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground))
                    }
                    LayoutElements.SCORE -> Text("001234", fontSize = if (isMini) 10.sp else 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
                    LayoutElements.LEVEL -> Text("LV 1", fontSize = if (isMini) 7.sp else 9.sp, fontFamily = FontFamily.Monospace, color = theme.textSecondary, fontWeight = FontWeight.Bold)
                    LayoutElements.LINES -> Text("0 LINES", fontSize = if (isMini) 7.sp else 9.sp, fontFamily = FontFamily.Monospace, color = theme.textSecondary, fontWeight = FontWeight.Bold)
                    LayoutElements.NEXT_PREVIEW -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEXT", fontSize = if (isMini) 6.sp else 7.sp, color = theme.textSecondary)
                        Box(Modifier.size(if (isMini) 20.dp else 28.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground))
                    }
                }
            }
        }
    }
}

// ======================================================================
// BOARD ZONE — Draggable L/C/R with preview
// ======================================================================
@Composable
private fun ColumnScope.BoardZone(
    layout: CustomLayoutData, theme: GameTheme,
    selectedElement: String?, onSelect: (String?) -> Unit,
    onUpdate: (CustomLayoutData) -> Unit
) {
    val isSelected = selectedElement == LayoutElements.BOARD
    val align = layout.boardAlignment
    val hAlign = when (align) { "LEFT" -> Alignment.CenterStart; "RIGHT" -> Alignment.CenterEnd; else -> Alignment.Center }
    val boardWeight = when (layout.boardSize) { "COMPACT" -> 0.35f; "FULLSCREEN" -> 0.55f; else -> 0.45f }

    BoxWithConstraints(Modifier.fillMaxWidth().weight(boardWeight).background(ZONE)
        .clickable { onSelect(LayoutElements.BOARD) }) {

        val density = LocalDensity.current
        val maxWPx = with(density) { maxWidth.toPx() }
        var dragDX by remember { mutableStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        // Board width depends on alignment — center = 85%, left/right = 75%
        val boardW = if (align == "CENTER") maxWidth * 0.85f else maxWidth * 0.75f

        Box(Modifier.align(hAlign)
            .width(boardW).fillMaxHeight()
            .offset { IntOffset(if (isDragging) dragDX.roundToInt() else 0, 0) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true; dragDX = 0f },
                    onDragEnd = {
                        isDragging = false
                        // Determine new alignment based on drag direction
                        val threshold = maxWPx * 0.15f
                        val newAlign = when {
                            dragDX < -threshold -> "LEFT"
                            dragDX > threshold -> "RIGHT"
                            else -> align // stay
                        }
                        if (newAlign != align) onUpdate(layout.copy(boardAlignment = newAlign))
                        dragDX = 0f
                    },
                    onDragCancel = { isDragging = false; dragDX = 0f },
                    onDrag = { change, amt -> change.consume(); dragDX += amt.x }
                )
            }
            .clip(RoundedCornerShape(8.dp))
            .background(theme.screenBackground)
            .border(2.dp, if (isSelected) ACC else if (isDragging) GRN else Color.Transparent, RoundedCornerShape(8.dp))
        ) {
            // Preview board grid
            Column(Modifier.fillMaxSize().padding(4.dp), Arrangement.SpaceEvenly) {
                repeat(12) { r -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(10) { c ->
                    val on = (r == 3 && c in 4..6) || (r == 4 && c == 5) || (r >= 10)
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp).clip(RoundedCornerShape(1.dp))
                        .background(if (on) theme.pixelOn else theme.pixelOff))
                } } }
            }
            // Info overlay when top bar is hidden
            if (!layout.topBarVisible && layout.boardInfoOverlay != "HIDDEN") {
                Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color.Black.copy(0.4f)).padding(4.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("HOLD □", fontSize = 8.sp, color = Color.White.copy(0.7f), fontFamily = FontFamily.Monospace)
                        Text("001234", fontSize = 10.sp, color = Color.White.copy(0.9f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("LV1", fontSize = 8.sp, color = Color.White.copy(0.7f), fontFamily = FontFamily.Monospace)
                        Text("NEXT □", fontSize = 8.sp, color = Color.White.copy(0.7f), fontFamily = FontFamily.Monospace)
                    }
                }
            }
            // Alignment indicator
            if (isDragging) {
                Text(when {
                    dragDX < -(maxWPx * 0.15f) -> "← LEFT"
                    dragDX > (maxWPx * 0.15f) -> "RIGHT →"
                    else -> "CENTER"
                }, color = GRN, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(0.7f)).padding(horizontal = 12.dp, vertical = 4.dp))
            } else {
                // Idle hint
                Text("⟵ drag ⟶", color = DIM.copy(0.3f), fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
            }
        }
    }
}

// ======================================================================
// CONTROLS ZONE — Individually draggable buttons
// ======================================================================
@Composable
private fun ColumnScope.ControlsZone(
    layout: CustomLayoutData, theme: GameTheme,
    selectedElement: String?, onSelect: (String?) -> Unit,
    onUpdate: (CustomLayoutData) -> Unit
) {
    val btnSz = when (layout.controlSize) { "SMALL" -> 44.dp; "LARGE" -> 62.dp; else -> 54.dp }
    val rotSz = when (layout.controlSize) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 66.dp }
    val dpadArea = btnSz * 2.6f
    val isRotateCenter = layout.dpadStyle == "ROTATE_CENTER"

    BoxWithConstraints(Modifier.fillMaxWidth().weight(0.38f).background(ZONE)
        .pointerInput(Unit) { detectTapGestures { onSelect(null) } }
    ) {
        val density = LocalDensity.current
        val maxWPx = with(density) { maxWidth.toPx() }
        val maxHPx = with(density) { maxHeight.toPx() }
        val positions = layout.controlPositions

        // Render each control element
        LayoutElements.controlElements.forEach { elem ->
            if (elem == LayoutElements.ROTATE_BTN && isRotateCenter) return@forEach
            if (!layout.isVisible(elem) && elem != LayoutElements.MENU_BTN) return@forEach

            val pos = positions[elem] ?: CustomLayoutData.defaultControlPositions()[elem] ?: ElementPosition(0.5f, 0.5f)
            val elemW: Dp
            val elemH: Dp
            when (elem) {
                LayoutElements.DPAD -> { elemW = dpadArea; elemH = dpadArea }
                LayoutElements.ROTATE_BTN -> { elemW = rotSz; elemH = rotSz }
                LayoutElements.HOLD_BTN, LayoutElements.PAUSE_BTN -> { elemW = 78.dp; elemH = 34.dp }
                else -> { elemW = 46.dp; elemH = 24.dp } // MENU_BTN
            }

            val isSelected = selectedElement == elem
            val baseXPx = with(density) { (maxWidth * pos.x - elemW / 2).toPx() }
            val baseYPx = with(density) { (maxHeight * pos.y - elemH / 2).toPx() }
            var dragDX by remember { mutableStateOf(0f) }
            var dragDY by remember { mutableStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }
            var wasDragged by remember { mutableStateOf(false) }

            LaunchedEffect(pos.x, pos.y) { if (!isDragging) { dragDX = 0f; dragDY = 0f } }

            Box(Modifier
                .offset { IntOffset((baseXPx + dragDX).roundToInt(), (baseYPx + dragDY).roundToInt()) }
                .size(elemW, elemH)
                .zIndex(if (isDragging) 100f else if (isSelected) 50f else 1f)
                .pointerInput(elem) {
                    detectDragGestures(
                        onDragStart = { isDragging = true; wasDragged = false; dragDX = 0f; dragDY = 0f },
                        onDragEnd = {
                            isDragging = false
                            if (wasDragged) {
                                val newX = (pos.x + dragDX / maxWPx).coerceIn(0.05f, 0.95f)
                                val newY = (pos.y + dragDY / maxHPx).coerceIn(0.05f, 0.95f)
                                dragDX = 0f; dragDY = 0f
                                onUpdate(layout.copy(controlPositions = positions + (elem to ElementPosition(newX, newY))))
                            } else { dragDX = 0f; dragDY = 0f }
                        },
                        onDragCancel = { isDragging = false; dragDX = 0f; dragDY = 0f },
                        onDrag = { change, amt -> change.consume(); wasDragged = true; dragDX += amt.x; dragDY += amt.y }
                    )
                }
                .pointerInput(elem) { detectTapGestures { onSelect(elem) } }
                .then(
                    if (isDragging) Modifier.border(2.dp, GRN, RoundedCornerShape(6.dp))
                    else if (isSelected) Modifier.border(2.dp, ACC, RoundedCornerShape(6.dp))
                    else Modifier.border(1.dp, SEP.copy(0.5f), RoundedCornerShape(6.dp))
                )
            ) {
                when (elem) {
                    LayoutElements.DPAD -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Box(Modifier.size(btnSz).offset(y = -(btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("▲", color = theme.buttonSecondary, fontSize = 12.sp) }
                            Box(Modifier.size(btnSz).offset(y = (btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("▼", color = theme.buttonSecondary, fontSize = 12.sp) }
                            Box(Modifier.size(btnSz).offset(x = -(btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("◄", color = theme.buttonSecondary, fontSize = 12.sp) }
                            Box(Modifier.size(btnSz).offset(x = (btnSz * 0.6f)).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("►", color = theme.buttonSecondary, fontSize = 12.sp) }
                            if (isRotateCenter) Box(Modifier.size(btnSz * 0.7f).clip(CircleShape).background(theme.accentColor), Alignment.Center) { Text("↻", color = theme.backgroundColor, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            else Box(Modifier.size(btnSz * 0.5f).clip(CircleShape).background(theme.buttonPrimaryPressed.copy(0.4f)))
                        }
                    }
                    LayoutElements.ROTATE_BTN -> Box(Modifier.fillMaxSize().shadow(4.dp, CircleShape).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("↻", color = theme.buttonSecondary, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    LayoutElements.HOLD_BTN -> Box(Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)).background(theme.accentColor.copy(0.8f)), Alignment.Center) { Text("HOLD", color = theme.backgroundColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    LayoutElements.PAUSE_BTN -> Box(Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)).background(theme.accentColor.copy(0.8f)), Alignment.Center) { Text("PAUSE", color = theme.backgroundColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    LayoutElements.MENU_BTN -> Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(theme.accentColor), Alignment.Center) { Text("···", color = theme.backgroundColor, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ======================================================================
// SIDE MENU — Expandable sections with per-element settings
// ======================================================================
@Composable
private fun SideMenu(
    layout: CustomLayoutData, selectedElement: String?,
    onUpdate: (CustomLayoutData) -> Unit,
    onSelectElement: (String?) -> Unit,
    onUndo: () -> Unit, onRedo: () -> Unit,
    undoAvail: Boolean, redoAvail: Boolean,
    onClose: () -> Unit
) {
    Column(Modifier.width(210.dp).fillMaxHeight().background(Color(0xF0111111))
        .pointerInput(Unit) { detectTapGestures { } }
        .verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Close
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Settings", color = ACC, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("✕", color = DIM, fontSize = 16.sp, modifier = Modifier.clickable { onClose() }.padding(4.dp))
        }
        Spacer(Modifier.height(4.dp))

        // === Undo / Redo ===
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
            MiniBtn("↶ Undo", undoAvail) { onUndo() }
            MiniBtn("↷ Redo", redoAvail) { onRedo() }
        }
        Spacer(Modifier.height(4.dp))

        // === TOP BAR SECTION ===
        SideSection("▸ Top Bar") {
            SideToggle("Show Top Bar", layout.topBarVisible) { onUpdate(layout.copy(topBarVisible = it)) }
            if (layout.topBarVisible) {
                SideLabel("Bar Style")
                SideChips(listOf("DEVICE_FRAME" to "Frame", "COMPACT" to "Compact", "MINIMAL" to "Minimal"), layout.topBarStyle) { onUpdate(layout.copy(topBarStyle = it)) }
                SideLabel("Elements")
                LayoutElements.topBarElements.forEach { elem ->
                    val label = elem.replace("_PREVIEW", "").replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    SideToggle(label, layout.isVisible(elem)) { onUpdate(layout.copy(visibility = layout.visibility + (elem to it))) }
                }
                SideLabel("Next Queue")
                SideChips(listOf("1" to "1", "2" to "2", "3" to "3"), layout.nextQueueSize.toString()) { onUpdate(layout.copy(nextQueueSize = it.toInt())) }
            }
        }

        // === BOARD SECTION ===
        SideSection("▸ LCD Board") {
            SideLabel("Position")
            SideChips(listOf("LEFT" to "Left", "CENTER" to "Center", "RIGHT" to "Right"), layout.boardAlignment) { onUpdate(layout.copy(boardAlignment = it)) }
            SideLabel("Size")
            SideChips(listOf("COMPACT" to "Compact", "STANDARD" to "Standard", "FULLSCREEN" to "Full"), layout.boardSize) { onUpdate(layout.copy(boardSize = it)) }
            if (!layout.topBarVisible) {
                SideLabel("Info Overlay")
                SideChips(listOf("TOP" to "Top", "SIDE" to "Side", "HIDDEN" to "Hidden"), layout.boardInfoOverlay) { onUpdate(layout.copy(boardInfoOverlay = it)) }
            }
        }

        // === CONTROLS SECTION ===
        SideSection("▸ Controls") {
            SideLabel("Control Size")
            SideChips(listOf("SMALL" to "S", "MEDIUM" to "M", "LARGE" to "L"), layout.controlSize) { onUpdate(layout.copy(controlSize = it)) }
            SideLabel("D-Pad Style")
            SideChips(listOf("STANDARD" to "Standard", "ROTATE_CENTER" to "Rotate Center"), layout.dpadStyle) { onUpdate(layout.copy(dpadStyle = it)) }
            SideToggle("Hold Button", layout.isVisible(LayoutElements.HOLD_BTN)) { onUpdate(layout.copy(visibility = layout.visibility + (LayoutElements.HOLD_BTN to it))) }
            SideToggle("Pause Button", layout.isVisible(LayoutElements.PAUSE_BTN)) { onUpdate(layout.copy(visibility = layout.visibility + (LayoutElements.PAUSE_BTN to it))) }
        }

        // === SELECTED ELEMENT ===
        if (selectedElement != null) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(ACC.copy(0.3f)))
            Spacer(Modifier.height(4.dp))
            val label = selectedElement.replace("_PREVIEW", "").replace("_BTN", "").replace("_", " ")
            Text("Selected: $label", color = ACC, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            // Size per element (controls only)
            if (selectedElement in LayoutElements.controlElements) {
                SideLabel("Size")
                SideChips(listOf("SMALL" to "S", "MEDIUM" to "M", "LARGE" to "L"), layout.sizeFor(selectedElement)) { onUpdate(layout.copy(elementSizes = layout.elementSizes + (selectedElement to it))) }
            }

            // Hide (if hideable)
            if (selectedElement in LayoutElements.hideable) {
                SideToggle("Visible", layout.isVisible(selectedElement)) { onUpdate(layout.copy(visibility = layout.visibility + (selectedElement to it))) }
            }

            // Deselect
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2A)).clickable { onSelectElement(null) }.padding(8.dp), Alignment.Center) { Text("Deselect", color = DIM, fontSize = 11.sp) }
        }

        // === GENERAL ===
        Spacer(Modifier.height(4.dp))
        SideSection("▸ General") {
            SideToggle("Auto-rotate Screen", layout.autoRotate) { onUpdate(layout.copy(autoRotate = it)) }
        }

        Spacer(Modifier.height(8.dp))
        // Reset
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF3A1515)).clickable {
            onUpdate(CustomLayoutData(id = layout.id, name = layout.name))
        }.padding(8.dp), Alignment.Center) { Text("↺ Reset All", color = Color(0xFFFF6B6B), fontSize = 11.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(40.dp))
    }
}

// === Side menu helpers ===
@Composable private fun MiniBtn(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (enabled) Color(0xFF2A2A3A) else CARD)
        .clickable(enabled = enabled) { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(text, color = if (enabled) Color(0xFF8AB4F8) else DIM.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable private fun SideSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Text(title, color = TX, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CARD)
            .clickable { expanded = !expanded }.padding(8.dp))
    if (expanded) Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}
@Composable private fun SideLabel(text: String) { Text(text, color = DIM, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp)) }
@Composable private fun SideToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = if (value) TX else DIM, fontSize = 11.sp)
        Switch(value, onChange, modifier = Modifier.height(24.dp),
            colors = SwitchDefaults.colors(checkedTrackColor = ACC, uncheckedTrackColor = Color(0xFF333333)))
    }
}
@Composable private fun SideChips(options: List<Pair<String, String>>, current: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(3.dp)) {
        options.forEach { (k, v) ->
            val sel = current == k
            Box(Modifier.weight(1f).clip(RoundedCornerShape(5.dp)).background(if (sel) ACC.copy(0.2f) else Color(0xFF282828))
                .clickable { onChange(k) }.padding(vertical = 5.dp), Alignment.Center) {
                Text(v, color = if (sel) ACC else DIM, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

// ======================================================================
// THEME EDITOR (unchanged from previous)
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
        Box(Modifier.fillMaxWidth().weight(0.45f).padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp)).background(theme.backgroundColor).border(2.dp, if (selectedTarget?.label == "Background") GRN else Color.Transparent, RoundedCornerShape(12.dp)).clickable { selectedTarget = themeTargets[0] }) {
            Box(Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(8.dp)).background(theme.deviceColor).border(2.dp, if (selectedTarget?.label == "Device Frame") GRN else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedTarget = themeTargets[1] }) {
                Row(Modifier.fillMaxSize().padding(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(theme.screenBackground).border(2.dp, if (selectedTarget?.label == "Screen / LCD") GRN else Color.Transparent, RoundedCornerShape(6.dp)).clickable { selectedTarget = themeTargets[2] }) {
                        Column(Modifier.fillMaxSize().padding(6.dp), Arrangement.SpaceEvenly) { repeat(4) { r -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(6) { c -> val isOn = (r + c) % 3 == 0; Box(Modifier.size(16.dp).clip(RoundedCornerShape(2.dp)).background(if (isOn) theme.pixelOn else theme.pixelOff).border(1.dp, if (selectedTarget?.label == (if (isOn) "Blocks (ON)" else "Empty Cells")) GRN else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = if (isOn) themeTargets[3] else themeTargets[4] }) } } } }
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.width(50.dp).fillMaxHeight(), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                        Text("00123", color = theme.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Accent / Score") GRN else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[9] }.padding(2.dp))
                        Text("LV 3", color = theme.textPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Text Primary") GRN else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[5] }.padding(2.dp))
                        Text("Info", color = theme.textSecondary, fontSize = 9.sp, modifier = Modifier.border(1.dp, if (selectedTarget?.label == "Text Secondary") GRN else Color.Transparent, RoundedCornerShape(2.dp)).clickable { selectedTarget = themeTargets[6] }.padding(2.dp))
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(theme.buttonPrimary).border(2.dp, if (selectedTarget?.label == "Button Color") GRN else Color.Transparent, CircleShape).clickable { selectedTarget = themeTargets[7] })
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(theme.buttonSecondary).border(2.dp, if (selectedTarget?.label == "Button Text") GRN else Color.Transparent, RoundedCornerShape(12.dp)).clickable { selectedTarget = themeTargets[8] })
            }
        }
        Text("Tap element above or select:", color = DIM, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(themeTargets.size) { i -> val t = themeTargets[i]; val sel = selectedTarget == t; Row(Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) ACC.copy(0.2f) else CARD).border(1.dp, if (sel) ACC else DIM.copy(0.2f), RoundedCornerShape(8.dp)).clickable { selectedTarget = t }.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Box(Modifier.size(16.dp).clip(CircleShape).background(t.getColor(theme)).border(1.dp, DIM.copy(0.3f), CircleShape)); Text(t.label, color = if (sel) ACC else TX, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) } } }
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
