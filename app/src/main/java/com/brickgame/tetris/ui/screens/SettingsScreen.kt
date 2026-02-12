package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.*
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

private val BG = Color(0xFF0D0D0D); private val CARD = Color(0xFF1A1A1A)
private val ACC = Color(0xFFF4D03F); private val TX = Color(0xFFE8E8E8); private val DIM = Color(0xFF888888)

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
    on3DMode: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(BG).systemBarsPadding()) {
        when (page) {
            GameViewModel.SettingsPage.MAIN -> MainPage(onNavigate, onBack, on3DMode)
            GameViewModel.SettingsPage.GENERAL -> GeneralPage(appThemeMode, keepScreenOn, orientationLock, immersiveMode, frameRateTarget, batterySaver, highContrast, uiScale, onSetAppThemeMode, onSetKeepScreenOn, onSetOrientationLock, onSetImmersiveMode, onSetFrameRateTarget, onSetBatterySaver, onSetHighContrast, onSetUiScale) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.PROFILE -> ProfilePage(playerName, highScore, scoreHistory, onSetPlayerName) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.THEME -> ThemePage(currentTheme, customThemes, multiColorEnabled, pieceMaterial, onSetTheme, onSetMultiColorEnabled, onSetPieceMaterial, onNewTheme, onEditTheme, onDeleteTheme) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.THEME_EDITOR -> if (editingTheme != null) ThemeEditorScreen(editingTheme, onUpdateEditingTheme, onSaveTheme) { onNavigate(GameViewModel.SettingsPage.THEME) }
            GameViewModel.SettingsPage.LAYOUT -> LayoutPage(portraitLayout, landscapeLayout, dpadStyle, customLayouts, activeCustomLayout, onSetPortraitLayout, onSetLandscapeLayout, onSetDPadStyle, onNewLayout, onEditLayout, onSelectCustomLayout, onClearCustomLayout, onDeleteLayout, onEditFreeform = { onEditFreeform() }) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.LAYOUT_EDITOR -> if (editingLayout != null) LayoutEditorScreen(editingLayout, currentTheme, portraitLayout, dpadStyle, onUpdateEditingLayout, onSaveLayout) { onNavigate(GameViewModel.SettingsPage.LAYOUT) }
            GameViewModel.SettingsPage.GAMEPLAY -> GameplayPage(difficulty, gameMode, ghostEnabled, onSetDifficulty, onSetGameMode, onSetGhostEnabled) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.EXPERIENCE -> ExperiencePage(animationStyle, animationDuration, soundEnabled, vibrationEnabled, onSetAnimationStyle, onSetAnimationDuration, onSetSoundEnabled, onSetVibrationEnabled) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.CONTROLLER -> ControllerPage(controllerEnabled, controllerDeadzone, onSetControllerEnabled, onSetControllerDeadzone) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.ABOUT -> AboutPage { onNavigate(GameViewModel.SettingsPage.MAIN) }
        }
    }
}

// ===== MAIN =====
@Composable private fun MainPage(onNav: (GameViewModel.SettingsPage) -> Unit, onBack: () -> Unit, on3D: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Header("Settings", onBack); Spacer(Modifier.height(16.dp))
        MenuItem("General", "Theme mode, screen, performance") { onNav(GameViewModel.SettingsPage.GENERAL) }
        MenuItem("Profile", "Name, scores") { onNav(GameViewModel.SettingsPage.PROFILE) }
        MenuItem("Theme", "Colours and style") { onNav(GameViewModel.SettingsPage.THEME) }
        MenuItem("Layout", "Screen arrangement + 3D mode") { onNav(GameViewModel.SettingsPage.LAYOUT) }
        MenuItem("Gameplay", "Difficulty, mode") { onNav(GameViewModel.SettingsPage.GAMEPLAY) }
        MenuItem("Experience", "Animation, sound") { onNav(GameViewModel.SettingsPage.EXPERIENCE) }
        MenuItem("Controller", "Gamepad settings") { onNav(GameViewModel.SettingsPage.CONTROLLER) }
        MenuItem("About", "Version, credits") { onNav(GameViewModel.SettingsPage.ABOUT) }
    }
}

