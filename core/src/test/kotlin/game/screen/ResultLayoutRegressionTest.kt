package game.screen

import com.badlogic.gdx.math.Rectangle
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResultLayoutRegressionTest {
    private val sizes = listOf(540f to 960f, 540f to 1200f, 540f to 1440f, 720f to 960f)

    private fun assertInside(rect: Rectangle, width: Float, height: Float) {
        assertTrue(rect.x >= 0f && rect.y >= 0f)
        assertTrue(rect.x + rect.width <= width && rect.y + rect.height <= height)
    }

    @Test fun `victory stars and EXP stay above every action button`() {
        for ((width, height) in sizes) for (won in listOf(true, false)) {
            val layout = ResultLayout(width, height, won)
            assertTrue(layout.stars.y > layout.stats.y + layout.stats.height + 16f)
            assertTrue(layout.stats.y > layout.primary.y + layout.primary.height + 16f)
            assertTrue(layout.primary.y > layout.menu.y + layout.menu.height + 12f)
            assertTrue(layout.subtitleY > layout.stars.y + layout.stars.height + 12f)
            for (rect in listOf(layout.illustration, layout.stars, layout.stats, layout.primary, layout.menu)) {
                assertInside(rect, width, height)
            }
            if (won) assertFalse(layout.replay.overlaps(layout.menu))
        }
    }

    @Test fun `saved game button cannot cover main menu navigation or play`() {
        for ((width, height) in sizes) for (saved in listOf(true, false)) {
            val layout = HomeLayout(width, height, saved)
            val visible = listOf(layout.profile, layout.wallet, layout.hero, layout.onet,
                layout.triple, layout.play, layout.missions, layout.collection, layout.settings) +
                if (saved) listOf(layout.continueGame) else emptyList()
            for (rect in visible) assertInside(rect, width, height)
            for (a in visible.indices) for (b in a + 1 until visible.size) {
                assertFalse(visible[a].overlaps(visible[b]), "Overlapping controls at ${width}x$height saved=$saved")
            }
        }
    }

    @Test fun `reward descriptions have room above claims and list pagination`() {
        for ((width, height) in sizes) {
            val layout = ListLayout(width, height)
            for (index in 0 until layout.count) {
                val card = layout.card(index)
                val claim = layout.claim(index)
                assertInside(card, width, height)
                assertTrue(card.y > 110f, "List must clear bottom pagination")
                assertTrue(card.height - 60f - 18f > claim.y + claim.height - card.y,
                    "Description must clear the claim button")
                assertTrue(card.contains(claim))
                if (index > 0) assertFalse(card.overlaps(layout.card(index - 1)))
            }
        }
    }
}
