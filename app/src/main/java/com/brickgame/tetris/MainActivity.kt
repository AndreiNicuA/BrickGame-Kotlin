package com.brickgame.tetris

import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.input.GamepadController
import com.brickgame.tetris.ui.layout.FreeformEditorScreen
import com.brickgame.tetris.ui.layout.BoardShape
import com.brickgame.tetris.ui.layout.InfoBarType
import com.brickgame.tetris.ui.layout.InfoBarShape
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.components.PieceMaterial
import com.brickgame.tetris.ui.screens.Game3DScreen
import com.brickgame.tetris.ui.screens.GameScreen
import com.brickgame.tetris.ui.screens.GameViewModel
import com.brickgame.tetris.ui.screens.SettingsScreen
import com.brickgame.tetris.ui.theme.BrickGameTheme
import com.brickgame.tetris.ui.brand.Bw
import com.brickgame.tetris.ui.brand.BwDarkSystemBars
import com.brickgame.tetris.ui.brand.MenuScreen
import com.brickgame.tetris.ui.brand.OnboardingScreen
import com.brickgame.tetris.ui.brand.PlayerSummary
import com.brickgame.tetris.ui.brand.PlayersScreen
import com.brickgame.tetris.ui.brand.IncomingGarbageBar
import com.brickgame.tetris.ui.brand.VersusHud
import com.brickgame.tetris.ui.brand.VersusResultOverlay
import com.brickgame.tetris.ui.brand.VersusScreen
import com.brickgame.tetris.data.LocalPlayer
import com.brickgame.tetris.data.PlayStyle
import androidx.compose.runtime.saveable.rememberSaveable
import com.brickgame.tetris.ui.theme.LocalIsDarkMode
import kotlinx.coroutines.delay
import kotlin.math.*

class MainActivity : ComponentActivity() {

    private val gamepad = GamepadController()
    private val vm: GameViewModel by viewModels()

