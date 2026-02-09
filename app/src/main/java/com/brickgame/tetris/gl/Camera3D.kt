package com.brickgame.tetris.gl

import android.opengl.Matrix
import com.brickgame.tetris.game.Tetris3DGame

/**
 * Orbit camera around the board center.
 *
 * Coordinate system matches the game: X=width, Y=height(up), Z=depth.
 * Camera orbits in spherical coordinates around (BOARD_W/2, BOARD_H/3, BOARD_D/2).
 * Target is at 1/3 height so the board floor and lower pieces are more visible.
 */
class Camera3D {
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    var azimuth = 35f     // Horizontal orbit angle (degrees)
    var elevation = 25f   // Vertical angle (degrees, 0=level, 90=top-down)
    var zoom = 1f         // Zoom factor (affects FOV)
    var panX = 0f         // Screen-space pan
    var panY = 0f

    // Orbit target: slightly below board center for better framing
    private val targetX = Tetris3DGame.BOARD_W / 2f
    private val targetY = Tetris3DGame.BOARD_H / 3f  // 1/3 up = shows floor well
    private val targetZ = Tetris3DGame.BOARD_D / 2f
    private val camDistance = 24f

    /** Camera world position (for specular calculations). */
    val position = FloatArray(3)

    private var lastWidth = 1
    private var lastHeight = 1

    fun setProjection(width: Int, height: Int) {
        lastWidth = width
        lastHeight = height
        rebuildProjection()
    }

    /** Rebuild projection matrix with current zoom. */
    fun rebuildProjection() {
        val aspect = lastWidth.toFloat() / lastHeight
        val fov = 45f / zoom.coerceIn(0.3f, 3f)
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.5f, 200f)
    }

    fun update() {
        val radAz = Math.toRadians(azimuth.toDouble())
        // Clamp elevation to avoid gimbal lock at poles
        val clampedEl = elevation.toDouble().coerceIn(-80.0, 80.0)
        val radEl = Math.toRadians(clampedEl)
        val cosAz = Math.cos(radAz).toFloat()
        val sinAz = Math.sin(radAz).toFloat()
        val cosEl = Math.cos(radEl).toFloat()
        val sinEl = Math.sin(radEl).toFloat()

        // Spherical → cartesian: camera position relative to target
        val eyeX = targetX + camDistance * cosEl * sinAz + panX * 0.04f
        val eyeY = targetY + camDistance * sinEl - panY * 0.04f
        val eyeZ = targetZ + camDistance * cosEl * cosAz

        position[0] = eyeX
        position[1] = eyeY
        position[2] = eyeZ

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            targetX, targetY, targetZ,
            0f, 1f, 0f
        )

        // Rebuild projection in case zoom changed
        rebuildProjection()

        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    fun getMVP(modelMatrix: FloatArray, result: FloatArray) {
        Matrix.multiplyMM(result, 0, vpMatrix, 0, modelMatrix, 0)
    }

    fun getViewProjection(): FloatArray = vpMatrix.clone()
}
