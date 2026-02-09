package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.LocalGameTheme

@Composable
fun Game3DScreen(
    state: Game3DState,
    onMoveX: (Int) -> Unit,
    onMoveZ: (Int) -> Unit,
    onRotateXZ: () -> Unit,
    onRotateXY: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onPause: () -> Unit,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onSoftDrop: () -> Unit = {},
    onToggleGravity: () -> Unit = {}
) {
    val theme = LocalGameTheme.current

    // Camera state — full 360° on both axes
    var cameraY by remember { mutableFloatStateOf(35f) }
    var cameraX by remember { mutableFloatStateOf(25f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var starWars by remember { mutableStateOf(false) }

    // Snap camera to a predefined view
    fun snapTo(y: Float, x: Float) { cameraY = y; cameraX = x }

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Compact info bar
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.4f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", fontSize = 7.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(state.score.toString().padStart(7, '0'), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = theme.accentColor, letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LV", fontSize = 7.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    Text("${state.level}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LAYERS", fontSize = 7.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    Text("${state.layers}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("NXT ", fontSize = 7.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    state.nextPieces.forEachIndexed { i, type ->
                        Box(Modifier.size(20.dp).background(theme.pixelOn.copy(0.1f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(type.displayName.first().toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = pieceUIColor(type.colorIndex).copy(if (i == 0) 1f else 0.4f), fontFamily = FontFamily.Monospace)
                        }
                        if (i < state.nextPieces.size - 1) Spacer(Modifier.width(2.dp))
                    }
                }
                Box(Modifier.size(22.dp).background(theme.pixelOn.copy(0.1f), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center) {
                    val hp = state.holdPiece
                    if (hp != null) Text(hp.displayName.first().toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = pieceUIColor(hp.colorIndex).copy(if (state.holdUsed) 0.3f else 1f), fontFamily = FontFamily.Monospace)
                    else Text("H", fontSize = 8.sp, color = theme.textSecondary.copy(0.3f), fontFamily = FontFamily.Monospace)
                }
            }

            // 3D Board + ViewCube overlay
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .pointerInput(starWars) {
                        if (!starWars) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                // Drag to rotate
                                cameraY = (cameraY + pan.x * 0.3f) % 360f
                                cameraX = (cameraX - pan.y * 0.2f).coerceIn(-89f, 89f)
                                // Pinch to zoom
                                zoom = (zoom * gestureZoom).coerceIn(0.4f, 3f)
                            }
                        }
                    }
            ) {
                Tetris3DBoard(
                    state = state,
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    showGhost = true,
                    cameraAngleY = cameraY,
                    cameraAngleX = cameraX,
                    zoom = zoom,
                    themePixelOn = theme.pixelOn,
                    themeBg = theme.backgroundColor,
                    starWarsMode = starWars
                )

                // ViewCube — top right corner (only in free camera mode)
                if (!starWars) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(70.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    // Determine which face was tapped based on tap position
                                    val cx = offset.x / size.width
                                    val cy = offset.y / size.height
                                    when {
                                        cy < 0.3f -> snapTo(cameraY, 89f)    // Top area → top view
                                        cy > 0.7f -> snapTo(cameraY, -89f)   // Bottom area → bottom view
                                        cx < 0.3f -> snapTo(-90f, 20f)       // Left → left view
                                        cx > 0.7f -> snapTo(90f, 20f)        // Right → right view
                                        else -> snapTo(0f, 20f)              // Center → front view
                                    }
                                }
                            }
                    ) {
                        ViewCube(
                            cameraAngleY = cameraY,
                            cameraAngleX = cameraX,
                            modifier = Modifier.fillMaxSize(),
                            themeColor = theme.accentColor
                        )
                    }
                }

                // Zoom buttons — top left (only in free camera mode)
                if (!starWars) {
                    Column(
                        Modifier.align(Alignment.TopStart).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ZoomBtn("+") { zoom = (zoom * 1.2f).coerceAtMost(3f) }
                        ZoomBtn("−") { zoom = (zoom / 1.2f).coerceAtLeast(0.4f) }
                        // Reset zoom
                        ZoomBtn("⌂") { zoom = 1f; snapTo(35f, 25f) }
                    }
                }

                // Overlays
                when (state.status) {
                    GameStatus.MENU -> {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("3D", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                                color = theme.accentColor, letterSpacing = 8.sp)
                            Text("TETRIS", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                                color = theme.textPrimary.copy(0.8f), letterSpacing = 6.sp)
                            Spacer(Modifier.height(28.dp))
                            ActionButton("START", onStart, width = 140.dp, height = 48.dp)
                        }
                    }
                    GameStatus.GAME_OVER -> {
                        Column(Modifier.align(Alignment.Center)
                            .background(Color.Black.copy(0.85f), RoundedCornerShape(16.dp)).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GAME OVER", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444))
                            Spacer(Modifier.height(6.dp))
                            Text("Score: ${state.score}", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = theme.textPrimary)
                            Text("Level ${state.level} · ${state.layers} layers", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = theme.textSecondary)
                            Spacer(Modifier.height(16.dp))
                            ActionButton("PLAY AGAIN", onStart, width = 140.dp, height = 42.dp)
                        }
                    }
                    GameStatus.PAUSED -> {
                        Column(Modifier.align(Alignment.Center)
                            .background(Color.Black.copy(0.85f), RoundedCornerShape(16.dp)).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PAUSED", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.textPrimary)
                            Spacer(Modifier.height(16.dp))
                            ActionButton("RESUME", { onPause() }, width = 140.dp, height = 42.dp)
                        }
                    }
                    else -> {}
                }
            }

            // Controls row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                // Left: D-pad for XZ movement
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TapButton(ButtonIcon.UP, 44.dp) { onMoveZ(1) }
                    Row {
                        HoldButton(ButtonIcon.LEFT, 44.dp, onPress = { onMoveX(-1) }, onRelease = {})
                        Spacer(Modifier.width(22.dp))
                        HoldButton(ButtonIcon.RIGHT, 44.dp, onPress = { onMoveX(1) }, onRelease = {})
                    }
                    TapButton(ButtonIcon.DOWN, 44.dp) { onMoveZ(-1) }
                }

                // Center: action buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ActionButton("HOLD", onHold, width = 54.dp, height = 24.dp)
                    ActionButton(
                        if (state.status == GameStatus.MENU) "START" else "PAUSE",
                        { if (state.status == GameStatus.MENU) onStart() else onPause() },
                        width = 54.dp, height = 24.dp
                    )
                    // Toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        MiniToggle("SW", starWars, theme.accentColor) { starWars = !starWars }
                        MiniToggle(if (state.autoGravity) "▼" else "▪", state.autoGravity, Color(0xFF3B82F6)) { onToggleGravity() }
                    }
                    ActionButton("···", onOpenSettings, width = 36.dp, height = 18.dp)
                }

                // Right: rotations + drop — LABELED buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Rotate XZ — horizontal spin (like looking from above)
                    LabeledRotateBtn("XZ", theme.accentColor) { onRotateXZ() }
                    // Rotate XY — tilt forward/back
                    LabeledRotateBtn("XY", Color(0xFF3B82F6)) { onRotateXY() }
                    // Soft drop
                    TapButton(ButtonIcon.DOWN, 40.dp) { onSoftDrop() }
                    // Hard drop
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(theme.buttonPrimary)
                            .clickable { onHardDrop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏬", fontSize = 18.sp, color = theme.textPrimary)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun LabeledRotateBtn(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 52.dp, height = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(0.15f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("↻", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                color = color.copy(0.8f), letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun MiniToggle(label: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(5.dp))
            .background(if (active) color.copy(0.3f) else Color.White.copy(0.05f))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            color = if (active) color else Color.White.copy(0.4f))
    }
}

@Composable
private fun ZoomBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape)
            .background(Color.White.copy(0.08f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.7f),
            textAlign = TextAlign.Center)
    }
}

private fun pieceUIColor(idx: Int): Color = when (idx) {
    1 -> Color(0xFF00E5FF); 2 -> Color(0xFFFFD600); 3 -> Color(0xFFAA00FF); 4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00); 6 -> Color(0xFFFF1744); 7 -> Color(0xFF2979FF); 8 -> Color(0xFFFF4081)
    else -> Color(0xFF888888)
}
