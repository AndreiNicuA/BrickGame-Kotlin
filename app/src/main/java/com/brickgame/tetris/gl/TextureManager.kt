package com.brickgame.tetris.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import com.brickgame.tetris.R
import com.brickgame.tetris.ui.components.PieceMaterial

/**
 * Loads real PBR texture images from res/raw/ resources.
 *
 * Texture sources (all CC0 / Public Domain):
 *   - Stone:   Polyhaven stone_wall (rough gray stone surface with cracks)
 *   - Granite: Polyhaven granite_tile (dark speckled granite)
 *   - Marble:  Polyhaven marble_01 (cream marble with subtle veining)
 *   - Ice:     ambientCG Ice003 (blue-white crystalline ice)
 *   - Rock:    Polyhaven rock_face (raw rock, used for diamond base)
 *
 * Classic material uses a tiny 4x4 procedural texture (nearly flat white).
 */
class TextureManager(private val context: Context) {

    private val textures = mutableMapOf<PieceMaterial, Int>()

    fun loadAll() {
        textures[PieceMaterial.CLASSIC] = uploadTexture(generateClassicFallback())
        textures[PieceMaterial.STONE] = loadFromResource(R.raw.tex_stone)
        textures[PieceMaterial.GRANITE] = loadFromResource(R.raw.tex_granite)
        textures[PieceMaterial.GLASS] = loadFromResource(R.raw.tex_marble)    // GLASS → Marble
        textures[PieceMaterial.CRYSTAL] = loadFromResource(R.raw.tex_ice)     // CRYSTAL → Ice/Diamond
    }

    fun bind(material: PieceMaterial) {
        val texId = textures[material] ?: textures[PieceMaterial.CLASSIC] ?: return
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
    }

    fun getParams(material: PieceMaterial): MaterialShaderParams = when (material) {
        PieceMaterial.CLASSIC -> MaterialShaderParams(
            textureStrength = 0.08f,     // Barely visible texture, clean glossy look
            specularPower = 48f,
            specularStrength = 0.35f
        )
        PieceMaterial.STONE -> MaterialShaderParams(
            textureStrength = 0.80f,     // Strong texture, rough matte
            specularPower = 6f,
            specularStrength = 0.04f
        )
        PieceMaterial.GRANITE -> MaterialShaderParams(
            textureStrength = 0.75f,     // Visible speckle texture
            specularPower = 14f,
            specularStrength = 0.12f
        )
        PieceMaterial.GLASS -> MaterialShaderParams(  // Marble
            textureStrength = 0.70f,     // Prominent veining
            specularPower = 64f,
            specularStrength = 0.30f
        )
        PieceMaterial.CRYSTAL -> MaterialShaderParams( // Ice/Diamond
            textureStrength = 0.65f,     // Icy crystalline texture
            specularPower = 128f,
            specularStrength = 0.70f
        )
    }

    fun cleanup() {
        val ids = textures.values.toIntArray()
        if (ids.isNotEmpty()) GLES20.glDeleteTextures(ids.size, ids, 0)
        textures.clear()
    }

    /** Load a JPEG texture from res/raw/ and upload to GL. */
    private fun loadFromResource(resId: Int): Int {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            ?: return uploadTexture(generateClassicFallback())
        return uploadTexture(bitmap)
    }

    /** Tiny white texture for Classic material (glossy plastic, no visible texture). */
    private fun generateClassicFallback(): Bitmap {
        val size = 4
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) for (x in 0 until size) {
            bmp.setPixel(x, y, android.graphics.Color.rgb(230, 230, 230))
        }
        return bmp
    }

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

data class MaterialShaderParams(
    val textureStrength: Float,
    val specularPower: Float,
    val specularStrength: Float
)
