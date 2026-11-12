package game.security

import game.economy.*
import game.missions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Security & Anti-Exploit Test Suite.
 * Enforces the 10 Non-Negotiable Currency Rules against malicious tampering,
 * replay attacks, race conditions, and double-claim exploits.
 */
class RewardDuplicationAttackTest {

    // =========================================================================
    // Rule 1 & 2: Currency Origin & Direct Gameplay Invariants
    // =========================================================================

    @Test
    fun `attack direct gameplay gem injection is strictly rejected`() {
        val ledger = TransactionLedger()

        val ex = assertThrows<CurrencyRuleViolationException> {
            ledger.record(
                type = TransactionType.CREDIT,
                currency = Currency.GEM,
                amount = 100,
                source = TransactionSource.FORBIDDEN_DIRECT_GAMEPLAY,
                sourceId = "exploit_level_clear_reward",
                idempotencyKey = "tx_hack_01"
            )
        }
        assertEquals(2, ex.ruleNumber)
        assertEquals(0, ledger.getBalance())
    }

    @Test
    fun `attack level up gem grant is strictly rejected`() {
        val ledger = TransactionLedger()

        val ex = assertThrows<CurrencyRuleViolationException> {
            ledger.record(
                type = TransactionType.CREDIT,
                currency = Currency.GEM,
                amount = 50,
                source = TransactionSource.FORBIDDEN_LEVEL_UP,
                sourceId = "exploit_rank_up",
                idempotencyKey = "tx_hack_02"
            )
        }
        assertEquals(2, ex.ruleNumber)
        assertEquals(0, ledger.getBalance())
    }

    @Test
    fun `attack real money currency purchase is strictly rejected`() {
        val ledger = TransactionLedger()

        val ex = assertThrows<CurrencyRuleViolationException> {
            ledger.record(
                type = TransactionType.CREDIT,
                currency = Currency.GEM,
                amount = 1000,
                source = TransactionSource.FORBIDDEN_REAL_MONEY_IAP,
                sourceId = "pouch_of_gems_purchase",
                idempotencyKey = "tx_hack_03"
            )
        }
        assertEquals(3, ex.ruleNumber)
        assertEquals(0, ledger.getBalance())
    }

    // =========================================================================
    // Rule 4: Idempotency & Duplicate Claim Prevention
    // =========================================================================

    @Test
    fun `attack sequential double claim on completed mission returns cached result without double crediting`() {
        val ledger = TransactionLedger()
        val wallet = Wallet(ledger)
        val economyService = EconomyService(ledger, wallet)

        val mission = Mission(
            id = "daily_test_01",
            title = "Master Matcher",
            description = "Match 100 tiles",
            type = MissionType.DAILY,
            objective = MissionObjective.MATCH_TILES,
            targetCount = 100,
            currentCount = 100,
            gemReward = 25,
            status = MissionStatus.COMPLETED,
            createdAtMs = 1000L,
            expiryTimeMs = 100000L
        )

        val missionService = MissionService(initialMissions = listOf(mission))

        // First legitimate claim
        val (m1, res1) = missionService.claimMission("daily_test_01", economyService, timestampMs = 5000L)
        assertEquals(MissionStatus.CLAIMED, m1.status)
        assertTrue(res1 is TransactionResult.Success)
        assertEquals(25, (res1 as TransactionResult.Success).newBalance)
        assertEquals(25, economyService.getGemBalance())
        assertEquals(1, ledger.getTransactionCount())

        // Second duplicate claim attempt with identical mission ID
        val (m2, res2) = missionService.claimMission("daily_test_01", economyService, timestampMs = 6000L)
        assertEquals(MissionStatus.CLAIMED, m2.status)
        assertTrue(res2 is TransactionResult.AlreadyProcessed, "Duplicate claim must return AlreadyProcessed")
        assertEquals(25, economyService.getGemBalance(), "Gem balance must remain exactly 25, zero inflation")
        assertEquals(1, ledger.getTransactionCount(), "Transaction count must not increase")
    }

    @Test
    fun `attack replay transaction with same idempotency key but altered payload fails with conflict`() {
        val ledger = TransactionLedger()

        // Legitimate transaction
        val res1 = ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = 20,
            source = TransactionSource.MISSION_CLAIM,
            sourceId = "mission_01",
            idempotencyKey = "idemp_key_alpha"
        )
        assertTrue(res1 is TransactionResult.Success)
        assertEquals(20, ledger.getBalance())

        // Attacker replays same idempotency key with inflated amount (e.g. 500 gems)
        assertThrows<IllegalArgumentException> {
            ledger.record(
                type = TransactionType.CREDIT,
                currency = Currency.GEM,
                amount = 500,
                source = TransactionSource.MISSION_CLAIM,
                sourceId = "mission_01",
                idempotencyKey = "idemp_key_alpha"
            )
        }

