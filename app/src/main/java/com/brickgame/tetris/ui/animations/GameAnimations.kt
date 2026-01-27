package com.brickgame.tetris.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Animation state for game events
 */
sealed class GameAnimationEvent {
    object None : GameAnimationEvent()
    data class PieceLocked(val row: Int, val col: Int) : GameAnimationEvent()
    data class LineClearing(val rows: List<Int>) : GameAnimationEvent()
    object LevelUp : GameAnimationEvent()
    object GameOver : GameAnimationEvent()
}

/**
 * Piece lock animation - brief scale and brightness flash
 */
@Composable
fun Modifier.pieceLockAnimation(
    isLocking: Boolean
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isLocking) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "lockScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isLocking) 1.2f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "lockAlpha"
    )
    
    this
        .scale(scale)
        .graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) }
}

/**
 * Line clear animation - flash and collapse
 */
@Composable
fun rememberLineClearAnimation(
    clearingRows: List<Int>,
    onAnimationComplete: () -> Unit
): State<LineClearAnimationState> {
    val animationState = remember { mutableStateOf(LineClearAnimationState()) }
    
    LaunchedEffect(clearingRows) {
        if (clearingRows.isNotEmpty()) {
            // Phase 1: Flash
            animationState.value = LineClearAnimationState(
                isAnimating = true,
                phase = LineClearPhase.FLASH,
                rows = clearingRows,
                progress = 0f
            )
            
            // Flash animation
            repeat(3) {
                animationState.value = animationState.value.copy(progress = 1f)
                delay(50)
                animationState.value = animationState.value.copy(progress = 0f)
                delay(50)
            }
            
            // Phase 2: Collapse
            animationState.value = animationState.value.copy(
                phase = LineClearPhase.COLLAPSE,
                progress = 0f
            )
            
            // Collapse animation
            val steps = 10
            repeat(steps) { step ->
                animationState.value = animationState.value.copy(
                    progress = (step + 1).toFloat() / steps
                )
                delay(20)
            }
            
            // Complete
            animationState.value = LineClearAnimationState()
            onAnimationComplete()
        }
    }
    
    return animationState
}

data class LineClearAnimationState(
    val isAnimating: Boolean = false,
    val phase: LineClearPhase = LineClearPhase.NONE,
    val rows: List<Int> = emptyList(),
    val progress: Float = 0f
)

enum class LineClearPhase {
    NONE, FLASH, COLLAPSE
}

/**
 * Row animation modifier for line clear
 */
@Composable
fun Modifier.lineClearRowAnimation(
    animationState: LineClearAnimationState,
    rowIndex: Int
): Modifier = composed {
    if (!animationState.isAnimating || rowIndex !in animationState.rows) {
        return@composed this
    }
    
    when (animationState.phase) {
        LineClearPhase.FLASH -> {
            val flashAlpha = if (animationState.progress > 0.5f) 0.3f else 1f
            this.alpha(flashAlpha)
        }
        LineClearPhase.COLLAPSE -> {
            val scale = 1f - animationState.progress
            this
                .scale(scaleX = 1f, scaleY = scale)
                .alpha(1f - animationState.progress)
        }
        LineClearPhase.NONE -> this
    }
}

/**
 * Drop animation for falling piece
 */
@Composable
fun Modifier.pieceDropAnimation(
    isDroppingFast: Boolean
): Modifier = composed {
    val offsetY by animateFloatAsState(
        targetValue = 0f,
        animationSpec = if (isDroppingFast) {
            tween(durationMillis = 30, easing = LinearEasing)
        } else {
            spring(stiffness = Spring.StiffnessMediumLow)
        },
        label = "dropOffset"
    )
    
    this.graphicsLayer { translationY = offsetY }
}

/**
 * Game over animation - dramatic collapse
 */
@Composable
fun Modifier.gameOverAnimation(
    isGameOver: Boolean,
    rowIndex: Int,
    totalRows: Int
): Modifier = composed {
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            // Stagger animation per row from bottom to top
            delay((totalRows - rowIndex) * 50L)
            animationProgress = 1f
        } else {
            animationProgress = 0f
        }
    }
    
    val progress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 300, easing = EaseOutBounce),
        label = "gameOverProgress"
    )
    
    if (isGameOver && progress > 0f) {
        this
            .scale(scaleY = 1f - progress * 0.5f)
            .alpha(1f - progress * 0.7f)
            .graphicsLayer {
                rotationX = progress * 30f
            }
    } else {
        this
    }
}

/**
 * Level up celebration animation
 */
@Composable
fun Modifier.levelUpAnimation(
    isLevelingUp: Boolean
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "levelUp")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levelUpScale"
    )
    
    if (isLevelingUp) {
        this.scale(scale)
    } else {
        this
    }
}

private val EaseOutBounce: Easing = Easing { fraction ->
    val n1 = 7.5625f
    val d1 = 2.75f
    var t = fraction
    
    when {
        t < 1f / d1 -> n1 * t * t
        t < 2f / d1 -> {
            t -= 1.5f / d1
            n1 * t * t + 0.75f
        }
        t < 2.5f / d1 -> {
            t -= 2.25f / d1
            n1 * t * t + 0.9375f
        }
        else -> {
            t -= 2.625f / d1
            n1 * t * t + 0.984375f
        }
    }
}
