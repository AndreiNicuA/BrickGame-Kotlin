package com.brickgame.tetris.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Draws the AR play-area boundary: an outline on the floor and a grid "wall" along its edges
 * (like a VR headset's guardian). Coordinates are in the boundary anchor's space (metres,
 * y = 0 is the floor). Call [init] on the GL thread before drawing.
 */
class BoundaryRenderer {

    companion object {
        const val WALL_HEIGHT = 2.0f
        private const val POST_SPACING = 0.3f
        private const val RAIL_SPACING = 0.4f
    }

    private var program = 0
    private var aPos = 0
    private var uMvp = 0
    private var uColor = 0
    private var buffer: FloatBuffer = alloc(1024)

    fun init() {
        program = buildProgram(
            """
            uniform mat4 uMVP;
            attribute vec3 aPos;
            void main() { gl_Position = uMVP * vec4(aPos, 1.0); }
            """.trimIndent(),
            """
            precision mediump float;
            uniform vec4 uColor;
            void main() { gl_FragColor = uColor; }
            """.trimIndent()
        )
        if (program != 0) {
            aPos = GLES20.glGetAttribLocation(program, "aPos")
            uMvp = GLES20.glGetUniformLocation(program, "uMVP")
            uColor = GLES20.glGetUniformLocation(program, "uColor")
        }
    }

    /**
     * @param xs/zs corners in anchor space; [closed] joins the last corner to the first.
     * @param wallAlpha 0 = floor outline only, 1 = full wall.
     * @param cursor optional (x, z) where the next corner would go (drawn while setting up).
     */
    fun draw(
        mvp: FloatArray, xs: FloatArray, zs: FloatArray, n: Int, closed: Boolean,
        rgb: FloatArray, outlineAlpha: Float, wallAlpha: Float, wallHeight: Float = WALL_HEIGHT,
        cursor: FloatArray? = null
    ) {
        if (program == 0 || (n == 0 && cursor == null)) return
        val floor = ArrayList<Float>()   // GL_LINES pairs
        val wall = ArrayList<Float>()
        fun seg(list: ArrayList<Float>, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) {
            list.add(x1); list.add(y1); list.add(z1); list.add(x2); list.add(y2); list.add(z2)
        }
        val edges = if (closed && n >= 3) n else n - 1
        for (e in 0 until edges.coerceAtLeast(0)) {
            val i = e; val j = (e + 1) % n
            seg(floor, xs[i], 0.01f, zs[i], xs[j], 0.01f, zs[j])
            if (wallAlpha > 0.01f) {
                val dx = xs[j] - xs[i]; val dz = zs[j] - zs[i]
                val len = kotlin.math.sqrt(dx * dx + dz * dz)
                val posts = (len / POST_SPACING).toInt().coerceAtLeast(1)
                for (p in 0..posts) {
                    val t = p.toFloat() / posts
                    val px = xs[i] + dx * t; val pz = zs[i] + dz * t
                    seg(wall, px, 0f, pz, px, wallHeight, pz)
                }
                var y = RAIL_SPACING
                while (y <= wallHeight + 1e-3f) { seg(wall, xs[i], y, zs[i], xs[j], y, zs[j]); y += RAIL_SPACING }
            }
        }
        if (cursor != null) {
            val cx = cursor[0]; val cz = cursor[1]; val r = 0.08f
            seg(floor, cx - r, 0.01f, cz, cx + r, 0.01f, cz)
            seg(floor, cx, 0.01f, cz - r, cx, 0.01f, cz + r)
            if (n > 0) seg(floor, xs[n - 1], 0.01f, zs[n - 1], cx, 0.01f, cz)
        }

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnableVertexAttribArray(aPos)
        if (wall.isNotEmpty()) drawLines(wall, rgb, wallAlpha * 0.75f, 2f)
        if (floor.isNotEmpty()) drawLines(floor, rgb, outlineAlpha, 5f)
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun drawLines(v: ArrayList<Float>, rgb: FloatArray, alpha: Float, width: Float) {
        if (buffer.capacity() < v.size) buffer = alloc(v.size * 2)
        buffer.clear()
        for (f in v) buffer.put(f)
        buffer.flip()
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, buffer)
        GLES20.glUniform4f(uColor, rgb[0], rgb[1], rgb[2], alpha.coerceIn(0f, 1f))
        GLES20.glLineWidth(width)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, v.size / 3)
        GLES20.glLineWidth(1f)
    }

    private fun alloc(floats: Int): FloatBuffer =
        ByteBuffer.allocateDirect(floats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private fun buildProgram(vs: String, fs: String): Int {
        fun compile(type: Int, src: String): Int {
            val sh = GLES20.glCreateShader(type)
            GLES20.glShaderSource(sh, src); GLES20.glCompileShader(sh)
            val ok = IntArray(1); GLES20.glGetShaderiv(sh, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) { GLES20.glDeleteShader(sh); return 0 }
            return sh
        }
        val v = compile(GLES20.GL_VERTEX_SHADER, vs); val f = compile(GLES20.GL_FRAGMENT_SHADER, fs)
        if (v == 0 || f == 0) return 0
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v); GLES20.glAttachShader(p, f); GLES20.glLinkProgram(p)
        val ok = IntArray(1); GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        return if (ok[0] == 0) 0 else p
    }
}
