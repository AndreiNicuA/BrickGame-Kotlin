package com.brickgame.tetris.gl

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class TiltMathTest {

    private fun rotX(a: Double) = floatArrayOf(
        1f, 0f, 0f,
        0f, cos(a).toFloat(), -sin(a).toFloat(),
        0f, sin(a).toFloat(), cos(a).toFloat())

    private fun rotY(a: Double) = floatArrayOf(
        cos(a).toFloat(), 0f, sin(a).toFloat(),
        0f, 1f, 0f,
        -sin(a).toFloat(), 0f, cos(a).toFloat())

    private fun mul(a: FloatArray, b: FloatArray) = FloatArray(9) { i ->
        val r = i / 3; val c = i % 3
        a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c]
    }

    /** Phone held upright in portrait, facing north-east: an arbitrary non-trivial reference. */
    private val upright = mul(rotY(0.8), rotX(Math.PI / 2 - 0.2))

    private fun angles(ref: FloatArray, local: FloatArray): FloatArray {
        val out = FloatArray(2)
        TiltMath.relativeYawPitch(ref, mul(ref, local), out)
        return out
    }

    @Test fun `no movement gives zero`() {
        val a = angles(upright, rotY(0.0))
        assertEquals(0f, a[0], 1e-5f); assertEquals(0f, a[1], 1e-5f)
    }

    @Test fun `turning around the screen's up axis is yaw`() {
        val a = angles(upright, rotY(0.5))
        assertEquals(0.5f, a[0], 1e-4f); assertEquals(0f, a[1], 1e-4f)
    }

    @Test fun `tilting around the screen's right axis is pitch`() {
        val a = angles(upright, rotX(-0.3))
        assertEquals(0f, a[0], 1e-4f); assertEquals(-0.3f, a[1], 1e-4f)
    }

    @Test fun `combined yaw and pitch separate cleanly`() {
        val a = angles(upright, mul(rotY(-0.6), rotX(0.4)))
        assertEquals(-0.6f, a[0], 1e-4f); assertEquals(0.4f, a[1], 1e-4f)
    }

    @Test fun `works with the phone lying flat too`() {
        val flat = rotY(2.0)  // any heading, screen up
        val a = angles(flat, mul(rotY(0.25), rotX(0.15)))
        assertEquals(0.25f, a[0], 1e-4f); assertEquals(0.15f, a[1], 1e-4f)
    }

    @Test fun `wrap degrees`() {
        assertEquals(-170f, TiltMath.wrapDegrees(190f), 1e-4f)
        assertEquals(180f, TiltMath.wrapDegrees(-180f), 1e-4f)
        assertEquals(10f, TiltMath.wrapDegrees(370f), 1e-4f)
    }
}
