package com.brickgame.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            val viewModel: GameViewModel = viewModel()
            val gameState by viewModel.gameState.collectAsState()
            val uiState by viewModel.uiState.collectAsState()
            val currentTheme by viewModel.currentTheme.collectAsState()

            // Layout
            val portraitLayout by viewModel.portraitLayout.collectAsState()
            val landscapeLayout by viewModel.landscapeLayout.collectAsState()
            val dpadStyle by viewModel.dpadStyle.collectAsState()

            // Settings
            val ghostEnabled by viewModel.ghostPieceEnabled.collectAsState()
            val difficulty by viewModel.difficulty.collectAsState()
            val gameMode by viewModel.gameMode.collectAsState()
            val animationStyle by viewModel.animationStyle.collectAsState()
            val animationDuration by viewModel.animationDuration.collectAsState()
            val soundEnabled by viewModel.soundEnabled.collectAsState()
            val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
            val playerName by viewModel.playerName.collectAsState()
            val highScore by viewModel.highScore.collectAsState()
            val scoreHistory by viewModel.scoreHistory.collectAsState()

            // Orientation detection
            val config = LocalConfiguration.current
            val isLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val activeLayout = if (isLandscape) landscapeLayout else portraitLayout

            BrickGameTheme(gameTheme = currentTheme) {

                // Back handler
                BackHandler(enabled = uiState.showSettings) {
                    if (uiState.settingsPage != GameViewModel.SettingsPage.MAIN) {
                        viewModel.navigateSettings(GameViewModel.SettingsPage.MAIN)
                    } else {
                        viewModel.closeSettings()
                    }
                }

                if (uiState.showSettings) {
                    SettingsScreen(
                        page = uiState.settingsPage,
                        currentTheme = currentTheme,
                        portraitLayout = portraitLayout,
                        landscapeLayout = landscapeLayout,
                        dpadStyle = dpadStyle,
                        difficulty = difficulty,
                        gameMode = gameMode,
                        ghostEnabled = ghostEnabled,
                        animationStyle = animationStyle,
                        animationDuration = animationDuration,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        playerName = playerName,
                        highScore = highScore,
                        scoreHistory = scoreHistory,
                        onNavigate = viewModel::navigateSettings,
                        onBack = {
                            if (uiState.settingsPage != GameViewModel.SettingsPage.MAIN) {
                                viewModel.navigateSettings(GameViewModel.SettingsPage.MAIN)
                            } else {
                                viewModel.closeSettings()
                            }
                        },
                        onSetTheme = viewModel::setTheme,
                        onSetPortraitLayout = viewModel::setPortraitLayout,
                        onSetLandscapeLayout = viewModel::setLandscapeLayout,
                        onSetDPadStyle = viewModel::setDPadStyle,
                        onSetDifficulty = viewModel::setDifficulty,
                        onSetGameMode = viewModel::setGameMode,
                        onSetGhostEnabled = viewModel::setGhostPieceEnabled,
                        onSetAnimationStyle = viewModel::setAnimationStyle,
                        onSetAnimationDuration = viewModel::setAnimationDuration,
                        onSetSoundEnabled = viewModel::setSoundEnabled,
                        onSetVibrationEnabled = viewModel::setVibrationEnabled,
                        onSetPlayerName = viewModel::setPlayerName
                    )
                } else {
                    GameScreen(
                        gameState = gameState.copy(highScore = highScore),
                        layoutPreset = activeLayout,
                        dpadStyle = dpadStyle,
                        ghostEnabled = ghostEnabled,
                        animationStyle = animationStyle,
                        animationDuration = animationDuration,
                        onStartGame = viewModel::startGame,
                        onPause = viewModel::pauseGame,
                        onResume = viewModel::resumeGame,
                        onRotate = viewModel::rotate,
                        onRotateCCW = viewModel::rotateCounterClockwise,
                        onHardDrop = viewModel::hardDrop,
                        onHold = viewModel::holdPiece,
                        onLeftPress = viewModel::startLeftDAS,
                        onLeftRelease = viewModel::stopDAS,
                        onRightPress = viewModel::startRightDAS,
                        onRightRelease = viewModel::stopDAS,
                        onDownPress = viewModel::startDownDAS,
                        onDownRelease = viewModel::stopDAS,
                        onOpenSettings = viewModel::openSettings,
                        onToggleSound = viewModel::toggleSound
                    )
                }
            }
        }
    }
}
