package com.brickgame.tetris.ui.screens

import androidx.compose.animation.*
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

// ===== Colour Constants =====
private val BG = Color(0xFF0D0D0D)
private val CARD = Color(0xFF1A1A1A)
private val CARD_HOVER = Color(0xFF252525)
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
    Box(
        modifier = Modifier.fillMaxSize().background(BG)
    ) {
        when (page) {
            GameViewModel.SettingsPage.MAIN -> MainSettingsPage(onNavigate, onBack)
            GameViewModel.SettingsPage.PROFILE -> ProfilePage(playerName, highScore, scoreHistory, onSetPlayerName, { onNavigate(GameViewModel.SettingsPage.MAIN) })
            GameViewModel.SettingsPage.THEME -> ThemePage(currentTheme, onSetTheme, { onNavigate(GameViewModel.SettingsPage.MAIN) })
            GameViewModel.SettingsPage.LAYOUT -> LayoutPage(portraitLayout, landscapeLayout, dpadStyle, onSetPortraitLayout, onSetLandscapeLayout, onSetDPadStyle, { onNavigate(GameViewModel.SettingsPage.MAIN) })
            GameViewModel.SettingsPage.GAMEPLAY -> GameplayPage(difficulty, gameMode, ghostEnabled, onSetDifficulty, onSetGameMode, onSetGhostEnabled, { onNavigate(GameViewModel.SettingsPage.MAIN) })
            GameViewModel.SettingsPage.EXPERIENCE -> ExperiencePage(animationStyle, animationDuration, soundEnabled, vibrationEnabled, onSetAnimationStyle, onSetAnimationDuration, onSetSoundEnabled, onSetVibrationEnabled, { onNavigate(GameViewModel.SettingsPage.MAIN) })
            GameViewModel.SettingsPage.ABOUT -> AboutPage { onNavigate(GameViewModel.SettingsPage.MAIN) }
        }
    }
}

// ===== Main Settings Page =====

@Composable
private fun MainSettingsPage(
    onNavigate: (GameViewModel.SettingsPage) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Settings", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        SettingsMenuItem("👤", "Profile", "Name, scores, export/import") { onNavigate(GameViewModel.SettingsPage.PROFILE) }
        SettingsMenuItem("🎨", "Theme", "Colours and visual style") { onNavigate(GameViewModel.SettingsPage.THEME) }
        SettingsMenuItem("📐", "Layout", "Screen arrangement") { onNavigate(GameViewModel.SettingsPage.LAYOUT) }
        SettingsMenuItem("🎮", "Gameplay", "Difficulty, mode, controls") { onNavigate(GameViewModel.SettingsPage.GAMEPLAY) }
        SettingsMenuItem("✨", "Experience", "Animation, sound, vibration") { onNavigate(GameViewModel.SettingsPage.EXPERIENCE) }
        SettingsMenuItem("ℹ️", "About", "Version, credits") { onNavigate(GameViewModel.SettingsPage.ABOUT) }
    }
}

// ===== Profile Page =====

