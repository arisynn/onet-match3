package game.missions

import game.economy.EconomyService
import game.economy.TransactionLedger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class MissionSystemTest {

    private lateinit var rotationEngine: MissionRotationEngine
    private lateinit var missionService: MissionService
    private lateinit var economyService: EconomyService

    @BeforeEach
    fun setUp() {
        rotationEngine = MissionRotationEngine(Random(12345))
        missionService = MissionService(rotationEngine)
        economyService = EconomyService(TransactionLedger())
    }

    @Test
    fun testDailyMissionGeneration() {
        val dailies = rotationEngine.generateDailyMissions(currentTimeMs = 1000L, dayEpoch = 1L)
        assertEquals(MissionRotationEngine.DAILY_SLOT_COUNT, dailies.size)
        for (m in dailies) {
            assertEquals(MissionType.DAILY, m.type)
            assertEquals(MissionStatus.IN_PROGRESS, m.status)
            assertTrue(m.gemReward > 0)
        }
    }

    @Test
    fun testMissionProgressAndClaim() {
        missionService.refreshMissions(currentTimeMs = 86_400_000L)
        val active = missionService.getActiveMissions()
        assertTrue(active.isNotEmpty())

        val matchMission = active.first { it.objective == MissionObjective.MATCH_TILES }
        assertFalse(matchMission.isComplete)

        // Record partial progress
        missionService.recordProgress(MissionObjective.MATCH_TILES, amount = matchMission.targetCount / 2)
        val updated1 = missionService.getActiveMissions().first { it.id == matchMission.id }
        assertFalse(updated1.isComplete)
        assertEquals(MissionStatus.IN_PROGRESS, updated1.status)

        // Complete the mission
        missionService.recordProgress(MissionObjective.MATCH_TILES, amount = matchMission.targetCount)
        val updated2 = missionService.getActiveMissions().first { it.id == matchMission.id }
        assertTrue(updated2.isComplete)
        assertEquals(MissionStatus.COMPLETED, updated2.status)

        // Claim reward
        val initialBalance = economyService.wallet.gemBalance
        val (claimedMission, tx) = missionService.claimMission(updated2.id, economyService)
        assertEquals(MissionStatus.CLAIMED, claimedMission.status)
        assertEquals(initialBalance + updated2.gemReward, economyService.wallet.gemBalance)

        // Second claim attempt is idempotent and does not credit double
        val (claimedAgain, tx2) = missionService.claimMission(updated2.id, economyService)
        assertEquals(MissionStatus.CLAIMED, claimedAgain.status)
        assertEquals(initialBalance + updated2.gemReward, economyService.wallet.gemBalance)
    }

    @Test
    fun testPreserveUnclaimedCompletedMissionsAcrossRollover() {
        missionService.refreshMissions(currentTimeMs = 86_400_000L) // Day 1
        val active = missionService.getActiveMissions().filter { it.type == MissionType.DAILY }
        val target = active.first()

        // Complete target mission
        missionService.recordProgress(target.objective, amount = target.targetCount)
        val completed = missionService.getActiveMissions().first { it.id == target.id }
        assertEquals(MissionStatus.COMPLETED, completed.status)

        // Advance to Day 2 without claiming
        val day2Ms = 2 * 86_400_000L + 1000L
        missionService.refreshMissions(currentTimeMs = day2Ms)

        // Verify the completed unclaimed mission was preserved!
        val preserved = missionService.getActiveMissions().find { it.id == target.id }
        assertNotNull(preserved)
        assertEquals(MissionStatus.COMPLETED, preserved?.status)

        // Player can now claim it on Day 2
        missionService.claimMission(target.id, economyService)
        assertEquals(target.gemReward, economyService.wallet.gemBalance)
    }
}
