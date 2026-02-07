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
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickgame.tetris.ui.theme.LocalGameTheme

enum class ButtonIcon { UP, DOWN, LEFT, RIGHT, ROTATE }

// ===== Tap-only round button (for UP/hard drop) =====
@Composable
fun TapButton(
    icon: ButtonIcon, size: Dp = 60.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "s")

    Box(
        modifier.size(size).scale(scale)
            .shadow(if (isPressed) 2.dp else 6.dp, CircleShape)
            .clip(CircleShape).background(if (isPressed) theme.buttonPrimaryPressed else theme.buttonPrimary)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onClick() }
                )
            },
        Alignment.Center
    ) { IconDraw(icon, size) }
}

// ===== Hold button (for LEFT, RIGHT, DOWN — supports DAS via onPress/onRelease) =====
@Composable
fun HoldButton(
    icon: ButtonIcon, size: Dp = 60.dp,
    modifier: Modifier = Modifier,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "s")

    Box(
        modifier.size(size).scale(scale)
            .shadow(if (isPressed) 2.dp else 6.dp, CircleShape)
            .clip(CircleShape).background(if (isPressed) theme.buttonPrimaryPressed else theme.buttonPrimary)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Wait for finger down
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressed }) {
                            isPressed = true
                            onPress()
                            // Wait for finger up
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { !it.pressed }) {
                                    isPressed = false
                                    onRelease()
                                    break
                                }
                            }
                        }
                    }
                }
            },
        Alignment.Center
    ) { IconDraw(icon, size) }
}

// ===== Icon drawing helper =====
@Composable
private fun IconDraw(icon: ButtonIcon, size: Dp) {
    val iconColor = Color(0xFF1A1A1A)
    Canvas(Modifier.size(size * 0.4f)) {
        val w = this.size.width; val h = this.size.height
        val sw = w * 0.18f
        val stroke = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icon) {
            ButtonIcon.UP -> drawPath(Path().apply { moveTo(w*0.2f, h*0.7f); lineTo(w*0.5f, h*0.25f); lineTo(w*0.8f, h*0.7f) }, iconColor, style = stroke)
            ButtonIcon.DOWN -> drawPath(Path().apply { moveTo(w*0.2f, h*0.3f); lineTo(w*0.5f, h*0.75f); lineTo(w*0.8f, h*0.3f) }, iconColor, style = stroke)
            ButtonIcon.LEFT -> drawPath(Path().apply { moveTo(w*0.7f, h*0.2f); lineTo(w*0.25f, h*0.5f); lineTo(w*0.7f, h*0.8f) }, iconColor, style = stroke)
            ButtonIcon.RIGHT -> drawPath(Path().apply { moveTo(w*0.3f, h*0.2f); lineTo(w*0.75f, h*0.5f); lineTo(w*0.3f, h*0.8f) }, iconColor, style = stroke)
            ButtonIcon.ROTATE -> {
                drawPath(Path().apply { moveTo(w*0.65f, h*0.15f); cubicTo(w*0.9f, h*0.3f, w*0.9f, h*0.75f, w*0.5f, h*0.85f); cubicTo(w*0.15f, h*0.85f, w*0.1f, h*0.45f, w*0.35f, h*0.25f) }, iconColor, style = stroke)
                drawPath(Path().apply { moveTo(w*0.5f, h*0.05f); lineTo(w*0.65f, h*0.15f); lineTo(w*0.5f, h*0.3f) }, iconColor, style = Stroke(sw*0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

// ===== D-Pad =====
@Composable
fun DPad(
    buttonSize: Dp = 60.dp, modifier: Modifier = Modifier,
    rotateInCenter: Boolean = false,
    onUpPress: () -> Unit = {},
    onDownPress: () -> Unit = {}, onDownRelease: () -> Unit = {},
    onLeftPress: () -> Unit = {}, onLeftRelease: () -> Unit = {},
    onRightPress: () -> Unit = {}, onRightRelease: () -> Unit = {},
    onRotate: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val gap = 3.dp; val cs = buttonSize * 0.72f

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
        TapButton(ButtonIcon.UP, buttonSize, onClick = onUpPress)
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            HoldButton(ButtonIcon.LEFT, buttonSize, onPress = onLeftPress, onRelease = onLeftRelease)
            if (rotateInCenter) TapButton(ButtonIcon.ROTATE, cs, onClick = onRotate)
            else Box(Modifier.size(cs).clip(CircleShape).background(theme.buttonSecondaryPressed.copy(alpha = 0.4f)))
            HoldButton(ButtonIcon.RIGHT, buttonSize, onPress = onRightPress, onRelease = onRightRelease)
        }
        HoldButton(ButtonIcon.DOWN, buttonSize, onPress = onDownPress, onRelease = onDownRelease)
    }
}

// ===== Rotate Button =====
@Composable
fun RotateButton(onClick: () -> Unit, size: Dp = 72.dp, modifier: Modifier = Modifier) {
    TapButton(ButtonIcon.ROTATE, size, modifier, onClick)
}

// ===== Pill Action Button =====
@Composable
fun ActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    width: Dp = 90.dp, height: Dp = 40.dp, enabled: Boolean = true, backgroundColor: Color? = null
) {
    val theme = LocalGameTheme.current
    var isPressed by remember { mutableStateOf(false) }
    val bg = backgroundColor ?: theme.buttonSecondary
    val scale by animateFloatAsState(if (isPressed) 0.93f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "s")

    Box(
        modifier.width(width).height(height).scale(scale)
            .shadow(if (isPressed) 1.dp else 4.dp, RoundedCornerShape(height / 2))
            .clip(RoundedCornerShape(height / 2))
            .background(if (!enabled) bg.copy(alpha = 0.3f) else if (isPressed) bg.copy(alpha = 0.7f) else bg)
            .then(if (enabled) Modifier.pointerInput(Unit) {
                detectTapGestures(onPress = { isPressed = true; tryAwaitRelease(); isPressed = false }, onTap = { onClick() })
            } else Modifier),
        Alignment.Center
    ) {
        Text(text, fontSize = (height.value * 0.34f).sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp,
            color = if (enabled) theme.textPrimary else theme.textPrimary.copy(alpha = 0.3f))
    }
}

@Composable
fun WideActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    width: Dp = 100.dp, height: Dp = 44.dp, enabled: Boolean = true, backgroundColor: Color? = null
) = ActionButton(text, onClick, modifier, width, height, enabled, backgroundColor)
