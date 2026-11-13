package game.screen

import com.badlogic.gdx.math.Rectangle
import game.OnetMatch3Game
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResponsiveUiContractTest {
    private val sizes = listOf(540f to 960f, 540f to 1200f, 540f to 1440f, 720f to 960f)

    @Test fun `outside-board Onet paths remain visible and clear of HUD and controls`() {
        for ((w,h) in sizes) {
            val layout=PuzzleLayout(w,h)
            val left=layout.onetPoint(5,0)
            val right=layout.onetPoint(5,7)
            val top=layout.onetPoint(0,3)
            val bottom=layout.onetPoint(11,3)
            assertTrue(left.x > 0f && right.x < w, "Horizontal route padding at ${w}x$h")
            assertTrue(top.y < h - 152f, "Outside route overlaps HUD")
            assertTrue(bottom.y > layout.hint.y + layout.hint.height, "Outside route overlaps tools")
            assertTrue(layout.board.y > bottom.y)
        }
    }

    @Test fun `board and stack center taps map to the drawn tile on every supported shape`() {
        for ((w,h) in sizes) {
            val layout=PuzzleLayout(w,h)
            for (row in 1..10) for (col in 1..6) {
                val point=layout.onetPoint(row,col)
                assertEquals(row to col,layout.onetHit(point.x,point.y))
            }
            assertNull(layout.onetHit(0f,0f))
            for (stack in 0 until 12) {
                val r=layout.stack(stack)
                assertEquals(stack,layout.stackHit(r.x+r.width/2,r.y+r.height/2))
                assertTrue(r.y > layout.tray.y+layout.tray.height)
            }
        }
    }

    @Test fun `native test action ids resolve to usable visible hit regions`() {
        val game=OnetMatch3Game()
        val screens=listOf(
            MainMenuScreen(game) to listOf("play","missions","collection","profile","settings","resume_game"),
            ModeSelectScreen(game) to listOf("onet","triple","back"),
            LevelSelectScreen(game) to listOf("level_1","level_20","back"),
            OnetGameScreen(game) to listOf("pause","hint","shuffle"),
            TripleMatchGameScreen(game) to listOf("pause","undo"),
            PauseScreen(game) to listOf("resume","restart","settings","quit"),
            SettingsScreen(game) to listOf("toggle_0","toggle_4","back"),
            CosmeticCollectionScreen(game) to listOf("shop","card_0","back"),
            CosmeticShopScreen(game) to listOf("collection","card_0","back")
        )
        for ((screen,ids) in screens) for ((w,h) in sizes) {
            screen.viewport.setWorldSize(w,h)
            for(id in ids) {
                val r=screen.actionBounds(id)
                assertNotNull(r,"${screen.javaClass.simpleName}: $id")
                assertTrue(r!!.width>=44f&&r.height>=44f,"$id touch target too small")
                assertTrue(r.x>=0&&r.y>=0&&r.x+r.width<=w&&r.y+r.height<=h,"$id outside ${w}x$h")
            }
        }
        screens.forEach { it.first.dispose() }
    }
}
