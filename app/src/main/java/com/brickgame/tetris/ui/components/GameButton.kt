package com.brickgame.tetris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * Primary game button with press animation
 */
@Composable
fun PrimaryGameButton(
    text: String,
    size: Dp = 68.dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    backgroundColor: Color? = null,
    pressedColor: Color? = null,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    var isPressed by remember { mutableStateOf(false) }
    val bg = backgroundColor ?: theme.buttonPrimary
    val pressed = pressedColor ?: theme.buttonPrimaryPressed

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
            .shadow(shadowElevation, shape)
            .clip(shape)
            .background(if (isPressed) pressed else bg)
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
 * D-Pad with optional rotate button in center.
 * When rotateInCenter=true, the center of the cross becomes a rotate button
 * instead of a decorative circle.
 */
@Composable
fun DPad(
    buttonSize: Dp = 58.dp,
    modifier: Modifier = Modifier,
    rotateInCenter: Boolean = false,
    onUpPress: () -> Unit = {},
    onDownPress: () -> Unit = {},
    onDownRelease: () -> Unit = {},
    onLeftPress: () -> Unit = {},
    onLeftRelease: () -> Unit = {},
    onRightPress: () -> Unit = {},
    onRightRelease: () -> Unit = {},
    onRotate: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val spacing = 4.dp
    val centerSize = buttonSize * 0.85f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Up (hard drop)
        PrimaryGameButton(
            text = "▲",
            size = buttonSize,
            onClick = onUpPress
        )

        // Left - Center(Rotate) - Right
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

            if (rotateInCenter) {
                // Rotate button in center of D-Pad
                PrimaryGameButton(
                    text = "↻",
                    size = centerSize,
                    backgroundColor = theme.buttonSecondary,
                    pressedColor = theme.buttonSecondaryPressed,
                    onClick = onRotate
                )
            } else {
                // Decorative center
                Box(
                    modifier = Modifier
                        .size(centerSize)
                        .clip(CircleShape)
                        .background(theme.buttonSecondaryPressed)
                )
            }

            PrimaryGameButton(
                text = "▶",
                size = buttonSize,
                onPress = onRightPress,
                onRelease = onRightRelease
            )
        }

        // Down (soft drop)
        PrimaryGameButton(
            text = "▼",
            size = buttonSize,
            onPress = onDownPress,
            onRelease = onDownRelease
        )
    }
}

/**
 * Standalone rotate button with label
 */
@Composable
fun RotateButton(
    onClick: () -> Unit,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
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

        if (showLabel) {
            Text(
                text = "ROTATE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * Wide action button (for HOLD, DROP, etc.)
 */
@Composable
fun WideActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 44.dp,
    enabled: Boolean = true,
    backgroundColor: Color? = null
) {
    val theme = LocalGameTheme.current
    var isPressed by remember { mutableStateOf(false) }
    val bg = backgroundColor ?: theme.buttonSecondary

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )

    Box(
        modifier = modifier
            .width(width).height(height)
            .scale(scale)
            .shadow(if (isPressed) 2.dp else 6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (!enabled) bg.copy(alpha = 0.3f)
                else if (isPressed) bg.copy(alpha = 0.7f)
                else bg
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
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
        Text(
            text,
            fontSize = (height.value * 0.35f).sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f)
        )
    }
}
