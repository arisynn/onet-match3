package game.screen

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Screen transition effect interface.
 */
interface ScreenTransition {
    val duration: Float

    /**
     * Renders the transition effect between current and next screen textures/surfaces.
     * @param batch SpriteBatch for texture-based transitions.
     * @param shapeRenderer ShapeRenderer for solid color fades.
     * @param progress Transition progress from 0.0 (start) to 1.0 (finished).
     */
    fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer,
        progress: Float,
        width: Float,
        height: Float
    )
}

/**
 * Instantaneous screen transition with zero delay.
 */
object InstantTransition : ScreenTransition {
    override val duration: Float = 0f

    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer,
        progress: Float,
        width: Float,
        height: Float
    ) {
        // No-op
    }
}

/**
 * Smooth fade transition through a solid color (default black).
 */
class FadeTransition(
    override val duration: Float = 0.35f,
    val red: Float = 0f,
    val green: Float = 0f,
    val blue: Float = 0f
) : ScreenTransition {

    override fun render(
        batch: Batch,
        shapeRenderer: ShapeRenderer,
        progress: Float,
        width: Float,
        height: Float
    ) {
        // Alpha reaches 1.0 at midpoint (0.5), then fades back to 0.0
        val alpha = if (progress <= 0.5f) {
            progress * 2f
        } else {
            (1f - progress) * 2f
        }

        shapeRenderer.setColor(red, green, blue, alpha.coerceIn(0f, 1f))
        shapeRenderer.rect(0f, 0f, width, height)
    }
}
