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
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brickgame.tetris.gl.ArBoardView
import com.brickgame.tetris.gl.ArHeat
import com.brickgame.tetris.gl.ArHeatInfo
import com.brickgame.tetris.ui.brand.Bw
import com.brickgame.tetris.ui.brand.BwDarkSystemBars
import com.brickgame.tetris.ui.brand.BwGameOverOverlay
import com.brickgame.tetris.ui.brand.BwPadButton
import com.brickgame.tetris.ui.brand.BwPauseOverlay
import com.brickgame.tetris.ui.brand.BwRollingScore
import com.brickgame.tetris.ui.brand.BwType
import com.brickgame.tetris.ui.brand.Glyph
import com.brickgame.tetris.ui.brand.GlyphIcon
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.brickgame.tetris.gl.ArStatus
import com.brickgame.tetris.gl.ArSupport
import com.brickgame.tetris.gl.createArSession
import com.brickgame.tetris.gl.rememberArSupport
import com.google.ar.core.Session
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
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
    material: PieceMaterial = PieceMaterial.CLASSIC,
    /** Live piece (not the last composed one) for AR drag stepping */
    currentPiece: () -> Piece3DState? = { state.currentPiece },
    /** Battery °C at which AR switches itself off (player setting, capped below the hard limit) */
    arHeatLimit: Int = 43,
    onArHeatLimit: (Int) -> Unit = {}
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

    // Motion view: the phone's orientation steers the camera. Touch still works on top of it:
    // the camera = base (touch/sliders) + phone motion.
    val hasMotionSensor = rememberHasMotionSensor()
    var motionView by remember { mutableStateOf(false) }
    var motionRecenter by remember { mutableIntStateOf(0) }
    val motionBase = remember { FloatArray(2) }   // [azimuth, elevation] the motion is added to
    val motionLast = remember { FloatArray(2) }   // last applied motion offset [az, el] in degrees

    // ===== AR mode (only offered where ARCore is supported) =====
    val context = LocalContext.current
    val arSupport = rememberArSupport()
    var arSession by remember { mutableStateOf<Session?>(null) }
    var arStatus by remember { mutableStateOf(ArStatus.STARTING) }
    var arReplace by remember { mutableIntStateOf(0) }
    var arMessage by remember { mutableStateOf<String?>(null) }
    var arInstallPending by remember { mutableStateOf(false) }
    var arCell by remember { mutableFloatStateOf(0.03f) }
    var arHeat by remember { mutableStateOf<ArHeatInfo?>(null) }
    val heatLimit by rememberUpdatedState(arHeatLimit.coerceAtMost(AR_HARD_LIMIT_C - 1))

    /** Leave AR when the phone is too warm: the player's limit, or the safety net that always applies. */
    fun onHeatInfo(h: ArHeatInfo) {
        arHeat = h
        val t = h.batteryC
        val hardStop = h.thermal == ArHeat.CRITICAL || (t != null && t >= AR_HARD_LIMIT_C)
        val userStop = t != null && t >= heatLimit
        val unknownTempButHot = t == null && h.thermal == ArHeat.HOT
        if (hardStop || userStop || unknownTempButHot) {
            arSession = null
            if (state.status == GameStatus.PLAYING) onPause()
            arMessage = (if (t != null) "AR paused at %.1f°C to protect your phone".format(t) else "AR paused — your phone is too hot") +
                (if (h.charging) " (charging adds heat)." else ".") + " Your game is paused in 3D view."
        }
    }

    /** AR drag: step the piece one cell per axis toward the board cell under the finger. */
    fun dragPieceTo(bx: Float, bz: Float) {
        val p = currentPiece() ?: return
        val cx = p.x + p.blocks.map { it.x }.average().toFloat() + 0.5f
        val cz = p.z + p.blocks.map { it.z }.average().toFloat() + 0.5f
        val dx = kotlin.math.round(bx - cx).toInt()
        val dz = kotlin.math.round(bz - cz).toInt()
        if (dx != 0) onMoveX(if (dx > 0) 1 else -1)
        if (dz != 0) onMoveZ(if (dz > 0) 1 else -1)
    }
    val arOn = arSession != null

    fun startAr(userRequestedInstall: Boolean) {
        val activity = context as? Activity ?: return
        try {
            val session = createArSession(activity, userRequestedInstall)
            if (session == null) { arInstallPending = true; return }   // Play Services for AR is installing
            arStatus = ArStatus.STARTING
            arMessage = null
            arSession = session
            if (state.status == GameStatus.PLAYING) onPause()   // place the well first, then resume
        } catch (e: com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException) {
            arMessage = "AR needs Google Play Services for AR"
        } catch (e: com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException) {
            arMessage = "This device doesn't support AR"
        } catch (e: Exception) {
            arMessage = "AR couldn't start on this device"
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startAr(true) else arMessage = "AR needs the camera to see your table"
    }
    fun toggleAr() {
        if (arOn) { arSession = null; return }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) startAr(true) else cameraPermission.launch(Manifest.permission.CAMERA)
    }
    // Coming back from the Play Services for AR install screen: try again (without re-prompting)
    val arLifecycle = LocalLifecycleOwner.current
    DisposableEffect(arLifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && arInstallPending) { arInstallPending = false; startAr(false) }
        }
        arLifecycle.lifecycle.addObserver(observer)
        onDispose { arLifecycle.lifecycle.removeObserver(observer) }
    }

    // Reference to the GL view for pushing camera changes from UI (sliders, zoom buttons)
    val glViewRef = remember { mutableStateOf<BoardGLSurfaceView?>(null) }

    /** Push camera to both Compose state and GL view. Used by sliders/presets/zoom buttons. */
    fun setCamera(az: Float = azimuth, el: Float = elevation, z: Float = zoom, px: Float = panX, py: Float = panY) {
        azimuth = az; elevation = el; zoom = z; panX = px; panY = py
        glViewRef.value?.setCameraExternal(az, el, z, px, py)
    }

    fun setMotionView(on: Boolean) {
        motionView = on
        motionBase[0] = azimuth; motionBase[1] = elevation
        motionLast[0] = 0f; motionLast[1] = 0f
        motionRecenter++
    }

    MotionViewSensor(enabled = motionView && !starWars && !arOn, recenterKey = motionRecenter) { yawDeg, pitchDeg ->
        // Turning the phone left walks the camera round to the board's left side; tilting the
        // top edge towards you looks down into the well.
        val dAz = -yawDeg * MOTION_GAIN
        val dEl = pitchDeg * MOTION_GAIN
        motionLast[0] = dAz; motionLast[1] = dEl
        setCamera(
            az = com.brickgame.tetris.gl.TiltMath.wrapDegrees(motionBase[0] + dAz),
            el = (motionBase[1] + dEl).coerceIn(-10f, 85f)
        )
    }

    fun moveCameraRelative(screenDx: Int, screenDz: Int) {
        if (starWars) { onMoveX(screenDx); onMoveZ(screenDz); return }
        // Snap azimuth to nearest 90° for controls — prevents confusing diagonal mappings
        // Visual camera rotation stays smooth, only CONTROLS snap to cardinal directions
        val snapped = (Math.round(azimuth / 90f) * 90f).toDouble()
        val rad = Math.toRadians(snapped)
        val cosA = cos(rad).toFloat(); val sinA = sin(rad).toFloat()
        val worldX = screenDx * cosA - screenDz * sinA
        val worldZ = screenDx * sinA + screenDz * cosA
        if (abs(worldX) >= abs(worldZ)) onMoveX(if (worldX > 0) 1 else -1)
        else onMoveZ(if (worldZ > 0) 1 else -1)
    }

    BwDarkSystemBars()
    Box(Modifier.fillMaxSize().background(Bw.Ground).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Info bar (Brickwell design)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Hud3DBox(Modifier.width(58.dp)) {
                    Text("HOLD", style = BwType.Overline.copy(fontSize = 10.sp, letterSpacing = 1.sp))
                    Mini3DPiecePreview(state.holdPiece, Modifier.size(28.dp), Bw.Cyan, if (state.holdUsed) 0.3f else 1f)
                }
                Hud3DBox(Modifier.weight(1f)) {
                    BwRollingScore(state.score)
                    Text("LV ${state.level}  ·  ${state.layers} LAYERS", style = BwType.Small.copy(fontSize = 11.sp))
                }
                Hud3DBox(Modifier.width(74.dp)) {
                    Text("NEXT", style = BwType.Overline.copy(fontSize = 10.sp, letterSpacing = 1.sp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        state.nextPieces.take(2).forEachIndexed { i, type ->
                            Mini3DPiecePreview(type, Modifier.size(if (i == 0) 28.dp else 20.dp), Bw.Cyan, if (i == 0) 1f else 0.4f)
                        }
                    }
                }
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Bw.Surface)
                    .border(1.dp, Bw.Line, RoundedCornerShape(14.dp))
                    .clickable(onClickLabel = "Pause") { onPause() }, contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.PAUSE, Bw.Text, 18.dp)
                }
            }

            // 3D Board
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val session = arSession
                if (session != null) {
                    ArBoardView(
                        session = session,
                        state = state,
                        material = material,
                        themeColor = theme.pixelOn.toArgb().toLong() and 0xFFFFFFFFL,
                        replaceKey = arReplace,
                        cellMeters = arCell,
                        onCellMeters = { arCell = it },
                        onStatus = { arStatus = it },
                        // Walking around the well turns the D-pad with you
                        onViewAngle = { az, el -> azimuth = az; elevation = el },
                        onPieceDrag = { bx, bz -> dragPieceTo(bx, bz) },
                        onTap = onRotateXZ,
                        onHardDrop = onHardDrop,
                        onHeat = { onHeatInfo(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (starWars) {
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
                        // Color.value packs ARGB in the high 32 bits — convert explicitly
                        themePixelOn = theme.pixelOn.toArgb().toLong() and 0xFFFFFFFFL,
                        themeBg = theme.backgroundColor.toArgb().toLong() and 0xFFFFFFFFL,
                        material = material,
                        onCameraChange = { az, el, z, px, py ->
                            azimuth = az; elevation = el; zoom = z; panX = px; panY = py
                            // A touch drag moves the base the phone motion is added to
                            if (motionView) { motionBase[0] = az - motionLast[0]; motionBase[1] = el - motionLast[1] }
                        },
                        onViewCreated = { view -> glViewRef.value = view }
                    )
                }

                if (arOn) {
                    Column(Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ArStatusBar(arStatus, Modifier) { arReplace++ }
                        if (arStatus == ArStatus.PLACED) {
                            // Size presets: table-top, big, or a well you can stand inside
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Table" to 0.03f, "Big" to 0.08f, "Room" to 0.2f).forEach { (label, cell) ->
                                    val on = kotlin.math.abs(arCell - cell) < 0.004f
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        color = if (on) Color(0xFF0B0E1A) else Color.White,
                                        modifier = Modifier.background(if (on) Color(0xFF22D3EE) else Color(0xE6141A2E), RoundedCornerShape(10.dp))
                                            .clickable { arCell = cell }.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                            Text("Drag the piece · tap to spin · flick down to drop · two fingers: size, turn, move",
                                color = Color.White.copy(0.85f), fontSize = 11.sp,
                                modifier = Modifier.background(Color(0xB3141A2E), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                        arHeat?.let { h -> HeatChip(h, heatLimit) { onArHeatLimit(nextHeatLimit(heatLimit)) } }
                    }
                }
                arMessage?.let { msg ->
                    Text(msg, color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                            .background(Color(0xE6141A2E), RoundedCornerShape(12.dp))
                            .clickable { arMessage = null }.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                if (!starWars && !arOn) {
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
                            Text("BRICKWELL", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = theme.textPrimary.copy(0.8f), letterSpacing = 6.sp)
                            Spacer(Modifier.height(28.dp))
                            ActionButton("START", onStart, width = 140.dp, height = 48.dp)
                            Spacer(Modifier.height(12.dp))
                            ActionButton("SETTINGS", onOpenSettings, width = 140.dp, height = 38.dp)
                        }
                    }
                    GameStatus.PAUSED -> BwPauseOverlay(onResume = { onPause() }, onSettings = onOpenSettings, onQuit = onQuit)
                    GameStatus.GAME_OVER -> BwGameOverOverlay(
                        score = state.score, level = state.level, lines = state.layers, highScore = Int.MAX_VALUE,
                        maxCombo = 0, backToBack = 0, elapsedMs = 0L,
                        onAgain = onStart, onLeave = onQuit, linesLabel = "LAYERS"
                    )
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
                        CamPreset("Reset All", Modifier.fillMaxWidth()) {
                            setCamera(35f, 25f, 1f, 0f, 0f)
                            if (motionView) setMotionView(true)  // re-centre on the current pose
                        }
                        if (hasMotionSensor) {
                            Spacer(Modifier.height(6.dp))
                            CamPreset(if (motionView) "Motion view: ON" else "Motion view: OFF", Modifier.fillMaxWidth()) {
                                setMotionView(!motionView)
                            }
                            if (motionView) {
                                Spacer(Modifier.height(4.dp))
                                CamPreset("Re-centre", Modifier.fillMaxWidth()) { setMotionView(true) }
                            }
                        }
                    }
                }
            }

            // View & mode chips
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (arSupport == ArSupport.SUPPORTED) ModeChip("AR", arOn, Bw.Cyan) { toggleAr() }
                if (hasMotionSensor && !arOn) ModeChip("Motion view", motionView, Bw.Violet) { setMotionView(!motionView) }
                if (!arOn) ModeChip("Camera", showCamSettings, Bw.Amber) { showCamSettings = !showCamSettings }
                ModeChip("Freeze gravity", !state.autoGravity, Color(0xFF38BDF8)) { onToggleGravity() }
                if (!arOn) ModeChip("Flat view", starWars, Bw.Lime) { starWars = !starWars }
            }

            // Controls (Brickwell design)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Left: direction pad (follows the camera / where you stand in AR)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val padShape = RoundedCornerShape(16.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Spacer(Modifier.size(52.dp))
                        BwPadButton("Move away", Modifier.size(52.dp), padShape, Bw.Raised, Bw.LineStrong, { moveCameraRelative(0, 1) }) { GlyphIcon(Glyph.UP, Bw.Cyan, 22.dp) }
                        Spacer(Modifier.size(52.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        BwPadButton("Move left", Modifier.size(52.dp), padShape, Bw.Raised, Bw.LineStrong, { moveCameraRelative(-1, 0) }) { GlyphIcon(Glyph.LEFT, Bw.Cyan, 22.dp) }
                        // Compass: which way "away" points
                        Canvas(Modifier.size(52.dp)) {
                            val rad = Math.toRadians(-azimuth.toDouble()).toFloat()
                            val cx = size.width / 2f; val cy = size.height / 2f; val r = size.width * 0.3f
                            drawCircle(Bw.Line, r, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                            drawLine(Bw.TextMuted, Offset(cx, cy), Offset(cx + sin(rad) * r, cy - cos(rad) * r), 3f)
                            drawCircle(Bw.Cyan, 4f, Offset(cx + sin(rad) * r, cy - cos(rad) * r))
                        }
                        BwPadButton("Move right", Modifier.size(52.dp), padShape, Bw.Raised, Bw.LineStrong, { moveCameraRelative(1, 0) }) { GlyphIcon(Glyph.RIGHT, Bw.Cyan, 22.dp) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Spacer(Modifier.size(52.dp))
                        BwPadButton("Move closer", Modifier.size(52.dp), padShape, Bw.Raised, Bw.LineStrong, { moveCameraRelative(0, -1) }) { GlyphIcon(Glyph.DOWN, Bw.Cyan, 22.dp) }
                        Spacer(Modifier.size(52.dp))
                    }
                }
                // Right: hold, spin, tilt, drops
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        BwPadButton("Hold piece", Modifier.size(52.dp), RoundedCornerShape(16.dp), Bw.Raised, Bw.LineStrong, onHold) {
                            Text("HOLD", style = BwType.Overline.copy(color = Bw.Text, fontSize = 10.sp, letterSpacing = 1.sp))
                        }
                        RoundAction("Spin", Glyph.ROTATE, Bw.Cyan, onRotateXZ)
                        RoundAction("Tilt", Glyph.TILT, Bw.Violet, onRotateXY)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BwPadButton("Soft drop", Modifier.size(64.dp, 48.dp), RoundedCornerShape(16.dp), Bw.Raised, Bw.LineStrong, { onSoftDrop() }) {
                            GlyphIcon(Glyph.DOWN, Bw.Cyan, 22.dp)
                        }
                        BwPadButton("Hard drop", Modifier.size(120.dp, 48.dp), RoundedCornerShape(16.dp), Bw.Pink, null, onHardDrop) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlyphIcon(Glyph.DROP, Bw.Ground, 18.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("DROP", style = BwType.Button.copy(fontSize = 15.sp, letterSpacing = 2.sp, color = Bw.Ground))
                            }
                        }
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

// ===== Motion view =====

/** Degrees of camera movement per degree of phone movement — small wrist turns reveal the sides. */
private const val MOTION_GAIN = 1.5f

@Composable
private fun rememberHasMotionSensor(): Boolean {
    val context = LocalContext.current
    return remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ||
            sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }
}

/**
 * Reports how far the phone has turned (yaw) and tilted (pitch), in degrees, relative to its
 * orientation when [enabled] became true or [recenterKey] changed.
 *
 * Uses the game rotation vector (gyroscope + accelerometer, no compass, so nearby magnets or
 * metal don't make the view drift); falls back to the regular rotation vector. No camera and no
 * permission are needed.
 */
@Composable
private fun MotionViewSensor(enabled: Boolean, recenterKey: Int, onAngles: (yawDeg: Float, pitchDeg: Float) -> Unit) {
    val context = LocalContext.current
    val latestOnAngles by rememberUpdatedState(onAngles)
    DisposableEffect(enabled, recenterKey) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (!enabled || sm == null || sensor == null) return@DisposableEffect onDispose {}

        val ref = FloatArray(9); val cur = FloatArray(9); val out = FloatArray(2)
        var hasRef = false
        var yaw = 0f; var pitch = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Some devices report 5 values; the matrix conversion only wants the first 4
                val v = if (event.values.size > 4) event.values.copyOf(4) else event.values
                if (!hasRef) {
                    SensorManager.getRotationMatrixFromVector(ref, v)
                    hasRef = true
                    return
                }
                SensorManager.getRotationMatrixFromVector(cur, v)
                com.brickgame.tetris.gl.TiltMath.relativeYawPitch(ref, cur, out)
                // Light low-pass filter against hand tremor
                yaw += (Math.toDegrees(out[0].toDouble()).toFloat() - yaw) * 0.35f
                pitch += (Math.toDegrees(out[1].toDouble()).toFloat() - pitch) * 0.35f
                latestOnAngles(yaw, pitch)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
}

// ===== AR status =====

@Composable
private fun ArStatusBar(status: ArStatus, modifier: Modifier, onReplace: () -> Unit) {
    val text = when (status) {
        ArStatus.STARTING -> "Starting camera…"
        ArStatus.SEARCHING -> "Move your phone slowly over a table"
        ArStatus.READY_TO_PLACE -> "Tap the table to place the well"
        ArStatus.PLACED -> null
        ArStatus.TRACKING_LOST -> "Lost track — point back at the table"
        ArStatus.FAILED -> "AR stopped. Turn AR off and on again"
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (text != null) {
            Text(text, color = Color.White, fontSize = 13.sp,
                modifier = Modifier.background(Color(0xE6141A2E), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp))
        } else {
            Text("Move", color = Color(0xFF0B0E1A), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color(0xFF22D3EE), RoundedCornerShape(10.dp)).clickable { onReplace() }
                    .padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

// ===== Brickwell pieces for the 3D screen =====

/** Above this battery temperature AR always switches off, whatever the player's limit. */
private const val AR_HARD_LIMIT_C = 46

/** Player-selectable AR heat limits, cycled by tapping the temperature chip. */
private fun nextHeatLimit(current: Int): Int = if (current >= AR_HARD_LIMIT_C - 1) 39 else current + 1

@Composable
private fun HeatChip(h: ArHeatInfo, limit: Int, onCycleLimit: () -> Unit) {
    val t = h.batteryC
    val color = when {
        t == null -> if (h.thermal >= ArHeat.WARM) Bw.Amber else Bw.Lime
        t >= limit - 1 -> Bw.Pink
        t >= limit - 3 || h.thermal >= ArHeat.WARM -> Bw.Amber
        else -> Bw.Lime
    }
    val text = buildString {
        append(if (t != null) "%.1f°C".format(t) else "Temp n/a")
        append("  ·  AR stops at ${limit}°C")
        if (h.charging) append("  ·  charging")
    }
    Text(text, color = Bw.Ground, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color, RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Change AR temperature limit") { onCycleLimit() }
            .padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
private fun Hud3DBox(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.heightIn(min = 60.dp).clip(RoundedCornerShape(14.dp)).background(Bw.Surface)
        .border(1.dp, Bw.Line, RoundedCornerShape(14.dp)).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically), content = content)
}

@Composable
private fun ModeChip(label: String, on: Boolean, color: Color, onClick: () -> Unit) {
    Text(label, color = if (on) Bw.Ground else Bw.TextSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (on) color else Bw.Surface)
            .border(1.dp, if (on) color else Bw.Line, RoundedCornerShape(10.dp))
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp))
}

@Composable
private fun RoundAction(label: String, glyph: Glyph, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BwPadButton(label, Modifier.size(60.dp), CircleShape, color, null, onClick) { GlyphIcon(glyph, Bw.Ground, 26.dp) }
        Text(label.uppercase(), style = BwType.Overline.copy(fontSize = 9.sp, letterSpacing = 1.sp))
    }
}
