package com.brickgame.tetris.ui.layout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

private val ACCENT = Color(0xFFF4D03F)
private val BG_DARK = Color(0xFF0D0D0D)
private val BG_CARD = Color(0xFF1A1A1A)
private val BG_BUTTON = Color(0xFF2A2A2A)
private val TEXT_DIM = Color(0xFF888888)

/**
 * Drag-and-drop layout editor.
 * - Long-press to pick up an element, then drag to reposition.
 * - Tap to select an element and see its controls in the bottom panel.
 * - Menu button cannot be hidden.
 * - Elements snap to grid when enabled.
 * - Large touch targets and clear visual feedback.
 */
@Composable
fun LayoutEditorScreen(
    currentProfile: LayoutProfile,
    allProfiles: List<LayoutProfile>,
    snapToGrid: Boolean,
    onSaveProfile: (LayoutProfile) -> Unit,
    onSelectProfile: (String) -> Unit,
    onToggleSnap: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val density = LocalDensity.current

    var editingElements by remember(currentProfile.id) {
        mutableStateOf(currentProfile.elements.toList())
    }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    var hasChanges by remember { mutableStateOf(false) }
    val gridStep = 0.05f  // 5% grid for easier snapping

    Column(modifier = Modifier.fillMaxSize().background(BG_DARK)) {

        // ===== TOP BAR =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BG_CARD)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close
            Box(
                modifier = Modifier
                    .size(40.dp).clip(CircleShape).background(BG_BUTTON)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Text("✕", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold) }

            Text("LAYOUT EDITOR", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = ACCENT, letterSpacing = 2.sp)

            // Save
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (hasChanges) Color(0xFF27AE60) else BG_BUTTON)
                    .clickable(enabled = hasChanges) {
                        val updated = currentProfile.copy(
                            elements = editingElements,
                            isBuiltIn = false,
                            id = if (currentProfile.isBuiltIn) "custom_${System.currentTimeMillis()}" else currentProfile.id,
                            name = if (currentProfile.isBuiltIn) "${currentProfile.name} (Custom)" else currentProfile.name
                        )
                        onSaveProfile(updated)
                        hasChanges = false
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SAVE", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (hasChanges) Color.White else TEXT_DIM)
            }
        }

        // ===== PRESET SELECTOR =====
        LazyRow(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF121212)).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allProfiles.filter { it.isLandscape }) { profile ->
                val isSel = profile.id == currentProfile.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) ACCENT else BG_BUTTON)
                        .clickable { onSelectProfile(profile.id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(profile.name, fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) Color.Black else Color.White)
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (snapToGrid) Color(0xFF2980B9) else BG_BUTTON)
                        .clickable { onToggleSnap(!snapToGrid) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(if (snapToGrid) "⊞ Snap ON" else "⊞ Snap OFF",
                        fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // ===== HINT =====
        Text(
            "Long-press and drag to move  •  Tap to select",
            fontSize = 11.sp, color = TEXT_DIM,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            textAlign = TextAlign.Center
        )

        // ===== CANVAS =====
        Box(
            modifier = Modifier
                .weight(1f).fillMaxWidth()
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF151515))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                .onGloballyPositioned { canvasSize = it.size }
        ) {
            if (canvasSize.width > 0) {
                // Grid
                if (snapToGrid) GridOverlay(canvasSize, gridStep)

                // Board placeholder
                val bw = 0.32f; val bh = 0.82f
                val bl = (1f - bw) / 2; val bt = (1f - bh) / 2
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (canvasSize.width * bl).toDp() },
                            y = with(density) { (canvasSize.height * bt).toDp() }
                        )
                        .size(
                            width = with(density) { (canvasSize.width * bw).toDp() },
                            height = with(density) { (canvasSize.height * bh).toDp() }
                        )
                        .border(2.dp, Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
                        .background(Color(0xFF0A0A0A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GAME BOARD", color = Color(0xFF333333), fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                }

                // Elements
                editingElements.forEachIndexed { index, element ->
                    if (element.isVisible) {
                        DraggableElement(
                            element = element,
                            isSelected = selectedId == element.id,
                            isDragging = draggingId == element.id,
                            canvasSize = canvasSize,
                            snapToGrid = snapToGrid,
                            gridStep = gridStep,
                            onTap = { selectedId = element.id },
                            onDragStart = {
                                selectedId = element.id
                                draggingId = element.id
                            },
                            onDrag = { newX, newY ->
                                editingElements = editingElements.toMutableList().apply {
                                    this[index] = element.copy(
                                        offsetX = newX.coerceIn(0f, 1f - element.widthFraction),
                                        offsetY = newY.coerceIn(0f, 1f - element.heightFraction)
                                    )
                                }
                                hasChanges = true
                            },
                            onDragEnd = { draggingId = null }
                        )
                    }
                }
            }
        }

        // ===== BOTTOM CONTROL PANEL =====
        val selectedElement = editingElements.find { it.id == selectedId }
        BottomControlPanel(
            element = selectedElement,
            onWidthChange = { newW ->
                selectedId?.let { id ->
                    editingElements = editingElements.map {
                        if (it.id == id) it.copy(widthFraction = newW.coerceIn(0.06f, 0.45f)) else it
                    }
                    hasChanges = true
                }
            },
            onHeightChange = { newH ->
                selectedId?.let { id ->
                    editingElements = editingElements.map {
                        if (it.id == id) it.copy(heightFraction = newH.coerceIn(0.06f, 0.45f)) else it
                    }
                    hasChanges = true
                }
            },
            onToggleVisibility = {
                selectedId?.let { id ->
                    val el = editingElements.find { it.id == id } ?: return@let
                    // Prevent hiding menu button
                    if (el.type == ElementType.MENU_BUTTON) return@let
                    editingElements = editingElements.map {
                        if (it.id == id) it.copy(isVisible = !it.isVisible) else it
                    }
                    hasChanges = true
                }
            },
            onToggleLock = {
                selectedId?.let { id ->
                    editingElements = editingElements.map {
                        if (it.id == id) it.copy(isLocked = !it.isLocked) else it
                    }
                    hasChanges = true
                }
            },
            onDeselect = { selectedId = null }
        )
    }
}

