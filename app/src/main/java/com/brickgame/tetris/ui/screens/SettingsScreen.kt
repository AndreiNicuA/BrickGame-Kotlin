package com.brickgame.tetris.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.ScoreEntry
import com.brickgame.tetris.game.Difficulty
import com.brickgame.tetris.ui.styles.*
import com.brickgame.tetris.ui.theme.GameThemes
import java.text.SimpleDateFormat
import java.util.*

private val AccentColor = Color(0xFFF4D03F)
private val CardBackground = Color(0xFF1A1A1A)
private val CardBackgroundSelected = Color(0xFF252518)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF888888)
private val TextTertiary = Color(0xFF555555)

private val TitleSize = 32.sp
private val HeaderSize = 24.sp
private val BodySize = 18.sp
private val LabelSize = 14.sp
private val SmallSize = 12.sp

private const val APP_VERSION = "3.0.0"

enum class SettingsPage { MAIN, PROFILE, THEMES, LAYOUT, GAMEPLAY, FEEDBACK, STYLES, ABOUT }

@Composable
fun SettingsScreen(
    currentThemeName: String, layoutMode: LayoutMode,
    landscapeMode: LandscapeMode,
    vibrationEnabled: Boolean, vibrationIntensity: Float, vibrationStyle: VibrationStyle,
    soundEnabled: Boolean, soundVolume: Float, soundStyle: SoundStyle,
    animationEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    stylePreset: StylePreset, ghostPieceEnabled: Boolean, difficulty: Difficulty,
    playerName: String, highScore: Int, scoreHistory: List<ScoreEntry>,
    onThemeChange: (String) -> Unit, onLayoutModeChange: (LayoutMode) -> Unit,
    onLandscapeModeChange: (LandscapeMode) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit, onVibrationIntensityChange: (Float) -> Unit, onVibrationStyleChange: (VibrationStyle) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit, onSoundVolumeChange: (Float) -> Unit, onSoundStyleChange: (SoundStyle) -> Unit,
    onAnimationEnabledChange: (Boolean) -> Unit, onAnimationStyleChange: (AnimationStyle) -> Unit, onAnimationDurationChange: (Float) -> Unit,
    onStylePresetChange: (StylePreset) -> Unit,
    onGhostPieceChange: (Boolean) -> Unit, onDifficultyChange: (Difficulty) -> Unit,
    onPlayerNameChange: (String) -> Unit, onClearHistory: () -> Unit, onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        AnimatedContent(targetState = currentPage, transitionSpec = {
            if (targetState == SettingsPage.MAIN) slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            else slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
        }, label = "page") { page ->
            when (page) {
                SettingsPage.MAIN -> MainPage({ currentPage = it }, onClose)
                SettingsPage.PROFILE -> ProfilePage(playerName, highScore, scoreHistory, onPlayerNameChange, onClearHistory) { currentPage = SettingsPage.MAIN }
                SettingsPage.THEMES -> ThemesPage(currentThemeName, onThemeChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.LAYOUT -> LayoutPage(layoutMode, landscapeMode, onLayoutModeChange, onLandscapeModeChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.GAMEPLAY -> GameplayPage(ghostPieceEnabled, difficulty, onGhostPieceChange, onDifficultyChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.FEEDBACK -> FeedbackPage(vibrationEnabled, vibrationIntensity, soundEnabled, soundVolume, onVibrationEnabledChange, onVibrationIntensityChange, onSoundEnabledChange, onSoundVolumeChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.STYLES -> StylesPage(stylePreset, animationEnabled, animationStyle, animationDuration, vibrationStyle, soundStyle, onStylePresetChange, onAnimationEnabledChange, onAnimationStyleChange, onAnimationDurationChange, onVibrationStyleChange, onSoundStyleChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.ABOUT -> AboutPage { currentPage = SettingsPage.MAIN }
            }
        }
    }
}

// ── MAIN ──

@Composable
private fun MainPage(onNavigate: (SettingsPage) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Settings", fontSize = TitleSize, fontWeight = FontWeight.Light, color = TextPrimary)
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) { Text("×", fontSize = 32.sp, color = TextSecondary) }
        }
        Spacer(Modifier.height(36.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MenuRow("Profile", "Name and scores") { onNavigate(SettingsPage.PROFILE) }
            MenuRow("Theme", "Colours") { onNavigate(SettingsPage.THEMES) }
            MenuRow("Layout", "Screen arrangement") { onNavigate(SettingsPage.LAYOUT) }
            MenuRow("Gameplay", "Difficulty and options") { onNavigate(SettingsPage.GAMEPLAY) }
            MenuRow("Experience", "Animation and effects") { onNavigate(SettingsPage.STYLES) }
            MenuRow("Feedback", "Sound and vibration") { onNavigate(SettingsPage.FEEDBACK) }
            Spacer(Modifier.height(20.dp))
            Divider(color = Color(0xFF222222))
            Spacer(Modifier.height(20.dp))
            MenuRow("About", "Version $APP_VERSION") { onNavigate(SettingsPage.ABOUT) }
        }
    }
}

// ── LAYOUT ──

@Composable
private fun LayoutPage(layoutMode: LayoutMode, landscapeMode: LandscapeMode,
                       onLayoutModeChange: (LayoutMode) -> Unit,
                       onLandscapeModeChange: (LandscapeMode) -> Unit,
                       onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Layout", onBack)
        Spacer(Modifier.height(24.dp))

        Text("PORTRAIT", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableRow("Classic", "Device frame with LCD screen", layoutMode == LayoutMode.CLASSIC) { onLayoutModeChange(LayoutMode.CLASSIC) }
            SelectableRow("Modern", "Clean status bar, large board", layoutMode == LayoutMode.MODERN) { onLayoutModeChange(LayoutMode.MODERN) }
            SelectableRow("Fullscreen", "Maximum game area", layoutMode == LayoutMode.FULLSCREEN) { onLayoutModeChange(LayoutMode.FULLSCREEN) }
        }

        Spacer(Modifier.height(24.dp))
        Text("LANDSCAPE", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableRow("Default", "Controls left, info right", landscapeMode == LandscapeMode.DEFAULT) { onLandscapeModeChange(LandscapeMode.DEFAULT) }
            SelectableRow("Lefty", "Info left, controls right", landscapeMode == LandscapeMode.LEFTY) { onLandscapeModeChange(LandscapeMode.LEFTY) }
        }

        Spacer(Modifier.height(20.dp))
        Text("Custom layouts coming in a future update.", fontSize = SmallSize, color = TextTertiary)
    }
}

// ── ABOUT ──

@Composable
private fun AboutPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("About", onBack)
        Spacer(Modifier.height(56.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Brick Game", fontSize = 32.sp, fontWeight = FontWeight.Light, color = TextPrimary, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            Text(APP_VERSION, fontSize = BodySize, color = AccentColor, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text("Kotlin Edition", fontSize = LabelSize, color = TextSecondary)
            Spacer(Modifier.height(56.dp))
            Text("Developed by", fontSize = LabelSize, color = TextTertiary)
            Text("Andrei Anton", fontSize = BodySize, color = TextPrimary)
            Spacer(Modifier.height(40.dp))
            Text("Built with Kotlin & Jetpack Compose", fontSize = LabelSize, color = TextTertiary)

            Spacer(Modifier.height(48.dp))
            Text("CHANGELOG", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            ChangelogEntry("3.0.0", "Stabilised layouts, grouped buttons, popup notifications, clean architecture")
            ChangelogEntry("2.0.0", "SRS, Hold, T-Spin, B2B, Combos, Game Modes, Landscape")
            ChangelogEntry("1.0.0", "Original release — Classic Tetris")
        }
    }
}

@Composable
private fun ChangelogEntry(version: String, description: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(version, fontSize = SmallSize, color = AccentColor, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp))
        Text(description, fontSize = SmallSize, color = TextTertiary)
    }
}

