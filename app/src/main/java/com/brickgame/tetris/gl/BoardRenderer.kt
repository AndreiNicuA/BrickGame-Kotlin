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

/**
 * OpenGL ES 2.0 renderer for the 3D Tetris board.
 *
 * Replaces the Compose Canvas-based Tetris3DBoard with GPU-accelerated rendering.
 * Features:
 *   - Blinn-Phong lighting with per-pixel shading
 *   - Material textures (procedurally generated)
 *   - Ghost piece with Fresnel edge glow
 *   - Layer clearing flash animation
 *   - Floor grid and wireframe board frame
 *   - Transparency support (Glass/Crystal materials)
 */
class BoardRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // Shaders
    private lateinit var cubeShader: ShaderProgram
    private lateinit var ghostShader: ShaderProgram
    private lateinit var gridShader: ShaderProgram

    // Geometry
    private lateinit var cube: CubeGeometry
    private lateinit var grid: GridGeometry

    // Textures
    private lateinit var textures: TextureManager

    // Camera
    private val camera = Camera3D()

    // State — updated from the UI thread
    @Volatile private var currentState: Game3DState = Game3DState()
    @Volatile private var currentMaterial: PieceMaterial = PieceMaterial.CLASSIC
    @Volatile private var showGhost: Boolean = true
    @Volatile private var themeColorArgb: Long = 0xFF22C55EL
    @Volatile private var bgColorArgb: Long = 0xFF0A0A0AL

    // Reusable matrices to avoid per-frame allocation
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Light direction — top-right-front (normalized in shader)
    private val lightDir = floatArrayOf(0.4f, 0.8f, -0.5f)

    // ============ Public API (called from UI thread) ============

    fun updateState(
        state: Game3DState,
        material: PieceMaterial,
        ghost: Boolean,
        themeColor: Long,
        bgColor: Long = 0xFF0A0A0AL
    ) {
        currentState = state
        currentMaterial = material
        showGhost = ghost
        themeColorArgb = themeColor
        bgColorArgb = bgColor
    }

    fun updateCamera(azimuth: Float, elevation: Float, panX: Float, panY: Float, zoom: Float) {
        camera.azimuth = azimuth
        camera.elevation = elevation
        camera.panX = panX
        camera.panY = panY
        camera.zoom = zoom
    }

    // ============ GLSurfaceView.Renderer ============

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Background color from theme
        GLES20.glClearColor(0.04f, 0.04f, 0.04f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        // Compile shaders
        cubeShader = ShaderProgram(context, R.raw.cube_vertex, R.raw.cube_fragment)
        ghostShader = ShaderProgram(context, R.raw.cube_vertex, R.raw.ghost_fragment)
        gridShader = ShaderProgram(context, R.raw.grid_vertex, R.raw.grid_fragment)

        // Create geometry
        cube = CubeGeometry()
        grid = GridGeometry()

        // Generate textures
        textures = TextureManager()
        textures.loadAll()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        camera.setProjection(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Update background color
        val bgR = ((bgColorArgb shr 16) and 0xFF) / 255f
        val bgG = ((bgColorArgb shr 8) and 0xFF) / 255f
        val bgB = (bgColorArgb and 0xFF) / 255f
        GLES20.glClearColor(bgR, bgG, bgB, 1f)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        camera.update()

        val state = currentState
        val material = currentMaterial
        val matParams = textures.getParams(material)
        val isTransparent = matParams.transparency < 1f

        // 1. Grid (always behind everything)
        grid.draw(gridShader, camera.getViewProjection())

        // 2. Opaque blocks (or all blocks for opaque materials)
        if (!isTransparent) {
            drawBlocks(state, material, matParams)
        }

        // 3. Transparent blocks — disable depth writing so they blend correctly
        if (isTransparent) {
            GLES20.glDepthMask(false)
            drawBlocks(state, material, matParams)
            GLES20.glDepthMask(true)
        }

        // 4. Ghost piece
        if (showGhost) {
            drawGhost(state)
        }
    }

    // ============ Rendering ============

    private fun drawBlocks(state: Game3DState, material: PieceMaterial, params: MaterialShaderParams) {
        cubeShader.use()
        textures.bind(material)

        // Set shared uniforms
        cubeShader.setUniform1i("uTexture", 0)
        cubeShader.setUniform3f("uLightDir", lightDir[0], lightDir[1], lightDir[2])
        cubeShader.setUniform3f("uCameraPos", camera.position[0], camera.position[1], camera.position[2])
        cubeShader.setUniform1f("uTextureStrength", params.textureStrength)
        cubeShader.setUniform1f("uSpecularPower", params.specularPower)
        cubeShader.setUniform1f("uSpecularStrength", params.specularStrength)
        cubeShader.setUniform1f("uTransparency", params.transparency)

        // Board blocks
        if (state.board.isNotEmpty()) {
            for (y in 0 until Tetris3DGame.BOARD_H) {
                val clearing = if (y in state.clearingLayers) state.clearAnimProgress else 0f
                for (z in 0 until Tetris3DGame.BOARD_D) {
                    for (x in 0 until Tetris3DGame.BOARD_W) {
                        if (y < state.board.size && z < state.board[y].size && x < state.board[y][z].size) {
                            val colorIdx = state.board[y][z][x]
                            if (colorIdx > 0) {
                                val rgb = pieceColorRGB(colorIdx)
                                drawOneCube(x.toFloat(), y.toFloat(), z.toFloat(), rgb, 1f, clearing)
                            }
                        }
                    }
                }
            }
        }

        // Current piece
        val piece = state.currentPiece
        if (piece != null) {
            val rgb = pieceColorRGB(piece.type.colorIndex)
            for (b in piece.blocks) {
                val px = piece.x + b.x
                val py = piece.y + b.y
                val pz = piece.z + b.z
                if (py >= 0) {
                    drawOneCube(px.toFloat(), py.toFloat(), pz.toFloat(), rgb, 1f, 0f)
                }
            }
        }
    }

    private fun drawOneCube(x: Float, y: Float, z: Float, rgb: FloatArray, alpha: Float, clearing: Float) {
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

    private fun drawGhost(state: Game3DState) {
        val piece = state.currentPiece ?: return
        if (!showGhost || state.ghostY >= piece.y) return

        // Ghost uses a separate shader with Fresnel edge glow
        ghostShader.use()
        val rgb = pieceColorRGB(piece.type.colorIndex)

        // Disable depth write for transparent ghost
        GLES20.glDepthMask(false)

        for (b in piece.blocks) {
            val px = piece.x + b.x
            val py = state.ghostY + b.y
            val pz = piece.z + b.z
            if (py >= 0) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, px.toFloat(), py.toFloat(), pz.toFloat())
                camera.getMVP(modelMatrix, mvpMatrix)

                ghostShader.setUniformMatrix4fv("uMVPMatrix", mvpMatrix)
                ghostShader.setUniformMatrix4fv("uModelMatrix", modelMatrix)
                ghostShader.setUniform3f("uBaseColor", rgb[0], rgb[1], rgb[2])
                ghostShader.setUniform1f("uAlpha", 0.22f)
                ghostShader.setUniform3f("uCameraPos", camera.position[0], camera.position[1], camera.position[2])

                cube.draw(ghostShader)
            }
        }

        GLES20.glDepthMask(true)
    }

    // ============ Color Mapping ============

    /** Convert piece color index to normalized RGB floats [0..1]. */
    private fun pieceColorRGB(idx: Int): FloatArray = when (idx) {
        1 -> floatArrayOf(0.000f, 0.898f, 1.000f) // Cyan    #00E5FF
        2 -> floatArrayOf(1.000f, 0.839f, 0.000f) // Yellow  #FFD600
        3 -> floatArrayOf(0.667f, 0.000f, 1.000f) // Purple  #AA00FF
        4 -> floatArrayOf(0.000f, 0.902f, 0.463f) // Green   #00E676
        5 -> floatArrayOf(1.000f, 0.427f, 0.000f) // Orange  #FF6D00
        6 -> floatArrayOf(1.000f, 0.090f, 0.267f) // Red     #FF1744
        7 -> floatArrayOf(0.161f, 0.475f, 1.000f) // Blue    #2979FF
        8 -> floatArrayOf(1.000f, 0.251f, 0.506f) // Pink    #FF4081
        else -> {
            // Extract from theme accent color
            val r = ((themeColorArgb shr 16) and 0xFF) / 255f
            val g = ((themeColorArgb shr 8) and 0xFF) / 255f
            val b = (themeColorArgb and 0xFF) / 255f
            floatArrayOf(r, g, b)
        }
    }
}