    /** Gamepad presence, kept current by an InputDeviceListener instead of polling every recomposition. */
    private var controllerConnected by mutableStateOf(false)
    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = refreshControllers()
        override fun onInputDeviceRemoved(deviceId: Int) = refreshControllers()
        override fun onInputDeviceChanged(deviceId: Int) = refreshControllers()
    }
    private fun refreshControllers() {
        controllerConnected = GamepadController.getConnectedControllers().isNotEmpty()
    }

    override fun onStart() {
        super.onStart()
        getSystemService(InputManager::class.java)?.registerInputDeviceListener(inputDeviceListener, null)
        refreshControllers()
    }

    override fun onStop() {
        getSystemService(InputManager::class.java)?.unregisterInputDeviceListener(inputDeviceListener)
        super.onStop()
    }

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
        // System splash (icon) stays only until settings have loaded — typically a few frames.
        // Must be installed before super.onCreate().
        installSplashScreen().setKeepOnScreenCondition { !vm.dataLoaded.value }
        // Read saved theme mode BEFORE enableEdgeToEdge so bars match from first frame
        val prefs = getSharedPreferences("app_theme_cache", MODE_PRIVATE)
        val cachedMode = prefs.getString("mode", "auto") ?: "auto"
        val isSystemDark = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val startDark = when (cachedMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemDark
        }
        if (startDark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            )
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.light(
                    AndroidColor.parseColor("#F2F2F2"), AndroidColor.parseColor("#F2F2F2"))
            )
            window.decorView.setBackgroundColor(AndroidColor.parseColor("#F2F2F2"))
        }
        super.onCreate(savedInstanceState)
        setContent {

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
            val soundVolume by vm.soundVolume.collectAsState()
            val soundStyle by vm.soundStyle.collectAsState()
            val vibrationIntensity by vm.vibrationIntensity.collectAsState()
            val vibrationStyle by vm.vibrationStyle.collectAsState()
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
            // New features
            val levelEvents by vm.levelEventsEnabled.collectAsState()
            val buttonStyle by vm.buttonStyle.collectAsState()
            val boardShape by vm.boardShape.collectAsState()
            val infoBarType by vm.infoBarType.collectAsState()
            val infoBarShape by vm.infoBarShape.collectAsState()
            val controllerLayoutMode by vm.controllerLayout.collectAsState()
            val leftHanded by vm.leftHanded.collectAsState()
            val swipeControls by vm.swipeControls.collectAsState()
            val infinityTimer by vm.infinityTimer.collectAsState()
            val infinityTimerEnabled by vm.infinityTimerEnabled.collectAsState()
            val showOnboarding by vm.showOnboarding.collectAsState()

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

            val config = LocalConfiguration.current
            val isLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val activeLayout = if (isLandscape) landscapeLayout else portraitLayout
            // The chosen game decides 3D, not the orientation: rotating the phone mid-game keeps you in it
            val is3D = portraitLayout == LayoutPreset.PORTRAIT_3D

            BrickGameTheme(gameTheme = theme, appThemeMode = appThemeMode) {
                // Control system bar appearance based on theme mode
                val isDarkMode = LocalIsDarkMode.current
                LaunchedEffect(isDarkMode) {
                    // Cache for next app start so bars match from first frame
                    getSharedPreferences("app_theme_cache", MODE_PRIVATE)
                        .edit().putString("mode", appThemeMode).apply()
                    // Re-call enableEdgeToEdge with correct styles
                    if (isDarkMode) {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        )
                        window.decorView.setBackgroundColor(AndroidColor.parseColor("#0A0A0A"))
                    } else {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                            navigationBarStyle = SystemBarStyle.light(
                                AndroidColor.parseColor("#F2F2F2"), AndroidColor.parseColor("#F2F2F2"))
                        )
                        window.decorView.setBackgroundColor(AndroidColor.parseColor("#F2F2F2"))
                    }
                }
                Box(Modifier.fillMaxSize()) {
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
                            boardShape = BoardShape.entries.find { it.name == boardShape } ?: BoardShape.STANDARD,
                            onBoardShapeChanged = { shape -> vm.setBoardShape(shape.name) },
                            infoBarType = InfoBarType.entries.find { it.name == infoBarType } ?: InfoBarType.INDIVIDUAL,
                            onInfoBarTypeChanged = { type -> vm.setInfoBarType(type.name) },
                            infoBarShape = InfoBarShape.entries.find { it.name == infoBarShape } ?: InfoBarShape.PILL,
                            onInfoBarShapeChanged = { shape -> vm.setInfoBarShape(shape.name) },
                            elements = profile.freeformElements,
                            onElementUpdated = vm::updateFreeformElement,
                            onElementAdded = vm::addFreeformElement,
                            onElementRemoved = vm::removeFreeformElement,
                            onReset = vm::resetFreeformElements,
                            onDone = vm::exitFreeformEditMode
                        )
                    }

                    ui.showSettings -> {
                        val arSettings by vm.arSettings.collectAsState()
                        val arHeatLimitSetting by vm.arHeatLimit.collectAsState()
                        SettingsScreen(
                            page = ui.settingsPage, currentTheme = theme,
                            portraitLayout = portraitLayout, landscapeLayout = landscapeLayout, dpadStyle = dpadStyle,
                            difficulty = diff, gameMode = mode, ghostEnabled = ghost,
                            animationStyle = anim, animationDuration = animDur,
                            soundEnabled = sound, vibrationEnabled = vib, multiColorEnabled = multiColor,
                            soundVolume = soundVolume, soundStyle = soundStyle,
                            vibrationIntensity = vibrationIntensity, vibrationStyle = vibrationStyle,
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
                            onSetSoundVolume = vm::setSoundVolume,
                            onSetSoundStyle = vm::setSoundStyle,
                            onSetVibrationIntensity = vm::setVibrationIntensity,
                            onSetVibrationStyle = vm::setVibrationStyle,
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
                            on3DMode = { vm.setPortraitLayout(LayoutPreset.PORTRAIT_3D); vm.closeSettings() },
                            onClearHistory = vm::clearHistory,
                            levelEventsEnabled = levelEvents,
                            onSetLevelEventsEnabled = vm::setLevelEventsEnabled,
                            buttonStyle = buttonStyle,
                            onSetButtonStyle = vm::setButtonStyle,
                            controllerLayoutMode = controllerLayoutMode,
                            onSetControllerLayout = vm::setControllerLayout,
                            infinityTimer = infinityTimer,
                            onSetInfinityTimer = vm::setInfinityTimer,
                            infinityTimerEnabled = infinityTimerEnabled,
                            onSetInfinityTimerEnabled = vm::setInfinityTimerEnabled,
                            leftHanded = leftHanded,
                            onSetLeftHanded = vm::setLeftHanded,
                            swipeControls = swipeControls,
                            onSetSwipeControls = vm::setSwipeControls,
                            arSettings = arSettings, onArSettings = vm::updateArSettings,
                            arHeatLimit = arHeatLimitSetting, onSetArHeatLimit = vm::setArHeatLimit
                        )
                    }

                    else -> PlayScreen(
                        vm = vm, is3D = is3D, activeLayout = activeLayout,
                        portraitLayout = portraitLayout, dpadStyle = dpadStyle,
                        ghost = ghost, anim = anim, animDur = animDur, multiColor = multiColor,
                        activeCustomLayout = activeCustomLayout, history = history, hs = hs,
                        freeformElements = profile.freeformElements, levelEvents = levelEvents,
                        buttonStyle = buttonStyle, boardShape = boardShape,
                        infoBarShape = infoBarShape, infoBarType = infoBarType,
                        controllerLayoutMode = controllerLayoutMode,
                        controllerConnected = controllerConnected,
                        pieceMaterial = pieceMaterial, highContrast = highContrast,
                        uiScale = uiScale, leftHanded = leftHanded,
                        swipeControls = swipeControls,
                        showOnboarding = showOnboarding,
                        onCloseApp = { this@MainActivity.finishAndRemoveTask() }
                    )
                }
                }
            }
        }
    }
}

