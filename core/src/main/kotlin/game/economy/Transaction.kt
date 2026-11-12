package game.economy

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * Immutable transaction entry in the append-only ledger.
 * Enforces Rule 4 (Immutable idempotent ledger) and Rule 8 (Tamper-resistant verification).
 */
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val type: TransactionType,
    val currency: Currency = Currency.GEM,
    val amount: Int,
    val source: TransactionSource,
    val sourceId: String,
    val idempotencyKey: String,
    val previousHash: String = "GENESIS",
    val hash: String = computeHash(id, timestampMs, type, currency, amount, source, sourceId, idempotencyKey, previousHash)
) {
    init {
        require(amount > 0) { "Transaction amount must be strictly positive, was: $amount" }
        require(idempotencyKey.isNotBlank()) { "Idempotency key must not be blank" }
        require(sourceId.isNotBlank()) { "Source ID must not be blank" }
    }

    companion object {
        fun computeHash(
            id: String,
            timestampMs: Long,
            type: TransactionType,
            currency: Currency,
            amount: Int,
            source: TransactionSource,
            sourceId: String,
            idempotencyKey: String,
            previousHash: String
        ): String {
            val payload = "$id|$timestampMs|$type|$currency|$amount|$source|$sourceId|$idempotencyKey|$previousHash"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(payload.toByteArray(StandardCharsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
