package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.CustomLayoutData
import com.brickgame.tetris.data.ScoreEntry
import com.brickgame.tetris.game.Difficulty
import com.brickgame.tetris.game.GameMode
import com.brickgame.tetris.ui.layout.DPadStyle
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes
import com.brickgame.tetris.ui.theme.LocalIsDarkMode

// Adaptive colors — respond to light/dark mode via LocalIsDarkMode
@Composable private fun bg() = if (LocalIsDarkMode.current) Color(0xFF0D0D0D) else Color(0xFFF5F5F5)
@Composable private fun card() = if (LocalIsDarkMode.current) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)
@Composable private fun acc() = if (LocalIsDarkMode.current) Color(0xFFF4D03F) else Color(0xFFB8860B)
@Composable private fun tx() = if (LocalIsDarkMode.current) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
@Composable private fun dim() = if (LocalIsDarkMode.current) Color(0xFF888888) else Color(0xFF666666)
@Composable private fun selBg() = if (LocalIsDarkMode.current) acc().copy(0.15f) else acc().copy(0.12f)
@Composable private fun cardBorder() = if (LocalIsDarkMode.current) Color.Transparent else Color(0xFFE0E0E0)

@Composable
fun SettingsScreen(
    page: GameViewModel.SettingsPage, currentTheme: GameTheme,
    portraitLayout: LayoutPreset, landscapeLayout: LayoutPreset, dpadStyle: DPadStyle,
    difficulty: Difficulty, gameMode: GameMode, ghostEnabled: Boolean,
    animationStyle: AnimationStyle, animationDuration: Float,
    soundEnabled: Boolean, vibrationEnabled: Boolean, multiColorEnabled: Boolean,
    pieceMaterial: String = "CLASSIC",
    controllerEnabled: Boolean = true, controllerDeadzone: Float = 0.25f,
    appThemeMode: String = "auto", keepScreenOn: Boolean = true,
    orientationLock: String = "auto", immersiveMode: Boolean = false,
    frameRateTarget: Int = 60, batterySaver: Boolean = false,
    highContrast: Boolean = false, uiScale: Float = 1.0f,
    playerName: String, highScore: Int, scoreHistory: List<ScoreEntry>,
    customThemes: List<GameTheme>, editingTheme: GameTheme?,
    customLayouts: List<CustomLayoutData>, editingLayout: CustomLayoutData?,
    activeCustomLayout: CustomLayoutData?,
    onNavigate: (GameViewModel.SettingsPage) -> Unit, onBack: () -> Unit,
    onSetTheme: (GameTheme) -> Unit, onSetPortraitLayout: (LayoutPreset) -> Unit,
    onSetLandscapeLayout: (LayoutPreset) -> Unit, onSetDPadStyle: (DPadStyle) -> Unit,
    onSetDifficulty: (Difficulty) -> Unit, onSetGameMode: (GameMode) -> Unit,
    onSetGhostEnabled: (Boolean) -> Unit, onSetAnimationStyle: (AnimationStyle) -> Unit,
    onSetAnimationDuration: (Float) -> Unit, onSetSoundEnabled: (Boolean) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit, onSetPlayerName: (String) -> Unit,
    onSetMultiColorEnabled: (Boolean) -> Unit,
    onSetPieceMaterial: (String) -> Unit = {},
    onSetControllerEnabled: (Boolean) -> Unit = {},
    onSetControllerDeadzone: (Float) -> Unit = {},
    onSetAppThemeMode: (String) -> Unit = {},
    onSetKeepScreenOn: (Boolean) -> Unit = {},
    onSetOrientationLock: (String) -> Unit = {},
    onSetImmersiveMode: (Boolean) -> Unit = {},
    onSetFrameRateTarget: (Int) -> Unit = {},
    onSetBatterySaver: (Boolean) -> Unit = {},
    onSetHighContrast: (Boolean) -> Unit = {},
    onSetUiScale: (Float) -> Unit = {},
    onNewTheme: () -> Unit, onEditTheme: (GameTheme) -> Unit,
    onUpdateEditingTheme: (GameTheme) -> Unit, onSaveTheme: () -> Unit, onDeleteTheme: (String) -> Unit,
    onNewLayout: () -> Unit, onEditLayout: (CustomLayoutData) -> Unit,
    onUpdateEditingLayout: (CustomLayoutData) -> Unit, onSaveLayout: () -> Unit,
    onSelectCustomLayout: (CustomLayoutData) -> Unit, onClearCustomLayout: () -> Unit,
    onDeleteLayout: (String) -> Unit,
    onEditFreeform: () -> Unit = {},
    on3DMode: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    // New features
    levelEventsEnabled: Boolean = true,
    onSetLevelEventsEnabled: (Boolean) -> Unit = {},
    buttonStyle: String = "ROUND",
    onSetButtonStyle: (String) -> Unit = {},
    controllerLayoutMode: String = "auto",
    onSetControllerLayout: (String) -> Unit = {},
    infinityTimer: Int = 0,
    onSetInfinityTimer: (Int) -> Unit = {},
    infinityTimerEnabled: Boolean = false,
    onSetInfinityTimerEnabled: (Boolean) -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(bg()).systemBarsPadding()) {
        when (page) {
            GameViewModel.SettingsPage.MAIN -> MainPage(onNavigate, onBack, on3DMode)
            GameViewModel.SettingsPage.GENERAL -> GeneralPage(appThemeMode, keepScreenOn, orientationLock, immersiveMode, frameRateTarget, batterySaver, onSetAppThemeMode, onSetKeepScreenOn, onSetOrientationLock, onSetImmersiveMode, onSetFrameRateTarget, onSetBatterySaver) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.PROFILE -> ProfilePage(playerName, highScore, scoreHistory, onSetPlayerName, onClearHistory) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.THEME -> ThemePage(currentTheme, customThemes, multiColorEnabled, pieceMaterial, onSetTheme, onSetMultiColorEnabled, onSetPieceMaterial, onNewTheme, onEditTheme, onDeleteTheme) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.THEME_EDITOR -> if (editingTheme != null) ThemeEditorScreen(editingTheme, onUpdateEditingTheme, onSaveTheme) { onNavigate(GameViewModel.SettingsPage.THEME) }
            GameViewModel.SettingsPage.LAYOUT -> LayoutPage(portraitLayout, landscapeLayout, dpadStyle, buttonStyle, customLayouts, activeCustomLayout, onSetPortraitLayout, onSetLandscapeLayout, onSetDPadStyle, onSetButtonStyle, onNewLayout, onEditLayout, onSelectCustomLayout, onClearCustomLayout, onDeleteLayout, onEditFreeform = { onEditFreeform() }) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.LAYOUT_EDITOR -> if (editingLayout != null) LayoutEditorScreen(editingLayout, currentTheme, portraitLayout, dpadStyle, onUpdateEditingLayout, onSaveLayout) { onNavigate(GameViewModel.SettingsPage.LAYOUT) }
            GameViewModel.SettingsPage.GAMEPLAY -> GameplayPage(difficulty, gameMode, ghostEnabled, levelEventsEnabled, infinityTimer, infinityTimerEnabled, onSetDifficulty, onSetGameMode, onSetGhostEnabled, onSetLevelEventsEnabled, onSetInfinityTimer, onSetInfinityTimerEnabled) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.EXPERIENCE -> ExperiencePage(animationStyle, animationDuration, soundEnabled, vibrationEnabled, onSetAnimationStyle, onSetAnimationDuration, onSetSoundEnabled, onSetVibrationEnabled) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.CONTROLLER -> ControllerPage(controllerEnabled, controllerDeadzone, controllerLayoutMode, onSetControllerEnabled, onSetControllerDeadzone, onSetControllerLayout) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.ABOUT -> AboutPage { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.HOW_TO_PLAY -> HowToPlayPage { onNavigate(GameViewModel.SettingsPage.MAIN) }
        }
    }
}

