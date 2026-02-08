package com.brickgame.tetris.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.*
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.screens.LocalMultiColor
import com.brickgame.tetris.ui.theme.LocalGameTheme
import kotlin.math.roundToInt

// ============================================================================
// FREEFORM GAME LAYOUT — renders during actual gameplay
// Board and all elements are independently positioned.
// ============================================================================

@Composable
fun FreeformGameLayout(
    gs: GameState,
    dpadStyle: DPadStyle,
    ghostEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    elements: Map<String, FreeformElement>,
    onRotate: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onLeftPress: () -> Unit, onLeftRelease: () -> Unit,
    onRightPress: () -> Unit, onRightRelease: () -> Unit,
    onDownPress: () -> Unit, onDownRelease: () -> Unit,
    onPause: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartGame: () -> Unit
) {
    val theme = LocalGameTheme.current

    BoxWithConstraints(Modifier.fillMaxSize().background(theme.backgroundColor)) {
        val maxW = maxWidth
        val maxH = maxHeight

        // Render each visible element at its position
        elements.values.filter { it.visible }.sortedBy {
            // Board renders first (behind everything)
            if (it.key == "BOARD") 0 else 1
        }.forEach { elem ->
            val type = FreeformElementType.fromKey(elem.key) ?: return@forEach
            val scale = elem.size

            if (type == FreeformElementType.BOARD) {
                // Board uses same base size as editor
                val baseSize = elementBaseSize(type)
                val bw = baseSize.first * scale
                val bh = baseSize.second * scale
                Box(
                    Modifier
                        .offset(x = maxW * elem.x - bw / 2, y = maxH * elem.y - bh / 2)
                        .size(bw, bh)
                        .alpha(elem.alpha)
                ) {
                    GameBoard(
                        board = gs.board, modifier = Modifier.fillMaxSize(),
                        currentPiece = gs.currentPiece, ghostY = gs.ghostY,
                        showGhost = ghostEnabled, clearingLines = gs.clearedLineRows,
                        animationStyle = animationStyle, animationDuration = animationDuration,
                        multiColor = LocalMultiColor.current
                    )
                }
            } else {
                val baseSize = elementBaseSize(type)
                val w = baseSize.first * scale
                val h = baseSize.second * scale
                Box(
                    Modifier
                        .offset(x = maxW * elem.x - w / 2, y = maxH * elem.y - h / 2)
                        .alpha(elem.alpha)
                ) {
                    RenderElement(type, scale, gs, onRotate, onHardDrop, onHold,
                        onLeftPress, onLeftRelease, onRightPress, onRightRelease,
                        onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame)
                }
            }
        }
    }
}

/** Base width×height in dp for each element type at scale=1.0 */
private fun elementBaseSize(type: FreeformElementType): Pair<Dp, Dp> = when (type) {
    FreeformElementType.BOARD -> 200.dp to 360.dp
    FreeformElementType.DPAD -> 150.dp to 150.dp
    FreeformElementType.DPAD_ROTATE -> 150.dp to 150.dp
    FreeformElementType.BTN_UP, FreeformElementType.BTN_DOWN,
    FreeformElementType.BTN_LEFT, FreeformElementType.BTN_RIGHT -> 56.dp to 56.dp
    FreeformElementType.ROTATE -> 64.dp to 64.dp
    FreeformElementType.HOLD_BTN -> 78.dp to 34.dp
    FreeformElementType.PAUSE_BTN -> 78.dp to 34.dp
    FreeformElementType.MENU_BTN -> 46.dp to 24.dp
    FreeformElementType.SCORE -> 120.dp to 28.dp
    FreeformElementType.LEVEL -> 56.dp to 20.dp
    FreeformElementType.LINES -> 72.dp to 20.dp
    FreeformElementType.HOLD_PREVIEW -> 48.dp to 56.dp
    FreeformElementType.NEXT_PREVIEW -> 48.dp to 100.dp
}

