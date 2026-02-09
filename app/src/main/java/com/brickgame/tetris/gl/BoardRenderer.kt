package com.brickgame.tetris.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.brickgame.tetris.R
import com.brickgame.tetris.game.*
import com.brickgame.tetris.ui.components.PieceMaterial
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class BoardRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var cubeShader: ShaderProgram
    private lateinit var ghostShader: ShaderProgram
    private lateinit var gridShader: ShaderProgram
    private lateinit var cube: CubeGeometry
    private lateinit var grid: GridGeometry
    private lateinit var textures: TextureManager
    private val camera = Camera3D()

    @Volatile private var currentState: Game3DState = Game3DState()
    @Volatile private var currentMaterial: PieceMaterial = PieceMaterial.CLASSIC
    @Volatile private var showGhost: Boolean = true
    @Volatile private var themeColorArgb: Long = 0xFF22C55EL
    @Volatile private var bgColorArgb: Long = 0xFF0A0A0AL

    private var viewportWidth = 1
    private var viewportHeight = 1

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val lightDir = floatArrayOf(0.35f, 0.75f, -0.55f)

    fun updateState(state: Game3DState, material: PieceMaterial, ghost: Boolean, themeColor: Long, bgColor: Long = 0xFF0A0A0AL) {
        currentState = state; currentMaterial = material; showGhost = ghost; themeColorArgb = themeColor; bgColorArgb = bgColor
    }

    fun updateCamera(azimuth: Float, elevation: Float, panX: Float, panY: Float, zoom: Float) {
        camera.azimuth = azimuth; camera.elevation = elevation
        camera.panX = panX; camera.panY = panY; camera.zoom = zoom
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.04f, 0.04f, 0.04f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW)

        cubeShader = ShaderProgram(context, R.raw.cube_vertex, R.raw.cube_fragment)
        ghostShader = ShaderProgram(context, R.raw.cube_vertex, R.raw.ghost_fragment)
        gridShader = ShaderProgram(context, R.raw.grid_vertex, R.raw.grid_fragment)

        cube = CubeGeometry()
        grid = GridGeometry()
        textures = TextureManager()
        textures.loadAll()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width; viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        camera.setProjection(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val bgR = ((bgColorArgb shr 16) and 0xFF) / 255f
        val bgG = ((bgColorArgb shr 8) and 0xFF) / 255f
        val bgB = (bgColorArgb and 0xFF) / 255f
        GLES20.glClearColor(bgR, bgG, bgB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        camera.update()
        val state = currentState
        val material = currentMaterial
        val matParams = textures.getParams(material)

        // Grid (needs blending for alpha lines)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        grid.draw(gridShader, camera.getViewProjection())
        GLES20.glDisable(GLES20.GL_BLEND)

        // Solid cubes (NO blending)
        cubeShader.use()
        textures.bind(material)
        cubeShader.setUniform1i("uTexture", 0)
        cubeShader.setUniform3f("uLightDir", lightDir[0], lightDir[1], lightDir[2])
        cubeShader.setUniform3f("uCameraPos", camera.position[0], camera.position[1], camera.position[2])
        cubeShader.setUniform1f("uTextureStrength", matParams.textureStrength)
        cubeShader.setUniform1f("uSpecularPower", matParams.specularPower)
        cubeShader.setUniform1f("uSpecularStrength", matParams.specularStrength)

        // Board blocks
        if (state.board.isNotEmpty()) {
            for (y in 0 until Tetris3DGame.BOARD_H) {
                val clearing = if (y in state.clearingLayers) state.clearAnimProgress else 0f
                for (z in 0 until Tetris3DGame.BOARD_D) {
                    for (x in 0 until Tetris3DGame.BOARD_W) {
                        if (y < state.board.size && z < state.board[y].size && x < state.board[y][z].size) {
                            val ci = state.board[y][z][x]
                            if (ci > 0) drawCube(x.toFloat(), y.toFloat(), z.toFloat(), pieceColorRGB(ci), 1f, clearing)
                        }
                    }
                }
            }
        }

        // Current piece (brighter)
        val piece = state.currentPiece
        if (piece != null) {
            val rgb = pieceColorRGB(piece.type.colorIndex)
            val bright = floatArrayOf((rgb[0] * 1.2f).coerceAtMost(1f), (rgb[1] * 1.2f).coerceAtMost(1f), (rgb[2] * 1.2f).coerceAtMost(1f))
            for (b in piece.blocks) {
                val px = piece.x + b.x; val py = piece.y + b.y; val pz = piece.z + b.z
                if (py >= 0) drawCube(px.toFloat(), py.toFloat(), pz.toFloat(), bright, 1f, 0f)
            }
        }

        // Ghost piece (transparent)
        if (showGhost && piece != null && state.ghostY < piece.y) {
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glDepthMask(false)
            drawGhost(state, piece)
            GLES20.glDepthMask(true)
            GLES20.glDisable(GLES20.GL_BLEND)
        }
    }

    private fun drawCube(x: Float, y: Float, z: Float, rgb: FloatArray, alpha: Float, clearing: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        camera.getMVP(modelMatrix, mvpMatrix)
        cubeShader.setUniformMatrix4fv("uMVPMatrix", mvpMatrix)
        cubeShader.setUniformMatrix4fv("uModelMatrix", modelMatrix)
        cubeShader.setUniform3f("uBaseColor", rgb[0], rgb[1], rgb[2])
        cubeShader.setUniform1f("uAlpha", alpha)
        cubeShader.setUniform1f("uClearFlash", clearing)
        cube.draw(cubeShader)
    }

    private fun drawGhost(state: Game3DState, piece: Piece3DState) {
        ghostShader.use()
        val rgb = pieceColorRGB(piece.type.colorIndex)
        for (b in piece.blocks) {
            val px = piece.x + b.x; val py = state.ghostY + b.y; val pz = piece.z + b.z
            if (py >= 0) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, px.toFloat(), py.toFloat(), pz.toFloat())
                camera.getMVP(modelMatrix, mvpMatrix)
                ghostShader.setUniformMatrix4fv("uMVPMatrix", mvpMatrix)
                ghostShader.setUniformMatrix4fv("uModelMatrix", modelMatrix)
                ghostShader.setUniform3f("uBaseColor", rgb[0], rgb[1], rgb[2])
                ghostShader.setUniform1f("uAlpha", 0.25f)
                ghostShader.setUniform3f("uCameraPos", camera.position[0], camera.position[1], camera.position[2])
                cube.draw(ghostShader)
            }
        }
    }

    private fun pieceColorRGB(idx: Int): FloatArray = when (idx) {
        1 -> floatArrayOf(0.000f, 0.898f, 1.000f)
        2 -> floatArrayOf(1.000f, 0.839f, 0.000f)
        3 -> floatArrayOf(0.667f, 0.000f, 1.000f)
        4 -> floatArrayOf(0.000f, 0.902f, 0.463f)
        5 -> floatArrayOf(1.000f, 0.427f, 0.000f)
        6 -> floatArrayOf(1.000f, 0.090f, 0.267f)
        7 -> floatArrayOf(0.161f, 0.475f, 1.000f)
        8 -> floatArrayOf(1.000f, 0.251f, 0.506f)
        else -> {
            val r = ((themeColorArgb shr 16) and 0xFF) / 255f
            val g = ((themeColorArgb shr 8) and 0xFF) / 255f
            val b = (themeColorArgb and 0xFF) / 255f
            floatArrayOf(r, g, b)
        }
    }
}