@Composable
private fun ProfilePage(
    playerName: String,
    highScore: Int,
    scoreHistory: List<ScoreEntry>,
    onSetPlayerName: (String) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        item { PageHeader("Profile", onBack = onBack) }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            SettingsCard {
                Text("Player Name", color = TEXT_DIM, fontSize = 12.sp)
                Text(playerName, color = TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            SettingsCard {
                Text("High Score", color = TEXT_DIM, fontSize = 12.sp)
                Text(highScore.toString(), color = ACCENT, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        item {
            SectionLabel("Score History")
        }
        if (scoreHistory.isEmpty()) {
            item {
                SettingsCard { Text("No games played yet", color = TEXT_DIM) }
            }
        }
        items(scoreHistory.size.coerceAtMost(20)) { i ->
            val entry = scoreHistory[i]
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${i + 1}.", color = TEXT_DIM, fontSize = 14.sp)
                    Text("${entry.score}", color = ACCENT, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold)
                    Text("Lv${entry.level}", color = TEXT_DIM, fontSize = 12.sp)
                    Text("${entry.lines}L", color = TEXT_DIM, fontSize = 12.sp)
                }
            }
        }
        // Export/Import placeholder for v3.1
        item { Spacer(Modifier.height(16.dp)) }
        item {
            SettingsCard {
                Text("Export / Import", color = TEXT_DIM, fontSize = 14.sp)
                Text("Coming in v3.1.0", color = TEXT_DIM, fontSize = 12.sp)
            }
        }
    }
}

// ===== Theme Page =====

@Composable
private fun ThemePage(
    currentTheme: GameTheme,
    onSetTheme: (GameTheme) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        item { PageHeader("Theme", onBack = onBack) }
        item { Spacer(Modifier.height(16.dp)) }

        items(GameThemes.allThemes.size) { i ->
            val theme = GameThemes.allThemes[i]
            val isSelected = theme.id == currentTheme.id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) ACCENT.copy(alpha = 0.15f) else CARD)
                    .clickable { onSetTheme(theme) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colour preview
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(theme.screenBackground))
                    Box(Modifier.size(24.dp).clip(CircleShape).background(theme.pixelOn))
                    Box(Modifier.size(24.dp).clip(CircleShape).background(theme.buttonPrimary))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(theme.name, color = TEXT, fontWeight = FontWeight.Bold)
                }
                if (isSelected) {
                    Text("✓", color = ACCENT, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Custom theme builder placeholder for v3.2
        item { Spacer(Modifier.height(16.dp)) }
        item {
            SettingsCard {
                Text("+ Create Custom Theme", color = TEXT_DIM, fontSize = 14.sp)
                Text("Coming in v3.2.0", color = TEXT_DIM, fontSize = 12.sp)
            }
        }
    }
}

// ===== Layout Page =====

@Composable
private fun LayoutPage(
    portraitLayout: LayoutPreset,
    landscapeLayout: LayoutPreset,
    dpadStyle: DPadStyle,
    onSetPortrait: (LayoutPreset) -> Unit,
    onSetLandscape: (LayoutPreset) -> Unit,
    onSetDPadStyle: (DPadStyle) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        item { PageHeader("Layout", onBack = onBack) }

        item { SectionLabel("Portrait") }
        items(LayoutPreset.portraitPresets().size) { i ->
            val preset = LayoutPreset.portraitPresets()[i]
            SelectableRow(preset.displayName, preset == portraitLayout) { onSetPortrait(preset) }
        }

        item { SectionLabel("Landscape") }
        items(LayoutPreset.landscapePresets().size) { i ->
            val preset = LayoutPreset.landscapePresets()[i]
            SelectableRow(preset.displayName, preset == landscapeLayout) { onSetLandscape(preset) }
        }

        item { SectionLabel("D-Pad Style") }
        items(DPadStyle.entries.size) { i ->
            val style = DPadStyle.entries[i]
            SelectableRow(style.displayName, style == dpadStyle) { onSetDPadStyle(style) }
        }

        // Custom layout editor placeholder for v3.3
        item { Spacer(Modifier.height(16.dp)) }
        item {
            SettingsCard {
                Text("+ Custom Layout Editor", color = TEXT_DIM, fontSize = 14.sp)
                Text("Coming in v3.3.0", color = TEXT_DIM, fontSize = 12.sp)
            }
        }
    }
}

// ===== Gameplay Page =====

@Composable
private fun GameplayPage(
    difficulty: Difficulty,
    gameMode: GameMode,
    ghostEnabled: Boolean,
    onSetDifficulty: (Difficulty) -> Unit,
    onSetGameMode: (GameMode) -> Unit,
    onSetGhostEnabled: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        item { PageHeader("Gameplay", onBack = onBack) }

        item { SectionLabel("Difficulty") }
        items(Difficulty.entries.size) { i ->
            val diff = Difficulty.entries[i]
            SelectableRow(
                "${diff.displayName} — ${diff.description}",
                diff == difficulty
            ) { onSetDifficulty(diff) }
        }

        item { SectionLabel("Game Mode") }
        items(GameMode.entries.size) { i ->
            val mode = GameMode.entries[i]
            SelectableRow(
                "${mode.displayName} — ${mode.description}",
                mode == gameMode
            ) { onSetGameMode(mode) }
        }

        item { SectionLabel("Options") }
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ghost Piece", color = TEXT, fontSize = 16.sp)
                    Switch(
                        checked = ghostEnabled,
                        onCheckedChange = onSetGhostEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = ACCENT)
                    )
                }
            }
        }
    }
}

