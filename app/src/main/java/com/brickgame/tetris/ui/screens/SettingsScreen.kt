package com.brickgame.tetris.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.GameThemes

/**
 * Settings Screen
 * Theme, Layout, Vibration, Sound settings
 */
@Composable
fun SettingsScreen(
    currentThemeName: String,
    currentLayoutMode: LayoutMode,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    onThemeChange: (String) -> Unit,
    onLayoutChange: (LayoutMode) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    Text(
                        text = "✕",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Settings content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Layout section
                item {
                    SettingsSection(title = "📱 Layout") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LayoutOption.entries.forEach { mode ->
                                LayoutOptionItem(
                                    mode = mode,
                                    isSelected = currentLayoutMode == mode.layoutMode,
                                    onClick = { onLayoutChange(mode.layoutMode) }
                                )
                            }
                        }
                    }
                }
                
                // Theme section
                item {
                    SettingsSection(title = "🎨 Theme") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GameThemes.allThemes.forEach { gameTheme ->
                                ThemeOption(
                                    theme = gameTheme,
                                    isSelected = gameTheme.name == currentThemeName,
                                    onClick = { onThemeChange(gameTheme.name) }
                                )
                            }
                        }
                    }
                }
                
                // Feedback section
                item {
                    SettingsSection(title = "📳 Feedback") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ToggleOption(
                                title = "Vibration",
                                description = "Haptic feedback on actions",
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
                
                // About section
                item {
                    SettingsSection(title = "ℹ️ About") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Brick Game v1.1.0",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Developer: Andrei Anton",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Built with Kotlin & Jetpack Compose",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Spacer at bottom
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private enum class LayoutOption(
    val layoutMode: LayoutMode,
    val icon: String,
    val title: String,
    val description: String
) {
    CLASSIC(LayoutMode.CLASSIC, "📱", "Classic", "Full device with decorations"),
    COMPACT(LayoutMode.COMPACT, "🎮", "Compact", "Smaller device, more screen"),
    FULLSCREEN(LayoutMode.FULLSCREEN, "📺", "Fullscreen", "Game only, no frame")
}

@Composable
private fun LayoutOptionItem(
    mode: LayoutOption,
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
            Text(
                text = mode.icon,
                fontSize = 24.sp
            )
            Column {
                Text(
                    text = mode.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = mode.description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        if (isSelected) {
            Text(
                text = "✓",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF4D03F)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF4D03F)
        )
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
            
            Text(
                text = theme.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        
        if (isSelected) {
            Text(
                text = "✓",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF4D03F)
            )
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
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.Gray
            )
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
