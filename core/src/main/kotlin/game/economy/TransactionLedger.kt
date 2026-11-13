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
 * - Hash chaining detects corruption; it is not authentication against a modified APK.
 */
class TransactionLedger(initialTransactions: List<Transaction> = emptyList()) {

    private val transactions = mutableListOf<Transaction>()
    private val idempotencyIndex = ConcurrentHashMap<String, Transaction>()

    private val transactionIds = mutableSetOf<String>()
    private var balance = 0

    init {
        initialTransactions.forEach { tx ->
            validateRuleInvariants(tx.type, tx.currency, tx.amount, tx.source)
            require(tx.id.isNotBlank() && tx.sourceId.isNotBlank() && tx.idempotencyKey.isNotBlank() && tx.timestampMs >= 0) { "Invalid transaction identity or timestamp" }
            require(tx.id !in transactionIds && !idempotencyIndex.containsKey(tx.idempotencyKey)) { "Duplicate restored transaction" }
            require(tx.previousHash == (transactions.lastOrNull()?.hash ?: "GENESIS")) { "Broken ledger chain" }
            require(tx.hash == Transaction.computeHash(tx.id, tx.timestampMs, tx.type, tx.currency,
                tx.amount, tx.source, tx.sourceId, tx.idempotencyKey, tx.previousHash)) { "Corrupt transaction payload" }
            validateNextBalance(tx.type, tx.amount)
            appendInternal(tx)
        }
    }

    private fun validateNextBalance(type: TransactionType, amount: Int) {
        val next = balance.toLong() + if (type == TransactionType.CREDIT) amount.toLong() else -amount.toLong()
        require(next <= Int.MAX_VALUE) { "Currency balance overflow" }
        if (next < 0) throw InsufficientFundsException(required = amount, available = balance)
    }

    @Synchronized fun findByIdempotencyKey(key: String): Transaction? = idempotencyIndex[key]

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
        require(timestampMs >= 0) { "Timestamp must be nonnegative" }
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

        validateNextBalance(type, amount)

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
        require(amount > 0) { "Transaction amount must be positive" }
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
        require(transactionIds.add(tx.id)) { "Duplicate transaction ID" }
        balance = (balance.toLong() + if (tx.type == TransactionType.CREDIT) tx.amount.toLong() else -tx.amount.toLong()).toInt()
        transactions.add(tx)
        idempotencyIndex[tx.idempotencyKey] = tx
    }

    @Synchronized
    fun getBalance(currency: Currency = Currency.GEM): Int {
        return calculateBalance(currency)
    }

    private fun calculateBalance(currency: Currency): Int {
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
