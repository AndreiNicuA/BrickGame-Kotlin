package com.brickgame.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brickgame.tetris.ui.screens.GameScreen
import com.brickgame.tetris.ui.screens.GameViewModel
import com.brickgame.tetris.ui.screens.SettingsScreen
import com.brickgame.tetris.ui.theme.BrickGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        setContent {
            val viewModel: GameViewModel = viewModel()
            
            val gameState by viewModel.gameState.collectAsState()
            val uiState by viewModel.uiState.collectAsState()
            val lineClearAnimation by viewModel.lineClearAnimation.collectAsState()
            val currentTheme by viewModel.currentTheme.collectAsState()
            val scoreHistory by viewModel.scoreHistory.collectAsState()
            
            // Handle back button - don't exit the app
            BackHandler(enabled = true) {
                when {
                    uiState.showSettings -> {
                        // Close settings if open
                        viewModel.hideSettings()
                    }
                    else -> {
                        // Don't exit the app - just ignore or pause the game
                        // You could also show a confirmation dialog here
                    }
                }
            }
            
            BrickGameTheme(gameTheme = currentTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main game screen
                    GameScreen(
                        gameState = gameState.copy(highScore = uiState.highScore),
                        lineClearAnimation = lineClearAnimation,
                        vibrationEnabled = uiState.vibrationEnabled,
                        layoutMode = uiState.layoutMode,
                        onStartGame = viewModel::startGame,
                        onTogglePause = viewModel::togglePauseResume,
                        onResetGame = viewModel::resetGame,
                        onToggleSound = { viewModel.setSoundEnabled(!uiState.soundEnabled) },
                        onMoveLeft = viewModel::startLeftRepeat,
                        onMoveLeftRelease = viewModel::stopLeftRepeat,
                        onMoveRight = viewModel::startRightRepeat,
                        onMoveRightRelease = viewModel::stopRightRepeat,
                        onMoveDown = viewModel::startDownRepeat,
                        onMoveDownRelease = viewModel::stopDownRepeat,
                        onHardDrop = viewModel::hardDrop,
                        onRotate = viewModel::rotate,
                        onOpenSettings = viewModel::showSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Settings overlay
                    AnimatedVisibility(
                        visible = uiState.showSettings,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        SettingsScreen(
                            currentThemeName = currentTheme.name,
                            layoutMode = uiState.layoutMode,
                            vibrationEnabled = uiState.vibrationEnabled,
                            vibrationIntensity = uiState.vibrationIntensity,
                            soundEnabled = uiState.soundEnabled,
                            soundVolume = uiState.soundVolume,
                            playerName = uiState.playerName,
                            highScore = uiState.highScore,
                            scoreHistory = scoreHistory,
                            onThemeChange = viewModel::setTheme,
                            onLayoutModeChange = viewModel::setLayoutMode,
                            onVibrationEnabledChange = viewModel::setVibrationEnabled,
                            onVibrationIntensityChange = viewModel::setVibrationIntensity,
                            onSoundEnabledChange = viewModel::setSoundEnabled,
                            onSoundVolumeChange = viewModel::setSoundVolume,
                            onPlayerNameChange = viewModel::setPlayerName,
                            onClearHistory = viewModel::clearScoreHistory,
                            onClose = viewModel::hideSettings,
                            onBack = viewModel::hideSettings
                        )
                    }
                }
            }
        }
    }
}
