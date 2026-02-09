package com.brickgame.tetris.gl

import android.opengl.Matrix
import com.brickgame.tetris.game.Tetris3DGame

/**
 * Orbit camera around the board center.
 * Matches the existing azimuth/elevation/pan/zoom model from the Canvas renderer.
 *
 * Coordinate system:
 *   X = board width, Z = board depth, Y = board height (up).
 *   Camera orbits around (BOARD_W/2, BOARD_H/2, BOARD_D/2).
 */
class Camera3D {
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    var azimuth = 35f
    var elevation = 25f
    var zoom = 1f
    var panX = 0f
    var panY = 0f

    // Board center
    private val targetX = Tetris3DGame.BOARD_W / 2f
    private val targetY = Tetris3DGame.BOARD_H / 2f
    private val targetZ = Tetris3DGame.BOARD_D / 2f
    private val camDistance = 22f

    /** Camera world position — used for specular calculations in fragment shader. */
    val position = FloatArray(3)

    fun setProjection(width: Int, height: Int) {
        val aspect = width.toFloat() / height
        // FOV narrows with zoom (more zoom = narrower FOV = closer feel)
        val fov = 50f / zoom.coerceIn(0.3f, 3f)
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.5f, 200f)
    }

    fun update() {
        val radAz = Math.toRadians(azimuth.toDouble())
        val radEl = Math.toRadians(elevation.toDouble().coerceIn(-85.0, 85.0))
        val cosAz = Math.cos(radAz).toFloat()
        val sinAz = Math.sin(radAz).toFloat()
        val cosEl = Math.cos(radEl).toFloat()
        val sinEl = Math.sin(radEl).toFloat()

        // Spherical coordinates → cartesian offset from target
        val eyeX = targetX + camDistance * cosEl * sinAz + panX * 0.04f
        val eyeY = targetY + camDistance * sinEl + panY * -0.04f
        val eyeZ = targetZ + camDistance * cosEl * cosAz

        position[0] = eyeX
        position[1] = eyeY
        position[2] = eyeZ

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,              // Eye
            targetX, targetY, targetZ,       // Center (board center)
            0f, 1f, 0f                       // Up
        )

        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    /**
     * Compute the Model-View-Projection matrix for an object at the given model matrix.
     * Result stored in [result].
     */
    fun getMVP(modelMatrix: FloatArray, result: FloatArray) {
        Matrix.multiplyMM(result, 0, vpMatrix, 0, modelMatrix, 0)
    }

    /** Get a copy of the current view-projection matrix. */
    fun getViewProjection(): FloatArray = vpMatrix.clone()
}
