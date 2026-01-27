package com.brickgame.tetris.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.ui.theme.GameTheme
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * Primary game button (yellow D-pad buttons, rotate)
 */
@Composable
fun PrimaryGameButton(
    text: String,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    
    // Animation values
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )
    
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = tween(100),
        label = "shadow"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(shadowElevation, CircleShape)
            .clip(CircleShape)
            .background(
                if (isPressed) theme.buttonPrimaryPressed else theme.buttonPrimary
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                if (vibrationEnabled) vibrateButton(context)
                                onPress()
                                tryAwaitRelease()
                                isPressed = false
                                onRelease()
                            },
                            onTap = { onClick() }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Secondary game button (small gray buttons)
 */
@Composable
fun SecondaryGameButton(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(if (isPressed) 1.dp else 4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                if (isPressed) theme.buttonSecondaryPressed else theme.buttonSecondary
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                if (vibrationEnabled) vibrateButton(context)
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Small dot in center
        Box(
            modifier = Modifier
                .size(size * 0.25f)
                .clip(CircleShape)
                .background(theme.buttonSecondaryPressed)
        )
    }
}

/**
 * Labeled button (label above button)
 */
@Composable
fun LabeledSecondaryButton(
    label: String,
    buttonSize: Dp = 36.dp,
    modifier: Modifier = Modifier,
    vibrationEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = theme.textSecondary,
            textAlign = TextAlign.Center
        )
        
        SecondaryGameButton(
            size = buttonSize,
            vibrationEnabled = vibrationEnabled,
            onClick = onClick
        )
    }
}

/**
 * D-Pad component with all direction buttons
 */
@Composable
fun DPad(
    buttonSize: Dp = 54.dp,
    modifier: Modifier = Modifier,
    vibrationEnabled: Boolean = true,
    onUpPress: () -> Unit = {},
    onDownPress: () -> Unit = {},
    onDownRelease: () -> Unit = {},
    onLeftPress: () -> Unit = {},
    onLeftRelease: () -> Unit = {},
    onRightPress: () -> Unit = {},
    onRightRelease: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val spacing = 4.dp
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Up button
        PrimaryGameButton(
            text = "▲",
            size = buttonSize,
            vibrationEnabled = vibrationEnabled,
            onClick = onUpPress
        )
        
        // Left - Center - Right
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryGameButton(
                text = "◀",
                size = buttonSize,
                vibrationEnabled = vibrationEnabled,
                onPress = onLeftPress,
                onRelease = onLeftRelease
            )
            
            // Center circle
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(theme.buttonSecondaryPressed)
            )
            
            PrimaryGameButton(
                text = "▶",
                size = buttonSize,
                vibrationEnabled = vibrationEnabled,
                onPress = onRightPress,
                onRelease = onRightRelease
            )
        }
        
        // Down button
        PrimaryGameButton(
            text = "▼",
            size = buttonSize,
            vibrationEnabled = vibrationEnabled,
            onPress = onDownPress,
            onRelease = onDownRelease
        )
        
        // Labels
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text("LEFT", fontSize = 9.sp, color = theme.textSecondary)
            Text("DOWN", fontSize = 9.sp, color = theme.textSecondary)
            Text("RIGHT", fontSize = 9.sp, color = theme.textSecondary)
        }
    }
}

/**
 * Rotate button with label
 */
@Composable
fun RotateButton(
    size: Dp = 68.dp,
    modifier: Modifier = Modifier,
    vibrationEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PrimaryGameButton(
            text = "↻",
            size = size,
            vibrationEnabled = vibrationEnabled,
            onClick = onClick
        )
        
        Text(
            text = "ROTATE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = theme.textSecondary
        )
    }
}

// Vibration helper
private fun vibrateButton(context: Context, duration: Long = 15L) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    } catch (e: Exception) {
        // Vibration not available
    }
}
