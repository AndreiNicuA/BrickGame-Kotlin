package com.brickgame.tetris

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.input.GamepadController
import com.brickgame.tetris.ui.layout.FreeformEditorScreen
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.components.PieceMaterial
import com.brickgame.tetris.ui.screens.Game3DScreen
import com.brickgame.tetris.ui.screens.GameScreen
import com.brickgame.tetris.ui.screens.GameViewModel
import com.brickgame.tetris.ui.screens.SettingsScreen
import com.brickgame.tetris.ui.theme.BrickGameTheme
import kotlinx.coroutines.delay
import kotlin.math.*

class MainActivity : ComponentActivity() {

    private val gamepad = GamepadController()
    private var vmRef: GameViewModel? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (gamepad.handleKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (gamepad.handleKeyUp(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (gamepad.handleMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GameViewModel = viewModel()
            vmRef = vm

            // Wire gamepad controller to game actions
            LaunchedEffect(Unit) {
                gamepad.onAction = { action ->
                    val activeLayout = if (vm.portraitLayout.value == LayoutPreset.PORTRAIT_3D) "3D" else "2D"
                    val status2D = vm.gameState.value.status
                    val status3D = vm.game3DState.value.status
                    val is3D = activeLayout == "3D"
                    val isPlaying = if (is3D) status3D == GameStatus.PLAYING else status2D == GameStatus.PLAYING

                    when (action) {
                        GamepadController.Action.MOVE_LEFT -> if (isPlaying) { if (is3D) vm.move3DX(-1) else vm.moveLeft() }
                        GamepadController.Action.MOVE_RIGHT -> if (isPlaying) { if (is3D) vm.move3DX(1) else vm.moveRight() }
                        GamepadController.Action.SOFT_DROP -> if (isPlaying) { if (is3D) vm.softDrop3D() else vm.softDrop() }
                        GamepadController.Action.MOVE_UP -> if (isPlaying && is3D) vm.move3DX(0) // Up in 2D not used
                        GamepadController.Action.MOVE_Z_FORWARD -> if (isPlaying && is3D) vm.move3DZ(1)
                        GamepadController.Action.MOVE_Z_BACKWARD -> if (isPlaying && is3D) vm.move3DZ(-1)
                        GamepadController.Action.ROTATE_XZ -> if (isPlaying) { if (is3D) vm.rotate3DXZ() else vm.rotate() }
                        GamepadController.Action.ROTATE_XY -> if (isPlaying) { if (is3D) vm.rotate3DXY() else vm.rotateCounterClockwise() }
                        GamepadController.Action.HARD_DROP -> if (isPlaying) { if (is3D) vm.hardDrop3D() else vm.hardDrop() }
                        GamepadController.Action.HOLD -> if (isPlaying) { if (is3D) vm.hold3D() else vm.holdPiece() }
                        GamepadController.Action.PAUSE -> {
                            if (is3D) {
                                when (status3D) {
                                    GameStatus.PLAYING -> vm.pause3D()
                                    GameStatus.PAUSED -> vm.resume3D()
                                    GameStatus.MENU -> vm.start3DGame()
                                    else -> {}
                                }
                            } else {
                                when (status2D) {
                                    GameStatus.PLAYING -> vm.pauseGame()
                                    GameStatus.PAUSED -> vm.resumeGame()
                                    GameStatus.MENU -> vm.startGame()
                                    else -> {}
                                }
                            }
                        }
                        GamepadController.Action.SETTINGS -> vm.openSettings()
                        GamepadController.Action.TOGGLE_GRAVITY -> if (is3D) vm.toggle3DGravity()
                    }
                }
            }

            val dataLoaded by vm.dataLoaded.collectAsState()
            val gs by vm.gameState.collectAsState()
            val ui by vm.uiState.collectAsState()
            val theme by vm.currentTheme.collectAsState()
            val portraitLayout by vm.portraitLayout.collectAsState()
            val landscapeLayout by vm.landscapeLayout.collectAsState()
            val dpadStyle by vm.dpadStyle.collectAsState()
            val ghost by vm.ghostPieceEnabled.collectAsState()
            val diff by vm.difficulty.collectAsState()
            val mode by vm.gameMode.collectAsState()
            val anim by vm.animationStyle.collectAsState()
            val animDur by vm.animationDuration.collectAsState()
            val sound by vm.soundEnabled.collectAsState()
            val vib by vm.vibrationEnabled.collectAsState()
            val multiColor by vm.multiColorEnabled.collectAsState()
            val pieceMaterial by vm.pieceMaterial.collectAsState()
            val controllerEnabled by vm.controllerEnabled.collectAsState()
            val controllerDeadzone by vm.controllerDeadzone.collectAsState()
            // General App Settings
            val appThemeMode by vm.appThemeMode.collectAsState()
            val keepScreenOn by vm.keepScreenOn.collectAsState()
            val orientationLock by vm.orientationLock.collectAsState()
            val immersiveMode by vm.immersiveMode.collectAsState()
            val frameRateTarget by vm.frameRateTarget.collectAsState()
            val batterySaver by vm.batterySaver.collectAsState()
            val highContrast by vm.highContrast.collectAsState()
            val uiScale by vm.uiScale.collectAsState()

            // Sync controller settings to gamepad handler
            LaunchedEffect(controllerEnabled, controllerDeadzone) {
                gamepad.enabled = controllerEnabled
                gamepad.deadzone = controllerDeadzone
            }

            // Apply Keep Screen On setting
            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Apply Orientation Lock setting
            LaunchedEffect(orientationLock) {
                requestedOrientation = when (orientationLock) {
                    "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            // Apply Immersive Mode setting
            LaunchedEffect(immersiveMode) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (immersiveMode) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            val name by vm.playerName.collectAsState()
            val hs by vm.highScore.collectAsState()
            val history by vm.scoreHistory.collectAsState()
            val customThemes by vm.customThemes.collectAsState()
            val editingTheme by vm.editingTheme.collectAsState()
            val customLayouts by vm.customLayouts.collectAsState()
            val editingLayout by vm.editingLayout.collectAsState()
            val activeCustomLayout by vm.activeCustomLayout.collectAsState()
            val profile by vm.playerProfile.collectAsState()
            val freeformEditMode by vm.freeformEditMode.collectAsState()
            val game3DState by vm.game3DState.collectAsState()

            // 3-phase launch: LOADING → WELCOME → READY
            var phase by remember { mutableStateOf(0) } // 0=loading, 1=welcome, 2=ready

            // Phase transitions
            LaunchedEffect(dataLoaded) {
                if (dataLoaded && phase == 0) {
                    phase = 1 // show welcome
                    delay(2000) // welcome visible for 2s
                    phase = 2 // show content
                }
            }

            // Animated alphas for each phase
            val splashAlpha by animateFloatAsState(
                targetValue = if (phase == 0) 1f else 0f,
                animationSpec = tween(500, easing = EaseOut), label = "splash"
            )
            val welcomeAlpha by animateFloatAsState(
                targetValue = if (phase == 1) 1f else 0f,
                animationSpec = tween(if (phase == 1) 600 else 500, easing = if (phase == 1) EaseOut else EaseIn),
                label = "welcome"
            )
            val contentAlpha by animateFloatAsState(
                targetValue = if (phase == 2) 1f else 0f,
                animationSpec = tween(700, easing = EaseOut), label = "content"
            )

            val config = LocalConfiguration.current
            val isLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val activeLayout = if (isLandscape) landscapeLayout else portraitLayout
            val is3D = activeLayout == LayoutPreset.PORTRAIT_3D

            BrickGameTheme(gameTheme = theme, appThemeMode = appThemeMode) {
                Box(Modifier.fillMaxSize()) {
                    // Layer 1: Splash — rotating cube + falling pieces (visible during loading)
                    if (splashAlpha > 0.01f) {
                        SplashScreen(Modifier.fillMaxSize().alpha(splashAlpha))
                    }

                    // Layer 2: Welcome — "Welcome, Player" over the falling pieces background
                    if (welcomeAlpha > 0.01f) {
                        WelcomeScreen(
                            playerName = name,
                            modifier = Modifier.fillMaxSize().alpha(welcomeAlpha)
                        )
                    }

                    // Layer 3: Main content — landing page, game, settings
                    if (contentAlpha > 0.01f) {
                        Box(Modifier.fillMaxSize().alpha(contentAlpha)) {
                BackHandler(enabled = freeformEditMode) { vm.exitFreeformEditMode() }
                BackHandler(enabled = ui.showSettings && !freeformEditMode) {
                    when (ui.settingsPage) {
                        GameViewModel.SettingsPage.MAIN -> vm.closeSettings()
                        GameViewModel.SettingsPage.THEME_EDITOR -> vm.navigateSettings(GameViewModel.SettingsPage.THEME)
                        GameViewModel.SettingsPage.LAYOUT_EDITOR -> vm.navigateSettings(GameViewModel.SettingsPage.LAYOUT)
                        else -> vm.navigateSettings(GameViewModel.SettingsPage.MAIN)
                    }
                }

                when {freeformEditMode -> {
                        FreeformEditorScreen(
                            elements = profile.freeformElements,
                            onElementUpdated = vm::updateFreeformElement,
                            onElementAdded = vm::addFreeformElement,
                            onElementRemoved = vm::removeFreeformElement,
                            onReset = vm::resetFreeformElements,
                            onDone = vm::exitFreeformEditMode
                        )
                    }

                    ui.showSettings -> {
                        SettingsScreen(
                            page = ui.settingsPage, currentTheme = theme,
                            portraitLayout = portraitLayout, landscapeLayout = landscapeLayout, dpadStyle = dpadStyle,
                            difficulty = diff, gameMode = mode, ghostEnabled = ghost,
                            animationStyle = anim, animationDuration = animDur,
                            soundEnabled = sound, vibrationEnabled = vib, multiColorEnabled = multiColor,
                            pieceMaterial = pieceMaterial,
                            controllerEnabled = controllerEnabled, controllerDeadzone = controllerDeadzone,
                            appThemeMode = appThemeMode, keepScreenOn = keepScreenOn,
                            orientationLock = orientationLock, immersiveMode = immersiveMode,
                            frameRateTarget = frameRateTarget, batterySaver = batterySaver,
                            highContrast = highContrast, uiScale = uiScale,
                            playerName = name, highScore = hs, scoreHistory = history,
                            customThemes = customThemes, editingTheme = editingTheme,
                            customLayouts = customLayouts, editingLayout = editingLayout,
                            activeCustomLayout = activeCustomLayout,
                            onNavigate = vm::navigateSettings,
                            onBack = {
                                when (ui.settingsPage) {
                                    GameViewModel.SettingsPage.MAIN -> vm.closeSettings()
                                    GameViewModel.SettingsPage.THEME_EDITOR -> vm.navigateSettings(GameViewModel.SettingsPage.THEME)
                                    GameViewModel.SettingsPage.LAYOUT_EDITOR -> vm.navigateSettings(GameViewModel.SettingsPage.LAYOUT)
                                    else -> vm.navigateSettings(GameViewModel.SettingsPage.MAIN)
                                }
                            },
                            onSetTheme = vm::setTheme, onSetPortraitLayout = vm::setPortraitLayout,
                            onSetLandscapeLayout = vm::setLandscapeLayout, onSetDPadStyle = vm::setDPadStyle,
                            onSetDifficulty = vm::setDifficulty, onSetGameMode = vm::setGameMode,
                            onSetGhostEnabled = vm::setGhostPieceEnabled, onSetAnimationStyle = vm::setAnimationStyle,
                            onSetAnimationDuration = vm::setAnimationDuration, onSetSoundEnabled = vm::setSoundEnabled,
                            onSetVibrationEnabled = vm::setVibrationEnabled, onSetPlayerName = vm::setPlayerName,
                            onSetMultiColorEnabled = vm::setMultiColorEnabled,
                            onSetPieceMaterial = vm::setPieceMaterial,
                            onSetControllerEnabled = vm::setControllerEnabled,
                            onSetControllerDeadzone = vm::setControllerDeadzone,
                            onSetAppThemeMode = vm::setAppThemeMode,
                            onSetKeepScreenOn = vm::setKeepScreenOn,
                            onSetOrientationLock = vm::setOrientationLock,
                            onSetImmersiveMode = vm::setImmersiveMode,
                            onSetFrameRateTarget = vm::setFrameRateTarget,
                            onSetBatterySaver = vm::setBatterySaver,
                            onSetHighContrast = vm::setHighContrast,
                            onSetUiScale = vm::setUiScale,
                            onNewTheme = vm::startNewTheme, onEditTheme = vm::editTheme,
                            onUpdateEditingTheme = vm::updateEditingTheme, onSaveTheme = vm::saveEditingTheme,
                            onDeleteTheme = vm::deleteCustomTheme,
                            onNewLayout = vm::startNewLayout, onEditLayout = vm::editLayout,
                            onUpdateEditingLayout = vm::updateEditingLayout, onSaveLayout = vm::saveEditingLayout,
                            onSelectCustomLayout = vm::selectCustomLayout, onClearCustomLayout = vm::clearCustomLayout,
                            onDeleteLayout = vm::deleteCustomLayout,
                            onEditFreeform = { vm.closeSettings(); vm.enterFreeformEditMode() },
                            on3DMode = { vm.setPortraitLayout(LayoutPreset.PORTRAIT_3D); vm.closeSettings() }
                        )
                    }

                    else -> {
                        if (is3D && game3DState.status != GameStatus.MENU) {
                            Game3DScreen(
                                state = game3DState,
                                onMoveX = vm::move3DX,
                                onMoveZ = vm::move3DZ,
                                onRotateXZ = vm::rotate3DXZ,
                                onRotateXY = vm::rotate3DXY,
                                onHardDrop = vm::hardDrop3D,
                                onHold = vm::hold3D,
                                onPause = { if (game3DState.status == GameStatus.PLAYING) vm.pause3D() else vm.resume3D() },
                                onStart = vm::start3DGame,
                                onOpenSettings = vm::openSettings,
                                onSoftDrop = vm::softDrop3D,
                                onToggleGravity = vm::toggle3DGravity,
                                onQuit = vm::quit3DGame,
                                material = PieceMaterial.entries.find { it.name == pieceMaterial } ?: PieceMaterial.CLASSIC
                            )
                        } else {
                            GameScreen(
                                gameState = gs.copy(highScore = hs), layoutPreset = activeLayout, dpadStyle = dpadStyle,
                                ghostEnabled = ghost, animationStyle = anim, animationDuration = animDur,
                                multiColor = multiColor,
                                customLayout = activeCustomLayout, scoreHistory = history,
                                freeformElements = profile.freeformElements,
                                onStartGame = if (is3D) vm::start3DGame else vm::startGame,
                                onPause = vm::pauseGame, onResume = vm::resumeGame,
                                onRotate = vm::rotate, onRotateCCW = vm::rotateCounterClockwise,
                                onHardDrop = vm::hardDrop, onHold = vm::holdPiece,
                                onLeftPress = vm::startLeftDAS, onLeftRelease = vm::stopDAS,
                                onRightPress = vm::startRightDAS, onRightRelease = vm::stopDAS,
                                onDownPress = vm::startDownDAS, onDownRelease = vm::stopDAS,
                                onOpenSettings = vm::openSettings, onToggleSound = vm::toggleSound,
                                onQuit = vm::quitGame
                            )
                        }
                    }
                }
                        }
                    }
                }
            }
        }
    }
}

// ==================== WELCOME SCREEN ====================

@Composable
private fun WelcomeScreen(playerName: String, modifier: Modifier = Modifier) {
    val bgColor = Color(0xFF0A0A0A)
    val accentColor = Color(0xFFF4D03F)

    // Falling pieces in background (same as splash but dimmer)
    val inf = rememberInfiniteTransition(label = "welcomeBg")
    val fallAnim by inf.animateFloat(0f, 500000f, infiniteRepeatable(tween(750000, easing = LinearEasing)), label = "wFall")

    data class FP(val col: Float, val speed: Float, val sz: Float, val shape: Int, val colorIdx: Int, val startY: Float)
    val pieces = remember {
        val rng = java.util.Random(77)
        (0..120).map { FP(rng.nextFloat(), 0.3f + rng.nextFloat() * 0.8f, 4f + rng.nextFloat() * 6f, it % 7, it % 7, rng.nextFloat() * 8000f) }
    }
    val pieceColors = remember { listOf(Color(0xFFFF4444), Color(0xFF44AAFF), Color(0xFFFFAA00), Color(0xFF44FF44), Color(0xFFFF44FF), Color(0xFF44FFFF), Color(0xFFF4D03F)) }
    val shapes = remember { listOf(
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),
        listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1), listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1),
        listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1), listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
        listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1),
    ) }

    Box(modifier.background(bgColor), contentAlignment = Alignment.Center) {
        // Falling pieces background (dimmer than splash)
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val wrapH = h + 400f
            pieces.forEach { p ->
                val rawY = p.startY + fallAnim * p.speed
                val baseY = (rawY % wrapH) - 200f; val x = p.col * w; val s = p.sz
                val pColor = pieceColors[p.colorIdx]; val shape = shapes[p.shape % shapes.size]
                shape.forEach { (dx, dy) ->
                    drawRoundRect(pColor.copy(0.08f), Offset(x + dx * (s + 2), baseY + dy * (s + 2)), Size(s, s), CornerRadius(2f))
                }
                for (ti in 1..2) {
                    val ty = baseY - ti * (s + 2) * 1.1f; val ta = 0.04f * (1f - ti / 3f)
                    shape.forEach { (dx, dy) ->
                        drawRoundRect(Color(0xFF22C55E).copy(ta), Offset(x + dx * (s + 2), ty + dy * (s + 2)), Size(s * 0.9f, s * 0.9f), CornerRadius(2f))
                    }
                }
            }
        }

        // Welcome text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Welcome,",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(0.7f),
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                playerName.ifEmpty { "Player" },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = accentColor,
                letterSpacing = 4.sp
            )
        }
    }
}

