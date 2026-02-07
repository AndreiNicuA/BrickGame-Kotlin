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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.ui.theme.LocalGameTheme

/**
 * Round game button — used for D-Pad arrows and Rotate.
 * Clean, large touch target, bouncy press feedback.
 */
@Composable
fun GameBtn(
    text: String,
    size: Dp = 62.dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(if (isPressed) 2.dp else 8.dp, CircleShape)
            .clip(CircleShape)
            .background(if (isPressed) pressed else bg)
            .then(
                if (enabled) Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { isPressed = true; onPress(); tryAwaitRelease(); isPressed = false; onRelease() },
                        onTap = { onClick() }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = (size.value * 0.35f).sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A), textAlign = TextAlign.Center)
    }
}

/**
 * Clean D-Pad — minimalist arrows, generous spacing, optional rotate in center.
 */
@Composable
fun DPad(
    buttonSize: Dp = 62.dp,
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
    val gap = 4.dp
    val centerSize = buttonSize * 0.78f

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
        GameBtn("▲", buttonSize, onClick = onUpPress)
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            GameBtn("◀", buttonSize, onPress = onLeftPress, onRelease = onLeftRelease)
            if (rotateInCenter)
                GameBtn("↻", centerSize, backgroundColor = theme.buttonSecondary, pressedColor = theme.buttonSecondaryPressed, onClick = onRotate)
            else
                Box(Modifier.size(centerSize).clip(CircleShape).background(theme.buttonSecondaryPressed))
            GameBtn("▶", buttonSize, onPress = onRightPress, onRelease = onRightRelease)
        }
        GameBtn("▼", buttonSize, onPress = onDownPress, onRelease = onDownRelease)
    }
}

/**
 * Standalone rotate button — large, obvious.
 */
@Composable
fun RotateButton(onClick: () -> Unit, size: Dp = 72.dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GameBtn("↻", size, onClick = onClick)
    }
}

/**
 * Pill-shaped action button — for HOLD, PAUSE, START, etc.
 * Clean rounded pill, consistent height, gentle press animation.
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 90.dp,
    height: Dp = 40.dp,
    enabled: Boolean = true,
    backgroundColor: Color? = null
) {
    val theme = LocalGameTheme.current
    var isPressed by remember { mutableStateOf(false) }
    val bg = backgroundColor ?: theme.buttonSecondary

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )

    Box(
        modifier = modifier
            .width(width).height(height)
            .scale(scale)
            .shadow(if (isPressed) 1.dp else 4.dp, RoundedCornerShape(height / 2))
            .clip(RoundedCornerShape(height / 2))
            .background(if (!enabled) bg.copy(alpha = 0.3f) else if (isPressed) bg.copy(alpha = 0.7f) else bg)
            .then(
                if (enabled) Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                        onTap = { onClick() }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = (height.value * 0.36f).sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp,
            color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f))
    }
}

// Keep backward compatibility alias
@Composable
fun WideActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    width: Dp = 100.dp, height: Dp = 44.dp, enabled: Boolean = true, backgroundColor: Color? = null
) = ActionButton(text, onClick, modifier, width, height, enabled, backgroundColor)

// Keep backward compatibility alias
@Composable
fun PrimaryGameButton(
    text: String, size: Dp = 68.dp, modifier: Modifier = Modifier,
    enabled: Boolean = true, shape: androidx.compose.ui.graphics.Shape = CircleShape,
    backgroundColor: Color? = null, pressedColor: Color? = null,
    onPress: () -> Unit = {}, onRelease: () -> Unit = {}, onClick: () -> Unit = {}
) = GameBtn(text, size, modifier, enabled, backgroundColor, pressedColor, onPress, onRelease, onClick)
