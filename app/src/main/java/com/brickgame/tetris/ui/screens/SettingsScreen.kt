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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.data.ScoreEntry
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes
import java.text.SimpleDateFormat
import java.util.*

enum class SettingsPage {
    MAIN, PROFILE, THEMES, LAYOUT, FEEDBACK, ABOUT
}

@Composable
fun SettingsScreen(
    currentThemeName: String,
    layoutMode: LayoutMode,
    vibrationEnabled: Boolean,
    vibrationIntensity: Float,
    soundEnabled: Boolean,
    soundVolume: Float,
    playerName: String,
    highScore: Int,
    scoreHistory: List<ScoreEntry>,
    onThemeChange: (String) -> Unit,
    onLayoutModeChange: (LayoutMode) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onVibrationIntensityChange: (Float) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onPlayerNameChange: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    
    // Handle back navigation
    val handleBack: () -> Unit = {
        if (currentPage == SettingsPage.MAIN) {
            onClose()
        } else {
            currentPage = SettingsPage.MAIN
        }
    }
    
    // Expose back handler to parent
    LaunchedEffect(currentPage) {
        // This allows parent to call onBack
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
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
            label = "pageTransition"
        ) { page ->
            when (page) {
                SettingsPage.MAIN -> MainSettingsPage(
                    onNavigate = { currentPage = it },
                    onClose = onClose
                )
                SettingsPage.PROFILE -> ProfilePage(
                    playerName = playerName,
                    highScore = highScore,
                    scoreHistory = scoreHistory,
                    onPlayerNameChange = onPlayerNameChange,
                    onClearHistory = onClearHistory,
                    onBack = { currentPage = SettingsPage.MAIN }
                )
                SettingsPage.THEMES -> ThemesPage(
                    currentThemeName = currentThemeName,
                    onThemeChange = onThemeChange,
                    onBack = { currentPage = SettingsPage.MAIN }
                )
                SettingsPage.LAYOUT -> LayoutPage(
                    layoutMode = layoutMode,
                    onLayoutModeChange = onLayoutModeChange,
                    onBack = { currentPage = SettingsPage.MAIN }
                )
                SettingsPage.FEEDBACK -> FeedbackPage(
                    vibrationEnabled = vibrationEnabled,
                    vibrationIntensity = vibrationIntensity,
                    soundEnabled = soundEnabled,
                    soundVolume = soundVolume,
                    onVibrationEnabledChange = onVibrationEnabledChange,
                    onVibrationIntensityChange = onVibrationIntensityChange,
                    onSoundEnabledChange = onSoundEnabledChange,
                    onSoundVolumeChange = onSoundVolumeChange,
                    onBack = { currentPage = SettingsPage.MAIN }
                )
                SettingsPage.ABOUT -> AboutPage(
                    onBack = { currentPage = SettingsPage.MAIN }
                )
            }
        }
    }
}

@Composable
private fun MainSettingsPage(
    onNavigate: (SettingsPage) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚙️ Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            CloseButton(onClick = onClose)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuItem("👤", "Player Profile", "Name, high score, history") { onNavigate(SettingsPage.PROFILE) }
            MenuItem("🎨", "Themes", "Change colors and style") { onNavigate(SettingsPage.THEMES) }
            MenuItem("📱", "Layout", "Classic, Modern, Fullscreen") { onNavigate(SettingsPage.LAYOUT) }
            MenuItem("📳", "Feedback", "Vibration and sound") { onNavigate(SettingsPage.FEEDBACK) }
            MenuItem("ℹ️", "About", "Version and credits") { onNavigate(SettingsPage.ABOUT) }
        }
    }
}

