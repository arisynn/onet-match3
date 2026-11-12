package game.integration

import game.cosmetics.CosmeticCatalog
import game.cosmetics.CosmeticService
import game.cosmetics.PurchaseResult
import game.economy.EconomyService
import game.economy.TransactionLedger
import game.economy.Wallet
import game.gameplay.onet.*
import game.gameplay.shared.SeededRandom
import game.gameplay.triplematch.*
import game.missions.*
import game.progression.PlayerProfile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * End-to-End Gameplay & Economy Integration Suite.
 * Simulates real end-to-end user journeys connecting pure puzzle gameplay,
 * progression tracking, mission completion, ledger transaction recording,
 * and cosmetic store purchases.
 */
class EndToEndGameplayTest {

    @Test
    fun `e2e journey play onet round, trigger matches, achieve mission, claim reward, and purchase cosmetic`() {
        // 1. Initialize Economy, Mission & Cosmetic subsystems
        val ledger = TransactionLedger()
        val wallet = Wallet(ledger)
        val economyService = EconomyService(ledger, wallet)
        val cosmeticService = CosmeticService()

        // Setup active missions
        val tileMission = Mission(
            id = "e2e_daily_tile_master",
            title = "Match Master",
            description = "Match 2 tile pairs",
            type = MissionType.DAILY,
            objective = MissionObjective.MATCH_TILES,
            targetCount = 2,
            currentCount = 0,
            gemReward = 1200,
            status = MissionStatus.IN_PROGRESS,
            createdAtMs = 1000L,
            expiryTimeMs = 100000L
        )
        val missionService = MissionService(initialMissions = listOf(tileMission))
        var profile = PlayerProfile(playerId = "test_user_alpha")

        assertEquals(0, economyService.getGemBalance())
        assertFalse(cosmeticService.inventory.isOwned("theme_reward_bakery"))

        // 2. Start Onet puzzle round
        val rng = SeededRandom("e2e_onet_seed")
        var round = createOnetRound(theme = "candy", level = 1, rng = rng, seconds = 120)

        // Make first match
        val hint1 = hintFor(round)
        assertNotNull(hint1)
        round = selectTile(round, hint1!!.p1.r, hint1.p1.c, now = 1000L)
        round = selectTile(round, hint1.p2.r, hint1.p2.c, now = 1200L)
        round = resolveMatch(round)
        assertEquals(1, round.matches)

        // Update progression & mission progress
        missionService.recordProgress(MissionObjective.MATCH_TILES, amount = 1)
        var missions = missionService.getActiveMissions()
        assertEquals(MissionStatus.IN_PROGRESS, missions[0].status)
        assertEquals(1, missions[0].currentCount)

        // Make second match
        val hint2 = hintFor(round)
        assertNotNull(hint2)
        round = selectTile(round, hint2!!.p1.r, hint2.p1.c, now = 2000L)
        round = selectTile(round, hint2.p2.r, hint2.p2.c, now = 2100L)
        round = resolveMatch(round)
        assertEquals(2, round.matches)

        // Update progression & mission progress
        missionService.recordProgress(MissionObjective.MATCH_TILES, amount = 1)
        missions = missionService.getActiveMissions()
        assertEquals(MissionStatus.COMPLETED, missions[0].status, "Mission should be COMPLETED")
        assertEquals(2, missions[0].currentCount)

        // Verify INVARIANT: Balance remains 0 before claiming (Rule 1 & 2)
        assertEquals(0, economyService.getGemBalance(), "Completing mission must NOT auto-grant gems; player must claim")

        // 3. Claim completed mission reward
        val (claimedMission, txResult) = missionService.claimMission("e2e_daily_tile_master", economyService, timestampMs = 3000L)
        assertEquals(MissionStatus.CLAIMED, claimedMission.status)
        assertEquals(1200, economyService.getGemBalance(), "Gems credited to wallet upon explicit claim")
        assertEquals(1, ledger.getTransactionCount())

        // 4. Purchase Earnable Cosmetic item from Store using Gems
        val purchaseResult = cosmeticService.purchaseEarnable("theme_reward_bakery", economyService, timestampMs = 4000L)
        assertTrue(purchaseResult is PurchaseResult.Success)
        val success = purchaseResult as PurchaseResult.Success
        assertEquals("theme_reward_bakery", success.item.id)
        assertTrue(cosmeticService.inventory.isOwned("theme_reward_bakery"), "Bakery theme should now be unlocked in inventory")

        // Verify Wallet & Ledger after debit
        assertEquals(0, economyService.getGemBalance(), "200 gems spent, remaining balance is 0")
        assertEquals(2, ledger.getTransactionCount(), "Ledger should record 1 credit and 1 debit")
        assertTrue(ledger.verifyIntegrity(), "Ledger cryptographic hash chain remains unbroken")

        // 5. Equip cosmetic theme and start new round
        cosmeticService.inventory.equip("theme_reward_bakery")
        assertEquals("theme_reward_bakery", cosmeticService.inventory.getEquipped(CosmeticCatalog.THEME_REWARD_BAKERY.type))

        val bakeryRound = createOnetRound(theme = "reward_bakery", level = 1, rng = SeededRandom("bakery_round"))
        assertEquals("reward_bakery", bakeryRound.theme)
        assertEquals(60, countRemaining(bakeryRound.board))
    }

    @Test
    fun `e2e journey play triple match round to victory with no direct currency drop`() {
        val tilePool = listOf("a", "b", "c", "d", "e", "f")
        // Controlled deterministic stacks
        val customStacks = listOf(
            listOf("a", "b"),
            listOf("a", "b"),
            listOf("a", "b")
        )
        var s = createTripleRound(tilePool, stacks = customStacks, seconds = 60)
        s = startTripleRound(s)
        assertEquals("playing", s.phase)

        // Clear top layer ("b", "b", "b")
        s = pickTile(s, 0)
        s = pickTile(s, 1)
        s = pickTile(s, 2)
        assertEquals(1, s.cleared)
        assertEquals(0, s.tray.size)

        // Clear bottom layer ("a", "a", "a")
        s = pickTile(s, 0)
        s = pickTile(s, 1)
        s = pickTile(s, 2)
        assertEquals("won", s.phase)
        assertEquals(2, s.cleared)
        assertEquals(300, tripleScore(s)) // 2 trios * 150 = 300 score

        // Invariant check: Round state contains score, time, but ZERO currency tokens
        val result = tripleResult(s)
        assertEquals("won", result.phase)
        assertEquals(300, result.score)
        assertEquals(2, result.matches)
    }
}
