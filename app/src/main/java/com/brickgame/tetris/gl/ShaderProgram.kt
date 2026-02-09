package com.brickgame.tetris.gl

import android.content.Context
import android.opengl.GLES20
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Compiles and links a vertex+fragment shader pair from raw resources.
 * Provides helpers for setting uniforms and getting attribute locations.
 */
class ShaderProgram(
    context: Context,
    vertexResId: Int,
    fragmentResId: Int
) {
    val programId: Int

    init {
        val vertSrc = readRawResource(context, vertexResId)
        val fragSrc = readRawResource(context, fragmentResId)
        val vertShader = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val fragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)

        programId = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vertShader)
            GLES20.glAttachShader(prog, fragShader)
            GLES20.glLinkProgram(prog)

            val status = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(prog)
                GLES20.glDeleteProgram(prog)
                throw RuntimeException("Shader link failed: $log")
            }
        }
        GLES20.glDeleteShader(vertShader)
        GLES20.glDeleteShader(fragShader)
    }

    fun use() = GLES20.glUseProgram(programId)

    fun getAttribLocation(name: String): Int = GLES20.glGetAttribLocation(programId, name)
    fun getUniformLocation(name: String): Int = GLES20.glGetUniformLocation(programId, name)

    fun setUniform1f(name: String, value: Float) {
        GLES20.glUniform1f(getUniformLocation(name), value)
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        GLES20.glUniform3f(getUniformLocation(name), x, y, z)
    }

    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        GLES20.glUniform4f(getUniformLocation(name), x, y, z, w)
    }

    fun setUniformMatrix4fv(name: String, matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(getUniformLocation(name), 1, false, matrix, 0)
    }

    fun setUniform1i(name: String, value: Int) {
        GLES20.glUniform1i(getUniformLocation(name), value)
    }

    companion object {
        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader compile failed (${ if (type == GLES20.GL_VERTEX_SHADER) "vertex" else "fragment" }): $log")
            }
            return shader
        }

        private fun readRawResource(context: Context, resId: Int): String {
            val sb = StringBuilder()
            context.resources.openRawResource(resId).use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    reader.lineSequence().forEach { sb.appendLine(it) }
                }
            }
            return sb.toString()
        }
    }
}
