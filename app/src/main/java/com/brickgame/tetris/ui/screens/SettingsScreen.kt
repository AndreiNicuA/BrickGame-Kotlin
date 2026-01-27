package com.brickgame.tetris.ui.screens

import androidx.compose.animation.animateColorAsState
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

/**
 * Settings Screen - All settings + Profile + History in one place
 */
@Composable
fun SettingsScreen(
    currentThemeName: String,
    isFullscreen: Boolean,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    playerName: String,
    highScore: Int,
    scoreHistory: List<ScoreEntry>,
    onThemeChange: (String) -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onPlayerNameChange: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(playerName) }
    var showClearConfirm by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
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
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF444444))
                ) {
                    Text("✕", fontSize = 20.sp, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Scrollable content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Player Profile Section
                item {
                    SettingsSection(title = "👤 Player Profile") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Player name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .padding(16.dp),
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
                            
                            // High score
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("🏆 High Score", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        highScore.toString().padStart(6, '0'),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF4D03F),
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Layout Section
                item {
                    SettingsSection(title = "📱 Layout") {
                        ToggleOption(
                            title = "Fullscreen Mode",
                            description = "Game only, no device frame",
                            isEnabled = isFullscreen,
                            onToggle = onFullscreenChange
                        )
                    }
                }
                
                // Theme Section
                item {
                    SettingsSection(title = "🎨 Theme") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GameThemes.allThemes.forEach { theme ->
                                ThemeOption(
                                    theme = theme,
                                    isSelected = theme.name == currentThemeName,
                                    onClick = { onThemeChange(theme.name) }
                                )
                            }
                        }
                    }
                }
                
                // Feedback Section
                item {
                    SettingsSection(title = "📳 Feedback") {
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
                    }
                }
                
                // Score History Section
                item {
                    SettingsSection(
                        title = "📊 Score History",
                        action = if (scoreHistory.isNotEmpty()) {
                            { TextButton(onClick = { showClearConfirm = true }) {
                                Text("Clear", color = Color.Gray, fontSize = 14.sp)
                            }}
                        } else null
                    ) {
                        if (scoreHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No games played yet.\nStart playing to see your history!",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                scoreHistory.take(10).forEachIndexed { index, entry ->
                                    ScoreHistoryItem(
                                        rank = index + 1,
                                        entry = entry,
                                        isHighScore = entry.score == highScore
                                    )
                                }
                                if (scoreHistory.size > 10) {
                                    Text(
                                        "... and ${scoreHistory.size - 10} more",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // About Section
                item {
                    SettingsSection(title = "ℹ️ About") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E1E))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Brick Game v1.2.0", fontSize = 14.sp, color = Color.White)
                            Text("Developer: Andrei Anton", fontSize = 13.sp, color = Color.Gray)
                            Text("Built with Kotlin & Jetpack Compose", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
                
                // Bottom spacer
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
        
        // Clear confirmation dialog
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear History?") },
                text = { Text("This will delete all your score history. This cannot be undone.") },
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
}

@Composable
private fun SettingsSection(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF4D03F)
            )
            action?.invoke()
        }
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
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color preview
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.screenBackground)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.pixelOn)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.buttonPrimary)
                )
            }
            
            Text(theme.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
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
            Text(description, fontSize = 13.sp, color = Color.Gray)
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
            .background(if (isHighScore) Color(0xFF2A2A1A) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color(0xFFF4D03F) else Color.Gray,
                modifier = Modifier.width(32.dp)
            )
            
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.playerName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    if (isHighScore) {
                        Text("👑", fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${entry.score} pts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4D03F)
                    )
                    Text(
                        "Lv.${entry.level} • ${entry.lines}L",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Text(
            dateFormat.format(Date(entry.timestamp)),
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}
