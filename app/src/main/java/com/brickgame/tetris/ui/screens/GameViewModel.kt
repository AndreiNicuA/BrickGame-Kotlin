package com.brickgame.tetris.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brickgame.tetris.audio.SoundManager
import com.brickgame.tetris.audio.VibrationManager
import com.brickgame.tetris.data.PlayerRepository
import com.brickgame.tetris.data.SettingsRepository
import com.brickgame.tetris.data.ScoreEntry
import com.brickgame.tetris.game.Difficulty
import com.brickgame.tetris.game.GameState
import com.brickgame.tetris.game.GameStatus
import com.brickgame.tetris.game.TetrisGame
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.styles.SoundStyle
import com.brickgame.tetris.ui.styles.StylePreset
import com.brickgame.tetris.ui.styles.VibrationStyle
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
    private val soundManager = SoundManager(application)
    private val vibrationManager = VibrationManager(application)
    
    val gameState: StateFlow<GameState> = game.state
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _currentTheme = MutableStateFlow(GameThemes.Classic)
    val currentTheme: StateFlow<GameTheme> = _currentTheme.asStateFlow()
    
    private val _scoreHistory = MutableStateFlow<List<ScoreEntry>>(emptyList())
    val scoreHistory: StateFlow<List<ScoreEntry>> = _scoreHistory.asStateFlow()
    
    private var gameLoopJob: Job? = null
    private var leftRepeatJob: Job? = null
    private var rightRepeatJob: Job? = null
    private var downRepeatJob: Job? = null
    private var lastLevel = 1
    
    init {
        loadSettings()
        observeScoreHistory()
        observeGameState()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val themeName = settingsRepository.themeName.first()
            _currentTheme.value = GameThemes.getThemeByName(themeName)
            
            val vibrationEnabled = settingsRepository.vibrationEnabled.first()
            val vibrationIntensity = settingsRepository.vibrationIntensity.first()
            val vibrationStyle = try { VibrationStyle.valueOf(settingsRepository.vibrationStyle.first()) } catch (e: Exception) { VibrationStyle.CLASSIC }
            val soundEnabled = settingsRepository.soundEnabled.first()
            val soundVolume = settingsRepository.soundVolume.first()
            val soundStyle = try { SoundStyle.valueOf(settingsRepository.soundStyle.first()) } catch (e: Exception) { SoundStyle.RETRO_BEEP }
            val animationStyle = try { AnimationStyle.valueOf(settingsRepository.animationStyle.first()) } catch (e: Exception) { AnimationStyle.MODERN }
            val stylePreset = try { StylePreset.valueOf(settingsRepository.stylePreset.first()) } catch (e: Exception) { StylePreset.CUSTOM }
            val highScore = settingsRepository.highScore.first()
            val playerName = playerRepository.playerName.first()
            val layoutMode = try { LayoutMode.valueOf(settingsRepository.layoutMode.first()) } catch (e: Exception) { LayoutMode.CLASSIC }
            val ghostPieceEnabled = settingsRepository.ghostPieceEnabled.first()
            val difficulty = try { Difficulty.valueOf(settingsRepository.difficulty.first()) } catch (e: Exception) { Difficulty.NORMAL }
            
            vibrationManager.setEnabled(vibrationEnabled)
            vibrationManager.setIntensity(vibrationIntensity)
            vibrationManager.setVibrationStyle(vibrationStyle)
            soundManager.setEnabled(soundEnabled)
            soundManager.setVolume(soundVolume)
            soundManager.setSoundStyle(soundStyle)
            game.setDifficulty(difficulty)
            
            _uiState.value = UiState(
                vibrationEnabled = vibrationEnabled,
                vibrationIntensity = vibrationIntensity,
                vibrationStyle = vibrationStyle,
                soundEnabled = soundEnabled,
                soundVolume = soundVolume,
                soundStyle = soundStyle,
                animationStyle = animationStyle,
                stylePreset = stylePreset,
                highScore = highScore,
                playerName = playerName,
                layoutMode = layoutMode,
                ghostPieceEnabled = ghostPieceEnabled,
                difficulty = difficulty
            )
        }
    }
    
    private fun observeScoreHistory() {
        viewModelScope.launch { playerRepository.scoreHistory.collect { _scoreHistory.value = it } }
    }
    
    private fun observeGameState() {
        viewModelScope.launch {
            game.state.collect { state ->
                if (state.level > lastLevel && state.status == GameStatus.PLAYING) {
                    vibrationManager.vibrateLevelUp()
                    soundManager.playLevelUp()
                    lastLevel = state.level
                }
                
                if (state.linesCleared > 0) {
                    vibrationManager.vibrateClear(state.linesCleared)
                    soundManager.playClear()
                }
                
                if (state.status == GameStatus.GAME_OVER && state.score > 0) {
                    vibrationManager.vibrateGameOver()
                    soundManager.playGameOver()
                    playerRepository.addScore(_uiState.value.playerName, state.score, state.level, state.lines)
                    if (state.score > _uiState.value.highScore) {
                        _uiState.update { it.copy(highScore = state.score) }
                        settingsRepository.setHighScore(state.score)
                    }
                }
            }
        }
    }
    
    fun startGame() {
        lastLevel = _uiState.value.difficulty.startLevel
        game.setDifficulty(_uiState.value.difficulty)
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
        lastLevel = _uiState.value.difficulty.startLevel
        game.setDifficulty(_uiState.value.difficulty)
        game.startGame()
        game.pauseGame()
    }
    
    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                if (game.state.value.status == GameStatus.PLAYING) game.moveDown()
                delay(game.getDropSpeed())
            }
        }
    }
    
    private fun stopGameLoop() { gameLoopJob?.cancel(); gameLoopJob = null }
    
    fun moveLeft() { if (game.state.value.status == GameStatus.PLAYING && game.moveLeft()) { vibrationManager.vibrateMove(); soundManager.playMove() } }
    fun startLeftRepeat() { moveLeft(); leftRepeatJob?.cancel(); leftRepeatJob = viewModelScope.launch { delay(200); while (true) { moveLeft(); delay(50) } } }
    fun stopLeftRepeat() { leftRepeatJob?.cancel(); leftRepeatJob = null }
    
    fun moveRight() { if (game.state.value.status == GameStatus.PLAYING && game.moveRight()) { vibrationManager.vibrateMove(); soundManager.playMove() } }
    fun startRightRepeat() { moveRight(); rightRepeatJob?.cancel(); rightRepeatJob = viewModelScope.launch { delay(200); while (true) { moveRight(); delay(50) } } }
    fun stopRightRepeat() { rightRepeatJob?.cancel(); rightRepeatJob = null }
    
    fun startDownRepeat() { downRepeatJob?.cancel(); downRepeatJob = viewModelScope.launch { while (true) { if (game.state.value.status == GameStatus.PLAYING) game.moveDown(); delay(50) } } }
    fun stopDownRepeat() { downRepeatJob?.cancel(); downRepeatJob = null }
    
    fun hardDrop() { if (game.state.value.status == GameStatus.PLAYING && game.hardDrop() > 0) { vibrationManager.vibrateDrop(); soundManager.playDrop() } }
    fun rotate() { if (game.state.value.status == GameStatus.PLAYING && game.rotate()) { vibrationManager.vibrateRotate(); soundManager.playRotate() } }
    
    private fun stopAllRepeats() { leftRepeatJob?.cancel(); rightRepeatJob?.cancel(); downRepeatJob?.cancel(); leftRepeatJob = null; rightRepeatJob = null; downRepeatJob = null }
    
    fun showSettings() { if (game.state.value.status == GameStatus.PLAYING) { game.pauseGame(); stopGameLoop() }; _uiState.update { it.copy(showSettings = true) } }
    fun hideSettings() { _uiState.update { it.copy(showSettings = false) } }
    
    fun setPlayerName(name: String) { _uiState.update { it.copy(playerName = name) }; viewModelScope.launch { playerRepository.setPlayerName(name) } }
    fun clearScoreHistory() { viewModelScope.launch { playerRepository.clearHistory() } }
    fun setTheme(name: String) { _currentTheme.value = GameThemes.getThemeByName(name); viewModelScope.launch { settingsRepository.setThemeName(name) } }
    
    fun setVibrationEnabled(enabled: Boolean) { _uiState.update { it.copy(vibrationEnabled = enabled) }; vibrationManager.setEnabled(enabled); viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }; if (enabled) vibrationManager.testVibration() }
    fun setVibrationIntensity(intensity: Float) { _uiState.update { it.copy(vibrationIntensity = intensity) }; vibrationManager.setIntensity(intensity); viewModelScope.launch { settingsRepository.setVibrationIntensity(intensity) } }
    fun setVibrationStyle(style: VibrationStyle) { _uiState.update { it.copy(vibrationStyle = style, stylePreset = StylePreset.CUSTOM) }; vibrationManager.setVibrationStyle(style); viewModelScope.launch { settingsRepository.setVibrationStyle(style.name); settingsRepository.setStylePreset(StylePreset.CUSTOM.name) }; vibrationManager.testVibration() }
    
    fun setSoundEnabled(enabled: Boolean) { _uiState.update { it.copy(soundEnabled = enabled) }; soundManager.setEnabled(enabled); viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }; if (enabled) soundManager.playMove() }
    fun setSoundVolume(volume: Float) { _uiState.update { it.copy(soundVolume = volume) }; soundManager.setVolume(volume); viewModelScope.launch { settingsRepository.setSoundVolume(volume) } }
    fun setSoundStyle(style: SoundStyle) { _uiState.update { it.copy(soundStyle = style, stylePreset = StylePreset.CUSTOM) }; soundManager.setSoundStyle(style); viewModelScope.launch { settingsRepository.setSoundStyle(style.name); settingsRepository.setStylePreset(StylePreset.CUSTOM.name) }; soundManager.playMove() }
    
    fun setAnimationStyle(style: AnimationStyle) { _uiState.update { it.copy(animationStyle = style, stylePreset = StylePreset.CUSTOM) }; viewModelScope.launch { settingsRepository.setAnimationStyle(style.name); settingsRepository.setStylePreset(StylePreset.CUSTOM.name) } }
    
    fun applyStylePreset(preset: StylePreset) {
        _uiState.update { it.copy(stylePreset = preset, animationStyle = preset.animationStyle, vibrationStyle = preset.vibrationStyle, soundStyle = preset.soundStyle) }
        vibrationManager.setVibrationStyle(preset.vibrationStyle)
        soundManager.setSoundStyle(preset.soundStyle)
        viewModelScope.launch { settingsRepository.setStylePreset(preset.name); settingsRepository.setAnimationStyle(preset.animationStyle.name); settingsRepository.setVibrationStyle(preset.vibrationStyle.name); settingsRepository.setSoundStyle(preset.soundStyle.name) }
        if (preset.vibrationStyle != VibrationStyle.NONE) vibrationManager.testVibration()
        if (preset.soundStyle != SoundStyle.NONE) soundManager.playMove()
    }
    
    fun setLayoutMode(mode: LayoutMode) { _uiState.update { it.copy(layoutMode = mode) }; viewModelScope.launch { settingsRepository.setLayoutMode(mode.name) } }
    fun setGhostPieceEnabled(enabled: Boolean) { _uiState.update { it.copy(ghostPieceEnabled = enabled) }; viewModelScope.launch { settingsRepository.setGhostPieceEnabled(enabled) } }
    fun setDifficulty(diff: Difficulty) { _uiState.update { it.copy(difficulty = diff) }; game.setDifficulty(diff); viewModelScope.launch { settingsRepository.setDifficulty(diff.name) } }
    
    override fun onCleared() { super.onCleared(); stopGameLoop(); stopAllRepeats(); soundManager.release() }
}

data class UiState(
    val showSettings: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Float = 0.7f,
    val vibrationStyle: VibrationStyle = VibrationStyle.CLASSIC,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val soundStyle: SoundStyle = SoundStyle.RETRO_BEEP,
    val animationStyle: AnimationStyle = AnimationStyle.MODERN,
    val stylePreset: StylePreset = StylePreset.CUSTOM,
    val highScore: Int = 0,
    val playerName: String = "Player",
    val layoutMode: LayoutMode = LayoutMode.CLASSIC,
    val ghostPieceEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.NORMAL
)
