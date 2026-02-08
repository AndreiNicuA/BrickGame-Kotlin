package com.brickgame.tetris.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = maxWidth
        val maxH = maxHeight

        // Background board fills screen
        GameBoard(
            board = gs.board, modifier = Modifier.fillMaxSize(),
            currentPiece = gs.currentPiece, ghostY = gs.ghostY,
            showGhost = ghostEnabled, clearingLines = gs.clearedLineRows,
            animationStyle = animationStyle, animationDuration = animationDuration,
            multiColor = LocalMultiColor.current
        )

        // Render each visible element at its position
        elements.values.filter { it.visible }.forEach { elem ->
            val type = FreeformElementType.fromKey(elem.key) ?: return@forEach
            val scale = elem.size
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

/** Base width×height in dp for each element type at scale=1.0 */
private fun elementBaseSize(type: FreeformElementType): Pair<Dp, Dp> = when (type) {
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

/** Render the actual game element based on its type */
@Composable
private fun RenderElement(
    type: FreeformElementType, scale: Float, gs: GameState,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit,
    onSet: () -> Unit, onStart: () -> Unit
) {
    val theme = LocalGameTheme.current
    val sz = (50 * scale).dp
    when (type) {
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
        FreeformElementType.HOLD_BTN -> ActionButton("HOLD", onHold, width = (78 * scale).dp, height = (34 * scale).dp)
        FreeformElementType.PAUSE_BTN -> ActionButton(
            if (gs.status == GameStatus.MENU) "START" else "PAUSE",
            { if (gs.status == GameStatus.MENU) onStart() else onPause() },
            width = (78 * scale).dp, height = (34 * scale).dp)
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

// ============================================================================
// STANDALONE FREEFORM EDITOR SCREEN
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

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        // Empty board grid background
        EmptyBoardPreview(Modifier.fillMaxSize(), gridColor = theme.pixelOff.copy(alpha = 0.3f))

        // Draggable elements
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val maxW = maxWidth
            val maxH = maxHeight

            elements.values.filter { it.visible }.forEach { elem ->
                val type = FreeformElementType.fromKey(elem.key) ?: return@forEach
                val isSelected = elem.key == selectedKey
                val baseSize = elementBaseSize(type)
                val handleSizeDp = minOf(baseSize.first, baseSize.second) * elem.size
                val handleSizePx = with(density) { handleSizeDp.toPx() }
                val isControl = type.category == ElementCategory.CONTROL
                val baseColor = if (isControl) Color(0xFF3B82F6) else Color(0xFFF59E0B)

                DraggableElement(
                    key = elem.key,
                    label = type.displayName,
                    position = elem,
                    maxWidthPx = maxWidthPx,
                    maxHeightPx = maxHeightPx,
                    handleSizePx = handleSizePx,
                    handleSizeDp = handleSizeDp,
                    color = if (isSelected) Color(0xFF22C55E) else baseColor,
                    alpha = elem.alpha,
                    isSelected = isSelected,
                    onTap = { selectedKey = if (selectedKey == elem.key) null else elem.key },
                    onDragEnd = { newX, newY ->
                        onElementUpdated(elem.copy(x = newX, y = newY))
                    }
                )
            }
        }

        // Top: instructions
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Drag elements · Tap to select", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(Color(0xFF3B82F6), "Controls")
                    LegendDot(Color(0xFFF59E0B), "Info")
                    LegendDot(Color(0xFF22C55E), "Selected")
                }
            }
        }

        // Bottom panel: property controls for selected element + add/remove + actions
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            // Selected element properties
            if (selectedElement != null) {
                val selType = FreeformElementType.fromKey(selectedElement.key)
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    color = Color(0xFF1A1A1A).copy(alpha = 0.95f)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(selType?.displayName ?: selectedElement.key, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("✕ Remove", color = Color(0xFFFF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onElementRemoved(selectedElement.key); selectedKey = null }.padding(4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        // Size slider
                        SliderRow("Size", selectedElement.size, 0.4f, 2.0f) {
                            onElementUpdated(selectedElement.copy(size = it))
                        }
                        // Transparency slider
                        SliderRow("Opacity", selectedElement.alpha, 0.05f, 1.0f) {
                            onElementUpdated(selectedElement.copy(alpha = it))
                        }
                    }
                }
            }

            // Add elements row
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                color = Color(0xFF111111).copy(alpha = 0.95f)
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text("+ Add Element", color = Color(0xFF888888), fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        val existing = elements.keys
                        val available = PlayerProfile.availableElements().filter { it.key !in existing }
                        items(available) { type ->
                            val isControl = type.category == ElementCategory.CONTROL
                            Surface(
                                modifier = Modifier.clickable {
                                    // Add at center with defaults
                                    onElementAdded(FreeformElement(type.key, 0.5f, 0.5f))
                                    selectedKey = type.key
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isControl) Color(0xFF3B82F6).copy(0.2f) else Color(0xFFF59E0B).copy(0.2f)
                            ) {
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(type.displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text(type.description, color = Color.White.copy(0.5f), fontSize = 9.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons: Reset + Done
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.9f)).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(onClick = { selectedKey = null; onReset() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Text("↺ Reset")
                }
                Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))) {
                    Text("✓ Done", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.width(56.dp))
        Slider(
            value = value, onValueChange = onChange, valueRange = min..max,
            modifier = Modifier.weight(1f).height(24.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
        )
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

/** Draggable handle for editor — shows colored circle + label, drag to reposition */
@Composable
private fun BoxWithConstraintsScope.DraggableElement(
    key: String,
    label: String,
    position: FreeformElement,
    maxWidthPx: Float,
    maxHeightPx: Float,
    handleSizePx: Float,
    handleSizeDp: Dp,
    color: Color,
    alpha: Float,
    isSelected: Boolean,
    onTap: () -> Unit,
    onDragEnd: (Float, Float) -> Unit
) {
    var offsetX by remember(key, position.x) { mutableFloatStateOf(position.x * maxWidthPx - handleSizePx / 2) }
    var offsetY by remember(key, position.y) { mutableFloatStateOf(position.y * maxHeightPx - handleSizePx / 2) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(position.x, position.y) {
        if (!isDragging) {
            offsetX = position.x * maxWidthPx - handleSizePx / 2
            offsetY = position.y * maxHeightPx - handleSizePx / 2
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(key) {
                detectDragGestures(
                    onDragStart = { isDragging = true; onTap() },
                    onDragEnd = {
                        isDragging = false
                        val cx = (offsetX + handleSizePx / 2) / maxWidthPx
                        val cy = (offsetY + handleSizePx / 2) / maxHeightPx
                        onDragEnd(cx.coerceIn(0.03f, 0.97f), cy.coerceIn(0.03f, 0.97f))
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWidthPx - handleSizePx)
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxHeightPx - handleSizePx)
                    }
                )
            }
            .clickable { onTap() }
    ) {
        val clampedSize = handleSizeDp.coerceIn(32.dp, 80.dp)
        Surface(
            modifier = Modifier.size(clampedSize)
                .then(if (isSelected) Modifier.border(2.dp, Color(0xFF22C55E), CircleShape) else Modifier)
                .shadow(if (isDragging) 12.dp else 4.dp, CircleShape),
            shape = CircleShape,
            color = color.copy(alpha = if (isDragging) 0.95f else 0.8f * alpha)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(label.take(4), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = clampedSize + 1.dp)
                .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
    }
}

/** Empty 10×20 grid preview for editor background */
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
