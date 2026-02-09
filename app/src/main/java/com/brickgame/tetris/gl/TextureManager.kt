package com.brickgame.tetris.gl

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLUtils
import com.brickgame.tetris.ui.components.PieceMaterial
import kotlin.math.*

/**
 * Generates high-quality procedural textures for each material.
 * Uses value noise with octaves for realistic surface detail.
 *
 * Texture size: 256×256 ARGB, seamlessly tileable.
 */
class TextureManager {

    private val textures = mutableMapOf<PieceMaterial, Int>()
    private val SIZE = 256

    fun loadAll() {
        textures[PieceMaterial.CLASSIC] = uploadTexture(genClassic())
        textures[PieceMaterial.STONE] = uploadTexture(genStone())
        textures[PieceMaterial.GRANITE] = uploadTexture(genGranite())
        textures[PieceMaterial.GLASS] = uploadTexture(genMarble())   // GLASS → Marble
        textures[PieceMaterial.CRYSTAL] = uploadTexture(genDiamond()) // CRYSTAL → Diamond
    }

    fun bind(material: PieceMaterial) {
        val texId = textures[material] ?: textures[PieceMaterial.CLASSIC] ?: return
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
    }

    fun getParams(material: PieceMaterial): MaterialShaderParams = when (material) {
        PieceMaterial.CLASSIC -> MaterialShaderParams(
            textureStrength = 0.10f,
            specularPower = 48f,
            specularStrength = 0.35f
        )
        PieceMaterial.STONE -> MaterialShaderParams(
            textureStrength = 0.75f,
            specularPower = 6f,
            specularStrength = 0.04f
        )
        PieceMaterial.GRANITE -> MaterialShaderParams(
            textureStrength = 0.80f,
            specularPower = 12f,
            specularStrength = 0.10f
        )
        PieceMaterial.GLASS -> MaterialShaderParams( // Marble
            textureStrength = 0.70f,
            specularPower = 64f,
            specularStrength = 0.30f
        )
        PieceMaterial.CRYSTAL -> MaterialShaderParams( // Diamond
            textureStrength = 0.55f,
            specularPower = 256f,
            specularStrength = 0.90f
        )
    }

    fun cleanup() {
        val ids = textures.values.toIntArray()
        if (ids.isNotEmpty()) GLES20.glDeleteTextures(ids.size, ids, 0)
        textures.clear()
    }

    // =====================================================================
    // NOISE FOUNDATION — Tileable value noise with fractal octaves
    // =====================================================================

    /** Permutation table for noise. */
    private val perm = IntArray(512).also {
        val base = IntArray(256) { i -> i }
        val rng = java.util.Random(42)
        for (i in 255 downTo 1) { val j = rng.nextInt(i + 1); val t = base[i]; base[i] = base[j]; base[j] = t }
        for (i in 0 until 512) it[i] = base[i and 255]
    }

