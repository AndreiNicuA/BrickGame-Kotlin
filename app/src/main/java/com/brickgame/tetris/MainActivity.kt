package com.brickgame.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brickgame.tetris.ui.screens.GameScreen
import com.brickgame.tetris.ui.screens.GameViewModel
import com.brickgame.tetris.ui.screens.SettingsScreen
import com.brickgame.tetris.ui.theme.BrickGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
            val name by vm.playerName.collectAsState()
            val hs by vm.highScore.collectAsState()
            val history by vm.scoreHistory.collectAsState()

            val config = LocalConfiguration.current
            val isLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val activeLayout = if (isLandscape) landscapeLayout else portraitLayout

            BrickGameTheme(gameTheme = theme) {
                BackHandler(enabled = ui.showSettings) {
                    if (ui.settingsPage != GameViewModel.SettingsPage.MAIN) vm.navigateSettings(GameViewModel.SettingsPage.MAIN)
                    else vm.closeSettings()
                }

                if (ui.showSettings) {
                    SettingsScreen(
                        page = ui.settingsPage, currentTheme = theme,
                        portraitLayout = portraitLayout, landscapeLayout = landscapeLayout, dpadStyle = dpadStyle,
                        difficulty = diff, gameMode = mode, ghostEnabled = ghost,
                        animationStyle = anim, animationDuration = animDur,
                        soundEnabled = sound, vibrationEnabled = vib,
                        playerName = name, highScore = hs, scoreHistory = history,
                        onNavigate = vm::navigateSettings,
                        onBack = { if (ui.settingsPage != GameViewModel.SettingsPage.MAIN) vm.navigateSettings(GameViewModel.SettingsPage.MAIN) else vm.closeSettings() },
                        onSetTheme = vm::setTheme, onSetPortraitLayout = vm::setPortraitLayout,
                        onSetLandscapeLayout = vm::setLandscapeLayout, onSetDPadStyle = vm::setDPadStyle,
                        onSetDifficulty = vm::setDifficulty, onSetGameMode = vm::setGameMode,
                        onSetGhostEnabled = vm::setGhostPieceEnabled, onSetAnimationStyle = vm::setAnimationStyle,
                        onSetAnimationDuration = vm::setAnimationDuration, onSetSoundEnabled = vm::setSoundEnabled,
                        onSetVibrationEnabled = vm::setVibrationEnabled, onSetPlayerName = vm::setPlayerName
                    )
                } else {
                    GameScreen(
                        gameState = gs.copy(highScore = hs), layoutPreset = activeLayout, dpadStyle = dpadStyle,
                        ghostEnabled = ghost, animationStyle = anim, animationDuration = animDur,
                        onStartGame = vm::startGame, onPause = vm::pauseGame, onResume = vm::resumeGame,
                        onRotate = vm::rotate, onRotateCCW = vm::rotateCounterClockwise,
                        onHardDrop = vm::hardDrop, onHold = vm::holdPiece,
                        onLeftPress = vm::startLeftDAS, onLeftRelease = vm::stopDAS,
                        onRightPress = vm::startRightDAS, onRightRelease = vm::stopDAS,
                        onDownPress = vm::startDownDAS, onDownRelease = vm::stopDAS,
                        onOpenSettings = vm::openSettings, onToggleSound = vm::toggleSound
                    )
                }
            }
        }
    }
}
