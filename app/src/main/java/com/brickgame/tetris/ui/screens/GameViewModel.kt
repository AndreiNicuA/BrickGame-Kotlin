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
    enum class SettingsPage { MAIN, GENERAL, PROFILE, THEME, THEME_EDITOR, LAYOUT, LAYOUT_EDITOR, GAMEPLAY, EXPERIENCE, CONTROLLER, ABOUT, HOW_TO_PLAY }
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
    private val _pieceMaterial = MutableStateFlow("CLASSIC")
    val pieceMaterial: StateFlow<String> = _pieceMaterial.asStateFlow()
    private val _controllerEnabled = MutableStateFlow(true)
    val controllerEnabled: StateFlow<Boolean> = _controllerEnabled.asStateFlow()
    private val _controllerDeadzone = MutableStateFlow(0.25f)
    val controllerDeadzone: StateFlow<Float> = _controllerDeadzone.asStateFlow()
    // General App Settings
    private val _appThemeMode = MutableStateFlow("auto")
    val appThemeMode: StateFlow<String> = _appThemeMode.asStateFlow()
    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()
    private val _orientationLock = MutableStateFlow("auto")
    val orientationLock: StateFlow<String> = _orientationLock.asStateFlow()
    private val _immersiveMode = MutableStateFlow(false)
    val immersiveMode: StateFlow<Boolean> = _immersiveMode.asStateFlow()
    private val _frameRateTarget = MutableStateFlow(60)
    val frameRateTarget: StateFlow<Int> = _frameRateTarget.asStateFlow()
    private val _batterySaver = MutableStateFlow(false)
    val batterySaver: StateFlow<Boolean> = _batterySaver.asStateFlow()
    private val _highContrast = MutableStateFlow(false)
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()
    private val _uiScale = MutableStateFlow(1.0f)
    val uiScale: StateFlow<Float> = _uiScale.asStateFlow()
    // New features
    private val _levelEventsEnabled = MutableStateFlow(true)
    val levelEventsEnabled: StateFlow<Boolean> = _levelEventsEnabled.asStateFlow()
    private val _buttonStyle = MutableStateFlow("ROUND")
    val buttonStyle: StateFlow<String> = _buttonStyle.asStateFlow()
    private val _controllerLayout = MutableStateFlow("auto")
    val controllerLayout: StateFlow<String> = _controllerLayout.asStateFlow()
    private val _infinityTimer = MutableStateFlow(0)
    val infinityTimer: StateFlow<Int> = _infinityTimer.asStateFlow()
    private val _infinityTimerEnabled = MutableStateFlow(false)
    val infinityTimerEnabled: StateFlow<Boolean> = _infinityTimerEnabled.asStateFlow()
    // Countdown state
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()
    private val _timerExpired = MutableStateFlow(false)
    val timerExpired: StateFlow<Boolean> = _timerExpired.asStateFlow()
    private var countdownJob: Job? = null
    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()
    private val _dataLoaded = MutableStateFlow(false)
    val dataLoaded: StateFlow<Boolean> = _dataLoaded.asStateFlow()
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
        viewModelScope.launch { settingsRepo.pieceMaterial.collect { _pieceMaterial.value = it } }
        viewModelScope.launch { settingsRepo.controllerEnabled.collect { _controllerEnabled.value = it } }
        viewModelScope.launch { settingsRepo.controllerDeadzone.collect { _controllerDeadzone.value = it } }
        // General App Settings
        viewModelScope.launch { settingsRepo.appThemeMode.collect { _appThemeMode.value = it } }
        viewModelScope.launch { settingsRepo.keepScreenOn.collect { _keepScreenOn.value = it } }
        viewModelScope.launch { settingsRepo.orientationLock.collect { _orientationLock.value = it } }
        viewModelScope.launch { settingsRepo.immersiveMode.collect { _immersiveMode.value = it } }
        viewModelScope.launch { settingsRepo.frameRateTarget.collect { _frameRateTarget.value = it } }
        viewModelScope.launch { settingsRepo.batterySaver.collect { _batterySaver.value = it } }
        viewModelScope.launch { settingsRepo.highContrast.collect { _highContrast.value = it } }
        viewModelScope.launch { settingsRepo.uiScale.collect { _uiScale.value = it } }
        // New feature settings
        viewModelScope.launch { settingsRepo.levelEventsEnabled.collect { _levelEventsEnabled.value = it } }
        viewModelScope.launch { settingsRepo.buttonStyle.collect { _buttonStyle.value = it } }
        viewModelScope.launch { settingsRepo.controllerLayout.collect { _controllerLayout.value = it } }
        viewModelScope.launch { settingsRepo.gameMode.collect { name -> _gameMode.value = GameMode.entries.find { it.name == name } ?: GameMode.MARATHON; game.setGameMode(_gameMode.value) } }
        viewModelScope.launch { settingsRepo.infinityTimer.collect { _infinityTimer.value = it } }
        viewModelScope.launch { settingsRepo.infinityTimerEnabled.collect { _infinityTimerEnabled.value = it } }
        viewModelScope.launch { settingsRepo.onboardingComplete.collect { _showOnboarding.value = !it } }
        // Signal that data is loaded after critical settings have their first emission
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsRepo.themeName, settingsRepo.highScore, settingsRepo.portraitLayout
            ) { _, _, _ -> true }.first()
            _dataLoaded.value = true
        }
        // Custom themes
        viewModelScope.launch { customThemeRepo.customThemes.collect { list -> val themes = list.map { it.toGameTheme() }; _customThemes.value = themes; GameThemes.updateCustomThemes(themes) } }
        // Custom layouts
        viewModelScope.launch { customLayoutRepo.customLayouts.collect { _customLayouts.value = it } }
    }

    fun startGame() {
        game.setDifficulty(_difficulty.value); game.setGameMode(_gameMode.value); game.startGame(); handlingLineClear = false
        _timerExpired.value = false
        startGameLoop()
        startCountdownIfNeeded()
    }
    fun pauseGame() { game.pauseGame(); stopGameLoop(); countdownJob?.cancel() }
    fun resumeGame() { game.resumeGame(); startGameLoop(); startCountdownIfNeeded() }
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

    private fun startCountdownIfNeeded() {
        countdownJob?.cancel()
        val mode = _gameMode.value
        val timerEnabled = _infinityTimerEnabled.value
        val timerSecs = _infinityTimer.value
        if (mode != GameMode.INFINITY || !timerEnabled || timerSecs <= 0) {
            _remainingSeconds.value = 0
            return
        }
        // If remaining is 0 (fresh start), initialize from setting
        if (_remainingSeconds.value <= 0) _remainingSeconds.value = timerSecs
        countdownJob = viewModelScope.launch {
            while (isActive && _remainingSeconds.value > 0 && gameState.value.status == GameStatus.PLAYING) {
                delay(1000L)
                _remainingSeconds.value = (_remainingSeconds.value - 1).coerceAtLeast(0)
            }
            if (_remainingSeconds.value <= 0 && gameState.value.status == GameStatus.PLAYING) {
                _timerExpired.value = true
                game.pauseGame()
                stopGameLoop()
            }
        }
    }

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
    fun setGameMode(m: GameMode) { _gameMode.value = m; viewModelScope.launch { settingsRepo.setGameMode(m.name) } }
    fun setLevelEventsEnabled(v: Boolean) { _levelEventsEnabled.value = v; viewModelScope.launch { settingsRepo.setLevelEventsEnabled(v) } }
    fun setButtonStyle(v: String) { _buttonStyle.value = v; viewModelScope.launch { settingsRepo.setButtonStyle(v) } }
    fun setControllerLayout(v: String) { _controllerLayout.value = v; viewModelScope.launch { settingsRepo.setControllerLayout(v) } }
    fun setInfinityTimer(v: Int) { _infinityTimer.value = v; viewModelScope.launch { settingsRepo.setInfinityTimer(v) } }
    fun dismissOnboarding() { _showOnboarding.value = false; viewModelScope.launch { settingsRepo.setOnboardingComplete(true) } }

    fun setInfinityTimerEnabled(v: Boolean) { _infinityTimerEnabled.value = v; viewModelScope.launch { settingsRepo.setInfinityTimerEnabled(v) } }
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
    fun setPieceMaterial(v: String) { _pieceMaterial.value = v; viewModelScope.launch { settingsRepo.setPieceMaterial(v) } }
    fun setControllerEnabled(v: Boolean) { _controllerEnabled.value = v; viewModelScope.launch { settingsRepo.setControllerEnabled(v) } }
    fun setControllerDeadzone(v: Float) { _controllerDeadzone.value = v; viewModelScope.launch { settingsRepo.setControllerDeadzone(v) } }

    // General App Settings setters
    fun setAppThemeMode(v: String) { _appThemeMode.value = v; viewModelScope.launch { settingsRepo.setAppThemeMode(v) } }
    fun setKeepScreenOn(v: Boolean) { _keepScreenOn.value = v; viewModelScope.launch { settingsRepo.setKeepScreenOn(v) } }
    fun setOrientationLock(v: String) { _orientationLock.value = v; viewModelScope.launch { settingsRepo.setOrientationLock(v) } }
    fun setImmersiveMode(v: Boolean) { _immersiveMode.value = v; viewModelScope.launch { settingsRepo.setImmersiveMode(v) } }
    fun setFrameRateTarget(v: Int) { _frameRateTarget.value = v; viewModelScope.launch { settingsRepo.setFrameRateTarget(v) } }
    fun setBatterySaver(v: Boolean) { _batterySaver.value = v; viewModelScope.launch { settingsRepo.setBatterySaver(v) } }
    fun setHighContrast(v: Boolean) { _highContrast.value = v; viewModelScope.launch { settingsRepo.setHighContrast(v) } }
    fun setUiScale(v: Float) { _uiScale.value = v; viewModelScope.launch { settingsRepo.setUiScale(v) } }

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
    fun clearHistory() { viewModelScope.launch { playerRepo.clearHistory() } }
    fun deleteCustomLayout(id: String) {
        viewModelScope.launch { customLayoutRepo.deleteLayout(id) }
        if (_activeCustomLayout.value?.id == id) _activeCustomLayout.value = null
    }

    // ===== Freeform Layout =====
    fun enterFreeformEditMode() { _freeformEditMode.value = true }
    fun exitFreeformEditMode() { _freeformEditMode.value = false }
    fun updateFreeformElement(element: FreeformElement) {
        _playerProfile.value = _playerProfile.value.copy(
            freeformElements = _playerProfile.value.freeformElements + (element.key to element)
        )
        viewModelScope.launch { profileRepo.updateFreeformElement(element) }
    }
    fun addFreeformElement(element: FreeformElement) {
        _playerProfile.value = _playerProfile.value.copy(
            freeformElements = _playerProfile.value.freeformElements + (element.key to element)
        )
        viewModelScope.launch { profileRepo.addFreeformElement(element) }
    }
    fun removeFreeformElement(key: String) {
        _playerProfile.value = _playerProfile.value.copy(
            freeformElements = _playerProfile.value.freeformElements - key
        )
        viewModelScope.launch { profileRepo.removeFreeformElement(key) }
    }
    fun resetFreeformElements() {
        _playerProfile.value = _playerProfile.value.copy(
            freeformElements = PlayerProfile.defaultFreeformElements()
        )
        viewModelScope.launch { profileRepo.resetFreeformElements() }
    }

    // ===== Profile save (syncs both profile + legacy) =====
    fun saveProfile(profile: PlayerProfile) {
        viewModelScope.launch {
            profileRepo.saveProfile(profile)
            profileRepo.syncToLegacy(profile, settingsRepo, playerRepo)
        }
    }

    // ===== 3D Tetris =====
    private val game3D = Tetris3DGame()
    val game3DState: StateFlow<Game3DState> = game3D.state
    private var game3DJob: Job? = null

    fun start3DGame() {
        game3D.start()
        start3DLoop()
        start3DSoundObserver()
    }
    private fun start3DLoop() {
        game3DJob?.cancel()
        game3DJob = viewModelScope.launch {
            while (isActive) {
                val st = game3DState.value.status
                if (st != GameStatus.PLAYING) break
                game3D.tick(16L)
                delay(16L)
            }
        }
    }

    // Reactive sound/vibration observer for 3D game events (line clears, level ups, game over)
    private var prev3DLayers = 0
    private var prev3DLevel = 1
    private var prev3DStatus = GameStatus.MENU
    private var sound3DJob: Job? = null

    private fun start3DSoundObserver() {
        prev3DLayers = 0; prev3DLevel = 1; prev3DStatus = GameStatus.PLAYING
        sound3DJob?.cancel()
        sound3DJob = viewModelScope.launch {
            game3DState.collect { s ->
                // Layer clear
                val clearedNow = s.layers - prev3DLayers
                if (clearedNow > 0 && prev3DLayers >= 0) {
                    soundManager.playClear(); vibrationManager.vibrateClear(clearedNow)
                }
                prev3DLayers = s.layers
                // Level up
                if (s.level > prev3DLevel && prev3DLevel > 0) {
                    soundManager.playLevelUp(); vibrationManager.vibrateLevelUp()
                }
                prev3DLevel = s.level
                // Game over
                if (s.status == GameStatus.GAME_OVER && prev3DStatus == GameStatus.PLAYING) {
                    soundManager.playGameOver(); vibrationManager.vibrateGameOver()
                }
                prev3DStatus = s.status
            }
        }
    }
    fun pause3D() { game3D.pause(); game3DJob?.cancel() }
    fun resume3D() { game3D.resume(); start3DLoop() }

    /** Quit 2D game — save score to history, return to menu */
    fun quitGame() {
        stopGameLoop()
        val s = gameState.value
        if (s.score > 0) {
            viewModelScope.launch {
                if (s.score > _highScore.value) settingsRepo.setHighScore(s.score)
                playerRepo.addScore(playerName.value, s.score, s.level, s.lines)
            }
        }
        game.resetToMenu()
    }

    /** Quit 3D game — save score to history, return to menu */
    fun quit3DGame() {
        game3DJob?.cancel()
        sound3DJob?.cancel()
        val s = game3DState.value
        if (s.score > 0) {
            viewModelScope.launch {
                if (s.score > _highScore.value) settingsRepo.setHighScore(s.score)
                playerRepo.addScore(playerName.value, s.score, s.level, s.layers)
            }
        }
        game3D.resetToMenu()
    }
    fun move3DX(dx: Int) { if (game3D.moveX(dx)) { soundManager.playMove(); vibrationManager.vibrateMove() } }
    fun move3DZ(dz: Int) { if (game3D.moveZ(dz)) { soundManager.playMove(); vibrationManager.vibrateMove() } }
    fun rotate3DXZ() { if (game3D.rotateXZ()) { soundManager.playRotate(); vibrationManager.vibrateRotate() } }
    fun rotate3DXY() { if (game3D.rotateXY()) { soundManager.playRotate(); vibrationManager.vibrateRotate() } }
    fun hardDrop3D() { val d = game3D.hardDrop(); if (d > 0) { soundManager.playDrop(); vibrationManager.vibrateDrop() } }
    fun hold3D() { if (game3D.hold()) { soundManager.playRotate(); vibrationManager.vibrateMove() } }
    fun softDrop3D() { game3D.softDrop() }
    fun toggle3DGravity() { game3D.toggleGravity() }

    override fun onCleared() { super.onCleared(); stopGameLoop(); game3DJob?.cancel(); sound3DJob?.cancel(); soundManager.release() }
}