// ── All existing pages preserved ──

@Composable
private fun GameplayPage(ghostPieceEnabled: Boolean, difficulty: Difficulty, onGhostPieceChange: (Boolean) -> Unit, onDifficultyChange: (Difficulty) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader("Gameplay", onBack) }
        item { Spacer(Modifier.height(16.dp)); SectionTitle("Difficulty") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Difficulty.entries.forEach { diff ->
                    SelectableRow(diff.displayName, diff.description, difficulty == diff) { onDifficultyChange(diff) }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)); SectionTitle("Options") }
        item { SettingCard { SettingToggle("Ghost Piece", "Shows where piece lands", ghostPieceEnabled, onGhostPieceChange) } }
    }
}

@Composable
private fun StylesPage(
    stylePreset: StylePreset, animationEnabled: Boolean, animationStyle: AnimationStyle, animationDuration: Float,
    vibrationStyle: VibrationStyle, soundStyle: SoundStyle,
    onStylePresetChange: (StylePreset) -> Unit, onAnimationEnabledChange: (Boolean) -> Unit,
    onAnimationStyleChange: (AnimationStyle) -> Unit, onAnimationDurationChange: (Float) -> Unit,
    onVibrationStyleChange: (VibrationStyle) -> Unit, onSoundStyleChange: (SoundStyle) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader("Experience", onBack) }
        item { SectionTitle("Animation") }
        item {
            SettingCard {
                Column {
                    SettingToggle("Line Clear Animation", "Visual effects when clearing lines", animationEnabled, onAnimationEnabledChange)
                    if (animationEnabled) {
                        Spacer(Modifier.height(20.dp))
                        Column {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Duration", fontSize = LabelSize, color = TextTertiary)
                                Text("${(animationDuration * 1000).toInt()}ms", fontSize = LabelSize, color = AccentColor)
                            }
                            Slider(value = animationDuration, onValueChange = onAnimationDurationChange,
                                valueRange = 0.1f..2f,
                                colors = SliderDefaults.colors(thumbColor = AccentColor, activeTrackColor = AccentColor, inactiveTrackColor = Color(0xFF333333)))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Style", fontSize = LabelSize, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        AnimationStyle.entries.filter { it != AnimationStyle.NONE }.forEach { style ->
                            CompactOption(style.displayName, style.description, animationStyle == style) { onAnimationStyleChange(style) }
                        }
                    }
                }
            }
        }
        item { SectionTitle("Quick Presets") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StylePreset.entries.filter { it != StylePreset.CUSTOM }.forEach { preset ->
                    SelectableRow(preset.displayName.replace(Regex("^[^ ]+ "), ""), preset.description, stylePreset == preset) { onStylePresetChange(preset) }
                }
            }
        }
        item { SectionTitle("Custom Patterns"); Text("Selecting below switches to custom", fontSize = SmallSize, color = TextTertiary) }
        item { SettingCard { Column { Text("Vibration Pattern", fontSize = LabelSize, color = TextSecondary); Spacer(Modifier.height(8.dp)); VibrationStyle.entries.forEach { CompactOption(it.displayName, it.description, vibrationStyle == it) { onVibrationStyleChange(it) } } } } }
        item { SettingCard { Column { Text("Sound Style", fontSize = LabelSize, color = TextSecondary); Spacer(Modifier.height(8.dp)); SoundStyle.entries.forEach { CompactOption(it.displayName, it.description, soundStyle == it) { onSoundStyleChange(it) } } } } }
    }
}