/**
 * The game itself (menu, 2D or 3D). Game state is collected HERE rather than at the activity
 * root, so the ~60 state emissions per second during play recompose only this subtree —
 * not the settings plumbing and everything else in setContent.
 */
@Composable
private fun PlayScreen(
    vm: GameViewModel, is3D: Boolean, activeLayout: LayoutPreset, portraitLayout: LayoutPreset,
    dpadStyle: com.brickgame.tetris.ui.layout.DPadStyle, ghost: Boolean,
    anim: com.brickgame.tetris.ui.styles.AnimationStyle, animDur: Float, multiColor: Boolean,
    activeCustomLayout: com.brickgame.tetris.data.CustomLayoutData?,
    history: List<com.brickgame.tetris.data.ScoreEntry>, hs: Int,
    freeformElements: Map<String, com.brickgame.tetris.data.FreeformElement>, levelEvents: Boolean,
    buttonStyle: String, boardShape: String, infoBarShape: String, infoBarType: String,
    controllerLayoutMode: String, controllerConnected: Boolean, pieceMaterial: String,
    highContrast: Boolean, uiScale: Float, leftHanded: Boolean, swipeControls: Boolean, showOnboarding: Boolean,
    onCloseApp: () -> Unit
) {
    val game3DState by vm.game3DState.collectAsState()
    if (is3D && game3DState.status != GameStatus.MENU) {
        val arHeatLimit by vm.arHeatLimit.collectAsState()
        val motionView by vm.motionView.collectAsState()
        Box(Modifier.fillMaxSize()) {
        Game3DScreen(
            arHeatLimit = arHeatLimit,
            onArHeatLimit = vm::setArHeatLimit,
            motionViewSaved = motionView,
            onMotionView = vm::setMotionView,
            state = game3DState,
            onMoveX = vm::move3DX,
            onMoveZ = vm::move3DZ,
            onRotateXZ = vm::rotate3DXZ,
            onRotateXY = vm::rotate3DXY,
            onHardDrop = vm::hardDrop3D,
            onHold = vm::hold3D,
            onPause = { if (vm.game3DState.value.status == GameStatus.PLAYING) vm.pause3D() else vm.resume3D() },
            onStart = vm::start3DGame,
            onOpenSettings = vm::openSettings,
            onSoftDrop = vm::softDrop3D,
            onToggleGravity = vm::toggle3DGravity,
            onQuit = vm::quit3DGame,
            currentPiece = { vm.game3DState.value.currentPiece },
            material = PieceMaterial.entries.find { it.name == pieceMaterial } ?: PieceMaterial.CLASSIC
        )
        VersusLayer(vm)
        }
        return
    }
    val gs by vm.gameState.collectAsState()
    if (gs.status == GameStatus.MENU) {
        BrandScreens(vm, is3D, portraitLayout, swipeControls, history, showOnboarding)
        return
    }
    val timerExpired by vm.timerExpired.collectAsState()
    val remainingSeconds by vm.remainingSeconds.collectAsState()
    Box(Modifier.fillMaxSize()) {
    GameScreen(
        gameState = gs.copy(highScore = hs), layoutPreset = activeLayout, dpadStyle = dpadStyle,
        ghostEnabled = ghost, animationStyle = anim, animationDuration = animDur,
        multiColor = multiColor,
        customLayout = activeCustomLayout, scoreHistory = history,
        freeformElements = freeformElements,
        levelEventsEnabled = levelEvents,
        buttonStyle = buttonStyle,
        boardShape = boardShape,
        infoBarShape = infoBarShape,
        infoBarType = infoBarType,
        controllerLayoutMode = controllerLayoutMode,
        controllerConnected = controllerConnected,
        timerExpired = timerExpired,
        remainingSeconds = remainingSeconds,
        pieceMaterial = pieceMaterial,
        highContrast = highContrast,
        uiScale = uiScale,
        leftHanded = leftHanded,
        portraitLayout = portraitLayout,
        swipeControls = swipeControls,
        // In Versus the countdown is the intro; a paused guide would hand the friend a free lead
        showSwipeIntro = !vm.versus.collectAsState().value.active,
        onCloseApp = onCloseApp,
        showOnboarding = showOnboarding,
        onDismissOnboarding = vm::dismissOnboarding,
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
    VersusLayer(vm)
    }
}

/** Versus extras on top of a running game: opponent strip and the round result. */
@Composable
private fun VersusLayer(vm: GameViewModel) {
    val v by vm.versus.collectAsState()
    if (!v.active && v.result == null) return
    val peer by vm.versusLink.peer.collectAsState()
    val phase by vm.versusLink.phase.collectAsState()
    val opponent = peer?.name ?: "Friend"
    Box(Modifier.fillMaxSize()) {
        if (v.result == null) {
            VersusHud(opponent, v.opponentScore, v.opponentLines, v.received, v.sent,
                Modifier.align(Alignment.TopCenter).padding(top = 84.dp), incoming = v.incoming)
            if (v.incoming > 0) IncomingGarbageBar(v.incoming, Modifier.align(Alignment.CenterStart))
        } else {
            VersusResultOverlay(
                won = v.result == GameViewModel.VersusResult.WIN,
                dropped = v.result == GameViewModel.VersusResult.DROPPED, opponent = opponent,
                wins = v.wins, losses = v.losses,
                connected = phase == com.brickgame.tetris.net.VersusLink.Phase.CONNECTED,
                onRematch = vm::versusStartRound, onLobby = vm::versusBackToLobby
            )
        }
    }
}

/** Menu, intro and "Who's playing?" — the brand screens shown while no game is running. */
@Composable
private fun BrandScreens(
    vm: GameViewModel, is3D: Boolean, portraitLayout: LayoutPreset, swipeControls: Boolean,
    history: List<com.brickgame.tetris.data.ScoreEntry>, showOnboarding: Boolean
) {
    BwDarkSystemBars()
    val playersState by vm.players.collectAsState()
    val playerName by vm.playerName.collectAsState()
    val difficulty by vm.difficulty.collectAsState()
    var showPlayers by rememberSaveable { mutableStateOf(false) }
    var showVersus by rememberSaveable { mutableStateOf(false) }
    val versusPhase by vm.versusLink.phase.collectAsState()
    val versusPeer by vm.versusLink.peer.collectAsState()
    val versus by vm.versus.collectAsState()
    val style = when (portraitLayout) {
        LayoutPreset.PORTRAIT_CLASSIC -> PlayStyle.CLASSIC
        LayoutPreset.PORTRAIT_3D -> PlayStyle.THREE_D
        else -> PlayStyle.NEON
    }
    val active = playersState.active
    // Scores recorded before profiles existed belong to the player with that name
    fun scoresOf(p: LocalPlayer?) = history.filter { e ->
        if (p == null) true else e.profileId == p.id || (e.profileId.isEmpty() && e.playerName == p.name)
    }

    when {
        showOnboarding -> OnboardingScreen(
            initialName = active?.name ?: playerName,
            initialSwipe = swipeControls,
            initialStyle = style,
            initialDifficulty = difficulty,
            onFinish = { n, sw, st, d -> vm.completeOnboarding(n, sw, st, d) },
            onSkip = vm::dismissOnboarding
        )
        // While searching or connected you stay in the lobby (also after a round ends)
        showVersus || versusPhase != com.brickgame.tetris.net.VersusLink.Phase.IDLE -> VersusScreen(
            myName = active?.name ?: playerName,
            myColor = Bw.playerColor(active?.colorIndex ?: 0),
            phase = versusPhase, peer = versusPeer, style = style,
            wins = versus.wins, losses = versus.losses, countdown = versus.countdown,
            onSelectStyle = vm::versusPick,
            onSearch = vm::versusSearch,
            onStart = vm::versusStartRound,
            onLeave = { vm.versusLeave(); showVersus = false }
        )
        showPlayers -> PlayersScreen(
            players = playersState.players.map { p ->
                val mine = scoresOf(p)
                PlayerSummary(p, mine.maxOfOrNull { it.score } ?: 0, mine.maxOfOrNull { it.timestamp })
            },
            activeId = playersState.activeId,
            onPick = { id -> vm.switchPlayer(id) },
            onAdd = { n -> vm.addPlayer(n) },
            onRemove = vm::removePlayer,
            onClose = { showPlayers = false }
        )
        else -> {
            val mine = scoresOf(active)
            val best = mine.maxByOrNull { it.score }
            MenuScreen(
                playerName = active?.name ?: playerName,
                playerColor = Bw.playerColor(active?.colorIndex ?: 0),
                bestScore = best?.score ?: 0,
                bestLevel = best?.level ?: 0,
                style = style,
                onSelectStyle = vm::selectStyle,
                onPlay = { if (is3D) vm.start3DGame() else vm.startGame() },
                onSwitchPlayer = { showPlayers = true },
                onSettings = vm::openSettings,
                onRecords = { vm.openSettings(); vm.navigateSettings(GameViewModel.SettingsPage.PROFILE) },
                onHowToPlay = vm::replayOnboarding,
                onVersus = { showVersus = true }
            )
        }
    }
}
