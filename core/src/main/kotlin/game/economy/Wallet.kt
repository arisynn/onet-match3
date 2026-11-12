package game.economy

/**
 * Player wallet encapsulating currency balances verified through the transaction ledger.
 */
class Wallet(val ledger: TransactionLedger = TransactionLedger()) {

    @get:JvmName("gemBalanceValue")
    val gemBalance: Int
        get() = ledger.getBalance(Currency.GEM)

    fun getGemBalance(): Int = gemBalance

    fun hasSufficientGems(cost: Int): Boolean {
        require(cost >= 0) { "Cost cannot be negative: $cost" }
        return gemBalance >= cost
    }
}
