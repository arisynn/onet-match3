package game.progression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProgressionEngineTest {

    private lateinit var engine: ProgressionEngine

    @BeforeEach
    fun setUp() {
        engine = ProgressionEngine()
    }

    @Test
    fun testLevelCurveCalculation() {
        assertEquals(0L, engine.xpRequiredForLevel(1))
        assertTrue(engine.xpRequiredForLevel(2) > 0)
        assertTrue(engine.xpRequiredForLevel(3) > engine.xpRequiredForLevel(2))

        assertEquals(1, engine.calculateLevelForXp(0L))
        assertEquals(1, engine.calculateLevelForXp(50L))
    }

    @Test
    fun testOnetXpCalculation() {
        val winXp = engine.calculateOnetXp(
            won = true,
            tilesMatched = 60,
            maxCombo = 5,
            remainingTimeRatio = 0.50f
        )
        // 50 (base) + 25 (combo) + 10 (speed) + 120 (tiles) = 205
        assertEquals(205L, winXp)

        val lossXp = engine.calculateOnetXp(
            won = false,
            tilesMatched = 20,
            maxCombo = 2,
            remainingTimeRatio = 0.0f
        )
        // 20 * 2 = 40
        assertEquals(40L, lossXp)
    }

    @Test
    fun testTripleMatchXpCalculation() {
        val winXp = engine.calculateTripleMatchXp(
            won = true,
            triosMatched = 12,
            emptyTraySlotsRemaining = 5
        )
        // 60 (base) + 50 (tray) + 60 (trios) = 170
        assertEquals(170L, winXp)
    }

    @Test
    fun testLevelUpFlowGuaranteesZeroCurrency() {
        val profile = PlayerProfile(level = 1, currentXp = 0L)
        val (updatedProfile, update) = engine.applyXp(profile, 1000L)

        assertTrue(update.didLevelUp)
        assertTrue(updatedProfile.level > 1)
        assertEquals(0, update.directCurrencyReward) // Rule 2 strict invariant
    }
}
