package com.brickgame.tetris.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brickgame.tetris.audio.SoundManager
import com.brickgame.tetris.audio.VibrationManager
import com.brickgame.tetris.data.PlayerRepository
import com.brickgame.tetris.data.SettingsRepository
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.layout.DPadStyle
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.styles.SoundStyle
import com.brickgame.tetris.ui.styles.VibrationStyle
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // ===== Repositories =====
    private val settingsRepo = SettingsRepository(application)
    private val playerRepo = PlayerRepository(application)
    val soundManager = SoundManager(application)
    val vibrationManager = VibrationManager(application)

    // ===== Game Engine =====
    private val game = TetrisGame()
    val gameState: StateFlow<GameState> = game.state

    // ===== UI State =====
    data class UiState(
        val showSettings: Boolean = false,
        val settingsPage: SettingsPage = SettingsPage.MAIN,
        val showAbout: Boolean = false
    )

    enum class SettingsPage { MAIN, PROFILE, THEME, LAYOUT, GAMEPLAY, EXPERIENCE, ABOUT }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ===== Theme =====
    private val _currentTheme = MutableStateFlow(GameThemes.ClassicGreen)
    val currentTheme: StateFlow<GameTheme> = _currentTheme.asStateFlow()

    // ===== Layout =====
    private val _portraitLayout = MutableStateFlow(LayoutPreset.PORTRAIT_CLASSIC)
    val portraitLayout: StateFlow<LayoutPreset> = _portraitLayout.asStateFlow()

    private val _landscapeLayout = MutableStateFlow(LayoutPreset.LANDSCAPE_DEFAULT)
    val landscapeLayout: StateFlow<LayoutPreset> = _landscapeLayout.asStateFlow()

    private val _dpadStyle = MutableStateFlow(DPadStyle.STANDARD)
    val dpadStyle: StateFlow<DPadStyle> = _dpadStyle.asStateFlow()

    // ===== Gameplay Settings =====
    private val _ghostPieceEnabled = MutableStateFlow(true)
    val ghostPieceEnabled: StateFlow<Boolean> = _ghostPieceEnabled.asStateFlow()

    private val _difficulty = MutableStateFlow(Difficulty.NORMAL)
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    private val _gameMode = MutableStateFlow(GameMode.MARATHON)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    // ===== Experience =====
    private val _animationStyle = MutableStateFlow(AnimationStyle.MODERN)
    val animationStyle: StateFlow<AnimationStyle> = _animationStyle.asStateFlow()

    private val _animationDuration = MutableStateFlow(0.5f)
    val animationDuration: StateFlow<Float> = _animationDuration.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // ===== Player =====
    val playerName: StateFlow<String> = playerRepo.playerName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Player")
    val scoreHistory = playerRepo.scoreHistory
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // ===== Game Loop =====
    private var gameLoopJob: Job? = null
    private var lockDelayJob: Job? = null
    private var dasJob: Job? = null
    private var lineClearJob: Job? = null

    // DAS/ARR
    private var dasDelayMs = 170L
    private var arrSpeedMs = 50L

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Theme
            settingsRepo.themeName.collect { name ->
                _currentTheme.value = GameThemes.getThemeByName(name)
            }
        }
        viewModelScope.launch {
            settingsRepo.ghostPieceEnabled.collect { _ghostPieceEnabled.value = it }
        }
        viewModelScope.launch {
            settingsRepo.difficulty.collect { name ->
                val diff = Difficulty.entries.find { it.name == name } ?: Difficulty.NORMAL
                _difficulty.value = diff
                game.setDifficulty(diff)
            }
        }
        viewModelScope.launch {
            settingsRepo.highScore.collect { _highScore.value = it }
        }
        viewModelScope.launch {
            settingsRepo.soundEnabled.collect { enabled ->
                _soundEnabled.value = enabled
                soundManager.setEnabled(enabled)
            }
        }
        viewModelScope.launch {
            settingsRepo.soundVolume.collect { soundManager.setVolume(it) }
        }
        viewModelScope.launch {
            settingsRepo.soundStyle.collect { name ->
                val style = SoundStyle.entries.find { it.name == name } ?: SoundStyle.RETRO_BEEP
                soundManager.setSoundStyle(style)
            }
        }
        viewModelScope.launch {
            settingsRepo.vibrationEnabled.collect { enabled ->
                _vibrationEnabled.value = enabled
                vibrationManager.setEnabled(enabled)
            }
        }
        viewModelScope.launch {
            settingsRepo.vibrationIntensity.collect { vibrationManager.setIntensity(it) }
        }
        viewModelScope.launch {
            settingsRepo.vibrationStyle.collect { name ->
                val style = VibrationStyle.entries.find { it.name == name } ?: VibrationStyle.CLASSIC
                vibrationManager.setVibrationStyle(style)
            }
        }
        viewModelScope.launch {
            settingsRepo.animationStyle.collect { name ->
                _animationStyle.value = AnimationStyle.entries.find { it.name == name } ?: AnimationStyle.MODERN
            }
        }
        viewModelScope.launch {
            settingsRepo.animationDuration.collect { _animationDuration.value = it }
        }
    }

    // ===== Game Actions =====

    fun startGame() {
        game.setDifficulty(_difficulty.value)
        game.setGameMode(_gameMode.value)
        game.startGame()
        startGameLoop()
    }

    fun pauseGame() {
        game.pauseGame()
        stopGameLoop()
    }

    fun resumeGame() {
        game.resumeGame()
        startGameLoop()
    }

    fun togglePause() {
        if (gameState.value.status == GameStatus.PLAYING) pauseGame()
        else if (gameState.value.status == GameStatus.PAUSED) resumeGame()
    }

    // ===== Movement =====

    fun moveLeft() {
        if (game.moveLeft()) {
            soundManager.playMove()
            vibrationManager.vibrateMove()
        }
    }

    fun moveRight() {
        if (game.moveRight()) {
            soundManager.playMove()
            vibrationManager.vibrateMove()
        }
    }

    fun softDrop() {
        game.moveDown()
    }

    fun hardDrop() {
        val distance = game.hardDrop()
        if (distance > 0) {
            soundManager.playDrop()
            vibrationManager.vibrateDrop()
        }
    }

    fun rotate() {
        if (game.rotate()) {
            soundManager.playRotate()
            vibrationManager.vibrateRotate()
        }
    }

    fun rotateCounterClockwise() {
        if (game.rotateCounterClockwise()) {
            soundManager.playRotate()
            vibrationManager.vibrateRotate()
        }
    }

    fun holdPiece() {
        game.holdCurrentPiece()
    }

    // ===== DAS (Delayed Auto Shift) =====

    fun startLeftDAS() {
        stopDAS()
        moveLeft()
        dasJob = viewModelScope.launch {
            delay(dasDelayMs)
            while (isActive) {
                moveLeft()
                delay(arrSpeedMs)
            }
        }
    }

    fun startRightDAS() {
        stopDAS()
        moveRight()
        dasJob = viewModelScope.launch {
            delay(dasDelayMs)
            while (isActive) {
                moveRight()
                delay(arrSpeedMs)
            }
        }
    }

    fun startDownDAS() {
        stopDAS()
        softDrop()
        dasJob = viewModelScope.launch {
            delay(dasDelayMs)
            while (isActive) {
                softDrop()
                delay(arrSpeedMs)
            }
        }
    }

    fun stopDAS() {
        dasJob?.cancel()
        dasJob = null
    }

    // ===== Game Loop =====

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        lockDelayJob?.cancel()

        gameLoopJob = viewModelScope.launch {
            while (isActive && gameState.value.status == GameStatus.PLAYING) {
                if (game.isGameActive()) {
                    val prevLevel = gameState.value.level
                    val result = game.moveDown()

                    if (result == MoveResult.BLOCKED && game.isGameActive()) {
                        // Start lock delay check
                    }

                    // Line clear animation
                    if (game.isPendingLineClear()) {
                        val state = gameState.value
                        val linesCount = state.linesCleared
                        if (linesCount > 0) {
                            soundManager.playClear()
                            vibrationManager.vibrateClear(linesCount)
                        }

                        lineClearJob?.cancel()
                        lineClearJob = launch {
                            delay((_animationDuration.value * 500).toLong().coerceAtLeast(100))
                            game.completePendingLineClear()

                            val newState = gameState.value
                            if (newState.level > prevLevel) {
                                soundManager.playLevelUp()
                                vibrationManager.vibrateLevelUp()
                            }
                            if (newState.status == GameStatus.GAME_OVER) {
                                onGameOver()
                            }
                        }
                    }
                }

                delay(game.getDropSpeed())
            }

            if (gameState.value.status == GameStatus.GAME_OVER) {
                onGameOver()
            }
        }

        // Lock delay checker (60fps)
        lockDelayJob = viewModelScope.launch {
            while (isActive && gameState.value.status == GameStatus.PLAYING) {
                if (game.checkLockDelay()) {
                    if (game.isPendingLineClear()) {
                        // Line clear will handle spawning
                    } else if (gameState.value.status == GameStatus.GAME_OVER) {
                        onGameOver()
                    }
                }
                delay(16L)
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        lockDelayJob?.cancel()
        lineClearJob?.cancel()
    }

    private fun onGameOver() {
        stopGameLoop()
        soundManager.playGameOver()
        vibrationManager.vibrateGameOver()

        val state = gameState.value
        viewModelScope.launch {
            if (state.score > _highScore.value) {
                settingsRepo.setHighScore(state.score)
            }
            playerRepo.addScore(
                playerName = playerName.value,
                score = state.score,
                level = state.level,
                lines = state.lines
            )
        }
    }

    // ===== Settings Navigation =====

    fun openSettings() {
        if (gameState.value.status == GameStatus.PLAYING) pauseGame()
        _uiState.update { it.copy(showSettings = true, settingsPage = SettingsPage.MAIN) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun navigateSettings(page: SettingsPage) {
        _uiState.update { it.copy(settingsPage = page) }
    }

    // ===== Settings Updates =====

    fun setTheme(theme: GameTheme) {
        _currentTheme.value = theme
        viewModelScope.launch { settingsRepo.setThemeName(theme.name) }
    }

    fun setGhostPieceEnabled(enabled: Boolean) {
        _ghostPieceEnabled.value = enabled
        viewModelScope.launch { settingsRepo.setGhostPieceEnabled(enabled) }
    }

    fun setDifficulty(diff: Difficulty) {
        _difficulty.value = diff
        game.setDifficulty(diff)
        viewModelScope.launch { settingsRepo.setDifficulty(diff.name) }
    }

    fun setGameMode(mode: GameMode) {
        _gameMode.value = mode
    }

    fun setPortraitLayout(preset: LayoutPreset) {
        _portraitLayout.value = preset
    }

    fun setLandscapeLayout(preset: LayoutPreset) {
        _landscapeLayout.value = preset
    }

    fun setDPadStyle(style: DPadStyle) {
        _dpadStyle.value = style
    }

    fun setAnimationStyle(style: AnimationStyle) {
        _animationStyle.value = style
        viewModelScope.launch { settingsRepo.setAnimationStyle(style.name) }
    }

    fun setAnimationDuration(duration: Float) {
        _animationDuration.value = duration
        viewModelScope.launch { settingsRepo.setAnimationDuration(duration) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        soundManager.setEnabled(enabled)
        viewModelScope.launch { settingsRepo.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        vibrationManager.setEnabled(enabled)
        viewModelScope.launch { settingsRepo.setVibrationEnabled(enabled) }
    }

    fun setPlayerName(name: String) {
        viewModelScope.launch { playerRepo.setPlayerName(name) }
    }

    fun toggleSound() {
        setSoundEnabled(!_soundEnabled.value)
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        soundManager.release()
    }
}
