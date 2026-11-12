package game.security

import game.economy.*
import game.missions.*
import game.cosmetics.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EconomyBoundaryRegressionTest {
    @Test fun `restore rejects duplicate history forged hash forbidden source and overflow`() {
        val ledger = TransactionLedger()
        ledger.record(TransactionType.CREDIT, Currency.GEM, Int.MAX_VALUE,
            TransactionSource.MISSION_CLAIM, "mission", "claim", 1)
        val tx = ledger.getTransactions().single()
        assertThrows(IllegalArgumentException::class.java) { TransactionLedger(listOf(tx, tx)) }
        assertThrows(IllegalArgumentException::class.java) { TransactionLedger(listOf(tx.copy(amount = 1))) }
        val forbidden = Transaction(type = TransactionType.CREDIT, amount = 10,
            source = TransactionSource.FORBIDDEN_DIRECT_GAMEPLAY, sourceId = "game", idempotencyKey = "game")
        assertThrows(CurrencyRuleViolationException::class.java) { TransactionLedger(listOf(forbidden)) }
        assertThrows(IllegalArgumentException::class.java) {
            ledger.record(TransactionType.CREDIT, Currency.GEM, 1, TransactionSource.MISSION_CLAIM, "next", "next", 2)
        }
        assertEquals(Int.MAX_VALUE, ledger.getBalance())
        assertEquals(1, ledger.getTransactionCount())
    }

    @Test fun `daily budget is 100 weekly budget is 200 and clock rollback cannot rotate backward`() {
        val service = MissionService()
        service.refreshMissions(10 * MissionRotationEngine.MS_PER_DAY)
        val missions = service.getActiveMissions()
        assertEquals(100, missions.filter { it.type == MissionType.DAILY }.sumOf { it.gemReward })
        assertEquals(200, missions.filter { it.type == MissionType.WEEKLY }.sumOf { it.gemReward })
        service.refreshMissions(MissionRotationEngine.MS_PER_DAY)
        assertEquals(missions, service.getActiveMissions())
        val restored = MissionService(initialMissions = missions, initialLastRefreshMs = service.lastRefreshTimeMs)
        restored.refreshMissions(0)
        assertEquals(missions, restored.getActiveMissions())
    }

    @Test fun `pending old reward survives without replacing new daily slots`() {
        val day = MissionRotationEngine.MS_PER_DAY
        val service = MissionService()
        service.refreshMissions(day)
        val old = service.getActiveMissions().first { it.type == MissionType.DAILY }
        service.recordProgress(old.objective, Int.MAX_VALUE)
        service.refreshMissions(day * 2)
        assertTrue(service.getActiveMissions().any { it.id == old.id && it.isClaimable })
        val today = service.getActiveMissions().filter { it.id.startsWith("daily_2_") }
        assertEquals(3, today.size)
        assertEquals(100, today.sumOf { it.gemReward })
    }

    @Test fun `forged claimed marker never creates missing ledger reward`() {
        val m = Mission("saved", "Saved", "Saved", MissionType.DAILY, MissionObjective.MATCH_TILES,
            1, 1, 20, MissionStatus.CLAIMED, 1, 100)
        val service = MissionService(initialMissions = listOf(m))
        val economy = EconomyService()
        assertThrows(IllegalStateException::class.java) { service.claimMission("saved", economy, 3) }
        assertEquals(0, economy.gemBalance)
    }

    @Test fun `negative progress wrong slot forged price and unknown unlock fail safely`() {
        val missions = MissionService()
        assertThrows(IllegalArgumentException::class.java) { missions.recordProgress(MissionObjective.MATCH_TILES, -1) }
        val inventory = CosmeticInventory(initialUnlockedIds = setOf("missing"), initialEquipped = mapOf(CosmeticType.TILE_BODY to "theme_candy"))
        assertFalse(inventory.isOwned("missing"))
        assertFalse(inventory.unlock("missing"))
        assertFalse(inventory.equip("theme_candy", CosmeticType.TILE_BODY))
        assertEquals("body_square_ivory", inventory.getEquipped(CosmeticType.TILE_BODY))
        val economy = EconomyService()
        assertThrows(IllegalArgumentException::class.java) { economy.purchaseCosmetic("theme_reward_bakery", 1) }
        assertThrows(IllegalArgumentException::class.java) { economy.purchaseCosmetic("theme_candy", 1) }
        assertEquals(0, economy.ledger.getTransactionCount())
    }
}
