package game.billing

/**
 * Record of a verified purchase transaction from Google Play Billing.
 */
data class PurchaseRecord(
    val skuId: String,
    val purchaseToken: String,
    val purchaseTimeMs: Long,
    val isAcknowledged: Boolean
)

/**
 * Result of a Google Play purchase flow.
 */
sealed class BillingResult {
    data class Success(val purchase: PurchaseRecord) : BillingResult()
    data object UserCancelled : BillingResult()
    data class ItemAlreadyOwned(val skuId: String) : BillingResult()
    data class Error(val code: Int, val message: String) : BillingResult()
}

/**
 * Abstraction over Google Play Billing Client.
 * Decouples core logic from Android platform dependencies.
 */
interface BillingProvider {
    fun isConnected(): Boolean
    fun getAvailableEntitlements(): List<Entitlement>
    fun queryPurchasedSkus(): Set<String>
    fun launchPurchaseFlow(skuId: String): BillingResult
    fun acknowledgePurchase(purchaseToken: String): Boolean
}

/**
 * In-memory test provider simulating Google Play Billing.
 */
class FakeBillingProvider(
    private val availableEntitlements: List<Entitlement> = EntitlementCatalog.ALL_ENTITLEMENTS
) : BillingProvider {

    private val ownedPurchases = mutableMapOf<String, PurchaseRecord>()
    private var connected: Boolean = true

    fun setConnected(status: Boolean) {
        connected = status
    }

    override fun isConnected(): Boolean = connected

    override fun getAvailableEntitlements(): List<Entitlement> = availableEntitlements

    override fun queryPurchasedSkus(): Set<String> = synchronized(ownedPurchases) {
        HashSet(ownedPurchases.keys)
    }

    override fun launchPurchaseFlow(skuId: String): BillingResult {
        if (!connected) {
            return BillingResult.Error(code = -1, message = "Billing client not connected")
        }

        // Rule 3 Validation: Explicit rejection of consumable currency purchases
        if (skuId.contains("gem", ignoreCase = true) || skuId.contains("coin", ignoreCase = true)) {
            return BillingResult.Error(
                code = 403,
                message = "Rule 3 Violation: In-app real money purchases for currency are prohibited."
            )
        }

        val entitlement = availableEntitlements.find { it.skuId == skuId }
            ?: return BillingResult.Error(code = 404, message = "SKU '$skuId' not found in entitlement catalog")

        synchronized(ownedPurchases) {
            if (ownedPurchases.containsKey(skuId)) {
                return BillingResult.ItemAlreadyOwned(skuId)
            }

            val record = PurchaseRecord(
                skuId = skuId,
                purchaseToken = "fake_token_${skuId}_${System.currentTimeMillis()}",
                purchaseTimeMs = System.currentTimeMillis(),
                isAcknowledged = true
            )
            ownedPurchases[skuId] = record
            return BillingResult.Success(record)
        }
    }

    override fun acknowledgePurchase(purchaseToken: String): Boolean = true
}
