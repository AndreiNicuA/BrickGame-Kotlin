package com.brickgame.tetris.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
    boardShape: BoardShape = BoardShape.STANDARD,
    infoBarType: InfoBarType = InfoBarType.INDIVIDUAL,
    infoBarShape: InfoBarShape = InfoBarShape.PILL,
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
                val bh = baseSize.second * elem.effectiveH

                // Board shape container modifier
                val boardMod = when (boardShape) {
                    BoardShape.STANDARD -> Modifier
                        .border(1.5.dp, theme.deviceColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                    BoardShape.FRAMELESS -> Modifier  // No border at all
                    BoardShape.DEVICE_FRAME -> Modifier
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .border(6.dp, theme.deviceColor, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                    BoardShape.BEVELED -> Modifier
                        .drawBehind {
                            val bevelW = 4.dp.toPx()
                            val hl = theme.deviceColor.lighten(0.3f)
                            val sh = theme.deviceColor.darken(0.3f)
                            // Top highlight
                            drawLine(hl, Offset(0f, bevelW / 2), Offset(size.width, bevelW / 2), bevelW)
                            // Left highlight
                            drawLine(hl, Offset(bevelW / 2, 0f), Offset(bevelW / 2, size.height), bevelW)
                            // Bottom shadow
                            drawLine(sh, Offset(0f, size.height - bevelW / 2), Offset(size.width, size.height - bevelW / 2), bevelW)
                            // Right shadow
                            drawLine(sh, Offset(size.width - bevelW / 2, 0f), Offset(size.width - bevelW / 2, size.height), bevelW)
                        }
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                    BoardShape.ROUNDED -> Modifier
                        .border(2.dp, theme.deviceColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                }

                Box(
                    Modifier
                        .offset(x = maxW * elem.x - bw / 2, y = maxH * elem.y - bh / 2)
                        .size(bw, bh)
                        .alpha(elem.alpha)
                        .then(boardMod)
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
                    val elemShape = ButtonShape.entries.find { it.name == elem.buttonShape } ?: ButtonShape.ROUND
                    RenderElement(type, scale, gs, onRotate, onHardDrop, onHold,
                        onLeftPress, onLeftRelease, onRightPress, onRightRelease,
                        onDownPress, onDownRelease, onPause, onOpenSettings, onStartGame, elemShape, infoBarShape)
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
    FreeformElementType.INFO_BAR_HORIZONTAL -> 280.dp to 36.dp
    FreeformElementType.INFO_BAR_VERTICAL -> 64.dp to 200.dp
    FreeformElementType.INFO_SPLIT_STATS -> 200.dp to 28.dp
    FreeformElementType.INFO_SPLIT_PIECES -> 120.dp to 56.dp
}

/** Render actual game components — used both in gameplay and editor preview */
@Composable
fun RenderElement(
    type: FreeformElementType, scale: Float, gs: GameState,
    onRotate: () -> Unit, onHD: () -> Unit, onHold: () -> Unit,
    onLP: () -> Unit, onLR: () -> Unit, onRP: () -> Unit, onRR: () -> Unit,
    onDP: () -> Unit, onDR: () -> Unit, onPause: () -> Unit,
    onSet: () -> Unit, onStart: () -> Unit,
    elemButtonShape: ButtonShape = ButtonShape.ROUND,
    infoBarShape: InfoBarShape = InfoBarShape.PILL
) {
    val theme = LocalGameTheme.current
    val sz = (50 * scale).dp
    when (type) {
        FreeformElementType.BOARD -> {} // handled separately
        FreeformElementType.DPAD -> DPad(buttonSize = sz, rotateInCenter = false, shapeOverride = elemButtonShape,
            onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
            onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        FreeformElementType.DPAD_ROTATE -> DPad(buttonSize = sz, rotateInCenter = true, shapeOverride = elemButtonShape,
            onUpPress = onHD, onDownPress = onDP, onDownRelease = onDR,
            onLeftPress = onLP, onLeftRelease = onLR, onRightPress = onRP, onRightRelease = onRR, onRotate = onRotate)
        FreeformElementType.BTN_UP -> TapButton(ButtonIcon.UP, sz, shapeOverride = elemButtonShape, onClick = onHD)
        FreeformElementType.BTN_DOWN -> HoldButton(ButtonIcon.DOWN, sz, shapeOverride = elemButtonShape, onPress = onDP, onRelease = onDR)
        FreeformElementType.BTN_LEFT -> HoldButton(ButtonIcon.LEFT, sz, shapeOverride = elemButtonShape, onPress = onLP, onRelease = onLR)
        FreeformElementType.BTN_RIGHT -> HoldButton(ButtonIcon.RIGHT, sz, shapeOverride = elemButtonShape, onPress = onRP, onRelease = onRR)
        FreeformElementType.ROTATE -> RotateButton(onRotate, (64 * scale).dp, shapeOverride = elemButtonShape)
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
        // Compound info bars — full rendering
        FreeformElementType.INFO_BAR_HORIZONTAL -> {
            val shapeMod = infoBarShapeModifier(infoBarShape, theme)
            Box(shapeMod.padding(horizontal = 8.dp, vertical = 3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size((24 * scale).dp))
                    Text(gs.score.toString().padStart(7, '0'), fontSize = (14 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
                    Text("LV ${gs.level}", fontSize = (11 * scale).sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    Text("${gs.lines}L", fontSize = (11 * scale).sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    gs.nextPieces.firstOrNull()?.let { NextPiecePreview(it.shape, Modifier.size((24 * scale).dp)) }
                }
            }
        }
        FreeformElementType.INFO_BAR_VERTICAL -> {
            val shapeMod = infoBarShapeModifier(infoBarShape, theme)
            Box(shapeMod.padding(4.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size((32 * scale).dp))
                    Text(gs.score.toString().padStart(7, '0'), fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
                    Text("LV ${gs.level}", fontSize = (10 * scale).sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    Text("${gs.lines} L", fontSize = (10 * scale).sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    gs.nextPieces.take(3).forEachIndexed { i, p -> NextPiecePreview(p.shape, Modifier.size((if (i == 0) 28 * scale else 20 * scale).dp)) }
                }
            }
        }
        FreeformElementType.INFO_SPLIT_STATS -> {
            val shapeMod = infoBarShapeModifier(infoBarShape, theme)
            Box(shapeMod.padding(horizontal = 8.dp, vertical = 3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(gs.score.toString().padStart(7, '0'), fontSize = (14 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.pixelOn)
                    Text("LV ${gs.level}", fontSize = (11 * scale).sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    Text("${gs.lines}L", fontSize = (11 * scale).sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                }
            }
        }
        FreeformElementType.INFO_SPLIT_PIECES -> {
            val shapeMod = infoBarShapeModifier(infoBarShape, theme)
            Box(shapeMod.padding(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size((28 * scale).dp))
                    gs.nextPieces.take(3).forEachIndexed { i, p -> NextPiecePreview(p.shape, Modifier.size((if (i == 0) 28 * scale else 20 * scale).dp)) }
                }
            }
        }
    }
}

/** Returns a Modifier for the info bar container based on shape variant */
@Composable
private fun infoBarShapeModifier(shape: InfoBarShape, theme: com.brickgame.tetris.ui.theme.GameTheme): Modifier = when (shape) {
    InfoBarShape.PILL -> Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(50))
    InfoBarShape.RECTANGLE -> Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp))
    InfoBarShape.NO_BORDER -> Modifier
    InfoBarShape.FRAMED -> Modifier
        .border(3.dp, theme.deviceColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        .background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp))
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
            else -> RenderElement(type, scale, dummyGs, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop, infoBarShape = InfoBarShape.PILL)
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
    boardShape: BoardShape = BoardShape.STANDARD,
    infoBarType: InfoBarType = InfoBarType.INDIVIDUAL,
    infoBarShape: InfoBarShape = InfoBarShape.PILL,
    onBoardShapeChanged: (BoardShape) -> Unit = {},
    onInfoBarTypeChanged: (InfoBarType) -> Unit = {},
    onInfoBarShapeChanged: (InfoBarShape) -> Unit = {},
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
    var snapEnabled by remember { mutableStateOf(false) }
    var showLabels by remember { mutableStateOf(true) }
    val snapGridDp = 16.dp
    val snapGridPx = with(density) { snapGridDp.toPx() }

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
                    val bh = bs.second * elem.effectiveH
                    val bwPx = with(density) { bw.toPx() }
                    val bhPx = with(density) { bh.toPx() }

                    // Board outline — reflects current board shape
                    val previewShape = when (boardShape) {
                        BoardShape.STANDARD -> RoundedCornerShape(4.dp)
                        BoardShape.FRAMELESS -> RoundedCornerShape(0.dp)
                        BoardShape.DEVICE_FRAME -> RoundedCornerShape(16.dp)
                        BoardShape.BEVELED -> RoundedCornerShape(4.dp)
                        BoardShape.ROUNDED -> RoundedCornerShape(24.dp)
                    }
                    val previewBorderW = when (boardShape) {
                        BoardShape.FRAMELESS -> 0.5.dp
                        BoardShape.DEVICE_FRAME -> 4.dp
                        else -> if (isSelected) 2.dp else 1.dp
                    }
                    Box(
                        Modifier
                            .offset(x = maxWidth * elem.x - bw / 2, y = maxHeight * elem.y - bh / 2)
                            .size(bw, bh)
                            .border(
                                width = previewBorderW,
                                color = if (isSelected) Color(0xFF22C55E) else when (boardShape) {
                                    BoardShape.FRAMELESS -> Color.White.copy(0.15f)
                                    BoardShape.DEVICE_FRAME -> Color.White.copy(0.6f)
                                    else -> Color.White.copy(0.4f)
                                },
                                shape = previewShape
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
                        showLabels = showLabels,
                        maxWidthPx = maxWidthPx,
                        maxHeightPx = maxHeightPx,
                        elemWidthPx = handleSizePx,
                        elemHeightPx = handleSizePx,
                        isSelected = isSelected,
                        onTap = { selectedKey = if (selectedKey == elem.key) null else elem.key },
                        onDragEnd = { newX, newY -> onElementUpdated(elem.copy(x = newX, y = newY)) },
                        snapEnabled = snapEnabled,
                        snapGridPx = snapGridPx,
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
                    // Normal elements — use actual width/height for centering (matches game layout)
                    val baseSize = elementBaseSize(type)
                    val elemW = baseSize.first * scale
                    val elemH = baseSize.second * scale
                    val elemWPx = with(density) { elemW.toPx() }
                    val elemHPx = with(density) { elemH.toPx() }

                    DraggableRealElement(
                        key = elem.key,
                        label = type.displayName,
                        position = elem,
                        type = type,
                        scale = scale,
                        showLabels = showLabels,
                        maxWidthPx = maxWidthPx,
                        maxHeightPx = maxHeightPx,
                        elemWidthPx = elemWPx,
                        elemHeightPx = elemHPx,
                        isSelected = isSelected,
                        onTap = { selectedKey = if (selectedKey == elem.key) null else elem.key },
                        onDragEnd = { newX, newY -> onElementUpdated(elem.copy(x = newX, y = newY)) },
                        snapEnabled = snapEnabled,
                        snapGridPx = snapGridPx
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

        // Bottom/Top panel for selected element properties
        // Flips position: if element is in lower half → show at top, else → show at bottom
        if (selectedElement != null && !showMenu) {
            // Tap outside panel to deselect
            Box(Modifier.fillMaxSize().clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { selectedKey = null })
            val selType = FreeformElementType.fromKey(selectedElement.key)
            val selKey = selectedElement.key
            val panelOnTop = selectedElement.y > 0.5f
            Surface(
                modifier = Modifier
                    .align(if (panelOnTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(if (panelOnTop) Modifier.statusBarsPadding() else Modifier.navigationBarsPadding()),
                shape = if (panelOnTop) RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color(0xFF1A1A1A).copy(0.75f),
                shadowElevation = 0.dp
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Header: element name + close/remove
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(Color(0xFF22C55E), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(selType?.displayName ?: selKey, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row {
                            Text("Remove", color = Color(0xFFFF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    onElementRemoved(selKey); selectedKey = null
                                }.padding(horizontal = 8.dp, vertical = 4.dp))
                            Text("✕", color = Color(0xFF888888), fontSize = 16.sp,
                                modifier = Modifier.clickable { selectedKey = null }.padding(4.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Position: H and V
                    PositionRow("H", (selectedElement.x * 100).toInt()) { newPct ->
                        val fresh = elements[selKey] ?: return@PositionRow
                        onElementUpdated(fresh.copy(x = (newPct / 100f).coerceIn(0f, 1f)))
                    }
                    PositionRow("V", (selectedElement.y * 100).toInt()) { newPct ->
                        val fresh = elements[selKey] ?: return@PositionRow
                        onElementUpdated(fresh.copy(y = (newPct / 100f).coerceIn(0f, 1f)))
                    }
                    Spacer(Modifier.height(4.dp))
                    // Size and Opacity sliders
                    SliderRow(if (selKey == "BOARD") "Width" else "Size", selectedElement.size, 0.4f, 2.0f) { newSize ->
                        val fresh = elements[selKey] ?: return@SliderRow
                        onElementUpdated(fresh.copy(size = newSize))
                    }
                    if (selKey == "BOARD") {
                        SliderRow("Height", selectedElement.effectiveH, 0.4f, 2.5f) { newH ->
                            val fresh = elements[selKey] ?: return@SliderRow
                            onElementUpdated(fresh.copy(sizeH = newH))
                        }
                    }
                    SliderRow("Opacity", selectedElement.alpha, 0.05f, 1.0f) { newAlpha ->
                        val fresh = elements[selKey] ?: return@SliderRow
                        onElementUpdated(fresh.copy(alpha = newAlpha))
                    }
                }
            }
        }

        // Scrim when drawer is open
        if (showMenu) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)).clickable { showMenu = false })
        }

        // Side Drawer — slides from right (40% width)
        AnimatedVisibility(
            visible = showMenu,
            enter = slideInHorizontally(animationSpec = tween(250)) { it },
            exit = slideOutHorizontally(animationSpec = tween(250)) { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.42f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                color = Color(0xFF1A1A1A).copy(0.95f),
                shadowElevation = 8.dp
            ) {
                Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Freeform", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("✕", color = Color(0xFF888888), fontSize = 20.sp,
                            modifier = Modifier.clickable { showMenu = false }.padding(4.dp))
                    }
                    Spacer(Modifier.height(6.dp))

                    LazyColumn(Modifier.weight(1f)) {
                        // ── Shape picker for selected element ──
                        if (selectedElement != null) {
                            val selType = FreeformElementType.fromKey(selectedElement.key)
                            val isControl = selType?.category == ElementCategory.CONTROL
                            if (isControl) {
                                item {
                                    Text("Button Shape", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    val current = ButtonShape.entries.find { it.name == selectedElement.buttonShape } ?: ButtonShape.ROUND
                                    ButtonShape.entries.forEach { shape ->
                                        val isSel = shape == current
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFF22C55E).copy(0.15f) else Color(0xFF252525))
                                                .clickable {
                                                    val fresh = elements[selectedElement.key] ?: return@clickable
                                                    onElementUpdated(fresh.copy(buttonShape = shape.name))
                                                }
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            Arrangement.SpaceBetween, Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(shape.displayName, color = if (isSel) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                                Text(shape.description, color = Color(0xFF666666), fontSize = 10.sp)
                                            }
                                            if (isSel) Text("✓", color = Color(0xFF22C55E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }

                        // ── Board shape picker (when board selected) ──
                        if (selectedElement != null && selectedElement.key == "BOARD") {
                            item {
                                Text("Board Shape", color = Color(0xFF8B5CF6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                BoardShape.entries.forEach { shape ->
                                    val isSel = shape == boardShape
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF8B5CF6).copy(0.15f) else Color(0xFF252525))
                                            .clickable { onBoardShapeChanged(shape) }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        Arrangement.SpaceBetween, Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(shape.displayName, color = if (isSel) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                            Text(shape.description, color = Color(0xFF666666), fontSize = 10.sp)
                                        }
                                        if (isSel) Text("✓", color = Color(0xFF8B5CF6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }

                        // ── Info Bar Shape picker (when compound info bar selected) ──
                        if (selectedElement != null) {
                            val selType = FreeformElementType.fromKey(selectedElement.key)
                            val isCompoundInfo = selType != null && selType.key.startsWith("INFO_")
                            if (isCompoundInfo) {
                                item {
                                    Text("Info Bar Shape", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    InfoBarShape.entries.forEach { shape ->
                                        val isSel = shape == infoBarShape
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFFF59E0B).copy(0.15f) else Color(0xFF252525))
                                                .clickable { onInfoBarShapeChanged(shape) }
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            Arrangement.SpaceBetween, Alignment.CenterVertically
                                        ) {
                                            Text(shape.displayName, color = if (isSel) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                            if (isSel) Text("✓", color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }

                        // ── Info Bar Type switcher ──
                        item {
                            Text("Info Bar Type", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            InfoBarType.entries.forEach { type ->
                                val isSel = type == infoBarType
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFFF59E0B).copy(0.15f) else Color(0xFF252525))
                                        .clickable { onInfoBarTypeChanged(type) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    Arrangement.SpaceBetween, Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(type.displayName, color = if (isSel) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                        Text(type.description, color = Color(0xFF666666), fontSize = 10.sp)
                                    }
                                    if (isSel) Text("✓", color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // ── Board Shape (when BOARD selected) ──
                        if (selectedElement != null && selectedElement.key == "BOARD") {
                            item {
                                Text("Board Shape", color = Color(0xFF8B5CF6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                BoardShape.entries.forEach { shape ->
                                    val isSel = shape == boardShape
                                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF8B5CF6).copy(0.15f) else Color(0xFF252525))
                                        .clickable { onBoardShapeChanged(shape) }.padding(horizontal = 10.dp, vertical = 7.dp),
                                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Column { Text(shape.displayName, color = if (isSel) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal); Text(shape.description, color = Color(0xFF666666), fontSize = 10.sp) }
                                        if (isSel) Text("✓", color = Color(0xFF8B5CF6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }

                        // ── Info Bar Shape (when compound info selected) ──
                        if (selectedElement != null && selectedElement.key.startsWith("INFO_")) {
                            item {
                                Text("Info Bar Shape", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                InfoBarShape.entries.forEach { shape ->
                                    val isSel = shape == infoBarShape
                                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFFF59E0B).copy(0.15f) else Color(0xFF252525))
                                        .clickable { onInfoBarShapeChanged(shape) }.padding(horizontal = 10.dp, vertical = 7.dp),
                                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(shape.displayName, color = if (isSel) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                        if (isSel) Text("✓", color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }

                        // ── Info Bar Type ──
                        item {
                            Text("Info Bar Type", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            InfoBarType.entries.forEach { t ->
                                val isSel = t == infoBarType
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFFF59E0B).copy(0.15f) else Color(0xFF252525))
                                    .clickable { onInfoBarTypeChanged(t) }.padding(horizontal = 10.dp, vertical = 7.dp),
                                    Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column { Text(t.displayName, color = if (isSel) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal); Text(t.description, color = Color(0xFF666666), fontSize = 10.sp) }
                                    if (isSel) Text("✓", color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // ── Labels & Snap toggles ──
                        item {
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF222222))
                                .clickable { showLabels = !showLabels }.padding(horizontal = 10.dp, vertical = 7.dp),
                                Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Show Labels", color = Color.White, fontSize = 12.sp)
                                Text(if (showLabels) "ON" else "OFF", color = if (showLabels) Color(0xFF22C55E) else Color(0xFF888888),
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF222222))
                                .clickable { snapEnabled = !snapEnabled }.padding(horizontal = 10.dp, vertical = 7.dp),
                                Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Snap Grid", color = Color.White, fontSize = 12.sp)
                                Text(if (snapEnabled) "ON" else "OFF", color = if (snapEnabled) Color(0xFF22C55E) else Color(0xFF888888),
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // ── LCD & Info section ──
                        item { Text("LCD & Info", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)) }
                        val infoTypes = listOf(FreeformElementType.BOARD, FreeformElementType.SCORE, FreeformElementType.LEVEL,
                            FreeformElementType.LINES, FreeformElementType.HOLD_PREVIEW, FreeformElementType.NEXT_PREVIEW,
                            FreeformElementType.INFO_BAR_HORIZONTAL, FreeformElementType.INFO_BAR_VERTICAL,
                            FreeformElementType.INFO_SPLIT_STATS, FreeformElementType.INFO_SPLIT_PIECES)
                        items(infoTypes.size) { i ->
                            val t = infoTypes[i]; val el = elements[t.key]
                            val isOn = el != null && el.visible
                            ElementToggleRow(t.displayName, isOn) {
                                if (el == null) { onElementAdded(FreeformElement(t.key, 0.5f, 0.5f)); selectedKey = t.key }
                                else onElementUpdated(el.copy(visible = !el.visible))
                            }
                        }

                        // ── Buttons section ──
                        item { Spacer(Modifier.height(8.dp)); Text("Buttons", color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)) }
                        val ctrlTypes = FreeformElementType.entries.filter { it.category == ElementCategory.CONTROL }
                        items(ctrlTypes.size) { i ->
                            val t = ctrlTypes[i]; val el = elements[t.key]
                            val isOn = el != null && el.visible
                            ElementToggleRow(t.displayName, isOn) {
                                if (el == null) { onElementAdded(FreeformElement(t.key, 0.5f, 0.5f)); selectedKey = t.key }
                                else onElementUpdated(el.copy(visible = !el.visible))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        OutlinedButton(onClick = { selectedKey = null; onReset(); showMenu = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                            Text("↺ Reset All")
                        }
                    }
                }
            }
        }

        // Edge tab to open drawer (when drawer is closed)
        if (!showMenu) {
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .offset(x = (-2).dp)
                    .width(20.dp).height(56.dp)
                    .clickable { showMenu = true },
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                color = Color(0xFF3B82F6).copy(0.7f)
            ) {
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    repeat(3) {
                        Box(Modifier.width(10.dp).height(2.dp).background(Color.White.copy(0.7f), RoundedCornerShape(1.dp)))
                        if (it < 2) Spacer(Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}

/** Draggable element that renders the REAL button/component preview.
 *  5A: Separate tap detection for immediate selection (green outline on first tap)
 *  5B: graphicsLayer for smooth drag rendering (no layout thrashing)
 *  5C: Snap-to-grid support during drag
 *  5D: Fixed labels — centered, auto-hide when element too small
 */
@Composable
private fun BoxWithConstraintsScope.DraggableRealElement(
    key: String,
    label: String,
    position: FreeformElement,
    type: FreeformElementType,
    scale: Float,
    maxWidthPx: Float,
    maxHeightPx: Float,
    elemWidthPx: Float,
    elemHeightPx: Float,
    isSelected: Boolean,
    onTap: () -> Unit,
    onDragEnd: (Float, Float) -> Unit,
    snapEnabled: Boolean = false,
    snapGridPx: Float = 0f,
    showLabels: Boolean = true,
    customContent: (@Composable () -> Unit)? = null
) {
    // Committed position (stored state) — center using actual w/h
    val committedX = position.x * maxWidthPx - elemWidthPx / 2
    val committedY = position.y * maxHeightPx - elemHeightPx / 2

    // Transient drag delta (only during active drag)
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Reset drag delta when position changes externally
    LaunchedEffect(position.x, position.y, elemWidthPx, elemHeightPx) {
        if (!isDragging) { dragDeltaX = 0f; dragDeltaY = 0f }
    }

    val currentElemWPx by rememberUpdatedState(elemWidthPx)
    val currentElemHPx by rememberUpdatedState(elemHeightPx)

    // Snap helper
    fun snap(v: Float): Float = if (snapEnabled && snapGridPx > 0f) {
        (v / snapGridPx).roundToInt() * snapGridPx
    } else v

    Box(
        modifier = Modifier
            .offset { IntOffset(committedX.roundToInt(), committedY.roundToInt()) }
            // 5B: Use graphicsLayer for drag delta — GPU-accelerated, no recomposition
            .graphicsLayer {
                translationX = if (isDragging) snap(dragDeltaX) else 0f
                translationY = if (isDragging) snap(dragDeltaY) else 0f
            }
            // Tap via clickable (always fires reliably, no gesture conflict)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onTap() }
            // Drag detection
            .pointerInput(key, elemWidthPx, elemHeightPx) {
                detectDragGestures(
                    onDragStart = { isDragging = true; onTap() },
                    onDragEnd = {
                        val w = currentElemWPx; val h = currentElemHPx
                        val finalX = snap(dragDeltaX)
                        val finalY = snap(dragDeltaY)
                        val newAbsX = committedX + finalX
                        val newAbsY = committedY + finalY
                        val cx = (newAbsX + w / 2).coerceIn(0f, maxWidthPx) / maxWidthPx
                        val cy = (newAbsY + h / 2).coerceIn(0f, maxHeightPx) / maxHeightPx
                        isDragging = false; dragDeltaX = 0f; dragDeltaY = 0f
                        onDragEnd(cx.coerceIn(0.03f, 0.97f), cy.coerceIn(0.03f, 0.97f))
                    },
                    onDragCancel = { isDragging = false; dragDeltaX = 0f; dragDeltaY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = currentElemWPx; val h = currentElemHPx
                        dragDeltaX = (dragDeltaX + dragAmount.x).coerceIn(-committedX, maxWidthPx - w - committedX)
                        dragDeltaY = (dragDeltaY + dragAmount.y).coerceIn(-committedY, maxHeightPx - h - committedY)
                    }
                )
            }
            .alpha(position.alpha)
    ) {
        // Selection highlight border (5A: shows immediately on tap)
        if (isSelected && customContent == null) {
            Box(Modifier.matchParentSize().border(2.dp, Color(0xFF22C55E), RoundedCornerShape(8.dp)))
        }
        if (customContent != null) {
            customContent()
        } else {
            RenderEditorPreview(type, scale)
        }
        // 5D: Labels — togglable, never clipped by parent size
        if (customContent == null && showLabels) {
            Text(label, fontSize = 10.sp, color = Color.White.copy(0.85f),
                textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 14.dp)
                    .wrapContentWidth(unbounded = true)
                    .background(Color.Black.copy(0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp))
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
private fun PositionRow(label: String, valuePct: Int, onChange: (Int) -> Unit) {
    val clamped = valuePct.coerceIn(0, 100)
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp))
        // − button
        Surface(
            modifier = Modifier.size(32.dp).clickable { if (clamped > 0) onChange(clamped - 1) },
            shape = RoundedCornerShape(6.dp),
            color = if (clamped > 0) Color(0xFF333333) else Color(0xFF222222)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("−", color = if (clamped > 0) Color(0xFF22C55E) else Color(0xFF555555),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Slider for quick large jumps
        Slider(value = clamped / 100f, onValueChange = { onChange((it * 100).toInt()) },
            modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E),
                inactiveTrackColor = Color(0xFF22C55E).copy(0.15f)))
        // Value display
        Text("${clamped}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End, modifier = Modifier.width(32.dp))
        Spacer(Modifier.width(4.dp))
        // + button
        Surface(
            modifier = Modifier.size(32.dp).clickable { if (clamped < 100) onChange(clamped + 1) },
            shape = RoundedCornerShape(6.dp),
            color = if (clamped < 100) Color(0xFF333333) else Color(0xFF222222)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("+", color = if (clamped < 100) Color(0xFF22C55E) else Color(0xFF555555),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ElementToggleRow(name: String, isOn: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(6.dp))
        .background(Color(0xFF252525)).clickable { onToggle() }
        .padding(horizontal = 10.dp, vertical = 8.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(name, color = if (isOn) Color.White else Color(0xFF666666), fontSize = 12.sp)
        Text(if (isOn) "ON" else "OFF",
            color = if (isOn) Color(0xFF22C55E) else Color(0xFF555555),
            fontSize = 11.sp, fontWeight = FontWeight.Bold)
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


// Color helpers for board shapes
private fun Color.lighten(f: Float) = Color(
    (red + (1 - red) * f).coerceIn(0f, 1f),
    (green + (1 - green) * f).coerceIn(0f, 1f),
    (blue + (1 - blue) * f).coerceIn(0f, 1f), alpha
)
private fun Color.darken(f: Float) = Color(
    (red * (1 - f)).coerceIn(0f, 1f),
    (green * (1 - f)).coerceIn(0f, 1f),
    (blue * (1 - f)).coerceIn(0f, 1f), alpha
)
