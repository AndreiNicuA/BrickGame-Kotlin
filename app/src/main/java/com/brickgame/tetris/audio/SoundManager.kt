package com.brickgame.tetris.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.brickgame.tetris.ui.styles.SoundStyle

/**
 * Manages game sound effects with multiple styles
 */
class SoundManager(@Suppress("unused") private val context: Context) {
    
    companion object {
        private const val TAG = "SoundManager"
    }
    
    private var toneGenerator: ToneGenerator? = null
    private var volume: Float = 0.7f
    private var enabled: Boolean = true
    private var soundStyle: SoundStyle = SoundStyle.RETRO_BEEP
    
    init {
        recreateToneGenerator()
    }
    
    private fun recreateToneGenerator() {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, (volume * 100).toInt())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ToneGenerator", e)
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        recreateToneGenerator()
    }
    
    fun setSoundStyle(style: SoundStyle) {
        this.soundStyle = style
        Log.d(TAG, "Sound style set to: ${style.name}")
    }
    
    fun playMove() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        
        when (soundStyle) {
            SoundStyle.RETRO_BEEP -> playTone(ToneGenerator.TONE_DTMF_1, 30)
            SoundStyle.MODERN_SOFT -> playTone(ToneGenerator.TONE_PROP_BEEP, 20)
            SoundStyle.ARCADE -> playTone(ToneGenerator.TONE_DTMF_5, 40)
            SoundStyle.MECHANICAL -> playTone(ToneGenerator.TONE_CDMA_PIP, 25)
            SoundStyle.SYNTHWAVE -> playTone(ToneGenerator.TONE_DTMF_2, 25)
            SoundStyle.NONE -> {}
        }
    }
    
    fun playRotate() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        
        when (soundStyle) {
            SoundStyle.RETRO_BEEP -> playTone(ToneGenerator.TONE_DTMF_3, 40)
            SoundStyle.MODERN_SOFT -> playTone(ToneGenerator.TONE_PROP_BEEP2, 30)
            SoundStyle.ARCADE -> playTone(ToneGenerator.TONE_DTMF_9, 50)
            SoundStyle.MECHANICAL -> playTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE, 35)
            SoundStyle.SYNTHWAVE -> playTone(ToneGenerator.TONE_DTMF_B, 35)
            SoundStyle.NONE -> {}
        }
    }
    
    fun playDrop() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        
        when (soundStyle) {
            SoundStyle.RETRO_BEEP -> playTone(ToneGenerator.TONE_DTMF_0, 60)
            SoundStyle.MODERN_SOFT -> playTone(ToneGenerator.TONE_PROP_ACK, 40)
            SoundStyle.ARCADE -> playTone(ToneGenerator.TONE_DTMF_D, 80)
            SoundStyle.MECHANICAL -> playTone(ToneGenerator.TONE_CDMA_ANSWER, 50)
            SoundStyle.SYNTHWAVE -> playTone(ToneGenerator.TONE_SUP_RINGTONE, 70)
            SoundStyle.NONE -> {}
        }
    }
    
    fun playClear() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        
        when (soundStyle) {
            SoundStyle.RETRO_BEEP -> playTone(ToneGenerator.TONE_DTMF_A, 80)
            SoundStyle.MODERN_SOFT -> playTone(ToneGenerator.TONE_PROP_PROMPT, 120)
            SoundStyle.ARCADE -> playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            SoundStyle.MECHANICAL -> playTone(ToneGenerator.TONE_CDMA_CONFIRM, 150)
            SoundStyle.SYNTHWAVE -> playTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 180)
            SoundStyle.NONE -> {}
        }
    }
    
    fun playGameOver() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        
        when (soundStyle) {
            SoundStyle.RETRO_BEEP -> playTone(ToneGenerator.TONE_SUP_ERROR, 400)
            SoundStyle.MODERN_SOFT -> playTone(ToneGenerator.TONE_PROP_NACK, 300)
            SoundStyle.ARCADE -> playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 500)
            SoundStyle.MECHANICAL -> playTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE, 400)
            SoundStyle.SYNTHWAVE -> playTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
            SoundStyle.NONE -> {}
        }
    }
    
    fun playLevelUp() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        
        when (soundStyle) {
            SoundStyle.RETRO_BEEP -> playTone(ToneGenerator.TONE_DTMF_B, 150)
            SoundStyle.MODERN_SOFT -> playTone(ToneGenerator.TONE_PROP_PROMPT, 200)
            SoundStyle.ARCADE -> playTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 300)
            SoundStyle.MECHANICAL -> playTone(ToneGenerator.TONE_CDMA_NETWORK_CALLWAITING, 200)
            SoundStyle.SYNTHWAVE -> playTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 250)
            SoundStyle.NONE -> {}
        }
    }
    
    /** Perfect Clear celebration sound */
    fun playPerfectClear() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        playTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 600)
    }
    

    fun playHold() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        playTone(ToneGenerator.TONE_DTMF_4, 35)
    }

    fun playCombo(count: Int) {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        // Higher pitched tone for longer combos
        val tone = when {
            count >= 8 -> ToneGenerator.TONE_DTMF_A
            count >= 5 -> ToneGenerator.TONE_DTMF_9
            count >= 3 -> ToneGenerator.TONE_DTMF_7
            else -> ToneGenerator.TONE_DTMF_5
        }
        playTone(tone, 60 + count * 10)
    }

    fun playPieceLock() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        playTone(ToneGenerator.TONE_PROP_BEEP, 15)
    }

    fun playTimerWarning() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        playTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
    }

    fun playNewHighScore() {
        if (!enabled || soundStyle == SoundStyle.NONE) return
        playTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 400)
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
