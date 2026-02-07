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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * Full-screen drag-and-drop layout editor.
 * Users can move UI elements (D-Pad, Rotate, Score, Next queue, etc.)
 * to any position within the screen.
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
    val theme = LocalGameTheme.current
    val density = LocalDensity.current

    // Mutable state for dragging elements
    var editingElements by remember(currentProfile.id) {
        mutableStateOf(currentProfile.elements.toList())
    }
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    var hasChanges by remember { mutableStateOf(false) }
    val gridStep = 0.025f  // 2.5% grid snap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Text(
                "LAYOUT EDITOR",
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFFF4D03F), letterSpacing = 2.sp
            )

            // Save button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (hasChanges) Color(0xFF27AE60) else Color(0xFF333333))
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SAVE", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (hasChanges) Color.White else Color.Gray)
            }
        }

        // Profile selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allProfiles.filter { it.isLandscape }) { profile ->
                val isSelected = profile.id == currentProfile.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFF4D03F) else Color(0xFF2A2A2A))
                        .clickable { onSelectProfile(profile.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        profile.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }

            // Snap toggle
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (snapToGrid) Color(0xFF2980B9) else Color(0xFF2A2A2A))
                        .clickable { onToggleSnap(!snapToGrid) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (snapToGrid) "⊞ Snap ON" else "⊞ Snap OFF",
                        fontSize = 11.sp, color = Color.White
                    )
                }
            }
        }

        // Canvas area - the drag surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                .onGloballyPositioned { canvasSize = it.size }
        ) {
            // Grid overlay
            if (snapToGrid && canvasSize.width > 0) {
                GridOverlay(canvasSize)
            }

            // Game board placeholder (center)
            val boardWidthFraction = 0.35f
            val boardHeightFraction = 0.85f
            val boardLeft = (1f - boardWidthFraction) / 2f
            val boardTop = (1f - boardHeightFraction) / 2f

            if (canvasSize.width > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = with(density) { (canvasSize.width * boardLeft).toDp() },
                            top = with(density) { (canvasSize.height * boardTop).toDp() }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(
                                width = with(density) { (canvasSize.width * boardWidthFraction).toDp() },
                                height = with(density) { (canvasSize.height * boardHeightFraction).toDp() }
                            )
                            .border(2.dp, Color(0xFF333333).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .background(Color(0xFF0A0A0A).copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GAME\nBOARD", color = Color(0xFF444444), fontSize = 14.sp,
                            textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Draggable elements
            editingElements.forEachIndexed { index, element ->
                if (element.isVisible && canvasSize.width > 0) {
                    DraggableElement(
                        element = element,
                        isSelected = selectedElementId == element.id,
                        canvasSize = canvasSize,
                        snapToGrid = snapToGrid,
                        gridStep = gridStep,
                        onSelect = { selectedElementId = element.id },
                        onDrag = { newX, newY ->
                            editingElements = editingElements.toMutableList().apply {
                                this[index] = element.copy(
                                    offsetX = newX.coerceIn(0f, 1f - element.widthFraction),
                                    offsetY = newY.coerceIn(0f, 1f - element.heightFraction)
                                )
                            }
                            hasChanges = true
                        }
                    )
                }
            }
        }

        // Element info / controls at bottom
        selectedElementId?.let { id ->
            val element = editingElements.find { it.id == id }
            if (element != null) {
                ElementControlBar(
                    element = element,
                    onToggleVisibility = {
                        editingElements = editingElements.map {
                            if (it.id == id) it.copy(isVisible = !it.isVisible) else it
                        }
                        hasChanges = true
                    },
                    onToggleLock = {
                        editingElements = editingElements.map {
                            if (it.id == id) it.copy(isLocked = !it.isLocked) else it
                        }
                        hasChanges = true
                    },
                    onResize = { dw, dh ->
                        editingElements = editingElements.map {
                            if (it.id == id) it.copy(
                                widthFraction = (it.widthFraction + dw).coerceIn(0.05f, 0.5f),
                                heightFraction = (it.heightFraction + dh).coerceIn(0.05f, 0.5f)
                            ) else it
                        }
                        hasChanges = true
                    }
                )
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tap an element to select, then drag to reposition",
                    fontSize = 12.sp, color = Color(0xFF666666))
            }
        }
    }
}

@Composable
private fun DraggableElement(
    element: LayoutElement,
    isSelected: Boolean,
    canvasSize: IntSize,
    snapToGrid: Boolean,
    gridStep: Float,
    onSelect: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val elementColor = when (element.type) {
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

    val xPx = element.offsetX * canvasSize.width
    val yPx = element.offsetY * canvasSize.height
    val wPx = element.widthFraction * canvasSize.width
    val hPx = element.heightFraction * canvasSize.height

    Box(
        modifier = Modifier
            .offset(
                x = with(density) { (xPx + dragOffset.x).toDp() },
                y = with(density) { (yPx + dragOffset.y).toDp() }
            )
            .size(
                width = with(density) { wPx.toDp() },
                height = with(density) { hPx.toDp() }
            )
            .clip(RoundedCornerShape(6.dp))
            .background(elementColor.copy(alpha = if (isSelected) 0.5f else 0.3f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else elementColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp)
            )
            .then(
                if (!element.isLocked) {
                    Modifier.pointerInput(element.id) {
                        detectDragGestures(
                            onDragStart = {
                                onSelect()
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
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
                            },
                            onDragCancel = { dragOffset = Offset.Zero }
                        )
                    }
                } else Modifier
            )
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                element.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (element.isLocked) {
                Text("🔒", fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun GridOverlay(canvasSize: IntSize) {
    val density = LocalDensity.current
    val gridCount = 20

    Box(modifier = Modifier.fillMaxSize()) {
        // Vertical lines
        for (i in 1 until gridCount) {
            val x = canvasSize.width.toFloat() / gridCount * i
            Box(
                modifier = Modifier
                    .offset(x = with(density) { x.toDp() })
                    .width(0.5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF222222))
            )
        }
        // Horizontal lines
        for (i in 1 until gridCount) {
            val y = canvasSize.height.toFloat() / gridCount * i
            Box(
                modifier = Modifier
                    .offset(y = with(density) { y.toDp() })
                    .height(0.5.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF222222))
            )
        }
    }
}

@Composable
private fun ElementControlBar(
    element: LayoutElement,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onResize: (Float, Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Element name
        Column {
            Text(element.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "X:${(element.offsetX * 100).toInt()}% Y:${(element.offsetY * 100).toInt()}%  " +
                        "W:${(element.widthFraction * 100).toInt()}% H:${(element.heightFraction * 100).toInt()}%",
                fontSize = 10.sp, color = Color(0xFF888888)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Resize smaller
            ControlButton("-") { onResize(-0.02f, -0.02f) }
            // Resize larger
            ControlButton("+") { onResize(0.02f, 0.02f) }
            // Toggle visibility
            ControlButton(if (element.isVisible) "👁" else "🚫") { onToggleVisibility() }
            // Toggle lock
            ControlButton(if (element.isLocked) "🔒" else "🔓") { onToggleLock() }
        }
    }
}

@Composable
private fun ControlButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF333333))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = Color.White)
    }
}
