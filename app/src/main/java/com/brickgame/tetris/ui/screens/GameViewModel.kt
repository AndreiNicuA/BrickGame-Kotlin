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
import com.brickgame.tetris.audio.SoundManager
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
    private val soundManager = SoundManager(application)
    
    // Vibrator
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
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
    
    init {
        // Load settings
        viewModelScope.launch {
            val themeName = settingsRepository.themeName.first()
            _currentTheme.value = GameThemes.getThemeByName(themeName)
            
            val vibrationEnabled = settingsRepository.vibrationEnabled.first()
            val vibrationIntensity = settingsRepository.vibrationIntensity.first()
            val soundEnabled = settingsRepository.soundEnabled.first()
            val soundVolume = settingsRepository.soundVolume.first()
            val highScore = settingsRepository.highScore.first()
            val playerName = playerRepository.playerName.first()
            val layoutModeStr = settingsRepository.layoutMode.first()
            val layoutMode = try { LayoutMode.valueOf(layoutModeStr) } catch (e: Exception) { LayoutMode.CLASSIC }
            
            soundManager.setEnabled(soundEnabled)
            soundManager.setVolume(soundVolume)
            
            _uiState.value = UiState(
                vibrationEnabled = vibrationEnabled,
                vibrationIntensity = vibrationIntensity,
                soundEnabled = soundEnabled,
                soundVolume = soundVolume,
                highScore = highScore,
                playerName = playerName,
                layoutMode = layoutMode
            )
            
            Log.d(TAG, "Settings loaded: vibration=$vibrationEnabled($vibrationIntensity), sound=$soundEnabled($soundVolume)")
        }
        
        // Collect score history
        viewModelScope.launch {
            playerRepository.scoreHistory.collect { _scoreHistory.value = it }
        }
        
        // Watch game state
        viewModelScope.launch {
            game.state.collect { state ->
                // Game over
                if (state.status == GameStatus.GAME_OVER && state.score > 0) {
                    soundManager.playGameOver()
                    
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
    
    // VIBRATION with intensity
    private fun vibrate(baseDurationMs: Long) {
        if (!_uiState.value.vibrationEnabled) return
        
        val intensity = _uiState.value.vibrationIntensity
        val duration = (baseDurationMs * intensity).toLong().coerceAtLeast(5)
        
        vibrator?.let { vib ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitude = (255 * intensity).toInt().coerceIn(1, 255)
                    vib.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(duration)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration error", e)
            }
        }
    }
    
    // Game controls
    fun startGame() {
        game.startGame()
        startGameLoop()
    }
    
    fun togglePauseResume() {
        when (game.state.value.status) {
            GameStatus.PLAYING -> { game.pauseGame(); stopGameLoop() }
            GameStatus.PAUSED -> { game.resumeGame(); startGameLoop() }
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
    
    // Movement with feedback
    fun moveLeft() {
        if (game.state.value.status == GameStatus.PLAYING && game.moveLeft()) {
            vibrate(10)
            soundManager.playMove()
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
            vibrate(10)
            soundManager.playMove()
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
        if (game.state.value.status == GameStatus.PLAYING && game.hardDrop() > 0) {
            vibrate(30)
            soundManager.playDrop()
        }
    }
    
    fun rotate() {
        if (game.state.value.status == GameStatus.PLAYING && game.rotate()) {
            vibrate(15)
            soundManager.playRotate()
        }
    }
    
    // Called when lines are cleared
    fun onLinesClear() {
        vibrate(50)
        soundManager.playClear()
    }
    
    private fun stopAllRepeats() {
        leftRepeatJob?.cancel(); rightRepeatJob?.cancel(); downRepeatJob?.cancel()
        leftRepeatJob = null; rightRepeatJob = null; downRepeatJob = null
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
        viewModelScope.launch { playerRepository.setPlayerName(name) }
    }
    
    fun clearScoreHistory() {
        viewModelScope.launch { playerRepository.clearHistory() }
    }
    
    fun setTheme(themeName: String) {
        _currentTheme.value = GameThemes.getThemeByName(themeName)
        viewModelScope.launch { settingsRepository.setThemeName(themeName) }
    }
    
    // Vibration settings
    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
        if (enabled) vibrate(100)
    }
    
    fun setVibrationIntensity(intensity: Float) {
        _uiState.update { it.copy(vibrationIntensity = intensity) }
        viewModelScope.launch { settingsRepository.setVibrationIntensity(intensity) }
        vibrate(50) // Test vibration
    }
    
    // Sound settings
    fun setSoundEnabled(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
        soundManager.setEnabled(enabled)
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
        if (enabled) soundManager.playMove() // Test sound
    }
    
    fun setSoundVolume(volume: Float) {
        _uiState.update { it.copy(soundVolume = volume) }
        soundManager.setVolume(volume)
        viewModelScope.launch { settingsRepository.setSoundVolume(volume) }
        soundManager.playMove() // Test sound
    }
    
    fun setLayoutMode(mode: LayoutMode) {
        _uiState.update { it.copy(layoutMode = mode) }
        viewModelScope.launch { settingsRepository.setLayoutMode(mode.name) }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        stopAllRepeats()
        soundManager.release()
    }
}

data class UiState(
    val showSettings: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Float = 0.7f,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val highScore: Int = 0,
    val playerName: String = "Player",
    val layoutMode: LayoutMode = LayoutMode.CLASSIC
)
