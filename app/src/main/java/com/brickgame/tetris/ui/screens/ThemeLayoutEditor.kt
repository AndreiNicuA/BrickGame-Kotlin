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
// LAYOUT EDITOR v7 — REAL GAME SIZES
// Elements render at their actual game dimensions.
// Drag positions map 1:1 to CustomLayout in GameScreen.
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

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // HEADER: ← [name] SAVE ☰
            Row(Modifier.fillMaxWidth().background(PBG).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = ACC, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(6.dp))
                BasicTextField(layoutName, { layoutName = it; onUpdateLayout(layout.copy(name = it)) },
                    textStyle = TextStyle(color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(ACC), singleLine = true,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF252525)).padding(horizontal = 10.dp, vertical = 7.dp),
                    decorationBox = { inner -> if (layoutName.isEmpty()) Text("Layout name...", color = DIM, fontSize = 14.sp, fontFamily = FontFamily.Monospace); inner() })
                Spacer(Modifier.width(8.dp))
                Text("SAVE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ACC).clickable { onSave() }.padding(horizontal = 12.dp, vertical = 7.dp))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(if (showSideMenu) ACC.copy(0.2f) else Color(0xFF333333)).clickable { showSideMenu = !showSideMenu; selectedElement = null }, Alignment.Center) { Text("☰", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }

            // CANVAS — full screen drag area
            BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { selectedElement = null; showSideMenu = false } }) {
                val density = LocalDensity.current
                val maxW = maxWidth; val maxH = maxHeight
                val maxWPx = with(density) { maxW.toPx() }
                val maxHPx = with(density) { maxH.toPx() }

                // Grid overlay
                if (gridSnap) Canvas(Modifier.fillMaxSize()) {
                    for (i in 1 until (1f / gridStepX).toInt()) { val x = size.width * i * gridStepX; drawLine(Color.White.copy(0.06f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f) }
                    for (i in 1 until (1f / gridStepY).toInt()) { val y = size.height * i * gridStepY; drawLine(Color.White.copy(0.06f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f) }
                }

                // REAL GAME ELEMENT SIZES — must match CustomLayout in GameScreen
                val boardW = maxW * 0.85f; val boardH = maxH * 0.6f
                val dpadBtnSz = when (layout.sizeFor(LayoutElements.DPAD)) { "SMALL" -> 44.dp; "LARGE" -> 62.dp; else -> 54.dp }
                val dpadArea = 140.dp // overall dpad area
                val rotSz = when (layout.sizeFor(LayoutElements.ROTATE_BTN)) { "SMALL" -> 52.dp; "LARGE" -> 74.dp; else -> 66.dp }

                val vis = layout.visibility
                val pos = layout.positions

                // == BOARD ==
                if (vis.getOrDefault(LayoutElements.BOARD, true)) {
                    val elem = LayoutElements.BOARD
                    val ep = pos[elem] ?: ElementPosition(0.5f, 0.38f)
                    // Game offset: x = maxW * bp.x - maxW*0.425, y = maxH * bp.y - maxH*0.3
                    val offX = maxW * ep.x - boardW / 2; val offY = maxH * ep.y - boardH / 2
                    DragHandle(elem, ep, boardW, boardH, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        // Preview board
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(theme.screenBackground)) {
                            Column(Modifier.fillMaxSize().padding(4.dp), Arrangement.SpaceEvenly) {
                                repeat(10) { r -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { repeat(10) { c ->
                                    val on = (r == 3 && c in 4..6) || (r == 4 && c == 5) || (r >= 8)
                                    Box(Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp).clip(RoundedCornerShape(1.dp)).background(if (on) theme.pixelOn else theme.pixelOff))
                                } } }
                            }
                        }
                    }
                }

                // == SCORE ==
                if (vis.getOrDefault(LayoutElements.SCORE, true)) {
                    val elem = LayoutElements.SCORE
                    val ep = pos[elem] ?: ElementPosition(0.5f, 0.02f)
                    val txtW = 100.dp; val txtH = 20.dp
                    val offX = maxW * ep.x - 40.dp; val offY = maxH * ep.y
                    DragHandle(elem, ep, txtW, txtH, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Text("0001234", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                    }
                }

                // == LEVEL ==
                if (vis.getOrDefault(LayoutElements.LEVEL, true)) {
                    val elem = LayoutElements.LEVEL; val ep = pos[elem] ?: ElementPosition(0.2f, 0.02f)
                    val offX = maxW * ep.x - 16.dp; val offY = maxH * ep.y
                    DragHandle(elem, ep, 40.dp, 16.dp, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Text("LV1", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // == LINES ==
                if (vis.getOrDefault(LayoutElements.LINES, true)) {
                    val elem = LayoutElements.LINES; val ep = pos[elem] ?: ElementPosition(0.8f, 0.02f)
                    val offX = maxW * ep.x - 16.dp; val offY = maxH * ep.y
                    DragHandle(elem, ep, 32.dp, 16.dp, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Text("0L", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // == HOLD PREVIEW ==
                if (vis.getOrDefault(LayoutElements.HOLD_PREVIEW, true)) {
                    val elem = LayoutElements.HOLD_PREVIEW; val ep = pos[elem] ?: ElementPosition(0.08f, 0.08f)
                    val offX = maxW * ep.x - 24.dp; val offY = maxH * ep.y - 12.dp
                    DragHandle(elem, ep, 48.dp, 56.dp, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HOLD", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground.copy(0.3f)))
                        }
                    }
                }

                // == NEXT PREVIEW ==
                if (vis.getOrDefault(LayoutElements.NEXT_PREVIEW, true)) {
                    val elem = LayoutElements.NEXT_PREVIEW; val ep = pos[elem] ?: ElementPosition(0.92f, 0.08f)
                    val offX = maxW * ep.x - 24.dp; val offY = maxH * ep.y - 12.dp
                    val queueH = 34.dp + 28.dp * (layout.nextQueueSize - 1)
                    DragHandle(elem, ep, 48.dp, 16.dp + queueH, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NEXT", fontSize = 9.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            repeat(layout.nextQueueSize) { i ->
                                Box(Modifier.size(if (i == 0) 34.dp else 24.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground.copy(if (i == 0) 0.3f else 0.15f)))
                            }
                        }
                    }
                }

                // == DPAD ==
                if (vis.getOrDefault(LayoutElements.DPAD, true)) {
                    val elem = LayoutElements.DPAD; val ep = pos[elem] ?: ElementPosition(0.18f, 0.85f)
                    val offX = maxW * ep.x - 70.dp; val offY = maxH * ep.y - 70.dp
                    DragHandle(elem, ep, dpadArea, dpadArea, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        val bSz = dpadBtnSz
                        Box(Modifier.size(dpadArea), Alignment.Center) {
                            Box(Modifier.size(bSz).align(Alignment.TopCenter).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("▲", color = theme.buttonSecondary, fontSize = 14.sp) }
                            Box(Modifier.size(bSz).align(Alignment.BottomCenter).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("▼", color = theme.buttonSecondary, fontSize = 14.sp) }
                            Box(Modifier.size(bSz).align(Alignment.CenterStart).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("◄", color = theme.buttonSecondary, fontSize = 14.sp) }
                            Box(Modifier.size(bSz).align(Alignment.CenterEnd).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("►", color = theme.buttonSecondary, fontSize = 14.sp) }
                            Box(Modifier.size(bSz * 0.6f).clip(CircleShape).background(theme.buttonPrimaryPressed))
                        }
                    }
                }

                // == ROTATE BTN ==
                if (vis.getOrDefault(LayoutElements.ROTATE_BTN, true)) {
                    val elem = LayoutElements.ROTATE_BTN; val ep = pos[elem] ?: ElementPosition(0.85f, 0.85f)
                    val offX = maxW * ep.x - rotSz / 2; val offY = maxH * ep.y - rotSz / 2
                    DragHandle(elem, ep, rotSz, rotSz, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Box(Modifier.fillMaxSize().shadow(6.dp, CircleShape).clip(CircleShape).background(theme.buttonPrimary), Alignment.Center) { Text("↻", color = theme.buttonSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // == HOLD BTN ==
                if (vis.getOrDefault(LayoutElements.HOLD_BTN, true)) {
                    val elem = LayoutElements.HOLD_BTN; val ep = pos[elem] ?: ElementPosition(0.5f, 0.80f)
                    val offX = maxW * ep.x - 39.dp; val offY = maxH * ep.y - 17.dp
                    DragHandle(elem, ep, 78.dp, 34.dp, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)).background(theme.accentColor.copy(0.8f)), Alignment.Center) { Text("HOLD", color = theme.backgroundColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // == PAUSE BTN ==
                if (vis.getOrDefault(LayoutElements.PAUSE_BTN, true)) {
                    val elem = LayoutElements.PAUSE_BTN; val ep = pos[elem] ?: ElementPosition(0.5f, 0.87f)
                    val offX = maxW * ep.x - 39.dp; val offY = maxH * ep.y - 17.dp
                    DragHandle(elem, ep, 78.dp, 34.dp, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)).background(theme.accentColor.copy(0.8f)), Alignment.Center) { Text("PAUSE", color = theme.backgroundColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // == MENU BTN ==
                run {
                    val elem = LayoutElements.MENU_BTN; val ep = pos[elem] ?: ElementPosition(0.5f, 0.94f)
                    val offX = maxW * ep.x - 23.dp; val offY = maxH * ep.y - 12.dp
                    DragHandle(elem, ep, 46.dp, 24.dp, offX, offY, maxWPx, maxHPx, maxW, maxH,
                        selectedElement == elem, { selectedElement = if (selectedElement == it) null else it },
                        { id, newEp -> withUndo(layout.copy(positions = pos + (id to snap(newEp)))) }) {
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(theme.accentColor), Alignment.Center) { Text("···", color = theme.backgroundColor, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // == SETTINGS CARD for selected element ==
                if (selectedElement != null) {
                    val elem = selectedElement!!
                    val ePos = pos[elem] ?: CustomLayoutData.defaultPositions()[elem] ?: ElementPosition(0.5f, 0.5f)
                    // Position card below or above element
                    val cardBelow = ePos.y < 0.6f
                    val cardXDp = with(density) { (ePos.x * maxWPx).toDp() - 85.dp }.coerceIn(4.dp, maxW - 174.dp)
                    val cardYDp = if (cardBelow) with(density) { (ePos.y * maxHPx).toDp() + 40.dp }
                                  else with(density) { (ePos.y * maxHPx).toDp() - 180.dp }
                    val adjY = cardYDp.coerceIn(4.dp, maxH - 200.dp)

                    Column(Modifier.offset(x = cardXDp, y = adjY).width(170.dp).zIndex(300f)
                        .clip(RoundedCornerShape(12.dp)).shadow(16.dp, RoundedCornerShape(12.dp))
                        .background(Color(0xF0181818)).border(1.dp, ACC.copy(0.4f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) { detectTapGestures { /* consume tap */ } }
                        .padding(10.dp)) {

                        Text(elem.replace("_", " "), color = ACC, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))

                        // Hide
                        if (elem != LayoutElements.MENU_BTN) {
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A1515))
                                .clickable { withUndo(layout.copy(visibility = vis + (elem to false))); selectedElement = null }
                                .padding(horizontal = 8.dp, vertical = 5.dp)) { Text("👁 Hide", color = Color(0xFFFF6B6B), fontSize = 11.sp) }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Per-element size
                        if (elem in listOf(LayoutElements.DPAD, LayoutElements.ROTATE_BTN, LayoutElements.HOLD_BTN, LayoutElements.PAUSE_BTN)) {
                            Text("Size", color = DIM, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                listOf("SMALL" to "S", "MEDIUM" to "M", "LARGE" to "L").forEach { (k, v) ->
                                    val sel = layout.sizeFor(elem) == k
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if (sel) ACC.copy(0.25f) else Color(0xFF333333))
                                        .clickable { withUndo(layout.copy(elementSizes = layout.elementSizes + (elem to k))) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(v, color = if (sel) ACC else DIM, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Per-element style
                        if (elem in listOf(LayoutElements.DPAD, LayoutElements.ROTATE_BTN, LayoutElements.HOLD_BTN, LayoutElements.PAUSE_BTN)) {
                            Text("Style", color = DIM, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                listOf("ROUND" to "◯", "SQUARE" to "◻").forEach { (k, v) ->
                                    val sel = layout.styleFor(elem) == k
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if (sel) ACC.copy(0.25f) else Color(0xFF333333))
                                        .clickable { withUndo(layout.copy(elementStyles = layout.elementStyles + (elem to k))) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(v, color = if (sel) ACC else DIM, fontSize = 12.sp)
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
                                        .clickable { withUndo(layout.copy(nextQueueSize = n)) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("$n", color = if (sel) ACC else DIM, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // == SIDE MENU ==
        AnimatedVisibility(showSideMenu, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)) {
            Column(Modifier.width(190.dp).fillMaxHeight().background(Color(0xEE111111))
                .pointerInput(Unit) { detectTapGestures { } }.padding(14.dp), Arrangement.spacedBy(6.dp)) {
                Text("Layout Tools", color = ACC, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))

                // Undo / Redo
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(if (undoStack.isNotEmpty()) Color(0xFF2A2A3A) else CBG)
                        .clickable(enabled = undoStack.isNotEmpty()) { undo() }.padding(8.dp), Alignment.Center) { Text("↶ Undo", color = if (undoStack.isNotEmpty()) Color(0xFF8AB4F8) else DIM.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(if (redoStack.isNotEmpty()) Color(0xFF2A2A3A) else CBG)
                        .clickable(enabled = redoStack.isNotEmpty()) { redo() }.padding(8.dp), Alignment.Center) { Text("↷ Redo", color = if (redoStack.isNotEmpty()) Color(0xFF8AB4F8) else DIM.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }

                // Grid snap
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (gridSnap) ACC.copy(0.15f) else CBG).clickable { gridSnap = !gridSnap }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (gridSnap) "⊞" else "⊟", fontSize = 14.sp, color = if (gridSnap) ACC else DIM); Spacer(Modifier.width(6.dp)); Text("Grid Snap", color = if (gridSnap) ACC else TX, fontSize = 11.sp) }

                // Info mode
                Text("Info Layout", color = DIM, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                listOf("SCATTERED" to "Scattered", "PANEL" to "Side Panel", "INTEGRATED" to "Integrated").forEach { (k, v) ->
                    val sel = layout.infoMode == k
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (sel) ACC.copy(0.15f) else CBG)
                        .clickable { withUndo(layout.copy(infoMode = k)) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (sel) "◉" else "○", color = if (sel) ACC else DIM, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(v, color = if (sel) ACC else TX, fontSize = 11.sp)
                    }
                }

                // Hidden elements
                val hidden = LayoutElements.hideable.filter { !(layout.visibility.getOrDefault(it, true)) }
                if (hidden.isNotEmpty()) { Text("Hidden", color = DIM, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    hidden.forEach { elem -> val lbl = elem.replace("_PREVIEW", "").replace("_BTN", "").lowercase().replaceFirstChar { c -> c.uppercase() }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CBG).clickable { withUndo(layout.copy(visibility = layout.visibility + (elem to true))) }.padding(6.dp)) { Text("👁 Show $lbl", color = DRG, fontSize = 10.sp) } } }

                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF4A1A1A)).clickable {
                    withUndo(layout.copy(positions = CustomLayoutData.defaultPositions(), visibility = LayoutElements.allElements.associateWith { true }, controlSize = "MEDIUM", elementSizes = emptyMap(), elementStyles = emptyMap(), nextQueueSize = 1, infoMode = "SCATTERED"))
                    showSideMenu = false
                }.padding(8.dp)) { Text("↺ Reset All", color = Color(0xFFFF6B6B), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ======================================================================
// DRAG HANDLE — wraps any element with drag + tap behavior.
// Uses delta-based approach: base position derived from ElementPosition,
// drag adds temporary delta. On drag end, saves new normalized position.
// ======================================================================
@Composable
private fun BoxWithConstraintsScope.DragHandle(
    elementId: String, position: ElementPosition,
    itemWidth: Dp, itemHeight: Dp,
    offsetX: Dp, offsetY: Dp,
    maxWPx: Float, maxHPx: Float, maxW: Dp, maxH: Dp,
    isSelected: Boolean,
    onTap: (String) -> Unit,
    onDragEnd: (String, ElementPosition) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val baseXPx = with(density) { offsetX.toPx() }
    val baseYPx = with(density) { offsetY.toPx() }
    val wPx = with(density) { itemWidth.toPx() }
    val hPx = with(density) { itemHeight.toPx() }

    var dragDX by remember { mutableStateOf(0f) }
    var dragDY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var wasDragged by remember { mutableStateOf(false) }

    // Reset drag delta when position changes externally (undo, other element)
    LaunchedEffect(position.x, position.y) { if (!isDragging) { dragDX = 0f; dragDY = 0f } }

    val finalXPx = baseXPx + dragDX
    val finalYPx = baseYPx + dragDY

    Box(
        Modifier
            .offset { IntOffset(finalXPx.roundToInt(), finalYPx.roundToInt()) }
            .size(itemWidth, itemHeight)
            .zIndex(if (isDragging) 100f else if (isSelected) 50f else 1f)
            .pointerInput(elementId) {
                detectDragGestures(
                    onDragStart = { isDragging = true; wasDragged = false; dragDX = 0f; dragDY = 0f },
                    onDragEnd = {
                        isDragging = false
                        if (wasDragged) {
                            // Reverse-engineer the normalized position from the final pixel offset
                            // The offset formula varies per element, so we compute the CENTER of the dragged element
                            // and normalize. The receiving code in CustomLayout uses element-specific offset formulas,
                            // but they all place the element such that:
                            //   pixelX = maxW * normX + elementSpecificOffset
                            // We stored the original baseXPx = offsetX.toPx() which IS that formula.
                            // So the new normX is whatever value makes: newBaseXPx = baseXPx + dragDX
                            // Since baseXPx = f(normX), and f is linear, we can compute:
                            //   normX_new = normX_old + dragDX / maxWPx
                            val newNormX = (position.x + dragDX / maxWPx).coerceIn(0.02f, 0.98f)
                            val newNormY = (position.y + dragDY / maxHPx).coerceIn(0.02f, 0.98f)
                            dragDX = 0f; dragDY = 0f
                            onDragEnd(elementId, ElementPosition(newNormX, newNormY))
                        } else { dragDX = 0f; dragDY = 0f }
                    },
                    onDragCancel = { isDragging = false; dragDX = 0f; dragDY = 0f },
                    onDrag = { change, amt ->
                        change.consume()
                        wasDragged = true
                        dragDX += amt.x
                        dragDY += amt.y
                    }
                )
            }
            .pointerInput(elementId) { detectTapGestures { onTap(elementId) } }
            .then(if (isDragging) Modifier.border(2.dp, DRG, RoundedCornerShape(4.dp))
                  else if (isSelected) Modifier.border(2.dp, ACC, RoundedCornerShape(4.dp))
                  else Modifier.border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(4.dp)))
    ) {
        content()
    }
}

// ======================================================================
// THEME EDITOR (unchanged from v6)
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