// ===== Draggable Element =====

@Composable
private fun DraggableElement(
    element: LayoutElement,
    isSelected: Boolean,
    isDragging: Boolean,
    canvasSize: IntSize,
    snapToGrid: Boolean,
    gridStep: Float,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val elementColor = elementTypeColor(element.type)

    val borderColor by animateColorAsState(
        targetValue = when {
            isDragging -> Color.White
            isSelected -> ACCENT
            else -> elementColor.copy(alpha = 0.5f)
        },
        label = "border"
    )

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else if (isSelected) 4.dp else 0.dp,
        animationSpec = spring(),
        label = "elevation"
    )

    val xPx = element.offsetX * canvasSize.width + dragOffset.x
    val yPx = element.offsetY * canvasSize.height + dragOffset.y
    val wPx = element.widthFraction * canvasSize.width
    val hPx = element.heightFraction * canvasSize.height

    Box(
        modifier = Modifier
            .zIndex(if (isDragging) 100f else if (isSelected) 10f else 1f)
            .offset(
                x = with(density) { xPx.toDp() },
                y = with(density) { yPx.toDp() }
            )
            .size(
                width = with(density) { wPx.toDp() },
                height = with(density) { hPx.toDp() }
            )
            .shadow(elevation, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDragging) elementColor.copy(alpha = 0.6f)
                else elementColor.copy(alpha = if (isSelected) 0.4f else 0.25f)
            )
            .border(
                width = if (isDragging) 3.dp else if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (!element.isLocked) {
                    Modifier.pointerInput(element.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragStart()
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                            },
                            onDragEnd = {
                                var newX = element.offsetX + dragOffset.x / canvasSize.width
                                var newY = element.offsetY + dragOffset.y / canvasSize.height
                                if (snapToGrid) {
                                    newX = (newX / gridStep).toInt() * gridStep
                                    newY = (newY / gridStep).toInt() * gridStep
                                }
                                onDrag(newX, newY)
                                dragOffset = Offset.Zero
                                onDragEnd()
                            },
                            onDragCancel = {
                                dragOffset = Offset.Zero
                                onDragEnd()
                            }
                        )
                    }
                } else Modifier
            )
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Icon/emoji for element type
            Text(
                elementIcon(element.type),
                fontSize = if (hPx > 80) 20.sp else 14.sp
            )
            if (hPx > 50) {
                Text(
                    element.label,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center, maxLines = 1
                )
            }
            if (element.isLocked) {
                Text("🔒", fontSize = 10.sp)
            }
        }
    }
}

// ===== Bottom Control Panel =====