// ==================== SPLASH SCREEN ====================

@Composable
private fun SplashScreen(modifier: Modifier = Modifier) {
    val bgColor = Color(0xFF0A0A0A)
    val inf = rememberInfiniteTransition(label = "splash")
    val rotation by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "rot")
    val elev by inf.animateFloat(15f, 40f, infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse), label = "elev")

    data class FP(val col: Float, val speed: Float, val sz: Float, val shape: Int, val colorIdx: Int, val startY: Float)
    val pieces = remember {
        val rng = java.util.Random(77)
        (0..120).map { FP(rng.nextFloat(), 0.3f + rng.nextFloat() * 0.8f, 4f + rng.nextFloat() * 6f, it % 7, it % 7, rng.nextFloat() * 8000f) }
    }
    val fallAnim by inf.animateFloat(0f, 500000f, infiniteRepeatable(tween(750000, easing = LinearEasing)), label = "fall")

    val pieceColors = remember { listOf(Color(0xFFFF4444), Color(0xFF44AAFF), Color(0xFFFFAA00), Color(0xFF44FF44), Color(0xFFFF44FF), Color(0xFF44FFFF), Color(0xFFF4D03F)) }
    val shapes = remember { listOf(
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),
        listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1), listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1),
        listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1), listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
        listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1),
    ) }

    Box(modifier.background(bgColor), contentAlignment = Alignment.Center) {
        // Falling pieces background
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val wrapH = h + 400f
            pieces.forEach { p ->
                val rawY = p.startY + fallAnim * p.speed
                val baseY = (rawY % wrapH) - 200f; val x = p.col * w; val s = p.sz
                val pColor = pieceColors[p.colorIdx]; val shape = shapes[p.shape % shapes.size]
                shape.forEach { (dx, dy) ->
                    drawRoundRect(pColor.copy(0.12f), Offset(x + dx * (s + 2), baseY + dy * (s + 2)), Size(s, s), CornerRadius(2f))
                }
                for (ti in 1..3) {
                    val ty = baseY - ti * (s + 2) * 1.1f; val ta = 0.06f * (1f - ti / 4f)
                    shape.forEach { (dx, dy) ->
                        drawRoundRect(Color(0xFF22C55E).copy(ta), Offset(x + dx * (s + 2), ty + dy * (s + 2)), Size(s * 0.9f, s * 0.9f), CornerRadius(2f))
                    }
                }
            }
        }

        // Central rotating 3D cube
        Canvas(Modifier.size(100.dp)) {
            val w = size.width; val h = size.height; val s = minOf(w, h) * 0.35f
            val cx = w / 2f; val cy = h / 2f
            val radAz = Math.toRadians(rotation.toDouble()); val radEl = Math.toRadians(elev.toDouble())
            val cosAz = cos(radAz).toFloat(); val sinAz = sin(radAz).toFloat()
            val cosEl = cos(radEl).toFloat(); val sinEl = sin(radEl).toFloat()
            fun proj(px: Float, py: Float, pz: Float): Offset {
                val rx = px * cosAz - py * sinAz; val ry = px * sinAz + py * cosAz; val rz = pz * cosEl - ry * sinEl
                return Offset(cx + rx * s, cy - rz * s)
            }
            val c = arrayOf(proj(-1f,-1f,-1f), proj(1f,-1f,-1f), proj(1f,1f,-1f), proj(-1f,1f,-1f),
                proj(-1f,-1f,1f), proj(1f,-1f,1f), proj(1f,1f,1f), proj(-1f,1f,1f))
            val corners3d = arrayOf(floatArrayOf(-1f,-1f,-1f), floatArrayOf(1f,-1f,-1f), floatArrayOf(1f,1f,-1f), floatArrayOf(-1f,1f,-1f),
                floatArrayOf(-1f,-1f,1f), floatArrayOf(1f,-1f,1f), floatArrayOf(1f,1f,1f), floatArrayOf(-1f,1f,1f))
            data class CF(val i: IntArray, val col: Color)
            val faces = listOf(
                CF(intArrayOf(4,5,6,7), Color(0xFF22C55E).copy(0.5f)), CF(intArrayOf(0,3,2,1), Color(0xFF22C55E).copy(0.15f)),
                CF(intArrayOf(0,1,5,4), Color(0xFFFF9800).copy(0.35f)), CF(intArrayOf(3,7,6,2), Color(0xFFFF9800).copy(0.15f)),
                CF(intArrayOf(0,4,7,3), Color(0xFF2196F3).copy(0.35f)), CF(intArrayOf(1,2,6,5), Color(0xFF2196F3).copy(0.15f)))
            data class SF(val i: IntArray, val col: Color, val d: Float)
            val sorted = faces.map { f ->
                val d = f.i.map { idx -> val cr = corners3d[idx]; val ry = cr[0] * sinAz + cr[1] * cosAz; cr[2] * sinEl + ry * cosEl }.average().toFloat()
                SF(f.i, f.col, d)
            }.sortedBy { it.d }
            for (face in sorted) {
                val path = Path().apply { moveTo(c[face.i[0]].x, c[face.i[0]].y); for (j in 1 until face.i.size) lineTo(c[face.i[j]].x, c[face.i[j]].y); close() }
                drawPath(path, face.col, style = Fill)
                drawPath(path, Color.White.copy(0.2f), style = Stroke(1.5f))
            }
        }
    }
}
