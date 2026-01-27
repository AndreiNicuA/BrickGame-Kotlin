package com.brickgame.tetris.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.ScoreEntry
import com.brickgame.tetris.ui.styles.*
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes
import java.text.SimpleDateFormat
import java.util.*

private val AccentColor = Color(0xFFF4D03F)
private val CardBackground = Color(0xFF1A1A1A)
private val CardBackgroundSelected = Color(0xFF252518)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF888888)
private val TextTertiary = Color(0xFF555555)

enum class SettingsPage {
    MAIN, PROFILE, THEMES, LAYOUT, GAMEPLAY, FEEDBACK, STYLES, ABOUT
}

@Composable
fun SettingsScreen(
    currentThemeName: String,
    layoutMode: LayoutMode,
    vibrationEnabled: Boolean,
    vibrationIntensity: Float,
    vibrationStyle: VibrationStyle,
    soundEnabled: Boolean,
    soundVolume: Float,
    soundStyle: SoundStyle,
    animationStyle: AnimationStyle,
    stylePreset: StylePreset,
    ghostPieceEnabled: Boolean,
    playerName: String,
    highScore: Int,
    scoreHistory: List<ScoreEntry>,
    onThemeChange: (String) -> Unit,
    onLayoutModeChange: (LayoutMode) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onVibrationIntensityChange: (Float) -> Unit,
    onVibrationStyleChange: (VibrationStyle) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onSoundStyleChange: (SoundStyle) -> Unit,
    onAnimationStyleChange: (AnimationStyle) -> Unit,
    onStylePresetChange: (StylePreset) -> Unit,
    onGhostPieceChange: (Boolean) -> Unit,
    onPlayerNameChange: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == SettingsPage.MAIN) {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                } else {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                }
            },
            label = "page"
        ) { page ->
            when (page) {
                SettingsPage.MAIN -> MainPage(onNavigate = { currentPage = it }, onClose = onClose)
                SettingsPage.PROFILE -> ProfilePage(playerName, highScore, scoreHistory, onPlayerNameChange, onClearHistory) { currentPage = SettingsPage.MAIN }
                SettingsPage.THEMES -> ThemesPage(currentThemeName, onThemeChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.LAYOUT -> LayoutPage(layoutMode, onLayoutModeChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.GAMEPLAY -> GameplayPage(ghostPieceEnabled, onGhostPieceChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.FEEDBACK -> FeedbackPage(vibrationEnabled, vibrationIntensity, soundEnabled, soundVolume, onVibrationEnabledChange, onVibrationIntensityChange, onSoundEnabledChange, onSoundVolumeChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.STYLES -> StylesPage(stylePreset, animationStyle, vibrationStyle, soundStyle, onStylePresetChange, onAnimationStyleChange, onVibrationStyleChange, onSoundStyleChange) { currentPage = SettingsPage.MAIN }
                SettingsPage.ABOUT -> AboutPage { currentPage = SettingsPage.MAIN }
            }
        }
    }
}

@Composable
private fun MainPage(onNavigate: (SettingsPage) -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
            IconButton(onClick = onClose) {
                Text("×", fontSize = 28.sp, color = TextSecondary)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Menu items
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MenuRow("Profile", "Name and scores") { onNavigate(SettingsPage.PROFILE) }
            MenuRow("Theme", "Colors") { onNavigate(SettingsPage.THEMES) }
            MenuRow("Layout", "Screen arrangement") { onNavigate(SettingsPage.LAYOUT) }
            MenuRow("Gameplay", "Game options") { onNavigate(SettingsPage.GAMEPLAY) }
            MenuRow("Experience", "Animation and effects") { onNavigate(SettingsPage.STYLES) }
            MenuRow("Feedback", "Sound and vibration levels") { onNavigate(SettingsPage.FEEDBACK) }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF222222), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            MenuRow("About", "Version 1.3.0") { onNavigate(SettingsPage.ABOUT) }
        }
    }
}

@Composable
private fun MenuRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Normal)
            Text(subtitle, fontSize = 12.sp, color = TextTertiary)
        }
        Text("›", fontSize = 20.sp, color = TextTertiary)
    }
}

