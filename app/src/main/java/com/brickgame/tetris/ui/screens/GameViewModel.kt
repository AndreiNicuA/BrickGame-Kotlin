package com.brickgame.tetris.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brickgame.tetris.audio.SoundManager
import com.brickgame.tetris.audio.VibrationManager
import com.brickgame.tetris.data.*
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.layout.DPadStyle
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.styles.SoundStyle
import com.brickgame.tetris.ui.styles.VibrationStyle
import com.brickgame.tetris.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val playerRepo = PlayerRepository(application)
    private val customThemeRepo = CustomThemeRepository(application)
    private val customLayoutRepo = CustomLayoutRepository(application)
    private val profileRepo = PlayerProfileRepository(application)
    val soundManager = SoundManager(application)
    val vibrationManager = VibrationManager(application)

    private val game = TetrisGame()
    val gameState: StateFlow<GameState> = game.state

    data class UiState(val showSettings: Boolean = false, val settingsPage: SettingsPage = SettingsPage.MAIN)
    enum class SettingsPage { MAIN, PROFILE, THEME, THEME_EDITOR, LAYOUT, LAYOUT_EDITOR, GAMEPLAY, EXPERIENCE, ABOUT }
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentTheme = MutableStateFlow(GameThemes.ClassicGreen)
    val currentTheme: StateFlow<GameTheme> = _currentTheme.asStateFlow()
    private val _portraitLayout = MutableStateFlow(LayoutPreset.PORTRAIT_CLASSIC)
    val portraitLayout: StateFlow<LayoutPreset> = _portraitLayout.asStateFlow()
    private val _landscapeLayout = MutableStateFlow(LayoutPreset.LANDSCAPE_DEFAULT)
    val landscapeLayout: StateFlow<LayoutPreset> = _landscapeLayout.asStateFlow()
    private val _dpadStyle = MutableStateFlow(DPadStyle.STANDARD)
    val dpadStyle: StateFlow<DPadStyle> = _dpadStyle.asStateFlow()
    private val _ghostPieceEnabled = MutableStateFlow(true)
    val ghostPieceEnabled: StateFlow<Boolean> = _ghostPieceEnabled.asStateFlow()
    private val _difficulty = MutableStateFlow(Difficulty.NORMAL)
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()
    private val _gameMode = MutableStateFlow(GameMode.MARATHON)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()
    private val _animationStyle = MutableStateFlow(AnimationStyle.MODERN)
    val animationStyle: StateFlow<AnimationStyle> = _animationStyle.asStateFlow()
    private val _animationDuration = MutableStateFlow(0.5f)
    val animationDuration: StateFlow<Float> = _animationDuration.asStateFlow()
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    private val _multiColorEnabled = MutableStateFlow(false)
    val multiColorEnabled: StateFlow<Boolean> = _multiColorEnabled.asStateFlow()
    val playerName = playerRepo.playerName.stateIn(viewModelScope, SharingStarted.Eagerly, "Player")
    val scoreHistory = playerRepo.scoreHistory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // Custom themes & layouts
    private val _customThemes = MutableStateFlow<List<GameTheme>>(emptyList())
    val customThemes: StateFlow<List<GameTheme>> = _customThemes.asStateFlow()
    private val _editingTheme = MutableStateFlow<GameTheme?>(null)
    val editingTheme: StateFlow<GameTheme?> = _editingTheme.asStateFlow()

    private val _customLayouts = MutableStateFlow<List<CustomLayoutData>>(emptyList())
    val customLayouts: StateFlow<List<CustomLayoutData>> = _customLayouts.asStateFlow()
    private val _editingLayout = MutableStateFlow<CustomLayoutData?>(null)
    val editingLayout: StateFlow<CustomLayoutData?> = _editingLayout.asStateFlow()
    private val _activeCustomLayout = MutableStateFlow<CustomLayoutData?>(null)
    val activeCustomLayout: StateFlow<CustomLayoutData?> = _activeCustomLayout.asStateFlow()

    // Player profile & freeform layout
    private val _playerProfile = MutableStateFlow(PlayerProfile())
    val playerProfile: StateFlow<PlayerProfile> = _playerProfile.asStateFlow()
    private val _freeformEditMode = MutableStateFlow(false)
    val freeformEditMode: StateFlow<Boolean> = _freeformEditMode.asStateFlow()

    private var gravityJob: Job? = null
    private var lockDelayJob: Job? = null
    private var dasJob: Job? = null
    @Volatile private var handlingLineClear = false

    init { loadSettings(); loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            // Migrate legacy settings into profile on first run
            profileRepo.migrateIfNeeded(settingsRepo, playerRepo)
            // Observe profile changes
            profileRepo.profile.collect { _playerProfile.value = it }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch { settingsRepo.themeName.collect { _currentTheme.value = GameThemes.getThemeByName(it) } }
        viewModelScope.launch { settingsRepo.ghostPieceEnabled.collect { _ghostPieceEnabled.value = it } }
        viewModelScope.launch { settingsRepo.difficulty.collect { val d = Difficulty.entries.find { e -> e.name == it } ?: Difficulty.NORMAL; _difficulty.value = d; game.setDifficulty(d) } }
        viewModelScope.launch { settingsRepo.highScore.collect { _highScore.value = it } }
        viewModelScope.launch { settingsRepo.soundEnabled.collect { _soundEnabled.value = it; soundManager.setEnabled(it) } }
        viewModelScope.launch { settingsRepo.soundVolume.collect { soundManager.setVolume(it) } }
        viewModelScope.launch { settingsRepo.soundStyle.collect { soundManager.setSoundStyle(SoundStyle.entries.find { e -> e.name == it } ?: SoundStyle.RETRO_BEEP) } }
        viewModelScope.launch { settingsRepo.vibrationEnabled.collect { _vibrationEnabled.value = it; vibrationManager.setEnabled(it) } }
        viewModelScope.launch { settingsRepo.vibrationIntensity.collect { vibrationManager.setIntensity(it) } }
        viewModelScope.launch { settingsRepo.vibrationStyle.collect { vibrationManager.setVibrationStyle(VibrationStyle.entries.find { e -> e.name == it } ?: VibrationStyle.CLASSIC) } }
        viewModelScope.launch { settingsRepo.animationStyle.collect { _animationStyle.value = AnimationStyle.entries.find { e -> e.name == it } ?: AnimationStyle.MODERN } }
        viewModelScope.launch { settingsRepo.animationDuration.collect { _animationDuration.value = it } }
        viewModelScope.launch { settingsRepo.portraitLayout.collect { name -> _portraitLayout.value = LayoutPreset.entries.find { it.name == name } ?: LayoutPreset.PORTRAIT_CLASSIC } }
        viewModelScope.launch { settingsRepo.landscapeLayout.collect { name -> _landscapeLayout.value = LayoutPreset.entries.find { it.name == name } ?: LayoutPreset.LANDSCAPE_DEFAULT } }
        viewModelScope.launch { settingsRepo.dpadStyle.collect { name -> _dpadStyle.value = DPadStyle.entries.find { it.name == name } ?: DPadStyle.STANDARD } }
        viewModelScope.launch { settingsRepo.multiColorEnabled.collect { _multiColorEnabled.value = it } }
        // Custom themes
        viewModelScope.launch { customThemeRepo.customThemes.collect { list -> val themes = list.map { it.toGameTheme() }; _customThemes.value = themes; GameThemes.updateCustomThemes(themes) } }
        // Custom layouts
        viewModelScope.launch { customLayoutRepo.customLayouts.collect { _customLayouts.value = it } }
    }

    fun startGame() { game.setDifficulty(_difficulty.value); game.setGameMode(_gameMode.value); game.startGame(); handlingLineClear = false; startGameLoop() }
    fun pauseGame() { game.pauseGame(); stopGameLoop() }
    fun resumeGame() { game.resumeGame(); startGameLoop() }
    fun togglePause() { if (gameState.value.status == GameStatus.PLAYING) pauseGame() else if (gameState.value.status == GameStatus.PAUSED) resumeGame() }

    fun moveLeft() { if (game.moveLeft()) { soundManager.playMove(); vibrationManager.vibrateMove() } }
    fun moveRight() { if (game.moveRight()) { soundManager.playMove(); vibrationManager.vibrateMove() } }
    fun softDrop() { game.moveDown() }
    fun hardDrop() { val d = game.hardDrop(); if (d > 0) { soundManager.playDrop(); vibrationManager.vibrateDrop() } }
    fun rotate() { if (game.rotate()) { soundManager.playRotate(); vibrationManager.vibrateRotate() } }
    fun rotateCounterClockwise() { if (game.rotateCounterClockwise()) { soundManager.playRotate(); vibrationManager.vibrateRotate() } }
    fun holdPiece() { game.holdCurrentPiece() }

    fun startLeftDAS() { stopDAS(); moveLeft(); dasJob = viewModelScope.launch { delay(170); while (isActive) { moveLeft(); delay(50) } } }
    fun startRightDAS() { stopDAS(); moveRight(); dasJob = viewModelScope.launch { delay(170); while (isActive) { moveRight(); delay(50) } } }
    fun startDownDAS() { stopDAS(); softDrop(); dasJob = viewModelScope.launch { delay(60); while (isActive) { softDrop(); delay(30) } } }
    fun stopDAS() { dasJob?.cancel(); dasJob = null }

    private fun startGameLoop() {
        gravityJob?.cancel(); lockDelayJob?.cancel(); handlingLineClear = false
        gravityJob = viewModelScope.launch {
            while (isActive && gameState.value.status == GameStatus.PLAYING) {
                if (game.isGameActive()) game.moveDown()
                delay(game.getDropSpeed())
            }
        }
        lockDelayJob = viewModelScope.launch {
            while (isActive && gameState.value.status == GameStatus.PLAYING) {
                if (game.isPendingLineClear() && !handlingLineClear) {
                    handlingLineClear = true
                    val prevLevel = gameState.value.level; val lc = gameState.value.linesCleared
                    if (lc > 0) { soundManager.playClear(); vibrationManager.vibrateClear(lc) }
                    delay((_animationDuration.value * 500).toLong().coerceAtLeast(200))
                    game.completePendingLineClear(); handlingLineClear = false
                    if (gameState.value.level > prevLevel) { soundManager.playLevelUp(); vibrationManager.vibrateLevelUp() }
                    if (gameState.value.status == GameStatus.GAME_OVER) { onGameOver(); break }
                    continue
                }
                if (game.checkLockDelay()) {
                    if (gameState.value.status == GameStatus.GAME_OVER) { onGameOver(); break }
                    continue
                }
                delay(16L)
            }
            if (gameState.value.status == GameStatus.GAME_OVER) onGameOver()
        }
    }

    private fun stopGameLoop() { gravityJob?.cancel(); lockDelayJob?.cancel(); handlingLineClear = false }

    private fun onGameOver() {
        stopGameLoop(); soundManager.playGameOver(); vibrationManager.vibrateGameOver()
        val s = gameState.value
        viewModelScope.launch { if (s.score > _highScore.value) settingsRepo.setHighScore(s.score); playerRepo.addScore(playerName.value, s.score, s.level, s.lines) }
    }

    // ===== Settings navigation =====
    fun openSettings() { if (gameState.value.status == GameStatus.PLAYING) pauseGame(); _uiState.update { it.copy(showSettings = true, settingsPage = SettingsPage.MAIN) } }
    fun closeSettings() { _uiState.update { it.copy(showSettings = false) } }
    fun navigateSettings(p: SettingsPage) { _uiState.update { it.copy(settingsPage = p) } }

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
    fun setPortraitLayout(p: LayoutPreset) { _portraitLayout.value = p; _activeCustomLayout.value = null; viewModelScope.launch { settingsRepo.setPortraitLayout(p.name) } }
    fun setLandscapeLayout(p: LayoutPreset) { _landscapeLayout.value = p; viewModelScope.launch { settingsRepo.setLandscapeLayout(p.name) } }
    fun setDPadStyle(s: DPadStyle) { _dpadStyle.value = s; viewModelScope.launch { settingsRepo.setDpadStyle(s.name) } }
    fun setMultiColorEnabled(v: Boolean) { _multiColorEnabled.value = v; viewModelScope.launch { settingsRepo.setMultiColorEnabled(v) } }

    // ===== Custom Theme =====
    fun startNewTheme() {
        val base = _currentTheme.value
        _editingTheme.value = base.copy(id = "custom_${System.currentTimeMillis()}", name = "My Theme", isBuiltIn = false)
        navigateSettings(SettingsPage.THEME_EDITOR)
    }
    fun editTheme(theme: GameTheme) { _editingTheme.value = theme; navigateSettings(SettingsPage.THEME_EDITOR) }
    fun updateEditingTheme(theme: GameTheme) { _editingTheme.value = theme }
    fun saveEditingTheme() {
        val t = _editingTheme.value ?: return
        viewModelScope.launch { customThemeRepo.saveTheme(t.toCustomData()) }
        setTheme(t)
        navigateSettings(SettingsPage.THEME)
    }
    fun deleteCustomTheme(id: String) {
        viewModelScope.launch { customThemeRepo.deleteTheme(id) }
        if (_currentTheme.value.id == id) setTheme(GameThemes.ClassicGreen)
    }

    // ===== Custom Layout =====
    fun startNewLayout() {
        _editingLayout.value = CustomLayoutData(id = "layout_${System.currentTimeMillis()}", name = "My Layout")
        navigateSettings(SettingsPage.LAYOUT_EDITOR)
    }
    fun editLayout(layout: CustomLayoutData) { _editingLayout.value = layout; navigateSettings(SettingsPage.LAYOUT_EDITOR) }
    fun updateEditingLayout(layout: CustomLayoutData) { _editingLayout.value = layout }
    fun saveEditingLayout() {
        val l = _editingLayout.value ?: return
        viewModelScope.launch { customLayoutRepo.saveLayout(l) }
        _activeCustomLayout.value = l
        navigateSettings(SettingsPage.LAYOUT)
    }
    fun selectCustomLayout(layout: CustomLayoutData) { _activeCustomLayout.value = layout }
    fun clearCustomLayout() { _activeCustomLayout.value = null }
    fun deleteCustomLayout(id: String) {
        viewModelScope.launch { customLayoutRepo.deleteLayout(id) }
        if (_activeCustomLayout.value?.id == id) _activeCustomLayout.value = null
    }

    // ===== Freeform Layout =====
    fun enterFreeformEditMode() { _freeformEditMode.value = true }
    fun exitFreeformEditMode() { _freeformEditMode.value = false }
    fun updateFreeformControlPosition(key: String, pos: FreeformPosition) {
        // Immediate visual update
        _playerProfile.value = _playerProfile.value.copy(
            freeformPositions = _playerProfile.value.freeformPositions + (key to pos)
        )
        // Persist
        viewModelScope.launch { profileRepo.updateFreeformPosition(key, pos) }
    }
    fun updateFreeformInfoPosition(key: String, pos: FreeformPosition) {
        _playerProfile.value = _playerProfile.value.copy(
            freeformInfoPositions = _playerProfile.value.freeformInfoPositions + (key to pos)
        )
        viewModelScope.launch { profileRepo.updateFreeformInfoPosition(key, pos) }
    }
    fun resetFreeformPositions() {
        _playerProfile.value = _playerProfile.value.copy(
            freeformPositions = PlayerProfile.defaultFreeformPositions(),
            freeformInfoPositions = PlayerProfile.defaultFreeformInfoPositions()
        )
        viewModelScope.launch { profileRepo.resetFreeformPositions() }
    }

    // ===== Profile save (syncs both profile + legacy) =====
    fun saveProfile(profile: PlayerProfile) {
        viewModelScope.launch {
            profileRepo.saveProfile(profile)
            profileRepo.syncToLegacy(profile, settingsRepo, playerRepo)
        }
    }

    override fun onCleared() { super.onCleared(); stopGameLoop(); soundManager.release() }
}
