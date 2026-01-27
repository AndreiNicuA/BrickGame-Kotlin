package com.brickgame.tetris.ui.screens

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brickgame.tetris.data.PlayerRepository
import com.brickgame.tetris.data.SettingsRepository
import com.brickgame.tetris.data.ScoreEntry
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
    
    companion object {
        private const val TAG = "GameViewModel"
    }
    
    private val settingsRepository = SettingsRepository(application)
    private val playerRepository = PlayerRepository(application)
    private val game = TetrisGame()
    
    // Vibrator - get once and cache
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get vibrator", e)
        null
    }
    
    val gameState: StateFlow<GameState> = game.state
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _lineClearAnimation = MutableStateFlow(LineClearAnimationState())
    val lineClearAnimation: StateFlow<LineClearAnimationState> = _lineClearAnimation.asStateFlow()
    
    private val _currentTheme = MutableStateFlow(GameThemes.Classic)
    val currentTheme: StateFlow<GameTheme> = _currentTheme.asStateFlow()
    
    private val _scoreHistory = MutableStateFlow<List<ScoreEntry>>(emptyList())
    val scoreHistory: StateFlow<List<ScoreEntry>> = _scoreHistory.asStateFlow()
    
    private var gameLoopJob: Job? = null
    private var leftRepeatJob: Job? = null
    private var rightRepeatJob: Job? = null
    private var downRepeatJob: Job? = null
    
    // Track current vibration setting locally for immediate effect
    private var vibrationOn = true
    
    init {
        viewModelScope.launch {
            val themeName = settingsRepository.themeName.first()
            _currentTheme.value = GameThemes.getThemeByName(themeName)
            
            val vibration = settingsRepository.vibrationEnabled.first()
            val sound = settingsRepository.soundEnabled.first()
            val highScore = settingsRepository.highScore.first()
            val playerName = playerRepository.playerName.first()
            val layoutModeStr = settingsRepository.layoutMode.first()
            val layoutMode = try { LayoutMode.valueOf(layoutModeStr) } catch (e: Exception) { LayoutMode.CLASSIC }
            
            vibrationOn = vibration
            
            _uiState.update {
                it.copy(
                    vibrationEnabled = vibration,
                    soundEnabled = sound,
                    highScore = highScore,
                    playerName = playerName,
                    layoutMode = layoutMode
                )
            }
            
            Log.d(TAG, "Loaded settings: vibration=$vibration, sound=$sound")
        }
        
        viewModelScope.launch {
            playerRepository.scoreHistory.collect { _scoreHistory.value = it }
        }
        
        viewModelScope.launch {
            game.state.collect { state ->
                if (state.clearedRows.isNotEmpty()) {
                    playLineClearAnimation(state.clearedRows)
                    doVibrate(50)
                }
                
                if (state.status == GameStatus.GAME_OVER && state.score > 0) {
                    val playerName = _uiState.value.playerName
                    playerRepository.addScore(playerName, state.score, state.level, state.lines)
                    
                    if (state.score > _uiState.value.highScore) {
                        _uiState.update { it.copy(highScore = state.score) }
                        settingsRepository.setHighScore(state.score)
                    }
                }
            }
        }
    }
    
    // VIBRATION - uses local flag for immediate effect
    private fun doVibrate(durationMs: Long) {
        if (!vibrationOn) {
            Log.d(TAG, "Vibration disabled, skipping")
            return
        }
        
        vibrator?.let { vib ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(durationMs)
                }
                Log.d(TAG, "Vibrated for ${durationMs}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Vibration failed", e)
            }
        } ?: Log.w(TAG, "No vibrator available")
    }
    
    fun startGame() {
        game.startGame()
        startGameLoop()
    }
    
    fun togglePauseResume() {
        when (game.state.value.status) {
            GameStatus.PLAYING -> {
                game.pauseGame()
                stopGameLoop()
            }
            GameStatus.PAUSED -> {
                game.resumeGame()
                startGameLoop()
            }
            GameStatus.MENU, GameStatus.GAME_OVER -> startGame()
        }
    }
    
    fun resetGame() {
        stopGameLoop()
        stopAllRepeats()
        game.startGame()
        game.pauseGame()
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
    
    fun moveLeft() {
        if (game.state.value.status == GameStatus.PLAYING && game.moveLeft()) {
            doVibrate(10)
        }
    }
    
    fun startLeftRepeat() {
        moveLeft()
        leftRepeatJob?.cancel()
        leftRepeatJob = viewModelScope.launch {
            delay(200)
            while (true) { moveLeft(); delay(50) }
        }
    }
    
    fun stopLeftRepeat() { leftRepeatJob?.cancel(); leftRepeatJob = null }
    
    fun moveRight() {
        if (game.state.value.status == GameStatus.PLAYING && game.moveRight()) {
            doVibrate(10)
        }
    }
    
    fun startRightRepeat() {
        moveRight()
        rightRepeatJob?.cancel()
        rightRepeatJob = viewModelScope.launch {
            delay(200)
            while (true) { moveRight(); delay(50) }
        }
    }
    
    fun stopRightRepeat() { rightRepeatJob?.cancel(); rightRepeatJob = null }
    
    fun startDownRepeat() {
        downRepeatJob?.cancel()
        downRepeatJob = viewModelScope.launch {
            while (true) {
                if (game.state.value.status == GameStatus.PLAYING) game.moveDown()
                delay(50)
            }
        }
    }
    
    fun stopDownRepeat() { downRepeatJob?.cancel(); downRepeatJob = null }
    
    fun hardDrop() {
        if (game.state.value.status == GameStatus.PLAYING) {
            val dropped = game.hardDrop()
            if (dropped > 0) doVibrate(30)
        }
    }
    
    fun rotate() {
        if (game.state.value.status == GameStatus.PLAYING && game.rotate()) {
            doVibrate(15)
        }
    }
    
    private fun stopAllRepeats() {
        leftRepeatJob?.cancel(); rightRepeatJob?.cancel(); downRepeatJob?.cancel()
        leftRepeatJob = null; rightRepeatJob = null; downRepeatJob = null
    }
    
    private fun playLineClearAnimation(rows: List<Int>) {
        viewModelScope.launch {
            _lineClearAnimation.value = LineClearAnimationState(true, LineClearPhase.FLASH, rows, 0f)
            repeat(3) {
                _lineClearAnimation.value = _lineClearAnimation.value.copy(progress = 1f)
                delay(60)
                _lineClearAnimation.value = _lineClearAnimation.value.copy(progress = 0f)
                delay(60)
            }
            _lineClearAnimation.value = _lineClearAnimation.value.copy(phase = LineClearPhase.COLLAPSE, progress = 0f)
            repeat(8) { step ->
                _lineClearAnimation.value = _lineClearAnimation.value.copy(progress = (step + 1).toFloat() / 8)
                delay(25)
            }
            _lineClearAnimation.value = LineClearAnimationState()
            game.clearEvent()
        }
    }
    
    // SETTINGS
    fun showSettings() {
        if (game.state.value.status == GameStatus.PLAYING) {
            game.pauseGame()
            stopGameLoop()
        }
        _uiState.update { it.copy(showSettings = true) }
    }
    
    fun hideSettings() { _uiState.update { it.copy(showSettings = false) } }
    
    fun setPlayerName(name: String) {
        _uiState.update { it.copy(playerName = name) }
        viewModelScope.launch { playerRepository.setPlayerName(name) }
    }
    
    fun clearScoreHistory() {
        viewModelScope.launch { playerRepository.clearHistory() }
    }
    
    fun setTheme(themeName: String) {
        _currentTheme.value = GameThemes.getThemeByName(themeName)
        viewModelScope.launch { settingsRepository.setThemeName(themeName) }
    }
    
    // VIBRATION TOGGLE - immediate effect
    fun setVibration(enabled: Boolean) {
        Log.d(TAG, "setVibration: $enabled")
        vibrationOn = enabled  // Update local flag immediately
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
        
        // Test vibration when enabling
        if (enabled) {
            doVibrate(100)
        }
    }
    
    // SOUND TOGGLE
    fun setSound(enabled: Boolean) {
        Log.d(TAG, "setSound: $enabled")
        _uiState.update { it.copy(soundEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }
    
    fun toggleSound() { setSound(!_uiState.value.soundEnabled) }
    
    fun setLayoutMode(mode: LayoutMode) {
        _uiState.update { it.copy(layoutMode = mode) }
        viewModelScope.launch { settingsRepository.setLayoutMode(mode.name) }
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
    val layoutMode: LayoutMode = LayoutMode.CLASSIC
)
