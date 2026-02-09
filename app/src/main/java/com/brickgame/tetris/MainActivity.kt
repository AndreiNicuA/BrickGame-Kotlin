package com.brickgame.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.ui.layout.FreeformEditorScreen
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.screens.Game3DScreen
import com.brickgame.tetris.ui.screens.GameScreen
import com.brickgame.tetris.ui.screens.GameViewModel
import com.brickgame.tetris.ui.screens.SettingsScreen
import com.brickgame.tetris.ui.theme.BrickGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GameViewModel = viewModel()
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

            val config = LocalConfiguration.current
            val isLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val activeLayout = if (isLandscape) landscapeLayout else portraitLayout
            val is3D = activeLayout == LayoutPreset.PORTRAIT_3D

            BrickGameTheme(gameTheme = theme) {
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
                        // 3D: show landing page (2D menu) when in MENU, switch to 3D screen when playing
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
                                onQuit = vm::quit3DGame
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
