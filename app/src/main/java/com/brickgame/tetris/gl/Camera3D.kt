package com.brickgame.tetris.gl

import android.opengl.Matrix
import com.brickgame.tetris.game.Tetris3DGame

/**
 * Orbit camera around the board center.
 * Zoom works by changing distance (not FOV) for a more natural feel.
 */
class Camera3D {
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    var azimuth = 35f
    var elevation = 25f
    var zoom = 1f
    var panX = 0f
    var panY = 0f

    private val targetX = Tetris3DGame.BOARD_W / 2f
    private val targetY = Tetris3DGame.BOARD_H / 3f
    private val targetZ = Tetris3DGame.BOARD_D / 2f
    private val baseCamDistance = 24f

    val position = FloatArray(3)

    private var lastWidth = 1
    private var lastHeight = 1

    fun setProjection(width: Int, height: Int) {
        lastWidth = width
        lastHeight = height
        val aspect = width.toFloat() / height
        // Fixed FOV — zoom is handled by camera distance
        Matrix.perspectiveM(projMatrix, 0, 45f, aspect, 0.5f, 200f)
    }

    fun update() {
        val radAz = Math.toRadians(azimuth.toDouble())
        val clampedEl = elevation.toDouble().coerceIn(-80.0, 80.0)
        val radEl = Math.toRadians(clampedEl)
        val cosAz = Math.cos(radAz).toFloat()
        val sinAz = Math.sin(radAz).toFloat()
        val cosEl = Math.cos(radEl).toFloat()
        val sinEl = Math.sin(radEl).toFloat()

        // Zoom via distance: zoom=1 → baseDist, zoom=2 → closer, zoom=0.5 → farther
        val dist = baseCamDistance / zoom.coerceIn(0.3f, 3f)

        val eyeX = targetX + dist * cosEl * sinAz + panX * 0.04f
        val eyeY = targetY + dist * sinEl - panY * 0.04f
        val eyeZ = targetZ + dist * cosEl * cosAz

        position[0] = eyeX
        position[1] = eyeY
        position[2] = eyeZ

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            targetX, targetY, targetZ,
            0f, 1f, 0f
        )

        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    fun getMVP(modelMatrix: FloatArray, result: FloatArray) {
        Matrix.multiplyMM(result, 0, vpMatrix, 0, modelMatrix, 0)
    }

    fun getViewProjection(): FloatArray = vpMatrix.clone()
}
