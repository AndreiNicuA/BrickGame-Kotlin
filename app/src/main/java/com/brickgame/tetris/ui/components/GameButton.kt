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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.brickgame.tetris.ui.layout.ButtonShape
import com.brickgame.tetris.ui.theme.LocalGameTheme

enum class ButtonIcon { UP, DOWN, LEFT, RIGHT, ROTATE }

val LocalButtonShape = androidx.compose.runtime.compositionLocalOf { ButtonShape.ROUND }

// ===== Tap-only round button (for UP/hard drop) =====
@Composable
fun TapButton(
    icon: ButtonIcon, size: Dp = 60.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val shape = LocalButtonShape.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "s")
    val bg = theme.buttonPrimary
    val bgPressed = theme.buttonPrimaryPressed
    val btnShape = when (shape) {
        ButtonShape.ROUND -> CircleShape
        ButtonShape.SQUARE -> RoundedCornerShape(12.dp)
        ButtonShape.OUTLINE -> CircleShape
    }
    val isOutline = shape == ButtonShape.OUTLINE

    Box(
        modifier.size(size).scale(scale)
            .shadow(if (isPressed || isOutline) 0.dp else 8.dp, btnShape)
            .clip(btnShape)
            .then(if (isOutline) {
                Modifier
                    .background(Color.Transparent)
                    .drawBehind {
                        drawRoundRect(
                            color = if (isPressed) bgPressed else bg,
                            style = Stroke(3.dp.toPx()),
                            cornerRadius = if (shape == ButtonShape.SQUARE) CornerRadius(12.dp.toPx()) else CornerRadius(this.size.width / 2)
                        )
                    }
            } else {
                Modifier
                    .background(Brush.verticalGradient(
                        if (isPressed) listOf(bgPressed, bgPressed.darken(0.15f))
                        else listOf(bg.lighten(0.1f), bg.darken(0.1f))
                    ))
                    .drawBehind {
                        drawCircle(Color.White.copy(alpha = if (isPressed) 0.05f else 0.15f),
                            radius = this.size.width * 0.38f,
                            center = Offset(this.size.width / 2, this.size.height * 0.35f))
                    }
            })
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onClick() }
                )
            },
        Alignment.Center
    ) { IconDraw(icon, size, if (isOutline) bg else Color(0xFF1A1A1A)) }
}

// ===== Hold button (for LEFT, RIGHT, DOWN — supports DAS) =====
@Composable
fun HoldButton(
    icon: ButtonIcon, size: Dp = 60.dp,
    modifier: Modifier = Modifier,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val shape = LocalButtonShape.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "s")
    val bg = theme.buttonPrimary
    val bgPressed = theme.buttonPrimaryPressed
    val btnShape = when (shape) {
        ButtonShape.ROUND -> CircleShape
        ButtonShape.SQUARE -> RoundedCornerShape(12.dp)
        ButtonShape.OUTLINE -> CircleShape
    }
    val isOutline = shape == ButtonShape.OUTLINE

    Box(
        modifier.size(size).scale(scale)
            .shadow(if (isPressed || isOutline) 0.dp else 8.dp, btnShape)
            .clip(btnShape)
            .then(if (isOutline) {
                Modifier
                    .background(Color.Transparent)
                    .drawBehind {
                        drawRoundRect(
                            color = if (isPressed) bgPressed else bg,
                            style = Stroke(3.dp.toPx()),
                            cornerRadius = if (shape == ButtonShape.SQUARE) CornerRadius(12.dp.toPx()) else CornerRadius(this.size.width / 2)
                        )
                    }
            } else {
                Modifier
                    .background(Brush.verticalGradient(
                        if (isPressed) listOf(bgPressed, bgPressed.darken(0.15f))
                        else listOf(bg.lighten(0.1f), bg.darken(0.1f))
                    ))
                    .drawBehind {
                        drawCircle(Color.White.copy(alpha = if (isPressed) 0.05f else 0.15f),
                            radius = this.size.width * 0.38f,
                            center = Offset(this.size.width / 2, this.size.height * 0.35f))
                    }
            })
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressed }) {
                            isPressed = true; onPress()
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { !it.pressed }) { isPressed = false; onRelease(); break }
                            }
                        }
                    }
                }
            },
        Alignment.Center
    ) { IconDraw(icon, size) }
}

