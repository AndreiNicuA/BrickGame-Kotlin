package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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

    // Camera state
    var cameraY by remember { mutableFloatStateOf(35f) }
    var cameraX by remember { mutableFloatStateOf(25f) }
    var starWars by remember { mutableStateOf(false) }
    var showCamSettings by remember { mutableStateOf(false) }
    var fov by remember { mutableFloatStateOf(700f) }
    var camDist by remember { mutableFloatStateOf(16f) }

    // Star Wars fixed view: looking up into the board
    val effectiveCamY = if (starWars) 0f else cameraY
    val effectiveCamX = if (starWars) 75f else cameraX

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

            // 3D Board — swipe to rotate camera
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .pointerInput(starWars) {
                        if (!starWars) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                cameraY = (cameraY + dragAmount.x * 0.3f) % 360f
                                cameraX = (cameraX - dragAmount.y * 0.2f).coerceIn(-10f, 80f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Tetris3DBoard(
                    state = state,
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    showGhost = true,
                    cameraAngleY = effectiveCamY,
                    cameraAngleX = effectiveCamX,
                    themePixelOn = theme.pixelOn,
                    themeBg = theme.backgroundColor,
                    starWarsMode = starWars
                )

                // Overlays
                when (state.status) {
                    GameStatus.MENU -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("3D", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                                color = theme.accentColor, letterSpacing = 8.sp)
                            Text("TETRIS", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                                color = theme.textPrimary.copy(0.8f), letterSpacing = 6.sp)
                            Spacer(Modifier.height(28.dp))
                            ActionButton("START", onStart, width = 140.dp, height = 48.dp)
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
                        }
                    }
                    GameStatus.PAUSED -> {
                        Column(Modifier.background(Color.Black.copy(0.85f), RoundedCornerShape(16.dp)).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PAUSED", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.textPrimary)
                            Spacer(Modifier.height(16.dp))
                            ActionButton("RESUME", { onPause() }, width = 140.dp, height = 42.dp)
                        }
                    }
                    else -> {}
                }

                // Camera settings popup
                if (showCamSettings) {
                    Column(
                        Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                            .width(180.dp)
                            .background(Color.Black.copy(0.9f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Camera", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("✕", color = Color.White.copy(0.5f), fontSize = 16.sp,
                                modifier = Modifier.clickable { showCamSettings = false }.padding(4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        CamSlider("Angle", cameraY, -180f, 180f) { cameraY = it }
                        CamSlider("Tilt", cameraX, -10f, 80f) { cameraX = it }
                        Spacer(Modifier.height(4.dp))
                        // Preset buttons
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                            CamPreset("Front", Modifier.weight(1f)) { cameraY = 0f; cameraX = 15f }
                            CamPreset("Side", Modifier.weight(1f)) { cameraY = 90f; cameraX = 20f }
                            CamPreset("Top", Modifier.weight(1f)) { cameraY = 35f; cameraX = 70f }
                        }
                    }
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
                    // Toggle row: SW, Gravity, Cam
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        MiniToggle("SW", starWars, theme.accentColor) { starWars = !starWars }
                        MiniToggle(if (state.autoGravity) "▼" else "▪", state.autoGravity, Color(0xFF3B82F6)) { onToggleGravity() }
                        MiniToggle("⚙", showCamSettings, Color(0xFFF59E0B)) { showCamSettings = !showCamSettings }
                    }
                    ActionButton("···", onOpenSettings, width = 36.dp, height = 18.dp)
                }

                // Right: rotations + drop
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    RotateButton(onRotateXZ, 48.dp)
                    // Soft drop (single step down)
                    TapButton(ButtonIcon.DOWN, 42.dp) { onSoftDrop() }
                    // Hard drop
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .background(theme.buttonPrimary)
                            .clickable { onHardDrop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏬", fontSize = 20.sp, color = theme.textPrimary)
                    }
                    // Tilt rotate
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(theme.buttonPrimary.copy(0.6f))
                            .clickable { onRotateXY() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⤵", fontSize = 16.sp, color = theme.textPrimary.copy(0.8f))
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
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
private fun CamSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp))
        Slider(value = value, onValueChange = onChange, valueRange = min..max,
            modifier = Modifier.weight(1f).height(20.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E)))
        Text("${value.toInt()}°", color = Color.White.copy(0.4f), fontSize = 9.sp,
            modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun CamPreset(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.08f))
        .clickable { onClick() }.padding(vertical = 4.dp),
        contentAlignment = Alignment.Center) {
        Text(label, fontSize = 9.sp, color = Color.White.copy(0.6f), fontFamily = FontFamily.Monospace)
    }
}

private fun pieceUIColor(idx: Int): Color = when (idx) {
    1 -> Color(0xFF00E5FF); 2 -> Color(0xFFFFD600); 3 -> Color(0xFFAA00FF); 4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00); 6 -> Color(0xFFFF1744); 7 -> Color(0xFF2979FF); 8 -> Color(0xFFFF4081)
    else -> Color(0xFF888888)
}
