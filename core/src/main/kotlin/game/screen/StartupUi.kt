package game.screen

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.graphics.Color

/** GL resources are created only on the render thread when the screen is shown. */
internal class StartupUi(private val screen: BaseScreen) {
    private val font = BitmapFont().apply { data.setScale(2f) }
    private val stage = Stage(screen.viewport)
    init { screen.addInputProcessor(stage) }
    fun text(value: String, x: Float, y: Float) {
        stage.addActor(Label(value, Label.LabelStyle(font, Color.WHITE)).apply { setPosition(x, y) })
    }
    fun draw(delta: Float) { stage.act(delta); stage.draw() }
    fun dispose() { screen.removeInputProcessor(stage); stage.dispose(); font.dispose() }
}