// ===== GENERAL APP SETTINGS =====
@Composable private fun GeneralPage(
    appThemeMode: String, keepScreenOn: Boolean, orientationLock: String, immersiveMode: Boolean,
    frameRateTarget: Int, batterySaver: Boolean, highContrast: Boolean, uiScale: Float,
    onThemeMode: (String) -> Unit, onKeepScreen: (Boolean) -> Unit, onOrientation: (String) -> Unit,
    onImmersive: (Boolean) -> Unit, onFrameRate: (Int) -> Unit, onBatterySaver: (Boolean) -> Unit,
    onHighContrast: (Boolean) -> Unit, onUiScale: (Float) -> Unit, onBack: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("General", onBack) }
        item { Lbl("App Theme Mode") }
        item { Card {
            Text("Controls the app interface appearance (settings, menus, dialogs)", color = DIM, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            listOf("auto" to "Auto (System)", "dark" to "Dark Mode", "light" to "Light Mode").forEach { (id, label) ->
                Sel(label, appThemeMode == id) { onThemeMode(id) }
            }
        } }
        item { Lbl("Screen") }
        item { Card {
            Toggle("Keep Screen On", keepScreenOn, onKeepScreen)
            Spacer(Modifier.height(4.dp))
            Text("Prevents screen dimming during gameplay", color = DIM, fontSize = 11.sp)
        } }
        item { Card {
            Toggle("Immersive Mode", immersiveMode, onImmersive)
            Spacer(Modifier.height(4.dp))
            Text("Hide status bar and navigation bar during gameplay", color = DIM, fontSize = 11.sp)
        } }
        item { Card {
            Text("Orientation Lock", color = TX, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            listOf("auto" to "Auto", "portrait" to "Portrait Only", "landscape" to "Landscape Only").forEach { (id, label) ->
                Sel(label, orientationLock == id) { onOrientation(id) }
            }
        } }
        item { Lbl("Performance") }
        item { Card {
            Text("Frame Rate", color = TX, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(30, 60, 120).forEach { fps ->
                    val sel = frameRateTarget == fps
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (sel) ACC.copy(0.2f) else CARD)
                        .then(if (sel) Modifier.border(1.dp, ACC, RoundedCornerShape(8.dp)) else Modifier)
                        .clickable { onFrameRate(fps) }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text("$fps FPS", color = if (sel) ACC else DIM, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Higher frame rates use more battery", color = DIM, fontSize = 11.sp)
        } }
        item { Card {
            Toggle("Battery Saver", batterySaver, onBatterySaver)
            Spacer(Modifier.height(4.dp))
            Text("Reduces animations and lowers frame rate to save battery", color = DIM, fontSize = 11.sp)
        } }
        item { Lbl("Accessibility") }
        item { Card {
            Toggle("High Contrast", highContrast, onHighContrast)
            Spacer(Modifier.height(4.dp))
            Text("Increases colour contrast for better visibility", color = DIM, fontSize = 11.sp)
        } }
        item { Card {
            Text("UI Scale", color = TX, fontSize = 14.sp)
            Slider(uiScale, onUiScale, valueRange = 0.8f..1.5f, colors = sliderColors())
            Text("${(uiScale * 100).toInt()}%", color = DIM, fontSize = 12.sp)
        } }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ===== PROFILE =====
@Composable private fun ProfilePage(name: String, hs: Int, history: List<ScoreEntry>, onName: (String) -> Unit, onBack: () -> Unit) {
    var editName by remember { mutableStateOf(name) }
    var sortBy by remember { mutableStateOf("score") }
    LaunchedEffect(name) { editName = name }
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
        item { Card { Text("Player Name", color = DIM, fontSize = 12.sp); Spacer(Modifier.height(4.dp)); EditField(editName) { editName = it; onName(it) } } }
        item { Card { Text("High Score", color = DIM, fontSize = 12.sp); Text(hs.toString(), color = ACC, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) } }
        item { Lbl("Score History") }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.spacedBy(6.dp)) {
                listOf("score" to "Score", "level" to "Level", "lines" to "Lines", "recent" to "Recent").forEach { (k, v) ->
                    Chip(v, sortBy == k) { sortBy = k }
                }
            }
        }
        if (sorted.isEmpty()) { item { Card { Text("No games yet", color = DIM) } } }
        items(sorted.size.coerceAtMost(50)) { i ->
            val e = sorted[i]
            Card {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("${i+1}.", color = DIM, modifier = Modifier.width(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.playerName, color = TX, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${e.score}", color = ACC, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Lv${e.level}", color = DIM, fontSize = 12.sp)
                        Text("${e.lines}L", color = DIM, fontSize = 12.sp)
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
            Text("Each piece type has its own color (Cyan, Yellow, Purple, Green, Red, Blue, Orange)", color = DIM, fontSize = 11.sp)
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
            Text("Piece Material", color = TX, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Visual texture for 3D and 2D pieces", color = DIM, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("CLASSIC" to "Classic", "STONE" to "Stone", "GRANITE" to "Granite", "GLASS" to "Marble", "CRYSTAL" to "Diamond").forEach { (id, label) ->
                    val sel = pieceMaterial == id
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (sel) ACC.copy(0.2f) else CARD)
                        .then(if (sel) Modifier.border(1.dp, ACC, RoundedCornerShape(8.dp)) else Modifier)
                        .clickable { onMaterial(id) }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center) {
                        Text(label, color = if (sel) ACC else DIM, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) ACC.copy(0.15f) else CARD).clickable { onSet(t) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ThemeSwatches(t); Spacer(Modifier.width(12.dp)); Text(t.name, color = TX, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("EDIT", color = ACC, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onEdit(t) }.padding(8.dp))
                    Text("✕", color = Color(0xFFFF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onDelete(t.id) }.padding(8.dp))
                    if (sel) Text("✓", color = ACC, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)); ActionCard("+ Create Custom Theme", onNew) }
    }
}

@Composable private fun ThemeRow(t: GameTheme, sel: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) ACC.copy(0.15f) else CARD).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ThemeSwatches(t); Spacer(Modifier.width(12.dp)); Text(t.name, color = TX, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (sel) Text("✓", color = ACC, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun ThemeSwatches(t: GameTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(t.screenBackground).border(1.dp, DIM.copy(0.3f), CircleShape))
        Box(Modifier.size(22.dp).clip(CircleShape).background(t.pixelOn).border(1.dp, DIM.copy(0.3f), CircleShape))
        Box(Modifier.size(22.dp).clip(CircleShape).background(t.accentColor).border(1.dp, DIM.copy(0.3f), CircleShape))
    }
}

// ===== LAYOUT =====
@Composable private fun LayoutPage(p: LayoutPreset, l: LayoutPreset, d: DPadStyle, custom: List<CustomLayoutData>, active: CustomLayoutData?,
                                    onP: (LayoutPreset) -> Unit, onL: (LayoutPreset) -> Unit, onD: (DPadStyle) -> Unit,
                                    onNew: () -> Unit, onEdit: (CustomLayoutData) -> Unit, onSelect: (CustomLayoutData) -> Unit, onClear: () -> Unit, onDelete: (String) -> Unit,
                                    onEditFreeform: () -> Unit = {}, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Layout", onBack) }
        item { Lbl("Portrait") }
        items(LayoutPreset.portraitPresets().size) { i -> val x = LayoutPreset.portraitPresets()[i]; Sel(x.displayName, x == p && active == null) { onClear(); onP(x) } }
        if (p == LayoutPreset.PORTRAIT_FREEFORM && active == null) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(ACC.copy(0.08f)).padding(12.dp)) {
                    Text("Board fills the screen. Drag controls and info to any position.", color = DIM, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ACC.copy(0.15f)).clickable { onEditFreeform() }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("✎ Edit Element Positions", color = ACC, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Lbl("Landscape") }; items(LayoutPreset.landscapePresets().size) { i -> val x = LayoutPreset.landscapePresets()[i]; Sel(x.displayName, x == l) { onL(x) } }
        item { Lbl("D-Pad Style") }; items(DPadStyle.entries.size) { i -> val x = DPadStyle.entries[i]; Sel(x.displayName, x == d) { onD(x) } }
    }
}

