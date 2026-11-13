package game.screen

import com.badlogic.gdx.math.Rectangle
import kotlin.math.min

/** Visual-only layout contracts. Interactive and rendered geometry share these rectangles. */
class HomeLayout(val width: Float, val height: Float, resume: Boolean) {
    val profile = Rectangle(24f, height - 88f, 64f, 64f)
    val wallet = Rectangle(width - 184f, height - 80f, 160f, 48f)
    val navY = 26f
    val navWidth = (width - 64f) / 3f
    val missions = Rectangle(24f, navY, navWidth, 82f)
    val collection = Rectangle(32f + navWidth, navY, navWidth, 82f)
    val settings = Rectangle(40f + navWidth * 2f, navY, navWidth, 82f)
    private val available = height - 322f
    private val heroHeight = (available * .42f).coerceIn(210f, 380f)
    private val modeHeight = (available * .27f).coerceIn(166f, 230f)
    private val actions = if (resume) 134f else 64f
    private val gap = ((available - heroHeight - modeHeight - actions) / 3f).coerceAtLeast(14f)
    val hero = Rectangle(24f, height - 178f - heroHeight, width - 48f, heroHeight)
    private val modeY = hero.y - gap - modeHeight
    val onet = Rectangle(24f, modeY, (width - 64f) / 2f, modeHeight)
    val triple = Rectangle(width / 2f + 8f, modeY, (width - 64f) / 2f, modeHeight)
    val play = Rectangle(24f, modeY - gap - 64f, width - 48f, 64f)
    val continueGame = Rectangle(24f, play.y - 70f, width - 48f, 56f)
}

/** All result content has its own block; no text is positioned under a button. */
class ResultLayout(val width: Float, val height: Float, val won: Boolean) {
    val blockHeight = min(height - 64f, 644f)
    private val top = (height + blockHeight) / 2f
    val illustration = Rectangle(width / 2f - 98f, top - 192f, 196f, 192f)
    val titleY = illustration.y - 22f
    val subtitleY = titleY - 40f
    val stars = Rectangle(width / 2f - 94f, subtitleY - 70f, 188f, 46f)
    val stats = Rectangle(28f, stars.y - 152f, width - 56f, 128f)
    val primary = Rectangle(28f, stats.y - 90f, width - 56f, 64f)
    val replay = Rectangle(28f, primary.y - 78f, (width - 72f) / 2f, 58f)
    val menu = if (won) {
        Rectangle(width / 2f + 8f, replay.y, (width - 72f) / 2f, 58f)
    } else {
        Rectangle(28f, replay.y, width - 56f, 58f)
    }
}

/** Reserve header/footer space and enough card height for description and claim rows. */
class ListLayout(val width: Float, val height: Float) {
    val count = ((height - 300f) / 180f).toInt().coerceIn(3, 6)
    val step = (height - 300f) / count

    fun card(index: Int) = Rectangle(
        28f, height - 182f - (index + 1) * step + 16f, width - 56f, step - 16f
    )

    fun claim(index: Int): Rectangle {
        val card = card(index)
        return Rectangle(width - 160f, card.y + 18f, 112f, 48f)
    }
}
