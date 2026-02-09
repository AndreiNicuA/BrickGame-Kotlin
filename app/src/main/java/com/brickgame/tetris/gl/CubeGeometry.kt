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
 * CRITICAL: All faces wound counter-clockwise (CCW) when viewed from OUTSIDE the cube.
 * This is required for correct back-face culling with OpenGL's default GL_CCW front face.
 */
class CubeGeometry {

    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val indexCount: Int

    val stride = 8 * 4 // 32 bytes per vertex

    companion object {
        //                 posX  posY  posZ  nX    nY    nZ    u     v
        private val VERTICES = floatArrayOf(
            // TOP face (Y=1) — Normal (0,1,0)
            // Viewed from above: CCW order in XZ plane
            0f, 1f, 0f,   0f, 1f, 0f,   0f, 0f,
            0f, 1f, 1f,   0f, 1f, 0f,   0f, 1f,
            1f, 1f, 1f,   0f, 1f, 0f,   1f, 1f,
            1f, 1f, 0f,   0f, 1f, 0f,   1f, 0f,

            // BOTTOM face (Y=0) — Normal (0,-1,0)
            // Viewed from below: CCW order in XZ plane
            0f, 0f, 0f,   0f, -1f, 0f,  0f, 0f,
            1f, 0f, 0f,   0f, -1f, 0f,  1f, 0f,
            1f, 0f, 1f,   0f, -1f, 0f,  1f, 1f,
            0f, 0f, 1f,   0f, -1f, 0f,  0f, 1f,

            // FRONT face (Z=0) — Normal (0,0,-1)
            // Viewed from -Z: CCW in XY plane
            0f, 0f, 0f,   0f, 0f, -1f,  0f, 1f,
            0f, 1f, 0f,   0f, 0f, -1f,  0f, 0f,
            1f, 1f, 0f,   0f, 0f, -1f,  1f, 0f,
            1f, 0f, 0f,   0f, 0f, -1f,  1f, 1f,

            // BACK face (Z=1) — Normal (0,0,1)
            // Viewed from +Z: CCW in XY plane
            1f, 0f, 1f,   0f, 0f, 1f,   0f, 1f,
            1f, 1f, 1f,   0f, 0f, 1f,   0f, 0f,
            0f, 1f, 1f,   0f, 0f, 1f,   1f, 0f,
            0f, 0f, 1f,   0f, 0f, 1f,   1f, 1f,

            // LEFT face (X=0) — Normal (-1,0,0)
            // Viewed from -X: CCW in ZY plane
            0f, 0f, 0f,   -1f, 0f, 0f,  0f, 1f,
            0f, 0f, 1f,   -1f, 0f, 0f,  1f, 1f,
            0f, 1f, 1f,   -1f, 0f, 0f,  1f, 0f,
            0f, 1f, 0f,   -1f, 0f, 0f,  0f, 0f,

            // RIGHT face (X=1) — Normal (1,0,0)
            // Viewed from +X: CCW in ZY plane
            1f, 0f, 0f,   1f, 0f, 0f,   1f, 1f,
            1f, 1f, 0f,   1f, 0f, 0f,   1f, 0f,
            1f, 1f, 1f,   1f, 0f, 0f,   0f, 0f,
            1f, 0f, 1f,   1f, 0f, 0f,   0f, 1f,
        )

        // Each face: 2 triangles, index pattern (0,1,2) + (0,2,3)
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

    fun draw(shader: ShaderProgram) {
        val posLoc = shader.getAttribLocation("aPosition")
        val normalLoc = shader.getAttribLocation("aNormal")
        val uvLoc = shader.getAttribLocation("aTexCoord")

        GLES20.glEnableVertexAttribArray(posLoc)
        if (normalLoc >= 0) GLES20.glEnableVertexAttribArray(normalLoc)
        if (uvLoc >= 0) GLES20.glEnableVertexAttribArray(uvLoc)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        if (normalLoc >= 0) {
            vertexBuffer.position(3)
            GLES20.glVertexAttribPointer(normalLoc, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        }

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
