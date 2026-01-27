package com.brickgame.tetris.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.brickgame.tetris.ui.styles.VibrationStyle

/**
 * Manages haptic feedback with multiple vibration styles
 */
class VibrationManager(context: Context) {
    
    companion object {
        private const val TAG = "VibrationManager"
    }
    
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get vibrator", e)
        null
    }
    
    private var enabled: Boolean = true
    private var intensity: Float = 0.7f
    private var vibrationStyle: VibrationStyle = VibrationStyle.CLASSIC
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    fun setIntensity(intensity: Float) {
        this.intensity = intensity.coerceIn(0f, 1f)
    }
    
    fun setVibrationStyle(style: VibrationStyle) {
        this.vibrationStyle = style
        Log.d(TAG, "Vibration style set to: ${style.name}")
    }
    
    /**
     * Vibrate for move actions (left/right)
     */
    fun vibrateMove() {
        if (!enabled || vibrationStyle == VibrationStyle.NONE) return
        
        when (vibrationStyle) {
            VibrationStyle.SUBTLE -> vibrateSingle(5, 0.3f)
            VibrationStyle.CLASSIC -> vibrateSingle(10, 0.6f)
            VibrationStyle.RETRO -> vibratePattern(longArrayOf(0, 8, 30, 8), false) // double tap
            VibrationStyle.MODERN -> vibrateRamp(15, true)
            VibrationStyle.HEAVY -> vibrateSingle(20, 1.0f)
            VibrationStyle.NONE -> {}
        }
    }
    
    /**
     * Vibrate for rotate action
     */
    fun vibrateRotate() {
        if (!enabled || vibrationStyle == VibrationStyle.NONE) return
        
        when (vibrationStyle) {
            VibrationStyle.SUBTLE -> vibrateSingle(8, 0.4f)
            VibrationStyle.CLASSIC -> vibrateSingle(15, 0.7f)
            VibrationStyle.RETRO -> vibratePattern(longArrayOf(0, 10, 20, 10), false)
            VibrationStyle.MODERN -> vibrateRamp(20, true)
            VibrationStyle.HEAVY -> vibrateSingle(25, 1.0f)
            VibrationStyle.NONE -> {}
        }
    }
    
    /**
     * Vibrate for hard drop
     */
    fun vibrateDrop() {
        if (!enabled || vibrationStyle == VibrationStyle.NONE) return
        
        when (vibrationStyle) {
            VibrationStyle.SUBTLE -> vibrateSingle(15, 0.5f)
            VibrationStyle.CLASSIC -> vibrateSingle(30, 0.8f)
            VibrationStyle.RETRO -> vibratePattern(longArrayOf(0, 15, 30, 15, 30, 15), false)
            VibrationStyle.MODERN -> vibrateRamp(40, false)
            VibrationStyle.HEAVY -> vibrateSingle(50, 1.0f)
            VibrationStyle.NONE -> {}
        }
    }
    
    /**
     * Vibrate for line clear
     */
    fun vibrateClear(lineCount: Int) {
        if (!enabled || vibrationStyle == VibrationStyle.NONE) return
        
        val multiplier = lineCount.coerceIn(1, 4)
        
        when (vibrationStyle) {
            VibrationStyle.SUBTLE -> vibrateSingle(20 * multiplier, 0.5f)
            VibrationStyle.CLASSIC -> vibrateSingle(40 * multiplier, 0.8f)
            VibrationStyle.RETRO -> {
                val pattern = when (multiplier) {
                    1 -> longArrayOf(0, 30, 30, 30)
                    2 -> longArrayOf(0, 30, 30, 30, 30, 30)
                    3 -> longArrayOf(0, 30, 30, 30, 30, 30, 30, 30)
                    else -> longArrayOf(0, 50, 30, 50, 30, 50, 30, 50) // Tetris!
                }
                vibratePattern(pattern, false)
            }
            VibrationStyle.MODERN -> vibrateRamp(50 * multiplier, false)
            VibrationStyle.HEAVY -> vibrateSingle(80 * multiplier, 1.0f)
            VibrationStyle.NONE -> {}
        }
    }
    
    /**
     * Vibrate for game over
     */
    fun vibrateGameOver() {
        if (!enabled || vibrationStyle == VibrationStyle.NONE) return
        
        when (vibrationStyle) {
            VibrationStyle.SUBTLE -> vibratePattern(longArrayOf(0, 50, 100, 50), false)
            VibrationStyle.CLASSIC -> vibratePattern(longArrayOf(0, 100, 100, 100, 100, 100), false)
            VibrationStyle.RETRO -> vibratePattern(longArrayOf(0, 80, 80, 80, 80, 80, 80, 80, 80, 80), false)
            VibrationStyle.MODERN -> vibratePattern(longArrayOf(0, 150, 100, 150), false)
            VibrationStyle.HEAVY -> vibratePattern(longArrayOf(0, 200, 100, 200, 100, 300), false)
            VibrationStyle.NONE -> {}
        }
    }
    
    /**
     * Vibrate for level up
     */
    fun vibrateLevelUp() {
        if (!enabled || vibrationStyle == VibrationStyle.NONE) return
        
        when (vibrationStyle) {
            VibrationStyle.SUBTLE -> vibratePattern(longArrayOf(0, 20, 40, 20, 40, 20), false)
            VibrationStyle.CLASSIC -> vibratePattern(longArrayOf(0, 30, 50, 30, 50, 50), false)
            VibrationStyle.RETRO -> vibratePattern(longArrayOf(0, 20, 20, 20, 20, 20, 20, 40), false)
            VibrationStyle.MODERN -> vibratePattern(longArrayOf(0, 40, 60, 60, 60, 80), false)
            VibrationStyle.HEAVY -> vibratePattern(longArrayOf(0, 50, 50, 50, 50, 100), false)
            VibrationStyle.NONE -> {}
        }
    }
    
    private fun vibrateSingle(durationMs: Int, amplitudeMultiplier: Float) {
        val actualDuration = (durationMs * intensity).toLong().coerceAtLeast(1)
        val actualAmplitude = (255 * intensity * amplitudeMultiplier).toInt().coerceIn(1, 255)
        
        vibrator?.let { vib ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(actualDuration, actualAmplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(actualDuration)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration error", e)
            }
        }
    }
    
    private fun vibratePattern(pattern: LongArray, repeat: Boolean) {
        // Scale pattern durations by intensity
        val scaledPattern = pattern.mapIndexed { index, value ->
            if (index == 0) value else (value * intensity).toLong().coerceAtLeast(1)
        }.toLongArray()
        
        vibrator?.let { vib ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createWaveform(scaledPattern, if (repeat) 0 else -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(scaledPattern, if (repeat) 0 else -1)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration pattern error", e)
            }
        }
    }
    
    private fun vibrateRamp(durationMs: Int, rampUp: Boolean) {
        val actualDuration = (durationMs * intensity).toLong().coerceAtLeast(10)
        
        vibrator?.let { vib ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Create a simple ramp effect using waveform
                    val steps = 5
                    val stepDuration = actualDuration / steps
                    val timings = LongArray(steps) { stepDuration }
                    val amplitudes = IntArray(steps) { i ->
                        val progress = if (rampUp) (i + 1).toFloat() / steps else (steps - i).toFloat() / steps
                        (255 * progress * intensity).toInt().coerceIn(1, 255)
                    }
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(actualDuration)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration ramp error", e)
            }
        }
    }
    
    /**
     * Test vibration with current style
     */
    fun testVibration() {
        vibrateMove()
    }
}