@Composable
private fun FeedbackPage(vibrationEnabled: Boolean, vibrationIntensity: Float, soundEnabled: Boolean, soundVolume: Float, onVibrationEnabledChange: (Boolean) -> Unit, onVibrationIntensityChange: (Float) -> Unit, onSoundEnabledChange: (Boolean) -> Unit, onSoundVolumeChange: (Float) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Feedback", onBack)
        Spacer(Modifier.height(32.dp))
        SettingCard { Column { SettingToggle("Vibration", "Haptic feedback", vibrationEnabled, onVibrationEnabledChange); if (vibrationEnabled) { Spacer(Modifier.height(20.dp)); IntensitySlider("Intensity", vibrationIntensity, onVibrationIntensityChange) } } }
        Spacer(Modifier.height(16.dp))
        SettingCard { Column { SettingToggle("Sound", "Audio effects", soundEnabled, onSoundEnabledChange); if (soundEnabled) { Spacer(Modifier.height(20.dp)); IntensitySlider("Volume", soundVolume, onSoundVolumeChange) } } }
    }
}

@Composable
private fun ProfilePage(playerName: String, highScore: Int, scoreHistory: List<ScoreEntry>, onPlayerNameChange: (String) -> Unit, onClearHistory: () -> Unit, onBack: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(playerName) }
    var showClear by remember { mutableStateOf(false) }
    val sortedHistory = remember(scoreHistory) { scoreHistory.sortedByDescending { it.score } }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Profile", onBack)
        Spacer(Modifier.height(32.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SettingCard {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        if (editing) {
                            BasicTextField(nameInput, { nameInput = it.take(20) }, textStyle = TextStyle(fontSize = BodySize, color = TextPrimary), cursorBrush = SolidColor(AccentColor), modifier = Modifier.weight(1f))
                            TextButton({ if (nameInput.isNotBlank()) onPlayerNameChange(nameInput); editing = false }) { Text("Save", color = AccentColor, fontSize = BodySize) }
                        } else {
                            Column { Text("Name", fontSize = LabelSize, color = TextTertiary); Text(playerName, fontSize = BodySize, color = TextPrimary) }
                            TextButton({ nameInput = playerName; editing = true }) { Text("Edit", color = AccentColor, fontSize = LabelSize) }
                        }
                    }
                }
            }
            item { SettingCard { Column { Text("High Score", fontSize = LabelSize, color = TextTertiary); Text(highScore.toString().padStart(6, '0'), fontSize = 40.sp, fontWeight = FontWeight.Light, color = AccentColor) } } }
            item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("History (by score)", fontSize = BodySize, color = TextSecondary); if (sortedHistory.isNotEmpty()) TextButton({ showClear = true }) { Text("Clear", color = TextTertiary, fontSize = LabelSize) } } }
            if (sortedHistory.isEmpty()) item { Text("No games yet", fontSize = LabelSize, color = TextTertiary, modifier = Modifier.padding(vertical = 24.dp)) }
            else itemsIndexed(sortedHistory.take(15)) { index, entry -> ScoreRowEntry(index + 1, entry, entry.score == highScore) }
        }
    }
    if (showClear) AlertDialog(onDismissRequest = { showClear = false }, title = { Text("Clear History?", color = TextPrimary) }, text = { Text("This cannot be undone.", color = TextSecondary) }, confirmButton = { TextButton({ onClearHistory(); showClear = false }) { Text("Clear", color = Color(0xFFE57373)) } }, dismissButton = { TextButton({ showClear = false }) { Text("Cancel", color = TextSecondary) } }, containerColor = CardBackground)
}

