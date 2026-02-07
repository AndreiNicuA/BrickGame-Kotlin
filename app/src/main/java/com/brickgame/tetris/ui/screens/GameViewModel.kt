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

    private val settingsRepo = SettingsRepository(application)
    private val playerRepo = PlayerRepository(application)
    val soundManager = SoundManager(application)
    val vibrationManager = VibrationManager(application)

    private val game = TetrisGame()
    val gameState: StateFlow<GameState> = game.state

    // UI
    data class UiState(
        val showSettings: Boolean = false,
        val settingsPage: SettingsPage = SettingsPage.MAIN
    )
    enum class SettingsPage { MAIN, PROFILE, THEME, LAYOUT, GAMEPLAY, EXPERIENCE, ABOUT }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Theme
    private val _currentTheme = MutableStateFlow(GameThemes.ClassicGreen)
    val currentTheme: StateFlow<GameTheme> = _currentTheme.asStateFlow()

    // Layout — persisted
    private val _portraitLayout = MutableStateFlow(LayoutPreset.PORTRAIT_CLASSIC)
    val portraitLayout: StateFlow<LayoutPreset> = _portraitLayout.asStateFlow()
    private val _landscapeLayout = MutableStateFlow(LayoutPreset.LANDSCAPE_DEFAULT)
    val landscapeLayout: StateFlow<LayoutPreset> = _landscapeLayout.asStateFlow()
    private val _dpadStyle = MutableStateFlow(DPadStyle.STANDARD)
    val dpadStyle: StateFlow<DPadStyle> = _dpadStyle.asStateFlow()

    // Gameplay
    private val _ghostPieceEnabled = MutableStateFlow(true)
    val ghostPieceEnabled: StateFlow<Boolean> = _ghostPieceEnabled.asStateFlow()
    private val _difficulty = MutableStateFlow(Difficulty.NORMAL)
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()
    private val _gameMode = MutableStateFlow(GameMode.MARATHON)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    // Experience
    private val _animationStyle = MutableStateFlow(AnimationStyle.MODERN)
    val animationStyle: StateFlow<AnimationStyle> = _animationStyle.asStateFlow()
    private val _animationDuration = MutableStateFlow(0.5f)
    val animationDuration: StateFlow<Float> = _animationDuration.asStateFlow()
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // Player
    val playerName = playerRepo.playerName.stateIn(viewModelScope, SharingStarted.Eagerly, "Player")
    val scoreHistory = playerRepo.scoreHistory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // Game loop jobs
    private var gameLoopJob: Job? = null
    private var lockDelayJob: Job? = null
    private var lineClearJob: Job? = null
    private var dasJob: Job? = null

    // FIX: track if we already started a line clear animation to prevent re-triggering
    private var lineClearInProgress = false

    private var dasDelayMs = 170L
    private var arrSpeedMs = 50L

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch { settingsRepo.themeName.collect { _currentTheme.value = GameThemes.getThemeByName(it) } }
        viewModelScope.launch { settingsRepo.ghostPieceEnabled.collect { _ghostPieceEnabled.value = it } }
        viewModelScope.launch {
            settingsRepo.difficulty.collect {
                val d = Difficulty.entries.find { e -> e.name == it } ?: Difficulty.NORMAL
                _difficulty.value = d; game.setDifficulty(d)
            }
        }
        viewModelScope.launch { settingsRepo.highScore.collect { _highScore.value = it } }
        viewModelScope.launch { settingsRepo.soundEnabled.collect { _soundEnabled.value = it; soundManager.setEnabled(it) } }
        viewModelScope.launch { settingsRepo.soundVolume.collect { soundManager.setVolume(it) } }
        viewModelScope.launch { settingsRepo.soundStyle.collect { soundManager.setSoundStyle(SoundStyle.entries.find { e -> e.name == it } ?: SoundStyle.RETRO_BEEP) } }
        viewModelScope.launch { settingsRepo.vibrationEnabled.collect { _vibrationEnabled.value = it; vibrationManager.setEnabled(it) } }
        viewModelScope.launch { settingsRepo.vibrationIntensity.collect { vibrationManager.setIntensity(it) } }
        viewModelScope.launch { settingsRepo.vibrationStyle.collect { vibrationManager.setVibrationStyle(VibrationStyle.entries.find { e -> e.name == it } ?: VibrationStyle.CLASSIC) } }
        viewModelScope.launch { settingsRepo.animationStyle.collect { _animationStyle.value = AnimationStyle.entries.find { e -> e.name == it } ?: AnimationStyle.MODERN } }
        viewModelScope.launch { settingsRepo.animationDuration.collect { _animationDuration.value = it } }
        // Layout persistence
        viewModelScope.launch { settingsRepo.portraitLayout.collect { name -> _portraitLayout.value = LayoutPreset.entries.find { it.name == name } ?: LayoutPreset.PORTRAIT_CLASSIC } }
        viewModelScope.launch { settingsRepo.landscapeLayout.collect { name -> _landscapeLayout.value = LayoutPreset.entries.find { it.name == name } ?: LayoutPreset.LANDSCAPE_DEFAULT } }
        viewModelScope.launch { settingsRepo.dpadStyle.collect { name -> _dpadStyle.value = DPadStyle.entries.find { it.name == name } ?: DPadStyle.STANDARD } }
    }

    // ===== Game Actions =====

    fun startGame() {
        game.setDifficulty(_difficulty.value)
        game.setGameMode(_gameMode.value)
        game.startGame()
        lineClearInProgress = false
        startGameLoop()
    }

    fun pauseGame() { game.pauseGame(); stopGameLoop() }
    fun resumeGame() { game.resumeGame(); startGameLoop() }
    fun togglePause() {
        if (gameState.value.status == GameStatus.PLAYING) pauseGame()
        else if (gameState.value.status == GameStatus.PAUSED) resumeGame()
    }

    fun moveLeft() { if (game.moveLeft()) { soundManager.playMove(); vibrationManager.vibrateMove() } }
    fun moveRight() { if (game.moveRight()) { soundManager.playMove(); vibrationManager.vibrateMove() } }
    fun softDrop() { game.moveDown() }
    fun hardDrop() { val d = game.hardDrop(); if (d > 0) { soundManager.playDrop(); vibrationManager.vibrateDrop() } }
    fun rotate() { if (game.rotate()) { soundManager.playRotate(); vibrationManager.vibrateRotate() } }
    fun rotateCounterClockwise() { if (game.rotateCounterClockwise()) { soundManager.playRotate(); vibrationManager.vibrateRotate() } }
    fun holdPiece() { game.holdCurrentPiece() }

    // DAS
    fun startLeftDAS() { stopDAS(); moveLeft(); dasJob = viewModelScope.launch { delay(dasDelayMs); while (isActive) { moveLeft(); delay(arrSpeedMs) } } }
    fun startRightDAS() { stopDAS(); moveRight(); dasJob = viewModelScope.launch { delay(dasDelayMs); while (isActive) { moveRight(); delay(arrSpeedMs) } } }
    fun startDownDAS() { stopDAS(); softDrop(); dasJob = viewModelScope.launch { delay(dasDelayMs); while (isActive) { softDrop(); delay(arrSpeedMs) } } }
    fun stopDAS() { dasJob?.cancel(); dasJob = null }

    // ===== FIXED Game Loop =====

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        lockDelayJob?.cancel()

        gameLoopJob = viewModelScope.launch {
            while (isActive && gameState.value.status == GameStatus.PLAYING) {
                if (game.isGameActive()) {
                    val prevLevel = gameState.value.level
                    game.moveDown()

                    // FIX: only trigger line clear animation ONCE per clear event
                    if (game.isPendingLineClear() && !lineClearInProgress) {
                        lineClearInProgress = true
                        val linesCount = gameState.value.linesCleared
                        if (linesCount > 0) {
                            soundManager.playClear()
                            vibrationManager.vibrateClear(linesCount)
                        }

                        lineClearJob?.cancel()
                        lineClearJob = launch {
                            delay((_animationDuration.value * 500).toLong().coerceAtLeast(150))
                            game.completePendingLineClear()
                            lineClearInProgress = false

                            if (gameState.value.level > prevLevel) {
                                soundManager.playLevelUp()
                                vibrationManager.vibrateLevelUp()
                            }
                            if (gameState.value.status == GameStatus.GAME_OVER) onGameOver()
                        }
                    }
                }
                delay(game.getDropSpeed())
            }
            if (gameState.value.status == GameStatus.GAME_OVER) onGameOver()
        }

        lockDelayJob = viewModelScope.launch {
            while (isActive && gameState.value.status == GameStatus.PLAYING) {
                if (game.checkLockDelay()) {
                    // After lock, check if line clear happened
                    if (game.isPendingLineClear() && !lineClearInProgress) {
                        lineClearInProgress = true
                        val linesCount = gameState.value.linesCleared
                        if (linesCount > 0) {
                            soundManager.playClear()
                            vibrationManager.vibrateClear(linesCount)
                        }
                        lineClearJob?.cancel()
                        lineClearJob = launch {
                            delay((_animationDuration.value * 500).toLong().coerceAtLeast(150))
                            game.completePendingLineClear()
                            lineClearInProgress = false
                            if (gameState.value.status == GameStatus.GAME_OVER) onGameOver()
                        }
                    } else if (gameState.value.status == GameStatus.GAME_OVER) {
                        onGameOver()
                    }
                }
                delay(16L)
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel(); lockDelayJob?.cancel(); lineClearJob?.cancel()
        lineClearInProgress = false
    }

    private fun onGameOver() {
        stopGameLoop()
        soundManager.playGameOver(); vibrationManager.vibrateGameOver()
        val state = gameState.value
        viewModelScope.launch {
            if (state.score > _highScore.value) settingsRepo.setHighScore(state.score)
            playerRepo.addScore(playerName.value, state.score, state.level, state.lines)
        }
    }

    // ===== Settings Navigation =====
    fun openSettings() { if (gameState.value.status == GameStatus.PLAYING) pauseGame(); _uiState.update { it.copy(showSettings = true, settingsPage = SettingsPage.MAIN) } }
    fun closeSettings() { _uiState.update { it.copy(showSettings = false) } }
    fun navigateSettings(page: SettingsPage) { _uiState.update { it.copy(settingsPage = page) } }

    // ===== Settings Updates (all persisted) =====
    fun setTheme(t: GameTheme) { _currentTheme.value = t; viewModelScope.launch { settingsRepo.setThemeName(t.name) } }
    fun setGhostPieceEnabled(v: Boolean) { _ghostPieceEnabled.value = v; viewModelScope.launch { settingsRepo.setGhostPieceEnabled(v) } }
    fun setDifficulty(d: Difficulty) { _difficulty.value = d; game.setDifficulty(d); viewModelScope.launch { settingsRepo.setDifficulty(d.name) } }
    fun setGameMode(m: GameMode) { _gameMode.value = m }
    fun setAnimationStyle(s: AnimationStyle) { _animationStyle.value = s; viewModelScope.launch { settingsRepo.setAnimationStyle(s.name) } }
    fun setAnimationDuration(d: Float) { _animationDuration.value = d; viewModelScope.launch { settingsRepo.setAnimationDuration(d) } }
    fun setSoundEnabled(v: Boolean) { _soundEnabled.value = v; soundManager.setEnabled(v); viewModelScope.launch { settingsRepo.setSoundEnabled(v) } }
    fun setVibrationEnabled(v: Boolean) { _vibrationEnabled.value = v; vibrationManager.setEnabled(v); viewModelScope.launch { settingsRepo.setVibrationEnabled(v) } }
    fun setPlayerName(n: String) { viewModelScope.launch { playerRepo.setPlayerName(n) } }
    fun toggleSound() { setSoundEnabled(!_soundEnabled.value) }

    // FIX: Layout settings now persist to DataStore
    fun setPortraitLayout(p: LayoutPreset) { _portraitLayout.value = p; viewModelScope.launch { settingsRepo.setPortraitLayout(p.name) } }
    fun setLandscapeLayout(p: LayoutPreset) { _landscapeLayout.value = p; viewModelScope.launch { settingsRepo.setLandscapeLayout(p.name) } }
    fun setDPadStyle(s: DPadStyle) { _dpadStyle.value = s; viewModelScope.launch { settingsRepo.setDpadStyle(s.name) } }

    override fun onCleared() { super.onCleared(); stopGameLoop(); soundManager.release() }
}
