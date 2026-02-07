package com.brickgame.tetris.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brickgame.tetris.audio.SoundManager
import com.brickgame.tetris.audio.VibrationManager
import com.brickgame.tetris.data.PlayerRepository
import com.brickgame.tetris.data.ScoreEntry
import com.brickgame.tetris.data.SettingsRepository
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.styles.SoundStyle
import com.brickgame.tetris.ui.styles.StylePreset
import com.brickgame.tetris.ui.styles.VibrationStyle
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes
import com.brickgame.tetris.ui.layout.LayoutPresets
import com.brickgame.tetris.ui.layout.LayoutProfile
import com.brickgame.tetris.ui.layout.LayoutRepository
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

    private val _clearingLines = MutableStateFlow<List<Int>>(emptyList())
    val clearingLines: StateFlow<List<Int>> = _clearingLines.asStateFlow()

    private var gameLoopJob: Job? = null
    private var lockDelayJob: Job? = null
    private var leftRepeatJob: Job? = null
    private var rightRepeatJob: Job? = null
    private var downRepeatJob: Job? = null
    private var animationJob: Job? = null
    private var lastLevel = 1

    // DAS/ARR settings (ms)
    private var dasDelay = 170L   // Delayed Auto Shift
    private var arrSpeed = 50L    // Auto Repeat Rate

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
            val vibrationStyle = try { VibrationStyle.valueOf(settingsRepository.vibrationStyle.first()) } catch (_: Exception) { VibrationStyle.CLASSIC }
            val soundEnabled = settingsRepository.soundEnabled.first()
            val soundVolume = settingsRepository.soundVolume.first()
            val soundStyle = try { SoundStyle.valueOf(settingsRepository.soundStyle.first()) } catch (_: Exception) { SoundStyle.RETRO_BEEP }
            val animationEnabled = settingsRepository.animationEnabled.first()
            val animationStyle = try { AnimationStyle.valueOf(settingsRepository.animationStyle.first()) } catch (_: Exception) { AnimationStyle.MODERN }
            val animationDuration = settingsRepository.animationDuration.first()
            val stylePreset = try { StylePreset.valueOf(settingsRepository.stylePreset.first()) } catch (_: Exception) { StylePreset.CUSTOM }
            val highScore = settingsRepository.highScore.first()
            val playerName = playerRepository.playerName.first()
            val layoutModeStr = settingsRepository.layoutMode.first()
            val layoutMode = try { LayoutMode.valueOf(layoutModeStr) } catch (_: Exception) { LayoutMode.CLASSIC }
            val ghostPieceEnabled = settingsRepository.ghostPieceEnabled.first()
            val difficulty = try { Difficulty.valueOf(settingsRepository.difficulty.first()) } catch (_: Exception) { Difficulty.NORMAL }

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
                animationEnabled = animationEnabled,
                animationStyle = animationStyle,
                animationDuration = animationDuration,
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
        viewModelScope.launch {
            playerRepository.scoreHistory.collect { _scoreHistory.value = it }
        }
    }

    private fun observeGameState() {
        viewModelScope.launch {
            game.state.collect { state ->
                // Level up feedback
                if (state.level > lastLevel && state.status == GameStatus.PLAYING) {
                    vibrationManager.vibrateLevelUp()
                    soundManager.playLevelUp()
                    lastLevel = state.level
                }

                // Handle line clear animation
                if (state.linesCleared > 0 && state.clearedLineRows.isNotEmpty()) {
                    vibrationManager.vibrateClear(state.linesCleared)
                    soundManager.playClear()

                    _clearingLines.value = state.clearedLineRows
                    animationJob?.cancel()

                    if (_uiState.value.animationEnabled) {
                        animationJob = viewModelScope.launch {
                            delay((_uiState.value.animationDuration * 500).toLong().coerceAtLeast(100))
                            _clearingLines.value = emptyList()
                            game.completePendingLineClear()
                        }
                    } else {
                        _clearingLines.value = emptyList()
                        game.completePendingLineClear()
                    }
                }

                // Game over
                if (state.status == GameStatus.GAME_OVER && state.score > 0) {
                    vibrationManager.vibrateGameOver()
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

    // ===== Game Control =====

    fun startGame() {
        lastLevel = _uiState.value.difficulty.startLevel
        game.setDifficulty(_uiState.value.difficulty)
        game.setGameMode(_uiState.value.gameMode)
        game.startGame()
        startGameLoop()
    }

    fun pauseGame() {
        if (game.state.value.status == GameStatus.PLAYING) {
            game.pauseGame()
            stopGameLoop()
        }
    }

    fun resumeGame() {
        if (game.state.value.status == GameStatus.PAUSED) {
            game.resumeGame()
            startGameLoop()
        }
    }

    fun resetGame() {
        stopGameLoop()
        stopAllRepeats()
        animationJob?.cancel()
        _clearingLines.value = emptyList()
        lastLevel = _uiState.value.difficulty.startLevel
        game.setDifficulty(_uiState.value.difficulty)
        game.setGameMode(_uiState.value.gameMode)
        game.startGame()
        game.pauseGame()
    }

    // ===== Game Loop with Lock Delay =====

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        lockDelayJob?.cancel()

        // Gravity loop
        gameLoopJob = viewModelScope.launch {
            while (true) {
                if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
                    game.moveDown()
                }
                delay(game.getDropSpeed())
            }
        }

        // Lock delay loop (checks every 16ms ~60fps)
        lockDelayJob = viewModelScope.launch {
            while (true) {
                if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
                    game.checkLockDelay()
                }
                delay(16L)
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        lockDelayJob?.cancel()
        lockDelayJob = null
    }

    // ===== Input Actions =====

    fun moveLeft() {
        if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
            if (game.moveLeft()) {
                vibrationManager.vibrateMove()
                soundManager.playMove()
            }
        }
    }

    fun startLeftRepeat() {
        moveLeft()
        leftRepeatJob?.cancel()
        leftRepeatJob = viewModelScope.launch {
            delay(dasDelay)
            while (true) {
                moveLeft()
                delay(arrSpeed)
            }
        }
    }

    fun stopLeftRepeat() {
        leftRepeatJob?.cancel()
        leftRepeatJob = null
    }

    fun moveRight() {
        if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
            if (game.moveRight()) {
                vibrationManager.vibrateMove()
                soundManager.playMove()
            }
        }
    }

    fun startRightRepeat() {
        moveRight()
        rightRepeatJob?.cancel()
        rightRepeatJob = viewModelScope.launch {
            delay(dasDelay)
            while (true) {
                moveRight()
                delay(arrSpeed)
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
                if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
                    game.moveDown()
                }
                delay(arrSpeed)
            }
        }
    }

    fun stopDownRepeat() {
        downRepeatJob?.cancel()
        downRepeatJob = null
    }

    fun hardDrop() {
        if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
            if (game.hardDrop() > 0) {
                vibrationManager.vibrateDrop()
                soundManager.playDrop()
            }
        }
    }

    fun rotate() {
        if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
            if (game.rotate()) {
                vibrationManager.vibrateRotate()
                soundManager.playRotate()
            }
        }
    }

    fun rotateCounterClockwise() {
        if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
            if (game.rotateCounterClockwise()) {
                vibrationManager.vibrateRotate()
                soundManager.playRotate()
            }
        }
    }

    fun holdPiece() {
        if (game.state.value.status == GameStatus.PLAYING && !game.isPendingLineClear()) {
            if (game.holdCurrentPiece()) {
                vibrationManager.vibrateMove()
                soundManager.playMove()
            }
        }
    }

    private fun stopAllRepeats() {
        leftRepeatJob?.cancel(); leftRepeatJob = null
        rightRepeatJob?.cancel(); rightRepeatJob = null
        downRepeatJob?.cancel(); downRepeatJob = null
    }

    // ===== Settings =====

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

    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        vibrationManager.setEnabled(enabled)
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
        if (enabled) vibrationManager.testVibration()
    }

    fun setVibrationIntensity(intensity: Float) {
        _uiState.update { it.copy(vibrationIntensity = intensity) }
        vibrationManager.setIntensity(intensity)
        viewModelScope.launch { settingsRepository.setVibrationIntensity(intensity) }
    }

    fun setVibrationStyle(style: VibrationStyle) {
        _uiState.update { it.copy(vibrationStyle = style, stylePreset = StylePreset.CUSTOM) }
        vibrationManager.setVibrationStyle(style)
        viewModelScope.launch {
            settingsRepository.setVibrationStyle(style.name)
            settingsRepository.setStylePreset(StylePreset.CUSTOM.name)
        }
        vibrationManager.testVibration()
    }

    fun setSoundEnabled(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
        soundManager.setEnabled(enabled)
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
        if (enabled) soundManager.playMove()
    }

    fun setSoundVolume(volume: Float) {
        _uiState.update { it.copy(soundVolume = volume) }
        soundManager.setVolume(volume)
        viewModelScope.launch { settingsRepository.setSoundVolume(volume) }
    }

    fun setSoundStyle(style: SoundStyle) {
        _uiState.update { it.copy(soundStyle = style, stylePreset = StylePreset.CUSTOM) }
        soundManager.setSoundStyle(style)
        viewModelScope.launch {
            settingsRepository.setSoundStyle(style.name)
            settingsRepository.setStylePreset(StylePreset.CUSTOM.name)
        }
        soundManager.playMove()
    }

    fun setAnimationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(animationEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setAnimationEnabled(enabled) }
    }

    fun setAnimationStyle(style: AnimationStyle) {
        _uiState.update { it.copy(animationStyle = style, stylePreset = StylePreset.CUSTOM) }
        viewModelScope.launch {
            settingsRepository.setAnimationStyle(style.name)
            settingsRepository.setStylePreset(StylePreset.CUSTOM.name)
        }
    }

    fun setAnimationDuration(duration: Float) {
        _uiState.update { it.copy(animationDuration = duration) }
        viewModelScope.launch { settingsRepository.setAnimationDuration(duration) }
    }

    fun applyStylePreset(preset: StylePreset) {
        _uiState.update {
            it.copy(
                stylePreset = preset,
                animationStyle = preset.animationStyle,
                vibrationStyle = preset.vibrationStyle,
                soundStyle = preset.soundStyle
            )
        }
        vibrationManager.setVibrationStyle(preset.vibrationStyle)
        soundManager.setSoundStyle(preset.soundStyle)
        viewModelScope.launch {
            settingsRepository.setStylePreset(preset.name)
            settingsRepository.setAnimationStyle(preset.animationStyle.name)
            settingsRepository.setVibrationStyle(preset.vibrationStyle.name)
            settingsRepository.setSoundStyle(preset.soundStyle.name)
        }
        if (preset.vibrationStyle != VibrationStyle.NONE) vibrationManager.testVibration()
        if (preset.soundStyle != SoundStyle.NONE) soundManager.playMove()
    }

    fun setLayoutMode(mode: LayoutMode) {
        _uiState.update { it.copy(layoutMode = mode) }
        viewModelScope.launch { settingsRepository.setLayoutMode(mode.name) }
    }

    fun setGhostPieceEnabled(enabled: Boolean) {
        _uiState.update { it.copy(ghostPieceEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setGhostPieceEnabled(enabled) }
    }

    fun setDifficulty(diff: Difficulty) {
        _uiState.update { it.copy(difficulty = diff) }
        game.setDifficulty(diff)
        viewModelScope.launch { settingsRepository.setDifficulty(diff.name) }
    }

    fun setGameMode(mode: GameMode) {
        _uiState.update { it.copy(gameMode = mode) }
        game.setGameMode(mode)
    }

    fun setDasDelay(ms: Long) { dasDelay = ms.coerceIn(50, 300) }
    fun setArrSpeed(ms: Long) { arrSpeed = ms.coerceIn(0, 100) }

    // ===== Layout Editor =====

    private val layoutRepository = LayoutRepository(getApplication())

    private val _showLayoutEditor = MutableStateFlow(false)
    val showLayoutEditor: StateFlow<Boolean> = _showLayoutEditor.asStateFlow()

    private val _currentLayoutProfile = MutableStateFlow(LayoutPresets.getDefaultLandscape())
    val currentLayoutProfile: StateFlow<LayoutProfile> = _currentLayoutProfile.asStateFlow()

    private val _allLayoutProfiles = MutableStateFlow(LayoutPresets.getAllPresets())
    val allLayoutProfiles: StateFlow<List<LayoutProfile>> = _allLayoutProfiles.asStateFlow()

    private val _snapToGrid = MutableStateFlow(true)
    val snapToGrid: StateFlow<Boolean> = _snapToGrid.asStateFlow()

    init {
        // Load layout profiles
        viewModelScope.launch {
            layoutRepository.getAllProfiles().collect { profiles ->
                _allLayoutProfiles.value = profiles
            }
        }
        viewModelScope.launch {
            layoutRepository.snapToGrid.collect { _snapToGrid.value = it }
        }
        viewModelScope.launch {
            layoutRepository.activeLandscapeProfileId.collect { id ->
                val profile = _allLayoutProfiles.value.find { it.id == id }
                    ?: LayoutPresets.getDefaultLandscape()
                _currentLayoutProfile.value = profile
            }
        }
    }

    fun showLayoutEditor() {
        if (game.state.value.status == GameStatus.PLAYING) {
            game.pauseGame()
            stopGameLoop()
        }
        _showLayoutEditor.value = true
    }

    fun hideLayoutEditor() {
        _showLayoutEditor.value = false
    }

    fun saveLayoutProfile(profile: LayoutProfile) {
        viewModelScope.launch {
            layoutRepository.saveProfile(profile)
            layoutRepository.setActiveLandscapeProfile(profile.id)
        }
    }

    fun selectLayoutProfile(profileId: String) {
        val profile = _allLayoutProfiles.value.find { it.id == profileId }
            ?: return
        _currentLayoutProfile.value = profile
        viewModelScope.launch {
            layoutRepository.setActiveLandscapeProfile(profileId)
        }
    }

    fun setSnapToGrid(enabled: Boolean) {
        _snapToGrid.value = enabled
        viewModelScope.launch { layoutRepository.setSnapToGrid(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        stopAllRepeats()
        animationJob?.cancel()
        soundManager.release()
    }
}

data class UiState(
    val showSettings: Boolean = false,
    val showLayoutEditor: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Float = 0.7f,
    val vibrationStyle: VibrationStyle = VibrationStyle.CLASSIC,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val soundStyle: SoundStyle = SoundStyle.RETRO_BEEP,
    val animationEnabled: Boolean = true,
    val animationStyle: AnimationStyle = AnimationStyle.MODERN,
    val animationDuration: Float = 0.5f,
    val stylePreset: StylePreset = StylePreset.CUSTOM,
    val highScore: Int = 0,
    val playerName: String = "Player",
    val layoutMode: LayoutMode = LayoutMode.CLASSIC,
    val ghostPieceEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val gameMode: GameMode = GameMode.MARATHON
)
