package com.brickgame.tetris.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.brickgame.tetris.ui.styles.SoundStyle
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Game sound effects, one synthesized sample set per [SoundStyle].
 *
 * Samples are generated off the main thread, written as small WAV files to the cache dir and
 * loaded into a [SoundPool]. Playing is then a cheap, non-blocking call that can mix several
 * sounds at once (the old ToneGenerator blocked the main thread, played only one tone at a
 * time and was recreated on every volume change).
 */
class SoundManager(context: Context) {

    companion object {
        private const val TAG = "SoundManager"
        private const val SAMPLE_RATE = 22050
    }

    private enum class Wave { SINE, SQUARE, TRIANGLE, SAW, NOISE }

    /** One synthesized note: frequency sweep [f0]→[f1] over [ms], with relative [gain]. */
    private data class Note(val f0: Float, val f1: Float = f0, val ms: Int, val gain: Float = 1f)

    private enum class Sfx { MOVE, ROTATE, DROP, CLEAR1, CLEAR2, CLEAR3, CLEAR4, LEVEL_UP, GAME_OVER,
        HOLD, COMBO, LOCK, TIMER_WARNING, PERFECT_CLEAR, HIGH_SCORE }

    private val cacheDir = File(context.cacheDir, "sfx").apply { mkdirs() }
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "sfx-loader").apply { isDaemon = true } }
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    /** Loaded sample ids for the active style. Written on the loader thread, read on main. */
    private val sampleIds = ConcurrentHashMap<Sfx, Int>()

    @Volatile private var volume: Float = 0.7f
    @Volatile private var enabled: Boolean = true
    @Volatile private var soundStyle: SoundStyle = SoundStyle.RETRO_BEEP
    @Volatile private var released = false

    init {
        loadStyle(soundStyle)
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }

    fun setSoundStyle(style: SoundStyle) {
        if (style == soundStyle && sampleIds.isNotEmpty()) return
        soundStyle = style
        loadStyle(style)
    }

    fun playMove() = play(Sfx.MOVE, 0.5f)
    fun playRotate() = play(Sfx.ROTATE, 0.6f)
    fun playDrop() = play(Sfx.DROP, 0.8f)

    /** Line clear; bigger clears get a longer, brighter jingle. */
    fun playClear(lines: Int = 1) = play(
        when (lines) { 1 -> Sfx.CLEAR1; 2 -> Sfx.CLEAR2; 3 -> Sfx.CLEAR3; else -> Sfx.CLEAR4 }
    )

    fun playGameOver() = play(Sfx.GAME_OVER)
    fun playLevelUp() = play(Sfx.LEVEL_UP)

    /** Perfect Clear celebration sound */
    fun playPerfectClear() = play(Sfx.PERFECT_CLEAR)
    fun playHold() = play(Sfx.HOLD, 0.6f)

    /** Higher pitch for longer combos */
    fun playCombo(count: Int) = play(Sfx.COMBO, 0.8f, rate = (1f + count.coerceIn(0, 10) * 0.06f))
    fun playPieceLock() = play(Sfx.LOCK, 0.5f)
    fun playTimerWarning() = play(Sfx.TIMER_WARNING)
    fun playNewHighScore() = play(Sfx.HIGH_SCORE)

    private fun play(sfx: Sfx, gain: Float = 1f, rate: Float = 1f) {
        if (!enabled || released || soundStyle == SoundStyle.NONE) return
        val id = sampleIds[sfx] ?: return  // still loading — skip rather than block
        val v = (volume * gain).coerceIn(0f, 1f)
        try {
            soundPool.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play $sfx", e)
        }
    }

    fun release() {
        released = true
        executor.shutdownNow()
        soundPool.release()
    }

    // ===================== Loading =====================

    private fun loadStyle(style: SoundStyle) {
        if (released) return
        executor.execute {
            try {
                // Unload the previous style's samples
                val old = sampleIds.values.toList()
                sampleIds.clear()
                old.forEach { soundPool.unload(it) }
                if (style == SoundStyle.NONE) return@execute
                for (sfx in Sfx.entries) {
                    if (released || soundStyle != style) return@execute  // superseded
                    val file = File(cacheDir, "${style.name.lowercase()}_${sfx.name.lowercase()}.wav")
                    if (!file.exists() || file.length() == 0L) writeWav(file, synthesize(style, sfx))
                    sampleIds[sfx] = soundPool.load(file.path, 1)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sound style $style", e)
            }
        }
    }

    // ===================== Synthesis =====================

    private fun synthesize(style: SoundStyle, sfx: Sfx): ShortArray {
        val wave = when (style) {
            SoundStyle.RETRO_BEEP -> Wave.SQUARE
            SoundStyle.MODERN_SOFT -> Wave.SINE
            SoundStyle.ARCADE -> Wave.TRIANGLE
            SoundStyle.MECHANICAL -> Wave.NOISE
            SoundStyle.SYNTHWAVE -> Wave.SAW
            SoundStyle.NONE -> Wave.SINE
        }
        // Per-style pitch shift (semitones) and how long notes ring
        val shift = when (style) { SoundStyle.ARCADE -> 12; SoundStyle.SYNTHWAVE -> -12; else -> 0 }
        val ring = when (style) { SoundStyle.MODERN_SOFT, SoundStyle.SYNTHWAVE -> 1.6f; SoundStyle.MECHANICAL -> 0.5f; else -> 1f }
        val notes = notesFor(sfx).map { n ->
            val k = 2f.pow(shift / 12f)
            n.copy(f0 = n.f0 * k, f1 = n.f1 * k, ms = (n.ms * ring).toInt().coerceAtLeast(8))
        }
        // Mechanical: a click/noise layer over a soft triangle body keeps a recognisable pitch
        return if (wave == Wave.NOISE) mix(render(notes, Wave.TRIANGLE, 0.7f), render(notes.map { it.copy(ms = minOf(it.ms, 25)) }, Wave.NOISE, 0.35f))
        else if (style == SoundStyle.SYNTHWAVE) mix(render(notes, Wave.SAW, 0.45f), render(notes.map { it.copy(f0 = it.f0 * 1.006f, f1 = it.f1 * 1.006f) }, Wave.SAW, 0.45f))
        else render(notes, wave, if (wave == Wave.SQUARE || wave == Wave.SAW) 0.5f else 0.85f)
    }

    private fun notesFor(sfx: Sfx): List<Note> {
        val c5 = 523.25f; val e5 = 659.25f; val g5 = 783.99f; val c6 = 1046.5f; val e6 = 1318.5f; val g6 = 1568f
        return when (sfx) {
            Sfx.MOVE -> listOf(Note(660f, ms = 22, gain = 0.6f))
            Sfx.ROTATE -> listOf(Note(880f, 1100f, ms = 35, gain = 0.7f))
            Sfx.DROP -> listOf(Note(260f, 90f, ms = 80))
            Sfx.LOCK -> listOf(Note(180f, 140f, ms = 18, gain = 0.6f))
            Sfx.HOLD -> listOf(Note(587f, 700f, ms = 45, gain = 0.7f))
            Sfx.CLEAR1 -> listOf(Note(c5, ms = 50), Note(g5, ms = 70))
            Sfx.CLEAR2 -> listOf(Note(c5, ms = 45), Note(e5, ms = 45), Note(g5, ms = 80))
            Sfx.CLEAR3 -> listOf(Note(c5, ms = 40), Note(e5, ms = 40), Note(g5, ms = 40), Note(c6, ms = 100))
            Sfx.CLEAR4 -> listOf(Note(c5, ms = 40), Note(e5, ms = 40), Note(g5, ms = 40), Note(c6, ms = 40),
                Note(e6, ms = 40), Note(g6, ms = 180))
            Sfx.COMBO -> listOf(Note(e5, ms = 35), Note(g5, ms = 60))
            Sfx.LEVEL_UP -> listOf(Note(g5, ms = 70), Note(c6, ms = 70), Note(e6, ms = 70), Note(g6, ms = 200))
            Sfx.GAME_OVER -> listOf(Note(392f, ms = 140), Note(329.6f, ms = 140), Note(261.6f, ms = 140), Note(196f, 150f, ms = 420))
            Sfx.TIMER_WARNING -> listOf(Note(988f, ms = 80), Note(0f, ms = 60, gain = 0f), Note(988f, ms = 80))
            Sfx.PERFECT_CLEAR -> listOf(Note(c6, ms = 60), Note(e6, ms = 60), Note(g6, ms = 60), Note(c6 * 2, ms = 60),
                Note(g6, ms = 60), Note(c6 * 2, ms = 300))
            Sfx.HIGH_SCORE -> listOf(Note(g5, ms = 90), Note(g5, ms = 90), Note(c6, ms = 90), Note(e6, ms = 300))
        }
    }

    /** Renders notes back to back with a short attack and exponential decay per note. */
    private fun render(notes: List<Note>, wave: Wave, level: Float): ShortArray {
        val total = notes.sumOf { it.ms * SAMPLE_RATE / 1000 }
        val out = ShortArray(total)
        val rng = java.util.Random(7)
        var pos = 0
        var phase = 0.0
        for (n in notes) {
            val len = n.ms * SAMPLE_RATE / 1000
            val attack = (0.003 * SAMPLE_RATE).toInt().coerceAtMost(len / 2).coerceAtLeast(1)
            for (i in 0 until len) {
                val t = i.toDouble() / len
                val f = n.f0 + (n.f1 - n.f0) * t
                phase += f / SAMPLE_RATE
                val p = phase - kotlin.math.floor(phase)
                val v = when (wave) {
                    Wave.SINE -> sin(2 * PI * p)
                    Wave.SQUARE -> if (p < 0.5) 1.0 else -1.0
                    Wave.TRIANGLE -> 4 * kotlin.math.abs(p - 0.5) - 1
                    Wave.SAW -> 2 * p - 1
                    Wave.NOISE -> rng.nextDouble() * 2 - 1
                }
                val env = (if (i < attack) i.toDouble() / attack else 1.0) * exp(-3.0 * t)
                out[pos + i] = (v * env * n.gain * level * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            pos += len
        }
        return out
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val out = ShortArray(maxOf(a.size, b.size))
        for (i in out.indices) {
            val s = (if (i < a.size) a[i].toInt() else 0) + (if (i < b.size) b[i].toInt() else 0)
            out[i] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /** Writes 16-bit mono PCM as a WAV file (atomically, via a temp file). */
    private fun writeWav(file: File, pcm: ShortArray) {
        val dataLen = pcm.size * 2
        val buf = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()); buf.putInt(36 + dataLen); buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()); buf.putInt(16); buf.putShort(1); buf.putShort(1)
        buf.putInt(SAMPLE_RATE); buf.putInt(SAMPLE_RATE * 2); buf.putShort(2); buf.putShort(16)
        buf.put("data".toByteArray()); buf.putInt(dataLen)
        pcm.forEach { buf.putShort(it) }
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(tmp).use { it.write(buf.array()) }
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }
}
