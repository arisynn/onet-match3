package game.economy

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Immutable append-only transaction ledger.
 * Strictly enforces:
 * - Rule 1 & 2: Gems only earned from claiming missions/achievements.
 * - Rule 3: Zero real-money currency sales.
 * - Rule 4: Immutable idempotent transaction ledger.
 * - Rule 5: Strict non-negative balance invariant.
 * - Rule 6: Zero pay-to-win booster sinks.
 * - Rule 8: Tamper-resistant cryptographic integrity check.
 */
class TransactionLedger(initialTransactions: List<Transaction> = emptyList()) {

    private val transactions = mutableListOf<Transaction>()
    private val idempotencyIndex = ConcurrentHashMap<String, Transaction>()

    init {
        initialTransactions.forEach { appendInternal(it) }
    }

    /**
     * Appends a new transaction or returns the existing transaction if idempotent key matches.
     * Thread-safe and atomic.
     */
    @Synchronized
    fun record(
        type: TransactionType,
        currency: Currency,
        amount: Int,
        source: TransactionSource,
        sourceId: String,
        idempotencyKey: String,
        timestampMs: Long = System.currentTimeMillis()
    ): TransactionResult {
        // Idempotency check (Rule 4)
        val existing = idempotencyIndex[idempotencyKey]
        if (existing != null) {
            if (existing.type == type &&
                existing.currency == currency &&
                existing.amount == amount &&
                existing.source == source &&
                existing.sourceId == sourceId
            ) {
                return TransactionResult.AlreadyProcessed(existing)
            } else {
                throw IllegalArgumentException("Idempotency key conflict: key '$idempotencyKey' already used with differing parameters.")
            }
        }

        // Rule Invariant Validations
        validateRuleInvariants(type, currency, amount, source)

        // Rule 5: Sufficiency check for debits
        val currentBalance = calculateBalance(currency)
        if (type == TransactionType.DEBIT && amount > currentBalance) {
            throw InsufficientFundsException(required = amount, available = currentBalance)
        }

        // Cryptographic chaining (Rule 8)
        val previousHash = transactions.lastOrNull()?.hash ?: "GENESIS"
        val transaction = Transaction(
            timestampMs = timestampMs,
            type = type,
            currency = currency,
            amount = amount,
            source = source,
            sourceId = sourceId,
            idempotencyKey = idempotencyKey,
            previousHash = previousHash
        )

        appendInternal(transaction)
        return TransactionResult.Success(transaction, calculateBalance(currency))
    }

    private fun validateRuleInvariants(
        type: TransactionType,
        currency: Currency,
        amount: Int,
        source: TransactionSource
    ) {
        // Rule 1 & 2: Credits can ONLY be earned from claiming missions/achievements.
        if (type == TransactionType.CREDIT) {
            when (source) {
                TransactionSource.FORBIDDEN_DIRECT_GAMEPLAY -> throw CurrencyRuleViolationException(
                    ruleNumber = 2,
                    message = "Zero currency from direct gameplay is permitted. Points/XP rewarded instead."
                )
                TransactionSource.FORBIDDEN_LEVEL_UP -> throw CurrencyRuleViolationException(
                    ruleNumber = 2,
                    message = "Zero currency from level-ups is permitted. Levels grant visual badges and unlocks only."
                )
                TransactionSource.FORBIDDEN_REAL_MONEY_IAP -> throw CurrencyRuleViolationException(
                    ruleNumber = 3,
                    message = "Zero real-money currency purchases. Real money only buys non-consumable cosmetic entitlements."
                )
                TransactionSource.MISSION_CLAIM,
                TransactionSource.ACHIEVEMENT_CLAIM -> {
                    // Valid credit sources
                }
                else -> throw CurrencyRuleViolationException(
                    ruleNumber = 1,
                    message = "Gems can ONLY be earned from claiming completed missions or achievements. Source '$source' rejected."
                )
            }
        }

        // Rule 6: Zero pay-to-win booster sinks.
        if (type == TransactionType.DEBIT) {
            when (source) {
                TransactionSource.FORBIDDEN_PAY_TO_WIN_BOOSTER -> throw CurrencyRuleViolationException(
                    ruleNumber = 6,
                    message = "Zero pay-to-win sinks. Boosters/hints/shuffles cannot be purchased with currency."
                )
                TransactionSource.COSMETIC_PURCHASE -> {
                    // Valid debit source
                }
                else -> throw CurrencyRuleViolationException(
                    ruleNumber = 6,
                    message = "Debits are strictly restricted to cosmetic unlocks. Source '$source' rejected."
                )
            }
        }
    }

    @Synchronized
    private fun appendInternal(tx: Transaction) {
        transactions.add(tx)
        idempotencyIndex[tx.idempotencyKey] = tx
    }

    @Synchronized
    fun getBalance(currency: Currency = Currency.GEM): Int {
        return calculateBalance(currency)
    }

    private fun calculateBalance(currency: Currency): Int {
        var balance = 0
        for (tx in transactions) {
            if (tx.currency == currency) {
                when (tx.type) {
                    TransactionType.CREDIT -> balance += tx.amount
                    TransactionType.DEBIT -> balance -= tx.amount
                }
            }
        }
        return balance
    }

    @Synchronized
    fun getTransactions(): List<Transaction> {
        return Collections.unmodifiableList(ArrayList(transactions))
    }

    @Synchronized
    fun getTransactionCount(): Int = transactions.size

    /**
     * Rule 8: Tamper-resistant cryptographic integrity check.
     * Verifies that all transactions form an unbroken hash chain and match their payload hashes.
     */
    @Synchronized
    fun verifyIntegrity(): Boolean {
        var expectedPreviousHash = "GENESIS"
        for (tx in transactions) {
            if (tx.previousHash != expectedPreviousHash) {
                return false
            }
            val expectedHash = Transaction.computeHash(
                id = tx.id,
                timestampMs = tx.timestampMs,
                type = tx.type,
                currency = tx.currency,
                amount = tx.amount,
                source = tx.source,
                sourceId = tx.sourceId,
                idempotencyKey = tx.idempotencyKey,
                previousHash = tx.previousHash
            )
            if (tx.hash != expectedHash) {
                return false
            }
            expectedPreviousHash = tx.hash
        }
        return true
    }
}

/**
 * Result of a ledger transaction operation.
 */
sealed class TransactionResult {
    data class Success(val transaction: Transaction, val newBalance: Int) : TransactionResult()
    data class AlreadyProcessed(val existingTransaction: Transaction) : TransactionResult()
}