@Composable
private fun GameplayPage(
    ghostPieceEnabled: Boolean,
    onGhostPieceChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Gameplay", onBack)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SettingCard {
            SettingToggle(
                title = "Ghost Piece",
                description = "Shows where the piece will land",
                checked = ghostPieceEnabled,
                onCheckedChange = onGhostPieceChange
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "The ghost piece appears as a faded outline at the bottom of the board, helping you see exactly where your piece will land.",
            fontSize = 12.sp,
            color = TextTertiary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun StylesPage(
    stylePreset: StylePreset,
    animationStyle: AnimationStyle,
    vibrationStyle: VibrationStyle,
    soundStyle: SoundStyle,
    onStylePresetChange: (StylePreset) -> Unit,
    onAnimationStyleChange: (AnimationStyle) -> Unit,
    onVibrationStyleChange: (VibrationStyle) -> Unit,
    onSoundStyleChange: (SoundStyle) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { PageHeader("Experience", onBack) }
        
        // Presets
        item {
            SectionTitle("Presets")
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StylePreset.entries.filter { it != StylePreset.CUSTOM }.forEach { preset ->
                    SelectableRow(
                        title = preset.displayName.removePrefix("🕹️ ").removePrefix("✨ ").removePrefix("🎮 ").removePrefix("🔇 "),
                        subtitle = preset.description,
                        selected = stylePreset == preset,
                        onClick = { onStylePresetChange(preset) }
                    )
                }
            }
        }
        
        // Custom options
        item {
            SectionTitle("Custom")
            Text("Selecting any option below switches to custom mode", fontSize = 11.sp, color = TextTertiary)
        }
        
        item {
            SettingCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Animation", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimationStyle.entries.forEach { style ->
                        CompactOption(style.displayName, style.description, animationStyle == style) { onAnimationStyleChange(style) }
                    }
                }
            }
        }
        
        item {
            SettingCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Vibration Pattern", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    VibrationStyle.entries.forEach { style ->
                        CompactOption(style.displayName, style.description, vibrationStyle == style) { onVibrationStyleChange(style) }
                    }
                }
            }
        }
        
        item {
            SettingCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sound Style", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    SoundStyle.entries.forEach { style ->
                        CompactOption(style.displayName, style.description, soundStyle == style) { onSoundStyleChange(style) }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun FeedbackPage(
    vibrationEnabled: Boolean,
    vibrationIntensity: Float,
    soundEnabled: Boolean,
    soundVolume: Float,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onVibrationIntensityChange: (Float) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Feedback", onBack)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SettingCard {
            Column {
                SettingToggle("Vibration", "Haptic feedback", vibrationEnabled, onVibrationEnabledChange)
                if (vibrationEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    IntensitySlider("Intensity", vibrationIntensity, onVibrationIntensityChange)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingCard {
            Column {
                SettingToggle("Sound", "Audio effects", soundEnabled, onSoundEnabledChange)
                if (soundEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    IntensitySlider("Volume", soundVolume, onSoundVolumeChange)
                }
            }
        }
    }
}

@Composable
private fun ProfilePage(
    playerName: String,
    highScore: Int,
    scoreHistory: List<ScoreEntry>,
    onPlayerNameChange: (String) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(playerName) }
    var showClear by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Profile", onBack)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editing) {
                            BasicTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it.take(20) },
                                textStyle = TextStyle(fontSize = 16.sp, color = TextPrimary),
                                cursorBrush = SolidColor(AccentColor),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                if (nameInput.isNotBlank()) onPlayerNameChange(nameInput)
                                editing = false
                            }) { Text("Save", color = AccentColor, fontSize = 14.sp) }
                        } else {
                            Column {
                                Text("Name", fontSize = 12.sp, color = TextTertiary)
                                Text(playerName, fontSize = 16.sp, color = TextPrimary)
                            }
                            TextButton(onClick = { nameInput = playerName; editing = true }) {
                                Text("Edit", color = AccentColor, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            
            item {
                SettingCard {
                    Column {
                        Text("High Score", fontSize = 12.sp, color = TextTertiary)
                        Text(
                            highScore.toString().padStart(6, '0'),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            color = AccentColor,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("History", fontSize = 14.sp, color = TextSecondary)
                    if (scoreHistory.isNotEmpty()) {
                        TextButton(onClick = { showClear = true }) {
                            Text("Clear", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            if (scoreHistory.isEmpty()) {
                item {
                    Text("No games yet", fontSize = 13.sp, color = TextTertiary, modifier = Modifier.padding(vertical = 20.dp))
                }
            } else {
                itemsIndexed(scoreHistory.take(15)) { index, entry ->
                    ScoreRow(index + 1, entry, entry.score == highScore)
                }
            }
        }
    }
    
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Clear History?", color = TextPrimary) },
            text = { Text("This cannot be undone.", color = TextSecondary) },
            confirmButton = { TextButton(onClick = { onClearHistory(); showClear = false }) { Text("Clear", color = Color(0xFFE57373)) } },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel", color = TextSecondary) } },
            containerColor = CardBackground
        )
    }
}

@Composable
private fun ThemesPage(currentThemeName: String, onThemeChange: (String) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Theme", onBack)
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GameThemes.allThemes.size) { index ->
                val theme = GameThemes.allThemes[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (theme.name == currentThemeName) CardBackgroundSelected else CardBackground)
                        .clickable { onThemeChange(theme.name) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Color preview
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(3.dp)).background(theme.screenBackground))
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(3.dp)).background(theme.pixelOn))
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(3.dp)).background(theme.buttonPrimary))
                        }
                        Text(theme.name, fontSize = 15.sp, color = TextPrimary)
                    }
                    if (theme.name == currentThemeName) {
                        Text("✓", fontSize = 16.sp, color = AccentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutPage(layoutMode: LayoutMode, onLayoutModeChange: (LayoutMode) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("Layout", onBack)
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableRow("Classic", "Device frame with LCD display", layoutMode == LayoutMode.CLASSIC) { onLayoutModeChange(LayoutMode.CLASSIC) }
            SelectableRow("Modern", "Clean design with status bar", layoutMode == LayoutMode.MODERN) { onLayoutModeChange(LayoutMode.MODERN) }
            SelectableRow("Fullscreen", "Maximum game area", layoutMode == LayoutMode.FULLSCREEN) { onLayoutModeChange(LayoutMode.FULLSCREEN) }
        }
    }
}

