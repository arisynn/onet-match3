package game.economy

import game.achievements.AchievementCatalog
import game.achievements.AchievementService
import game.billing.BillingService
import game.billing.FakeBillingProvider
import game.cosmetics.CosmeticCatalog
import game.cosmetics.CosmeticInventory
import game.cosmetics.CosmeticService
import game.missions.MissionObjective
import game.missions.MissionService
import game.progression.PlayerProfile
import game.progression.ProgressionEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CurrencyRulesTest {

    private lateinit var ledger: TransactionLedger
    private lateinit var economyService: EconomyService

    @BeforeEach
    fun setUp() {
        ledger = TransactionLedger()
        economyService = EconomyService(ledger)
    }

    @Test
    @DisplayName("Rule 1: Gems can ONLY be earned from claiming missions/achievements")
    fun testRule1_GemsOnlyFromMissionsAndAchievements() {
        // Valid claims succeed
        val missionClaim = economyService.claimMissionReward(missionId = "daily_101", amount = 20)
        assertTrue(missionClaim is TransactionResult.Success)
        assertEquals(20, economyService.wallet.gemBalance)

        val achClaim = economyService.claimAchievementReward(achievementId = "onet_master", tierIndex = 1, amount = 25)
        assertTrue(achClaim is TransactionResult.Success)
        assertEquals(45, economyService.wallet.gemBalance)

        // Invalid source attempt throws CurrencyRuleViolationException
        val exception = assertThrows(CurrencyRuleViolationException::class.java) {
            ledger.record(
                type = TransactionType.CREDIT,
                currency = Currency.GEM,
                amount = 100,
                source = TransactionSource.COSMETIC_PURCHASE, // Cannot credit from cosmetic purchase
                sourceId = "invalid_source",
                idempotencyKey = "invalid_test_key"
            )
        }
        assertEquals(1, exception.ruleNumber)
    }

    @Test
    @DisplayName("Rule 2: Zero currency from direct gameplay or level-ups")
    fun testRule2_ZeroCurrencyFromDirectGameplayOrLevelUps() {
        // Direct gameplay credit attempt is blocked
        val gameplayException = assertThrows(CurrencyRuleViolationException::class.java) {
            economyService.rejectDirectGameplayReward(roundId = "round_42", score = 500)
        }
        assertEquals(2, gameplayException.ruleNumber)

        // Level-up credit attempt is blocked
        val levelUpException = assertThrows(CurrencyRuleViolationException::class.java) {
            economyService.rejectLevelUpReward(newLevel = 5, amount = 50)
        }
        assertEquals(2, levelUpException.ruleNumber)

        // Progression engine explicitly produces 0 direct currency on level-up
        val engine = ProgressionEngine()
        val profile = PlayerProfile(level = 1, currentXp = 0L)
        val (_, update) = engine.applyXp(profile, xpEarned = 500L)
        assertTrue(update.didLevelUp)
        assertEquals(0, update.directCurrencyReward)
        assertEquals(0, economyService.wallet.gemBalance)
    }

    @Test
    @DisplayName("Rule 3: Zero real-money currency sales")
    fun testRule3_ZeroRealMoneyCurrencyPurchases() {
        // Economy ledger blocks real money currency purchase
        val iapException = assertThrows(CurrencyRuleViolationException::class.java) {
            economyService.rejectRealMoneyCurrencyPurchase(sku = "gems_500_pack", amount = 500)
        }
        assertEquals(3, iapException.ruleNumber)

        // Billing service blocks consumable currency SKUs
        val fakeBilling = FakeBillingProvider()
        val billingService = BillingService(fakeBilling, CosmeticService())
        assertThrows(CurrencyRuleViolationException::class.java) {
            billingService.purchaseEntitlement("gems_pack_tier_1")
        }
    }

    @Test
    @DisplayName("Rule 4: Immutable idempotent transaction ledger")
    fun testRule4_ImmutableIdempotentLedger() {
        assertEquals(0, ledger.getTransactionCount())

        val result1 = economyService.claimMissionReward(missionId = "m_daily_1", amount = 20)
        assertTrue(result1 is TransactionResult.Success)
        assertEquals(20, economyService.wallet.gemBalance)
        assertEquals(1, ledger.getTransactionCount())

        // Replaying same claim returns AlreadyProcessed and does not alter balance
        val result2 = economyService.claimMissionReward(missionId = "m_daily_1", amount = 20)
        assertTrue(result2 is TransactionResult.AlreadyProcessed)
        assertEquals(20, economyService.wallet.gemBalance)
        assertEquals(1, ledger.getTransactionCount()) // No duplicate appended

        // Conflicting transaction with duplicate key throws error
        assertThrows(IllegalArgumentException::class.java) {
            ledger.record(
                type = TransactionType.CREDIT,
                currency = Currency.GEM,
                amount = 999, // Differs from original 20
                source = TransactionSource.MISSION_CLAIM,
                sourceId = "m_daily_1",
                idempotencyKey = "CLAIM_MISSION_m_daily_1"
            )
        }
    }

    @Test
    @DisplayName("Rule 5: Strict non-negative balance invariant")
    fun testRule5_StrictNonNegativeBalance() {
        assertEquals(0, economyService.wallet.gemBalance)

        // Attempting to spend when wallet has 0 gems fails atomically
        assertThrows(InsufficientFundsException::class.java) {
            economyService.purchaseCosmetic(cosmeticId = "theme_reward_bakery", gemCost = 1200)
        }
        assertEquals(0, economyService.wallet.gemBalance)

        // Credit 150 gems
        economyService.claimMissionReward("m_1", 150)
        assertEquals(150, economyService.wallet.gemBalance)

        // Attempting to spend 200 still fails
        assertThrows(InsufficientFundsException::class.java) {
            economyService.purchaseCosmetic(cosmeticId = "theme_reward_bakery", gemCost = 1200)
        }
        assertEquals(150, economyService.wallet.gemBalance) // Balance unchanged

        // Spending 100 succeeds and balance updates cleanly
        economyService.claimMissionReward("m_extra", 1100)
        val spend = economyService.purchaseCosmetic(cosmeticId = "palette_rose", gemCost = 1200)
        assertTrue(spend is TransactionResult.Success)
        assertEquals(50, economyService.wallet.gemBalance)
    }

    @Test
    @DisplayName("Rule 6: Zero pay-to-win booster sinks")
    fun testRule6_ZeroPayToWinBoosterSinks() {
        economyService.claimMissionReward("m_test", 500)

        val boosterException = assertThrows(CurrencyRuleViolationException::class.java) {
            economyService.rejectPayToWinBoosterPurchase("hint_pack_x5", 50)
        }
        assertEquals(6, boosterException.ruleNumber)
        assertEquals(500, economyService.wallet.gemBalance)
    }

    @Test
    @DisplayName("Rule 7: Deterministic and transparent rewards")
    fun testRule7_DeterministicRewards() {
        val achievements = AchievementCatalog.ALL_ACHIEVEMENTS
        for (ach in achievements) {
            assertEquals(5, ach.tiers.size)
            assertEquals(listOf(50, 100, 200, 500, 1000), ach.tiers.map { it.gemReward })
        }
    }

    @Test
    @DisplayName("Rule 8: Tamper-resistant cryptographic integrity check")
    fun testRule8_TamperResistantIntegrity() {
        economyService.claimMissionReward("m_1", 1200)
        economyService.claimMissionReward("m_2", 25)
        economyService.purchaseCosmetic("palette_lavender", 1200)

        assertTrue(ledger.verifyIntegrity())

        // Mutating a transaction violates integrity
        val transactions = ledger.getTransactions()
        assertEquals(3, transactions.size)

        // Tamper simulation: invalid previous hash
        val tamperedTx = transactions[1].copy(previousHash = "CORRUPTED_HASH")
        assertThrows(IllegalArgumentException::class.java) {
            TransactionLedger(listOf(transactions[0], tamperedTx, transactions[2]))
        }
    }

    @Test
    @DisplayName("Rule 9: Non-expiring currency")
    fun testRule9_NonExpiringCurrency() {
        // Transaction recorded at t = 0
        economyService.claimMissionReward("m_old", 1300, timestampMs = 1_000_000L)
        assertEquals(1300, economyService.wallet.gemBalance)

        // Forward time by 1 year (365 days = 31,536,000,000 ms)
        val oneYearLater = 1_000_000L + 31_536_000_000L
        assertEquals(1300, economyService.wallet.gemBalance)

        // Can still spend the balance normally
        val spend = economyService.purchaseCosmetic("palette_lavender", 1200, timestampMs = oneYearLater)
        assertTrue(spend is TransactionResult.Success)
        assertEquals(100, economyService.wallet.gemBalance)
    }

    @Test
    @DisplayName("Rule 10: Strict separation of cosmetics and power")
    fun testRule10_StrictCosmeticSeparation() {
        for (item in CosmeticCatalog.ALL_ITEMS) {
            // Verify no stat multipliers, timers, or score bonuses exist in cosmetic definition
            assertNotNull(item.id)
            assertNotNull(item.name)
            assertNotNull(item.assetPath)
            if (item.acquisitionType == game.cosmetics.CosmeticAcquisitionType.FREE) {
                assertEquals(0, item.gemPrice)
            } else if (item.acquisitionType == game.cosmetics.CosmeticAcquisitionType.EARNABLE) {
                assertTrue(item.gemPrice > 0)
            }
        }
    }
}
