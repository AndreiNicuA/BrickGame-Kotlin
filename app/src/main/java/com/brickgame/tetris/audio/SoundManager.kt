package com.brickgame.tetris.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Manages game sound effects using ToneGenerator (no external files needed)
 */
class SoundManager(context: Context) {
    
    companion object {
        private const val TAG = "SoundManager"
    }
    
    private var toneGenerator: ToneGenerator? = null
    private var volume: Float = 1.0f
    private var enabled: Boolean = true
    
    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            Log.d(TAG, "ToneGenerator initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ToneGenerator", e)
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        Log.d(TAG, "Sound enabled: $enabled")
    }
    
    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        // Recreate tone generator with new volume
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, (volume * 100).toInt())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update volume", e)
        }
    }
    
    fun playMove() {
        if (enabled) {
            playTone(ToneGenerator.TONE_PROP_BEEP, 30)
        }
    }
    
    fun playRotate() {
        if (enabled) {
            playTone(ToneGenerator.TONE_PROP_BEEP2, 40)
        }
    }
    
    fun playDrop() {
        if (enabled) {
            playTone(ToneGenerator.TONE_PROP_ACK, 50)
        }
    }
    
    fun playClear() {
        if (enabled) {
            playTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        }
    }
    
    fun playGameOver() {
        if (enabled) {
            playTone(ToneGenerator.TONE_PROP_NACK, 300)
        }
    }
    
    private fun playTone(tone: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(tone, durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play tone", e)
        }
    }
    
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
