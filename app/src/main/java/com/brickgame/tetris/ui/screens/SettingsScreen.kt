package com.brickgame.tetris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.ScoreEntry
import com.brickgame.tetris.game.Difficulty
import com.brickgame.tetris.game.GameMode
import com.brickgame.tetris.ui.layout.DPadStyle
import com.brickgame.tetris.ui.layout.LayoutPreset
import com.brickgame.tetris.ui.styles.AnimationStyle
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes

private val BG = Color(0xFF0D0D0D)
private val CARD = Color(0xFF1A1A1A)
private val ACCENT = Color(0xFFF4D03F)
private val TEXT = Color(0xFFE8E8E8)
private val TEXT_DIM = Color(0xFF888888)

@Composable
fun SettingsScreen(
    page: GameViewModel.SettingsPage,
    currentTheme: GameTheme,
    portraitLayout: LayoutPreset,
    landscapeLayout: LayoutPreset,
    dpadStyle: DPadStyle,
    difficulty: Difficulty,
    gameMode: GameMode,
    ghostEnabled: Boolean,
    animationStyle: AnimationStyle,
    animationDuration: Float,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    playerName: String,
    highScore: Int,
    scoreHistory: List<ScoreEntry>,
    onNavigate: (GameViewModel.SettingsPage) -> Unit,
    onBack: () -> Unit,
    onSetTheme: (GameTheme) -> Unit,
    onSetPortraitLayout: (LayoutPreset) -> Unit,
    onSetLandscapeLayout: (LayoutPreset) -> Unit,
    onSetDPadStyle: (DPadStyle) -> Unit,
    onSetDifficulty: (Difficulty) -> Unit,
    onSetGameMode: (GameMode) -> Unit,
    onSetGhostEnabled: (Boolean) -> Unit,
    onSetAnimationStyle: (AnimationStyle) -> Unit,
    onSetAnimationDuration: (Float) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit,
    onSetPlayerName: (String) -> Unit
) {
    Box(Modifier.fillMaxSize().background(BG)) {
        when (page) {
            GameViewModel.SettingsPage.MAIN -> MainPage(onNavigate, onBack)
            GameViewModel.SettingsPage.PROFILE -> ProfilePage(playerName, highScore, scoreHistory, onSetPlayerName) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.THEME -> ThemePage(currentTheme, onSetTheme) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.LAYOUT -> LayoutPage(portraitLayout, landscapeLayout, dpadStyle, onSetPortraitLayout, onSetLandscapeLayout, onSetDPadStyle) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.GAMEPLAY -> GameplayPage(difficulty, gameMode, ghostEnabled, onSetDifficulty, onSetGameMode, onSetGhostEnabled) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.EXPERIENCE -> ExperiencePage(animationStyle, animationDuration, soundEnabled, vibrationEnabled, onSetAnimationStyle, onSetAnimationDuration, onSetSoundEnabled, onSetVibrationEnabled) { onNavigate(GameViewModel.SettingsPage.MAIN) }
            GameViewModel.SettingsPage.ABOUT -> AboutPage { onNavigate(GameViewModel.SettingsPage.MAIN) }
        }
    }
}

@Composable private fun MainPage(onNav: (GameViewModel.SettingsPage) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Header("Settings", onBack)
        Spacer(Modifier.height(16.dp))
        MenuItem("👤", "Profile", "Name, scores") { onNav(GameViewModel.SettingsPage.PROFILE) }
        MenuItem("🎨", "Theme", "Colours and style") { onNav(GameViewModel.SettingsPage.THEME) }
        MenuItem("📐", "Layout", "Screen arrangement") { onNav(GameViewModel.SettingsPage.LAYOUT) }
        MenuItem("🎮", "Gameplay", "Difficulty, mode") { onNav(GameViewModel.SettingsPage.GAMEPLAY) }
        MenuItem("✨", "Experience", "Animation, sound") { onNav(GameViewModel.SettingsPage.EXPERIENCE) }
        MenuItem("ℹ️", "About", "Version, credits") { onNav(GameViewModel.SettingsPage.ABOUT) }
    }
}

@Composable private fun ProfilePage(name: String, hs: Int, history: List<ScoreEntry>, onName: (String) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Profile", onBack); Spacer(Modifier.height(16.dp)) }
        item { Card { Text("Player Name", color = TEXT_DIM, fontSize = 12.sp); Text(name, color = TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
        item { Card { Text("High Score", color = TEXT_DIM, fontSize = 12.sp); Text(hs.toString(), color = ACCENT, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) } }
        item { Label("Score History") }
        if (history.isEmpty()) { item { Card { Text("No games yet", color = TEXT_DIM) } } }
        items(history.size.coerceAtMost(20)) { i ->
            val e = history[i]
            Card { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${i+1}.", color = TEXT_DIM, fontSize = 14.sp)
                Text("${e.score}", color = ACCENT, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("Lv${e.level}", color = TEXT_DIM, fontSize = 12.sp)
                Text("${e.lines}L", color = TEXT_DIM, fontSize = 12.sp)
            }}
        }
        item { Spacer(Modifier.height(16.dp)); Card { Text("Export / Import — v3.1.0", color = TEXT_DIM, fontSize = 13.sp) } }
    }
}

@Composable private fun ThemePage(current: GameTheme, onSet: (GameTheme) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Theme", onBack); Spacer(Modifier.height(16.dp)) }
        items(GameThemes.allThemes.size) { i ->
            val t = GameThemes.allThemes[i]; val sel = t.id == current.id
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) ACCENT.copy(alpha = 0.15f) else CARD)
                    .clickable { onSet(t) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(t.screenBackground))
                    Box(Modifier.size(24.dp).clip(CircleShape).background(t.pixelOn))
                    Box(Modifier.size(24.dp).clip(CircleShape).background(t.buttonPrimary))
                }
                Spacer(Modifier.width(16.dp))
                Text(t.name, color = TEXT, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (sel) Text("✓", color = ACCENT, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(16.dp)); Card { Text("+ Custom Theme Builder — v3.2.0", color = TEXT_DIM, fontSize = 13.sp) } }
    }
}

