package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import game.OnetMatch3Game
import java.util.ArrayDeque

/** Owns each screen exactly once, including screens retained beneath a modal. */
class ScreenManager(val game: OnetMatch3Game) {
    private data class Entry(val type: ScreenType, val screen: BaseScreen)
    private val factories = mutableMapOf<ScreenType, () -> BaseScreen>()
    private val stack = ArrayDeque<Entry>()
    var currentScreen: BaseScreen? = null
        private set
    var currentScreenType: ScreenType? = null
        private set
    private var pending: Entry? = null
    private var transition: ScreenTransition = InstantTransition
    private var elapsed = 0f
    private var preservePrevious = false
    private var resumeNext = false
    private var switched = false
    private var batch: SpriteBatch? = null
    private var shapes: ShapeRenderer? = null
    val isTransitioning: Boolean get() = pending != null

    fun registerScreen(type: ScreenType, factory: () -> BaseScreen) { factories[type] = factory }

    fun setScreen(type: ScreenType, transition: ScreenTransition = InstantTransition, disposePrevious: Boolean = true) {
        if (isTransitioning) return
        val screen = factories[type]?.invoke() ?: error("Unregistered screen: $type")
        // A replacement ends the previous navigation flow and any abandoned modals.
        if (disposePrevious) while (stack.isNotEmpty()) stack.pop().screen.dispose()
        navigate(Entry(type, screen), transition, !disposePrevious, false)
    }

    fun pushScreen(type: ScreenType, transition: ScreenTransition = InstantTransition) {
        if (isTransitioning) return
        val next = factories[type]?.invoke() ?: error("Unregistered screen: $type")
        currentScreen?.let { screen ->
            screen.pause()
            stack.push(Entry(requireNotNull(currentScreenType), screen))
        }
        navigate(Entry(type, next), transition, true, false)
    }

    fun popScreen(transition: ScreenTransition = InstantTransition): Boolean {
        if (isTransitioning || stack.isEmpty()) return false
        navigate(stack.pop(), transition, false, true)
        return true
    }

    private fun navigate(entry: Entry, effect: ScreenTransition, preserve: Boolean, resume: Boolean) {
        pending = entry
        transition = effect
        elapsed = 0f
        preservePrevious = preserve
        resumeNext = resume
        switched = false
        // Block input while a fade owns navigation, avoiding duplicate pushes/pops.
        Gdx.input?.inputProcessor = null
        if (effect.duration <= 0f) {
            switchScreen()
            pending = null
        }
    }

    private fun switchScreen() {
        val entry = pending ?: return
        currentScreen?.hide()
        if (!preservePrevious) currentScreen?.dispose()
        currentScreen = entry.screen
        currentScreenType = entry.type
        entry.screen.show()
        entry.screen.resize(Gdx.graphics?.width ?: 540, Gdx.graphics?.height ?: 960)
        if (resumeNext) entry.screen.resume()
        switched = true
    }

    fun render(delta: Float) {
        if (!isTransitioning) {
            currentScreen?.render(delta)
            return
        }
        elapsed += delta.coerceIn(0f, 0.1f)
        val progress = (elapsed / transition.duration).coerceIn(0f, 1f)
        if (progress >= 0.5f && !switched) switchScreen()
        // Draw without advancing outgoing gameplay or scheduling another transition.
        currentScreen?.renderStatic()
        if (Gdx.gl != null) {
            val width = (Gdx.graphics?.width ?: 540).toFloat()
            val height = (Gdx.graphics?.height ?: 960).toFloat()
            Gdx.gl.glViewport(0, 0, width.toInt(), height.toInt())
            val overlay = shapes ?: ShapeRenderer().also { shapes = it }
            val overlayBatch = batch ?: SpriteBatch().also { batch = it }
            overlay.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, width, height)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            overlay.begin(ShapeRenderer.ShapeType.Filled)
            transition.render(overlayBatch, overlay, progress, width, height)
            overlay.end()
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }
        if (progress >= 1f) {
            pending = null
            Gdx.input?.inputProcessor = currentScreen?.inputMultiplexer
        } else Gdx.input?.inputProcessor = null
    }

    fun resize(width: Int, height: Int) { currentScreen?.resize(width, height) }
    fun pause() { currentScreen?.pause() }
    fun resume() { currentScreen?.resume() }
    fun dispose() {
        currentScreen?.hide()
        currentScreen?.dispose()
        pending?.screen?.takeIf { it !== currentScreen }?.dispose()
        pending = null
        currentScreen = null
        while (stack.isNotEmpty()) stack.pop().screen.dispose()
        batch?.dispose()
        shapes?.dispose()
    }
}
