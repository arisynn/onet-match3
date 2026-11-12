package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import game.assets.TileAssetResolver

/** Small GL-backed UI shared by Android and desktop; allocated only with a GL context. */
class ScreenUi : Disposable {
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
    private val layout = GlyphLayout()
    private val pixel = Pixmap(1, 1, Pixmap.Format.RGBA8888).let {
        it.setColor(Color.WHITE); it.fill()
        Texture(it).also { _ -> it.dispose() }
    }
    private val textures = mutableMapOf<String, Texture>()
    private val cream = Color.valueOf("FFF5E6")
    private val ink = Color.valueOf("263B41")
    private val mint = Color.valueOf("A7DCC2")

    fun render(projection: Matrix4, draw: ScreenUi.() -> Unit) {
        batch.projectionMatrix = projection
        batch.begin()
        try { draw() } finally { batch.end() }
    }

    fun box(x: Float, y: Float, width: Float, height: Float, color: Color = cream) {
        batch.color = color
        batch.draw(pixel, x, y, width, height)
        batch.color = Color.WHITE
    }

    fun text(value: String, x: Float, y: Float, size: Float = 1.5f, color: Color = cream, width: Float = 0f) {
        font.data.setScale(size)
        font.color = color
        layout.setText(font, value)
        font.draw(batch, layout, if (width > 0f) x + (width - layout.width) / 2f else x, y)
    }

    fun title(value: String, subtitle: String = "") {
        text(value, 20f, 805f, 2.5f, width = 500f)
        if (subtitle.isNotBlank()) text(subtitle, 20f, 755f, 1.25f, width = 500f)
    }

    fun button(value: String, x: Float, y: Float, width: Float, height: Float, enabled: Boolean = true) {
        box(x, y - 4f, width, height, Color.valueOf("162C32"))
        box(x, y, width, height, if (enabled) mint else Color.valueOf("718080"))
        text(value, x, y + height / 2f + 9f, 1.5f, ink, width)
    }

    fun back() { button("Back", 40f, 880f, 100f, 50f) }

    fun tile(id: String, x: Float, y: Float, width: Float, height: Float, selected: Boolean = false) {
        box(x, y - 3f, width, height, Color.valueOf("142A30"))
        box(x, y, width, height, if (selected) Color.valueOf("FFD27D") else cream)
        val path = TileAssetResolver.tileAssetFor(id)
        // libGDX Pixmap supports PNG/JPEG. The playable themes use the original PNGs.
        val texture = if (path != null && path.endsWith(".png")) textures.getOrPut(path) {
            Texture(Gdx.files.internal(path)).apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        } else null
        if (texture != null) {
            val scale = minOf((width - 10f) / texture.width, (height - 10f) / texture.height)
            val w = texture.width * scale
            val h = texture.height * scale
            batch.draw(texture, x + (width - w) / 2f, y + (height - h) / 2f, w, h)
        } else text(id.substringAfterLast('_'), x, y + height / 2f + 6f, 1.2f, ink, width)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1; val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        batch.color = Color.valueOf("FFD27D")
        batch.draw(pixel, x1, y1 - 2f, 0f, 2f, length, 4f, 1f, 1f,
            Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat(), 0, 0, 1, 1, false, false)
        batch.color = Color.WHITE
    }

    override fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
        font.dispose()
        pixel.dispose()
        batch.dispose()
    }
}
