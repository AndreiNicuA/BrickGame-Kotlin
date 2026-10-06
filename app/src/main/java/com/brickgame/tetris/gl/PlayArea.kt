package com.brickgame.tetris.gl

import kotlin.math.hypot

/**
 * The player's safe floor area for AR: a polygon on the floor (x, z in metres, any winding).
 * Pure Kotlin so it can be unit-tested.
 */
class PlayArea(xs: List<Float>, zs: List<Float>) {
    val x: FloatArray = xs.toFloatArray()
    val z: FloatArray = zs.toFloatArray()
    val size get() = x.size

    init { require(xs.size == zs.size) }

    /** Point-in-polygon (even-odd rule). Needs at least 3 corners. */
    fun contains(px: Float, pz: Float): Boolean {
        if (size < 3) return false
        var inside = false
        var j = size - 1
        for (i in 0 until size) {
            if ((z[i] > pz) != (z[j] > pz) &&
                px < (x[j] - x[i]) * (pz - z[i]) / (z[j] - z[i]) + x[i]) inside = !inside
            j = i
        }
        return inside
    }

    /** Distance from the point to the nearest edge of the polygon, in metres. */
    fun distanceToEdge(px: Float, pz: Float): Float {
        if (size < 2) return Float.MAX_VALUE
        var best = Float.MAX_VALUE
        var j = size - 1
        for (i in 0 until size) {
            best = minOf(best, segmentDistance(px, pz, x[j], z[j], x[i], z[i]))
            j = i
        }
        return best
    }

    /** How far inside the area the point is (negative = outside by that much). */
    fun signedDistance(px: Float, pz: Float): Float {
        val d = distanceToEdge(px, pz)
        return if (contains(px, pz)) d else -d
    }

    /** True if every point is inside the area. */
    fun containsAll(points: List<Pair<Float, Float>>) = points.all { (px, pz) -> contains(px, pz) }

    companion object {
        fun segmentDistance(px: Float, pz: Float, ax: Float, az: Float, bx: Float, bz: Float): Float {
            val dx = bx - ax; val dz = bz - az
            val len2 = dx * dx + dz * dz
            val t = if (len2 < 1e-9f) 0f else (((px - ax) * dx + (pz - az) * dz) / len2).coerceIn(0f, 1f)
            return hypot(px - (ax + t * dx), pz - (az + t * dz))
        }
    }
}
