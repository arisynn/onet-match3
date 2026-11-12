package game.gameplay

import game.gameplay.shared.SeededRandom
import game.gameplay.shared.tileIdsFor
import game.gameplay.triplematch.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CampaignGenerationTest {
    @Test fun `campaign boards have winning legal witnesses across difficulty tiers and seeds`() {
        for (level in listOf(1, 2, 3, 4, 9, 10, 24, 25, 49, 50, 100, Int.MAX_VALUE)) {
            for (seed in 0 until 8) {
                val generated = generateTripleLevel(tileIdsFor("candy"), level, SeededRandom("$level:$seed"))
                assertEquals(generated, generateTripleLevel(tileIdsFor("candy"), level, SeededRandom("$level:$seed")))
                assertEquals(12, generated.stacks.size)
                assertTrue(generated.stacks.all { it.size == generated.difficulty.stackDepth })
                assertTrue(generated.stacks.flatten().groupingBy { it }.eachCount().values.all { it % 3 == 0 })
                var round = startTripleRound(createTripleRound(emptyList(), stacks = generated.stacks,
                    seconds = generated.difficulty.seconds))
                generated.solution.forEach { index ->
                    assertEquals("playing", round.phase, "premature end at level $level, seed $seed")
                    val before = remainingTiles(round)
                    round = pickTile(round, index)
                    assertEquals(before - 1, remainingTiles(round), "witness must only pick accessible tiles")
                    assertTrue(round.tray.size < TRAY_CAPACITY)
                }
                assertEquals("won", round.phase)
                assertTrue(round.tray.isEmpty())
                assertEquals(0, remainingTiles(round))
            }
        }
    }

    @Test fun `campaign difficulty grows while source generator preserves original shape`() {
        assertTrue(getTripleDifficulty(1).tutorial)
        assertEquals(1, getTripleDifficulty(1).stackDepth)
        assertFalse(getTripleDifficulty(4).tutorial)
        assertEquals(6, getTripleDifficulty(50).stackDepth)
        assertEquals(12, getTripleDifficulty(50).tileTypes)
        assertThrows(IllegalArgumentException::class.java) { getTripleDifficulty(0) }
        assertThrows(IllegalArgumentException::class.java) {
            generateTripleLevel(List(6) { "same" }, 1, SeededRandom("invalid"))
        }
        val legacy = createStacks(tileIdsFor("candy"), SeededRandom("legacy"))
        assertEquals(12, legacy.size)
        assertTrue(legacy.all { it.size == 3 })
        val campaign = createTripleLevelRound(tileIdsFor("candy"), 1, SeededRandom("tutorial"))
        assertEquals("ready", campaign.phase)
        assertEquals(12, remainingTiles(campaign))
    }

    @Test fun `legacy stacks match an executed JavaScript reference fixture`() {
        // Captured by running src/match3/index.js with src/shared/random.js, seed "triple".
        val expected = "candy_12,candy_15,candy_15;candy_15,candy_7,candy_2;candy_6,candy_7,candy_15;" +
            "candy_12,candy_12,candy_12;candy_15,candy_15,candy_0;candy_7,candy_0,candy_2;" +
            "candy_15,candy_15,candy_15;candy_6,candy_0,candy_2;candy_6,candy_12,candy_0;" +
            "candy_7,candy_0,candy_12;candy_7,candy_7,candy_12;candy_12,candy_12,candy_0"
        val actual = createStacks(tileIdsFor("candy"), SeededRandom("triple"))
            .joinToString(";") { it.joinToString(",") }
        assertEquals(expected, actual)
    }

    @Test fun `campaign accommodates smaller owned themes without missing tile IDs`() {
        val ids = tileIdsFor("reward_sakura")
        val generated = generateTripleLevel(ids, 100, SeededRandom("smaller-theme"))
        assertEquals(10, generated.difficulty.tileTypes)
        assertTrue(generated.stacks.flatten().all { it in ids })
    }
}
