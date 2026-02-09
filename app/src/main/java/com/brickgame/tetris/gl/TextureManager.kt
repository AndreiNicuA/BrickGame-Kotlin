package com.brickgame.tetris.gl

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLUtils
import com.brickgame.tetris.ui.components.PieceMaterial
import kotlin.math.abs
import kotlin.math.sin

/**
 * Generates procedural textures at startup and manages GL texture objects.
 * No PNG assets required — all textures are created programmatically.
 *
 * Each material texture is 128×128 ARGB, seamlessly tileable.
 */
class TextureManager {

    private val textures = mutableMapOf<PieceMaterial, Int>()

    /** Generate all textures and upload to GPU. Call from onSurfaceCreated. */
    fun loadAll() {
        textures[PieceMaterial.CLASSIC] = uploadTexture(generateClassic())
        textures[PieceMaterial.STONE] = uploadTexture(generateStone())
        textures[PieceMaterial.GRANITE] = uploadTexture(generateGranite())
        textures[PieceMaterial.GLASS] = uploadTexture(generateGlass())
        textures[PieceMaterial.CRYSTAL] = uploadTexture(generateCrystal())
    }

    /** Bind the texture for a given material to GL_TEXTURE0. */
    fun bind(material: PieceMaterial) {
        val texId = textures[material] ?: textures[PieceMaterial.CLASSIC] ?: return
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
    }

    /** Get shader parameters for each material. */
    fun getParams(material: PieceMaterial): MaterialShaderParams = when (material) {
        PieceMaterial.CLASSIC -> MaterialShaderParams(
            textureStrength = 0.12f,
            specularPower = 32f,
            specularStrength = 0.22f,
            transparency = 1.0f
        )
        PieceMaterial.STONE -> MaterialShaderParams(
            textureStrength = 0.8f,
            specularPower = 8f,
            specularStrength = 0.05f,
            transparency = 1.0f
        )
        PieceMaterial.GRANITE -> MaterialShaderParams(
            textureStrength = 0.85f,
            specularPower = 16f,
            specularStrength = 0.12f,
            transparency = 1.0f
        )
        PieceMaterial.GLASS -> MaterialShaderParams(
            textureStrength = 0.25f,
            specularPower = 128f,
            specularStrength = 0.8f,
            transparency = 0.55f
        )
        PieceMaterial.CRYSTAL -> MaterialShaderParams(
            textureStrength = 0.35f,
            specularPower = 256f,
            specularStrength = 1.0f,
            transparency = 0.45f
        )
    }

    fun cleanup() {
        val ids = textures.values.toIntArray()
        if (ids.isNotEmpty()) GLES20.glDeleteTextures(ids.size, ids, 0)
        textures.clear()
    }

    // ============ Procedural Texture Generators ============

    private fun generateClassic(size: Int = 128): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        // Subtle bevel: center is slightly brighter than edges
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = (x.toFloat() / size - 0.5f) * 2f
                val dy = (y.toFloat() / size - 0.5f) * 2f
                val dist = (dx * dx + dy * dy).coerceAtMost(1f)
                val v = (210 + (1f - dist) * 40).toInt().coerceIn(180, 255)
                bmp.setPixel(x, y, Color.argb(255, v, v, v))
            }
        }
        return bmp
    }

    private fun generateStone(size: Int = 128): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = java.util.Random(42)
        // Pre-generate a noise field
        val noise = Array(size) { IntArray(size) { rng.nextInt(60) } }
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Low-frequency noise via sampling neighbors
                val n0 = noise[y][x]
                val n1 = noise[(y + 4) % size][(x + 3) % size]
                val n2 = noise[(y + 7) % size][(x + 11) % size]
                val base = 100 + (n0 + n1) / 4
                // Crack lines at specific Y positions
                val crack = if (abs(y % 37 - 18) < 1 || abs(x % 43 - 21) < 1) -25 else 0
                val v = (base + crack + n2 / 8).coerceIn(60, 190)
                bmp.setPixel(x, y, Color.argb(255, v, v - 3, v - 8))
            }
        }
        return bmp
    }

    private fun generateGranite(size: Int = 128): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = java.util.Random(123)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val base = 140 + rng.nextInt(40)
                // Speckles: occasional pink, black, or white spots
                val speckle = rng.nextInt(100)
                val (r, g, b) = when {
                    speckle < 5 -> Triple(base + 40, base - 10, base - 10) // Pink
                    speckle < 10 -> Triple(base - 40, base - 40, base - 35) // Dark
                    speckle < 13 -> Triple(base + 30, base + 30, base + 30) // Light
                    else -> Triple(base, base - 5, base - 2)
                }
                bmp.setPixel(x, y, Color.argb(255,
                    r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)))
            }
        }
        return bmp
    }

    private fun generateGlass(size: Int = 128): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Nearly white with subtle diagonal reflection streaks
                val diag = (x + y).toFloat()
                val streak1 = (sin(diag * 0.15) * 0.5 + 0.5).toFloat()
                val streak2 = (sin(diag * 0.08 + 2.0) * 0.5 + 0.5).toFloat()
                val v = (220 + (streak1 * 20 + streak2 * 10)).toInt().coerceIn(210, 255)
                bmp.setPixel(x, y, Color.argb(255, v, v, (v + 5).coerceAtMost(255)))
            }
        }
        return bmp
    }

    private fun generateCrystal(size: Int = 128): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Rainbow prismatic bands at 45 degrees
                val t = ((x + y) % size).toFloat() / size
                val r = (sin(t * 6.28 + 0.0) * 30 + 210).toInt().coerceIn(180, 255)
                val g = (sin(t * 6.28 + 2.09) * 30 + 210).toInt().coerceIn(180, 255)
                val b = (sin(t * 6.28 + 4.19) * 30 + 215).toInt().coerceIn(180, 255)
                bmp.setPixel(x, y, Color.argb(255, r, g, b))
            }
        }
        return bmp
    }

    // ============ GL Texture Upload ============

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
}

/** Shader parameters per material. */
data class MaterialShaderParams(
    val textureStrength: Float,
    val specularPower: Float,
    val specularStrength: Float,
    val transparency: Float
)