@Composable
private fun ThemesPage(currentThemeName: String, onThemeChange: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Theme", onBack)
        Spacer(Modifier.height(32.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(GameThemes.allThemes.size) { i ->
                val theme = GameThemes.allThemes[i]
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (theme.name == currentThemeName) CardBackgroundSelected else CardBackground).clickable { onThemeChange(theme.name) }.padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground)); Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.pixelOn)); Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.buttonPrimary)) }
                        Text(theme.name, fontSize = BodySize, color = TextPrimary)
                    }
                    if (theme.name == currentThemeName) Text("✓", fontSize = 20.sp, color = AccentColor)
                }
            }
        }
    }
}

// ── SHARED COMPONENTS ──

@Composable private fun MenuRow(title: String, subtitle: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column { Text(title, fontSize = BodySize, color = TextPrimary); Text(subtitle, fontSize = LabelSize, color = TextTertiary) }; Text("›", fontSize = 24.sp, color = TextTertiary) } }
@Composable private fun PageHeader(title: String, onBack: () -> Unit) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) { Text("‹", fontSize = 28.sp, color = TextSecondary) }; Spacer(Modifier.width(14.dp)); Text(title, fontSize = HeaderSize, fontWeight = FontWeight.Light, color = TextPrimary) } }
@Composable private fun SectionTitle(title: String) { Text(title, fontSize = LabelSize, fontWeight = FontWeight.Medium, color = AccentColor, letterSpacing = 1.sp) }
@Composable private fun SettingCard(content: @Composable () -> Unit) { Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardBackground).padding(18.dp)) { content() } }
@Composable private fun SettingToggle(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = BodySize, color = TextPrimary); Text(desc, fontSize = LabelSize, color = TextTertiary) }; Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = AccentColor, checkedTrackColor = AccentColor.copy(0.4f), uncheckedThumbColor = TextTertiary, uncheckedTrackColor = Color(0xFF333333))) } }
@Composable private fun IntensitySlider(label: String, value: Float, onChange: (Float) -> Unit) { Column { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(label, fontSize = LabelSize, color = TextTertiary); Text("${(value * 100).toInt()}%", fontSize = LabelSize, color = AccentColor) }; Slider(value, onChange, colors = SliderDefaults.colors(thumbColor = AccentColor, activeTrackColor = AccentColor, inactiveTrackColor = Color(0xFF333333))) } }
@Composable private fun SelectableRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) CardBackgroundSelected else CardBackground).clickable(onClick = onClick).padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = BodySize, color = TextPrimary); Text(subtitle, fontSize = LabelSize, color = TextTertiary) }; if (selected) Text("✓", fontSize = 20.sp, color = AccentColor) } }
@Composable private fun CompactOption(name: String, desc: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (selected) Color(0xFF252520) else Color.Transparent).clickable(onClick = onClick).padding(14.dp, 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(name, fontSize = LabelSize, color = if (selected) TextPrimary else TextSecondary); Text(desc, fontSize = SmallSize, color = TextTertiary) }; RadioButton(selected, onClick, colors = RadioButtonDefaults.colors(selectedColor = AccentColor, unselectedColor = TextTertiary), modifier = Modifier.size(24.dp)) } }
@Composable private fun ScoreRowEntry(rank: Int, entry: ScoreEntry, isHigh: Boolean) { val fmt = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }; val rowColor = if (isHigh) AccentColor.copy(alpha = 0.1f) else Color.Transparent; Row(Modifier.fillMaxWidth().background(rowColor).padding(vertical = 10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Text("#$rank", fontSize = LabelSize, color = if (rank <= 3) AccentColor else TextTertiary, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp)); Column { Text(entry.playerName, fontSize = LabelSize, color = TextPrimary); Text("${entry.score} • Lv.${entry.level}", fontSize = SmallSize, color = TextTertiary) } }; Text(fmt.format(Date(entry.timestamp)), fontSize = SmallSize, color = TextTertiary) } }