/** Render actual game components — used both in gameplay and editor preview */
@Composable
fun RenderElement(
    type: FreeformElementType, scale: Float, gs: GameState,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit,
    onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    val sz = (50 * scale).dp
    when (type) {
        FreeformElementType.BOARD -> {} // handled separately
        FreeformElementType.DPAD -> DPad(buttonSize = sz, rotateInCenter = false,
            onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
            onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        FreeformElementType.DPAD_ROTATE -> DPad(buttonSize = sz, rotateInCenter = true,
            onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
            onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        FreeformElementType.BTN_UP -> TapButton(ButtonIcon.UP, sz, onClick = onHD)
        FreeformElementType.BTN_DOWN -> HoldButton(ButtonIcon.DOWN, sz, onPress = onDP, onRelease = onDR)
        FreeformElementType.BTN_LEFT -> HoldButton(ButtonIcon.LEFT, sz, onPress = onLP, onRelease = onLR)
        FreeformElementType.BTN_RIGHT -> HoldButton(ButtonIcon.RIGHT, sz, onPress = onRP, onRelease = onRR)
        FreeformElementType.ROTATE -> RotateButton(onRotate, (64 * scale).dp)
        FreeformElementType.HOLD_BTN -> ActionButton("HOLD", onHold, width = (78 * scale).dp, height = (34 * scale).dp, backgroundColor = theme.buttonPrimary)
        FreeformElementType.PAUSE_BTN -> ActionButton(
            if (gs.status == GameStatus.MENU) "START" else "PAUSE",
            { if (gs.status == GameStatus.MENU) onStart() else onPause() },
            width = (78 * scale).dp, height = (34 * scale).dp, backgroundColor = theme.buttonPrimary)
        FreeformElementType.MENU_BTN -> ActionButton("···", onSet, width = (46 * scale).dp, height = (24 * scale).dp)
        FreeformElementType.SCORE -> Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(gs.score.toString().padStart(7, '0'), fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn, letterSpacing = 2.sp)
        }
        FreeformElementType.LEVEL -> Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text("LV ${gs.level}", fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
        }
        FreeformElementType.LINES -> Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text("${gs.lines} LINES", fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
        }
        FreeformElementType.HOLD_PREVIEW -> Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(4.dp)) {
            HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size((36 * scale).dp))
        }
        FreeformElementType.NEXT_PREVIEW -> Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(4.dp)) {
            Column {
                gs.nextPieces.take(3).forEachIndexed { i, p ->
                    NextPiecePreview(p.shape, Modifier.size((if (i == 0) 36 * scale else 24 * scale).dp), if (i == 0) 1f else 0.5f)
                }
            }
        }
    }
}

/** Render a non-interactive preview of an element for the editor */
@Composable
private fun RenderEditorPreview(type: FreeformElementType, scale: Float) {
    val theme = LocalGameTheme.current
    val dummyGs = remember { GameState() }
    val noop: () -> Unit = {}
    // Wrap in a box that absorbs all pointer events so dragging works
    Box(Modifier.pointerInput(Unit) { /* absorb touches */ }) {
        when (type) {
            FreeformElementType.BOARD -> {
                // Board: DO NOT render here — board uses BoardOutlineHandle instead
            }
            else -> RenderElement(type, scale, dummyGs, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop)
        }
    }
}

// ============================================================================
// STANDALONE FREEFORM EDITOR SCREEN
// Full screen for dragging. Real buttons shown. Floating toolbar for menu.
// ============================================================================

