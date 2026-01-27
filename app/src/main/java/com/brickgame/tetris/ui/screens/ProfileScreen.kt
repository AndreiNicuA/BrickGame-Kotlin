package com.brickgame.tetris.ui.screens

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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Profile Screen - Player name and score history
 */
@Composable
fun ProfileScreen(
    playerName: String,
    highScore: Int,
    scoreHistory: List<ScoreEntry>,
    onNameChange: (String) -> Unit,
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
                    text = "👤 Profile",
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
            
            // Player Name Section
            ProfileSection(title = "🎮 Player Name") {
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
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                color = Color.White
                            ),
                            cursorBrush = SolidColor(Color(0xFFF4D03F)),
                            modifier = Modifier.weight(1f)
                        )
                        
                        TextButton(onClick = {
                            if (nameInput.isNotBlank()) {
                                onNameChange(nameInput)
                            }
                            editingName = false
                        }) {
                            Text("Save", color = Color(0xFFF4D03F))
                        }
                    } else {
                        Text(
                            text = playerName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        
                        TextButton(onClick = {
                            nameInput = playerName
                            editingName = true
                        }) {
                            Text("Edit", color = Color(0xFFF4D03F))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // High Score
            ProfileSection(title = "🏆 High Score") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = highScore.toString().padStart(6, '0'),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4D03F),
                        letterSpacing = 4.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score History
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Score History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF4D03F)
                )
                
                if (scoreHistory.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirm = true }) {
                        Text("Clear", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (scoreHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No games played yet.\nStart playing to see your history!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(scoreHistory) { index, entry ->
                        ScoreHistoryItem(
                            rank = index + 1,
                            entry = entry,
                            isHighScore = entry.score == highScore
                        )
                    }
                }
            }
        }
        
        // Clear confirmation dialog
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear History?") },
                text = { Text("This will delete all your score history. This action cannot be undone.") },
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
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                text = "#$rank",
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
                    Text(
                        text = entry.score.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isHighScore) {
                        Text(
                            text = "👑",
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = "Lv.${entry.level} • ${entry.lines} lines",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Text(
            text = dateFormat.format(Date(entry.timestamp)),
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}
