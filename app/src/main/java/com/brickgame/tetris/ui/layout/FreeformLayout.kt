package com.brickgame.tetris.ui.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.FreeformPosition
import com.brickgame.tetris.data.PlayerProfile
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.screens.LocalMultiColor
import com.brickgame.tetris.ui.theme.LocalGameTheme
import kotlin.math.roundToInt

/**
 * Freeform game layout — all controls positioned via saved normalized coordinates.
 * The board fills the background, with translucent info + controls overlaid.
 * Uses the RoadTrip DraggableButton pattern for the editor.
 */
@Composable
fun FreeformGameLayout(
    gs: GameState,
    dpadStyle: DPadStyle,
    ghostEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    controlPositions: Map<String, FreeformPosition>,
    infoPositions: Map<String, FreeformPosition>,
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

        // Background board — fills entire screen
        GameBoard(
            board = gs.board,
            modifier = Modifier.fillMaxSize(),
            currentPiece = gs.currentPiece,
            ghostY = gs.ghostY,
            showGhost = ghostEnabled,
            clearingLines = gs.clearedLineRows,
            animationStyle = animationStyle,
            animationDuration = animationDuration,
            multiColor = LocalMultiColor.current
        )

        // === INFO OVERLAYS ===
        val defaults = PlayerProfile.defaultFreeformInfoPositions()

        // Score
        val sp = infoPositions["SCORE"] ?: defaults["SCORE"]!!
        Box(Modifier.offset(x = maxW * sp.x - 60.dp, y = maxH * sp.y)) {
            Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(
                    gs.score.toString().padStart(7, '0'),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = theme.pixelOn, letterSpacing = 2.sp
                )
            }
        }

        // Level
        val lp = infoPositions["LEVEL"] ?: defaults["LEVEL"]!!
        Box(Modifier.offset(x = maxW * lp.x - 24.dp, y = maxH * lp.y)) {
            Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("LV ${gs.level}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            }
        }

        // Lines
        val lip = infoPositions["LINES"] ?: defaults["LINES"]!!
        Box(Modifier.offset(x = maxW * lip.x - 28.dp, y = maxH * lip.y)) {
            Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("${gs.lines} LINES", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = theme.accentColor)
            }
        }

        // Hold preview
        val hp = infoPositions["HOLD_PREVIEW"] ?: defaults["HOLD_PREVIEW"]!!
        Box(Modifier.offset(x = maxW * hp.x - 22.dp, y = maxH * hp.y)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(4.dp)) {
                    HoldPiecePreview(gs.holdPiece?.shape, gs.holdUsed, Modifier.size(36.dp))
                }
            }
        }

        // Next preview
        val np = infoPositions["NEXT_PREVIEW"] ?: defaults["NEXT_PREVIEW"]!!
        Box(Modifier.offset(x = maxW * np.x - 22.dp, y = maxH * np.y)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.background(Color.Black.copy(0.45f), RoundedCornerShape(6.dp)).padding(4.dp)) {
                    Column {
                        gs.nextPieces.take(3).forEachIndexed { i, p ->
                            NextPiecePreview(
                                p.shape,
                                Modifier.size(if (i == 0) 36.dp else 24.dp),
                                if (i == 0) 1f else 0.5f
                            )
                        }
                    }
                }
            }
        }

        // === CONTROLS ===
        val cDefaults = PlayerProfile.defaultFreeformPositions()

        // D-Pad
        val dp = controlPositions["DPAD"] ?: cDefaults["DPAD"]!!
        Box(Modifier.offset(x = maxW * dp.x - 25.dp, y = maxH * dp.y - 25.dp)) {
            DPad(
                buttonSize = 50.dp,
                rotateInCenter = dpadStyle == DPadStyle.ROTATE_CENTRE,
                onUpPress = onHardDrop,
                onDownPress = onDownPress, onDownRelease = onDownRelease,
                onLeftPress = onLeftPress, onLeftRelease = onLeftRelease,
                onRightPress = onRightPress, onRightRelease = onRightRelease,
                onRotate = onRotate
            )
        }

        // Rotate button
        if (dpadStyle == DPadStyle.STANDARD) {
            val rp = controlPositions["ROTATE_BTN"] ?: cDefaults["ROTATE_BTN"]!!
            Box(Modifier.offset(x = maxW * rp.x - 30.dp, y = maxH * rp.y - 30.dp)) {
                RotateButton(onRotate, 60.dp)
            }
        }

        // Hold button
        val holdP = controlPositions["HOLD_BTN"] ?: cDefaults["HOLD_BTN"]!!
        Box(Modifier.offset(x = maxW * holdP.x - 36.dp, y = maxH * holdP.y - 15.dp)) {
            ActionButton("HOLD", onHold, width = 72.dp, height = 30.dp)
        }

        // Pause/Start button
        val pauseP = controlPositions["PAUSE_BTN"] ?: cDefaults["PAUSE_BTN"]!!
        Box(Modifier.offset(x = maxW * pauseP.x - 36.dp, y = maxH * pauseP.y - 15.dp)) {
            ActionButton(
                if (gs.status == GameStatus.MENU) "START" else "PAUSE",
                { if (gs.status == GameStatus.MENU) onStartGame() else onPause() },
                width = 72.dp, height = 30.dp
            )
        }

        // Settings button
        val menuP = controlPositions["MENU_BTN"] ?: cDefaults["MENU_BTN"]!!
        Box(Modifier.offset(x = maxW * menuP.x - 21.dp, y = maxH * menuP.y - 11.dp)) {
            ActionButton("···", onOpenSettings, width = 42.dp, height = 22.dp)
        }
    }
}

