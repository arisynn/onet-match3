package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import game.OnetMatch3Game
import java.util.ArrayDeque

/**
 * Robust screen lifecycle and navigation manager.
 * Supports screen factories, transition animations, and screen stack push/pop.
 */
class ScreenManager(
    val game: OnetMatch3Game
) {
    private val screenFactories = mutableMapOf<ScreenType, () -> BaseScreen>()
    private val screenStack = ArrayDeque<BaseScreen>()

    var currentScreen: BaseScreen? = null
        private set
    var currentScreenType: ScreenType? = null
        private set

    // Transition state
    private var transition: ScreenTransition = InstantTransition
    private var transitionProgress: Float = 1.0f
    private var transitionElapsed: Float = 0f
    private var nextScreen: BaseScreen? = null
    private var nextScreenType: ScreenType? = null
    private var isTransitioning: Boolean = false

    private var transitionBatch: SpriteBatch? = null
    private var transitionShapes: ShapeRenderer? = null

    private fun getBatch(): SpriteBatch {
        if (transitionBatch == null) transitionBatch = SpriteBatch()
        return transitionBatch!!
    }

    private fun getShapes(): ShapeRenderer {
        if (transitionShapes == null) transitionShapes = ShapeRenderer()
        return transitionShapes!!
    }

    /**
     * Register a factory function for a specific screen type.
     */
    fun registerScreen(type: ScreenType, factory: () -> BaseScreen) {
        screenFactories[type] = factory
    }

    /**
     * Switch to a screen type, replacing the current screen with an optional transition effect.
     * Disposes the previous screen if not pushed to the stack.
     */
    fun setScreen(
        type: ScreenType,
        transition: ScreenTransition = InstantTransition,
        disposePrevious: Boolean = true
    ) {
        if (isTransitioning) return
        game.diagnostics("Screen transition: $currentScreenType -> $type")
        val factory = screenFactories[type]
            ?: error("Screen type $type has not been registered in ScreenManager.")

        val newScreen = factory()
        if (transition.duration <= 0f) {
            // Instant transition
            completeScreenSwitch(type, newScreen, disposePrevious)
        } else {
            // Animated transition setup
            this.transition = transition
            this.nextScreen = newScreen
            this.nextScreenType = type
            this.transitionElapsed = 0f
            this.transitionProgress = 0f
            this.isTransitioning = true
        }
    }

    /**
     * Push current screen onto stack and switch to a modal screen (e.g. Settings, Result).
     * Current screen is paused and hidden, but NOT disposed.
     */
    fun pushScreen(type: ScreenType, transition: ScreenTransition = InstantTransition) {
        currentScreen?.let {
            it.pause()
            it.hide()
            screenStack.push(it)
        }
        setScreen(type, transition, disposePrevious = false)
    }

    /**
     * Pop top screen from stack and resume previous screen.
     */
    fun popScreen(transition: ScreenTransition = InstantTransition): Boolean {
        if (screenStack.isEmpty()) return false

        val previousScreen = screenStack.pop()
        val previousType = screenFactories.entries.firstOrNull { it.value()::class == previousScreen::class }?.key

        currentScreen?.hide()
        currentScreen?.dispose()

        currentScreen = previousScreen
        currentScreenType = previousType
        previousScreen.show()
        previousScreen.resize(Gdx.graphics?.width ?: 540, Gdx.graphics?.height ?: 960)
        previousScreen.resume()
        return true
    }

    private fun completeScreenSwitch(type: ScreenType, newScreen: BaseScreen, disposePrevious: Boolean) {
        val oldScreen = currentScreen
        if (oldScreen != null) {
            oldScreen.hide()
            if (disposePrevious) {
                oldScreen.dispose()
            }
        }

        currentScreen = newScreen
        currentScreenType = type

        game.diagnostics("Screen active: $type")
        newScreen.show()
        newScreen.resize(Gdx.graphics?.width ?: 540, Gdx.graphics?.height ?: 960)
    }

    /**
     * Update and render current screen and any active transition.
     */
    fun render(delta: Float) {
        if (!isTransitioning) {
            currentScreen?.render(delta)
        } else {
            transitionElapsed += delta
            transitionProgress = (transitionElapsed / transition.duration).coerceIn(0f, 1f)

            if (transitionProgress < 0.5f) {
                // First half: Render current screen
                currentScreen?.render(delta)
            } else {
                // Second half: Switch and render next screen
                if (nextScreen != null && currentScreen != nextScreen) {
                    val old = currentScreen
                    currentScreen = nextScreen
                    currentScreenType = nextScreenType
                    old?.hide()
                    old?.dispose()

                    game.diagnostics("Screen active: $currentScreenType")
                    currentScreen?.show()
                    currentScreen?.resize(Gdx.graphics?.width ?: 540, Gdx.graphics?.height ?: 960)
                    nextScreen = null
                }
                currentScreen?.render(delta)
            }

            // Render transition overlay if OpenGL is available
            if (Gdx.gl != null) {
                val batch = getBatch()
                val shapes = getShapes()
                Gdx.gl.glEnable(GL20.GL_BLEND)
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
                shapes.begin(ShapeRenderer.ShapeType.Filled)
                transition.render(
                    batch,
                    shapes,
                    transitionProgress,
                    (Gdx.graphics?.width ?: 540).toFloat(),
                    (Gdx.graphics?.height ?: 960).toFloat()
                )
                shapes.end()
                Gdx.gl.glDisable(GL20.GL_BLEND)
            }

            if (transitionProgress >= 1.0f) {
                isTransitioning = false
                transition = InstantTransition
                nextScreen = null
                nextScreenType = null
            }
        }
    }

    fun resize(width: Int, height: Int) {
        currentScreen?.resize(width, height)
    }

    fun pause() {
        currentScreen?.pause()
    }

    fun resume() {
        currentScreen?.resume()
    }

    fun dispose() {
        currentScreen?.hide()
        currentScreen?.dispose()
        currentScreen = null

        while (screenStack.isNotEmpty()) {
            screenStack.pop().dispose()
        }

        transitionBatch?.dispose()
        transitionBatch = null
        transitionShapes?.dispose()
        transitionShapes = null
    }
}
