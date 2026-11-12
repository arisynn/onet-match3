package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import game.OnetMatch3Game

/**
 * Base abstract screen providing lifecycle management, virtual portrait viewport,
 * input multiplexing, and robust resource cleanup.
 */
abstract class BaseScreen(
    val game: OnetMatch3Game,
    val virtualWidth: Float = 540f,
    val virtualHeight: Float = 960f
) : Screen {

    val camera: OrthographicCamera = OrthographicCamera()
    val viewport: Viewport = ExtendViewport(virtualWidth, virtualHeight, camera)
    val inputMultiplexer: InputMultiplexer = InputMultiplexer()

    var backgroundColor: Color = Color(0.12f, 0.12f, 0.15f, 1.0f)
    var isDisposed: Boolean = false
        private set

    init {
        camera.up.set(0f, 1f, 0f)
        camera.direction.set(0f, 0f, -1f)
        camera.position.set(virtualWidth / 2.0f, virtualHeight / 2.0f, 0f)
        camera.viewportWidth = virtualWidth
        camera.viewportHeight = virtualHeight
        camera.near = 0f
        camera.far = 100f
        if (Gdx.gl != null) {
            camera.update(false)
        }
    }

    override fun show() {
        Gdx.input?.inputProcessor = inputMultiplexer
        onScreenShown()
    }

    /**
     * Hook invoked after viewport and input processors are configured on screen display.
     */
    open fun onScreenShown() {}

    /**
     * Add an input processor to this screen's multiplexer.
     */
    fun addInputProcessor(processor: InputProcessor) {
        if (!inputMultiplexer.processors.contains(processor, true)) {
            inputMultiplexer.addProcessor(processor)
        }
    }

    /**
     * Remove an input processor from this screen's multiplexer.
     */
    fun removeInputProcessor(processor: InputProcessor) {
        inputMultiplexer.removeProcessor(processor)
    }

    protected open val responsiveLayout: Boolean = false
    private val contentOffsetX: Float get() = if (responsiveLayout) 0f else (viewport.worldWidth - virtualWidth) / 2f
    private val contentOffsetY: Float get() = if (responsiveLayout) 0f else (viewport.worldHeight - virtualHeight) / 2f

    protected fun inputPoint(screenX: Int, screenY: Int): Vector2 =
        viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat())).sub(contentOffsetX, contentOffsetY)

    fun projectUi(point: Vector2): Vector2 = viewport.project(point.add(contentOffsetX, contentOffsetY))

    private var screenUi: ScreenUi? = null

    protected fun ui(draw: ScreenUi.() -> Unit) {
        if (Gdx.gl == null) return
        val renderer = screenUi ?: ScreenUi().also { screenUi = it }
        val pointer = if (Gdx.input?.isTouched == true) inputPoint(Gdx.input.x, Gdx.input.y) else null
        renderer.render(camera.combined, viewport.worldWidth, viewport.worldHeight,
            contentOffsetX, contentOffsetY, pointer, draw)
    }

    fun renderStatic() { renderFrame(0f, false) }

    override fun render(delta: Float) { renderFrame(delta, true) }

    private fun renderFrame(delta: Float, advance: Boolean) {
        // Clamp delta time to avoid physics/animation explosions on frame hitch
        val clampedDelta = delta.coerceIn(0f, 0.1f)

        // Clear display buffer
        Gdx.gl?.glClearColor(
            backgroundColor.r,
            backgroundColor.g,
            backgroundColor.b,
            backgroundColor.a
        )
        Gdx.gl?.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        if (Gdx.gl != null) {
            camera.update(false)
            viewport.apply()
        }

        if (advance) update(clampedDelta)
        draw(clampedDelta)
    }

    /**
     * Logic update step executed before drawing each frame.
     */
    abstract fun update(delta: Float)

    /**
     * Rendering step for drawing visual entities, UI stages, and particle effects.
     */
    abstract fun draw(delta: Float)

    override fun resize(width: Int, height: Int) {
        if (Gdx.gl != null) {
            viewport.update(width, height, true)
        }
    }

    override fun pause() {
        // Lifecycle hook for Android onPause or desktop defocus
    }

    override fun resume() {
        // Lifecycle hook for Android onResume or desktop refocus
    }

    override fun hide() {
        if (Gdx.input?.inputProcessor == inputMultiplexer) {
            Gdx.input?.inputProcessor = null
        }
        onScreenHidden()
    }

    /**
     * Hook invoked when screen is no longer the active foreground screen.
     */
    open fun onScreenHidden() {}

    override fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            inputMultiplexer.clear()
            screenUi?.dispose()
            screenUi = null
            onDispose()
        }
    }

    /**
     * Override to dispose screen-specific textures, stages, and meshes.
     */
    open fun onDispose() {}
}
