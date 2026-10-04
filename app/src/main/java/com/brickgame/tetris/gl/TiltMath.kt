package com.brickgame.tetris.gl

import kotlin.math.atan2

/**
 * Pure math for the motion ("phone as a window") camera. No Android dependencies so it can be
 * unit-tested on the JVM.
 *
 * Input matrices are 3×3 row-major rotation matrices as produced by
 * SensorManager.getRotationMatrixFromVector: R maps device coordinates to world coordinates.
 * Device axes: +X = screen right, +Y = screen up, +Z = out of the screen towards the viewer.
 */
object TiltMath {

    /**
     * How far the phone has turned since [ref], in its own frame.
     * Writes into [out]: out[0] = yaw (radians, turning around the screen's up axis; positive =
     * turned left), out[1] = pitch (radians, around the screen's right axis; positive = top edge
     * tilted towards the viewer).
     *
     * Uses Q = refᵀ · cur (the current orientation expressed in the reference device frame) and
     * decomposes Q ≈ RotY(yaw) · RotX(pitch). Valid in any holding position (upright, flat, …)
     * because everything is relative to the reference, so there is no gimbal problem at the
     * usual upright portrait pose.
     */
    fun relativeYawPitch(ref: FloatArray, cur: FloatArray, out: FloatArray) {
        // Q[i][j] = Σk ref[k][i] * cur[k][j]  (row-major: m[r*3+c])
        fun q(i: Int, j: Int): Float =
            ref[i] * cur[j] + ref[3 + i] * cur[3 + j] + ref[6 + i] * cur[6 + j]

        out[0] = atan2(q(0, 2), q(2, 2))
        out[1] = atan2(-q(1, 2), q(1, 1))
    }

    /** Wraps an angle in degrees to (-180, 180]. */
    fun wrapDegrees(d: Float): Float {
        var a = d % 360f
        if (a > 180f) a -= 360f
        if (a <= -180f) a += 360f
        return a
    }
}