// ===== MAIN =====
@Composable private fun MainPage(onNav: (GameViewModel.SettingsPage) -> Unit, onBack: () -> Unit, on3D: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Header("Settings", onBack); Spacer(Modifier.height(16.dp))
        MenuItem("General", "Theme mode, screen, performance") { onNav(GameViewModel.SettingsPage.GENERAL) }
        MenuItem("Profile", "Name, scores") { onNav(GameViewModel.SettingsPage.PROFILE) }
        MenuItem("Theme", "Colours and style") { onNav(GameViewModel.SettingsPage.THEME) }
        MenuItem("Layout", "Screen arrangement + 3D mode") { onNav(GameViewModel.SettingsPage.LAYOUT) }
        MenuItem("Gameplay", "Difficulty, mode") { onNav(GameViewModel.SettingsPage.GAMEPLAY) }
        MenuItem("Experience", "Animation, sound") { onNav(GameViewModel.SettingsPage.EXPERIENCE) }
        MenuItem("Controller", "Gamepad settings") { onNav(GameViewModel.SettingsPage.CONTROLLER) }
        MenuItem("How to Play", "Rules, scoring, tips") { onNav(GameViewModel.SettingsPage.HOW_TO_PLAY) }
        MenuItem("About", "Version, credits") { onNav(GameViewModel.SettingsPage.ABOUT) }
    }
}