@Composable private fun LayoutPage(
    portrait: LayoutPreset, landscape: LayoutPreset, dpad: DPadStyle,
    onP: (LayoutPreset) -> Unit, onL: (LayoutPreset) -> Unit, onD: (DPadStyle) -> Unit, onBack: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Layout", onBack) }
        item { Label("Portrait") }
        items(LayoutPreset.portraitPresets().size) { i -> val p = LayoutPreset.portraitPresets()[i]; Sel(p.displayName, p == portrait) { onP(p) } }
        item { Label("Landscape") }
        items(LayoutPreset.landscapePresets().size) { i -> val p = LayoutPreset.landscapePresets()[i]; Sel(p.displayName, p == landscape) { onL(p) } }
        item { Label("D-Pad Style") }
        items(DPadStyle.entries.size) { i -> val s = DPadStyle.entries[i]; Sel(s.displayName, s == dpad) { onD(s) } }
        item { Spacer(Modifier.height(16.dp)); Card { Text("+ Custom Layout Editor — v3.3.0", color = TEXT_DIM, fontSize = 13.sp) } }
    }
}

@Composable private fun GameplayPage(
    diff: Difficulty, mode: GameMode, ghost: Boolean,
    onD: (Difficulty) -> Unit, onM: (GameMode) -> Unit, onG: (Boolean) -> Unit, onBack: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Gameplay", onBack) }
        item { Label("Difficulty") }
        items(Difficulty.entries.size) { i -> val d = Difficulty.entries[i]; Sel("${d.displayName} — ${d.description}", d == diff) { onD(d) } }
        item { Label("Game Mode") }
        items(GameMode.entries.size) { i -> val m = GameMode.entries[i]; Sel("${m.displayName} — ${m.description}", m == mode) { onM(m) } }
        item { Label("Options") }
        item { Card { Toggle("Ghost Piece", ghost, onG) } }
    }
}

@Composable private fun ExperiencePage(
    aStyle: AnimationStyle, aDur: Float, sound: Boolean, vib: Boolean,
    onAS: (AnimationStyle) -> Unit, onAD: (Float) -> Unit, onS: (Boolean) -> Unit, onV: (Boolean) -> Unit, onBack: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Header("Experience", onBack) }
        item { Label("Animation") }
        items(AnimationStyle.entries.size) { i -> val s = AnimationStyle.entries[i]; Sel("${s.displayName} — ${s.description}", s == aStyle) { onAS(s) } }
        item { Card {
            Text("Speed", color = TEXT, fontSize = 14.sp)
            Slider(value = aDur, onValueChange = onAD, valueRange = 0.1f..2f,
                colors = SliderDefaults.colors(thumbColor = ACCENT, activeTrackColor = ACCENT))
            Text("${(aDur * 1000).toInt()}ms", color = TEXT_DIM, fontSize = 12.sp)
        }}
        item { Label("Sound & Vibration") }
        item { Card { Toggle("Sound", sound, onS); Spacer(Modifier.height(8.dp)); Toggle("Vibration", vib, onV) } }
        item { Spacer(Modifier.height(16.dp)); Card { Text("✨ Visual Evolution — v3.4.0", color = TEXT_DIM, fontSize = 13.sp) } }
    }
}

@Composable private fun AboutPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Header("About", onBack); Spacer(Modifier.height(24.dp))
        Card {
            Text("BRICK GAME", color = ACCENT, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            Text("Kotlin Edition", color = TEXT, fontSize = 16.sp)
        }
        Card { Info("Version", "3.0.0"); Info("Build", "10"); Info("Min Android", "8.0 (API 26)") }
        Card { Text("Developer", color = TEXT_DIM, fontSize = 12.sp); Text("Andrei Anton", color = TEXT, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Card {
            Text("v3.0.0 — Fresh start", color = ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Core engine (SRS, T-Spin, B2B, Combos)", color = TEXT, fontSize = 12.sp)
            Text("5 themes, 5 layouts, 2 D-Pad styles", color = TEXT, fontSize = 12.sp)
            Text("Full settings with profile & experience", color = TEXT, fontSize = 12.sp)
        }
    }
}

// ===== Reusable =====
@Composable private fun Header(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("←", color = ACCENT, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(8.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = TEXT, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun MenuItem(icon: String, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(CARD).clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp); Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) { Text(title, color = TEXT, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(sub, color = TEXT_DIM, fontSize = 12.sp) }
        Text("›", color = TEXT_DIM, fontSize = 20.sp)
    }
}

@Composable private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(CARD).padding(16.dp), content = content)
}

@Composable private fun Label(text: String) { Text(text, color = ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }

@Composable private fun Sel(text: String, sel: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp))
            .background(if (sel) ACCENT.copy(alpha = 0.15f) else CARD).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = TEXT, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (sel) Text("✓", color = ACCENT, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Toggle(label: String, v: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = TEXT, fontSize = 16.sp)
        Switch(checked = v, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = ACCENT))
    }
}

@Composable private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
        Text(label, color = TEXT_DIM, fontSize = 14.sp); Text(value, color = TEXT, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}