// ===== GAMEPLAY =====
@Composable private fun GameplayPage(diff: Difficulty, mode: GameMode, ghost: Boolean, onD: (Difficulty) -> Unit, onM: (GameMode) -> Unit, onG: (Boolean) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Gameplay", onBack) }
        item { Lbl("Difficulty") }; items(Difficulty.entries.size) { i -> val d = Difficulty.entries[i]; Sel("${d.displayName} — ${d.description}", d == diff) { onD(d) } }
        item { Lbl("Game Mode") }; items(GameMode.entries.size) { i -> val m = GameMode.entries[i]; Sel("${m.displayName} — ${m.description}", m == mode) { onM(m) } }
        item { Lbl("Options") }; item { Card { Toggle("Ghost Piece", ghost, onG) } }
    }
}

// ===== EXPERIENCE =====
@Composable private fun ExperiencePage(aS: AnimationStyle, aD: Float, s: Boolean, v: Boolean, onAS: (AnimationStyle) -> Unit, onAD: (Float) -> Unit, onS: (Boolean) -> Unit, onV: (Boolean) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Experience", onBack) }
        item { Lbl("Animation") }; items(AnimationStyle.entries.size) { i -> val x = AnimationStyle.entries[i]; Sel("${x.displayName} — ${x.description}", x == aS) { onAS(x) } }
        item { Card { Text("Speed", color = TX, fontSize = 14.sp); Slider(aD, onAD, valueRange = 0.1f..2f, colors = sliderColors()); Text("${(aD*1000).toInt()}ms", color = DIM, fontSize = 12.sp) } }
        item { Lbl("Sound & Vibration") }; item { Card { Toggle("Sound", s, onS); Spacer(Modifier.height(8.dp)); Toggle("Vibration", v, onV) } }
    }
}

