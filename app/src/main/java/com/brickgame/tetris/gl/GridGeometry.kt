package com.brickgame.tetris.gl

import android.opengl.GLES20
import com.brickgame.tetris.game.Tetris3DGame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Pre-built line geometry for the board wireframe:
 *   - Floor grid lines
 *   - Board frame edges (vertical pillars, top/bottom rectangles)
 *   - Semi-transparent wall quads (drawn as 2 triangles each)
 */
class GridGeometry {

    private val floorBuffer: FloatBuffer
    private val floorVertexCount: Int

    private val frameBuffer: FloatBuffer
    private val frameVertexCount: Int

    private val frameDimBuffer: FloatBuffer
    private val frameDimVertexCount: Int

    private val bw = Tetris3DGame.BOARD_W.toFloat()
    private val bd = Tetris3DGame.BOARD_D.toFloat()
    private val bh = Tetris3DGame.BOARD_H.toFloat()

    init {
        // Floor grid: lines along X and Z at Y=0
        val floorVerts = mutableListOf<Float>()
        for (x in 0..Tetris3DGame.BOARD_W) {
            floorVerts.addAll(listOf(x.toFloat(), 0f, 0f, x.toFloat(), 0f, bd))
        }
        for (z in 0..Tetris3DGame.BOARD_D) {
            floorVerts.addAll(listOf(0f, 0f, z.toFloat(), bw, 0f, z.toFloat()))
        }
        floorVertexCount = floorVerts.size / 3
        floorBuffer = allocBuffer(floorVerts.toFloatArray())

        // Bright frame edges: vertical pillars + top rectangle
        val frameVerts = mutableListOf<Float>()
        // Vertical pillars
        for ((x, z) in listOf(0f to 0f, bw to 0f, 0f to bd, bw to bd)) {
            frameVerts.addAll(listOf(x, 0f, z, x, bh, z))
        }
        // Top rectangle
        frameVerts.addAll(listOf(0f, bh, 0f, bw, bh, 0f))
        frameVerts.addAll(listOf(0f, bh, bd, bw, bh, bd))
        frameVerts.addAll(listOf(0f, bh, 0f, 0f, bh, bd))
        frameVerts.addAll(listOf(bw, bh, 0f, bw, bh, bd))
        frameVertexCount = frameVerts.size / 3
        frameBuffer = allocBuffer(frameVerts.toFloatArray())

        // Dim frame edges: bottom rectangle
        val dimVerts = mutableListOf<Float>()
        dimVerts.addAll(listOf(0f, 0f, 0f, bw, 0f, 0f))
        dimVerts.addAll(listOf(0f, 0f, bd, bw, 0f, bd))
        dimVerts.addAll(listOf(0f, 0f, 0f, 0f, 0f, bd))
        dimVerts.addAll(listOf(bw, 0f, 0f, bw, 0f, bd))
        frameDimVertexCount = dimVerts.size / 3
        frameDimBuffer = allocBuffer(dimVerts.toFloatArray())
    }

    /**
     * Draw the complete grid using the given shader.
     * Shader must have: aPosition attribute, uMVPMatrix + uColor uniforms.
     */
    fun draw(shader: ShaderProgram, vpMatrix: FloatArray) {
        shader.use()
        shader.setUniformMatrix4fv("uMVPMatrix", vpMatrix)
        val posLoc = shader.getAttribLocation("aPosition")
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glLineWidth(1.0f)

        // Floor grid — subtle
        shader.setUniform4f("uColor", 1f, 1f, 1f, 0.06f)
        floorBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, floorBuffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, floorVertexCount)

        // Bright frame — top edges + pillars
        GLES20.glLineWidth(1.5f)
        shader.setUniform4f("uColor", 1f, 1f, 1f, 0.22f)
        frameBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, frameBuffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, frameVertexCount)

        // Dim frame — bottom edges
        shader.setUniform4f("uColor", 1f, 1f, 1f, 0.10f)
        frameDimBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, frameDimBuffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, frameDimVertexCount)

        GLES20.glLineWidth(1.0f)
        GLES20.glDisableVertexAttribArray(posLoc)
    }

    private fun allocBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(data).also { it.position(0) }
    }
}
