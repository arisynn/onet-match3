package game.economy

/**
 * Currency types in Onet + Triple Match.
 * Rule 1: GEMS is the exclusive soft currency in the game.
 */
enum class Currency {
    GEM
}

/**
 * Direction of a balance transaction.
 */
enum class TransactionType {
    CREDIT,
    DEBIT
}

/**
 * Permitted sources of transactions.
 * Strict enforcement of Non-Negotiable Currency Rules 1, 2, 3, and 6:
 * - Credits are ONLY permitted from MISSION_CLAIM or ACHIEVEMENT_CLAIM.
 * - Debits are ONLY permitted for COSMETIC_PURCHASE.
 * - Direct gameplay, level-ups, and real-money currency sales are strictly forbidden.
 */
enum class TransactionSource {
    MISSION_CLAIM,
    ACHIEVEMENT_CLAIM,
    COSMETIC_PURCHASE,

    // Prohibited sources included for explicit security assertion testing
    FORBIDDEN_DIRECT_GAMEPLAY,
    FORBIDDEN_LEVEL_UP,
    FORBIDDEN_REAL_MONEY_IAP,
    FORBIDDEN_PAY_TO_WIN_BOOSTER
}

/**
 * Thrown when any action violates one of the 10 Non-Negotiable Currency Rules.
 */
class CurrencyRuleViolationException(
    val ruleNumber: Int,
    override val message: String
) : IllegalStateException("Rule $ruleNumber Violation: $message")

/**
 * Thrown when a debit transaction exceeds current available balance.
 * Enforces Rule 5: Strict Non-Negative Balance.
 */
class InsufficientFundsException(
    val required: Int,
    val available: Int
) : IllegalStateException("Insufficient funds: required $required, but available balance is $available.")
