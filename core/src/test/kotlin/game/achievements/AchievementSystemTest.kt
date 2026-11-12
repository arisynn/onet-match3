package game.achievements

import game.economy.EconomyService
import game.economy.TransactionLedger
import game.progression.PlayerStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AchievementSystemTest {

    private lateinit var service: AchievementService
    private lateinit var economyService: EconomyService

    @BeforeEach
    fun setUp() {
        service = AchievementService(AchievementCatalog.createInitialCatalog())
        economyService = EconomyService(TransactionLedger())
    }

    @Test
    fun testAchievementCatalogStructure() {
        val all = service.getAchievements()
        assertEquals(6, all.size)
        for (ach in all) {
            assertEquals(5, ach.tiers.size)
            assertEquals(listOf(1, 2, 3, 4, 5), ach.tiers.map { it.tierIndex })
            assertEquals(listOf(50, 100, 200, 500, 1000), ach.tiers.map { it.gemReward })
        }
    }

    @Test
    fun testSyncStatsAndTierUnlocking() {
        val stats = PlayerStats(
            totalOnetRoundsWon = 55, // Qualifies for Tier 1 (10) and Tier 2 (50) of onet_master
            highestComboStreak = 12   // Qualifies for Tier 1 (5) and Tier 2 (10) of combo_king
        )
        service.syncStats(stats)

        val onetMaster = service.getAchievement("onet_master")!!
        assertEquals(55L, onetMaster.currentValue)
        assertTrue(onetMaster.isTierClaimable(1))
        assertTrue(onetMaster.isTierClaimable(2))
        assertFalse(onetMaster.isTierClaimable(3)) // Tier 3 requires 200

        // Claim Tier 1 (25 gems)
        val (updated1, tx1) = service.claimTier("onet_master", 1, economyService)
        assertEquals(50, economyService.wallet.gemBalance)
        assertTrue(updated1.tiers.first { it.tierIndex == 1 }.isClaimed)
        assertFalse(updated1.isTierClaimable(1)) // No longer claimable

        // Claim Tier 2 (50 gems)
        val (updated2, tx2) = service.claimTier("onet_master", 2, economyService)
        assertEquals(150, economyService.wallet.gemBalance)
        assertTrue(updated2.tiers.first { it.tierIndex == 2 }.isClaimed)

        // Attempting to claim Tier 3 before reaching 200 throws exception
        assertThrows(IllegalStateException::class.java) {
            service.claimTier("onet_master", 3, economyService)
        }
    }

    @Test
    fun testIdempotentTierClaiming() {
        val stats = PlayerStats(totalOnetRoundsWon = 20)
        service.syncStats(stats)

        val (firstClaim, _) = service.claimTier("onet_master", 1, economyService)
        assertEquals(50, economyService.wallet.gemBalance)

        // Repeat claim call
        val (repeatClaim, _) = service.claimTier("onet_master", 1, economyService)
        assertEquals(50, economyService.wallet.gemBalance) // Unchanged!
    }
}
