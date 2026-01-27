package com.brickgame.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
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
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Hide system bars for immersive experience
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
            
            BrickGameTheme(gameTheme = currentTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main game screen
                    GameScreen(
                        gameState = gameState.copy(highScore = uiState.highScore),
                        lineClearAnimation = lineClearAnimation,
                        vibrationEnabled = uiState.vibrationEnabled,
                        onStartGame = viewModel::startGame,
                        onPauseGame = viewModel::pauseGame,
                        onResetGame = viewModel::resetGame,
                        onToggleSound = viewModel::toggleSound,
                        onMoveLeft = viewModel::moveLeft,
                        onMoveLeftRelease = viewModel::stopLeftRepeat,
                        onMoveRight = viewModel::moveRight,
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
                            vibrationEnabled = uiState.vibrationEnabled,
                            soundEnabled = uiState.soundEnabled,
                            onThemeChange = viewModel::setTheme,
                            onVibrationChange = viewModel::setVibration,
                            onSoundChange = viewModel::setSound,
                            onClose = viewModel::hideSettings
                        )
                    }
                }
            }
        }
    }
}
