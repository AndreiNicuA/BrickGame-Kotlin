package com.brickgame.tetris.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Button press animation with scale and slight rotation
 */
@Composable
fun Modifier.bounceClickAnimation(
    onClick: () -> Unit,
    onPress: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isPressed) -2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotation"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    onPress?.invoke()
                    tryAwaitRelease()
                    isPressed = false
                    onRelease?.invoke()
                },
                onTap = {
                    onClick()
                }
            )
        }
}

/**
 * Simple scale animation on press
 */
@Composable
fun Modifier.pressScaleAnimation(
    isPressed: Boolean,
    pressedScale: Float = 0.9f
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressScale"
    )
    this.scale(scale)
}

/**
 * Pulse animation for highlighted elements
 */
@Composable
fun Modifier.pulseAnimation(
    enabled: Boolean = true,
    minScale: Float = 0.97f,
    maxScale: Float = 1.03f,
    duration: Int = 800
): Modifier = composed {
    if (!enabled) return@composed this
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    this.scale(scale)
}

/**
 * Shake animation for invalid actions
 */
@Composable
fun Modifier.shakeAnimation(
    trigger: Boolean,
    onAnimationEnd: () -> Unit = {}
): Modifier = composed {
    var shouldShake by remember { mutableStateOf(false) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            shouldShake = true
        }
    }
    
    val shakeOffset by animateFloatAsState(
        targetValue = if (shouldShake) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        finishedListener = {
            if (shouldShake) {
                shouldShake = false
                onAnimationEnd()
            }
        },
        label = "shake"
    )
    
    val shakeTranslation = if (shouldShake) {
        kotlin.math.sin(shakeOffset * 6 * Math.PI).toFloat() * 10f
    } else 0f
    
    this.graphicsLayer {
        translationX = shakeTranslation
    }
}