@Composable
private fun AboutPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        PageHeader("About", onBack)
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Brick Game", fontSize = 24.sp, fontWeight = FontWeight.Light, color = TextPrimary, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("1.3.0", fontSize = 14.sp, color = AccentColor)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text("Developed by", fontSize = 12.sp, color = TextTertiary)
            Text("Andrei Anton", fontSize = 15.sp, color = TextPrimary)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Built with Kotlin", fontSize = 12.sp, color = TextTertiary)
            Text("Jetpack Compose", fontSize = 12.sp, color = TextTertiary)
        }
    }
}

// Reusable components
@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", fontSize = 24.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Light, color = TextPrimary, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccentColor, letterSpacing = 1.sp)
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 15.sp, color = TextPrimary)
            Text(description, fontSize = 12.sp, color = TextTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentColor,
                checkedTrackColor = AccentColor.copy(alpha = 0.4f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
private fun IntensitySlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = TextTertiary)
            Text("${(value * 100).toInt()}%", fontSize = 12.sp, color = AccentColor)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = AccentColor,
                activeTrackColor = AccentColor,
                inactiveTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
private fun SelectableRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CardBackgroundSelected else CardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextTertiary)
        }
        if (selected) {
            Text("✓", fontSize = 16.sp, color = AccentColor)
        }
    }
}

@Composable
private fun CompactOption(name: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFF252520) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, color = if (selected) TextPrimary else TextSecondary)
            Text(description, fontSize = 10.sp, color = TextTertiary)
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AccentColor,
                unselectedColor = TextTertiary
            ),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ScoreRow(rank: Int, entry: ScoreEntry, isHighScore: Boolean) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$rank",
                fontSize = 12.sp,
                color = if (rank <= 3) AccentColor else TextTertiary,
                modifier = Modifier.width(28.dp)
            )
            Column {
                Text(entry.playerName, fontSize = 13.sp, color = TextPrimary)
                Text("${entry.score} • Lv.${entry.level}", fontSize = 11.sp, color = TextTertiary)
            }
        }
        Text(dateFormat.format(Date(entry.timestamp)), fontSize = 11.sp, color = TextTertiary)
    }
}
.dp))
            Column {
                Text(entry.playerName, fontSize = LabelSize, color = TextPrimary)
                Text("${entry.score} • Lv.${entry.level}", fontSize = SmallSize, color = TextTertiary)
            }
        }
        Text(dateFormat.format(Date(entry.timestamp)), fontSize = SmallSize, color = TextTertiary)
    }
}
