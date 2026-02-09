package com.brickgame.tetris.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Unit cube geometry: (0,0,0) to (1,1,1).
 *
 * 24 vertices (4 per face, unique normals), 36 indices (2 triangles per face).
 * Vertex format: [posX, posY, posZ, normX, normY, normZ, u, v] — 8 floats per vertex.
 *
 * Coordinate system:
 *   X = board width axis (left/right)
 *   Y = board height axis (up, pieces fall along -Y)
 *   Z = board depth axis (front/back)
 */
class CubeGeometry {

    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val indexCount: Int

    /** Stride in bytes: 8 floats × 4 bytes = 32 */
    val stride = 8 * 4

    companion object {
        //                 posX  posY  posZ  nX    nY    nZ    u     v
        private val VERTICES = floatArrayOf(
            // TOP face (Y=1) — Normal (0,1,0)
            0f, 1f, 0f,   0f, 1f, 0f,   0f, 0f,
            1f, 1f, 0f,   0f, 1f, 0f,   1f, 0f,
            1f, 1f, 1f,   0f, 1f, 0f,   1f, 1f,
            0f, 1f, 1f,   0f, 1f, 0f,   0f, 1f,

            // BOTTOM face (Y=0) — Normal (0,-1,0)
            0f, 0f, 1f,   0f, -1f, 0f,  0f, 0f,
            1f, 0f, 1f,   0f, -1f, 0f,  1f, 0f,
            1f, 0f, 0f,   0f, -1f, 0f,  1f, 1f,
            0f, 0f, 0f,   0f, -1f, 0f,  0f, 1f,

            // FRONT face (Z=0) — Normal (0,0,-1)
            0f, 0f, 0f,   0f, 0f, -1f,  0f, 1f,
            1f, 0f, 0f,   0f, 0f, -1f,  1f, 1f,
            1f, 1f, 0f,   0f, 0f, -1f,  1f, 0f,
            0f, 1f, 0f,   0f, 0f, -1f,  0f, 0f,

            // BACK face (Z=1) — Normal (0,0,1)
            1f, 0f, 1f,   0f, 0f, 1f,   0f, 1f,
            0f, 0f, 1f,   0f, 0f, 1f,   1f, 1f,
            0f, 1f, 1f,   0f, 0f, 1f,   1f, 0f,
            1f, 1f, 1f,   0f, 0f, 1f,   0f, 0f,

            // LEFT face (X=0) — Normal (-1,0,0)
            0f, 0f, 1f,   -1f, 0f, 0f,  0f, 1f,
            0f, 0f, 0f,   -1f, 0f, 0f,  1f, 1f,
            0f, 1f, 0f,   -1f, 0f, 0f,  1f, 0f,
            0f, 1f, 1f,   -1f, 0f, 0f,  0f, 0f,

            // RIGHT face (X=1) — Normal (1,0,0)
            1f, 0f, 0f,   1f, 0f, 0f,   0f, 1f,
            1f, 0f, 1f,   1f, 0f, 0f,   1f, 1f,
            1f, 1f, 1f,   1f, 0f, 0f,   1f, 0f,
            1f, 1f, 0f,   1f, 0f, 0f,   0f, 0f,
        )

        private val INDICES = shortArrayOf(
            0, 1, 2, 0, 2, 3,         // Top
            4, 5, 6, 4, 6, 7,         // Bottom
            8, 9, 10, 8, 10, 11,      // Front
            12, 13, 14, 12, 14, 15,   // Back
            16, 17, 18, 16, 18, 19,   // Left
            20, 21, 22, 20, 22, 23    // Right
        )
    }

    init {
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(VERTICES).also { it.position(0) }

        indexBuffer = ByteBuffer.allocateDirect(INDICES.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
            .put(INDICES).also { it.position(0) }

        indexCount = INDICES.size
    }

    /**
     * Bind vertex attributes and draw the cube.
     * The caller must have already called shader.use() and set all uniforms.
     */
    fun draw(shader: ShaderProgram) {
        val posLoc = shader.getAttribLocation("aPosition")
        val normalLoc = shader.getAttribLocation("aNormal")
        val uvLoc = shader.getAttribLocation("aTexCoord")

        GLES20.glEnableVertexAttribArray(posLoc)
        if (normalLoc >= 0) GLES20.glEnableVertexAttribArray(normalLoc)
        if (uvLoc >= 0) GLES20.glEnableVertexAttribArray(uvLoc)

        // Position: offset 0
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        // Normal: offset 3 floats
        if (normalLoc >= 0) {
            vertexBuffer.position(3)
            GLES20.glVertexAttribPointer(normalLoc, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        }

        // UV: offset 6 floats
        if (uvLoc >= 0) {
            vertexBuffer.position(6)
            GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        }

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(posLoc)
        if (normalLoc >= 0) GLES20.glDisableVertexAttribArray(normalLoc)
        if (uvLoc >= 0) GLES20.glDisableVertexAttribArray(uvLoc)
    }
}
