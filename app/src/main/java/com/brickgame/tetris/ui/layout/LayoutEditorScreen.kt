package com.brickgame.tetris.ui.layout

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ACCENT = Color(0xFFF4D03F)
private val BG = Color(0xFF0D0D0D)
private val BAR_BG = Color(0xFF1A1A1A)
private val CARD = Color(0xFF2A2A2A)
private val ERR = Color(0xFFE74C3C)

@Composable
fun LayoutEditorScreen(
    initialProfile: LayoutProfile,
    allProfiles: List<LayoutProfile>,
    snapToGrid: Boolean,
    onSave: (LayoutProfile) -> Unit,
    onSelectPreset: (String) -> Unit,
    onToggleSnap: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val density = LocalDensity.current
    var elements by remember(initialProfile.id) { mutableStateOf(initialProfile.elements.toList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    var hasChanges by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var overlapping by remember { mutableStateOf(emptySet<String>()) }
    val gridStep = 0.025f

    // Recompute overlaps whenever elements change
    LaunchedEffect(elements) {
        val profile = LayoutProfile(id = "", name = "", elements = elements)
        val pairs = profile.findOverlaps()
        overlapping = pairs.flatMap { listOf(it.first, it.second) }.toSet()
    }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        // ── TOP BAR ──
        Row(
            Modifier.fillMaxWidth().background(BAR_BG).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(CARD).clickable(onClick = onClose),
                contentAlignment = Alignment.Center) {
                Text("✕", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("LAYOUT EDITOR", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = ACCENT, letterSpacing = 2.sp)
            // Save button
            val canSave = hasChanges && overlapping.isEmpty()
            Box(Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (canSave) Color(0xFF27AE60) else Color(0xFF333333))
                .clickable(enabled = canSave) { showNameDialog = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text("SAVE", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (canSave) Color.White else Color.Gray)
            }
        }

        // ── PRESET / SNAP BAR ──
        LazyRow(
            Modifier.fillMaxWidth().background(Color(0xFF141414)).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allProfiles.filter { it.isLandscape == initialProfile.isLandscape }) { p ->
                val sel = p.id == initialProfile.id
                Box(Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (sel) ACCENT else CARD)
                    .clickable { onSelectPreset(p.id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(p.name, fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel) Color.Black else Color.White)
                }
            }
            item {
                Box(Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (snapToGrid) Color(0xFF2980B9) else CARD)
                    .clickable { onToggleSnap(!snapToGrid) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(if (snapToGrid) "⊞ Snap ON" else "⊞ Snap OFF",
                        fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Overlap warning
        if (overlapping.isNotEmpty()) {
            Text("⚠ Elements overlap — fix before saving", fontSize = 11.sp,
                color = ERR, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().background(ERR.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp))
        }

        // ── CANVAS ──
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(4.dp)
                .clip(RoundedCornerShape(8.dp)).background(BAR_BG)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                .onGloballyPositioned { canvasSize = it.size }
        ) {
            if (snapToGrid && canvasSize.width > 0) GridLines(canvasSize)

            elements.forEachIndexed { index, elem ->
                if (elem.isVisible && canvasSize.width > 0) {
                    DraggableElement(
                        element = elem,
                        isSelected = selectedId == elem.id,
                        isOverlapping = elem.id in overlapping,
                        canvasSize = canvasSize,
                        snapToGrid = snapToGrid,
                        gridStep = gridStep,
                        onSelect = { selectedId = elem.id },
                        onMoved = { newX, newY ->
                            elements = elements.toMutableList().apply {
                                this[index] = elem.copy(x = newX, y = newY).clampToScreen()
                            }
                            hasChanges = true
                        }
                    )
                }
            }
        }

        // ── BOTTOM CONTROLS ──
        val sel = selectedId?.let { id -> elements.find { it.id == id } }
        if (sel != null) {
            ElementControls(
                element = sel,
                onResize = { dw, dh ->
                    elements = elements.map {
                        if (it.id == sel.id) it.copy(w = it.w + dw, h = it.h + dh).clampSize().clampToScreen()
                        else it
                    }
                    hasChanges = true
                },
                onToggleVisible = {
                    elements = elements.map {
                        if (it.id == sel.id) it.copy(isVisible = !it.isVisible) else it
                    }
                    hasChanges = true
                },
                onDeselect = { selectedId = null }
            )
        } else {
            Box(Modifier.fillMaxWidth().background(BAR_BG).padding(12.dp),
                contentAlignment = Alignment.Center) {
                Text("Tap an element to select · Drag to move · Use controls to resize",
                    fontSize = 11.sp, color = Color(0xFF666666))
            }
        }
    }

    // ── SAVE NAME DIALOG ──
    if (showNameDialog) {
        SaveNameDialog(
            defaultName = if (initialProfile.isBuiltIn) "" else initialProfile.name,
            onSave = { name ->
                val profile = if (initialProfile.isBuiltIn) {
                    LayoutPresets.fromTemplate(
                        initialProfile.copy(elements = elements),
                        name
                    )
                } else {
                    initialProfile.copy(name = name, elements = elements)
                }
                onSave(profile)
                showNameDialog = false
                hasChanges = false
            },
            onDismiss = { showNameDialog = false }
        )
    }
}

// ── DRAGGABLE ELEMENT (renders appearance matching its type) ──

@Composable
private fun DraggableElement(
    element: LayoutElement,
    isSelected: Boolean,
    isOverlapping: Boolean,
    canvasSize: IntSize,
    snapToGrid: Boolean,
    gridStep: Float,
    onSelect: () -> Unit,
    onMoved: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val xPx = element.x * canvasSize.width + dragOffset.x
    val yPx = element.y * canvasSize.height + dragOffset.y
    val wPx = element.w * canvasSize.width
    val hPx = element.h * canvasSize.height

    val borderColor = when {
        isOverlapping -> ERR
        isSelected -> Color.White
        else -> typeColor(element.type).copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .offset(
                x = with(density) { xPx.toDp() },
                y = with(density) { yPx.toDp() }
            )
            .size(
                width = with(density) { wPx.toDp() },
                height = with(density) { hPx.toDp() }
            )
            .clip(RoundedCornerShape(4.dp))
            .background(typeColor(element.type).copy(alpha = if (isSelected) 0.35f else 0.20f))
            .border(
                if (isSelected || isOverlapping) 2.dp else 1.dp,
                borderColor, RoundedCornerShape(4.dp)
            )
            .pointerInput(element.id) {
                detectDragGestures(
                    onDragStart = { onSelect(); dragOffset = Offset.Zero },
                    onDrag = { change, amount -> change.consume(); dragOffset += amount },
                    onDragEnd = {
                        var nx = element.x + dragOffset.x / canvasSize.width
                        var ny = element.y + dragOffset.y / canvasSize.height
                        if (snapToGrid) {
                            nx = (nx / gridStep).toInt() * gridStep
                            ny = (ny / gridStep).toInt() * gridStep
                        }
                        onMoved(
                            nx.coerceIn(0f, (1f - element.w).coerceAtLeast(0f)),
                            ny.coerceIn(0f, (1f - element.h).coerceAtLeast(0f))
                        )
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = { dragOffset = Offset.Zero }
                )
            }
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        // Render a miniature representation of the actual element
        ElementPreview(element.type, element.label, with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
    }
}

/** Mini representation inside the editor that matches the real game element */
@Composable
private fun ElementPreview(type: ElementType, label: String, widthDp: androidx.compose.ui.unit.Dp, heightDp: androidx.compose.ui.unit.Dp) {
    val small = minOf(widthDp.value, heightDp.value) < 40f
    val fontSize = if (small) 7.sp else 9.sp
    val tc = Color.White

    when (type) {
        ElementType.GAME_BOARD -> {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                // Grid hint
                Box(Modifier.fillMaxSize(0.85f).border(1.dp, Color(0xFF444444), RoundedCornerShape(2.dp)).background(Color(0xFF0A0A0A).copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center) {
                    Text("GAME\nBOARD", fontSize = if (small) 6.sp else 10.sp, color = Color(0xFF555555),
                        textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, lineHeight = if (small) 8.sp else 12.sp)
                }
            }
        }
        ElementType.SCORE_PANEL -> {
            Column(Modifier.fillMaxSize().padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("000000", fontSize = if (small) 6.sp else 10.sp, fontFamily = FontFamily.Monospace,
                    color = ACCENT, fontWeight = FontWeight.Bold)
                if (!small) Text("LV 1 · 0", fontSize = 7.sp, color = Color.Gray)
            }
        }
        ElementType.DPAD -> {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val bs = if (small) 10.dp else 16.dp
                Box(Modifier.size(bs).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                Row {
                    Box(Modifier.size(bs).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(bs * 0.6f))
                    Box(Modifier.size(bs).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                }
                Box(Modifier.size(bs).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
            }
        }
        ElementType.ROTATE_BUTTON -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val s = if (small) 16.dp else 28.dp
                Box(Modifier.size(s).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center) {
                    Text("↻", fontSize = if (small) 8.sp else 14.sp, color = tc)
                }
            }
        }
        ElementType.HOLD_PREVIEW -> {
            Column(Modifier.fillMaxSize().padding(1.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("HOLD", fontSize = fontSize, color = tc.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                Box(Modifier.size(if (small) 12.dp else 20.dp).background(Color(0xFF9B59B6).copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            }
        }
        ElementType.NEXT_PIECE_QUEUE -> {
            Column(Modifier.fillMaxSize().padding(1.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("NEXT", fontSize = fontSize, color = tc.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                repeat(if (small) 1 else 3) {
                    Box(Modifier.padding(1.dp).size(if (small) 10.dp else 14.dp).background(Color(0xFF1ABC9C).copy(alpha = 0.3f - it * 0.08f), RoundedCornerShape(2.dp)))
                }
            }
        }
        else -> {
            // Buttons: HOLD_BUTTON, START_BUTTON, PAUSE_BUTTON, SOUND_TOGGLE, MENU_BUTTON, ACTION_LABEL
            Box(Modifier.fillMaxSize().padding(2.dp), contentAlignment = Alignment.Center) {
                Text(label, fontSize = fontSize, color = tc, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

// ── BOTTOM ELEMENT CONTROLS ──

@Composable
private fun ElementControls(
    element: LayoutElement,
    onResize: (Float, Float) -> Unit,
    onToggleVisible: () -> Unit,
    onDeselect: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(BAR_BG).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(element.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("X:${pct(element.x)} Y:${pct(element.y)}  W:${pct(element.w)} H:${pct(element.h)}",
                fontSize = 9.sp, color = Color(0xFF888888))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CtrlBtn("W-") { onResize(-0.02f, 0f) }
            CtrlBtn("W+") { onResize(0.02f, 0f) }
            CtrlBtn("H-") { onResize(0f, -0.02f) }
            CtrlBtn("H+") { onResize(0f, 0.02f) }
            CtrlBtn(if (element.isVisible) "👁" else "🚫") { onToggleVisible() }
            CtrlBtn("✓") { onDeselect() }
        }
    }
}

@Composable
private fun CtrlBtn(text: String, onClick: () -> Unit) {
    Box(Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF333333)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(text, fontSize = 11.sp, color = Color.White)
    }
}

// ── SAVE NAME DIALOG ──

@Composable
private fun SaveNameDialog(defaultName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Layout", color = Color.White) },
        text = {
            Column {
                Text("Enter a name for this layout:", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    textStyle = TextStyle(fontSize = 18.sp, color = Color.White),
                    cursorBrush = SolidColor(ACCENT),
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (name.isEmpty()) Text("My Layout", fontSize = 18.sp, color = Color(0xFF555555))
                        inner()
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text("This layout will appear in your Layout menu and can be exported.",
                    fontSize = 11.sp, color = Color(0xFF666666))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Save", color = if (name.isNotBlank()) ACCENT else Color.Gray) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}

// ── GRID ──

@Composable
private fun GridLines(canvasSize: IntSize) {
    val density = LocalDensity.current
    val n = 20
    Box(Modifier.fillMaxSize()) {
        for (i in 1 until n) {
            val xp = canvasSize.width.toFloat() / n * i
            Box(Modifier.offset(x = with(density) { xp.toDp() }).width(0.5.dp).fillMaxHeight().background(Color(0xFF1E1E1E)))
        }
        for (i in 1 until n) {
            val yp = canvasSize.height.toFloat() / n * i
            Box(Modifier.offset(y = with(density) { yp.toDp() }).height(0.5.dp).fillMaxWidth().background(Color(0xFF1E1E1E)))
        }
    }
}

// ── HELPERS ──

private fun typeColor(type: ElementType): Color = when (type) {
    ElementType.GAME_BOARD -> Color(0xFF7F8C8D)
    ElementType.SCORE_PANEL -> Color(0xFF2ECC71)
    ElementType.HOLD_PREVIEW -> Color(0xFF9B59B6)
    ElementType.NEXT_PIECE_QUEUE -> Color(0xFF1ABC9C)
    ElementType.DPAD -> Color(0xFF3498DB)
    ElementType.ROTATE_BUTTON -> Color(0xFFE74C3C)
    ElementType.HOLD_BUTTON -> Color(0xFF9B59B6)
    ElementType.START_BUTTON -> Color(0xFF27AE60)
    ElementType.PAUSE_BUTTON -> Color(0xFFF39C12)
    ElementType.SOUND_TOGGLE -> Color(0xFF7F8C8D)
    ElementType.MENU_BUTTON -> Color(0xFF7F8C8D)
    ElementType.ACTION_LABEL -> Color(0xFFF4D03F)
}

private fun pct(f: Float): String = "${(f * 100).toInt()}%"