@Composable
private fun MenuItem(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Text("›", fontSize = 24.sp, color = Color.Gray)
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PageHeader(title = "📳 Feedback", onBack = onBack)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Vibration section
        SectionCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Vibration", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text("Haptic feedback on actions", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = onVibrationEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF4D03F),
                            checkedTrackColor = Color(0xFFF4D03F).copy(alpha = 0.5f)
                        )
                    )
                }
                
                if (vibrationEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Intensity", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔅", fontSize = 16.sp)
                        Slider(
                            value = vibrationIntensity,
                            onValueChange = onVibrationIntensityChange,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF4D03F),
                                activeTrackColor = Color(0xFFF4D03F)
                            )
                        )
                        Text("🔆", fontSize = 16.sp)
                    }
                    Text(
                        "${(vibrationIntensity * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color(0xFFF4D03F),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sound section
        SectionCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sound", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text("Sound effects", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = onSoundEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF4D03F),
                            checkedTrackColor = Color(0xFFF4D03F).copy(alpha = 0.5f)
                        )
                    )
                }
                
                if (soundEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Volume", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔈", fontSize = 16.sp)
                        Slider(
                            value = soundVolume,
                            onValueChange = onSoundVolumeChange,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF4D03F),
                                activeTrackColor = Color(0xFFF4D03F)
                            )
                        )
                        Text("🔊", fontSize = 16.sp)
                    }
                    Text(
                        "${(soundVolume * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color(0xFFF4D03F),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
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
    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(playerName) }
    var showClearConfirm by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PageHeader(title = "👤 Player Profile", onBack = onBack)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingName) {
                            BasicTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it.take(20) },
                                textStyle = TextStyle(fontSize = 18.sp, color = Color.White),
                                cursorBrush = SolidColor(Color(0xFFF4D03F)),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                if (nameInput.isNotBlank()) onPlayerNameChange(nameInput)
                                editingName = false
                            }) { Text("Save", color = Color(0xFFF4D03F)) }
                        } else {
                            Column {
                                Text("Name", fontSize = 12.sp, color = Color.Gray)
                                Text(playerName, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            }
                            TextButton(onClick = { nameInput = playerName; editingName = true }) {
                                Text("Edit", color = Color(0xFFF4D03F))
                            }
                        }
                    }
                }
            }
            
            item {
                SectionCard {
                    Column {
                        Text("🏆 High Score", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            highScore.toString().padStart(6, '0'),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4D03F)
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
                    Text("📊 Score History", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF4D03F))
                    if (scoreHistory.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text("Clear", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            if (scoreHistory.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            "No games played yet.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                itemsIndexed(scoreHistory.take(20)) { index, entry ->
                    ScoreItem(rank = index + 1, entry = entry, isHighScore = entry.score == highScore)
                }
            }
        }
    }
    
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear History?") },
            text = { Text("This will delete all score history.") },
            confirmButton = {
                TextButton(onClick = { onClearHistory(); showClearConfirm = false }) {
                    Text("Clear", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThemesPage(currentThemeName: String, onThemeChange: (String) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PageHeader(title = "🎨 Themes", onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(GameThemes.allThemes.size) { index ->
                val theme = GameThemes.allThemes[index]
                ThemeItem(theme = theme, isSelected = theme.name == currentThemeName) { onThemeChange(theme.name) }
            }
        }
    }
}

@Composable
private fun LayoutPage(layoutMode: LayoutMode, onLayoutModeChange: (LayoutMode) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PageHeader(title = "📱 Layout", onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LayoutItem("🎮", "Classic", "Unified LCD like original hardware", layoutMode == LayoutMode.CLASSIC) { onLayoutModeChange(LayoutMode.CLASSIC) }
            LayoutItem("✨", "Modern", "Clean design with status bar", layoutMode == LayoutMode.MODERN) { onLayoutModeChange(LayoutMode.MODERN) }
            LayoutItem("📺", "Fullscreen", "Maximum game area", layoutMode == LayoutMode.FULLSCREEN) { onLayoutModeChange(LayoutMode.FULLSCREEN) }
        }
    }
}

@Composable
private fun AboutPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PageHeader(title = "ℹ️ About", onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        
        SectionCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎮", fontSize = 48.sp)
                Text("Brick Game", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Version 1.3.0", fontSize = 14.sp, color = Color(0xFFF4D03F))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Developer", fontSize = 12.sp, color = Color.Gray)
                Text("Andrei Anton", fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Built with Kotlin & Jetpack Compose", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// Helper composables
@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF333333)).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Text("‹", fontSize = 24.sp, color = Color.White) }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF444444)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text("✕", fontSize = 20.sp, color = Color.White) }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1E1E)).padding(16.dp)
    ) { content() }
}

@Composable
private fun ThemeItem(theme: GameTheme, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1E1E))
            .then(if (isSelected) Modifier.background(Color(0xFF2A2A1A)) else Modifier)
            .clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground))
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.pixelOn))
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.buttonPrimary))
            }
            Text(theme.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        if (isSelected) Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F))
    }
}

@Composable
private fun LayoutItem(icon: String, title: String, desc: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1E1E))
            .then(if (isSelected) Modifier.background(Color(0xFF2A2A1A)) else Modifier)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
        }
        if (isSelected) Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F))
    }
}

@Composable
private fun ScoreItem(rank: Int, entry: ScoreEntry, isHighScore: Boolean) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (isHighScore) Color(0xFF2A2A1A) else Color(0xFF1E1E1E)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#$rank", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color(0xFFF4D03F) else Color.Gray, modifier = Modifier.width(36.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.playerName, fontSize = 14.sp, color = Color.White)
                    if (isHighScore) Text(" 👑", fontSize = 12.sp)
                }
                Text("${entry.score} pts • Lv.${entry.level} • ${entry.lines}L", fontSize = 12.sp, color = Color(0xFFF4D03F))
            }
        }
        Text(dateFormat.format(Date(entry.timestamp)), fontSize = 10.sp, color = Color.Gray)
    }
}