    /** Smooth interpolation. */
    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)

    /** Tileable 2D value noise in range [0, 1]. */
    private fun noise(x: Float, y: Float, period: Int = SIZE): Float {
        val xi = floor(x).toInt(); val yi = floor(y).toInt()
        val xf = x - xi; val yf = y - yi
        val fx = fade(xf); val fy = fade(yf)

        val x0 = (xi % period + period) % period
        val x1 = (x0 + 1) % period
        val y0 = (yi % period + period) % period
        val y1 = (y0 + 1) % period

        fun hash(ix: Int, iy: Int): Float = (perm[perm[ix and 255] + (iy and 255)] and 255) / 255f

        return lerp(
            lerp(hash(x0, y0), hash(x1, y0), fx),
            lerp(hash(x0, y1), hash(x1, y1), fx), fy
        )
    }

    /** Fractal Brownian Motion: multiple octaves of noise for natural textures. */
    private fun fbm(x: Float, y: Float, octaves: Int = 6, lacunarity: Float = 2f, gain: Float = 0.5f): Float {
        var value = 0f; var amp = 1f; var freq = 1f; var maxAmp = 0f
        for (i in 0 until octaves) {
            value += noise(x * freq, y * freq) * amp
            maxAmp += amp; amp *= gain; freq *= lacunarity
        }
        return value / maxAmp
    }

    /** Turbulence: sum of abs(noise) for rough surfaces. */
    private fun turbulence(x: Float, y: Float, octaves: Int = 5): Float {
        var value = 0f; var amp = 1f; var freq = 1f; var maxAmp = 0f
        for (i in 0 until octaves) {
            value += abs(noise(x * freq, y * freq) - 0.5f) * 2f * amp
            maxAmp += amp; amp *= 0.5f; freq *= 2f
        }
        return value / maxAmp
    }

    // =====================================================================
    // MATERIAL GENERATORS
    // =====================================================================

    /** Classic: clean glossy surface with subtle gradient, like polished plastic. */
    private fun genClassic(): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            val n = fbm(x.toFloat() * 0.03f, y.toFloat() * 0.03f, 2)
            val v = (220 + n * 35).toInt().coerceIn(200, 255)
            bmp.setPixel(x, y, Color.rgb(v, v, v))
        }
        return bmp
    }

    /**
     * Stone: rough gray surface with cracks and surface detail.
     * Multi-layer noise: base roughness + crack veins + surface pitting.
     */
    private fun genStone(): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            val fx = x.toFloat(); val fy = y.toFloat()
            // Base roughness
            val base = fbm(fx * 0.02f, fy * 0.02f, 6) * 0.6f + 0.2f
            // Crack lines — sharp high-frequency detail
            val crack = turbulence(fx * 0.05f + 100f, fy * 0.05f + 100f, 4)
            val crackMask = if (crack < 0.15f) 0.6f else 1f
            // Surface pitting
            val pit = noise(fx * 0.12f, fy * 0.12f)
            val pitMask = if (pit < 0.2f) 0.85f else 1f

            val intensity = (base * crackMask * pitMask * 255).toInt().coerceIn(45, 195)
            // Slightly warm gray
            bmp.setPixel(x, y, Color.rgb(intensity, (intensity * 0.97f).toInt(), (intensity * 0.93f).toInt()))
        }
        return bmp
    }

    /**
     * Granite: speckled gray/pink/black surface with crystal flecks.
     * Combines fine-grain noise with random bright/dark speckles.
     */
    private fun genGranite(): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val rng = java.util.Random(777)
        // Pre-generate speckle field
        val speckles = Array(SIZE) { IntArray(SIZE) { rng.nextInt(1000) } }

        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            val fx = x.toFloat(); val fy = y.toFloat()
            val base = fbm(fx * 0.025f, fy * 0.025f, 5) * 0.4f + 0.3f
            val fine = noise(fx * 0.15f, fy * 0.15f) * 0.15f

            val v = ((base + fine) * 255).toInt()
            val sp = speckles[y][x]

            val (r, g, b) = when {
                sp < 30 -> Triple(v + 55, v + 10, v + 10)   // Pink feldspar crystals
                sp < 50 -> Triple(v + 40, v + 15, v + 10)   // Warm spots
                sp < 80 -> Triple(v - 50, v - 50, v - 45)   // Dark biotite flecks
                sp < 95 -> Triple(v + 35, v + 35, v + 40)   // White quartz spots
                sp < 100 -> Triple(v + 50, v + 48, v + 55)  // Bright crystal flecks
                else -> Triple(v, (v * 0.98f).toInt(), (v * 0.95f).toInt())
            }
            bmp.setPixel(x, y, Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)))
        }
        return bmp
    }

    /**
     * Marble: white/cream base with flowing gray veins.
     * Uses domain-warped noise for organic vein patterns.
     */
    private fun genMarble(): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            val fx = x.toFloat(); val fy = y.toFloat()
            // Domain warping for organic flow
            val warpX = fbm(fx * 0.012f + 50f, fy * 0.012f + 50f, 4) * 8f
            val warpY = fbm(fx * 0.012f + 150f, fy * 0.012f + 150f, 4) * 8f
            // Main vein pattern: sinusoidal + warped noise
            val vein = sin((fx * 0.02f + warpX + fbm(fx * 0.015f + warpX, fy * 0.015f + warpY, 5) * 4f).toDouble()).toFloat()
            val veinStrength = (vein * 0.5f + 0.5f).coerceIn(0f, 1f)
            // Secondary fine veins
            val fineVein = turbulence(fx * 0.06f + warpX * 0.5f, fy * 0.06f + warpY * 0.5f, 3)

            // White base → gray veins
            val baseWhite = 235f
            val veinGray = 90f
            val fineGray = 160f
            val mainV = lerp(baseWhite, veinGray, (1f - veinStrength).pow(3f))
            val fineV = lerp(1f, 0.85f, fineVein * 0.5f)
            val v = (mainV * fineV).toInt().coerceIn(65, 248)

            // Slight warm tint
            bmp.setPixel(x, y, Color.rgb(v, (v * 0.98f).toInt(), (v * 0.95f).toInt()))
        }
        return bmp
    }

    /**
     * Diamond: brilliant sparkle with prismatic rainbow reflections.
     * Uses layered angular noise patterns simulating faceted light dispersion.
     */
    private fun genDiamond(): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            val fx = x.toFloat(); val fy = y.toFloat()
            // Faceted sparkle pattern
            val angle = atan2(fy - SIZE / 2f, fx - SIZE / 2f)
            val dist = sqrt((fx - SIZE / 2f).pow(2) + (fy - SIZE / 2f).pow(2)) / SIZE
            val facet = (sin(angle * 8.0 + dist * 20.0) * 0.5 + 0.5).toFloat()
            // Prismatic rainbow based on angle
            val rainbow = (angle / (2f * PI.toFloat()) + 1f) % 1f
            val base = fbm(fx * 0.04f, fy * 0.04f, 3)
            // Sparkle highlights
            val sparkle = noise(fx * 0.2f, fy * 0.2f)
            val sparkleHit = if (sparkle > 0.85f) 1.5f else 1f

            // Base: very bright white/blue tint
            val baseV = (200 + base * 55).toInt()
            // Rainbow dispersion overlay
            val rPhase = rainbow * 6.28f
            val rr = (sin(rPhase.toDouble()) * 20 + baseV).toInt()
            val gg = (sin((rPhase + 2.09f).toDouble()) * 20 + baseV).toInt()
            val bb = (sin((rPhase + 4.19f).toDouble()) * 25 + baseV + 10).toInt()
            // Mix in facet pattern and sparkle
            val fr = (rr * (0.8f + facet * 0.2f) * sparkleHit).toInt()
            val fg = (gg * (0.8f + facet * 0.2f) * sparkleHit).toInt()
            val fb = (bb * (0.8f + facet * 0.2f) * sparkleHit).toInt()

            bmp.setPixel(x, y, Color.rgb(fr.coerceIn(0, 255), fg.coerceIn(0, 255), fb.coerceIn(0, 255)))
        }
        return bmp
    }

    // =====================================================================
    // GL TEXTURE UPLOAD
    // =====================================================================

    private fun uploadTexture(bitmap: Bitmap): Int {
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0])

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        return texIds[0]
    }

    private fun Float.pow(n: Float): Float = Math.pow(this.toDouble(), n.toDouble()).toFloat()
}

data class MaterialShaderParams(
    val textureStrength: Float,
    val specularPower: Float,
    val specularStrength: Float
)
