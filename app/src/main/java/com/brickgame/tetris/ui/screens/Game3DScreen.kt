package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * 3D Tetris game screen — isometric board + swipe/button controls.
 * Controls: 4-directional movement (X/Z), rotate XZ, rotate XY, hard drop.
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
    onOpenSettings: () -> Unit
) {
    val theme = LocalGameTheme.current

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Info bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                // Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", fontSize = 9.sp, color = Color.White.copy(0.5f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(state.score.toString().padStart(7, '0'), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = Color(0xFF22C55E), letterSpacing = 1.sp)
                }
                // Level + Layers
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEVEL", fontSize = 9.sp, color = Color.White.copy(0.5f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("${state.level}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFF22C55E))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LAYERS", fontSize = 9.sp, color = Color.White.copy(0.5f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("${state.layers}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFF22C55E))
                }
                // Hold piece
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HOLD", fontSize = 9.sp, color = Color.White.copy(0.5f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(32.dp).background(Color.White.copy(0.05f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        if (state.holdPiece != null) {
                            Text(state.holdPiece.displayName.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = pieceUIColor(state.holdPiece.colorIndex).copy(if (state.holdUsed) 0.4f else 1f))
                        }
                    }
                }
            }

            // 3D Board — takes most of the screen
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Tetris3DBoard(state, Modifier.fillMaxSize().padding(horizontal = 4.dp))

                // Menu / Game Over overlay
                when (state.status) {
                    GameStatus.MENU -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("3D", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                                color = Color(0xFF22C55E), letterSpacing = 8.sp)
                            Text("TETRIS", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(0.8f), letterSpacing = 6.sp)
                            Spacer(Modifier.height(32.dp))
                            ActionButton("START", onStart, width = 140.dp, height = 48.dp)
                        }
                    }
                    GameStatus.GAME_OVER -> {
                        Column(
                            Modifier.background(Color.Black.copy(0.8f), RoundedCornerShape(16.dp)).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("GAME OVER", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFFFF4444))
                            Spacer(Modifier.height(8.dp))
                            Text("Score: ${state.score}", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            Text("Level: ${state.level} · Layers: ${state.layers}", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(0.6f))
                            Spacer(Modifier.height(20.dp))
                            ActionButton("PLAY AGAIN", onStart, width = 140.dp, height = 42.dp)
                        }
                    }
                    GameStatus.PAUSED -> {
                        Column(
                            Modifier.background(Color.Black.copy(0.8f), RoundedCornerShape(16.dp)).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("PAUSED", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color.White)
                            Spacer(Modifier.height(20.dp))
                            ActionButton("RESUME", { onPause() }, width = 140.dp, height = 42.dp)
                        }
                    }
                    else -> {}
                }
            }

            // Next pieces row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                Arrangement.Center, Alignment.CenterVertically
            ) {
                Text("NEXT  ", fontSize = 9.sp, color = Color.White.copy(0.4f), fontFamily = FontFamily.Monospace)
                state.nextPieces.forEachIndexed { i, type ->
                    Box(
                        Modifier.size(28.dp).background(Color.White.copy(0.05f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(type.displayName.take(2), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = pieceUIColor(type.colorIndex).copy(if (i == 0) 1f else 0.5f), fontFamily = FontFamily.Monospace)
                    }
                    if (i < state.nextPieces.size - 1) Spacer(Modifier.width(6.dp))
                }
            }

            // Controls
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                // Left side: XZ movement D-Pad
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Z- (away from camera)
                    TapButton(ButtonIcon.UP, 48.dp) { onMoveZ(1) }
                    Row {
                        // X- (left)
                        HoldButton(ButtonIcon.LEFT, 48.dp, onPress = { onMoveX(-1) }, onRelease = {})
                        Spacer(Modifier.width(28.dp))
                        // X+ (right)
                        HoldButton(ButtonIcon.RIGHT, 48.dp, onPress = { onMoveX(1) }, onRelease = {})
                    }
                    // Z+ (toward camera)
                    TapButton(ButtonIcon.DOWN, 48.dp) { onMoveZ(-1) }
                }

                // Center: action buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionButton("HOLD", onHold, width = 62.dp, height = 28.dp)
                    ActionButton(
                        if (state.status == GameStatus.MENU) "START" else "PAUSE",
                        { if (state.status == GameStatus.MENU) onStart() else onPause() },
                        width = 62.dp, height = 28.dp
                    )
                    ActionButton("···", onOpenSettings, width = 40.dp, height = 22.dp)
                }

                // Right side: rotations + hard drop
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Rotate XZ (horizontal spin)
                    RotateButton(onRotateXZ, 52.dp)
                    // Hard drop
                    TapButton(ButtonIcon.DOWN, 52.dp) { onHardDrop() }
                    // Rotate XY (tilt forward/back) — smaller button
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⟳", fontSize = 18.sp, color = Color.White.copy(0.8f),
                            modifier = Modifier.padding(0.dp).then(
                                Modifier.clip(CircleShape)
                            ))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun pieceUIColor(idx: Int): Color = when (idx) {
    1 -> Color(0xFF00E5FF)
    2 -> Color(0xFFFFD600)
    3 -> Color(0xFFAA00FF)
    4 -> Color(0xFF00E676)
    5 -> Color(0xFFFF6D00)
    6 -> Color(0xFFFF1744)
    7 -> Color(0xFF2979FF)
    8 -> Color(0xFFFF4081)
    else -> Color(0xFF888888)
}
