package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.JsonReader
import game.assets.TileAssetResolver
import kotlin.math.min

/** Ceramic artwork, crisp scalable typography, and consistent controls for native screens. */
class ScreenUi : Disposable {
    val ink = Color.valueOf("344F45")
    val muted = Color.valueOf("7D8B7D")
    val sage = Color.valueOf("62876D")
    val cream = Color.valueOf("FFFDF5")
    private val batch = SpriteBatch()
    private val glyphs = GlyphLayout()
    private val bodyFont = createFont("Nunito.ttf")
    private val displayFont = createFont("DMSerifDisplay.ttf")
    private val sourcePixmaps = mutableListOf<Pixmap>()
    private val pixel = managedPixmap(1) { it.setColor(Color.WHITE); it.fill() }
    private val circle = managedPixmap(128) {
        it.setColor(Color.WHITE); it.fillCircle(64, 64, 63)
    }
    private val textures = mutableMapOf<String, Texture>()
    private val regions = mutableMapOf<String, TextureRegion>()
    private val patches = mutableMapOf<String, NinePatch>()
    private val manifest = JsonReader().parse(Gdx.files.internal("ui-native/manifest.json"))
    private var originX = 0f
    private var originY = 0f
    private var pointer: Vector2? = null
    var width = 540f
        private set
    var height = 960f
        private set