// ===== CONTROLLER =====
@Composable private fun ControllerPage(enabled: Boolean, deadzone: Float, onEnable: (Boolean) -> Unit, onDeadzone: (Float) -> Unit, onBack: () -> Unit) {
    val controllers = remember { com.brickgame.tetris.input.GamepadController.getConnectedControllers() }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Controller", onBack) }
        item { Lbl("Gamepad / Controller") }
        item { Card {
            Toggle("Controller Enabled", enabled, onEnable)
            Spacer(Modifier.height(4.dp))
            Text("Use a connected gamepad or Bluetooth controller", color = DIM, fontSize = 11.sp)
        } }
        item { Card {
            Text("Stick Deadzone", color = TX, fontSize = 14.sp)
            Slider(deadzone, onDeadzone, valueRange = 0.05f..0.8f, colors = sliderColors())
            Text("${(deadzone * 100).toInt()}%", color = DIM, fontSize = 12.sp)
            Text("Higher = less sensitive to small stick movements", color = DIM, fontSize = 11.sp)
        } }
        item { Lbl("Connected Controllers") }
        if (controllers.isEmpty()) {
            item { Card { Text("No controllers detected", color = DIM, fontSize = 14.sp); Spacer(Modifier.height(4.dp)); Text("Connect a Bluetooth or USB gamepad to get started", color = DIM, fontSize = 11.sp) } }
        } else {
            items(controllers.size) { i ->
                val c = controllers[i]
                Card {
                    Text(c.name, color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Vendor: ${c.vendorId}  Product: ${c.productId}", color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("✓ Connected", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Lbl("Button Mapping") }
        item { Card {
            Text("Default Layout", color = TX, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        Text(button, color = ACC, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(120.dp))
        Text(action, color = TX, fontSize = 12.sp)
    }
}

// ===== ABOUT (Updated v3.6.0, no changelog) =====
@Composable private fun AboutPage(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("About", onBack); Spacer(Modifier.height(24.dp)) }
        item { Card { Text("BRICK GAME", color = ACC, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace); Text("Kotlin Edition", color = TX, fontSize = 14.sp) } }
        item { Card { Info("Version", "3.6.0"); Info("Build", "16"); Info("Platform", "Android 8.0+"); Info("Engine", "Compose + OpenGL ES") } }
        item { Card { Text("Developer", color = DIM, fontSize = 12.sp); Text("Andrei Anton", color = TX, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        item { Lbl("Features") }
        item { Card {
            Text("3D Tetris mode with OpenGL ES rendering", color = TX, fontSize = 12.sp)
            Text("Free camera rotation, zoom, and pan", color = TX, fontSize = 12.sp)
            Text("Realistic piece materials (Stone, Granite, Marble, Diamond)", color = TX, fontSize = 12.sp)
            Text("6 layouts (Classic, Modern, Fullscreen, Compact, Freeform, 3D)", color = TX, fontSize = 12.sp)
            Text("Freeform editor: drag, resize, snap grid, transparency", color = TX, fontSize = 12.sp)
            Text("5 built-in themes + custom theme editor", color = TX, fontSize = 12.sp)
            Text("General app settings with Auto/Dark/Light mode", color = TX, fontSize = 12.sp)
            Text("Hold piece, Next queue (1-3), Ghost piece", color = TX, fontSize = 12.sp)
            Text("D-Pad and Swipe control styles + Gamepad support", color = TX, fontSize = 12.sp)
            Text("Score history with sort and filter", color = TX, fontSize = 12.sp)
            Text("SRS rotation + wall kicks", color = TX, fontSize = 12.sp)
        } }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ===== REUSABLE =====
@Composable private fun Header(t: String, onBack: () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("←", color = ACC, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(8.dp)); Spacer(Modifier.width(12.dp)); Text(t, color = TX, fontSize = 22.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun MenuItem(t: String, sub: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(CARD).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(t, color = TX, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(sub, color = DIM, fontSize = 12.sp) }; Text("›", color = DIM, fontSize = 20.sp) } }
@Composable private fun Card(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(CARD).padding(16.dp), content = content) }
@Composable private fun Lbl(t: String) { Text(t, color = ACC, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
@Composable private fun Sel(text: String, sel: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(if (sel) ACC.copy(0.15f) else CARD).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(text, color = TX, fontSize = 14.sp, modifier = Modifier.weight(1f)); if (sel) Text("✓", color = ACC, fontWeight = FontWeight.Bold) } }
@Composable private fun Toggle(label: String, v: Boolean, on: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(label, color = TX, fontSize = 14.sp); Switch(v, on, colors = SwitchDefaults.colors(checkedTrackColor = ACC)) } }
@Composable private fun Info(l: String, v: String) { Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) { Text(l, color = DIM, fontSize = 14.sp); Text(v, color = TX, fontSize = 14.sp, fontFamily = FontFamily.Monospace) } }
@Composable private fun ActionCard(text: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ACC.copy(0.15f)).clickable { onClick() }.padding(16.dp), Arrangement.Center) { Text(text, color = ACC, fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
@Composable private fun Chip(text: String, sel: Boolean, onClick: () -> Unit) { Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) ACC.copy(0.2f) else CARD).border(1.dp, if (sel) ACC else DIM.copy(0.3f), RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text(text, color = if (sel) ACC else TX, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) } }
@Composable private fun EditField(value: String, onChange: (String) -> Unit) { BasicTextField(value, onChange, textStyle = TextStyle(color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(ACC), singleLine = true, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF252525)).padding(12.dp)) }
@Composable private fun sliderColors() = SliderDefaults.colors(thumbColor = ACC, activeTrackColor = ACC, inactiveTrackColor = ACC.copy(0.15f))