// ===== Experience Page =====

@Composable
private fun ExperiencePage(
    animationStyle: AnimationStyle,
    animationDuration: Float,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSetAnimationStyle: (AnimationStyle) -> Unit,
    onSetAnimationDuration: (Float) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        item { PageHeader("Experience", onBack = onBack) }

        item { SectionLabel("Animation") }
        items(AnimationStyle.entries.size) { i ->
            val style = AnimationStyle.entries[i]
            SelectableRow(
                "${style.displayName} — ${style.description}",
                style == animationStyle
            ) { onSetAnimationStyle(style) }
        }
        item {
            SettingsCard {
                Text("Animation Speed", color = TEXT, fontSize = 14.sp)
                Slider(
                    value = animationDuration,
                    onValueChange = onSetAnimationDuration,
                    valueRange = 0.1f..2f,
                    colors = SliderDefaults.colors(thumbColor = ACCENT, activeTrackColor = ACCENT)
                )
                Text("${(animationDuration * 1000).toInt()}ms", color = TEXT_DIM, fontSize = 12.sp)
            }
        }

        item { SectionLabel("Sound & Vibration") }
        item {
            SettingsCard {
                ToggleRow("Sound", soundEnabled, onSetSoundEnabled)
                Spacer(Modifier.height(8.dp))
                ToggleRow("Vibration", vibrationEnabled, onSetVibrationEnabled)
            }
        }

        // Visual evolution placeholder for v3.4
        item { Spacer(Modifier.height(16.dp)) }
        item {
            SettingsCard {
                Text("✨ Visual Evolution", color = TEXT_DIM, fontSize = 14.sp)
                Text("Coming in v3.4.0", color = TEXT_DIM, fontSize = 12.sp)
            }
        }
    }
}

// ===== About Page =====

@Composable
private fun AboutPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("About", onBack = onBack)
        Spacer(Modifier.height(24.dp))

        SettingsCard {
            Text("BRICK GAME", color = ACCENT, fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            Text("Kotlin Edition", color = TEXT, fontSize = 16.sp)
        }

        SettingsCard {
            InfoRow("Version", "3.0.0")
            InfoRow("Build", "10")
            InfoRow("Min Android", "8.0 (API 26)")
            InfoRow("Architecture", "MVVM + Compose")
        }

        SettingsCard {
            Text("Developer", color = TEXT_DIM, fontSize = 12.sp)
            Text("Andrei Anton", color = TEXT, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        SettingsCard {
            Text("Changelog", color = TEXT_DIM, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("v3.0.0 — Fresh start", color = ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("• Core Tetris engine (SRS, T-Spin, B2B, Combos)", color = TEXT, fontSize = 12.sp)
            Text("• 5 built-in themes", color = TEXT, fontSize = 12.sp)
            Text("• 5 fixed layouts (3 portrait + 2 landscape)", color = TEXT, fontSize = 12.sp)
            Text("• Both D-Pad styles (Standard + Rotate Centre)", color = TEXT, fontSize = 12.sp)
            Text("• Full settings with profile, gameplay, experience", color = TEXT, fontSize = 12.sp)
        }
    }
}

// ===== Reusable Components =====

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("←", color = ACCENT, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onBack() }.padding(8.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = TEXT, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsMenuItem(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CARD)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TEXT, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = TEXT_DIM, fontSize = 12.sp)
        }
        Text("›", color = TEXT_DIM, fontSize = 20.sp)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CARD)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = ACCENT, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun SelectableRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ACCENT.copy(alpha = 0.15f) else CARD)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = TEXT, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (isSelected) Text("✓", color = ACCENT, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleRow(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TEXT, fontSize = 16.sp)
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = ACCENT)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TEXT_DIM, fontSize = 14.sp)
        Text(value, color = TEXT, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}
