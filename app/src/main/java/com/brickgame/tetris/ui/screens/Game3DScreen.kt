package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.components.*
import com.brickgame.tetris.ui.theme.LocalGameTheme
import com.brickgame.tetris.gl.GLBoardView
import com.brickgame.tetris.gl.BoardGLSurfaceView
import kotlin.math.*

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
    onQuit: () -> Unit = {},
    material: PieceMaterial = PieceMaterial.CLASSIC
) {
    val theme = LocalGameTheme.current

    // Camera state — updated by touch callbacks from GLBoardView
    // These are READ by ViewCube, zoom buttons, sliders
    // WRITTEN by touch callbacks from the GL view
    var azimuth by remember { mutableFloatStateOf(35f) }
    var elevation by remember { mutableFloatStateOf(25f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var starWars by remember { mutableStateOf(false) }
    var showCamSettings by remember { mutableStateOf(false) }

    // Reference to the GL view for pushing camera changes from UI (sliders, zoom buttons)
    val glViewRef = remember { mutableStateOf<BoardGLSurfaceView?>(null) }

    /** Push camera to both Compose state and GL view. Used by sliders/presets/zoom buttons. */
    fun setCamera(az: Float = azimuth, el: Float = elevation, z: Float = zoom, px: Float = panX, py: Float = panY) {
        azimuth = az; elevation = el; zoom = z; panX = px; panY = py
        glViewRef.value?.setCameraExternal(az, el, z, px, py)
    }

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

            // 3D Board
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (starWars) {
                    StarWarsBoardCanvas(state, Modifier.fillMaxSize().padding(2.dp), true, theme.pixelOn)
                } else {
                    GLBoardView(
                        state = state,
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        showGhost = true,
                        cameraAngleY = azimuth,
                        cameraAngleX = elevation,
                        panOffsetX = panX,
                        panOffsetY = panY,
                        zoom = zoom,
                        themePixelOn = theme.pixelOn.value.toLong(),
                        themeBg = theme.backgroundColor.value.toLong(),
                        material = material,
                        onCameraChange = { az, el, z, px, py ->
                            azimuth = az; elevation = el; zoom = z; panX = px; panY = py
                        },
                        onViewCreated = { view -> glViewRef.value = view }
                    )
                }

                if (!starWars) {
                    ViewCube(azimuth, elevation, Modifier.align(Alignment.TopEnd).padding(8.dp).size(60.dp))
                    Column(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(0.1f))
                            .clickable { setCamera(z = (zoom + 0.15f).coerceAtMost(3f)) }, contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.7f))
                        }
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(0.1f))
                            .clickable { setCamera(z = (zoom - 0.15f).coerceAtLeast(0.3f)) }, contentAlignment = Alignment.Center) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.7f))
                        }
                    }
                }

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
                            ActionButton("LEAVE", onQuit, width = 160.dp, height = 34.dp, backgroundColor = Color(0xFFB91C1C))
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

                if (showCamSettings) {
                    Column(Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 8.dp)
                        .width(190.dp).background(Color.Black.copy(0.92f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Camera", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("✕", color = Color.White.copy(0.5f), fontSize = 16.sp,
                                modifier = Modifier.clickable { showCamSettings = false }.padding(4.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        CamSlider("Orbit", azimuth, -180f, 180f) { setCamera(az = it) }
                        CamSlider("Tilt", elevation, -85f, 85f) { setCamera(el = it) }
                        CamSlider("Zoom", zoom, 0.3f, 3f) { setCamera(z = it) }
                        CamSlider("Pan X", panX, -200f, 200f) { setCamera(px = it) }
                        CamSlider("Pan Y", panY, -200f, 200f) { setCamera(py = it) }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                            CamPreset("Front", Modifier.weight(1f)) { setCamera(0f, 15f, 1f, 0f, 0f) }
                            CamPreset("Side", Modifier.weight(1f)) { setCamera(90f, 20f, 1f, 0f, 0f) }
                            CamPreset("Top", Modifier.weight(1f)) { setCamera(35f, 70f, 1f, 0f, 0f) }
                        }
                        Spacer(Modifier.height(4.dp))
                        CamPreset("Reset All", Modifier.fillMaxWidth()) { setCamera(35f, 25f, 1f, 0f, 0f) }
                    }
                }
            }

            // Controls
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                DPad(
                    buttonSize = 44.dp, rotateInCenter = false,
                    onUpPress = { moveCameraRelative(0, 1) },
                    onDownPress = { moveCameraRelative(0, -1) },
                    onDownRelease = {},
                    onLeftPress = { moveCameraRelative(-1, 0) },
                    onLeftRelease = {},
                    onRightPress = { moveCameraRelative(1, 0) },
                    onRightRelease = {}
                )
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ActionButton("↻ SPIN", onRotateXZ, width = 56.dp, height = 36.dp)
                        ActionButton("↻ TILT", onRotateXY, width = 56.dp, height = 36.dp)
                    }
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

