package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current
    var cameraY by remember { mutableFloatStateOf(35f) }
    var cameraX by remember { mutableFloatStateOf(25f) }
    var starWars by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(theme.backgroundColor).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Compact info bar
            Row(
                Modifier.fillMaxWidth()
                    .background(Color.Black.copy(0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", fontSize = 8.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(state.score.toString().padStart(7, '0'), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = theme.accentColor, letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LV", fontSize = 8.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    Text("${state.level}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LAYERS", fontSize = 8.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    Text("${state.layers}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.accentColor)
                }
                // Next pieces
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("NXT ", fontSize = 7.sp, color = theme.textSecondary, fontFamily = FontFamily.Monospace)
                    state.nextPieces.forEachIndexed { i, type ->
                        Box(Modifier.size(22.dp).background(theme.pixelOn.copy(0.1f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(type.displayName.first().toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = pieceUIColor(type.colorIndex).copy(if (i == 0) 1f else 0.4f), fontFamily = FontFamily.Monospace)
                        }
                        if (i < state.nextPieces.size - 1) Spacer(Modifier.width(3.dp))
                    }
                }
                // Hold
                Box(Modifier.size(24.dp).background(theme.pixelOn.copy(0.1f), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center) {
                    val hp = state.holdPiece
                    if (hp != null) Text(hp.displayName.first().toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = pieceUIColor(hp.colorIndex).copy(if (state.holdUsed) 0.3f else 1f), fontFamily = FontFamily.Monospace)
                    else Text("H", fontSize = 9.sp, color = theme.textSecondary.copy(0.3f), fontFamily = FontFamily.Monospace)
                }
            }

            // 3D Board — swipe to rotate camera
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            cameraY = (cameraY + dragAmount.x * 0.3f) % 360f
                            cameraX = (cameraX - dragAmount.y * 0.2f).coerceIn(-10f, 80f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Tetris3DBoard(
                    state = state,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    showGhost = true,
                    cameraAngleY = cameraY,
                    cameraAngleX = cameraX,
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
            }

            // Controls row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                // Left: D-pad for XZ movement
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TapButton(ButtonIcon.UP, 46.dp) { onMoveZ(1) }
                    Row {
                        HoldButton(ButtonIcon.LEFT, 46.dp, onPress = { onMoveX(-1) }, onRelease = {})
                        Spacer(Modifier.width(24.dp))
                        HoldButton(ButtonIcon.RIGHT, 46.dp, onPress = { onMoveX(1) }, onRelease = {})
                    }
                    TapButton(ButtonIcon.DOWN, 46.dp) { onMoveZ(-1) }
                }

                // Center: action buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ActionButton("HOLD", onHold, width = 56.dp, height = 26.dp)
                    ActionButton(
                        if (state.status == GameStatus.MENU) "START" else "PAUSE",
                        { if (state.status == GameStatus.MENU) onStart() else onPause() },
                        width = 56.dp, height = 26.dp
                    )
                    // Star Wars mode toggle
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (starWars) theme.accentColor.copy(0.3f) else Color.White.copy(0.05f))
                            .clickable { starWars = !starWars }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("SW", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            color = if (starWars) theme.accentColor else theme.textSecondary.copy(0.5f))
                    }
                    ActionButton("···", onOpenSettings, width = 36.dp, height = 20.dp)
                }

                // Right: rotations + hard drop
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RotateButton(onRotateXZ, 50.dp)
                    TapButton(ButtonIcon.DOWN, 50.dp) { onHardDrop() }
                    // Tilt rotate button
                    Box(
                        Modifier.size(42.dp).clip(CircleShape)
                            .background(theme.buttonPrimary.copy(0.7f))
                            .clickable { onRotateXY() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⤵", fontSize = 18.sp, color = theme.textPrimary.copy(0.8f))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun pieceUIColor(idx: Int): Color = when (idx) {
    1 -> Color(0xFF00E5FF); 2 -> Color(0xFFFFD600); 3 -> Color(0xFFAA00FF); 4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00); 6 -> Color(0xFFFF1744); 7 -> Color(0xFF2979FF); 8 -> Color(0xFFFF4081)
    else -> Color(0xFF888888)
}