@Composable
fun FreeformEditorScreen(
    elements: Map<String, FreeformElement>,
    onElementUpdated: (FreeformElement) -> Unit,
    onElementAdded: (FreeformElement) -> Unit,
    onElementRemoved: (String) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    val theme = LocalGameTheme.current
    val density = LocalDensity.current
    var selectedKey by remember { mutableStateOf<String?>(null) }
    val selectedElement = selectedKey?.let { elements[it] }
    var showMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        // Empty board grid background
        EmptyBoardPreview(Modifier.fillMaxSize(), gridColor = theme.pixelOff.copy(alpha = 0.3f))

        // Semi-transparent overlay
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.25f)))

        // Draggable elements with real button previews
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }

            elements.values.filter { it.visible }.sortedBy {
                // Board outline renders first (behind everything), other elements on top
                if (it.key == "BOARD") 0 else 1
            }.forEach { elem ->
                val type = FreeformElementType.fromKey(elem.key) ?: return@forEach
                val isSelected = elem.key == selectedKey
                val scale = elem.size

                if (type == FreeformElementType.BOARD) {
                    // Board: outline + centered text handle (non-blocking)
                    val bs = elementBaseSize(type)
                    val bw = bs.first * scale
                    val bh = bs.second * scale
                    val bwPx = with(density) { bw.toPx() }
                    val bhPx = with(density) { bh.toPx() }

                    // Dashed outline (not interactive — just shows size)
                    Box(
                        Modifier
                            .offset(x = maxWidth * elem.x - bw / 2, y = maxHeight * elem.y - bh / 2)
                            .size(bw, bh)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF22C55E) else Color.White.copy(0.4f),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )

                    // Draggable text handle at center of the outline
                    val handleSizePx = with(density) { 60.dp.toPx() }
                    DraggableRealElement(
                        key = elem.key,
                        label = "Board",
                        position = elem,
                        type = type,
                        scale = scale,
                        maxWidthPx = maxWidthPx,
                        maxHeightPx = maxHeightPx,
                        elemSizePx = handleSizePx,
                        isSelected = isSelected,
                        onTap = { selectedKey = if (selectedKey == elem.key) null else elem.key },
                        onDragEnd = { newX, newY -> onElementUpdated(elem.copy(x = newX, y = newY)) },
                        customContent = {
                            // Small "Board" label as the drag handle
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF22C55E).copy(0.9f) else Color(0xFF8B5CF6).copy(0.7f),
                                shadowElevation = 4.dp
                            ) {
                                Text("Board", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    )
                } else {
                    // Normal elements — same as before
                    val baseSize = elementBaseSize(type)
                    val elemSizeDp = when (type) {
                        FreeformElementType.DPAD, FreeformElementType.DPAD_ROTATE -> (150 * scale).dp
                        else -> minOf(baseSize.first, baseSize.second) * scale
                    }
                    val elemSizePx = with(density) { elemSizeDp.toPx() }

                    DraggableRealElement(
                        key = elem.key,
                        label = type.displayName,
                        position = elem,
                        type = type,
                        scale = scale,
                        maxWidthPx = maxWidthPx,
                        maxHeightPx = maxHeightPx,
                        elemSizePx = elemSizePx,
                        isSelected = isSelected,
                        onTap = { selectedKey = if (selectedKey == elem.key) null else elem.key },
                        onDragEnd = { newX, newY -> onElementUpdated(elem.copy(x = newX, y = newY)) }
                    )
                }
            }
        }

        // Compact top bar
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                .background(Color.Black.copy(0.7f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Drag to position", color = Color.White.copy(0.7f), fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Surface(
                modifier = Modifier.clickable { onDone() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF22C55E)
            ) {
                Text("✓ Done", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }

        // Floating toolbar button (bottom-right)
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
                .size(52.dp).clickable { showMenu = true },
            shape = CircleShape,
            color = if (selectedElement != null) Color(0xFF22C55E) else Color(0xFF3B82F6),
            shadowElevation = 8.dp
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("☰", fontSize = 22.sp, color = Color.White)
            }
        }

        // Popup menu overlay
        if (showMenu) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable { showMenu = false })
            Surface(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.88f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A1A),
                shadowElevation = 16.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Freeform Editor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("✕", color = Color(0xFF888888), fontSize = 20.sp,
                            modifier = Modifier.clickable { showMenu = false }.padding(4.dp))
                    }
                    Spacer(Modifier.height(12.dp))

                    // Selected element properties
                    if (selectedElement != null) {
                        val selType = FreeformElementType.fromKey(selectedElement.key)
                        val selKey = selectedElement.key
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFF222222)) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).background(Color(0xFF22C55E), CircleShape))
                                        Spacer(Modifier.width(6.dp))
                                        Text(selType?.displayName ?: selKey, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text("Remove", color = Color(0xFFFF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            onElementRemoved(selKey); selectedKey = null
                                        }.padding(4.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                SliderRow("Size", selectedElement.size, 0.4f, 2.0f) { newSize ->
                                    // Read fresh from elements map to avoid stale captures
                                    val fresh = elements[selKey] ?: return@SliderRow
                                    onElementUpdated(fresh.copy(size = newSize))
                                }
                                SliderRow("Opacity", selectedElement.alpha, 0.05f, 1.0f) { newAlpha ->
                                    val fresh = elements[selKey] ?: return@SliderRow
                                    onElementUpdated(fresh.copy(alpha = newAlpha))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Add elements
                    val existing = elements.keys
                    val available = PlayerProfile.availableElements().filter { it.key !in existing }
                    if (available.isNotEmpty()) {
                        Text("Add Element", color = Color(0xFF888888), fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        available.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { type ->
                                    val c = when (type.category) {
                                        ElementCategory.CONTROL -> Color(0xFF3B82F6)
                                        ElementCategory.INFO -> Color(0xFFF59E0B)
                                        ElementCategory.BOARD -> Color(0xFF8B5CF6)
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable {
                                            onElementAdded(FreeformElement(type.key, 0.5f, 0.5f))
                                            selectedKey = type.key; showMenu = false
                                        },
                                        shape = RoundedCornerShape(8.dp), color = c.copy(0.15f)
                                    ) {
                                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(type.displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                            Text(type.description, color = Color.White.copy(0.4f), fontSize = 9.sp, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Actions
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                        OutlinedButton(onClick = { selectedKey = null; onReset(); showMenu = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                            Text("↺ Reset All")
                        }
                        Button(onClick = { showMenu = false; onDone() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))) {
                            Text("✓ Done", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/** Draggable element that renders the REAL button/component preview */
@Composable
private fun BoxWithConstraintsScope.DraggableRealElement(
    key: String,
    label: String,
    position: FreeformElement,
    type: FreeformElementType,
    scale: Float,
    maxWidthPx: Float,
    maxHeightPx: Float,
    elemSizePx: Float,
    isSelected: Boolean,
    onTap: () -> Unit,
    onDragEnd: (Float, Float) -> Unit,
    customContent: (@Composable () -> Unit)? = null
) {
    // Use position + size as remember keys so offset recalculates on ANY property change
    var offsetX by remember(key, position.x, position.y, elemSizePx) {
        mutableFloatStateOf(position.x * maxWidthPx - elemSizePx / 2)
    }
    var offsetY by remember(key, position.x, position.y, elemSizePx) {
        mutableFloatStateOf(position.y * maxHeightPx - elemSizePx / 2)
    }
    var isDragging by remember { mutableStateOf(false) }

    // Sync position when changed externally (and not currently dragging)
    LaunchedEffect(position.x, position.y, elemSizePx) {
        if (!isDragging) {
            offsetX = position.x * maxWidthPx - elemSizePx / 2
            offsetY = position.y * maxHeightPx - elemSizePx / 2
        }
    }

    // Capture current elemSizePx for use inside drag lambda
    val currentElemSizePx by rememberUpdatedState(elemSizePx)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(key, elemSizePx) {
                detectDragGestures(
                    onDragStart = { isDragging = true; onTap() },
                    onDragEnd = {
                        isDragging = false
                        val sz = currentElemSizePx
                        val cx = (offsetX + sz / 2) / maxWidthPx
                        val cy = (offsetY + sz / 2) / maxHeightPx
                        onDragEnd(cx.coerceIn(0.03f, 0.97f), cy.coerceIn(0.03f, 0.97f))
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val sz = currentElemSizePx
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWidthPx - sz)
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxHeightPx - sz)
                    }
                )
            }
            .clickable { onTap() }
            .alpha(position.alpha)
    ) {
        // Selection highlight border
        if (isSelected && customContent == null) {
            Box(Modifier.matchParentSize().border(2.dp, Color(0xFF22C55E), RoundedCornerShape(8.dp)))
        }
        // Render the actual button/component or custom content
        if (customContent != null) {
            customContent()
        } else {
            RenderEditorPreview(type, scale)
        }
        // Small label underneath (skip for custom content — it has its own label)
        if (customContent == null) {
            Text(label, fontSize = 9.sp, color = Color.White.copy(0.8f),
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 14.dp)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp))
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.width(56.dp))
        Slider(value = value, onValueChange = onChange, valueRange = min..max,
            modifier = Modifier.weight(1f).height(24.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E)))
        Text("${(value * 100).toInt()}%", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
    }
}

@Composable
private fun EmptyBoardPreview(modifier: Modifier = Modifier, gridColor: Color) {
    androidx.compose.foundation.Canvas(modifier) {
        val cols = 10; val rows = 20
        val cellSize = minOf(size.width / cols, size.height / rows)
        val boardW = cellSize * cols; val boardH = cellSize * rows
        val ox = (size.width - boardW) / 2; val oy = (size.height - boardH) / 2
        for (r in 0 until rows) for (c in 0 until cols) {
            drawRect(gridColor, androidx.compose.ui.geometry.Offset(ox + c * cellSize + 1f, oy + r * cellSize + 1f),
                androidx.compose.ui.geometry.Size(cellSize - 2f, cellSize - 2f))
        }
    }
}
