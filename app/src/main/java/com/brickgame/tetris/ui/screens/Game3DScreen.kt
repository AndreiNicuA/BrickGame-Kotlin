package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.LocalGameTheme
import kotlin.math.*

/**
 * 3D Tetris game screen.
 *
 * Controls:
 *   D-pad (left):  move piece on XZ plane (camera-relative)
 *   Right column:  ROT XZ / ROT XY side by side, hard drop (UP arrow) / soft drop (DOWN arrow)
 *
 * Gestures on board:
 *   1 finger drag = rotate camera
 *   2 fingers same direction = pan
 *   2 fingers spread/pinch = zoom
 */
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
    onToggleGravity: () -> Unit = {},
    onQuit: () -> Unit = {}
) {
    val theme = LocalGameTheme.current

    var azimuth by remember { mutableFloatStateOf(35f) }
    var elevation by remember { mutableFloatStateOf(25f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var starWars by remember { mutableStateOf(false) }
    var showCamSettings by remember { mutableStateOf(false) }

    val effectiveAz = if (starWars) 0f else azimuth
    val effectiveEl = if (starWars) 75f else elevation

    // Camera-relative movement for D-pad
    fun moveCameraRelative(screenDx: Int, screenDz: Int) {
        if (starWars) { onMoveX(screenDx); onMoveZ(screenDz); return }
        val rad = Math.toRadians(azimuth.toDouble())
        val cosA = cos(rad).toFloat(); val sinA = sin(rad).toFloat()
        val worldX = screenDx * cosA - screenDz * sinA
        val worldY = screenDx * sinA + screenDz * cosA
        if (abs(worldX) >= abs(worldY)) onMoveX(if (worldX > 0) 1 else -1)
        else onMoveZ(if (worldY > 0) 1 else -1)
    }

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Info bar
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("NXT", fontSize = 7.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    state.nextPieces.forEachIndexed { i, type ->
                        Mini3DPiecePreview(type, Modifier.size(22.dp), theme.pixelOn, if (i == 0) 1f else 0.4f)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("H", fontSize = 7.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    Mini3DPiecePreview(state.holdPiece, Modifier.size(24.dp), theme.pixelOn, if (state.holdUsed) 0.3f else 1f)
                }
            }

            // 3D Board with gesture handling
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .pointerInput(starWars) {
                        if (starWars) return@pointerInput
                        awaitEachGesture {
                            val first = awaitFirstDown(pass = PointerEventPass.Main)
                            first.consume()
                            var prevCX = first.position.x; var prevCY = first.position.y
                            var prevSpread = 0f; var fingerCount = 1
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val pressed = event.changes.filter { !it.changedToUp() && it.pressed }
                                if (pressed.isEmpty()) break
                                val centX = pressed.map { it.position.x }.average().toFloat()
                                val centY = pressed.map { it.position.y }.average().toFloat()
                                val dx = centX - prevCX; val dy = centY - prevCY
                                if (pressed.size >= 2) {
                                    fingerCount = 2
                                    val spread = if (pressed.size == 2) {
                                        val d = pressed[0].position - pressed[1].position
                                        sqrt(d.x * d.x + d.y * d.y)
                                    } else 0f
                                    if (prevSpread > 0f && spread > 0f) {
                                        val sd = spread - prevSpread
                                        if (abs(sd) > 3f) zoom = (zoom + sd * 0.003f).coerceIn(0.3f, 3f)
                                        else { panX += dx; panY += dy }
                                    }
                                    prevSpread = spread
                                } else if (fingerCount == 1) {
                                    azimuth = (azimuth + dx * 0.3f) % 360f
                                    elevation = (elevation - dy * 0.2f) % 360f
                                }
                                prevCX = centX; prevCY = centY
                                pressed.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Tetris3DBoard(
                    state = state, modifier = Modifier.fillMaxSize().padding(2.dp),
                    showGhost = true, cameraAngleY = effectiveAz, cameraAngleX = effectiveEl,
                    panOffsetX = panX, panOffsetY = panY, zoom = zoom,
                    themePixelOn = theme.pixelOn, themeBg = theme.backgroundColor, starWarsMode = starWars
                )

                // ViewCube — top right
                if (!starWars) {
                    ViewCube(azimuth, elevation, Modifier.align(Alignment.TopEnd).padding(8.dp).size(60.dp))
                }

                // Zoom +/- buttons
                if (!starWars) {
                    Column(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(0.1f))
                            .clickable { zoom = (zoom + 0.15f).coerceAtMost(3f) }, contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.7f))
                        }
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(0.1f))
                            .clickable { zoom = (zoom - 0.15f).coerceAtLeast(0.3f) }, contentAlignment = Alignment.Center) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.7f))
                        }
                    }
                }

                // State overlays
                when (state.status) {
                    GameStatus.MENU -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("3D", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor, letterSpacing = 8.sp)
                            Text("TETRIS", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.textPrimary.copy(0.8f), letterSpacing = 6.sp)
                            Spacer(Modifier.height(28.dp))
                            ActionButton("START", onStart, width = 140.dp, height = 48.dp)
                            Spacer(Modifier.height(12.dp))
                            ActionButton("SETTINGS", onOpenSettings, width = 140.dp, height = 38.dp)
                        }
                    }
                    GameStatus.PAUSED -> {
                        Column(Modifier.background(Color.Black.copy(0.85f), RoundedCornerShape(16.dp)).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PAUSED", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.textPrimary)
                            Spacer(Modifier.height(16.dp))
                            ActionButton("RESUME", { onPause() }, width = 160.dp, height = 42.dp)
                            Spacer(Modifier.height(8.dp))
                            ActionButton("SETTINGS", onOpenSettings, width = 160.dp, height = 34.dp, backgroundColor = theme.buttonSecondary)
                            Spacer(Modifier.height(8.dp))
                            ActionButton("QUIT", onQuit, width = 160.dp, height = 34.dp, backgroundColor = Color(0xFFB91C1C))
                        }
                    }
                    GameStatus.GAME_OVER -> {
                        Column(Modifier.background(Color.Black.copy(0.85f), RoundedCornerShape(16.dp)).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GAME OVER", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444))
                            Spacer(Modifier.height(6.dp))
                            Text("Score: ${state.score}", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = theme.textPrimary)
                            Text("Level ${state.level} · ${state.layers} layers", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = theme.textSecondary)
                            Spacer(Modifier.height(16.dp))
                            ActionButton("PLAY AGAIN", onStart, width = 140.dp, height = 42.dp)
                            Spacer(Modifier.height(8.dp))
                            ActionButton("SETTINGS", onOpenSettings, width = 140.dp, height = 34.dp)
                        }
                    }
                    else -> {}
                }

                // Camera settings panel
                if (showCamSettings) {
                    Column(Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 8.dp)
                        .width(190.dp).background(Color.Black.copy(0.92f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Camera", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("✕", color = Color.White.copy(0.5f), fontSize = 16.sp,
                                modifier = Modifier.clickable { showCamSettings = false }.padding(4.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        CamSlider("Orbit", azimuth, -180f, 180f) { azimuth = it }
                        CamSlider("Tilt", elevation, -180f, 180f) { elevation = it }
                        CamSlider("Zoom", zoom, 0.3f, 3f) { zoom = it }
                        CamSlider("Pan X", panX, -200f, 200f) { panX = it }
                        CamSlider("Pan Y", panY, -200f, 200f) { panY = it }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                            CamPreset("Front", Modifier.weight(1f)) { azimuth = 0f; elevation = 15f; panX = 0f; panY = 0f; zoom = 1f }
                            CamPreset("Side", Modifier.weight(1f)) { azimuth = 90f; elevation = 20f; panX = 0f; panY = 0f; zoom = 1f }
                            CamPreset("Top", Modifier.weight(1f)) { azimuth = 35f; elevation = 70f; panX = 0f; panY = 0f; zoom = 1f }
                        }
                        Spacer(Modifier.height(4.dp))
                        CamPreset("Reset All", Modifier.fillMaxWidth()) { azimuth = 35f; elevation = 25f; panX = 0f; panY = 0f; zoom = 1f }
                    }
                }
            }

            // Controls row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                // Left: D-pad moves piece on XZ plane (camera-relative)
                DPad(
                    buttonSize = 44.dp,
                    rotateInCenter = false,
                    onUpPress = { moveCameraRelative(0, 1) },
                    onDownPress = { moveCameraRelative(0, -1) },
                    onDownRelease = {},
                    onLeftPress = { moveCameraRelative(-1, 0) },
                    onLeftRelease = {},
                    onRightPress = { moveCameraRelative(1, 0) },
                    onRightRelease = {}
                )

                // Center: action buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ActionButton("HOLD", onHold, width = 54.dp, height = 24.dp)
                    ActionButton(
                        if (state.status == GameStatus.MENU) "START" else "PAUSE",
                        { if (state.status == GameStatus.MENU) onStart() else onPause() },
                        width = 54.dp, height = 24.dp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        MiniToggle("SW", starWars, theme.accentColor) { starWars = !starWars }
                        MiniToggle("❄", !state.autoGravity, Color(0xFF38BDF8)) { onToggleGravity() }
                        MiniToggle("⚙", showCamSettings, Color(0xFFF59E0B)) { showCamSettings = !showCamSettings }
                    }
                    ActionButton("···", onOpenSettings, width = 36.dp, height = 18.dp)
                }

                // Right: ROT buttons side by side + hard drop / soft drop arrows
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Rotate buttons with text labels, side by side
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ActionButton("ROT XZ", onRotateXZ, width = 56.dp, height = 36.dp)
                        ActionButton("ROT XY", onRotateXY, width = 56.dp, height = 36.dp)
                    }
                    // Hard drop (UP arrow) / Soft drop (DOWN arrow)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TapButton(ButtonIcon.UP, 42.dp) { onHardDrop() }
                        HoldButton(ButtonIcon.DOWN, 42.dp, onPress = { onSoftDrop() }, onRelease = {})
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun MiniToggle(label: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(5.dp))
        .background(if (active) color.copy(0.3f) else Color.White.copy(0.05f))
        .clickable { onClick() }.padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            color = if (active) color else Color.White.copy(0.4f))
    }
}

@Composable
private fun CamSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(0.6f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(34.dp))
        Slider(value = value, onValueChange = onChange, valueRange = min..max,
            modifier = Modifier.weight(1f).height(20.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E)))
        Text(if (max <= 5f) "%.1f".format(value) else "${value.toInt()}", color = Color.White.copy(0.4f), fontSize = 9.sp,
            modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun CamPreset(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.08f))
        .clickable { onClick() }.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 9.sp, color = Color.White.copy(0.6f), fontFamily = FontFamily.Monospace)
    }
}
