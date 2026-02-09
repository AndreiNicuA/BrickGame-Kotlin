package com.brickgame.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.layout.FreeformEditorScreen
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.components.PieceMaterial
import com.brickgame.tetris.ui.screens.Game3DScreen
import com.brickgame.tetris.ui.screens.GameScreen
import com.brickgame.tetris.ui.screens.GameViewModel
import com.brickgame.tetris.ui.screens.SettingsScreen
import com.brickgame.tetris.ui.theme.BrickGameTheme
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GameViewModel = viewModel()
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

            // Fade in content once data loaded, fade out splash
            val contentAlpha by animateFloatAsState(
                targetValue = if (dataLoaded) 1f else 0f,
                animationSpec = tween(durationMillis = 600, easing = EaseOut),
                label = "contentFade"
            )
            val splashAlpha by animateFloatAsState(
                targetValue = if (dataLoaded) 0f else 1f,
                animationSpec = tween(durationMillis = 400, delayMillis = 200, easing = EaseIn),
                label = "splashFade"
            )

            val config = LocalConfiguration.current
            val isLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val activeLayout = if (isLandscape) landscapeLayout else portraitLayout
            val is3D = activeLayout == LayoutPreset.PORTRAIT_3D

            BrickGameTheme(gameTheme = theme) {
                Box(Modifier.fillMaxSize()) {
                    // Splash — animated falling pieces + rotating 3D cube
                    if (splashAlpha > 0.01f) {
                        SplashScreen(Modifier.fillMaxSize().alpha(splashAlpha))
                    }

                    // Main content — fades in
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