// ============================================================================
// FREEFORM EDITOR — RoadTrip-style drag overlay
// ============================================================================

/**
 * Edit-mode overlay for freeform layout.
 * Shows all draggable elements with handles; saves position on drop.
 */
@Composable
fun FreeformEditor(
    isEditMode: Boolean,
    controlPositions: Map<String, FreeformPosition>,
    infoPositions: Map<String, FreeformPosition>,
    onControlPositionChanged: (key: String, pos: FreeformPosition) -> Unit,
    onInfoPositionChanged: (key: String, pos: FreeformPosition) -> Unit,
    onResetPositions: () -> Unit,
    onExitEditMode: () -> Unit
) {
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = isEditMode,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
        ) {
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val maxW = maxWidth
            val maxH = maxHeight

            // Instructions
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.DragHandle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Drag elements to reposition",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Tap Done when finished",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Draggable control elements
            val cDefaults = PlayerProfile.defaultFreeformPositions()
            val controlItems = listOf(
                Triple("DPAD", "D-Pad", 50.dp),
                Triple("ROTATE_BTN", "Rotate", 60.dp),
                Triple("HOLD_BTN", "Hold", 72.dp),
                Triple("PAUSE_BTN", "Pause", 72.dp),
                Triple("MENU_BTN", "Menu", 42.dp)
            )

            controlItems.forEach { (key, label, size) ->
                val pos = controlPositions[key] ?: cDefaults[key]!!
                val sizePx = with(density) { size.toPx() }
                DraggableHandle(
                    label = label,
                    position = pos,
                    maxWidthPx = maxWidthPx,
                    maxHeightPx = maxHeightPx,
                    elementSizePx = sizePx,
                    elementSize = size,
                    color = Color(0xFF3B82F6),
                    onDragEnd = { newPos -> onControlPositionChanged(key, newPos) }
                )
            }

            // Draggable info elements
            val iDefaults = PlayerProfile.defaultFreeformInfoPositions()
            val infoItems = listOf(
                Triple("SCORE", "Score", 48.dp),
                Triple("LEVEL", "Level", 40.dp),
                Triple("LINES", "Lines", 40.dp),
                Triple("HOLD_PREVIEW", "Hold", 44.dp),
                Triple("NEXT_PREVIEW", "Next", 44.dp)
            )

            infoItems.forEach { (key, label, size) ->
                val pos = infoPositions[key] ?: iDefaults[key]!!
                val sizePx = with(density) { size.toPx() }
                DraggableHandle(
                    label = label,
                    position = pos,
                    maxWidthPx = maxWidthPx,
                    maxHeightPx = maxHeightPx,
                    elementSizePx = sizePx,
                    elementSize = size,
                    color = Color(0xFFF59E0B),
                    onDragEnd = { newPos -> onInfoPositionChanged(key, newPos) }
                )
            }

            // Bottom buttons: Reset + Done
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reset
                OutlinedButton(
                    onClick = onResetPositions,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }

                // Done
                Button(
                    onClick = onExitEditMode,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Done")
                }
            }
        }
    }
}

/**
 * A single draggable handle — the core reusable piece from RoadTrip's DraggableButton.
 * Renders as a labeled circle that follows the finger during drag.
 * Positions are normalized 0-1, converted to/from pixels at render time.
 */
@Composable
private fun BoxWithConstraintsScope.DraggableHandle(
    label: String,
    position: FreeformPosition,
    maxWidthPx: Float,
    maxHeightPx: Float,
    elementSizePx: Float,
    elementSize: androidx.compose.ui.unit.Dp,
    color: Color,
    onDragEnd: (FreeformPosition) -> Unit
) {
    // Convert normalized → pixel (center-adjusted)
    var offsetX by remember { mutableFloatStateOf(position.x * maxWidthPx - elementSizePx / 2) }
    var offsetY by remember { mutableFloatStateOf(position.y * maxHeightPx - elementSizePx / 2) }
    var isDragging by remember { mutableStateOf(false) }

    // Sync on external position change
    LaunchedEffect(position) {
        offsetX = position.x * maxWidthPx - elementSizePx / 2
        offsetY = position.y * maxHeightPx - elementSizePx / 2
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        // Convert pixel → normalized
                        val centerX = (offsetX + elementSizePx / 2) / maxWidthPx
                        val centerY = (offsetY + elementSizePx / 2) / maxHeightPx
                        onDragEnd(FreeformPosition(
                            x = centerX.coerceIn(0.05f, 0.95f),
                            y = centerY.coerceIn(0.05f, 0.95f)
                        ))
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWidthPx - elementSizePx)
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxHeightPx - elementSizePx)
                    }
                )
            }
    ) {
        // Handle circle
        Surface(
            modifier = Modifier
                .size(elementSize)
                .then(
                    if (isDragging) Modifier.border(2.dp, Color(0xFF22C55E), CircleShape)
                    else Modifier
                )
                .shadow(if (isDragging) 12.dp else 4.dp, CircleShape),
            shape = CircleShape,
            color = color.copy(alpha = if (isDragging) 0.95f else 0.8f)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    label.take(3),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Label below
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = elementSize + 2.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}
