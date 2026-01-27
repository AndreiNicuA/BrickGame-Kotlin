package com.brickgame.tetris.ui.screens

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brickgame.tetris.data.PlayerRepository
import com.brickgame.tetris.data.SettingsRepository
import com.brickgame.tetris.data.ScoreEntry
import com.brickgame.tetris.game.GameEvent
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
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
    private val playerRepository = PlayerRepository(application)
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
    
    // Score history
    private val _scoreHistory = MutableStateFlow<List<ScoreEntry>>(emptyList())
    val scoreHistory: StateFlow<List<ScoreEntry>> = _scoreHistory.asStateFlow()
    
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
            val playerName = playerRepository.playerName.first()
            val isFullscreen = settingsRepository.isFullscreen.first()
            
            _uiState.update {
                it.copy(
                    vibrationEnabled = vibration,
                    soundEnabled = sound,
                    highScore = highScore,
                    playerName = playerName,
                    isFullscreen = isFullscreen
                )
            }
        }
        
        // Load score history
        viewModelScope.launch {
            playerRepository.scoreHistory.collect { history ->
                _scoreHistory.value = history
            }
        }
        
        // Listen to game events
        viewModelScope.launch {
            game.state.collect { state ->
                // Handle line clear animation
                if (state.clearedRows.isNotEmpty()) {
                    playLineClearAnimation(state.clearedRows)
                    // Vibrate on line clear ONLY if enabled
                    if (_uiState.value.vibrationEnabled) {
                        vibrate(50)
                    }
                }
                
                // Handle game over - save score
                if (state.status == GameStatus.GAME_OVER && state.score > 0) {
                    // Save to history with player name
                    val playerName = _uiState.value.playerName
                    playerRepository.addScore(
                        playerName = playerName,
                        score = state.score, 
                        level = state.level, 
                        lines = state.lines
                    )
                    
                    // Update high score if needed
                    if (state.score > _uiState.value.highScore) {
                        _uiState.update { it.copy(highScore = state.score) }
                        settingsRepository.setHighScore(state.score)
                    }
                }
            }
        }
    }
    
    // Game controls
    fun startGame() {
        game.startGame()
        startGameLoop()
    }
    
    fun togglePauseResume() {
        val currentStatus = game.state.value.status
        when (currentStatus) {
            GameStatus.PLAYING -> {
                game.pauseGame()
                stopGameLoop()
            }
            GameStatus.PAUSED -> {
                game.resumeGame()
                startGameLoop()
            }
            GameStatus.MENU, GameStatus.GAME_OVER -> {
                startGame()
            }
        }
    }
    
    fun resetGame() {
        stopGameLoop()
        stopAllRepeats()
        _uiState.update { it.copy(showSettings = false) }
        game.startGame()
        game.pauseGame()
        stopGameLoop()
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
    
    // Movement controls with conditional vibration
    fun moveLeft() {
        if (game.state.value.status == GameStatus.PLAYING) {
            if (game.moveLeft() && _uiState.value.vibrationEnabled) {
                vibrate(10)
            }
        }
    }
    
    fun startLeftRepeat() {
        moveLeft()
        leftRepeatJob?.cancel()
        leftRepeatJob = viewModelScope.launch {
            delay(200)
            while (true) {
                moveLeft()
                delay(50)
            }
        }
    }
    
    fun stopLeftRepeat() {
        leftRepeatJob?.cancel()
        leftRepeatJob = null
    }
    
    fun moveRight() {
        if (game.state.value.status == GameStatus.PLAYING) {
            if (game.moveRight() && _uiState.value.vibrationEnabled) {
                vibrate(10)
            }
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
            val dropped = game.hardDrop()
            if (dropped > 0 && _uiState.value.vibrationEnabled) {
                vibrate(30)
            }
        }
    }
    
    fun rotate() {
        if (game.state.value.status == GameStatus.PLAYING) {
            if (game.rotate() && _uiState.value.vibrationEnabled) {
                vibrate(15)
            }
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
    
    // Vibration helper - ONLY vibrates if enabled
    private fun vibrate(durationMs: Long) {
        // Double-check vibration is enabled
        if (!_uiState.value.vibrationEnabled) return
        
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // Vibration not available
        }
    }
    
    // Line clear animation
    private fun playLineClearAnimation(rows: List<Int>) {
        viewModelScope.launch {
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
            
            _lineClearAnimation.value = LineClearAnimationState()
            game.clearEvent()
        }
    }
    
    // Settings
    fun showSettings() {
        if (game.state.value.status == GameStatus.PLAYING) {
            game.pauseGame()
            stopGameLoop()
        }
        _uiState.update { it.copy(showSettings = true) }
    }
    
    fun hideSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }
    
    fun setPlayerName(name: String) {
        _uiState.update { it.copy(playerName = name) }
        viewModelScope.launch {
            playerRepository.setPlayerName(name)
        }
    }
    
    fun clearScoreHistory() {
        viewModelScope.launch {
            playerRepository.clearHistory()
        }
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
        // Test vibration when enabling
        if (enabled) {
            vibrate(50)
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
    
    fun setFullscreen(enabled: Boolean) {
        _uiState.update { it.copy(isFullscreen = enabled) }
        viewModelScope.launch {
            settingsRepository.setFullscreen(enabled)
        }
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
    val highScore: Int = 0,
    val playerName: String = "Player",
    val isFullscreen: Boolean = false
)
