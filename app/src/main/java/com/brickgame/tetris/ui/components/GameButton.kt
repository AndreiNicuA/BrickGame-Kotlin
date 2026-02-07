package com.brickgame.tetris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.ui.theme.LocalGameTheme

// ===== Round Button with Custom Icon =====

@Composable
fun GameBtn(
    icon: ButtonIcon,
    size: Dp = 60.dp,
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
    val iconColor = Color(0xFF1A1A1A)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "s"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(if (isPressed) 2.dp else 6.dp, CircleShape)
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
        // Draw custom icon using Canvas
        Canvas(modifier = Modifier.size(size * 0.4f)) {
            val w = this.size.width
            val h = this.size.height
            val strokeWidth = w * 0.18f
            val stroke = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

            when (icon) {
                ButtonIcon.UP -> {
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.7f)
                        lineTo(w * 0.5f, h * 0.25f)
                        lineTo(w * 0.8f, h * 0.7f)
                    }
                    drawPath(path, iconColor, style = stroke)
                }
                ButtonIcon.DOWN -> {
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.3f)
                        lineTo(w * 0.5f, h * 0.75f)
                        lineTo(w * 0.8f, h * 0.3f)
                    }
                    drawPath(path, iconColor, style = stroke)
                }
                ButtonIcon.LEFT -> {
                    val path = Path().apply {
                        moveTo(w * 0.7f, h * 0.2f)
                        lineTo(w * 0.25f, h * 0.5f)
                        lineTo(w * 0.7f, h * 0.8f)
                    }
                    drawPath(path, iconColor, style = stroke)
                }
                ButtonIcon.RIGHT -> {
                    val path = Path().apply {
                        moveTo(w * 0.3f, h * 0.2f)
                        lineTo(w * 0.75f, h * 0.5f)
                        lineTo(w * 0.3f, h * 0.8f)
                    }
                    drawPath(path, iconColor, style = stroke)
                }
                ButtonIcon.ROTATE -> {
                    // Curved arrow
                    val path = Path().apply {
                        moveTo(w * 0.65f, h * 0.15f)
                        // Arc approximation
                        cubicTo(w * 0.9f, h * 0.3f, w * 0.9f, h * 0.75f, w * 0.5f, h * 0.85f)
                        cubicTo(w * 0.15f, h * 0.85f, w * 0.1f, h * 0.45f, w * 0.35f, h * 0.25f)
                    }
                    drawPath(path, iconColor, style = stroke)
                    // Arrow head
                    val arrow = Path().apply {
                        moveTo(w * 0.5f, h * 0.05f)
                        lineTo(w * 0.65f, h * 0.15f)
                        lineTo(w * 0.5f, h * 0.3f)
                    }
                    drawPath(arrow, iconColor, style = Stroke(strokeWidth * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

enum class ButtonIcon { UP, DOWN, LEFT, RIGHT, ROTATE }

// ===== D-Pad =====

@Composable
fun DPad(
    buttonSize: Dp = 60.dp,
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
    val gap = 3.dp
    val centerSize = buttonSize * 0.72f

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
        GameBtn(ButtonIcon.UP, buttonSize, onClick = onUpPress)
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            GameBtn(ButtonIcon.LEFT, buttonSize, onPress = onLeftPress, onRelease = onLeftRelease)
            if (rotateInCenter)
                GameBtn(ButtonIcon.ROTATE, centerSize, backgroundColor = theme.buttonSecondary, pressedColor = theme.buttonSecondaryPressed, onClick = onRotate)
            else
                Box(Modifier.size(centerSize).clip(CircleShape).background(theme.buttonSecondaryPressed.copy(alpha = 0.5f)))
            GameBtn(ButtonIcon.RIGHT, buttonSize, onPress = onRightPress, onRelease = onRightRelease)
        }
        GameBtn(ButtonIcon.DOWN, buttonSize, onPress = onDownPress, onRelease = onDownRelease)
    }
}

// ===== Rotate Button =====

@Composable
fun RotateButton(onClick: () -> Unit, size: Dp = 72.dp, modifier: Modifier = Modifier) {
    GameBtn(ButtonIcon.ROTATE, size, modifier = modifier, onClick = onClick)
}

// ===== Pill Action Button (HOLD, PAUSE, START) =====

@Composable
fun ActionButton(
    text: String, onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 90.dp, height: Dp = 40.dp,
    enabled: Boolean = true, backgroundColor: Color? = null
) {
    val theme = LocalGameTheme.current
    var isPressed by remember { mutableStateOf(false) }
    val bg = backgroundColor ?: theme.buttonSecondary

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh), label = "s"
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
        Text(
            text, fontSize = (height.value * 0.34f).sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp,
            color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f)
        )
    }
}

// Backward compat alias
@Composable
fun WideActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    width: Dp = 100.dp, height: Dp = 44.dp, enabled: Boolean = true, backgroundColor: Color? = null
) = ActionButton(text, onClick, modifier, width, height, enabled, backgroundColor)
