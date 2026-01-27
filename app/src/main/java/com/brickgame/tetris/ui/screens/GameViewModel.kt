package com.brickgame.tetris.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brickgame.tetris.data.SettingsRepository
import com.brickgame.tetris.game.GameEvent
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.game.MoveResult
import com.brickgame.tetris.game.TetrisGame
import com.brickgame.tetris.ui.animations.LineClearAnimationState
import com.brickgame.tetris.ui.animations.LineClearPhase
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsRepository = SettingsRepository(application)
    private val game = TetrisGame()
    
    // Game state
    val gameState: StateFlow<GameState> = game.state
    
    // UI state
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Line clear animation state
    private val _lineClearAnimation = MutableStateFlow(LineClearAnimationState())
    val lineClearAnimation: StateFlow<LineClearAnimationState> = _lineClearAnimation.asStateFlow()
    
    // Theme
    private val _currentTheme = MutableStateFlow(GameThemes.Classic)
    val currentTheme: StateFlow<GameTheme> = _currentTheme.asStateFlow()
    
    // Game loop job
    private var gameLoopJob: Job? = null
    
    // Move repeat jobs
    private var leftRepeatJob: Job? = null
    private var rightRepeatJob: Job? = null
    private var downRepeatJob: Job? = null
    
    init {
        // Load settings
        viewModelScope.launch {
            val themeName = settingsRepository.themeName.first()
            _currentTheme.value = GameThemes.getThemeByName(themeName)
            
            val vibration = settingsRepository.vibrationEnabled.first()
            val sound = settingsRepository.soundEnabled.first()
            val highScore = settingsRepository.highScore.first()
            
            _uiState.update {
                it.copy(
                    vibrationEnabled = vibration,
                    soundEnabled = sound,
                    highScore = highScore
                )
            }
        }
        
        // Listen to game events
        viewModelScope.launch {
            game.state.collect { state ->
                // Handle line clear animation
                if (state.clearedRows.isNotEmpty()) {
                    playLineClearAnimation(state.clearedRows)
                }
                
                // Handle game over - save high score
                if (state.status == GameStatus.GAME_OVER && state.score > _uiState.value.highScore) {
                    _uiState.update { it.copy(highScore = state.score) }
                    settingsRepository.setHighScore(state.score)
                }
            }
        }
    }
    
    // Game controls
    fun startGame() {
        game.startGame()
        startGameLoop()
    }
    
    fun pauseGame() {
        game.togglePause()
        if (game.state.value.status == GameStatus.PAUSED) {
            stopGameLoop()
        } else {
            startGameLoop()
        }
    }
    
    fun resetGame() {
        stopGameLoop()
        stopAllRepeats()
        _uiState.update { it.copy(showSettings = false) }
        // Reset to menu state
        viewModelScope.launch {
            game.startGame()
            game.pauseGame()
            // Immediately put in menu state by recreating
        }
    }
    
    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                if (game.state.value.status == GameStatus.PLAYING) {
                    game.moveDown()
                }
                delay(game.getDropSpeed())
            }
        }
    }
    
    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }
    
    // Movement controls
    fun moveLeft() {
        if (game.state.value.status == GameStatus.PLAYING) {
            game.moveLeft()
        }
    }
    
    fun startLeftRepeat() {
        moveLeft()
        leftRepeatJob?.cancel()
        leftRepeatJob = viewModelScope.launch {
            delay(200) // Initial delay
            while (true) {
                moveLeft()
                delay(50) // Repeat rate
            }
        }
    }
    
    fun stopLeftRepeat() {
        leftRepeatJob?.cancel()
        leftRepeatJob = null
    }
    
    fun moveRight() {
        if (game.state.value.status == GameStatus.PLAYING) {
            game.moveRight()
        }
    }
    
    fun startRightRepeat() {
        moveRight()
        rightRepeatJob?.cancel()
        rightRepeatJob = viewModelScope.launch {
            delay(200)
            while (true) {
                moveRight()
                delay(50)
            }
        }
    }
    
    fun stopRightRepeat() {
        rightRepeatJob?.cancel()
        rightRepeatJob = null
    }
    
    fun startDownRepeat() {
        downRepeatJob?.cancel()
        downRepeatJob = viewModelScope.launch {
            while (true) {
                if (game.state.value.status == GameStatus.PLAYING) {
                    game.moveDown()
                }
                delay(50)
            }
        }
    }
    
    fun stopDownRepeat() {
        downRepeatJob?.cancel()
        downRepeatJob = null
    }
    
    fun hardDrop() {
        if (game.state.value.status == GameStatus.PLAYING) {
            game.hardDrop()
        }
    }
    
    fun rotate() {
        if (game.state.value.status == GameStatus.PLAYING) {
            game.rotate()
        }
    }
    
    private fun stopAllRepeats() {
        leftRepeatJob?.cancel()
        rightRepeatJob?.cancel()
        downRepeatJob?.cancel()
        leftRepeatJob = null
        rightRepeatJob = null
        downRepeatJob = null
    }
    
    // Line clear animation
    private fun playLineClearAnimation(rows: List<Int>) {
        viewModelScope.launch {
            // Flash phase
            _lineClearAnimation.value = LineClearAnimationState(
                isAnimating = true,
                phase = LineClearPhase.FLASH,
                rows = rows,
                progress = 0f
            )
            
            repeat(3) {
                _lineClearAnimation.value = _lineClearAnimation.value.copy(progress = 1f)
                delay(60)
                _lineClearAnimation.value = _lineClearAnimation.value.copy(progress = 0f)
                delay(60)
            }
            
            // Collapse phase
            _lineClearAnimation.value = _lineClearAnimation.value.copy(
                phase = LineClearPhase.COLLAPSE,
                progress = 0f
            )
            
            val steps = 8
            repeat(steps) { step ->
                _lineClearAnimation.value = _lineClearAnimation.value.copy(
                    progress = (step + 1).toFloat() / steps
                )
                delay(25)
            }
            
            // Complete
            _lineClearAnimation.value = LineClearAnimationState()
            game.clearEvent()
        }
    }
    
    // Settings
    fun showSettings() {
        game.pauseGame()
        stopGameLoop()
        _uiState.update { it.copy(showSettings = true) }
    }
    
    fun hideSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }
    
    fun setTheme(themeName: String) {
        _currentTheme.value = GameThemes.getThemeByName(themeName)
        viewModelScope.launch {
            settingsRepository.setThemeName(themeName)
        }
    }
    
    fun setVibration(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(enabled)
        }
    }
    
    fun setSound(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
        }
    }
    
    fun toggleSound() {
        setSound(!_uiState.value.soundEnabled)
    }
    
    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        stopAllRepeats()
    }
}

data class UiState(
    val showSettings: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val highScore: Int = 0
)
