package game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Disposable

/**
 * App-owned font atlas pair, created lazily on the rendering thread.
 * Screens borrow these fonts and never dispose them. Font scale/color are set
 * before each label; all drawing remains serialized on the libGDX GL thread.
 */
class UiTypeface : Disposable {
    val body: BitmapFont = createFont("PuzzleSans-SemiBold.ttf")
    val display: BitmapFont = try {
        createFont("DMSerifDisplay.ttf")
    } catch (failure: Throwable) {
        body.dispose()
        throw failure
    }
    private var disposed = false

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

    override fun dispose() {
        if (disposed) return
        disposed = true
        body.dispose()
        display.dispose()
    }
}