    private fun createFont(file: String): BitmapFont {
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/$file"))
        return try {
            generator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                size = 64
                characters = FreeTypeFontGenerator.DEFAULT_CHARS + "•…"
                genMipMaps = true
                minFilter = Texture.TextureFilter.MipMapLinearLinear
                magFilter = Texture.TextureFilter.Linear
            })
        } finally { generator.dispose() }
    }

    private fun managedPixmap(size: Int, draw: (Pixmap) -> Unit): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        draw(pixmap)
        sourcePixmaps.add(pixmap)
        return Texture(PixmapTextureData(pixmap, Pixmap.Format.RGBA8888, true, false, true)).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
    }

    fun render(projection: Matrix4, worldWidth: Float, worldHeight: Float,
               offsetX: Float, offsetY: Float, touch: Vector2?, draw: ScreenUi.() -> Unit) {
        width = worldWidth; height = worldHeight
        pointer = touch
        originX = 0f; originY = 0f
        batch.projectionMatrix = projection
        batch.begin()
        try {
            background()
            originX = offsetX; originY = offsetY
            draw()
        } finally { batch.end() }
    }

    private fun texture(path: String) = textures.getOrPut(path) {
        Texture(Gdx.files.internal(path), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
    }

    private fun region(name: String) = regions.getOrPut(name) {
        val entry = manifest.get(name)
        val bounds = entry.get("bounds").asIntArray()
        TextureRegion(texture(entry.getString("path")), bounds[0], bounds[1], bounds[2], bounds[3])
    }

    private fun patch(name: String) = patches.getOrPut(name) {
        val source = region(name)
        val inset = min(source.regionWidth, source.regionHeight) / 4
        NinePatch(source, inset, inset, inset, inset).apply { scale(13f / inset, 13f / inset) }
    }

    private fun background() {
        fillRect(0f, 0f, width, height, Color.valueOf("F2F4EA"))
        val source = region("spa")
        val scale = maxOf(width / source.regionWidth, height / source.regionHeight)
        batch.setColor(1f, 1f, 1f, 0.24f)
        batch.draw(source, (width - source.regionWidth * scale) / 2f,
            (height - source.regionHeight * scale) / 2f, source.regionWidth * scale, source.regionHeight * scale)
        batch.color = Color.WHITE
    }

    private fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Color) {
        batch.color = color
        batch.draw(pixel, x + originX, y + originY, w, h)
        batch.color = Color.WHITE
    }

    fun rounded(x: Float, y: Float, w: Float, h: Float, radius: Float = 18f, color: Color = cream) {
        if (w <= 0f || h <= 0f) return
        val r = min(radius, min(w, h) / 2f)
        batch.color = color
        val ox = x + originX; val oy = y + originY
        batch.draw(circle, ox, oy, r * 2f, r * 2f)
        batch.draw(circle, ox + w - r * 2f, oy, r * 2f, r * 2f)
        batch.draw(circle, ox, oy + h - r * 2f, r * 2f, r * 2f)
        batch.draw(circle, ox + w - r * 2f, oy + h - r * 2f, r * 2f, r * 2f)
        batch.draw(pixel, ox + r, oy, w - r * 2f, h)
        batch.draw(pixel, ox, oy + r, w, h - r * 2f)
        batch.color = Color.WHITE
    }

    fun box(x: Float, y: Float, width: Float, height: Float, color: Color = cream) =
        rounded(x, y, width, height, min(16f, height / 2f), color)

    fun panel(rect: Rectangle, color: Color = cream) {
        rounded(rect.x, rect.y - 3f, rect.width, rect.height, 24f, Color.valueOf("D8DFCD"))
        rounded(rect.x, rect.y, rect.width, rect.height, 24f, color)
    }

    fun art(name: String, x: Float, y: Float, w: Float, h: Float, alpha: Float = 1f) {
        val source = region(name)
        val scale = min(w / source.regionWidth, h / source.regionHeight)
        val rw = source.regionWidth * scale; val rh = source.regionHeight * scale
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(source, x + originX + (w - rw) / 2f, y + originY + (h - rh) / 2f, rw, rh)
        batch.color = Color.WHITE
    }

    private fun label(value: String, x: Float, y: Float, points: Float, color: Color,
                      width: Float, display: Boolean = false, centeredVertically: Boolean = false) {
        val font = if (display) displayFont else bodyFont
        font.data.setScale(points / 64f)
        font.color = color
        glyphs.setText(font, value)
        val left = if (width > 0f) x + (width - glyphs.width) / 2f else x
        font.draw(batch, glyphs, left + originX, y + originY + if (centeredVertically) glyphs.height / 2f else 0f)
    }

    fun text(value: String, x: Float, y: Float, size: Float = 1.5f, color: Color = ink, width: Float = 0f) =
        label(value, x, y, size * 18f, color, width)

    fun heading(value: String, x: Float, y: Float, points: Float = 36f, width: Float = 0f) =
        label(value, x, y, points, ink, width, display = true)

    fun title(value: String, subtitle: String = "") {
        heading(value, 20f, 812f, 40f, 500f)
        if (subtitle.isNotBlank()) text(subtitle, 20f, 759f, 1.05f, muted, 500f)
    }

    fun button(value: String, x: Float, y: Float, width: Float, height: Float,
               enabled: Boolean = true, primary: Boolean = value in setOf("Play", "Resume", "Next puzzle", "Play again")) {
        val pressed = enabled && pointer?.let { Rectangle(x, y, width, height).contains(it) } == true
        val dy = if (pressed) -2f else 0f
        if (primary) {
            rounded(x, y - 4f, width, height, 20f, Color.valueOf("385F46"))
            rounded(x, y + dy, width, height, 20f, if (pressed) Color.valueOf("54795E") else sage)
        } else {
            batch.color = if (enabled) Color.WHITE else Color(1f, 1f, 1f, 0.55f)
            patch("button-mint").draw(batch, x + originX, y + originY + dy - 2f, width, height + 4f)
            batch.color = Color.WHITE
        }
        label(value, x, y + height / 2f + dy, min(24f, width / (value.length.coerceAtLeast(1) * 0.54f)),
            if (primary) cream else if (enabled) ink else muted, width, centeredVertically = true)
    }

    fun back() { button("Back", 40f, 880f, 100f, 50f) }

    fun tile(id: String, x: Float, y: Float, width: Float, height: Float, selected: Boolean = false) {
        if (selected) rounded(x - 2f, y - 2f, width + 4f, height + 4f, 15f, sage)
        patch(if (selected) "tile-mint" else "tile-ivory").draw(batch, x + originX, y + originY, width, height)
        val original = TileAssetResolver.tileAssetFor(id)
        val path = original?.let { if (it.endsWith(".webp")) it.replace("tiles/", "tiles-native/").removeSuffix(".webp") + ".png" else it }
        val icon = if (path != null && Gdx.files.internal(path).exists()) texture(path) else null
        if (icon != null) {
            val scale = min((width - 12f) / icon.width, (height - 12f) / icon.height) * 0.90f
            val w = icon.width * scale; val h = icon.height * scale
            batch.draw(icon, x + originX + (width - w) / 2f, y + originY + (height - h) / 2f + 1f, w, h)
        } else text(id.substringAfterLast('_'), x, y + height / 2f + 6f, 1f, ink, width)
    }

    fun tile(id: String, rect: Rectangle, selected: Boolean = false) =
        tile(id, rect.x, rect.y, rect.width, rect.height, selected)

    fun iconButton(rect: Rectangle, name: String) {
        val cx = rect.x + rect.width / 2f; val cy = rect.y + rect.height / 2f
        rounded(cx - 25f, cy - 27f, 50f, 50f, 25f, Color.valueOf("DCE3D3"))
        rounded(cx - 25f, cy - 24f, 50f, 50f, 25f, cream)
        if (name == "pause") {
            rounded(cx - 7f, cy - 8f, 5f, 17f, 2f, sage)
            rounded(cx + 3f, cy - 8f, 5f, 17f, 2f, sage)
        } else {
            line(cx + 4f, cy + 8f, cx - 4f, cy)
            line(cx - 4f, cy, cx + 4f, cy - 8f)
        }
    }

    fun gameHeader(title: String, subtitle: String, score: Int, seconds: Int, duration: Int, layout: PuzzleLayout) {
        iconButton(layout.back, "back"); iconButton(layout.pause, "pause")
        heading(title, 80f, height - 40f, 32f, width - 160f)
        text(subtitle, 80f, height - 75f, 0.78f, muted, width - 160f)
        panel(Rectangle(28f, height - 152f, width - 56f, 60f))
        text("SCORE", 48f, height - 108f, 0.61f, muted)
        text(score.toString().padStart(3, '0'), 48f, height - 125f, 1.18f, ink)
        val minutes = seconds / 60; val remainder = seconds % 60
        val time = "$minutes:${remainder.toString().padStart(2, '0')}"
        text("TIME LEFT", width - 150f, height - 108f, 0.61f, muted)
        text(time, width - 150f, height - 125f, 1.18f, if (seconds < 20) Color.valueOf("BE745C") else ink)
        val barX = 160f; val barWidth = width - 320f
        rounded(barX, height - 129f, barWidth, 6f, 3f, Color.valueOf("E4E8DC"))
        rounded(barX, height - 129f, barWidth * (seconds.toFloat() / duration).coerceIn(0f, 1f), 6f, 3f, sage)
    }

    fun tool(rect: Rectangle, icon: String, title: String, enabled: Boolean = true) {
        panel(rect, if (enabled) cream else Color.valueOf("E4E9DC"))
        art(icon, rect.x + 16f, rect.y + 11f, 46f, 46f, if (enabled) 1f else 0.4f)
        label(title, rect.x + 68f, rect.y + rect.height / 2f, 22f,
            if (enabled) ink else muted, rect.width - 82f, centeredVertically = true)
    }

    fun slot(rect: Rectangle) {
        patch("slot-mint").draw(batch, rect.x + originX, rect.y + originY, rect.width, rect.height)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1; val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        batch.color = sage
        batch.draw(pixel, x1 + originX, y1 + originY - 1.5f, 0f, 1.5f, length, 3f, 1f, 1f,
            Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat(), 0, 0, 1, 1, false, false)
        batch.color = Color.WHITE
    }

    override fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
        bodyFont.dispose(); displayFont.dispose()
        pixel.dispose(); circle.dispose()
        sourcePixmaps.forEach { it.dispose() }
        batch.dispose()
    }
}