        // Ledger balance remains unaltered
        assertEquals(20, ledger.getBalance())
        assertEquals(1, ledger.getTransactionCount())
    }

    @Test
    fun `attack concurrent multi-threaded double claim attempt results in single payout`() {
        val ledger = TransactionLedger()
        val wallet = Wallet(ledger)
        val economyService = EconomyService(ledger, wallet)

        val mission = Mission(
            id = "concurrency_target",
            title = "Combo God",
            description = "Get combo 10",
            type = MissionType.DAILY,
            objective = MissionObjective.COMBO_STREAK,
            targetCount = 10,
            currentCount = 10,
            gemReward = 50,
            status = MissionStatus.COMPLETED,
            createdAtMs = 1000L,
            expiryTimeMs = 100000L
        )

        val missionService = MissionService(initialMissions = listOf(mission))
        val threadCount = 16
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val results = ConcurrentLinkedQueue<TransactionResult>()

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    startLatch.await()
                    val (_, res) = missionService.claimMission("concurrency_target", economyService, timestampMs = 10000L)
                    results.add(res)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS))
        executor.shutdown()

        assertEquals(threadCount, results.size)
        val successCount = results.count { it is TransactionResult.Success }
        val alreadyProcessedCount = results.count { it is TransactionResult.AlreadyProcessed }

        assertEquals(1, successCount, "Exactly one thread must succeed with new transaction")
        assertEquals(threadCount - 1, alreadyProcessedCount, "All other concurrent calls must return AlreadyProcessed")
        assertEquals(50, economyService.getGemBalance(), "Balance must be exactly 50 after concurrent race")
        assertEquals(1, ledger.getTransactionCount(), "Ledger must contain exactly 1 transaction entry")
    }

    // =========================================================================
    // Rule 5: Non-Negative Balance & Overdraft Prevention
    // =========================================================================

    @Test
    fun `attack overspending wallet balance throws InsufficientFundsException`() {
        val ledger = TransactionLedger()

        // Give player 30 gems from mission
        ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = 30,
            source = TransactionSource.MISSION_CLAIM,
            sourceId = "m_init",
            idempotencyKey = "tx_init"
        )
        assertEquals(30, ledger.getBalance())

        // Attempt to spend 35 gems on cosmetic
        assertThrows<InsufficientFundsException> {
            ledger.record(
                type = TransactionType.DEBIT,
                currency = Currency.GEM,
                amount = 35,
                source = TransactionSource.COSMETIC_PURCHASE,
                sourceId = "cosmetic_sakura_theme",
                idempotencyKey = "tx_overdraft"
            )
        }

        assertEquals(30, ledger.getBalance(), "Balance must remain unchanged after failed overdraft")
    }

    // =========================================================================
    // Rule 6: Sinks Restricted to Cosmetics (Zero Pay-to-Win Boosters)
    // =========================================================================

    @Test
    fun `attack purchasing gameplay booster or hint with currency is blocked`() {
        val ledger = TransactionLedger()

        ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = 100,
            source = TransactionSource.MISSION_CLAIM,
            sourceId = "m_funds",
            idempotencyKey = "tx_funds"
        )

        val ex = assertThrows<CurrencyRuleViolationException> {
            ledger.record(
                type = TransactionType.DEBIT,
                currency = Currency.GEM,
                amount = 20,
                source = TransactionSource.FORBIDDEN_PAY_TO_WIN_BOOSTER,
                sourceId = "booster_hint_pack",
                idempotencyKey = "tx_p2w_sink"
            )
        }
        assertEquals(6, ex.ruleNumber)
        assertEquals(100, ledger.getBalance())
    }

    // =========================================================================
    // Rule 8: Cryptographic Chaining & Save State Tampering
    // =========================================================================

    @Test
    fun `ledger detects cryptographic tampering and breaks verification chain`() {
        val ledger = TransactionLedger()

        ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = 25,
            source = TransactionSource.MISSION_CLAIM,
            sourceId = "m1",
            idempotencyKey = "tx_chain_1"
        )
        ledger.record(
            type = TransactionType.CREDIT,
            currency = Currency.GEM,
            amount = 30,
            source = TransactionSource.MISSION_CLAIM,
            sourceId = "m2",
            idempotencyKey = "tx_chain_2"
        )

        assertTrue(ledger.verifyIntegrity(), "Clean ledger must verify integrity")

        // Maliciously reconstruct ledger with tampered transaction amount in history
        val originalTxList = ledger.getTransactions()
        val tamperedTx = originalTxList[0].copy(amount = 9999) // Tamper amount without valid hash
        val tamperedList = listOf(tamperedTx, originalTxList[1])

        val tamperedLedger = TransactionLedger(tamperedList)
        assertFalse(tamperedLedger.verifyIntegrity(), "Tampered transaction payload must fail hash integrity check")
    }
}