// Color helpers
private fun Color.darken(f: Float) = Color(
    (red * (1 - f)).coerceIn(0f, 1f),
    (green * (1 - f)).coerceIn(0f, 1f),
    (blue * (1 - f)).coerceIn(0f, 1f), alpha
)
private fun Color.lighten(f: Float) = Color(
    (red + (1 - red) * f).coerceIn(0f, 1f),
    (green + (1 - green) * f).coerceIn(0f, 1f),
    (blue + (1 - blue) * f).coerceIn(0f, 1f), alpha
)

// ===== Icon drawing =====
@Composable
private fun IconDraw(icon: ButtonIcon, size: Dp, iconColor: Color = Color(0xFF1A1A1A)) {
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
    horizontalSpread: Dp = 0.dp,
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
        Row(horizontalArrangement = Arrangement.spacedBy(gap + horizontalSpread), verticalAlignment = Alignment.CenterVertically) {
            HoldButton(ButtonIcon.LEFT, buttonSize, onPress = onLeftPress, onRelease = onLeftRelease)
            if (rotateInCenter) TapButton(ButtonIcon.ROTATE, cs, onClick = onRotate)
            else { val btnShape = when (LocalButtonShape.current) { ButtonShape.ROUND -> CircleShape; ButtonShape.SQUARE -> RoundedCornerShape(8.dp); ButtonShape.OUTLINE -> CircleShape }; Box(Modifier.size(cs).clip(btnShape).background(theme.buttonSecondaryPressed.copy(alpha = 0.4f))) }
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
    val bg = backgroundColor ?: theme.buttonPrimary
    val scale by animateFloatAsState(if (isPressed) 0.93f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "s")
    val rounding = height / 2

    Box(
        modifier.width(width).height(height).scale(scale)
            .shadow(if (isPressed) 1.dp else 5.dp, RoundedCornerShape(rounding))
            .clip(RoundedCornerShape(rounding))
            .background(Brush.verticalGradient(
                if (!enabled) listOf(bg.copy(alpha = 0.3f), bg.copy(alpha = 0.3f))
                else if (isPressed) listOf(bg.darken(0.05f), bg.darken(0.2f))
                else listOf(bg.lighten(0.08f), bg.darken(0.08f))
            ))
            .drawBehind {
                if (enabled && !isPressed) {
                    // Top highlight
                    drawRoundRect(Color.White.copy(alpha = 0.12f),
                        topLeft = Offset(size.width * 0.1f, 0f),
                        size = Size(size.width * 0.8f, size.height * 0.4f),
                        cornerRadius = CornerRadius(size.height / 2))
                    // Bottom edge
                    drawRoundRect(Color.Black.copy(alpha = 0.1f),
                        topLeft = Offset(0f, size.height * 0.7f),
                        size = Size(size.width, size.height * 0.3f),
                        cornerRadius = CornerRadius(size.height / 2))
                }
            }
            .then(if (enabled) Modifier.pointerInput(Unit) {
                detectTapGestures(onPress = { isPressed = true; tryAwaitRelease(); isPressed = false }, onTap = { onClick() })
            } else Modifier),
        Alignment.Center
    ) {
        // Button text should be proportional to button dp size, not affected by UI scale.
        // Counteract any external fontScale so text fits the fixed-size button.
        val density = LocalDensity.current
        val baseDensity = Density(density.density, 1f)
        CompositionLocalProvider(LocalDensity provides baseDensity) {
            Text(text, fontSize = (height.value * 0.34f).sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.sp,
                color = if (!enabled) theme.textPrimary.copy(alpha = 0.3f)
                        else { // Auto-contrast: use dark text on light backgrounds, light text on dark
                            val bgLum = bg.red * 0.299f + bg.green * 0.587f + bg.blue * 0.114f
                            if (bgLum > 0.5f) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
                        })
        }
    }
}

@Composable
fun WideActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    width: Dp = 100.dp, height: Dp = 44.dp, enabled: Boolean = true, backgroundColor: Color? = null
) = ActionButton(text, onClick, modifier, width, height, enabled, backgroundColor)
