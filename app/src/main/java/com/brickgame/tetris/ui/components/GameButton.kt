package com.brickgame.tetris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * Primary game button (D-pad, rotate) - 20% larger default
 */
@Composable
fun PrimaryGameButton(
    text: String,
    size: Dp = 68.dp,  // 20% larger than 56dp
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 10.dp,
        animationSpec = tween(100),
        label = "shadow"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(shadowElevation, CircleShape)
            .clip(CircleShape)
            .background(if (isPressed) theme.buttonPrimaryPressed else theme.buttonPrimary)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
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
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * D-Pad component - ergonomic layout with 20% larger buttons
 */
@Composable
fun DPad(
    buttonSize: Dp = 58.dp,  // 20% larger than 48dp
    modifier: Modifier = Modifier,
    onUpPress: () -> Unit = {},
    onDownPress: () -> Unit = {},
    onDownRelease: () -> Unit = {},
    onLeftPress: () -> Unit = {},
    onLeftRelease: () -> Unit = {},
    onRightPress: () -> Unit = {},
    onRightRelease: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val spacing = 6.dp
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Up button (hard drop)
        PrimaryGameButton(
            text = "▲",
            size = buttonSize,
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
                onPress = onLeftPress,
                onRelease = onLeftRelease
            )
            
            // Center decoration
            Box(
                modifier = Modifier
                    .size(buttonSize * 0.7f)
                    .clip(CircleShape)
                    .background(theme.buttonSecondaryPressed)
            )
            
            PrimaryGameButton(
                text = "▶",
                size = buttonSize,
                onPress = onRightPress,
                onRelease = onRightRelease
            )
        }
        
        // Down button
        PrimaryGameButton(
            text = "▼",
            size = buttonSize,
            onPress = onDownPress,
            onRelease = onDownRelease
        )
    }
}

/**
 * Rotate button - 20% larger
 */
@Composable
fun RotateButton(
    onClick: () -> Unit,
    size: Dp = 72.dp,  // 20% larger than 60dp
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PrimaryGameButton(
            text = "↻",
            size = size,
            onClick = onClick
        )
        
        Text(
            text = "ROTATE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textSecondary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
