package com.brickgame.tetris.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes
import java.text.SimpleDateFormat
import java.util.*

enum class SettingsPage {
    MAIN,
    PROFILE,
    THEMES,
    LAYOUT,
    FEEDBACK,
    ABOUT
}

/**
 * Settings Screen with multiple pages
 */
@Composable
fun SettingsScreen(
    currentThemeName: String,
    layoutMode: LayoutMode,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    playerName: String,
    highScore: Int,
    scoreHistory: List<ScoreEntry>,
    onThemeChange: (String) -> Unit,
    onLayoutModeChange: (LayoutMode) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onPlayerNameChange: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    
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
                    soundEnabled = soundEnabled,
                    onVibrationChange = onVibrationChange,
                    onSoundChange = onSoundChange,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚙️ Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            CloseButton(onClick = onClose)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Menu items
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsMenuItem(
                icon = "👤",
                title = "Player Profile",
                subtitle = "Name, high score, history",
                onClick = { onNavigate(SettingsPage.PROFILE) }
            )
            SettingsMenuItem(
                icon = "🎨",
                title = "Themes",
                subtitle = "Change colors and style",
                onClick = { onNavigate(SettingsPage.THEMES) }
            )
            SettingsMenuItem(
                icon = "📱",
                title = "Layout",
                subtitle = "Classic, Modern, Fullscreen",
                onClick = { onNavigate(SettingsPage.LAYOUT) }
            )
            SettingsMenuItem(
                icon = "📳",
                title = "Feedback",
                subtitle = "Vibration and sound",
                onClick = { onNavigate(SettingsPage.FEEDBACK) }
            )
            SettingsMenuItem(
                icon = "ℹ️",
                title = "About",
                subtitle = "Version and credits",
                onClick = { onNavigate(SettingsPage.ABOUT) }
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PageHeader(title = "👤 Player Profile", onBack = onBack)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
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
                                if (nameInput.isNotBlank()) {
                                    onPlayerNameChange(nameInput)
                                }
                                editingName = false
                            }) {
                                Text("Save", color = Color(0xFFF4D03F))
                            }
                        } else {
                            Column {
                                Text("Name", fontSize = 12.sp, color = Color.Gray)
                                Text(playerName, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            }
                            TextButton(onClick = {
                                nameInput = playerName
                                editingName = true
                            }) {
                                Text("Edit", color = Color(0xFFF4D03F))
                            }
                        }
                    }
                }
            }
            
            // High Score
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
            
            // Score History
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
                            "No games played yet.\nStart playing to see your history!",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                itemsIndexed(scoreHistory.take(20)) { index, entry ->
                    ScoreHistoryItem(
                        rank = index + 1,
                        entry = entry,
                        isHighScore = entry.score == highScore
                    )
                }
            }
        }
    }
    
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear History?") },
            text = { Text("This will delete all your score history.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    showClearConfirm = false
                }) {
                    Text("Clear", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemesPage(
    currentThemeName: String,
    onThemeChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PageHeader(title = "🎨 Themes", onBack = onBack)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(GameThemes.allThemes.size) { index ->
                val theme = GameThemes.allThemes[index]
                ThemeOption(
                    theme = theme,
                    isSelected = theme.name == currentThemeName,
                    onClick = { onThemeChange(theme.name) }
                )
            }
            
            // Custom theme placeholder
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Custom Theme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF4D03F)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SectionCard {
                    Text(
                        "🎨 Custom theme creator coming soon!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutPage(
    layoutMode: LayoutMode,
    onLayoutModeChange: (LayoutMode) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PageHeader(title = "📱 Layout", onBack = onBack)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LayoutOption(
                title = "Classic",
                description = "Unified LCD screen like original hardware",
                icon = "🎮",
                isSelected = layoutMode == LayoutMode.CLASSIC,
                onClick = { onLayoutModeChange(LayoutMode.CLASSIC) }
            )
            LayoutOption(
                title = "Modern",
                description = "Clean minimal design with status bar",
                icon = "✨",
                isSelected = layoutMode == LayoutMode.MODERN,
                onClick = { onLayoutModeChange(LayoutMode.MODERN) }
            )
            LayoutOption(
                title = "Fullscreen",
                description = "Maximum game area, minimal UI",
                icon = "📺",
                isSelected = layoutMode == LayoutMode.FULLSCREEN,
                onClick = { onLayoutModeChange(LayoutMode.FULLSCREEN) }
            )
        }
    }
}

@Composable
private fun FeedbackPage(
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    onVibrationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PageHeader(title = "📳 Feedback", onBack = onBack)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleOption(
                title = "Vibration",
                description = "Haptic feedback on button press",
                isEnabled = vibrationEnabled,
                onToggle = onVibrationChange
            )
            ToggleOption(
                title = "Sound",
                description = "Sound effects (coming soon)",
                isEnabled = soundEnabled,
                onToggle = onSoundChange
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Note: Changes take effect immediately.",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun AboutPage(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PageHeader(title = "ℹ️ About", onBack = onBack)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SectionCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎮", fontSize = 48.sp)
                Text(
                    "Brick Game",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Version 1.2.0",
                    fontSize = 14.sp,
                    color = Color(0xFFF4D03F)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Developer", fontSize = 12.sp, color = Color.Gray)
                Text("Andrei Anton", fontSize = 16.sp, color = Color.White)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Built with", fontSize = 12.sp, color = Color.Gray)
                Text("Kotlin & Jetpack Compose", fontSize = 14.sp, color = Color.White)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "A nostalgic recreation of the classic\nBrick Game handheld console",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Helper Components

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF333333))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", fontSize = 24.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF444444))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("✕", fontSize = 20.sp, color = Color.White)
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun ThemeOption(
    theme: GameTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFF4D03F) else Color.Transparent,
        label = "borderColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.screenBackground))
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.pixelOn))
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.buttonPrimary))
            }
            Text(theme.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        if (isSelected) {
            Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F))
        }
    }
}

@Composable
private fun LayoutOption(
    title: String,
    description: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFF4D03F) else Color.Transparent,
        label = "borderColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
        if (isSelected) {
            Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4D03F))
        }
    }
}

@Composable
private fun ToggleOption(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onToggle(!isEnabled) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFF4D03F),
                checkedTrackColor = Color(0xFFF4D03F).copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun ScoreHistoryItem(
    rank: Int,
    entry: ScoreEntry,
    isHighScore: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighScore) Color(0xFF2A2A1A) else Color(0xFF1E1E1E))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color(0xFFF4D03F) else Color.Gray,
                modifier = Modifier.width(36.dp)
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.playerName, fontSize = 14.sp, color = Color.White)
                    if (isHighScore) {
                        Text(" 👑", fontSize = 12.sp)
                    }
                }
                Text(
                    "${entry.score} pts • Lv.${entry.level} • ${entry.lines}L",
                    fontSize = 12.sp,
                    color = Color(0xFFF4D03F)
                )
            }
        }
        Text(
            dateFormat.format(Date(entry.timestamp)),
            fontSize = 10.sp,
            color = Color.Gray
        )
    }
}