// ===== GENERAL APP SETTINGS =====
@Composable private fun GeneralPage(
    appThemeMode: String, keepScreenOn: Boolean, orientationLock: String, immersiveMode: Boolean,
    frameRateTarget: Int, batterySaver: Boolean,
    onThemeMode: (String) -> Unit, onKeepScreen: (Boolean) -> Unit, onOrientation: (String) -> Unit,
    onImmersive: (Boolean) -> Unit, onFrameRate: (Int) -> Unit, onBatterySaver: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("General", onBack) }
        item { Lbl("App Theme Mode") }
        item { Card {
            Text("Controls the app interface appearance (settings, menus, dialogs)", color = dim(), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            listOf("auto" to "Auto (System)", "dark" to "Dark Mode", "light" to "Light Mode").forEach { (id, label) ->
                Sel(label, appThemeMode == id) { onThemeMode(id) }
            }
        } }
        item { Lbl("Screen") }
        item { Card {
            Toggle("Keep Screen On", keepScreenOn, onKeepScreen)
            Spacer(Modifier.height(4.dp))
            Text("Prevents screen dimming during gameplay", color = dim(), fontSize = 11.sp)
        } }
        item { Card {
            Toggle("Immersive Mode", immersiveMode, onImmersive)
            Spacer(Modifier.height(4.dp))
            Text("Hide status bar and navigation bar during gameplay", color = dim(), fontSize = 11.sp)
        } }
        item { Card {
            Text("Orientation Lock", color = tx(), fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            listOf("auto" to "Auto", "portrait" to "Portrait Only", "landscape" to "Landscape Only").forEach { (id, label) ->
                Sel(label, orientationLock == id) { onOrientation(id) }
            }
        } }
        item { Lbl("Performance") }
        item { Card {
            Text("Frame Rate", color = tx(), fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 30, 60, 120).forEach { fps ->
                    val sel = frameRateTarget == fps
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (sel) acc().copy(0.2f) else card())
                        .then(if (sel) Modifier.border(1.dp, acc(), RoundedCornerShape(8.dp)) else Modifier)
                        .clickable { onFrameRate(fps) }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text(if (fps == 0) "Auto" else "$fps FPS", color = if (sel) acc() else dim(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Higher frame rates use more battery", color = dim(), fontSize = 11.sp)
        } }
        item { Card {
            Toggle("Battery Saver", batterySaver, onBatterySaver)
            Spacer(Modifier.height(4.dp))
            Text("Reduces animations and lowers frame rate to save battery", color = dim(), fontSize = 11.sp)
        } }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ===== PROFILE =====
@Composable private fun ProfilePage(name: String, hs: Int, history: List<ScoreEntry>, onName: (String) -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    var editName by remember { mutableStateOf(name) }
    var sortBy by remember { mutableStateOf("score") }
    var showClearConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(name) { editName = name }
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()) }
    val sorted = remember(history, sortBy) {
        when (sortBy) {
            "score" -> history.sortedByDescending { it.score }
            "level" -> history.sortedByDescending { it.level }
            "lines" -> history.sortedByDescending { it.lines }
            else -> history.sortedByDescending { it.timestamp }
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Profile", onBack); Spacer(Modifier.height(16.dp)) }
        item { Card { Text("Player Name", color = dim(), fontSize = 12.sp); Spacer(Modifier.height(4.dp)); EditField(editName) { editName = it; onName(it) } } }
        item { Card { Text("High Score", color = dim(), fontSize = 12.sp); Text(hs.toString(), color = acc(), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) } }
        // Stats dashboard
        if (history.isNotEmpty()) {
            item { Lbl("Stats Dashboard") }
            item { Card {
                val totalGames = history.size
                val totalLines = history.sumOf { it.lines }
                val avgScore = history.sumOf { it.score } / totalGames
                val bestLevel = history.maxOf { it.level }
                val bestLines = history.maxOf { it.lines }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    DashStat("Games", "$totalGames")
                    DashStat("Total Lines", "$totalLines")
                    DashStat("Avg Score", "$avgScore")
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    DashStat("Best Level", "$bestLevel")
                    DashStat("Best Lines", "$bestLines")
                    DashStat("High Score", "$hs")
                }
            } }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Score History", color = acc(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (history.isNotEmpty()) {
                    if (showClearConfirm) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Clear all?", color = dim(), fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("Yes", color = Color(0xFFFF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onClear(); showClearConfirm = false }.padding(horizontal = 8.dp, vertical = 4.dp))
                            Text("No", color = dim(), fontSize = 12.sp,
                                modifier = Modifier.clickable { showClearConfirm = false }.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    } else {
                        Text("Clear", color = Color(0xFFFF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showClearConfirm = true }.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.spacedBy(6.dp)) {
                listOf("score" to "Score", "level" to "Level", "lines" to "Lines", "recent" to "Recent").forEach { (k, v) ->
                    Chip(v, sortBy == k) { sortBy = k }
                }
            }
        }
        if (sorted.isEmpty()) { item { Card { Text("No games yet", color = dim()) } } }
        items(sorted.size.coerceAtMost(100)) { i ->
            val e = sorted[i]
            Card {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("${i+1}.", color = dim(), modifier = Modifier.width(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.playerName, color = tx(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${e.score}", color = acc(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Lv${e.level}", color = dim(), fontSize = 12.sp)
                        Text("${e.lines}L", color = dim(), fontSize = 12.sp)
                        Text(dateFormat.format(java.util.Date(e.timestamp)), color = dim(), fontSize = 10.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ===== THEME =====
@Composable private fun ThemePage(current: GameTheme, custom: List<GameTheme>, multiColor: Boolean, pieceMaterial: String, onSet: (GameTheme) -> Unit, onMultiColor: (Boolean) -> Unit, onMaterial: (String) -> Unit, onNew: () -> Unit, onEdit: (GameTheme) -> Unit, onDelete: (String) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Theme", onBack); Spacer(Modifier.height(16.dp)) }
        item { Card {
            Toggle("Multicolor Pieces", multiColor, onMultiColor)
            Text("Each piece type has its own color (Cyan, Yellow, Purple, Green, Red, Blue, Orange)", color = dim(), fontSize = 11.sp)
            if (multiColor) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(Color(0xFF00D4FF), Color(0xFFF4D03F), Color(0xFFAA44FF), Color(0xFF44DD44), Color(0xFFFF4444), Color(0xFF4488FF), Color(0xFFFF8800)).forEach {
                        Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(it))
                    }
                }
            }
        } }
        item { Card {
            Text("Piece Material", color = tx(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Visual texture for 3D and 2D pieces", color = dim(), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("CLASSIC" to "Classic", "STONE" to "Stone", "GRANITE" to "Granite", "GLASS" to "Marble", "CRYSTAL" to "Diamond").forEach { (id, label) ->
                    val sel = pieceMaterial == id
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (sel) acc().copy(0.2f) else card())
                        .then(if (sel) Modifier.border(1.dp, acc(), RoundedCornerShape(8.dp)) else Modifier)
                        .clickable { onMaterial(id) }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center) {
                        Text(label, color = if (sel) acc() else dim(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } }
        item { Lbl("Built-in") }
        items(GameThemes.builtInThemes.size) { i -> val t = GameThemes.builtInThemes[i]; ThemeRow(t, t.id == current.id) { onSet(t) } }
        if (custom.isNotEmpty()) {
            item { Lbl("Custom") }
            items(custom.size) { i ->
                val t = custom[i]; val sel = t.id == current.id
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) selBg() else card()).clickable { onSet(t) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ThemeSwatches(t); Spacer(Modifier.width(12.dp)); Text(t.name, color = tx(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("EDIT", color = acc(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onEdit(t) }.padding(8.dp))
                    Text("✕", color = Color(0xFFFF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onDelete(t.id) }.padding(8.dp))
                    if (sel) Text("✓", color = acc(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)); ActionCard("+ Create Custom Theme", onNew) }
    }
}

@Composable private fun ThemeRow(t: GameTheme, sel: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) selBg() else card()).clickable { onClick() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        // Mini board preview
        ThemeMiniPreview(t)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, color = tx(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            ThemeSwatches(t)
        }
        if (sel) Text("✓", color = acc(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun ThemeMiniPreview(t: GameTheme) {
    // Tiny 5x4 board preview showing how the theme looks
    Box(Modifier.size(42.dp, 36.dp).clip(RoundedCornerShape(4.dp)).background(t.screenBackground).border(1.dp, t.deviceColor, RoundedCornerShape(4.dp)).padding(2.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val cellW = size.width / 5f; val cellH = size.height / 4f
            // Draw empty cells
            for (r in 0..3) for (c in 0..4) {
                drawRect(t.pixelOff, topLeft = androidx.compose.ui.geometry.Offset(c * cellW + 0.5f, r * cellH + 0.5f),
                    size = androidx.compose.ui.geometry.Size(cellW - 1f, cellH - 1f))
            }
            // Draw some "on" cells to simulate pieces
            val onCells = listOf(1 to 3, 2 to 3, 3 to 3, 4 to 3, 0 to 2, 1 to 2, 2 to 2, 3 to 1, 3 to 2, 4 to 2)
            onCells.forEach { (c, r) ->
                drawRect(t.pixelOn, topLeft = androidx.compose.ui.geometry.Offset(c * cellW + 0.5f, r * cellH + 0.5f),
                    size = androidx.compose.ui.geometry.Size(cellW - 1f, cellH - 1f))
            }
        }
    }
}

@Composable private fun ThemeSwatches(t: GameTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(t.screenBackground).border(1.dp, dim().copy(0.3f), CircleShape))
        Box(Modifier.size(22.dp).clip(CircleShape).background(t.pixelOn).border(1.dp, dim().copy(0.3f), CircleShape))
        Box(Modifier.size(22.dp).clip(CircleShape).background(t.accentColor).border(1.dp, dim().copy(0.3f), CircleShape))
    }
}

// ===== LAYOUT =====
@Composable private fun LayoutPage(p: LayoutPreset, l: LayoutPreset, d: DPadStyle, buttonStyle: String, custom: List<CustomLayoutData>, active: CustomLayoutData?,
                                    onP: (LayoutPreset) -> Unit, onL: (LayoutPreset) -> Unit, onD: (DPadStyle) -> Unit, onSetButtonStyle: (String) -> Unit,
                                    onNew: () -> Unit, onEdit: (CustomLayoutData) -> Unit, onSelect: (CustomLayoutData) -> Unit, onClear: () -> Unit, onDelete: (String) -> Unit,
                                    onEditFreeform: () -> Unit = {}, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Layout", onBack) }
        item { Lbl("Portrait") }
        items(LayoutPreset.portraitPresets().size) { i -> val x = LayoutPreset.portraitPresets()[i]; Sel(x.displayName, x == p && active == null) { onClear(); onP(x) } }
        if (p == LayoutPreset.PORTRAIT_FREEFORM && active == null) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(acc().copy(0.08f)).padding(12.dp)) {
                    Text("Board fills the screen. Drag controls and info to any position.", color = dim(), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(acc().copy(0.15f)).clickable { onEditFreeform() }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("✎ Edit Element Positions", color = acc(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Lbl("Landscape") }; items(LayoutPreset.landscapePresets().size) { i -> val x = LayoutPreset.landscapePresets()[i]; Sel(x.displayName, x == l) { onL(x) } }
        item { Lbl("D-Pad Style") }; items(DPadStyle.entries.size) { i -> val x = DPadStyle.entries[i]; Sel(x.displayName, x == d) { onD(x) } }
    }
}

// ===== GAMEPLAY =====
@Composable private fun GameplayPage(diff: Difficulty, mode: GameMode, ghost: Boolean, levelEvents: Boolean, infinityTimer: Int, infinityTimerEnabled: Boolean, onD: (Difficulty) -> Unit, onM: (GameMode) -> Unit, onG: (Boolean) -> Unit, onLE: (Boolean) -> Unit, onIT: (Int) -> Unit, onITE: (Boolean) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Gameplay", onBack) }
        item { Lbl("Difficulty") }; items(Difficulty.entries.size) { i -> val d = Difficulty.entries[i]; Sel("${d.displayName} — ${d.description}", d == diff) { onD(d) } }
        item { Lbl("Game Mode") }; items(GameMode.entries.size) { i -> val m = GameMode.entries[i]; Sel("${m.displayName} — ${m.description}", m == mode) { onM(m) } }
        if (mode == GameMode.INFINITY) {
            item {
                var showTimerDialog by remember { mutableStateOf(false) }
                Card {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Play Timer", color = tx(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Ends the session when time runs out", color = dim(), fontSize = 11.sp)
                        }
                        Switch(checked = infinityTimerEnabled && infinityTimer > 0, onCheckedChange = {
                            if (it && infinityTimer == 0) showTimerDialog = true
                            else onITE(it)
                        }, colors = SwitchDefaults.colors(checkedThumbColor = acc(), checkedTrackColor = acc().copy(0.3f)))
                    }
                    if (infinityTimer > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            val h = infinityTimer / 3600; val m = (infinityTimer % 3600) / 60; val s = infinityTimer % 60
                            Text("%02d : %02d : %02d".format(h, m, s), color = if (infinityTimerEnabled) acc() else dim(),
                                fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(acc().copy(0.15f)).border(1.dp, acc(), RoundedCornerShape(8.dp)).clickable { showTimerDialog = true }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text("Edit", color = acc(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (showTimerDialog) {
                    TimerPickerDialog(
                        initialSeconds = if (infinityTimer > 0) infinityTimer else 3600,
                        onConfirm = { onIT(it); onITE(true); showTimerDialog = false },
                        onDismiss = { showTimerDialog = false }
                    )
                }
            }
        }
        item { Lbl("Options") }
        item { Card { Toggle("Ghost Piece", ghost, onG) } }
        item { Card {
            Toggle("Level Events", levelEvents, onLE)
            Spacer(Modifier.height(4.dp))
            Text("Progressive gameplay changes at each level milestone (Marathon only). Ghost fades, previews reduce, hold locks.", color = dim(), fontSize = 11.sp)
        } }
    }
}

// ===== EXPERIENCE =====
@Composable private fun ExperiencePage(aS: AnimationStyle, aD: Float, s: Boolean, v: Boolean, onAS: (AnimationStyle) -> Unit, onAD: (Float) -> Unit, onS: (Boolean) -> Unit, onV: (Boolean) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Experience", onBack) }
        item { Lbl("Animation") }; items(AnimationStyle.entries.size) { i -> val x = AnimationStyle.entries[i]; Sel("${x.displayName} — ${x.description}", x == aS) { onAS(x) } }
        item { Card { Text("Speed", color = tx(), fontSize = 14.sp); Slider(aD, onAD, valueRange = 0.1f..2f, colors = sliderColors()); Text("${(aD*1000).toInt()}ms", color = dim(), fontSize = 12.sp) } }
        item { Lbl("Sound & Vibration") }; item { Card { Toggle("Sound", s, onS); Spacer(Modifier.height(8.dp)); Toggle("Vibration", v, onV) } }
    }
}

// ===== CONTROLLER =====
@Composable private fun ControllerPage(enabled: Boolean, deadzone: Float, controllerLayout: String, onEnable: (Boolean) -> Unit, onDeadzone: (Float) -> Unit, onLayout: (String) -> Unit, onBack: () -> Unit) {
    val controllers = remember { com.brickgame.tetris.input.GamepadController.getConnectedControllers() }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Controller", onBack) }
        item { Lbl("Gamepad / Controller") }
        item { Card {
            Toggle("Controller Enabled", enabled, onEnable)
            Spacer(Modifier.height(4.dp))
            Text("Use a connected gamepad or Bluetooth controller", color = dim(), fontSize = 11.sp)
        } }
        item { Card {
            Text("Stick Deadzone", color = tx(), fontSize = 14.sp)
            Slider(deadzone, onDeadzone, valueRange = 0.05f..0.8f, colors = sliderColors())
            Text("${(deadzone * 100).toInt()}%", color = dim(), fontSize = 12.sp)
            Text("Higher = less sensitive to small stick movements", color = dim(), fontSize = 11.sp)
        } }
        item { Lbl("Controller Layout") }
        item { Card {
            Text("When a gamepad is connected, the screen layout adjusts:", color = dim(), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            listOf("auto" to "Auto — full-screen board when gamepad detected", "minimal" to "Minimal — always use full-screen board", "normal" to "Normal — keep selected layout with gamepad").forEach { (id, label) ->
                Sel(label, controllerLayout == id) { onLayout(id) }
            }
        } }
        item { Lbl("Connected Controllers") }
        if (controllers.isEmpty()) {
            item { Card { Text("No controllers detected", color = dim(), fontSize = 14.sp); Spacer(Modifier.height(4.dp)); Text("Connect a Bluetooth or USB gamepad to get started", color = dim(), fontSize = 11.sp) } }
        } else {
            items(controllers.size) { i ->
                val c = controllers[i]
                Card {
                    Text(c.name, color = tx(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Vendor: ${c.vendorId}  Product: ${c.productId}", color = dim(), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("✓ Connected", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Lbl("Button Mapping") }
        item { Card {
            Text("Default Layout", color = tx(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            MapRow("D-Pad / L-Stick", "Move piece")
            MapRow("A / Cross", "Rotate (Spin)")
            MapRow("B / Circle", "Rotate (Tilt)")
            MapRow("X / Square", "Hold piece")
            MapRow("Y / Triangle", "Hard drop")
            MapRow("LB / L1", "Move Z- (3D)")
            MapRow("RB / R1", "Move Z+ (3D)")
            MapRow("Start", "Pause / Resume")
            MapRow("Select", "Settings")
            MapRow("L-Stick click", "Toggle gravity (3D)")
        } }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable private fun MapRow(button: String, action: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
        Text(button, color = acc(), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(120.dp))
        Text(action, color = tx(), fontSize = 12.sp)
    }
}

// ===== ABOUT (Updated v3.6.0, no changelog) =====
@Composable private fun AboutPage(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("About", onBack); Spacer(Modifier.height(24.dp)) }
        item { Card { Text("BRICK GAME", color = acc(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace); Text("Kotlin Edition", color = tx(), fontSize = 14.sp) } }
        item { Card { Info("Version", "3.6.0"); Info("Build", "16"); Info("Platform", "Android 8.0+"); Info("Engine", "Compose + OpenGL ES") } }
        item { Card { Text("Developer", color = dim(), fontSize = 12.sp); Text("Andrei Anton", color = tx(), fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        item { Lbl("Features") }
        item { Card {
            Text("3D Tetris mode with OpenGL ES rendering", color = tx(), fontSize = 12.sp)
            Text("Free camera rotation, zoom, and pan", color = tx(), fontSize = 12.sp)
            Text("Realistic piece materials (Stone, Granite, Marble, Diamond)", color = tx(), fontSize = 12.sp)
            Text("6 layouts (Classic, Modern, Fullscreen, Compact, Freeform, 3D)", color = tx(), fontSize = 12.sp)
            Text("Freeform editor: drag, resize, snap grid, transparency", color = tx(), fontSize = 12.sp)
            Text("5 built-in themes + custom theme editor", color = tx(), fontSize = 12.sp)
            Text("General app settings with Auto/Dark/Light mode", color = tx(), fontSize = 12.sp)
            Text("Hold piece, Next queue (1-3), Ghost piece", color = tx(), fontSize = 12.sp)
            Text("D-Pad and Swipe control styles + Gamepad support", color = tx(), fontSize = 12.sp)
            Text("Score history with sort and filter", color = tx(), fontSize = 12.sp)
            Text("SRS rotation + wall kicks", color = tx(), fontSize = 12.sp)
        } }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ===== HOW TO PLAY =====
@Composable private fun HowToPlayPage(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("How to Play", onBack); Spacer(Modifier.height(16.dp)) }

        item { Lbl("The Basics") }
        item { Card {
            Text("Tetris is a puzzle game where you arrange falling pieces (tetrominoes) to complete horizontal lines. Completed lines are cleared from the board and award points.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("The game ends when the pieces stack up to the top of the board.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
        } }

        item { Lbl("Controls") }
        item { Card {
            Text("Move Left / Right — slide the piece sideways", color = tx(), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("Soft Drop (Down) — push the piece down faster (1 point per cell)", color = tx(), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("Hard Drop (Up) — instantly drop the piece to the bottom (2 points per cell)", color = tx(), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("Rotate — spin the piece 90° clockwise", color = tx(), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("Hold — store the current piece for later (once per drop)", color = tx(), fontSize = 13.sp)
        } }

        item { Lbl("Scoring") }
        item { Card {
            Text("Points are awarded based on how many lines you clear at once, multiplied by your current level:", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            InfoLine("1 line (Single)", "100 × level")
            InfoLine("2 lines (Double)", "300 × level")
            InfoLine("3 lines (Triple)", "500 × level")
            InfoLine("4 lines (Tetris!)", "800 × level")
            Spacer(Modifier.height(8.dp))
            Text("T-Spin — rotating a T-piece into a tight space awards bonus points (200–1600 × level).", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("Back-to-Back — consecutive difficult clears (Tetris or T-Spin) earn a 1.5× bonus.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("Combo — clearing lines on consecutive piece drops adds 50 × combo × level bonus points.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
        } }

        item { Lbl("Levels & Speed") }
        item { Card {
            Text("You advance one level for every 10 lines cleared. The maximum level is 20.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("Each level increases the speed at which pieces fall:", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            InfoLine("Level 1", "1.0 s per drop")
            InfoLine("Level 5", "0.6 s per drop")
            InfoLine("Level 10", "0.3 s per drop")
            InfoLine("Level 15", "0.15 s per drop")
            InfoLine("Level 20", "0.075 s per drop")
        } }

        item { Lbl("Difficulty") }
        item { Card {
            Text("Choose your difficulty before starting. It affects starting level, speed, and score multiplier:", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            InfoLine("Easy", "Starts Lv1, 50% slower, 0.5× score")
            InfoLine("Normal", "Starts Lv1, standard speed and scoring")
            InfoLine("Hard", "Starts Lv5, 20% faster, 1.5× score")
            InfoLine("Expert", "Starts Lv10, 40% faster, 2× score")
            InfoLine("Master", "Starts Lv15, 60% faster, 3× score")
        } }

        item { Lbl("Game Modes") }
        item { Card {
            Text("Marathon — classic endless mode. Play until you top out and aim for the highest score.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("Sprint 40L — clear 40 lines as fast as possible. A race against the clock.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("Ultra 2min — score as high as you can in 2 minutes. Every piece counts.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
        } }

        item { Lbl("Tips") }
        item { Card {
            Text("Build flat — avoid creating holes and valleys. Keep the surface even.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("Save the I-piece — leave a column open on one side and use the long bar for Tetris (4-line) clears.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("Use Hold wisely — save awkward pieces for later when you have a better spot.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("Watch the Next queue — plan 2-3 pieces ahead for better placement.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("Use the Ghost piece — the translucent shadow shows exactly where the piece will land.", color = tx(), fontSize = 13.sp, lineHeight = 20.sp)
        } }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable private fun InfoLine(l: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
        Text(l, color = tx(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(v, color = acc(), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

// ===== REUSABLE =====
@Composable private fun Header(t: String, onBack: () -> Unit) { val a = acc(); val t2 = tx(); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("←", color = a, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(8.dp)); Spacer(Modifier.width(12.dp)); Text(t, color = t2, fontSize = 22.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun MenuItem(t: String, sub: String, onClick: () -> Unit) { val c = card(); val t2 = tx(); val d = dim(); Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(c).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(t, color = t2, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(sub, color = d, fontSize = 12.sp) }; Text("›", color = d, fontSize = 20.sp) } }
@Composable private fun Card(content: @Composable ColumnScope.() -> Unit) { val c = card(); val bd = cardBorder(); Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(c).then(if (bd != Color.Transparent) Modifier.border(1.dp, bd, RoundedCornerShape(12.dp)) else Modifier).padding(16.dp), content = content) }
@Composable private fun Lbl(t: String) { Text(t, color = acc(), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
@Composable private fun Sel(text: String, sel: Boolean, onClick: () -> Unit) { val a = acc(); val c = card(); val t2 = tx(); val sb = selBg(); Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(if (sel) sb else c).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(text, color = t2, fontSize = 14.sp, modifier = Modifier.weight(1f)); if (sel) Text("✓", color = a, fontWeight = FontWeight.Bold) } }
@Composable private fun Toggle(label: String, v: Boolean, on: (Boolean) -> Unit) { val a = acc(); val t2 = tx(); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(label, color = t2, fontSize = 14.sp); Switch(v, on, colors = SwitchDefaults.colors(checkedTrackColor = a)) } }
@Composable private fun Info(l: String, v: String) { val d = dim(); val t2 = tx(); Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) { Text(l, color = d, fontSize = 14.sp); Text(v, color = t2, fontSize = 14.sp, fontFamily = FontFamily.Monospace) } }
@Composable private fun ActionCard(text: String, onClick: () -> Unit) { val a = acc(); Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(a.copy(0.15f)).clickable { onClick() }.padding(16.dp), Arrangement.Center) { Text(text, color = a, fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
@Composable private fun Chip(text: String, sel: Boolean, onClick: () -> Unit) { val a = acc(); val c = card(); val d = dim(); val t2 = tx(); Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) a.copy(0.2f) else c).border(1.dp, if (sel) a else d.copy(0.3f), RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text(text, color = if (sel) a else t2, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) } }
@Composable private fun EditField(value: String, onChange: (String) -> Unit) { val a = acc(); val t2 = tx(); val fb = if (LocalIsDarkMode.current) Color(0xFF252525) else Color(0xFFF0F0F0); BasicTextField(value, onChange, textStyle = TextStyle(color = t2, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(a), singleLine = true, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(fb).padding(12.dp)) }
@Composable private fun sliderColors() = SliderDefaults.colors(thumbColor = acc(), activeTrackColor = acc(), inactiveTrackColor = acc().copy(0.15f))

@Composable private fun DashStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Text(value, color = acc(), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, color = dim(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

// ===== TIMER PICKER DIALOG — fullscreen overlay with h:m:s scroll wheels =====
@Composable private fun TimerPickerDialog(initialSeconds: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val a = acc(); val d = dim(); val t2 = tx()
    val cardBg = if (LocalIsDarkMode.current) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    var hours by remember { mutableStateOf(initialSeconds / 3600) }
    var minutes by remember { mutableStateOf((initialSeconds % 3600) / 60) }
    var seconds by remember { mutableStateOf(initialSeconds % 60) }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.7f)).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }, Alignment.Center) {
        Column(
            Modifier.padding(24.dp).clip(RoundedCornerShape(24.dp)).background(cardBg)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set Play Timer", color = t2, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Swipe to set · Max 10 hours", color = d, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))

            // Three swipeable wheels: H : M : S
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                SwipeWheelPicker(range = 0..10, value = hours, onValue = { hours = it; if (hours == 10) { minutes = 0; seconds = 0 } }, label = "hours", padDigits = 2)
                Text(":", color = a, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 2.dp).padding(bottom = 18.dp))
                SwipeWheelPicker(range = 0..59, value = minutes, onValue = { if (hours < 10) minutes = it }, label = "min", padDigits = 2, enabled = hours < 10)
                Text(":", color = a, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 2.dp).padding(bottom = 18.dp))
                SwipeWheelPicker(range = 0..59, value = seconds, onValue = { if (hours < 10) seconds = it }, label = "sec", padDigits = 2, enabled = hours < 10)
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(d.copy(0.15f)).clickable { onDismiss() }.padding(horizontal = 28.dp, vertical = 14.dp)) {
                    Text("Cancel", color = d, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(a).clickable {
                    onConfirm((hours * 3600 + minutes * 60 + seconds).coerceIn(1, 36000))
                }.padding(horizontal = 28.dp, vertical = 14.dp)) {
                    Text("Set Timer", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ===== SWIPE WHEEL PICKER — LazyColumn-based with snap scrolling =====
@Composable private fun SwipeWheelPicker(
    range: IntRange, value: Int, onValue: (Int) -> Unit,
    label: String, padDigits: Int = 2, enabled: Boolean = true
) {
    val a = if (enabled) acc() else dim().copy(0.4f)
    val d = dim()
    val itemHeight = 44.dp
    val visibleItems = 5
    val items = range.toList()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.roundToPx() }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, items.size - 1)
    )

    // Snap to nearest item when scroll settles
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && enabled) {
            val centerOffset = listState.firstVisibleItemScrollOffset
            val idx = listState.firstVisibleItemIndex
            val snapIdx = if (centerOffset > itemHeightPx / 2) (idx + 1).coerceAtMost(items.size - 1) else idx
            listState.animateScrollToItem(snapIdx)
            val newVal = items.getOrNull(snapIdx) ?: return@LaunchedEffect
            if (newVal != value) onValue(newVal)
        }
    }

    // Sync external value changes
    LaunchedEffect(value) {
        val targetIdx = (value - range.first).coerceIn(0, items.size - 1)
        if (listState.firstVisibleItemIndex != targetIdx) {
            listState.scrollToItem(targetIdx)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(72.dp).height(itemHeight * visibleItems)
            .clip(RoundedCornerShape(14.dp))
            .background(if (LocalIsDarkMode.current) Color(0xFF2A2A2A) else Color(0xFFEEEEEE))
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemHeight * 2),
                userScrollEnabled = enabled
            ) {
                items(items.size) { idx ->
                    val v = items[idx]
                    val isCenter = idx == listState.firstVisibleItemIndex
                    Box(Modifier.fillMaxWidth().height(itemHeight), Alignment.Center) {
                        Text(
                            v.toString().padStart(padDigits, '0'),
                            color = if (isCenter) a else d.copy(if (isCenter) 1f else 0.35f),
                            fontSize = if (isCenter) 28.sp else 16.sp,
                            fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            // Center highlight overlay
            Box(Modifier.fillMaxWidth().height(itemHeight).align(Alignment.Center)
                .background(a.copy(0.1f), RoundedCornerShape(8.dp))
                .border(1.5.dp, a.copy(0.4f), RoundedCornerShape(8.dp)))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = d, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
