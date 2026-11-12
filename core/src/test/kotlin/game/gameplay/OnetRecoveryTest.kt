package game.gameplay

import game.gameplay.onet.*
import game.gameplay.shared.BOARD_COLS
import game.gameplay.shared.BOARD_ROWS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OnetRecoveryTest {
    private fun board(): List<List<Tile?>> {
        val result = List(BOARD_ROWS) { MutableList<Tile?>(BOARD_COLS) { null } }
        result[1][1] = Tile("candy_0")
        result[1][2] = Tile("candy_0")
        return result
    }

    @Test fun `moving device clock backwards cannot extend a combo`() {
        var round = createOnetRound(board = board()).copy(lastMatchAt = 10000L, combo = 8)
        round = selectTile(round, 1, 1, 500L)
        round = selectTile(round, 1, 2, 500L)
        assertEquals(1, round.combo)
        assertEquals(100, round.pending!!.points)
    }

    @Test fun `invalid restored tile references do not crash input`() {
        val restored = createOnetRound(board = board()).copy(selected = Point(999, 999))
        val selected = selectTile(restored, 1, 2)
        assertEquals(Point(1, 2), selected.selected)
        val corrupt = board().map { it.toMutableList() }
        corrupt[1][1] = Tile("candy_0", isSlave = true, masterR = -1, masterC = 999)
        val round = createOnetRound(board = corrupt)
        assertSame(round, selectTile(round, 1, 1))
    }

    @Test fun `small tutorial boards are deterministic and clear through legal original paths`() {
        for (level in 1..3) {
            var round = createOnetLevelRound("candy", level, game.gameplay.shared.SeededRandom("lesson-$level"))
            assertEquals(level * 8, countRemaining(round.board))
            assertEquals(round, createOnetLevelRound("candy", level, game.gameplay.shared.SeededRandom("lesson-$level")))
            repeat(level * 4) {
                val hint = requireNotNull(hintFor(round))
                round = selectTile(round, hint.p1.r, hint.p1.c, 0)
                round = selectTile(round, hint.p2.r, hint.p2.c, 0)
                assertNotNull(round.pending)
                round = resolveMatch(round)
            }
            assertEquals("won", round.phase)
        }
        assertEquals(60, countRemaining(createOnetRound(theme = "candy").board))
    }

    @Test fun `large level numbers cannot overflow the Onet timer`() {
        assertEquals(75, getOnetDifficulty(Int.MAX_VALUE).time)
        assertEquals(150, getOnetDifficulty(1).time)
    }
}