@Composable
private fun BottomControlPanel(
    element: LayoutElement?,
    onWidthChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onDeselect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG_CARD)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (element == null) {
            Text("Tap an element to select it, then long-press to drag",
                fontSize = 13.sp, color = TEXT_DIM,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        } else {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(elementIcon(element.type), fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(element.label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = Color.White)
                        Text(
                            "Position: ${(element.offsetX * 100).toInt()}%, ${(element.offsetY * 100).toInt()}%",
                            fontSize = 10.sp, color = TEXT_DIM
                        )
                    }
                }

                // Deselect button
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(BG_BUTTON)
                        .clickable(onClick = onDeselect),
                    contentAlignment = Alignment.Center
                ) { Text("✕", fontSize = 14.sp, color = Color.White) }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Width slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("W", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TEXT_DIM,
                    modifier = Modifier.width(20.dp))
                Slider(
                    value = element.widthFraction,
                    onValueChange = onWidthChange,
                    valueRange = 0.06f..0.45f,
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = ACCENT, activeTrackColor = ACCENT,
                        inactiveTrackColor = BG_BUTTON
                    )
                )
                Text("${(element.widthFraction * 100).toInt()}%", fontSize = 11.sp,
                    color = ACCENT, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }

            // Height slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("H", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TEXT_DIM,
                    modifier = Modifier.width(20.dp))
                Slider(
                    value = element.heightFraction,
                    onValueChange = onHeightChange,
                    valueRange = 0.06f..0.45f,
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = ACCENT, activeTrackColor = ACCENT,
                        inactiveTrackColor = BG_BUTTON
                    )
                )
                Text("${(element.heightFraction * 100).toInt()}%", fontSize = 11.sp,
                    color = ACCENT, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lock toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (element.isLocked) "🔒 Locked" else "🔓 Unlocked",
                        fontSize = 12.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = element.isLocked,
                        onCheckedChange = { onToggleLock() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFE74C3C),
                            checkedTrackColor = Color(0xFFE74C3C).copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = BG_BUTTON
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Hide button (disabled for menu button)
                val canHide = element.type != ElementType.MENU_BUTTON
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canHide) BG_BUTTON else BG_BUTTON.copy(alpha = 0.3f))
                        .clickable(enabled = canHide, onClick = onToggleVisibility)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (!canHide) "🔒 Always visible"
                        else if (element.isVisible) "👁 Hide" else "Show",
                        fontSize = 12.sp,
                        color = if (canHide) Color.White else TEXT_DIM
                    )
                }
            }
        }
    }
}

// ===== Grid Overlay =====

@Composable
private fun GridOverlay(canvasSize: IntSize, gridStep: Float) {
    val density = LocalDensity.current
    val steps = (1f / gridStep).toInt()

    Box(modifier = Modifier.fillMaxSize()) {
        for (i in 1 until steps) {
            val frac = i * gridStep
            // Vertical
            Box(
                modifier = Modifier
                    .offset(x = with(density) { (canvasSize.width * frac).toDp() })
                    .width(0.5.dp).fillMaxHeight()
                    .background(Color(0xFF1E1E1E))
            )
            // Horizontal
            Box(
                modifier = Modifier
                    .offset(y = with(density) { (canvasSize.height * frac).toDp() })
                    .height(0.5.dp).fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
            )
        }
    }
}

// ===== Helpers =====

private fun elementTypeColor(type: ElementType): Color = when (type) {
    ElementType.DPAD -> Color(0xFF3498DB)
    ElementType.ROTATE_BUTTON -> Color(0xFFE74C3C)
    ElementType.HARD_DROP_BUTTON -> Color(0xFFF39C12)
    ElementType.HOLD_BUTTON -> Color(0xFF9B59B6)
    ElementType.SCORE_PANEL -> Color(0xFF2ECC71)
    ElementType.NEXT_PIECE_QUEUE -> Color(0xFF1ABC9C)
    ElementType.START_PAUSE_BUTTONS -> Color(0xFF95A5A6)
    ElementType.SOUND_TOGGLE -> Color(0xFF7F8C8D)
    ElementType.MENU_BUTTON -> Color(0xFF7F8C8D)
    ElementType.ACTION_LABEL -> Color(0xFFF4D03F)
}

private fun elementIcon(type: ElementType): String = when (type) {
    ElementType.DPAD -> "✛"
    ElementType.ROTATE_BUTTON -> "↻"
    ElementType.HARD_DROP_BUTTON -> "▼"
    ElementType.HOLD_BUTTON -> "◧"
    ElementType.SCORE_PANEL -> "★"
    ElementType.NEXT_PIECE_QUEUE -> "▶▶"
    ElementType.START_PAUSE_BUTTONS -> "▶❚❚"
    ElementType.SOUND_TOGGLE -> "♪"
    ElementType.MENU_BUTTON -> "☰"
    ElementType.ACTION_LABEL -> "💥"
}