// ===== Star Wars Canvas =====
@Composable
private fun StarWarsBoardCanvas(state: Game3DState, modifier: Modifier, showGhost: Boolean, themeColor: Color) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val bw = Tetris3DGame.BOARD_W; val bh = Tetris3DGame.BOARD_H
        val vpX = w / 2f; val bottomY = h * 0.97f; val topY = h * 0.05f; val bottomHalfW = w * 0.45f; val topHalfW = w * 0.12f
        fun rp(row: Int): SWRowParams { val t = row.toFloat() / bh; val sy = bottomY + (topY - bottomY) * t; val hw = bottomHalfW + (topHalfW - bottomHalfW) * t; return SWRowParams(sy, vpX - hw, hw * 2f / bw, (bottomY - topY) / bh * (1f - t * 0.35f), 1f - t * 0.5f) }
        for (row in 0..bh) { val t = row.toFloat() / bh; val sy = bottomY + (topY - bottomY) * t; val hw = bottomHalfW + (topHalfW - bottomHalfW) * t; drawLine(themeColor.copy(0.06f * (1f - t * 0.6f)), Offset(vpX - hw, sy), Offset(vpX + hw, sy), 0.5f) }
        for (col in 0..bw) { val bxp = vpX - bottomHalfW + col * (bottomHalfW * 2 / bw); val txp = vpX - topHalfW + col * (topHalfW * 2 / bw); drawLine(themeColor.copy(0.04f), Offset(bxp, bottomY), Offset(txp, topY), 0.5f) }
        for (row in bh - 1 downTo 0) { val r = rp(row); for (col in 0 until bw) { var ci = 0; if (state.board.isNotEmpty() && row < state.board.size) { for (z in 0 until Tetris3DGame.BOARD_D) { if (z < state.board[row].size && col < state.board[row][z].size) { val c = state.board[row][z][col]; if (c > 0) { ci = c; break } } } }; if (ci > 0) drawSWBlock(r, col, swColor(ci, themeColor), r.alpha) } }
        val piece = state.currentPiece
        if (piece != null && showGhost && state.ghostY < piece.y) { for (b in piece.blocks) { val row = state.ghostY + b.y; val col = piece.x + b.x; if (row in 0 until bh && col in 0 until bw) drawSWBlock(rp(row), col, themeColor, 0.15f * rp(row).alpha) } }
        if (piece != null) { for (b in piece.blocks) { val row = piece.y + b.y; val col = piece.x + b.x; if (row in 0 until bh && col in 0 until bw) drawSWBlock(rp(row), col, swColor(piece.type.colorIndex, themeColor), rp(row).alpha) } }
    }
}
private data class SWRowParams(val y: Float, val leftX: Float, val cellW: Float, val cellH: Float, val alpha: Float)
private fun DrawScope.drawSWBlock(rp: SWRowParams, col: Int, color: Color, alpha: Float) {
    val x = rp.leftX + col * rp.cellW; val y = rp.y - rp.cellH; val cw = rp.cellW; val ch = rp.cellH; val a = alpha.coerceIn(0f, 1f); val d = ch * 0.18f
    drawRect(color.copy(a), Offset(x + 1, y), Size(cw - 2, ch - 1))
    drawPath(Path().apply { moveTo(x + 1, y); lineTo(x + cw - 1, y); lineTo(x + cw - 1 - d, y - d); lineTo(x + 1 + d, y - d); close() }, swBr(color, 1.1f).copy(a * 0.7f))
    drawPath(Path().apply { moveTo(x + cw - 1, y); lineTo(x + cw - 1, y + ch - 1); lineTo(x + cw - 1 - d, y + ch - 1 - d); lineTo(x + cw - 1 - d, y - d); close() }, swDk(color, 0.5f).copy(a * 0.6f))
    drawRect(Color.White.copy(0.18f * a), Offset(x + 2, y + 1), Size(cw * 0.25f, 2f))
}
private fun swColor(idx: Int, tc: Color): Color = when (idx) { 1 -> Color(0xFF00E5FF); 2 -> Color(0xFFFFD600); 3 -> Color(0xFFAA00FF); 4 -> Color(0xFF00E676); 5 -> Color(0xFFFF6D00); 6 -> Color(0xFFFF1744); 7 -> Color(0xFF2979FF); 8 -> Color(0xFFFF4081); else -> tc }
private fun swDk(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f, c.alpha)
private fun swBr(c: Color, f: Float) = Color(minOf(c.red * f, 1f), minOf(c.green * f, 1f), minOf(c.blue * f, 1f), c.alpha)

@Composable
private fun MiniToggle(label: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(if (active) color.copy(0.3f) else Color.White.copy(0.05f))
        .clickable { onClick() }.padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (active) color else Color.White.copy(0.4f))
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
